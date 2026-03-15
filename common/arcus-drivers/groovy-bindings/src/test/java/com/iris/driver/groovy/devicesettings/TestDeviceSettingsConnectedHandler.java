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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

public class TestDeviceSettingsConnectedHandler {

   private static final ZWaveCommandClass CONFIG_CC = ZWaveAllCommandClasses.getClass("configuration");

   private DeviceDriverContext context;
   private DeviceSettingsConnectedHandler handler;

   private DeviceSettingsParamDefinition param1;
   private DeviceSettingsParamDefinition param2;
   private DeviceSettingsParamDefinition param3;

   @Before
   public void setUp() {
      param1 = DeviceSettingsParamDefinition.builder()
            .withParamId("ledMode")
            .withZwaveParamNo(2)
            .withZwaveParamSize(1)
            .withMin(0).withMax(3).withDefaultValue(0)
            .build();

      param2 = DeviceSettingsParamDefinition.builder()
            .withParamId("dimRate")
            .withZwaveParamNo(9)
            .withZwaveParamSize(1)
            .withMin(1).withMax(99).withDefaultValue(1)
            .build();

      param3 = DeviceSettingsParamDefinition.builder()
            .withParamId("powerRestore")
            .withZwaveParamNo(8)
            .withZwaveParamSize(1)
            .withMin(0).withMax(2).withDefaultValue(2)
            .build();

      DeviceSettingsContext settingsContext = new DeviceSettingsContext(Arrays.asList(param1, param2, param3));
      handler = new DeviceSettingsConnectedHandler(settingsContext);
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void testSendsConfigGetForEachParam() throws Exception {
      Capture<ZWaveCommandMessage> captures = Capture.newInstance(CaptureType.ALL);

      expect(context.getProtocolAddress())
            .andReturn(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), capture(captures), eq(-1));
      expectLastCall().times(3);

      replay(context);

      boolean result = handler.handleEvent(context, new DeviceConnectedEvent(null));
      assertFalse(result); // returns false to allow driver's onConnected to run

      verify(context);

      // Verify 3 config get commands were sent
      List<ZWaveCommandMessage> messages = captures.getValues();
      assertEquals(3, messages.size());

      // Verify each is a configuration.get with the right param number
      int[] expectedParams = {2, 9, 8};
      for (int i = 0; i < 3; i++) {
         ZWaveCommand cmd = messages.get(i).getCommand();
         assertEquals(CONFIG_CC.number, cmd.commandClass);
         byte[] bytes = cmd.toBytes();
         assertEquals(3, bytes.length);  // CC + CMD + paramNo (no trailing zeros)
         assertEquals((byte) 0x05, bytes[1]);  // get command
         assertEquals((byte) expectedParams[i], bytes[2]);  // param number
      }
   }

   @Test
   public void testReturnsFalseToAllowDriverHandler() throws Exception {
      expect(context.getProtocolAddress())
            .andReturn(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .anyTimes();
      context.sendToDevice(eq(ZWaveProtocol.INSTANCE), anyObject(ZWaveCommandMessage.class), eq(-1));
      expectLastCall().anyTimes();

      replay(context);

      assertFalse(handler.handleEvent(context, new DeviceConnectedEvent(null)));
      verify(context);
   }
}
