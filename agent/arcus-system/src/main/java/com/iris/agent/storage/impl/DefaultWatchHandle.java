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
import java.nio.file.Path;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.storage.WatchListener;

public class DefaultWatchHandle extends AbstractWatchHandle implements FileAlterationListener {
   private final Path path;
   private final File file;
   private final FileAlterationObserver obs;

   public DefaultWatchHandle(File file, FileAlterationObserver obs) {
      this.file = file;
      this.path = this.file.toPath();
      this.obs = obs;
   }

   @Override
   public void cancel() {
      obs.removeListener(this);
   }

   @Override
   public void onStart(@Nullable FileAlterationObserver observer) {
   }

   @Override
   public void onStop(@Nullable FileAlterationObserver observer) {
   }

   @Override
   public void onDirectoryCreate(@Nullable File directory) {
      if (directory == null) {
         return;
      }
      
      if (matches(directory)) {
         for (WatchListener listener : listeners) {
            listener.onDirectoryCreate(directory);
         }
      }
   }

   @Override
   public void onDirectoryChange(@Nullable File directory) {
      if (directory == null) {
         return;
      }
      
      if (matches(directory)) {
         for (WatchListener listener : listeners) {
            listener.onDirectoryChange(directory);
         }
      }
   }

   @Override
   public void onDirectoryDelete(@Nullable File directory) {
      if (directory == null) {
         return;
      }
      
      if (matches(directory)) {
         for (WatchListener listener : listeners) {
            listener.onDirectoryDelete(directory);
         }
      }
   }

   @Override
   public void onFileCreate(@Nullable File file) {
      if (file == null) {
         return;
      }
      
      if (matches(file)) {
         for (WatchListener listener : listeners) {
            listener.onFileCreate(file);
         }
      }
   }

   @Override
   public void onFileChange(@Nullable File file) {
      if (file == null) {
         return;
      }
      
      if (matches(file)) {
         for (WatchListener listener : listeners) {
            listener.onFileChange(file);
         }
      }
   }

   @Override
   public void onFileDelete(@Nullable File file) {
      if (file == null) {
         return;
      }
      
      if (matches(file)) {
         for (WatchListener listener : listeners) {
            listener.onFileDelete(file);
         }
      }
   }

   private boolean matches(File event) {
      return event.toPath().startsWith(path);
   }
}

