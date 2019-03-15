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
package com.iris.protocol.zwave.constants;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class TestZWaveBasicDevicesType {

   @Test
   public void testMerge() {
      byte b1,b2;
      int i;
      b1 = 0x00;
      b2 = 0x01;
      
      i = ZWaveBasicDevicesType.merge(b1,b2);
      Assert.assertTrue(i==0x0001);
      
      i = ZWaveBasicDevicesType.merge(b2,b1);
      Assert.assertTrue(i==0x0100);
      
      b2 = (byte) 0xFF;
      i = ZWaveBasicDevicesType.merge(b1,b2);
      Assert.assertTrue(i==0x00FF);
      
      i = ZWaveBasicDevicesType.merge(b2,b1);
      Assert.assertTrue(i==0xFF00);
      
      b1 = (byte) 0x8F;
      i = ZWaveBasicDevicesType.merge(b1,b2);
      Assert.assertTrue(i==0x8FFF);
      
      i = ZWaveBasicDevicesType.merge(b2,b1);
      Assert.assertTrue(i==0xFF8F);
      
   }

   
   @Test
   public void testGetNameOf() {
      byte genId = ZWaveBasicDevicesType.AV_CONTROL_POINT;
      byte spId = ZWaveBasicDevicesType.DOORBELL;

      String name = ZWaveBasicDevicesType.getNameOf(genId, spId);

      Assert.assertTrue(name.equalsIgnoreCase("AV Controller"));
   }



}

