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
package com.iris.resource.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.resource.Resource;
import com.iris.resource.ResourceListener;

public class TestFileWatcher {
   private final static String TEST_DATA = "4815162342";
   private FileSystemResourceFactory factory;
   private File tempFile;
   
   @Before
   public void setUp() throws Exception {
      tempFile = File.createTempFile("foo", ".txt");
      // Just in case tearDown isn't call.
      tempFile.deleteOnExit();
      factory = new FileSystemResourceFactory(tempFile.getParent());
   }
   
   @After
   public void tearDown() throws Exception {
      tempFile.delete();
   }
   
   @Test
   public void writeToFile() throws Exception {
      writeLineToFile(TEST_DATA);
      
      try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
         System.out.println(reader.readLine());
      }
   }
   
   @Test
   public void testWatchFileAbsolutePath() throws Exception {
      URI uri = new URI("file:" + tempFile.getAbsolutePath());
      Resource resource = factory.create(uri);
      
      verifyResource(resource);
      final CountDownLatch latch = new CountDownLatch(1);
      resource.addWatch(new ResourceListener() {
         @Override
         public void onChange() {
            latch.countDown();
         }        
      });
      // Give the file watcher a second to get started.
      Thread.sleep(1000);
      
      writeLineToFile(TEST_DATA);
      // It can take a few seconds for the event to get fired.
      boolean changeDetected = latch.await(20, TimeUnit.SECONDS);    
      assertTrue(changeDetected);
      
      try(BufferedReader reader = new BufferedReader(new InputStreamReader( resource.open()))) {
         assertEquals(TEST_DATA, reader.readLine());
      }
   }
   
   @Test
   public void testWatchFileRelativePath() throws Exception {
      URI uri = new URI(tempFile.getName());
      Resource resource = factory.create(uri);
      
      verifyResource(resource);
      final CountDownLatch latch = new CountDownLatch(1);
      resource.addWatch(new ResourceListener() {
         @Override
         public void onChange() {
            latch.countDown();
         }        
      });
      // Give the file watcher a second to get started.
      Thread.sleep(1000);
      
      writeLineToFile(TEST_DATA);
      // It can take a few seconds for the event to get fired.
      boolean changeDetected = latch.await(20, TimeUnit.SECONDS);    
      assertTrue(changeDetected);
      
      try(BufferedReader reader = new BufferedReader(new InputStreamReader( resource.open()))) {
         assertEquals(TEST_DATA, reader.readLine());
      }
   }
   
   private void writeLineToFile(String data) throws Exception {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
         writer.write(TEST_DATA);
      }
   }
   
   private void verifyResource(Resource resource) {
      assertNotNull(resource);
      assertTrue(resource instanceof FileSystemResource);
      assertTrue(resource.exists());
      assertTrue(resource.isReadable());
      assertTrue(resource.isFile());
      assertTrue(resource.isWatchable());
   }
}

