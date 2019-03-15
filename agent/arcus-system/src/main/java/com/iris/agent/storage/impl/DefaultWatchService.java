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
package com.iris.agent.storage.impl;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.storage.WatchHandle;

public class DefaultWatchService {
   private final ConcurrentMap<File,FileAlterationObserver> watches;
   private final FileAlterationMonitor monitor;

   public DefaultWatchService(long watchIntervalInMs) throws Exception {
      this.watches = new ConcurrentHashMap<>();

      this.monitor = new FileAlterationMonitor(watchIntervalInMs);
      this.monitor.setThreadFactory(new Factory());
      this.monitor.start();
   }

   public WatchHandle watch(File file) throws IOException {
      // If the file does not exist of if it is a file instead of a directory
      // then we need to monitor the parent directory instead.
      File fl = file;
      if (!file.exists() || file.isFile()) {
         fl = file.getParentFile();
      }

      // If a observer is already registered for this file or for a parent directory then reuse.
      FileAlterationObserver obs = find(fl);
      if (obs == null) {
         obs = new FileAlterationObserver(fl);
         watches.put(fl, obs);
         monitor.addObserver(obs);
      }

      DefaultWatchHandle handle = new DefaultWatchHandle(file,obs);
      obs.addListener(handle);

      return handle;
   }

   @Nullable
   private FileAlterationObserver find(File file) {
      File parent = file.getParentFile();
      if (parent != null && parent != file) {
         FileAlterationObserver result = find(parent);
         if (result != null) {
            return result;
         }
      }

      return watches.get(file);
   }

   private final class Factory implements ThreadFactory {
      private final AtomicInteger num = new AtomicInteger(0);

      @Override
      public Thread newThread(@Nullable Runnable r) {
         if (r == null) throw new NullPointerException();

         Thread thr = new Thread(r);
         thr.setDaemon(true);
         thr.setName("ifsw" + num.getAndIncrement());
         return thr;
      }
   }
}

