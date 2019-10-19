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

import org.junit.Assert;
import org.junit.Test;

import com.iris.messages.type.Population;
import com.iris.model.Version;

public class TestXMLFirmwareResolver extends FirmwareTestCase {
   
   @Test
   public void testResolveGeneralInValidRange() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.0.0.024");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.0.006", result.getTarget());
      
      result = resolver.getTargetForVersion("IH200", Version.fromRepresentation("2.0.0.124"));
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.0.006", result.getTarget());
      
      result = resolver.getTargetForVersion("IH200", "2.0.0.024", Population.NAME_GENERAL);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.0.006", result.getTarget());
      
      result = resolver.getTargetForVersion("IH200", "2.1.0.004");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.0.006", result.getTarget());
      
      result = resolver.getTargetForVersion("IH300", "3.1.0.023");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubOS_3.1.1.002", result.getTarget());
   }
   
   @Test
   public void testResolveGeneralOutsideAnyRanges() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // This is a weird test case because exceeding the max 2.0.x but it doesn't qualify for upgrading
      // to a 2.1.x version. 
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.1.0.002", Population.NAME_GENERAL);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_NEEDED, result.getResult());
      Assert.assertEquals(null, result.getTarget());
      
      // This is a weird test case because exceeding the max 2.0.x but it doesn't qualify for upgrading
      // to a 2.1.x version. 
      result = resolver.getTargetForVersion("IH200", "2.1.0.002");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_NEEDED, result.getResult());
      Assert.assertEquals(null, result.getTarget());
      
      result = resolver.getTargetForVersion("IH200", "2.0.0.023");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_POSSIBLE, result.getResult());
      Assert.assertEquals(null, result.getTarget());
      
      // This is a weird test case because exceeding the max 2.0.x but it doesn't qualify for upgrading
      // to a 2.1.x version. 
      result = resolver.getTargetForVersion("IH200", "2.0.1.005");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_NEEDED, result.getResult());
      Assert.assertEquals(null, result.getTarget());    
      
      result = resolver.getTargetForVersion("IH300", "3.1.0.001");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_POSSIBLE, result.getResult());
      Assert.assertEquals(null, result.getTarget());
      
      result = resolver.getTargetForVersion("IH300", "3.1.1.002");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_NEEDED, result.getResult());
      Assert.assertEquals(null, result.getTarget());
   }
   
   @Test
   public void testResolveGeneralInsideInvalidRanges() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // In both cases, these should come back without a target, but not as an error since
      // the versions are where they should be for the general population.
      
      
      // Check matching invalid ranges (wrong population)
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.1.0.006", Population.NAME_GENERAL);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_NEEDED, result.getResult());
      Assert.assertEquals(null, result.getTarget());
      
      result = resolver.getTargetForVersion("IH200", "2.1.0.007", Population.NAME_GENERAL);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_NEEDED, result.getResult());
      Assert.assertEquals(null, result.getTarget());
   }
   
   @Test
   public void testResolveAlphaMatchGeneral() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match QA Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.0.0.027", Population.NAME_QA);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.0.006", result.getTarget());      
   }
   
   @Test
   public void testResolveAlphaMatchAlphaOrBeta() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match QA Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.1.0.004", Population.NAME_QA);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.1.000", result.getTarget());      
   }
   
   @Test
   public void testResolveAlphaMatchAlphaOnly() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match QA Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.1.0.006", Population.NAME_QA);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubOS_2.1.2.000", result.getTarget());      
   }
   
   @Test
   public void testResolveAlphaNoMatch() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match QA Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.0.0.023", Population.NAME_QA);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_POSSIBLE, result.getResult());
      Assert.assertEquals(null, result.getTarget());      
   }
   
   @Test
   public void testResolveAlphaMatchBeta() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match QA Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.1.0.008", Population.NAME_QA);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_NEEDED, result.getResult());
      Assert.assertEquals(null, result.getTarget());      
   }
 
   @Test
   public void testResolveBetaMatchGeneral() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match Beta Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.0.0.999", "beta");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.0.006", result.getTarget());      
   }
   
   @Test
   public void testResolveBetaMatchAlphaOrBeta() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match Beta Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.1.0.004", "beta");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubBL_2.1.1.000", result.getTarget());      
   }
   
   @Test
   public void testResolveBetaMatchBetaOnly() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match Beta Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.1.0.006", "beta");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("hubOS_2.1.2.001", result.getTarget());      
   }
   
   @Test
   public void testResolveBetaNoMatch() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-cases.xml");
      
      // Match Beta Population Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "2.0.0.023", "beta");
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NOT_POSSIBLE, result.getResult());
      Assert.assertEquals(null, result.getTarget());      
   }
   
   @Test
   public void testResolveGeneralDifferentTargets() throws Exception {
      FirmwareUpdateResolver resolver = getResolver("firmware-test.xml");
      
      // Match First Firmware Entry
      FirmwareResult result = resolver.getTargetForVersion("IH200", "1.0.0.001", Population.NAME_GENERAL);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("HubOS_2.1.0.006", result.getTarget());
      
      // Match Second Firmware Entry
      result = resolver.getTargetForVersion("IH200", "2.1.0.010", Population.NAME_GENERAL);
      Assert.assertEquals(FirmwareResult.Result.UPGRADE_NEEDED, result.getResult());
      Assert.assertEquals("HubBL_2.1.1.002", result.getTarget());
   }
}

