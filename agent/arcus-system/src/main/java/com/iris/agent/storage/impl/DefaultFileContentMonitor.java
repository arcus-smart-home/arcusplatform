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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.storage.FileContentListener;
import com.iris.agent.storage.FileContentMonitor;
import com.iris.agent.storage.StorageService;
import com.iris.agent.storage.WatchHandle;
import com.iris.agent.storage.WatchListener;

public class DefaultFileContentMonitor implements FileContentMonitor, WatchListener {
   private final CopyOnWriteArraySet<FileContentListener> listeners = new CopyOnWriteArraySet<>();
   private final WatchHandle handle;

   @Nullable
   private volatile String contents = null;

   public DefaultFileContentMonitor(WatchHandle handle) throws IOException {
      this.handle = handle;
   }

   public void setup(File file) throws IOException {
      handle.addWatchListener(this);
      reload(file);
   }

   @Override
   @Nullable
   public String getContents() {
      return contents;
   }

   @Override
   public void addListener(FileContentListener listener) {
      listeners.add(listener);
   }

   @Override
   public void removeListener(FileContentListener listener) {
      listeners.remove(listener);
   }

   @Override
   public void cancel() {
      handle.cancel();
   }

   private void reload(File file) {
      String previous = contents;

      try (InputStream is = StorageService.getInputStream(file)) {
         this.contents = IOUtils.toString(is, StandardCharsets.UTF_8);
      } catch (IOException ex) {
         this.contents = null;
      }

      boolean changed = (previous != null && !previous.equals(this.contents)) ||
                        (contents != null && !contents.equals(previous));
      if (changed) {
         for (FileContentListener listener : listeners) {
            listener.fileContentsModified(this);
         }
      }
   }

   @Override
   public void onDirectoryCreate(File directory) {
   }

   @Override
   public void onDirectoryChange(File directory) {
   }

   @Override
   public void onDirectoryDelete(File directory) {
   }

   @Override
   public void onFileCreate(File file) {
      reload(file);
   }

   @Override
   public void onFileChange(File file) {
      reload(file);
   }

   @Override
   public void onFileDelete(File file) {
      reload(file);
   }
}

