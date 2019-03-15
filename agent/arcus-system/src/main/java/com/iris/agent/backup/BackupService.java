/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.agent.backup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.config.ConfigService;
import com.iris.agent.db.Db;
import com.iris.agent.db.DbService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.storage.StorageService;
import com.iris.agent.util.RxIris;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.HubBackupCapability;
import com.iris.messages.capability.HubCapability;

import rx.Observable;
import rx.Subscriber;

public final class BackupService {
   private static final Logger log = LoggerFactory.getLogger(BackupService.class);
   private static Serializer<MessageBody> MIGRATION_REPORT_SERIALIZER = JSON.createSerializer(MessageBody.class);
   private static Deserializer<MessageBody> MIGRATION_REPORT_DESERIALIZER = JSON.createDeserializer(MessageBody.class);

   private static final CopyOnWriteArraySet<BackupListener> listeners = new CopyOnWriteArraySet<>();
   private static final CopyOnWriteArraySet<BackupFinishedListener> finishListeners = new CopyOnWriteArraySet<>();
   private static final AtomicInteger numFinishedMigrations = new AtomicInteger(0);
   private static final AtomicInteger numStartedMigrations = new AtomicInteger(0);
   private static final AtomicInteger numMigrationDevices = new AtomicInteger(0);
   private static final AtomicInteger numMigrationDevicesFinished = new AtomicInteger(0);

   private BackupService() {
   }

   public static void start() {
      listeners.clear();
   }

   public static void shutdown() {
      listeners.clear();
   }

   public static void addListener(BackupListener listener) {
      listeners.add(listener);
   }

   public static void removeListener(BackupListener listener) {
      listeners.remove(listener);
   }

   public static void addListener(BackupFinishedListener listener) {
      finishListeners.add(listener);
   }

   public static void removeListener(BackupFinishedListener listener) {
      finishListeners.remove(listener);
   }

   @Nullable
   public static File doBackup() {
      ExecutorService es = Executors.newFixedThreadPool(listeners.isEmpty() ? 1 : listeners.size());
      String name = "backup." + Long.toHexString(ThreadLocalRandom.current().nextLong());

      try (final Db backupDb = DbService.get().backupAndConnect(name)) {
         if (backupDb == null) {
            return null;
         }

         List<Callable<Void>> tasks = getBackupTasks(backupDb);
         List<Void> results = invokeAllAndGather(es, tasks);

         log.info("backup results: {}", results);
         return DbService.getBackupDbPath(name);
      }
   }

