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
package com.iris.resource.classpath;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.iris.resource.Resource;
import com.iris.resource.ResourceFactory;

/**
 * 
 */
public class ClassPathResourceFactory implements ResourceFactory {
   public static final String SCHEME = "classpath";
   
   private final ClassLoader loader;
   private final String rootPath;
   
   /**
    * 
    */
   public ClassPathResourceFactory() {
      this(ClassPathResourceFactory.class.getClassLoader(), "com.iris");
   }
   
   public ClassPathResourceFactory(Class<?> relativeTo) {
      this(relativeTo.getClassLoader(), relativeTo.getPackage().getName());
   }
   
   public ClassPathResourceFactory(ClassLoader loader) {
      this(loader, "com.iris");
   }
   
   public ClassPathResourceFactory(ClassLoader loader, String packageName) {
      this.loader = loader;
      this.rootPath = packageName.replace('.', '/') + '/';
   }

   @Override
   public String getScheme() {
      return SCHEME;
   }

   @Override
   public Resource create(URI uri) throws IllegalArgumentException {
      String path = uri.getPath();
      if(path.charAt(0) == '/') {
         path = path.substring(1);
      }
      else {
         path = rootPath + path;
      }
      try {
         // Most of the time resources are in a jar
         if (isJar(path)) {
            return new ClassPathJarResource(loader, path);
         }
         else { // except in JUnit tests.  Then they are almost always regular files
            return new ClassPathResource(loader, path);
         }
      }
      catch(URISyntaxException e) {
         throw new IllegalArgumentException("Invalid URI path: " + path, e);
      }
   }

   private boolean isJar(String path) {
      URL url = this.loader.getResource(path);

      if (url != null && url.toString().startsWith("jar")) {
         return true;
      }

      return false;
   }

}

