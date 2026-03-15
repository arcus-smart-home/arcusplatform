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

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

public class TestDeviceSettingsSetParamHandler {

   private static final ZWaveCommandClass CONFIG_CC = ZWaveAllCommandClasses.getClass("configuration");

   private DeviceDriverContext context;
   private DeviceSettingsContext settingsContext;
   private DeviceSettingsSetParamHandler handler;

   private DeviceSettingsParamDefinition ledParam;

   @Before
   public void setUp() {
      ledParam = DeviceSettingsParamDefinition.builder()
            .withParamId("ledMode")
            .withDescription("LED Indicator Mode")
            .withType(DeviceSettingsParamDefinition.ParamType.RANGE)
            .withZwaveParamNo(2)
            .withZwaveParamSize(1)
            .withMin(0)
            .withMax(3)
            .withDefaultValue(0)
            .build();

      settingsContext = new DeviceSettingsContext(Arrays.asList(ledParam));
      handler = new DeviceSettingsSetParamHandler(settingsContext);
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void testRejectsUnknownParam() throws Exception {
      MessageBody request = DeviceSettingsCapability.SetParamRequest.builder()
            .withParamId("unknownParam")
            .withValue("1")
            .build();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request));
      assertEquals("INVALID_PARAM", DeviceSettingsCapability.SetParamResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testRejectsInvalidValue() throws Exception {
      MessageBody request = DeviceSettingsCapability.SetParamRequest.builder()
            .withParamId("ledMode")
            .withValue("99")  // out of range [0, 3]
            .build();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();
      replay(context);

      assertTrue(handler.handleEvent(context, request));
      assertEquals("INVALID_VALUE", DeviceSettingsCapability.SetParamResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testSendsConfigSetAndGet() throws Exception {
      MessageBody request = DeviceSettingsCapability.SetParamRequest.builder()
            .withParamId("ledMode")
            .withValue("2")
            .build();

      // Expect: setVariable, two sendToDevice calls (set + get), respondToPlatform
      context.setVariable("devsettings:ledMode", 2L);
      expectLastCall();

      // Capture the two Z-Wave messages sent (config set + config get)
      Capture<ZWaveCommandMessage> setCapture = Capture.newInstance();
      Capture<ZWaveCommandMessage> getCapture = Capture.newInstance();

      expect(context.getProtocolAddress())
            .andReturn(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(setCapture), eq(-1));
      expectLastCall();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(getCapture), eq(-1));
      expectLastCall();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();

      replay(context);

      assertTrue(handler.handleEvent(context, request));

      // Verify the config set command bytes: [CC, CMD, paramNo, size, val1]
      ZWaveCommand setCmd = setCapture.getValue().getCommand();
      assertEquals(CONFIG_CC.number, setCmd.commandClass);
      byte[] setBytes = setCmd.toBytes();
      assertEquals(5, setBytes.length);                   // CC + CMD + 3 send vars (no trailing zeros)
      assertEquals(CONFIG_CC.number, setBytes[0]);        // command class
      assertEquals((byte) 0x04, setBytes[1]);             // set command
      assertEquals((byte) 2, setBytes[2]);                // param number
      assertEquals((byte) 1, setBytes[3]);                // size
      assertEquals((byte) 2, setBytes[4]);                // value = 2

      // Verify the config get command bytes: [CC, CMD, paramNo]
      ZWaveCommand getCmd = getCapture.getValue().getCommand();
      assertEquals(CONFIG_CC.number, getCmd.commandClass);
      byte[] getBytes = getCmd.toBytes();
      assertEquals(3, getBytes.length);                   // CC + CMD + 1 send var
      assertEquals((byte) 2, getBytes[2]);                // param number

      // Verify response
      assertEquals("PENDING", DeviceSettingsCapability.SetParamResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }

   @Test
   public void testIgnoresNonPlatformMessage() throws Exception {
      replay(context);
      assertFalse(handler.handleEvent(context, "not a platform message"));
      verify(context);
   }

   @Test
   public void testEnumValidation() throws Exception {
      Map<String, String> enumValues = new LinkedHashMap<>();
      enumValues.put("0", "Always Off");
      enumValues.put("1", "On when load on");
      enumValues.put("2", "On when load off");

      DeviceSettingsParamDefinition enumParam = DeviceSettingsParamDefinition.builder()
            .withParamId("ledEnum")
            .withDescription("LED Enum")
            .withType(DeviceSettingsParamDefinition.ParamType.ENUM)
            .withZwaveParamNo(3)
            .withZwaveParamSize(1)
            .withEnumValues(enumValues)
            .withDefaultValue(0)
            .build();

      DeviceSettingsContext enumCtx = new DeviceSettingsContext(Arrays.asList(enumParam));
      DeviceSettingsSetParamHandler enumHandler = new DeviceSettingsSetParamHandler(enumCtx);

      // Valid enum value
      MessageBody validRequest = DeviceSettingsCapability.SetParamRequest.builder()
            .withParamId("ledEnum")
            .withValue("1")
            .build();

      expect(context.getProtocolAddress())
            .andReturn(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .anyTimes();
      context.setVariable(anyString(), anyObject());
      expectLastCall().anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), anyObject(ZWaveCommandMessage.class), eq(-1));
      expectLastCall().anyTimes();
      context.respondToPlatform(anyObject(MessageBody.class));
      expectLastCall();
      replay(context);

      assertTrue(enumHandler.handleEvent(context, validRequest));
      verify(context);

      // Invalid enum value
      reset(context);
      MessageBody invalidRequest = DeviceSettingsCapability.SetParamRequest.builder()
            .withParamId("ledEnum")
            .withValue("5")
            .build();

      Capture<MessageBody> responseCapture = Capture.newInstance();
      context.respondToPlatform(capture(responseCapture));
      expectLastCall();
      replay(context);

      assertTrue(enumHandler.handleEvent(context, invalidRequest));
      assertEquals("INVALID_VALUE", DeviceSettingsCapability.SetParamResponse.getStatus(responseCapture.getValue()));
      verify(context);
   }
}
