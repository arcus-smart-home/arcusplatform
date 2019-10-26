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
package com.iris.firmware;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestFirmwareParser extends FirmwareTestCase {

   @Test
   public void testBasicParsing() throws Exception {
      List<FirmwareUpdate> updates = loadFirmwares("firmware-test.xml");
      Assert.assertEquals("There should be three updates.", 3, updates.size());

      getAndVerifyUpdate(updates, "1.0.0.0", "2.1.0.005", "HubOS_2.1.0.006", "general", "qa", "beta");
      getAndVerifyUpdate(updates, "2.1.0.009", "2.1.0.026", "HubBL_2.1.1.002", "general");
      getAndVerifyUpdate(updates, "2.1.0.009", "2.1.1.002", "HubOS_2.1.2.0", "qa", "beta");
   }

   @Test
   public void testFirmwareCases() throws Exception {
      List<FirmwareUpdate> updates = loadFirmwares("firmware-cases.xml");
      Assert.assertEquals("There should be five updates.", 6, updates.size());

      getAndVerifyUpdate(updates, "2.0.0.024", "2.0.0.999", "hubBL_2.1.0.006", "general", "qa", "beta");
      getAndVerifyUpdate(updates, "2.1.0.003", "2.1.0.005", "hubBL_2.1.0.006", "general");
      getAndVerifyUpdate(updates, "2.1.0.003", "2.1.0.005", "hubBL_2.1.1.000", "qa", "beta");
      getAndVerifyUpdate(updates, "2.1.0.006", "2.1.0.006", "hubOS_2.1.2.000", "qa");
      getAndVerifyUpdate(updates, "2.1.0.006", "2.1.0.008", "hubOS_2.1.2.001", "beta");
      getAndVerifyUpdate(updates, "3.1.0.004", "3.1.0.054", "hubOS_3.1.1.002", "general", "qa", "beta");
   }

   @Test
   public void testFirmwareReal() throws Exception {
      List<FirmwareUpdate> updates = loadFirmwares("firmware-real.xml");
      Assert.assertEquals("There should be two updates.", 2, updates.size());

      getAndVerifyUpdate(updates, "2.0.0.024", "2.0.0.999", "hubBL_2.1.0.006", "general","beta","qa");
      getAndVerifyUpdate(updates, "2.1.0.003", "2.1.0.005", "hubBL_2.1.0.006", "general","beta","qa");
   }

   @Test
   public void testOverlappingVersions() throws Exception {
      try {
         loadFirmwares("firmware-simple-overlap.xml");
         Assert.fail("Shouldn't have been able to parse the firmware update.");
      }
      catch(RuntimeException ex) {
         Assert.assertTrue(ex.getCause().getMessage().startsWith("Overlapping versions"));
      }
   }

   @Test
   public void testFirmwareMissingMax() throws Exception {
      try {
         loadFirmwares("firmware-missing-max.xml");
         Assert.fail("Shouldn't have been able to parse the firmware update.");
      }
      catch(RuntimeException ex) {
         Assert.assertTrue(ex.getCause().getCause().getMessage().startsWith("Invalid version format"));
      }
   }

   @Test
   public void testFirmwareMissingMin() throws Exception {
      try {
         loadFirmwares("firmware-missing-min.xml");
         Assert.fail("Shouldn't have been able to parse the firmware update.");
      }
      catch(RuntimeException ex) {
         Assert.assertTrue(ex.getCause().getCause().getMessage().startsWith("Invalid version format"));
      }
   }

   @Test
   public void testFirmwareMissingTarget() throws Exception {
      try {
         loadFirmwares("firmware-missing-target.xml");
         Assert.fail("Shouldn't have been able to parse the firmware update.");
      }
      catch(RuntimeException ex) {
         Assert.assertTrue(ex.getCause().getCause().getMessage().startsWith("The firmware target cannot be empty"));
      }
   }

   @Test
   public void testFirmwareMissingModel() throws Exception {
      try {
         loadFirmwares("firmware-missing-model.xml");
         Assert.fail("Shouldn't have been able to parse the firmware update.");
      }
      catch(RuntimeException ex) {
         Assert.assertTrue(ex.getCause().getCause().getMessage().startsWith("The firmware hardware model cannot be empty"));
      }
   }
   @Test
   public void testFirmwareReverseMinMax() throws Exception {
      try {
         loadFirmwares("firmware-reverse-min-max.xml");
         Assert.fail("Shouldn't have been able to parse the firmware update.");
      }
      catch(RuntimeException ex) {
         Assert.assertTrue(ex.getCause().getCause().getMessage().startsWith("The max version cannot be less than the min version"));
      }
   }
}

