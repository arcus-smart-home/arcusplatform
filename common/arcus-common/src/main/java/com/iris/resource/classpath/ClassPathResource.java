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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.resource.AbstractResource;
import com.iris.resource.Resource;
import com.iris.resource.ResourceNotFoundException;

/**
 * 
 */
class ClassPathResource extends AbstractResource {

   private static final Logger logger = LoggerFactory.getLogger(ClassPathResource.class);
   private final ClassLoader loader;
   private final String path;

   ClassPathResource(ClassLoader loader, String path) throws URISyntaxException {
      super(new URI(ClassPathResourceFactory.SCHEME, '/' + path, null));
      this.loader = loader;
      this.path = path;
   }

   @Override
   public boolean exists() {
      return loader.getResource(path) != null;
   }

   @Override
   public boolean isReadable() {
      return exists();
   }

   @Override
   public boolean isFile() {
      Path fsPath = getNioPath();
      return fsPath != null && Files.isRegularFile(fsPath);
   }

   @Override
   public boolean isDirectory() {
      Path fsPath = getNioPath();
      return fsPath != null && Files.isDirectory(fsPath);
   }

   private Path getNioPath() {
      URL url = this.loader.getResource(this.path);
      Path fsPath = null;

      if (url != null) {
         try {
            URI uri = url.toURI();
            fsPath = Paths.get(uri);
         }
         catch (URISyntaxException e) {
            logger.trace("The Class Loader failed to find [{}]", this.path);
         }
      }

      return fsPath;
   }

   @Override
   public InputStream open() throws IOException {
      InputStream is = null;
      if (isFile()) {
         is = loader.getResourceAsStream(path);
      }

      if (is == null) {
         throw new ResourceNotFoundException(getUri());
      }

      return is;
   }

   @Override
   public File getFile() {
      Path fsPath = getNioPath();
      if (fsPath != null) {
         if (Files.isRegularFile(fsPath) || Files.isDirectory(fsPath)) {
            return fsPath.toFile();
         }
      }
      return null;
   }

   @Override
   public List<Resource> listResources() {
      ImmutableList.Builder<Resource> bldr = ImmutableList.builder();
      File dir = getFile();

      if (dir != null && dir.isDirectory()) {
         String[] files = dir.list();
         for (String file : files) {
            try {
               ClassPathResource resource = new ClassPathResource(this.loader, this.path + file);
               bldr.add(resource);
            }
            catch (URISyntaxException e) {
               logger.trace("The Class Loader failed to find [{}] in directory [{}]", this.path + file, this.path);
            }
         }
      }

      return bldr.build();
   }
}

