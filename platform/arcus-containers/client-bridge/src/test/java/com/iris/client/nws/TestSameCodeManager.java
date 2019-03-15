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
package com.iris.client.nws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.iris.resource.Resource;
import com.iris.resource.ResourceListener;
import com.iris.resource.filesystem.FileSystemResourceFactory;
import com.iris.test.util.TestUtils;

/*
 * Making Sure that tests are completed in a given order as we
 * update watched files in some tests that can affect assumptions
 */
public class TestSameCodeManager {

   //private static final Logger LOGGER = LoggerFactory.getLogger(TestSameCodeManager.class);

   /*
    * NOTE - We want these entries to be UNSORTED to Test the sort routines on
    * insert
    */
   private final static String[] TEST_SAMECODE_DATA = { "006105,Trinity, CA", "001001,Autauga, AL",
         "026113,Missaukee, MI" };

   /*
    * NOTE - We want these entries to be UNSORTED to Test the sort routines on
    * insert
    */
   private final static String[] TEST_SATE_MAPPING_DATA = { "MI,Michigan", "AL,Alabama", "CA,California" };

   private SameCodeManager sameCodeManager;

   private FileSystemResourceFactory factory;

   private File _SAMECodeFile;
   private Resource _SAMECodeResource;

   private File stateMappingFile;
   private Resource stateMappingResource;

   @Before
   public void setUp() throws Exception {
      // CREATE TEMP FILES FOR TEST THAT SHAPE DATA
      _SAMECodeFile = File.createTempFile("temp_SameCode", ".txt");
      _SAMECodeFile.deleteOnExit();

      stateMappingFile = File.createTempFile("temp_samecode_statename_mappings", ".csv");
      stateMappingFile.deleteOnExit();

      TestUtils.appendLinesToFile(_SAMECodeFile, TEST_SAMECODE_DATA);
      TestUtils.appendLinesToFile(stateMappingFile, TEST_SATE_MAPPING_DATA);

      factory = new FileSystemResourceFactory(stateMappingFile.getParent());

      _SAMECodeResource = factory.create(new URI("file:" + _SAMECodeFile.getAbsolutePath()));

      stateMappingResource = factory.create(new URI("file:" + stateMappingFile.getAbsolutePath()));

      sameCodeManager = new SameCodeManager(_SAMECodeResource, stateMappingResource);
   }

   @Test
   public void testLoadSAMECodesAndExpectSizes() throws Exception {
      assertEquals(sameCodeManager.listSameCodes().size(), TEST_SAMECODE_DATA.length);
      assertEquals(sameCodeManager.listSameStates().size(), TEST_SATE_MAPPING_DATA.length);
   }

   @Test
   public void testWatchedSAMECodeFileAndExpectSizes() throws Exception {
      final CountDownLatch latch = new CountDownLatch(1);
      _SAMECodeResource.addWatch(new ResourceListener() {
         @Override
         public void onChange() {
            latch.countDown();
         }
      });
      // Give the file watcher a second to get started.
      Thread.sleep(1000);

      /*
       * write a new code to the codes file and see if it is picked up by the
       * manager
       */
      String[] inputData = { "006059,Orange, CA" };

      SameCode sameCode = new SameCode("006059","Orange","CA", "California");
      
      TestUtils.appendLinesToFile(_SAMECodeFile, inputData);

      // It can take a few seconds for the event to get fired.
      boolean changeDetected = latch.await(20, TimeUnit.SECONDS);
      assertTrue(changeDetected);

      // test to make sure that the new data was picked up by the manager
      List<SameCode> sameCodes = sameCodeManager.listSameCodes();
      assertTrue(sameCodes.contains(sameCode));
   }

   @Test
   public void testWatchedSAMECodeFileAndExpectSizesForNewState() throws Exception {
      // Watch both resources
      final CountDownLatch latch1 = new CountDownLatch(1);
      _SAMECodeResource.addWatch(new ResourceListener() {
         @Override
         public void onChange() {
            latch1.countDown();
         }
      });

      final CountDownLatch latch2 = new CountDownLatch(1);
      stateMappingResource.addWatch(new ResourceListener() {
         @Override
         public void onChange() {
            latch2.countDown();
         }
      });

      // Give the file watchers a second to get started.
      Thread.sleep(1000);

      
      /*
       * write a new code to the codes file and see if it is picked up by the
       * manager
       */
      String[] inputData = { "029151,Osage, MO" };
      
      SameCode sameCode = new SameCode("029151","Osage","MO", "Missouri");

      TestUtils.appendLinesToFile(_SAMECodeFile, inputData);

      // It can take a few seconds for the event to get fired.
      boolean changeDetected = latch1.await(20, TimeUnit.SECONDS);
      assertTrue(changeDetected);

      /*
       * test to make sure that the new data was rejected up by the manager
       * because the State doesn't exist yet
       */
      assertFalse(sameCodeManager.listSameCodes().contains(sameCode));

      /*
       * Try Again, this time inserting the state
       * 
       * write a new code to the state mapping file and see if it is picked up
       * by the manager
       */
      String[] stateInputData = { "MO,Missouri"};

      TestUtils.appendLinesToFile(stateMappingFile, stateInputData);

      /*
       * This should trigger an update and the previously entered SAME code
       * should get picked up
       */

      changeDetected = latch2.await(30, TimeUnit.SECONDS);
      assertTrue(changeDetected);
      assertTrue(sameCodeManager.listSameCodes().contains(sameCode));
   }

   /*
    * Left in for the convenience of developers that may modify the test case
    * debug should be no-op on build server
    */
/*   private void printFile(File file) throws Exception {
      try (BufferedReader br = new BufferedReader(new FileReader(file))){
         String line = null;
         while ((line = br.readLine()) != null){
            LOGGER.trace(line);
         }
      }
   }
   */

}

