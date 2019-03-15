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
package com.iris.driver.service.registry;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class DriverWatcher {
   private static final Logger logger = LoggerFactory.getLogger(DriverWatcher.class);
   private static final ThreadFactory factory =
         new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("driver-watcher-%d")
            .build();
   
   private final ExecutorService executor = 
         Executors.newSingleThreadExecutor(factory);
   private Set<DriverWatcherListener> listeners = new HashSet<>();
   private Path watchDir;
   
   public DriverWatcher(String directory) {
      File file = new File(directory);
            
      if (file.exists() && file.isDirectory()) {
         watchDir = file.toPath();
      }
      else {
         logger.error("Could not find driver directory [{}] in working directory [{}]", directory, new File("").getAbsolutePath());
      }
   }
   
   public void addListener(DriverWatcherListener listener) {
      listeners.add(listener);
   }
   
   public void removeListener(DriverWatcherListener listener) {
      listeners.remove(listener);
   }
   
   public void watch() {
      final WatchService watcher; 
      if (watchDir == null) {
         return;
      }
      try {
         watcher = FileSystems.getDefault().newWatchService();
         watchDir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
         logger.trace("Watching driver directory [{}]", watchDir);
      } catch (IOException ex) {
         logger.error("Could not register watcher for driver directory [{}]", watchDir);
         return;
      }
      
      executor.submit(new Runnable() {
         @Override
         public void run() {
            while (true) {
               WatchKey key = null;
               try {
                  key = watcher.take();
               } catch (InterruptedException e) {
                  logger.error("Interrupted Exception encountered [{}]", e);
               }
               if (key != null) {
                  if (key.pollEvents().size() > 0) {
                     // Something has changed. That's all we need to know.
                     for (DriverWatcherListener listener : listeners) {
                        listener.onChange();
                     }
                  }
                  boolean isValid = key.reset();
                  if (!isValid) {
                     logger.error("Unable to watch driver directory. Watcher key invalid.");
                  }
               }
            }
         }
      });
   }

   public void shutdown() {
      executor.shutdownNow();
   }

}

