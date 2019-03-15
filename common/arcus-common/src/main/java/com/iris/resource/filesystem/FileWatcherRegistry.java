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
/**
 * 
 */
package com.iris.resource.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.iris.resource.ResourceListener;
import com.iris.util.Subscription;
import com.iris.util.ThreadPoolBuilder;

/**
 * 
 */
public class FileWatcherRegistry {
   private static final Logger logger = LoggerFactory.getLogger(FileWatcherRegistry.class);
   
   private final LoadingCache<Path, Watcher> watchers;
   private final ExecutorService executor;

   /**
    * 
    */
   public FileWatcherRegistry() {
      this.watchers =
            CacheBuilder
               .newBuilder()
               .removalListener(new RemovalListener<Path, FileWatcherRegistry.Watcher>() {
                  @Override
                  public void onRemoval(RemovalNotification<Path, Watcher> notification) {
                     notification.getValue().stop();
                  }
               })
               .build(new CacheLoader<Path, Watcher>() {
                  @Override
                  public FileWatcherRegistry.Watcher load(Path key) throws Exception {
                     return watch(key);
                  }
               });
      this.executor =
            new ThreadPoolBuilder()
               .withNameFormat("resource-watcher-%d")
               .withMetrics("resource.watcher")
               .build();
   }
   
   @PreDestroy
   public void stop() {
      executor.shutdownNow();
   }
   
   public Subscription watch(File resource, ResourceListener listener) throws IOException {
      Path directory = resource.getParentFile().toPath();
      for(int i=0; i<10; i++) {
         try {
            return 
               watchers
                  .getUnchecked(directory)
                  .addListener(resource, listener);
         }
         catch(IllegalStateException e) {
            // someone just removed a watcher as we were trying to add one
         }
         catch(UncheckedExecutionException e) {
            if(e.getCause() instanceof IOException) {
               throw (IOException) e.getCause();
            }
            if(e.getCause() instanceof RuntimeException) {
               throw (RuntimeException) e.getCause();
            }
            throw e;
         }
      }
      throw new IOException("Unable to watch " + resource + " after 10 tries");
   }

   private Watcher watch(Path directory) {
      try {
         WatchService service = FileSystems.getDefault().newWatchService();
         directory.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
         Watcher watcher = new Watcher(service, directory);
         executor.submit(watcher);
         return watcher;
      } catch (IOException e) {
         throw new RuntimeException("Could not create watcher for resource: " + directory, e);
      }
   }
   
   private class Watcher implements Runnable {
      private final WatchService service;
      private final Path directory;
      private final Set<ResourceEntry> listeners;
      private boolean running = true;
      
      private Watcher(WatchService service, Path directory) throws IOException {
         this.service = service;
         this.directory = directory;
         this.listeners = Collections.synchronizedSet(new LinkedHashSet<ResourceEntry>());
      }
      
      public Subscription addListener(File resource, ResourceListener listener) {
         Path path = resource.toPath();
         Preconditions.checkArgument(path.startsWith(directory), "Invalid resource, not a child of " + directory);
         
         final ResourceEntry entry = new ResourceEntry(path, listener);
         synchronized(listeners) {
            if(!running) {
               throw new IllegalStateException("Added a listener to a stopped watcher");
            }
            listeners.add(entry);
         }
         return new Subscription() {
            @Override
            public void remove() {
               synchronized(listeners) {
                  listeners.remove(entry);
                  if(listeners.isEmpty()) {
                     stop();
                  }
               }
            }
         };
      }
      
      public void stop() {
         synchronized(listeners) {
            this.running = false;
         }
         try {
            service.close();
         }
         catch(IOException e) {
            logger.warn("Error closing watcher service", e);
         }
      }
      
      @Override
      public void run() {
         try {
            while (true) {
               WatchKey key = null;
               try {
                  key = service.take();
               } catch (ClosedWatchServiceException cwse) {
                  return;
               } catch (InterruptedException e) {
                  logger.error("Interrupted exception encountered [{}], cancelling watch", e);
                  return;
               }
               if (key != null) {
                  Set<ResourceEntry> toNotify = new LinkedHashSet<>();
                  // smash all events into a single notification, we
                  // don't tell the caller what the change was, so we
                  // might as well not poke it a bunch of times either
                  for (WatchEvent<?> event: key.pollEvents()) {
                     Object context = event.context();
                     if(!(context instanceof Path)) {
                        continue;
                     }
                     
                     Path changed = (Path) context;
                     synchronized(listeners) {
                        for(ResourceEntry listener: listeners) {
                           if(listener.matches(changed)) {
                              toNotify.add(listener);
                           }
                        }
                     }
                  }
                  for(ResourceEntry resource: toNotify) {
                     resource.onChanged();
                  }
               }
               boolean isValid = key.reset();
               if (!isValid) {
                  // The watcher has been closed.
                  return;
               }
            }
         }
         finally {
            watchers.invalidate(directory);
         }
      }
   }
   
   private static class ResourceEntry {
      private final Path resource;
      private final ResourceListener listener;
      
      ResourceEntry(Path resource, ResourceListener listener) {
         this.resource = resource;
         this.listener = listener;
      }
      
      public boolean matches(Path changedPath) {
         return this.resource.endsWith(changedPath);
      }
      
      public void onChanged() {
         try {
            listener.onChange();
         }
         catch(Exception e) {
            logger.warn("Error notifying listener [{}] of resource [{}] change", listener, resource, e);
         }
      }
   }
}

