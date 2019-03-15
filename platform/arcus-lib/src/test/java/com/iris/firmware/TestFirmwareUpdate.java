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

import com.iris.messages.type.Population;
import com.iris.model.Version;

public class TestFirmwareUpdate extends FirmwareTestCase {
   
   private List<FirmwareUpdate> updates;
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      updates = loadFirmwares("firmware-cases.xml");
   }

   @Test
   public void testVersionIsInRange() throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, "2.0.0.024", "2.0.0.999", "hubBL_2.1.0.006","general", "qa", "beta");
      Assert.assertEquals("Should get a version match with general population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.0.0.037"), Population.NAME_GENERAL));
      Assert.assertEquals("Should get a version match with no population.", 
            FirmwareUpdate.MatchType.VERSION, 
            update.matches(Version.fromRepresentation("2.0.0.047")));
      Assert.assertEquals("Should get a version match with null population.", 
            FirmwareUpdate.MatchType.VERSION, 
            update.matches(Version.fromRepresentation("2.0.0.137"), null));
      Assert.assertEquals("Should get a version match with empty population.", 
            FirmwareUpdate.MatchType.VERSION, 
            update.matches(Version.fromRepresentation("2.0.0.998"), ""));
      Assert.assertEquals("Should get a version match with qa population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.0.0.998"), Population.NAME_QA));
      Assert.assertEquals("Should get a version match with beta population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.0.0.998"), Population.NAME_BETA));
   }
   
   @Test
   public void testVersionIsEqualMin() throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, "2.0.0.024", "2.0.0.999", "hubBL_2.1.0.006", "general", "qa", "beta");
      Assert.assertEquals("Should get a version match with general population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.0.0.024"), Population.NAME_GENERAL));
   }
   
   @Test
   public void testVersionIsEqualMax() throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, "2.0.0.024", "2.0.0.999", "hubBL_2.1.0.006", "general", "qa", "beta");
      Assert.assertEquals("Should get a version match with general population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.0.0.999"), Population.NAME_GENERAL));
   }
   
   @Test
   public void testVersionIsGreaterThanMax() throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, "2.0.0.024", "2.0.0.999", "hubBL_2.1.0.006", "general", "qa", "beta");
      Assert.assertEquals("Should not get a match with general population.", 
            FirmwareUpdate.MatchType.NONE, 
            update.matches(Version.fromRepresentation("2.0.1.024"), Population.NAME_GENERAL));
   }
   
   @Test
   public void testVersionIsLessThanMin() throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, "2.0.0.024", "2.0.0.999", "hubBL_2.1.0.006", "general", "qa", "beta");
      Assert.assertEquals("Should not get a match with general population.", 
            FirmwareUpdate.MatchType.NONE, 
            update.matches(Version.fromRepresentation("1.9.0.024"), Population.NAME_GENERAL));
   }
   
   @Test
   public void testMatchAlphaSpecifically() throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, "2.1.0.006", "2.1.0.006", "hubOS_2.1.2.000", Population.NAME_QA);
      Assert.assertEquals("Should get a version and population match with alpha population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.1.0.006"), Population.NAME_QA));
      Assert.assertEquals("Should not get a match with beta population.", 
            FirmwareUpdate.MatchType.NONE, 
            update.matches(Version.fromRepresentation("2.1.0.006"), Population.NAME_BETA));
      Assert.assertEquals("Should not get a match with general population.", 
            FirmwareUpdate.MatchType.NONE, 
            update.matches(Version.fromRepresentation("2.1.0.006"), Population.NAME_GENERAL));
      Assert.assertEquals("Should get a version with no population.", 
            FirmwareUpdate.MatchType.VERSION, 
            update.matches(Version.fromRepresentation("2.1.0.006")));
   }
   
   @Test
   public void testMatchAlphaOrBetaSpecifically() throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, "2.1.0.003", "2.1.0.005", "hubBL_2.1.1.000", Population.NAME_QA, Population.NAME_BETA);
      Assert.assertEquals("Should get a version and population match with alpha population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.1.0.004"), Population.NAME_QA));
      Assert.assertEquals("Should get a version and population match with beta population.", 
            FirmwareUpdate.MatchType.VERSION_AND_POPULATION, 
            update.matches(Version.fromRepresentation("2.1.0.004"), Population.NAME_BETA));
      Assert.assertEquals("Should not get a match with general population.", 
            FirmwareUpdate.MatchType.NONE, 
            update.matches(Version.fromRepresentation("2.1.0.004"), Population.NAME_GENERAL));
      Assert.assertEquals("Should get a match with no population.", 
            FirmwareUpdate.MatchType.VERSION, 
            update.matches(Version.fromRepresentation("2.1.0.004")));
   }
}

