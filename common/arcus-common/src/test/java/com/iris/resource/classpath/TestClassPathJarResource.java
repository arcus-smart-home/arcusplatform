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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.junit.Test;

import com.iris.resource.Resource;
import com.iris.resource.ResourceNotFoundException;

public class TestClassPathJarResource {

   @Test
   public void testJarClasspath() throws Exception {
      String path = "java/lang/String.class";
      ClassLoader loader = TestClassPathResource.class.getClassLoader();
      Resource resource = new ClassPathJarResource(loader, path);

      assertNotNull(resource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());

      assertTrue(resource.isFile());
      assertNull(resource.getFile());
      assertFalse(resource.isDirectory());
      assertFalse(resource.isWatchable());

      URI uri = new URI(ClassPathResourceFactory.SCHEME, '/' + path, null);
      assertEquals(uri.toString(), resource.getRepresentation());
      assertEquals(uri, resource.getUri());

      try (InputStream is = resource.open()) {
         String content = read(is);
         assertTrue(content.contains("equalsIgnoreCase"));
      }
   }
   
   @Test
   public void testJarClasspathDirectory() throws Exception {
      String path = "java/lang/String.class";
      ClassLoader loader = TestClassPathResource.class.getClassLoader();
      // The class loader won't find a directory inside a jar, but we can build a path to one.
      path = loader.getResource(path).toString();
      path = path.substring(0, path.lastIndexOf("/") + 1); // The full path contains the java version which will change from machine to machine.  Looking up String.class will always give us the right location.
      Resource resource = new ClassPathJarResource(loader, path);

      assertNotNull(resource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());

      assertFalse(resource.isFile());
      assertNull(resource.getFile());
      assertTrue(resource.isDirectory());
      assertFalse(resource.isWatchable());

      URI uri = new URI(ClassPathResourceFactory.SCHEME, '/' + path, null);
      assertEquals(uri.toString(), resource.getRepresentation());
      assertEquals(uri, resource.getUri());

      List<Resource> files = resource.listResources();
      assertNotNull(files);
      assertTrue(files.size() > 0);

      Resource file = files.get(1);
      try (InputStream is = file.open()) {
         assertNotNull(read(is));
      }
   }
   
   @Test
   public void testNonExistentFile() throws Exception {
      String path = "java/lang/String.class";
      ClassLoader loader = TestClassPathResource.class.getClassLoader();
      path = loader.getResource(path).toString();
      path += "zz"; // The class loader won't find a file that doesn't exist, but we can build a path to one. 
      Resource resource = new ClassPathJarResource(loader, path);
      
      assertNotNull(resource);
      assertTrue(resource instanceof ClassPathJarResource);
      assertFalse(resource.exists());
      // can't read a non-existent file
      assertFalse(resource.isReadable());
      // it isn't a file if it doesn't exist yet
      assertFalse(resource.isFile());
      assertNull(resource.getFile());
      assertFalse(resource.isWatchable());
      
      URI uri = new URI(ClassPathResourceFactory.SCHEME, '/' + path, null);
      assertEquals(uri.toString(), resource.getRepresentation());
      assertEquals(uri, resource.getUri());
      
      try(InputStream is = resource.open()) {
         fail("Opened a non-existent file");
      }
      catch(ResourceNotFoundException e) {
         // expected
      }
   }

   private String read(InputStream is) throws IOException {
      byte[] bytes = new byte[1024];
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int len = is.read(bytes);
      while (len > -1) {
         if (len > 0) {
            baos.write(bytes, 0, len);
         }
         len = is.read(bytes);
      }
      return baos.toString();
   }
}

