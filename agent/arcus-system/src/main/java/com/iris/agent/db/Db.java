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
package com.iris.agent.db;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLiteBackup;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteConstants;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.iris.agent.storage.StorageService;
import com.iris.agent.watchdog.WatchdogPoke;
import com.iris.agent.watchdog.WatchdogService;
import com.iris.agent.util.Backoff;
import com.iris.agent.util.Backoffs;
import com.iris.agent.util.ThreadUtils;

public final class Db implements AutoCloseable {
   private static final Logger log = LoggerFactory.getLogger(Db.class);
   private static final int SQLITE_MAX_ATTEMPTS = 5;

   private static final DbTask<?> POISON_PILL = new AbstractDbTask<Object>() {
      @Override
      @Nullable
      public Object execute(SQLiteConnection conn) throws Exception {
         return null;
      }
   };

   private final String name;
   private final int numWorkers;
   private final long walModeCheckpointNs;
   private final BlockingQueue<DbTask<?>> queue;
   private final @Nullable DbCheckpointer checkpointer;

   @Nullable
   private final File dbFile;

   Db(String name, @Nullable File dbFile, int numWorkers, long walModeCheckpointMs) {
      this.name = name;
      this.walModeCheckpointNs = TimeUnit.NANOSECONDS.convert(walModeCheckpointMs, TimeUnit.MILLISECONDS);
      this.dbFile = dbFile;
      this.numWorkers = numWorkers;
      this.queue = new SynchronousQueue<>();
      this.checkpointer = (walModeCheckpointMs > 0) ? new DbCheckpointer() : null;
   }

   public String getName() {
      return name;
   }

   void start(ExecutorService es, @Nullable ExecutorService walEs) {
      for (int i = 0; i < numWorkers; ++i) {
         es.submit(new DbWorker(WatchdogService.createWatchdogPoke(name + " db" + i)));
      }

      if (walEs != null && checkpointer != null) {
         walEs.submit(checkpointer);
      }
   }

   void kill() {
      for (int i = 0; i < numWorkers; ++i) {
         try {
            queue.put(POISON_PILL);
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
         }
      }
   }

   void shutdown() {
      if (checkpointer != null) {
         checkpointer.shutdown();
      }
   }

