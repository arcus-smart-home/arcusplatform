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
package com.iris.client.eas;

import static org.junit.Assert.assertEquals;
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
public class TestEasCodeManager {

   // private static final Logger LOGGER =
   // LoggerFactory.getLogger(TestSameCodeManager.class);

   private static final String eas1 = "SVR";
   private static final String name1 = "Severe Thunderstorm Warning";
   private static final String group1 = "Popular";

   private static final String eas2 = "EWW";
   private static final String name2 = "Extreme Wind Warning";
   private static final String group2 = "Other";

   // ??E Unrecognized Emergency Other
   private static final String eas3 = "??E";
   private static final String name3 = "Unrecognized Emergency";
   private static final String group3 = "Other";

   private static final String eas4 = "CFW";
   private static final String name4 = "Coastal Flood Warning";
   private static final String group4 = "Other";
   /*
    * /* NOTE - We want these entries to be UNSORTED to Test the sort routines
    * on insert
    */

   private final static String[] TEST_EAS_EVENT_CODE_DATA = {
         String.format("%s   %s  %s", eas1, name1, group1),
         String.format("%s   %s  %s", eas2, name2, group2),
         String.format("%s   %s  %s", eas3, name3, group3) };

   private EasCodeManager easCodeManager;

   private FileSystemResourceFactory factory;

   private File _EASCodeFile;
   private Resource _EASCodeResource;

   @Before
   public void setUp() throws Exception {
      // create test input data file
      _EASCodeFile = File.createTempFile("temp_EasCode", ".txt");
      _EASCodeFile.deleteOnExit();

      TestUtils.appendLinesToFile(_EASCodeFile, TEST_EAS_EVENT_CODE_DATA);

      factory = new FileSystemResourceFactory(_EASCodeFile.getParent());

      _EASCodeResource = factory.create(new URI("file:" + _EASCodeFile.getAbsolutePath()));

      easCodeManager = new EasCodeManager(_EASCodeResource);
   }

   @Test
   public void testLoadEASCodesAndExpectSizes() throws Exception {
      assertEquals(easCodeManager.listEasCodes().size(), TEST_EAS_EVENT_CODE_DATA.length);

      /* Verify that the values are parsed correctly */
      assertValid(easCodeManager.listEasCodes());
   }

   @Test
   public void testWatchedEASCodeFileAndExpectSizes() throws Exception {
      final CountDownLatch latch = new CountDownLatch(1);
      _EASCodeResource.addWatch(new ResourceListener() {
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
      String[] inputData = { String.format("%s   %s   %s", eas4, name4, group4) };

      TestUtils.appendLinesToFile(_EASCodeFile, inputData);

      // It can take a few seconds for the event to get fired.
      boolean changeDetected = latch.await(20, TimeUnit.SECONDS);
      assertTrue(changeDetected);

      // test to make sure that the new data was picked up by the manager
      List<EasCode> easCodes = easCodeManager.listEasCodes();
      assertTrue(easCodes.contains(EasCodeResourceLoader.stringToEas(inputData[0])));

      /*
       * Verify that the values are parsed correctly and the new EAS Code is
       * available
       */
      assertValid(easCodeManager.listEasCodes());
   }

   private void assertValid(List<EasCode> easCodes) {
      for (EasCode code : easCodeManager.listEasCodes()){

         switch (code.getEas()) {
         case eas1:
            assertTrue(name1.equals(code.getName()) && group1.equals(code.getGroup()));
            break;
         case eas2:
            assertTrue(name2.equals(code.getName()) && group2.equals(code.getGroup()));
            break;
         case eas3:
            assertTrue(name3.equals(code.getName()) && group3.equals(code.getGroup()));
            break;
         case eas4:
            assertTrue(name4.equals(code.getName()) && group4.equals(code.getGroup()));
            break;
         default:
            throw new AssertionError(String.format("The EAS CODE %s does not match any valid code in the inout file", code.getEas()));
         }
      }
   }
}

