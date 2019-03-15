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
package com.iris.driver.groovy.zigbee;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.zcl.Fan;
import com.iris.protocol.zigbee.zcl.General;

public class TestZigbeeAttributeReading extends ZigbeeHandlersTestCase {

   public TestZigbeeAttributeReading() {
      super("ZigbeeAttributeReading.driver");
   }

   @Test
   public void testReadAttributes() throws Exception {
      General.ZclReadAttributeRecord fanModeAttribute = General.ZclReadAttributeRecord.builder()
            .setAttributeIdentifier(Fan.ATTR_FAN_MODE)
            .setAttributeData(ZclData.builder().set8BitEnum(Fan.FAN_MODE_HIGH).create())
            .create();
       General.ZclReadAttributeRecord fanSeqModeAttribute = General.ZclReadAttributeRecord.builder()
             .setAttributeIdentifier(Fan.ATTR_FAN_MODE_SEQUENCE)
             .setAttributeData(ZclData.builder().set8BitEnum(Fan.FAN_MODE_SEQUENCE_LOW_HIGH).create())
             .create();
       General.ZclReadAttributesResponse response = General.ZclReadAttributesResponse.builder()
             .setAttributes(new General.ZclReadAttributeRecord[] { fanModeAttribute, fanSeqModeAttribute })
             .create();
       
       createAndSendZclMessage(Fan.CLUSTER_ID, General.ZclReadAttributesResponse.ID, false, response);
       
       Assert.assertEquals(Fan.FAN_MODE_HIGH, toByte(context.getVariable("fanMode")));
       Assert.assertEquals(Fan.FAN_MODE_SEQUENCE_LOW_HIGH, toByte(context.getVariable("fanSeqMode")));
       
   }
   
   @Test
   public void testReportAttributes() throws Exception {
      General.ZclReadAttributeRecord fanModeAttribute = General.ZclReadAttributeRecord.builder()
            .setAttributeIdentifier(Fan.ATTR_FAN_MODE)
            .setAttributeData(ZclData.builder().set8BitEnum(Fan.FAN_MODE_HIGH).create())
            .create();
       General.ZclReadAttributeRecord fanSeqModeAttribute = General.ZclReadAttributeRecord.builder()
             .setAttributeIdentifier(Fan.ATTR_FAN_MODE_SEQUENCE)
             .setAttributeData(ZclData.builder().set8BitEnum(Fan.FAN_MODE_SEQUENCE_LOW_HIGH).create())
             .create();
       General.ZclReadAttributesResponse response = General.ZclReadAttributesResponse.builder()
             .setAttributes(new General.ZclReadAttributeRecord[] { fanModeAttribute, fanSeqModeAttribute })
             .create();
       
       createAndSendZclMessage(Fan.CLUSTER_ID, General.ZclReadAttributesResponse.ID, false, response);
       
       Assert.assertEquals(Fan.FAN_MODE_HIGH, toByte(context.getVariable("fanMode")));
       Assert.assertEquals(Fan.FAN_MODE_SEQUENCE_LOW_HIGH, toByte(context.getVariable("fanSeqMode")));
       
   }
}

