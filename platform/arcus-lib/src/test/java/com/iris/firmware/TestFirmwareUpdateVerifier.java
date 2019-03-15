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

public class TestFirmwareUpdateVerifier extends FirmwareTestCase {
   
   @Test
   public void testNoOverlaps() throws Exception {
      loadFirmwares("firmware-test.xml");
      // No exceptions should be thrown.
   }
   
   @Test
   public void testNoOverlapsCases() throws Exception {
      loadFirmwares("firmware-cases.xml");
      // No exceptions should be thrown.
   }
   
   @Test
   public void testNoOverlapsReal() throws Exception {
      loadFirmwares("firmware-real.xml");
      // No exceptions should be thrown.
   }
   
   @Test
   public void testSimpleOverlap() throws Exception {
      try {
         loadFirmwares("firmware-simple-overlap.xml");
         Assert.fail("Should have thrown an exception.");
      }
      catch(RuntimeException ex) {
         Assert.assertEquals("Overlapping versions in firmware updates for population qa", ex.getCause().getMessage());
      }
   }
   
   @Test
   public void testExactOverlap() throws Exception {
      try {
         loadFirmwares("firmware-exact-overlap.xml");
         Assert.fail("Should have thrown an exception.");
      }
      catch(RuntimeException ex) {
         Assert.assertEquals("Overlapping versions in firmware updates for population qa", ex.getCause().getMessage());
      }
   }
   
   @Test
   public void testDuplicateOverlap() throws Exception {
      try {
         loadFirmwares("firmware-duplicate-overlap.xml");
         Assert.fail("Should have thrown an exception.");
      }
      catch(RuntimeException ex) {
         Assert.assertEquals("Overlapping versions in firmware updates for population general", ex.getCause().getMessage());
      }
   }
   
   @Test
   public void testAlphaOverlap() throws Exception {    
      try {
         loadFirmwares("firmware-overlap-alpha.xml");
         Assert.fail("Should have thrown an exception.");
      }
      catch(RuntimeException ex) {
         Assert.assertEquals("Overlapping versions in firmware updates for population qa", ex.getCause().getMessage());
      }
   }
   
   @Test
   public void testBetaOverlap() throws Exception {
      try {
         loadFirmwares("firmware-overlap-beta.xml");
         Assert.fail("Should have thrown an exception.");
      }
      catch(RuntimeException ex) {
         Assert.assertEquals("Overlapping versions in firmware updates for population beta", ex.getCause().getMessage());
      }
   }
}

