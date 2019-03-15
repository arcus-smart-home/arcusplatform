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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.iris.resource.Resource;
import com.iris.resource.ResourceNotFoundException;

/**
 * 
 */
public class TestClassPathResource {
   ClassPathResourceFactory factory;
   
   @Before
   public void setUp() {
      factory = new ClassPathResourceFactory(TestClassPathResource.class);
   }
   
   @Test
   public void testAbsoluteClasspath() throws Exception {
      URI uri = new URI("classpath:/test.file");
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof ClassPathResource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());

      assertTrue(resource.isFile());
      assertFalse(resource.isWatchable());
      
      assertEquals(uri.toString(), resource.getRepresentation());
      assertEquals(uri, resource.getUri());
      assertNotNull(resource.getFile());
      assertTrue(resource.getFile().exists());
      assertTrue(resource.getFile().canRead());
      assertFalse(resource.isDirectory());
      
      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }
   }

   @Test
   public void testRelativeClasspath() throws Exception {
      URI uri = new URI("test.file");
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof ClassPathResource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());

      assertTrue(resource.isFile());
      assertFalse(resource.isWatchable());
      
      URI expected = new URI("classpath:/com/iris/resource/classpath/test.file");
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNotNull(resource.getFile());
      assertTrue(resource.getFile().exists());
      assertTrue(resource.getFile().canRead());
      assertFalse(resource.isDirectory());
      
      try(InputStream is = resource.open()) {
         assertEquals("Used by TestClassPathResource", read(is));
      }
   }
   
   @Test
   public void testClasspathDirectory() throws Exception {
      URI uri = new URI("classpath:/folder/");
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof ClassPathResource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());

      assertFalse(resource.isFile());
      assertFalse(resource.isWatchable());
      assertTrue(resource.isDirectory());

      assertEquals(uri.toString(), resource.getRepresentation());
      assertEquals(uri, resource.getUri());

      File dir = resource.getFile();
      assertNotNull(dir);
      assertTrue(dir.exists());
      assertTrue(dir.canRead());

      List<Resource> files = resource.listResources();
      assertNotNull(files);
      assertEquals(1, files.size());
      assertTrue(files.get(0).isFile());
      try (InputStream is = files.get(0).open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }
   }

   @Test
   public void testNonExistentFile() throws Exception {
      URI uri = new URI("noSuchFile");
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof ClassPathResource);
      assertFalse(resource.exists());
      // can't read a non-existent file
      assertFalse(resource.isReadable());
      // it isn't a file if it doesn't exist yet
      assertFalse(resource.isFile());
      assertFalse(resource.isWatchable());
      
      URI expected = new URI("classpath:/com/iris/resource/classpath/").resolve(uri);
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNull(resource.getFile());
      
      try(InputStream is = resource.open()) {
         fail("Opened a non-existent file");
      }
      catch(ResourceNotFoundException e) {
         // expected
      }
   }

   private String read(InputStream is) throws IOException {
      byte [] bytes = new byte[1024];
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int len = is.read(bytes);
      while(len > -1) {
         if(len > 0) {
            baos.write(bytes, 0, len);
         }
         len = is.read(bytes);
      }
      return baos.toString();
   }

}

