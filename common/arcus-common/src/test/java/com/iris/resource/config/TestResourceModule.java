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
package com.iris.resource.config;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.junit.After;
import org.junit.Test;

import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.BootstrapException;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

/**
 *
 */
public class TestResourceModule {

   public void init(final String root) throws BootstrapException {
      ResourceModule rm = new ResourceModule() {

         @Override
         protected String getRootDirectory() {
            return root;
         }

      };
      Bootstrap bootstrap =
            Bootstrap
               .builder()
               .withModules(rm)
               .build()
               ;
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
   }

   @After
   public void tearDown() {
      ServiceLocator.destroy();
   }

   @Test
   public void testDefaultFileSystemRelativeFile() throws Exception {
      init("");
      Resource resource = Resources.getResource("src/test/resources/test.file");
      assertNotNull(resource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());

      URI expected = new File("src/test/resources/test.file").toURI();
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNotNull(resource.getFile());

      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }

   }

   @Test
   public void testFileSystemRelativeFile() throws Exception {
      init("src/test/resources");
      Resource resource = Resources.getResource("test.file");
      assertNotNull(resource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());

      URI expected = new File("src/test/resources/test.file").toURI();
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNotNull(resource.getFile());

      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }

   }

   @Test
   public void testFileSystemAbsoluteFile() throws Exception {
      init("");
      Resource resource = Resources.getResource("file:" + new File("src/test/resources/test.file").getAbsolutePath());
      assertNotNull(resource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());

      URI expected = new File("src/test/resources/test.file").toURI();
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNotNull(resource.getFile());

      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }

   }

   @Test
   public void testClasspathFile() throws Exception {
      init("");
      Resource resource = Resources.getResource("classpath:/test.file");
      assertNotNull(resource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());

      URI expected = new URI("classpath:/test.file");
      assertEquals(expected.toString(), resource.getRepresentation());
      assertEquals(expected, resource.getUri());
      assertNotNull(resource.getFile());

      try(InputStream is = resource.open()) {
         assertEquals("Used by TestFileSystemResource", read(is));
      }

   }

   @Test
   public void UnknownScheme() throws Exception {
      init("");
      try {
         Resources.getResource("scheme:/test.file");
         fail();
      }
      catch(IllegalArgumentException e) {
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

