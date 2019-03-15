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
package com.iris.resource.classpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.resource.AbstractResource;
import com.iris.resource.Resource;
import com.iris.resource.ResourceNotFoundException;

/**
 * A resource that exists inside a jar has different restrictions than a normal file. 
 * ClassPathJarResource puts a candy coating over the resource so it can be used in the same way as  
 * a regular file when returned by ClassPathResourceFactory.create
 */
public class ClassPathJarResource extends AbstractResource {

   private static final Logger logger = LoggerFactory.getLogger(ClassPathJarResource.class);
   private final ClassLoader loader;
   private final String path;
   private URI jarRoot; // where the zip file system will start
   private String jarPath; // the path relative to the zip

   ClassPathJarResource(ClassLoader clazzLoader, String rezPath) throws URISyntaxException {
      super(new URI(ClassPathResourceFactory.SCHEME, '/' + rezPath, null));
      this.loader = clazzLoader;
      this.path = rezPath;
      parseRoot();
   }

   private void parseRoot() throws URISyntaxException {
      URL url = this.loader.getResource(this.path);

      // The class loader won't find a directory inside a jar, but it can be specified
      if (url == null) {
         try {
            url = new URL(this.path);
         }
         catch (MalformedURLException e) {
            URISyntaxException ex = new URISyntaxException("Supplied path is malformed", this.path);
            ex.initCause(e);
            throw ex;
         }
      }

      if (url != null && url.toString().startsWith("jar")) {
         String strUrl = url.toString();
         int index = strUrl.lastIndexOf("!");

         if (index != -1 && index != strUrl.length() - 1) {
            String[] zipPath = { strUrl.substring(0, index), strUrl.substring(index + 1) };
            this.jarRoot = new URI(zipPath[0]);
            this.jarPath = zipPath[1];
         }
      }

      if (this.jarPath == null || this.jarRoot == null) {
         throw new URISyntaxException("Supplied path is not a jar resource", this.path);
      }
   }

   @Override
   public boolean exists() {
      boolean rtn = false;
      try (FileSystem fileSystem = FileSystems.newFileSystem(this.jarRoot, Collections.<String, Object> emptyMap())) {
         Path fsPath = fileSystem.getPath(this.jarPath);
         rtn = fsPath != null && Files.exists(fsPath);
      }
      catch (IOException e) {
         logger.warn("Failed to close jar filesystem root [{}] during exists check of [{}]", this.jarRoot, this.path, e);
      }

      return rtn;
   }

   @Override
   public boolean isReadable() {
      boolean rtn = false;
      try (FileSystem fileSystem = FileSystems.newFileSystem(this.jarRoot, Collections.<String, Object> emptyMap())) {
         Path fsPath = fileSystem.getPath(this.jarPath);
         rtn = fsPath != null && Files.isReadable(fsPath);
      }
      catch (IOException e) {
         logger.warn("Failed to close jar filesystem root [{}] during isReadable check of [{}]", this.jarRoot, this.path, e);
      }

      return rtn;
   }

   @Override
   public boolean isFile() {
      boolean rtn = false;
      try (FileSystem fileSystem = FileSystems.newFileSystem(this.jarRoot, Collections.<String, Object> emptyMap())) {
         Path fsPath = fileSystem.getPath(this.jarPath);
         rtn = fsPath != null && Files.isRegularFile(fsPath);
      }
      catch (IOException e) {
         logger.warn("Failed to close jar filesystem root [{}] during isFile check of [{}]", this.jarRoot, this.path, e);
      }

      return rtn;
   }

   @Override
   public boolean isDirectory() {
      boolean rtn = false;
      try (FileSystem fileSystem = FileSystems.newFileSystem(this.jarRoot, Collections.<String, Object> emptyMap())) {
         Path fsPath = fileSystem.getPath(this.jarPath);
         rtn = fsPath != null && Files.isDirectory(fsPath);
      }
      catch (IOException e) {
         logger.warn("Failed to close jar filesystem root [{}] during isDirectory check of [{}]", this.jarRoot, this.path, e);
      }

      return rtn;
   }

   @Override
   public List<Resource> listResources() {
      ImmutableList.Builder<Resource> bldr = ImmutableList.builder();

      try (FileSystem fileSystem = FileSystems.newFileSystem(this.jarRoot, Collections.<String, Object> emptyMap())) { // create zip file system
         Path fsPath = fileSystem.getPath(this.jarPath);
         if (fsPath != null && Files.isDirectory(fsPath)) { // ensure it's a directory inside the zip
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(fsPath)) { // java 7 method of streaming
               for (Path file : stream) {
                  try {
                     ClassPathJarResource resource;
                     if (Files.isDirectory(file)) {
                        resource = new ClassPathJarResource(this.loader, this.jarRoot.toString() + "!" + file.toString()); // has to be built explicitly if it's a directory.  getResource doens't work with directories.
                     }
                     else {
                        String rezPath = file.toString();
                        // The Resource Loader can't find it if it starts with a '/'
                        if (rezPath.charAt(0) == '/') {
                           rezPath = rezPath.substring(1);
                        }
                        resource = new ClassPathJarResource(this.loader, rezPath); // Non-Directories can be looked up without their root
                     }
                     bldr.add(resource);
                  }
                  catch (URISyntaxException e) {
                     logger.debug("The Class Loader failed to find [{}] in the jar [{}]", file.toString(), this.path, e);
                  }
               }
            }
         }
      }
      catch (IOException e) {
         logger.warn("Failed to close jar filesystem root [{}] during listFiles of [{}]", this.jarRoot, this.path, e);
      }

      return bldr.build();
   }

   @Override
   public File getFile() {
      return null;
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
}

