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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.iris.resource.Resource;
import com.iris.resource.ResourceNotFoundException;
import com.iris.resource.filesystem.FileSystemResource;
import com.iris.resource.filesystem.FileSystemResourceFactory;

/**
 * 
 */
public class TestFileSystemResource {
   FileSystemResourceFactory factory;
   File testFolder = new File("src/test/resources");
   
   @Before
   public void setUp() {
      factory = new FileSystemResourceFactory(testFolder.getPath());
   }
   
   @Test
   public void testAbsoluteFile() throws Exception {
      File f = new File(testFolder, "test.file");
      URI uri = new URI("file:" + f.getAbsolutePath());
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof FileSystemResource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());
      assertTrue(resource.isWatchable());
      
      assertEquals(uri.toString(), resource.getRepresentation());
      assertEquals(uri, resource.getUri());
      assertEquals(f.getAbsolutePath(), resource.getFile().getAbsolutePath());
      
      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }
   }

   @Test
   public void testRelativeFile() throws Exception {
      URI uri = new URI("test.file");
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof FileSystemResource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());
      assertTrue(resource.isWatchable());
      
      URI expected = testFolder.toURI().resolve(uri);
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNotNull(resource.getFile());
      
      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }
   }

   @Test
   public void testRelativeFileWithProtocol() throws Exception {
      URI uri = new URI("file:test.file");
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof FileSystemResource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());
      assertTrue(resource.isWatchable());
      
      URI expected = testFolder.toURI().resolve("test.file");
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNotNull(resource.getFile());
      
      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }
   }

   @Test
   public void testDirectory() throws Exception {
      URI uri = new URI("folder/");
      Resource resource = factory.create(uri);
      assertNotNull(resource);
      assertTrue(resource instanceof FileSystemResource);
      assertTrue(resource.exists());
      // can't read a directory
      assertFalse(resource.isReadable());
      assertFalse(resource.isFile());
      assertFalse(resource.isWatchable());
      
      URI expected = testFolder.toURI().resolve(uri);
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNull(resource.getFile());
      
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
      assertTrue(resource instanceof FileSystemResource);
      assertFalse(resource.exists());
      // can't read a non-existent file
      assertFalse(resource.isReadable());
      // it isn't a file if it doesn't exist yet
      assertFalse(resource.isFile());
      assertFalse(resource.isWatchable());
      
      URI expected = testFolder.toURI().resolve(uri);
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

