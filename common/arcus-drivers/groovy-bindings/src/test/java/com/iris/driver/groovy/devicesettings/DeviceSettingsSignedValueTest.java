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
package com.iris.driver.groovy.devicesettings;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition.ParamType;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

/**
 * A parameter whose range goes below zero is a two's-complement number on the wire
 * (the T6 Pro's temperature offset, -3..3, reports -1 as 0xFF).
 */
public class DeviceSettingsSignedValueTest {

   private static final ZWaveCommandClass CONFIG_CC = ZWaveAllCommandClasses.getClass("configuration");
   private static final ZWaveCommand REPORT_CMD = CONFIG_CC.commandsByName.get("report");

   private DeviceSettingsParamDefinition offset;    // -3..3, 1 byte
   private DeviceSettingsParamDefinition wide;      // -1000..1000, 2 bytes
   private DeviceSettingsParamDefinition rampRate;  // 0..255, 1 byte (unsigned)
   private DeviceSettingsContext settings;
   private DeviceDriverContext context;

   @Before
   public void setUp() {
      offset = range("temperatureOffset", 42, 1, -3, 3);
      wide = range("calibration", 7, 2, -1000, 1000);
      rampRate = range("rampRate", 28, 1, 0, 255);
      settings = new DeviceSettingsContext(Arrays.asList(offset, wide, rampRate));
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void signedOnlyWhenValuesCanBeNegative() {
      assertTrue(offset.isSigned());
      assertTrue(wide.isSigned());
      assertFalse(rampRate.isSigned());

      Map<String, String> negativeOption = new LinkedHashMap<>();
      negativeOption.put("-1", "Minus one");
      negativeOption.put("0", "Off");
      assertTrue(DeviceSettingsParamDefinition.builder().withParamId("e").withType(ParamType.ENUM)
            .withEnumValues(negativeOption).build().isSigned());

      Map<String, String> positiveOptions = new LinkedHashMap<>();
      positiveOptions.put("0", "Off");
      positiveOptions.put("255", "On");
      assertFalse(DeviceSettingsParamDefinition.builder().withParamId("e").withType(ParamType.ENUM)
            .withEnumValues(positiveOptions).build().isSigned());
   }

   @Test(expected = IllegalArgumentException.class)
   public void signedRangeMustFitItsSize() {
      // 200 and -56 are the same byte
      range("disabledOrLevel", 5, 1, -1, 255);
   }

   @Test
   public void signedRangeAtTheSizeLimitIsAllowed() {
      assertEquals(-128L, range("full", 5, 1, -128, 127).decode(0x80, 1));
      assertEquals(-32768L, range("full2", 5, 2, -32768, 32767).decode(0x8000, 2));
   }

   @Test
   public void decodeSignExtendsUpperHalf() {
      assertEquals(-1L, offset.decode(0xFF, 1));
      assertEquals(-3L, offset.decode(0xFD, 1));
      assertEquals(3L, offset.decode(0x03, 1));
      assertEquals(-2L, wide.decode(0xFFFE, 2));
      assertEquals(1000L, wide.decode(1000, 2));
      assertEquals(-1L, wide.decode(0xFFFFFFFFL, 4));
   }

   @Test
   public void decodeLeavesUnsignedAndDecodedValuesAlone() {
      assertEquals(255L, rampRate.decode(0xFF, 1));
      // already decoded (e.g. stored by SetParam) stays as is
      assertEquals(-1L, offset.decode(-1, 1));
      assertEquals(-3L, offset.decodeStored(-3L));
      // a stale unsigned value saved before signed decoding reads back signed
      assertEquals(-1L, offset.decodeStored(255L));
      assertEquals(-1L, offset.decodeStored(255.0d));
      assertEquals(255L, rampRate.decodeStored(255L));
   }

   @Test
   public void reportOfNegativeValueIsStoredSigned() throws Exception {
      context.setVariable("devsettings:temperatureOffset", -1L);
      expectLastCall();
      replay(context);

      assertFalse(new DeviceSettingsConfigReportHandler(settings).handleEvent(context, report(42, 1, (byte) 0xFF)));
      verify(context);
   }

   @Test
   public void reportOfNegativeTwoByteValueIsStoredSigned() throws Exception {
      context.setVariable("devsettings:calibration", -2L);
      expectLastCall();
      replay(context);

      assertFalse(new DeviceSettingsConfigReportHandler(settings).handleEvent(context, report(7, 2, (byte) 0xFF, (byte) 0xFE)));
      verify(context);
   }

   @Test
   public void reportOfUnsignedParamStaysUnsigned() throws Exception {
      context.setVariable("devsettings:rampRate", 255L);
      expectLastCall();
      replay(context);

      assertFalse(new DeviceSettingsConfigReportHandler(settings).handleEvent(context, report(28, 1, (byte) 0xFF)));
      verify(context);
   }

   @Test
   public void attributeShowsStaleUnsignedValueAsNegative() {
      List<Map<String, Object>> params = settings.buildParamsAttribute(key ->
            "devsettings:temperatureOffset".equals(key) ? (Object) 255L : null);
      assertEquals(-1L, params.get(0).get("value"));
   }

   @Test
   public void getParamReturnsStaleUnsignedValueAsNegative() throws Exception {
      expect(context.getVariable("devsettings:temperatureOffset")).andReturn(255L).anyTimes();
      expect(context.getProtocolAddress()).andReturn(Address.protocolAddress("ZWAV", new byte[] { 0x02 })).anyTimes();
      Capture<MessageBody> response = Capture.newInstance();
      context.respondToPlatform(capture(response));
      expectLastCall();
      replay(context);

      MessageBody request = DeviceSettingsCapability.GetParamRequest.builder().withParamId("temperatureOffset").build();
      assertTrue(new DeviceSettingsGetParamHandler(settings).handleEvent(context, request));
      assertEquals("-1", DeviceSettingsCapability.GetParamResponse.getValue(response.getValue()));
   }

   @Test
   public void setParamSendsNegativeValueAsTwosComplement() throws Exception {
      expect(context.getProtocolAddress()).andReturn(Address.protocolAddress("ZWAV", new byte[] { 0x02 })).anyTimes();
      Capture<ZWaveCommandMessage> set = Capture.newInstance();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(set), eq(-1));
      expectLastCall();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), anyObject(ZWaveCommandMessage.class), eq(-1));
      expectLastCall();
      replay(context);

      MessageBody request = DeviceSettingsCapability.SetParamRequest.builder()
            .withParamId("temperatureOffset").withValue("-1").build();
      assertTrue(new DeviceSettingsSetParamHandler(settings).handleEvent(context, request));

      byte[] bytes = set.getValue().getCommand().toBytes();
      // CC, set, param 42, size 1, 0xFF
      assertEquals(5, bytes.length);
      assertEquals((byte) 42, bytes[2]);
      assertEquals((byte) 1, bytes[3]);
      assertEquals((byte) 0xFF, bytes[4]);
   }

   private static DeviceSettingsParamDefinition range(String name, int no, int size, long min, long max) {
      return DeviceSettingsParamDefinition.builder()
            .withParamId(name)
            .withType(ParamType.RANGE)
            .withZwaveParamNo(no)
            .withZwaveParamSize(size)
            .withMin(min)
            .withMax(max)
            .build();
   }

   static ProtocolMessage report(int paramNo, int size, byte... value) {
      byte[] payload = new byte[2 + value.length];
      payload[0] = (byte) paramNo;
      payload[1] = (byte) (size & 0x07);
      System.arraycopy(value, 0, payload, 2, value.length);

      ZWaveCommand command = new ZWaveCommand(CONFIG_CC.number, REPORT_CMD.commandNumber, payload);
      command.commandClass = CONFIG_CC.number;
      command.recvBytes = payload.clone();

      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(new ZWaveNode((byte) 0x02));
      message.setCommand(command);

      return ProtocolMessage.builder()
            .from(Address.protocolAddress("ZWAV", new byte[] { 0x02 }))
            .to(Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, message)
            .create();
   }
}