   public static void doRestore(final File path) {
      Future<?> result = ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            doRestoreProcess(path);
         }
      }, 90, TimeUnit.SECONDS);

      try {
         result.get(10, TimeUnit.MINUTES);
      } catch (Exception ex) {
         throw new RuntimeException("migration phase finish", ex);
      }
   }

   private static void doRestoreProcess(File path) {
      ExecutorService es = Executors.newFixedThreadPool(listeners.isEmpty() ? 1 : listeners.size());
      String name = "restore." + Long.toHexString(ThreadLocalRandom.current().nextLong());

      try {
         Db restoreDb = DbService.get(name,path);

         List<Callable<Callable<Void>>> validateTasks = getRestoreTasks(restoreDb);
         List<Callable<Void>> restoreTasks = invokeAllAndGather(es, validateTasks);
         List<Void> results = invokeAllAndGather(es, restoreTasks);

         log.info("restore results: {}", results);
         updateApplicationDb(restoreDb, path);
      } catch (RuntimeException ex) {
         log.warn("could not restore hub:", ex);
         throw ex;
      } catch (Exception ex) {
         log.warn("could not restore hub:", ex);
         throw new RuntimeException(ex);
      } finally {
         path.delete();
      }
   }

   public static Observable<?> doMigrateV1(final JsonObject data) {
      final ExecutorService es = Executors.newFixedThreadPool(listeners.isEmpty() ? 1 : listeners.size());
      String name = "migrate." + Long.toHexString(ThreadLocalRandom.current().nextLong());

      File path;
      try {
         path = StorageService.createTempFile("migrate", "db");
      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }

      final Db restoreDb = DbService.get(name,path);
      ConfigService.setupSchema(restoreDb);

      markMigrationPhase0(restoreDb);
      UUID accId = HubAttributesService.getAccountId();
      UUID plcId = HubAttributesService.getPlaceId();
      if (accId == null || plcId == null) {
         throw new RuntimeException("cannot migrate hub with no account id or place id");
      }

      JsonObject migData = data.getAsJsonObject("migration");
      if (migData == null || migData.isJsonNull()) {
         throw new RuntimeException("could not migrate zigbee devices: no zigbee migration data present");
      }

      JsonArray deviceData = data.get("devices").getAsJsonArray();
      if (deviceData == null || deviceData.isJsonNull()) {
         throw new RuntimeException("could not migrate zigbee devices: no device data present");
      }

      JsonArray infoData = data.get("info").getAsJsonArray();
      if (infoData == null || infoData.isJsonNull()) {
         throw new RuntimeException("could not migrate zigbee devices: no migration info table present");
      }

      Map<Long,Map<String,String>> devices = BackupUtils.getDevices(deviceData);
      final V1MigrationReport report = new V1MigrationReport(devices);

      ConfigService.put(restoreDb, HubCapability.ATTR_PLACE, plcId.toString());
      ConfigService.put(restoreDb, HubCapability.ATTR_ACCOUNT, accId.toString());

      List<Callable<Callable<Void>>> validateTasks = getMigrateV1Tasks(restoreDb, migData, infoData, devices, report);
      final List<Callable<Void>> restoreTasks = invokeAllAndGather(es, validateTasks);
      es.shutdownNow();

      markMigrationPhase1(restoreDb);

      for (int i = 1; i <= 9; ++i) {
         final int num = i;
         ExecService.periodic().schedule(new Runnable() {
            @Override public void run() { BackupService.markMigrationPhase2WaitStep(restoreDb, num, 10); }
         }, i*6, TimeUnit.SECONDS);
      }

      final File dbFilePath = path;
      return Observable.create(new RxIris.OnSubscribe<Object>() {
         @Override
         public void run(Subscriber<? super Object> sub) {
            try {
               BackupService.markMigrationPhase2WaitStep(restoreDb, 10, 10);
               doMigrateV1Process(dbFilePath, report, restoreDb, restoreTasks, data);
               if (!sub.isUnsubscribed()) {
                  sub.onCompleted();
               }
            } catch (Throwable th) {
               if (!sub.isUnsubscribed()) {
                  sub.onError(th);
               }
            }
         }
      }).delaySubscription(60, TimeUnit.SECONDS, RxIris.io);
   }

   private static void doMigrateV1Process(@Nullable File path, V1MigrationReport report, Db restoreDb, List<Callable<Void>> restoreTasks, JsonObject data) {
      try {
         ExecutorService es = Executors.newFixedThreadPool(listeners.isEmpty() ? 1 : listeners.size());

         invokeAllAndGather(es, restoreTasks);
         markMigrationPhase2(restoreDb);
      } catch (RuntimeException ex) {
         log.warn("could not migrate hub:", ex);
         throw ex;
      } catch (Throwable ex) {
         log.warn("could not migrate hub:", ex);
         throw new RuntimeException(ex);
      } finally {
         MessageBody finished = HubBackupCapability.RestoreFinishedEvent.builder()
            .withReport(report.getReportAsList())
            .build();

         byte[] result = MIGRATION_REPORT_SERIALIZER.serialize(finished);
         BackupUtils.putName(restoreDb, "migration-report", Base64.encodeBase64String(result));
         updateApplicationDb(restoreDb, path);

         log.info("migration results: {}", new String(result, StandardCharsets.UTF_8));
         if (path != null) {
            path.delete();
         }
      }
   }

   // Initial start: 10% done
   public static void markMigrationPhase0(Db db) {
      BackupUtils.putName(db, "migration-phase", "0");
      sendProgressUpdate(0.10);
   }

   // Validation completed: 15% done
   public static void markMigrationPhase1(Db db) {
      BackupUtils.putName(db, "migration-phase", "1");
      sendProgressUpdate(0.15);
   }

   // Migration waiting: 15-25% done
   public static void markMigrationPhase2WaitStep(Db db, int num, int total) {
      double done = .15 + ((double)num/(double)total)*.10;
      sendProgressUpdate(done);
   }

   // Migration committing: 25-35% done
   public static void markMigrationPhase2Step(Db db, int num, int total) {
      if (total <= 0) {
         return;
      }

      double done = .25 + ((double)num/(double)total)*.10;
      sendProgressUpdate(done);
   }

   // Migration committed: 35% done
   public static void markMigrationPhase2(Db db) {
      BackupUtils.putName(db, "migration-phase", "2");
      sendProgressUpdate(0.35);
   }

   // Migration Restart complete: 50% done
   public static void markMigrationPhase3() {
      BackupUtils.putName(DbService.get(), "migration-phase", "3");
      sendProgressUpdate(0.5);
   }

   public static void checkCurrentMigrationPhase() {
      if ("2".equals(BackupUtils.getName("migration-phase"))) {
         markMigrationPhase3();
      }
   }

   // Migration Device Added: 50 - 95%
   public static void markMigrationStarted(String type, int num) {
      log.info("{} started add phase of migration process for {} devices", type, num);

      numStartedMigrations.incrementAndGet();
      numMigrationDevices.addAndGet(num);
   }

   public static void markMigrationPhase3Step(String type, int numDone) {
      int numFinished = numMigrationDevicesFinished.addAndGet(numDone);
      if (numStartedMigrations.get() < 3) {
         log.info("skipping migration progress event, only {} of 3 migration processes have started", numStartedMigrations.get());
         return;
      }

      int numTotal = numMigrationDevices.get();
      if (numTotal <= 0) {
         log.info("skipping migration progress event, no devices to migrate");
         return;
      }

      double percent = (double)numFinished / (double)numTotal;
      double sendPercent = 0.5 + percent*0.45;
      sendProgressUpdate(sendPercent);
   }

   public static void markMigrationPhase3Finished(String type) {
      log.info("{} finished add phase of migration process", type);
      if (numFinishedMigrations.incrementAndGet() == 3) {
         markMigrationPhase4();
         doFinishMigrateV1();
      }
   }

   // Migration complete: 100% done
   public static void markMigrationPhase4() {
      BackupUtils.putName(DbService.get(), "migration-phase", "4");
      sendProgressUpdate(1.0);
   }

   public static void doFinishMigrateV1() {
      String report = BackupUtils.getName("migration-report");
      if (report == null || report.trim().isEmpty()) {
         return;
      }

      MessageBody finished = MIGRATION_REPORT_DESERIALIZER.deserialize(Base64.decodeBase64(report));
      log.info("sending migration report: {}", finished);

      for (BackupFinishedListener listener : finishListeners) {
         listener.v1MigrationFinished(finished);
      }
   }

   public static boolean isDuringBackup() {
      String phase = BackupUtils.getName("migration-phase");
      if (phase == null || phase.trim().isEmpty()) {
         return false;
      }

      phase = phase.trim();
      return !"4".equals(phase);
   }

   private static void sendProgressUpdate(double percent) {
      double done = 100 * percent;
      done = (int)(10*done + 0.5) / 10.0;

      log.info("migration is {}% complete", done);
      MessageBody report = HubBackupCapability.RestoreProgressEvent.builder()
         .withProgress(done)
         .build();

      for (BackupFinishedListener listener : finishListeners) {
         listener.v1MigrationFinished(report);
      }
   }

   private static void updateApplicationDb(@Nullable Db restoreDb, File path) {
      try {
         File appDbPath = DbService.getApplicationDbPath();

         Path updatedDbPath = FileSystems.getDefault().getPath(path.getAbsolutePath());
         Path existingDbPath = FileSystems.getDefault().getPath(appDbPath.getAbsolutePath());

         Files.move(updatedDbPath, existingDbPath, StandardCopyOption.REPLACE_EXISTING);

         if (restoreDb != null) {
            restoreDb.close();
         }

         DbService.reloadApplicationDb();
      } catch (IOException ex) {
         log.warn("could not overwrite application database with backup copy:", ex);
         throw new RuntimeException(ex);
      }
   }

   private static List<Callable<Void>> getBackupTasks(final Db db) {
      List<Callable<Void>> tasks = new ArrayList<>(listeners.size());
      for (final BackupListener listener : listeners) {
         tasks.add(new Callable<Void>() {
            @Override
            @Nullable
            public Void call() {
               listener.hubBackup(db);
               return null;
            }
         });
      }

      return tasks;
   }

   private static List<Callable<Callable<Void>>> getRestoreTasks(final Db db) {
      List<Callable<Callable<Void>>> tasks = new ArrayList<>(listeners.size());
      for (final BackupListener listener : listeners) {
         tasks.add(new Callable<Callable<Void>>() {
            @Override
            @Nullable
            public Callable<Void> call() {
               return listener.hubValidateRestore(db);
            }
         });
      }

      return tasks;
   }

   private static List<Callable<Callable<Void>>> getMigrateV1Tasks(final Db db, final JsonObject migData, final JsonArray infoData, final Map<Long,Map<String,String>> devices, final V1MigrationReport report) {
      List<Callable<Callable<Void>>> tasks = new ArrayList<>(listeners.size());
      for (final BackupListener listener : listeners) {
         tasks.add(new Callable<Callable<Void>>() {
            @Override
            @Nullable
            public Callable<Void> call() {
               return listener.hubValidateMigrateV1(db, migData, infoData, devices, report);
            }
         });
      }

      return tasks;
   }

   private static <T> List<T> invokeAllAndGather(ExecutorService es, List<Callable<T>> tasks) {
      try {
         List<Future<T>> results = es.invokeAll(tasks);

         List<T> values = new ArrayList<>(results.size());
         for (Future<T> result : results) {
            values.add(result.get());
         }

         return values;
      } catch (RuntimeException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }
}