   @Override
   public void close() {
      DbService.close(this);
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   public long getLastRowId() {
      return DbUtils.complete(asyncLastRowId());
   }

   public Future<Long> asyncLastRowId() {
      try {
         GetRowIdTask task = new GetRowIdTask();
         queue.put(task);
         return task;
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Backup API
   /////////////////////////////////////////////////////////////////////////////

   public boolean backup(String name) {
      return DbUtils.complete(asyncBackup(name));
   }

   public Future<Boolean> asyncBackup(String name) {
      try {
         BackupTask task = new BackupTask(DbService.getBackupDbPath(name));
         queue.put(task);
         return task;
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   @Nullable
   public Db backupAndConnect(String name) {
      if (!backup(name)) {
         return null;
      }

      return DbService.get(name, DbService.getBackupDbPath(name));
   }

   /////////////////////////////////////////////////////////////////////////////
   // SQL statements
   /////////////////////////////////////////////////////////////////////////////

   public void execute(File file) {
      DbUtils.complete(asyncExecute(file));
   }

   public void execute(URI uri) {
      DbUtils.complete(asyncExecute(uri));
   }

   public void execute(URL url) {
      DbUtils.complete(asyncExecute(url));
   }

   public void execute(String sql, Object... args) {
      DbUtils.complete(asyncExecute(sql, args));
   }

   public <T> void execute(String sql, DbBinder<T> binder, T value) {
      DbUtils.complete(asyncExecute(sql, binder, value));
   }

   public <T> void execute(String sql, DbBinder<T> binder, List<T> values) {
      DbUtils.complete(asyncExecute(sql, binder, values));
   }

   public <T> void execute(String sql, DbBinder<T> binder, Collection<T> values) {
      DbUtils.complete(asyncExecute(sql, binder, values));
   }

   public Future<?> asyncExecute(File file) {
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
         return asyncExecute(IOUtils.toString(is, "UTF-8"));
      } catch (Throwable ex) {
         throw new DbException("could not execute sql: " + file, ex);
      }
   }

   public Future<?> asyncExecute(URI uri) {
      try (InputStream is = StorageService.getInputStream(uri)) {
         return asyncExecute(IOUtils.toString(is, "UTF-8"));
      } catch (Throwable ex) {
         throw new DbException("could not execute sql: " + uri, ex);
      }
   }

   public Future<?> asyncExecute(URL url) {
      try {
         return asyncExecute(IOUtils.toString(url, "UTF-8"));
      } catch (Throwable ex) {
         throw new DbException("could not execute sql: " + url, ex);
      }
   }

   public Future<?> asyncExecute(String sql, Object... args) {
      if (args.length == 0) {
         return asyncExecute(sql, null, (Object)null);
      }

      return asyncExecute(sql, ArgsBinder.INSTANCE, Arrays.asList(args));
   }

   public <T> Future<?> asyncExecute(String sql, @Nullable DbBinder<T> binder, @Nullable T value) {
      try {
         ExecTask<T> task = new ExecTask<>(sql, binder, value);
         queue.put(task);
         return task;
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   public <T> Future<?> asyncExecute(String sql, DbBinder<T> binder, List<T> values) {
      try {
         ExecBulkTask<T> task = new ExecBulkTask<>(sql, binder, values);
         queue.put(task);
         return task;
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   public <T> Future<?> asyncExecute(String sql, DbBinder<T> binder, Collection<T> values) {
      try {
         ExecBulkTask<T> task = new ExecBulkTask<>(sql, binder, new ArrayList<T>(values));
         queue.put(task);
         return task;
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // SQL queries
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   public List<?> query(String sql, Object... params) {
      return DbUtils.complete(asyncQuery(sql, params));
   }

   @Nullable
   public <T> T querySingleColumn(Class<T> type, String sql, Object... params) {
      return DbUtils.complete(asyncQuerySingleColumn(type, sql, params));
   }

   public <T> T query(String sql, DbExtractor<T> extractor) {
      return DbUtils.complete(asyncQuery(sql, ArgsBinder.INSTANCE, Collections.emptyList(), extractor));
   }

   public <I,O> O query(String sql, DbBinder<I> binder, I value, DbExtractor<O> extractor) {
      return DbUtils.complete(asyncQuery(sql, binder, value, extractor));
   }

   public <T> List<T> queryAll(String sql, DbExtractor<T> extractor) {
      return DbUtils.complete(asyncQueryAll(sql, extractor));
   }

   public <I,O> List<O> queryAll(String sql, DbBinder<I> binder, I value, DbExtractor<O> extractor) {
      return DbUtils.complete(asyncQueryAll(sql, binder, value, extractor));
   }

   public Future<List<?>> asyncQuery(String sql, Object... args) {
      return asyncQuery(sql, ArgsBinder.INSTANCE, Arrays.asList(args), ListExtractor.INSTANCE);
   }

   public <T> Future<T> asyncQuerySingleColumn(Class<T> type, String sql, Object... args) {
      return asyncQuery(sql, ArgsBinder.INSTANCE, Arrays.asList(args), new SingleExtractor<T>(type));
   }

   public <I,O> Future<O> asyncQuery(String sql, DbBinder<I> binder, I value, DbExtractor<O> extractor) {
      try {
         QueryTask<I,O> task = new QueryTask<>(sql, binder, value, extractor);
         queue.put(task);
         return task;
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   public <T> Future<List<T>> asyncQueryAll(String sql, DbExtractor<T> extractor) {
      return asyncQueryAll(sql, ArgsBinder.INSTANCE, Collections.emptyList(), extractor);
   }

   public <I,O> Future<List<O>> asyncQueryAll(String sql, DbBinder<I> binder, I value, DbExtractor<O> extractor) {
      try {
         QueryBulkTask<I,O> task = new QueryBulkTask<>(sql, binder, value, extractor);
         queue.put(task);
         return task;
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   private SQLiteConnection createConnection() {
      try {
         SQLiteConnection db = new SQLiteConnection(dbFile);
         db.openV2(
            SQLiteConstants.SQLITE_OPEN_READWRITE |
            SQLiteConstants.SQLITE_OPEN_CREATE |
            SQLiteConstants.SQLITE_OPEN_FULLMUTEX |
            SQLiteConstants.SQLITE_OPEN_PRIVATECACHE
         );
         return db;
      } catch (SQLiteException ex) {
         throw new DbException("could not connect to db", ex);
      }
   }

   private static final class BackupTask extends AbstractDbTask<Boolean> {
      private final File destination;

      public BackupTask(File destination) {
         this.destination = destination;
      }

      @Override
      @Nullable
      public Boolean execute(SQLiteConnection conn) throws Exception {
         SQLiteBackup backup = conn.initializeBackup(destination);
         try {
            backup.backupStep(-1);
         } finally {
            backup.dispose();
         }

         return backup.isFinished();
      }
   }

   private static final class GetRowIdTask extends AbstractDbTask<Long> {
      @Nullable
      @Override
      public Long execute(SQLiteConnection conn) throws Exception {
         return conn.getLastInsertId();
      }
   }

   private static final class ExecTask<T> extends DbUtils.DbTaskImpl<T,Void> {
      ExecTask(String sql, @Nullable DbBinder<T> binder, @Nullable T value) {
         super(sql, binder, value);
      }

      @Override
      public void row(SQLiteConnection conn, SQLiteStatement stmt) {
      }

      @Nullable
      @Override
      public Void results(SQLiteConnection conn) {
         return null;
      }
   }

   private static final class ExecBulkTask<T> extends DbUtils.DbBulkTaskImpl<T,Void> {
      ExecBulkTask(String sql, DbBinder<T> binder, List<T> values) {
         super(sql, binder, values);
      }

      @Override
      protected boolean runInTransaction(SQLiteConnection conn, SQLiteStatement stmt) {
         return true;
      }

      @Override
      public void next(SQLiteConnection conn, SQLiteStatement stmt) {
      }

      @Override
      public void row(SQLiteConnection conn, SQLiteStatement stmt) {
      }

      @Nullable
      @Override
      public Void results(SQLiteConnection conn) {
         return null;
      }
   }

   private static final class QueryTask<I,O> extends DbUtils.DbTaskImpl<I,O> {
      private final DbExtractor<O> extractor;

      @Nullable
      public O result;

      QueryTask(String sql, DbBinder<I> binder, I value, DbExtractor<O> extractor) {
         super(sql, binder, value);
         this.extractor = extractor;
      }

      @Override
      public void row(SQLiteConnection conn, SQLiteStatement stmt) {
         if (result != null) {
            return;
         }

         try {
            result = extractor.extract(conn, stmt);
         } catch (Exception ex) {
            log.debug("sql extractor failed: {}", ex.getMessage(), ex);
         }
      }

      @Nullable
      @Override
      public O results(SQLiteConnection conn) {
         return result;
      }
   }

   private static final class QueryBulkTask<I,O> extends DbUtils.DbTaskImpl<I,List<O>> {
      private final DbExtractor<O> extractor;
      public List<O> results;

      QueryBulkTask(String sql, DbBinder<I> binder, I value, DbExtractor<O> extractor) {
         super(sql, binder, value);
         this.extractor = extractor;
         this.results = new ArrayList<>();
      }

      @Override
      public void row(SQLiteConnection conn, SQLiteStatement stmt) {
         try {
            O result = extractor.extract(conn, stmt);
            if (result != null) {
               results.add(result);
            }
         } catch (Exception ex) {
            log.debug("sql extractor failed: {}", ex.getMessage(), ex);
         }
      }

      @Override
      public List<O> results(SQLiteConnection conn) {
         return results;
      }
   }

   private static enum ArgsBinder implements DbBinder<List<?>> {
      INSTANCE;

      @Override
      public void bind(SQLiteConnection conn, SQLiteStatement stmt, List<?> args) throws Exception {
         int idx = 0;
         for (Object arg : args) {
            DbUtils.bind(stmt, arg, ++idx);
         }
      }
   }

   private static final class SingleExtractor<T> implements DbExtractor<T> {
      private final Class<T> type;

      private SingleExtractor(Class<T> type) {
         this.type = type;
      }

      @Override
      public T extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         return DbUtils.extract(stmt, type, 0);
      }
   }

   private static enum ListExtractor implements DbExtractor<List<?>> {
      INSTANCE;

      @Override
      public List<?> extract(SQLiteConnection conn, SQLiteStatement stmt) throws Exception {
         int count = stmt.columnCount();
         List<Object> results = new ArrayList<>(count);
         for (int i = 0; i < count; ++i) {
            results.add(stmt.columnValue(i));
         }

         return results;
      }
   }

   private abstract class AbstractDbWorker implements Runnable {
      protected boolean isBatteryPoweredMode = false;

      @Override
      public void run() {
         SQLiteConnection conn;

         synchronized (AbstractDbWorker.class) {
            conn = createConnection();

            try {
               conn.setBusyTimeout(TimeUnit.MINUTES.toMillis(5));
               conn.exec("PRAGMA foreign_keys=ON");
               conn.exec("PRAGMA mmap_size=4194304");

               conn.exec("PRAGMA locking_mode=NORMAL");
               if (walModeCheckpointNs > 0) {
                  conn.exec("PRAGMA journal_mode=WAL");
                  conn.exec("PRAGMA synchronous=FULL");

                  if (checkpointer == null) {
                     conn.exec("PRAGMA wal_autocheckpoint=64");
                  } else {
                     conn.exec("PRAGMA wal_autocheckpoint=0");
                  }
               } else {
                  conn.exec("PRAGMA journal_mode=DELETE");
               }
            } catch (Throwable e) {
               log.error("could not start sql db worker: {}", e.getMessage(), e);
               throw new RuntimeException(e);
            }
         }

         try {
            execute(conn);
         } catch (InterruptedException ex) {
            log.warn("sqlite worker exiting due to shutdown");
         } catch (Throwable th) {
            log.warn("sqlite worker exiting abnormally", th);
         } finally {
            synchronized (AbstractDbWorker.class) {
               conn.dispose();
            }
         }
      }

      protected abstract void execute(SQLiteConnection conn) throws Exception;
   }

   private final class DbCheckpointer extends AbstractDbWorker {
      private @Nullable Thread thread;
      private final AtomicBoolean shutdown = new AtomicBoolean();
      private @Nullable SQLiteStatement checkpoint;

      private long lastCheckPointTime = System.nanoTime();

      @Override
      protected void execute(SQLiteConnection conn) throws Exception {
         log.trace("sqlite write ahead log checkpointer started...");
         thread = Thread.currentThread();
         try {
            checkpoint = conn.prepare("PRAGMA wal_checkpoint(TRUNCATE)", false);
            while (!shutdown.get()) {
               long now = System.nanoTime();
               long elapsed = now - lastCheckPointTime;
               if (elapsed > walModeCheckpointNs) {
                  lastCheckPointTime = now;
                  checkpoint(conn);
               }

               ThreadUtils.sleep(5, TimeUnit.SECONDS);
            }
         } catch (InterruptedException ex) {
            log.warn("sqlite checkpointer interrupted, shutting down...");
         } finally {
            log.warn("sqlite checkpointer shutting down...");
            thread = null;

            if (checkpoint != null) {
               checkpoint.dispose();
            }

            log.warn("killing sqlite wal checkpointer...");
         }
      }

      private void checkpoint(SQLiteConnection conn) throws Exception {
         long start = System.nanoTime();

         SQLiteStatement stmt = checkpoint;
         if (stmt == null) {
            log.warn("sqlite checkpoint statemnt null, skipping checkpoint");
            return;
         }

         try {
            stmt.stepThrough();
         } catch (SQLiteException ex) {
            switch (ex.getBaseErrorCode()) {
            case SQLiteConstants.SQLITE_BUSY:
            case SQLiteConstants.SQLITE_LOCKED:
               log.info("checkpoint failed: db busy");
               break;

            default:
               break;
            }
         } catch (Exception ex) {
            // ignore
         } finally {
            stmt.reset();
         }

         long elapsed = TimeUnit.MICROSECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
         log.debug("sqlite checkpoint complete in {}us", elapsed);
      }

      void shutdown() {
         shutdown.set(true);

         Thread thr = thread;
         if (thr != null) {
            thr.interrupt();
         }
      }
   }

   private final class DbWorker extends AbstractDbWorker {
      private final WatchdogPoke watchdog;
      private final Backoff backoff = Backoffs.exponential()
         .initial(0, TimeUnit.MILLISECONDS)
         .delay(5, TimeUnit.MILLISECONDS)
         .random(0.2)
         .max(500, TimeUnit.MILLISECONDS)
         .build();

      public DbWorker(WatchdogPoke watchdog) {
         this.watchdog = watchdog;
      }

      @Override
      @SuppressWarnings("unchecked")
      protected void execute(SQLiteConnection conn) throws Exception {
         log.trace("starting sqlite db worker...");
         while (true) {
            watchdog.poke();

            DbTask<?> task = queue.poll(10, TimeUnit.SECONDS);
            if (task == POISON_PILL) {
               log.trace("killing sqlite db worker...");
               return;
            }

            if (task == null || task.isDone()) {
               continue;
            }

            //long start = System.nanoTime();
            boolean retry = true;
            backoff.onSuccess();
            while (retry) {
               try {
                  retry = false;
                  Object result = task.execute(conn);

                  //long elapsed = TimeUnit.MICROSECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                  //log.info("sql statment took {}us: {}", elapsed, task);

                  ((DbTask<Object>)task).set(result);
               } catch (SQLiteException ex) {
                  switch (ex.getBaseErrorCode()) {
                  case SQLiteConstants.SQLITE_BUSY:
                     if (backoff.attempt() < SQLITE_MAX_ATTEMPTS) {
                        log.info("sqlite statement failed on attempt {} of {}: db busy", backoff.attempt(), SQLITE_MAX_ATTEMPTS);
                        retry = true;
                        ThreadUtils.sleep(backoff.nextDelay(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
                     } else {
                        fail(conn, task, ex);
                     }
                     break;

                  case SQLiteConstants.SQLITE_LOCKED:
                     if (backoff.attempt() < SQLITE_MAX_ATTEMPTS) {
                        log.info("sqlite statement failed on attempt {} of {}: db locked", backoff.attempt(), SQLITE_MAX_ATTEMPTS);
                        retry = true;
                        ThreadUtils.sleep(backoff.nextDelay(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
                     } else {
                        fail(conn, task, ex);
                     }
                     break;

                  default:
                     fail(conn, task, ex);
                     break;
                  }
               }
            }
         }
      }

      private void fail(SQLiteConnection conn, DbTask<?> task, Exception ex) {
         try {
            log.debug("failed to execute sql: {}", ex.getMessage());
            task.setException(ex);
         } catch (Exception fex) {
            log.warn("failed to fail task: ", fex);
         }
      }
   }
}

