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
package com.iris.modelmanager.resource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HomeDirectoryResourceLocator implements ResourceLocator {

   private final File homeDirectory;

   public HomeDirectoryResourceLocator(File homeDirectory) {
      this.homeDirectory = homeDirectory;
   }

   @Override
   public Collection<URL> listDirectory(String directory) throws IOException {
      File dir = new File(this.homeDirectory, directory);
      if(!dir.exists()) {
         return Collections.emptyList();
      }
      if(dir.isDirectory()) {
         File[] files = dir.listFiles();
         List<URL> urls = new ArrayList<>(files.length);
         for(File f : files) {
            urls.add(f.toURI().toURL());
         }
         return urls;
      }
      return Collections.singletonList(dir.toURI().toURL());
   }

   @Override
   public URL locate(String directory, String name) throws MalformedURLException {
      File file = new File(new File(this.homeDirectory, directory), name);
      return file.toURI().toURL();
   }
}

