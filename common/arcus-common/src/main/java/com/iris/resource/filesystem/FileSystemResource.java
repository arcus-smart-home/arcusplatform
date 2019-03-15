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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.collect.ImmutableList;
import com.iris.resource.AbstractResource;
import com.iris.resource.Resource;
import com.iris.resource.ResourceListener;
import com.iris.resource.ResourceNotFoundException;
import com.iris.util.Subscription;

/**
 * 
 */
class FileSystemResource extends AbstractResource {
   private final File file;
   private final FileWatcherRegistry watcher;
   
   FileSystemResource(File file, FileWatcherRegistry watcher) {
      super(file.toURI());
      this.file = file;
      this.watcher = watcher;
   }

   @Override
   public boolean exists() {
      return file.exists();
   }

   @Override
   public boolean isReadable() {
      return file.isFile() && file.canRead();
   }

   @Override
   public boolean isFile() {
       // NOTE - a directory is not a file
       // Better to return false than throw a Security Exception
       return file.exists() && file.canRead() && file.isFile();
   }
   
   @Override
   public boolean isDirectory() {
      // Better to return false than throw a Security Exception
      return file.exists() && file.canRead() && file.isDirectory();
   }

   @Override
   public File getFile() {
      return isFile() ? file : null;
   }

   @Override
   public InputStream open() throws IOException {
      if(!exists()) {
         throw new ResourceNotFoundException("No file named [" + getUri() + "] exists");
      }
      if(!isFile()) {
         throw new ResourceNotFoundException("File named [" + getUri() + "] is a directory, can't be opened");
      }
      return super.open();
   }
   
   @Override
   public boolean isWatchable() {
      return isFile();
   }

   @Override
   public Subscription addWatch(ResourceListener listener) {
      try {
         return watcher.watch(file, listener);
      }
      catch (IOException e) {
         throw new UncheckedExecutionException("Unable to watch file: " + file, e);
      }
   }
   
   @Override
   public List<Resource> listResources() {
      ImmutableList.Builder<Resource> bldr = ImmutableList.builder();

      if (isDirectory()) {
         File[] files = this.file.listFiles();

         for (File file : files) {
            FileSystemResource resource = new FileSystemResource(file, this.watcher);
            bldr.add(resource);
         }
      }

      return bldr.build();
   }

   @Override
   public void dispose() {
      // TODO track watch subscriptions and cancel them all out here?
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((file == null) ? 0 : file.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      FileSystemResource other = (FileSystemResource) obj;
      if (file == null) {
         if (other.file != null) return false;
      }
      else if (!file.equals(other.file)) return false;
      return true;
   }
}

