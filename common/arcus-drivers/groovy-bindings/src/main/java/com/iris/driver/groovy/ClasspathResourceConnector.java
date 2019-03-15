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
package com.iris.driver.groovy;

import groovy.util.ResourceConnector;
import groovy.util.ResourceException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class ClasspathResourceConnector implements ResourceConnector {
   private final ClassLoader loader;
   private final String directory;

   public ClasspathResourceConnector() {
      this(null, (String) null);
   }

   public ClasspathResourceConnector(Class<?> relativeTo) {
      this(relativeTo.getClassLoader(), relativeTo);
   }

   public ClasspathResourceConnector(ClassLoader loader) {
      this(loader, (String) null);
   }

   public ClasspathResourceConnector(ClassLoader loader, Class<?> relativeTo) {
      this(loader, relativeTo.getPackage().getName().replace('.', '/') + '/');
   }

   protected ClasspathResourceConnector(ClassLoader loader, String directory) {
      this.loader = loader != null ? loader: getClass().getClassLoader();
      this.directory = directory;
   }

   /* (non-Javadoc)
    * @see groovy.util.ResourceConnector#getResourceConnection(java.lang.String)
    */
   @Override
   public URLConnection getResourceConnection(String name) throws ResourceException {
      try {
         if(this.directory != null) {
            name = this.directory + name;
         }

         URL conn = loader.getResource(name);
         if(conn == null) {
            if (name.startsWith("file:")) {
               URI fileUri = URI.create(name);
               File file = new File(fileUri);
               if (file.isFile()) {
                  conn = fileUri.toURL();
                  return conn.openConnection();
               }
            }
            throw new ResourceException("Resource not found [classpath:/" + name + "]");
         }
         return conn.openConnection();
      }
      catch (IOException e) {
         throw new ResourceException("Unable to open classpath:/" + name, e);
      }
   }

}

