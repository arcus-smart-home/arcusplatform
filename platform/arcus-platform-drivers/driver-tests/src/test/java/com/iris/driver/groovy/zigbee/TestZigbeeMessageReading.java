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

import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.Fan;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.zdp.Discovery;

public class TestZigbeeMessageReading extends ZigbeeHandlersTestCase {

   public TestZigbeeMessageReading() {
      super("ZigbeeMessageReading.driver");
   }

   @Test
   public void testPackageAndUnpackageMessageInJava() throws Exception {
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
      
      ProtocolMessage msg = createZclMessage(Fan.CLUSTER_ID, General.ZclReadAttributesResponse.ID, false, response);
      
      ZigbeeMessage.Protocol zigbeeMessage = msg.getValue(ZigbeeProtocol.INSTANCE);
      Assert.assertEquals(ZigbeeMessage.Zcl.ID, zigbeeMessage.getType());
      
      ZigbeeMessage.Zcl zclMessage = ZigbeeProtocol.getZclMessage(zigbeeMessage);
      Assert.assertEquals(TEST_ENDPOINT, zclMessage.getEndpoint());
      Assert.assertEquals(TEST_PROFILE, zclMessage.getProfileId());
      Assert.assertEquals(Fan.CLUSTER_ID, zclMessage.getClusterId());
      Assert.assertEquals(General.ZclReadAttributesResponse.ID, zclMessage.getZclMessageId());
      Assert.assertEquals(0, zclMessage.getFlags());
      
      General.ZclReadAttributesResponse resp = General.ZclReadAttributesResponse.serde().fromBytes(BYTE_ORDER, zclMessage.getPayload());
      General.ZclReadAttributeRecord[] records = resp.getAttributes();
      Assert.assertEquals(2, records.length);
      for (General.ZclReadAttributeRecord record : records) {
         if (record.getAttributeIdentifier() == Fan.ATTR_FAN_MODE) {
            Assert.assertEquals(Fan.FAN_MODE_HIGH, (byte)record.getAttributeData().getDataValue());
         }
         if (record.getAttributeIdentifier() == Fan.ATTR_FAN_MODE_SEQUENCE) {
            Assert.assertEquals(Fan.FAN_MODE_SEQUENCE_LOW_HIGH, (byte)record.getAttributeData().getDataValue());
         }
      }
   }
   
   @Test
   public void testZdpMessageInDriver() throws Exception {
      Discovery.ZdpNwkAddrRsp nwkRsp = Discovery.ZdpNwkAddrRsp.builder()
            .setIeeeAddr(4)
            .setNwkAddr(8)
            .setStatus(15)
            .create();
      createAndSendZdpMessage(Discovery.ZdpNwkAddrRsp.ID, nwkRsp);
      
      Assert.assertEquals(4, toLong(context.getVariable("ieee")));
      Assert.assertEquals(8, toInt(context.getVariable("nwk")));
      Assert.assertEquals(15, toByte(context.getVariable("status")));
   }
   
   @Test
   public void testUnpackageMessageInDriver() throws Exception {
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
      
      Assert.assertEquals(Fan.CLUSTER_ID, toShort(context.getVariable("clusterId")));
      Assert.assertEquals(General.ZclReadAttributesResponse.ID, toByte(context.getVariable("messageId")));
      Assert.assertEquals(TEST_ENDPOINT, toByte(context.getVariable("endpoint")));
      Assert.assertEquals(TEST_PROFILE, toShort(context.getVariable("profileId")));
      Assert.assertEquals(0, toByte(context.getVariable("flags")));
      Assert.assertEquals(2, context.getVariable("numRecords"));
      Assert.assertEquals(Fan.ATTR_FAN_MODE, toShort(context.getVariable("attribId0")));
      Assert.assertEquals(Fan.FAN_MODE_HIGH, toByte(context.getVariable("attribValue0")));
      Assert.assertEquals(Fan.ATTR_FAN_MODE_SEQUENCE, toShort(context.getVariable("attribId1")));
      Assert.assertEquals(Fan.FAN_MODE_SEQUENCE_LOW_HIGH, toByte(context.getVariable("attribValue1")));
   }
}

