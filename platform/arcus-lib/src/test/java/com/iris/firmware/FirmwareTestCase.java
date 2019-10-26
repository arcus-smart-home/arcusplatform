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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;

import com.google.common.collect.Sets;
import com.iris.model.Version;
import com.iris.resource.Resource;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.util.IrisCollections;

public class FirmwareTestCase {
   protected ClassPathResourceFactory factory;

   @Before
   public void setUp() throws Exception {
      factory = new ClassPathResourceFactory();
   }
   
   protected void getAndVerifyUpdate(List<FirmwareUpdate> updates, 
         String min, 
         String max, 
         String target, 
         String... populations) throws Exception {
      getAndVerifyUpdate(updates, 
            Version.fromRepresentation(min), 
            Version.fromRepresentation(max),
            target,
            populations);
   }
   
   protected void getAndVerifyUpdate(List<FirmwareUpdate> updates, 
         Version min, 
         Version max, 
         String target, 
         String... populations) throws Exception {
      FirmwareUpdate update = getFirmwareUpdate(updates, min, max, target, populations);
      verifyFirmwareUpdate(update, min, max, target, populations);
   }
   
   protected FirmwareUpdate getFirmwareUpdate(List<FirmwareUpdate> updates, 
         String min, 
         String max, 
         String target, 
         String... populations) throws Exception {
      return getFirmwareUpdate(updates, 
            Version.fromRepresentation(min),
            Version.fromRepresentation(max),
            target,
            populations);
   }
   
   protected FirmwareUpdate getFirmwareUpdate(List<FirmwareUpdate> updates, 
         Version min, 
         Version max, 
         String target, 
         String... populations) throws Exception {
      List<FirmwareUpdate> matches = updates.stream()
            .filter(u -> isFirmwareUpdateEquals(u, min, max, target, populations))
            .collect(Collectors.toList());
      Assert.assertEquals("There should only be one matching update.", 1, matches.size());
      return matches.get(0);
   }
   
   protected boolean isFirmwareUpdateEquals(FirmwareUpdate update, 
         Version min, 
         Version max, 
         String target, 
         String... populations) {
      boolean isEquals = min.equals(update.getMin());
      if (isEquals) {
         isEquals = max.equals(update.getMax());
      }
      if (isEquals) {
         isEquals = target.equals(update.getTarget());
      }
      if (isEquals) {
         if (populations == null || populations.length == 0) {
            isEquals = false;
         }
         else {
            isEquals = Sets.difference(IrisCollections.setOf(populations), update.getPopulations()).isEmpty();
         }
      }
      return isEquals;
   }
   
   protected void verifyFirmwareUpdate(FirmwareUpdate update, 
         Version min, 
         Version max, 
         String target, 
         String... populations) throws Exception {
      Assert.assertEquals("Minimum versions should match.", min, update.getMin());
      Assert.assertEquals("Maximum versions should match.", max, update.getMax());
      Assert.assertEquals("Targets should match.", target, update.getTarget());
      if (populations == null || populations.length == 0) {
      	Assert.fail("Population should not be empty");
      }
      else {
         Assert.assertEquals("Populations should match.", IrisCollections.setOf(populations), update.getPopulations());
      }
   }
   
   protected List<FirmwareUpdate> loadFirmwares(String fileName) throws Exception {
      return getFirmwareManager(fileName).getParsedData();
   }
   
   protected FirmwareUpdateResolver getResolver(String fileName) throws Exception {
      return new XMLFirmwareResolver(getFirmwareManager(fileName));
   }
   
   protected FirmwareManager getFirmwareManager(String fileName) throws Exception {
      return getFirmwareManager(factory.create(new URI("classpath:/" + fileName)));
   }

   protected FirmwareManager getFirmwareManager(Resource resource) throws Exception {
      return new FirmwareManager(resource);
   }
}

