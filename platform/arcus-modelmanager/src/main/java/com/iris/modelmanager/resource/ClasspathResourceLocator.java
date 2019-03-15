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

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathResourceLocator implements ResourceLocator {

   @Override
   public Collection<URL> listDirectory(String directory) throws IOException {
      URL u = getClass().getClassLoader().getResource(directory);
      if(u == null) {
         return Collections.<URL>emptyList();
      }
      JarURLConnection jarConn = (JarURLConnection) u.openConnection();
      return listFromJar(jarConn.getJarFile(), directory);
   }

   private Collection<URL> listFromJar(JarFile jar, String directory) {
      List<URL> urls = new ArrayList<>();
      Enumeration<JarEntry> entries = jar.entries();
      while(entries.hasMoreElements()) {
         JarEntry entry = entries.nextElement();
         String[] parts = entry.getName().split("/");
         if(parts[0].equals(directory) && parts.length == 2 && parts[1].length() > 0) {
            urls.add(getClass().getClassLoader().getResource(entry.getName()));
         }
      }
      return urls;
   }

   @Override
   public URL locate(String directory, String name) throws MalformedURLException {
      return getClass().getClassLoader().getResource(directory + "/" + name);
   }
}

