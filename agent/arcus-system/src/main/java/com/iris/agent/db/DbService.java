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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.almworks.sqlite4java.SQLite;
import com.almworks.sqlite4java.SQLiteException;
import com.google.common.io.Files;
import com.iris.agent.storage.StorageService;
import com.iris.agent.util.NativeUtils;
import com.iris.agent.watchdog.WatchdogChecks;

public final class DbService {
   private static final Logger log = LoggerFactory.getLogger(DbService.class);
   private static final Object START_LOCK = new Object();
   private static final boolean ALLOW_MULTITHREADED;
   private static final long WAL_CHECKPOINT_MS;
   private static final int DEFAULT_MULTITHREADED_DB_WORKER_THREADS = 4;
   private static final long DEFAULT_WAL_CHECKPOINT_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);

   private static int DEFAULT_DB_WORKER_THREADS = 1;

   private static @Nullable Db db;
   private static @Nullable Map<String,Db> open;
   private static @Nullable ExecutorService es;
   private static @Nullable ExecutorService walEs;

   static {
      boolean allowMulti = true;
      if (System.getenv("IRIS_DB_DISABLE_MULTITHREADED") != null) {
         allowMulti = false;
      } else if (System.getenv("IRIS_DB_ENABLE_MULTITHREADED") != null) {
         allowMulti = true;
      }

      long walCheckpointMs = DEFAULT_WAL_CHECKPOINT_MS;
      if (System.getenv("IRIS_DB_DISABLE_WAL") != null) {
         walCheckpointMs = -1;
      } else if (System.getenv("IRIS_DB_ENABLE_WAL") != null) {
         walCheckpointMs = DEFAULT_WAL_CHECKPOINT_MS;
      } else if (System.getenv("IRIS_DB_WAL_CHECKPOINT_MS") != null) {
         try {
            walCheckpointMs = Long.parseLong(System.getenv("IRIS_DB_WAL_CHECKPOINT_MS"));
         } catch (Throwable th) {
            log.warn("cannot parse wal checkpoint time:", th);
         }
      }

      ALLOW_MULTITHREADED = allowMulti;
      WAL_CHECKPOINT_MS = walCheckpointMs;
   }

   private DbService() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Provided services
   /////////////////////////////////////////////////////////////////////////////

   public static Db get() {
      return getApplicationDb();
   }

   public static Db get(String name) {
      return get(name, DEFAULT_DB_WORKER_THREADS);
   }

   public static Db get(String name, int workerThreads) {
      return get(name, workerThreads, false);
   }

   public static Db getInMemoryDb(String name) {
      return get(name, 1, true);
   }

   private static Db get(String name, int workerThreads, boolean memory) {
      Map<String,Db> openDatabases = getOpenDatabases();
      ExecutorService executorService = getExecutorService();

      synchronized (openDatabases) {
         Db existing = openDatabases.get(name);
         if (existing != null) {
            return existing;
         }

         try {
            Db db;
            if (!memory) {
               File dbFile = StorageService.getFile("db:///" + name + ".db");
               db = new Db(name, dbFile, workerThreads, WAL_CHECKPOINT_MS);
            } else {
               db = new Db(name, null, workerThreads, -1);
            }

            db.start(executorService, walEs);
            openDatabases.put(name, db);
            return db;
         } catch (Exception ex) {
            throw new DbException("could not create database: " + name, ex);
         }
      }
   }

   public static Db get(String name, File path) {
      return get(name, path, DEFAULT_DB_WORKER_THREADS);
   }

   public static Db get(String name, File path, int workerThreads) {
      Map<String,Db> openDatabases = getOpenDatabases();
      ExecutorService executorService = getExecutorService();

      synchronized (openDatabases) {
         Db existing = openDatabases.get(name);
         if (existing != null) {
            return existing;
         }

         try {
            Db db = new Db(name, path, workerThreads, WAL_CHECKPOINT_MS);
            db.start(executorService, walEs);
            openDatabases.put(name, db);
            return db;
         } catch (Exception ex) {
            throw new DbException("could not create database: " + name, ex);
         }
      }
   }

   public static File getApplicationDbPath() {
      return getDbPath("iris");
   }

   public static File getDbPath(String name) {
      return StorageService.getFile("db:///" + name + ".db");
   }

   public static File getBackupDbPath(String name) {
      return StorageService.getFile("tmp:///" + name + ".db");
   }

   public static boolean remove(String name) {
      try {
         File dbFile = StorageService.getFile("db:///" + name + ".db");
         return dbFile.delete();
      } catch (Exception ex) {
         return false;
      }
   }

   public static void close(String name) {
      Map<String,Db> openDatabases = getOpenDatabases();

      Db existing = null;
      synchronized (openDatabases) {
         existing = openDatabases.remove(name);
      }

      if (existing != null) {
         existing.shutdown();
      }
   }

   public static void close(Db db) {
      close(db.getName());
   }

   /////////////////////////////////////////////////////////////////////////////
   // Startup and shutdown of the global database service
   /////////////////////////////////////////////////////////////////////////////

   public static void start() {
      synchronized (START_LOCK) {
         if (db != null) {
            throw new IllegalStateException("database service already started");
         }

         // Detemine the native library name
         String name = NativeUtils.getNativeLibraryPrefix() + "sqlite4java";
         if (NativeUtils.isLinux() && NativeUtils.isArm()) {
            name = name + "-linux-arm";
         } else if (NativeUtils.isLinux() && NativeUtils.isX86() && NativeUtils.is64bit()) {
            name = name + "-linux-amd64";
         } else if (NativeUtils.isLinux() && NativeUtils.isX86()) {
            name = name + "-linux-i386";
         } else if (NativeUtils.isMac()) {
            name = name + "-osx";
         } else if (NativeUtils.isWindows() && NativeUtils.isX86() && NativeUtils.is64bit()) {
            name = name + "-win32-x64";
         } else if (NativeUtils.isWindows() && NativeUtils.isX86()) {
            name = name + "-win32-x86";
         } else {
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            log.info("unknown architecture: os={}, arch={}", osName, osArch);
            log.info("native architecture info: os={}, arch={}", NativeUtils.getOperatingSystem(), NativeUtils.getArchitecture());
         }

         File existing = StorageService.getFile("agent:///native/" + name + NativeUtils.getNativeLibrarySuffix());
         if (existing.exists()) {
            log.info("using pre-extracted sqlite library at: " + existing);
            SQLite.setLibraryPath(existing.getParent());
         } else {
            // Extract and load the required native libraries
            InputStream is = null;
            OutputStream os = null;
            try {
               File dir = Files.createTempDir();
               dir.deleteOnExit();

               File file = new File(dir, name + NativeUtils.getNativeLibrarySuffix());
               file.deleteOnExit();

               log.info("using sqlite4java library at: {}", file);
               is = DbService.class.getResourceAsStream("/native/" + name + NativeUtils.getNativeLibrarySuffix());
               if (is == null) {
                  file.delete();
                  FileUtils.deleteDirectory(dir);
                  throw new DbException("could not startup database service: native library not found");
               }

               os = new FileOutputStream(file);
               IOUtils.copy(is, os);

               SQLite.setLibraryPath(dir.getAbsolutePath());
               log.info("using sqlite version: {}", SQLite.getSQLiteVersion());

               SQLite.setSharedCache(true);
               SQLite.setSoftHeapLimit(4*1024*1024);

               if (ALLOW_MULTITHREADED && SQLite.isThreadSafe()) {
                  log.warn("sqlite database is thread safe, enabling multi-threaded mode");
                  DEFAULT_DB_WORKER_THREADS = DEFAULT_MULTITHREADED_DB_WORKER_THREADS;
               } else {
                  log.warn("sqlite database is not thread safe, using single threaded mode");
               }

               if (WAL_CHECKPOINT_MS > 0) {
                  log.warn("sqlite write ahead log mode enabled, checkpointing every {}ms", WAL_CHECKPOINT_MS);
               }
            } catch (IOException | SQLiteException ex) {
               throw new DbException("could not startup database service", ex);
            } finally {
               IOUtils.closeQuietly(is);
               IOUtils.closeQuietly(os);

               is = null;
               os = null;
            }
         }

         // Create the shared tracking data structures
         ThreadFactory tf = new DbThreadFactory();
         BlockingQueue<Runnable> exq = new SynchronousQueue<>();
         BlockingQueue<Runnable> weq = new SynchronousQueue<>();
         ThreadPoolExecutor exec = new ThreadPoolExecutor(1, 256, 60, TimeUnit.SECONDS, exq, tf);
         ThreadPoolExecutor wexec = (WAL_CHECKPOINT_MS > 0) ? new ThreadPoolExecutor(1, 128, 60, TimeUnit.SECONDS, weq, tf) : null;

         exec.allowCoreThreadTimeOut(true);
         if (wexec != null) {
            wexec.allowCoreThreadTimeOut(true);
         }

         es = exec;
         walEs = wexec;
         open = new HashMap<>();

         WatchdogChecks.addExecutorWatchdog("db executor", exec);

         // Create the application db
         db = get("iris");
      }
   }

   public static void shutdown() {
      synchronized (START_LOCK) {
         if (open != null) {
            for (Db db : open.values()) {
               db.kill();
            }
         }

         ExecutorService esShutdown = es;
         if (esShutdown != null) {
            try {
               esShutdown.shutdown();
               esShutdown.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
               // shuttting down so ignore
            }
         }

         if (open != null) {
            for (Db db : open.values()) {
               db.shutdown();
            }
         }

         esShutdown = walEs;
         if (esShutdown != null) {
            try {
               esShutdown.shutdown();
               esShutdown.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
               // shuttting down so ignore
            }
         }

         open = null;
         es = null;
         db = null;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   public static void reloadApplicationDb() {
      if (db != null) {
         db.close();
      }

      db = get("iris");
   }

   private static Db getApplicationDb() {
      Db result = db;
      if (result == null) {
         throw new IllegalStateException("database service not started");
      }

      return result;
   }

   private static ExecutorService getExecutorService() {
      ExecutorService result = es;
      if (result == null) {
         throw new IllegalStateException("database service not started");
      }

      return result;
   }

   private static Map<String,Db> getOpenDatabases() {
      Map<String,Db> result = open;
      if (result == null) {
         throw new IllegalStateException("database service not started");
      }

      return result;
   }

   private static final class DbThreadFactory implements ThreadFactory {
      private final AtomicInteger num = new AtomicInteger();

      @Override
      public Thread newThread(@Nullable Runnable r) {
         Thread thr = new Thread(r);
         thr.setName("irdb" + num.getAndIncrement());

         return thr;
      }
   }
}

