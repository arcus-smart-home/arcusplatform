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

import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

public class TestDeviceSettingsConfigReportHandler {

   private static final ZWaveCommandClass CONFIG_CC = ZWaveAllCommandClasses.getClass("configuration");
   private static final ZWaveCommand REPORT_CMD = CONFIG_CC.commandsByName.get("report");

   private DeviceDriverContext context;
   private DeviceSettingsContext settingsContext;
   private DeviceSettingsConfigReportHandler handler;

   private DeviceSettingsParamDefinition ledParam;
   private DeviceSettingsParamDefinition dimRateParam;

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

      dimRateParam = DeviceSettingsParamDefinition.builder()
            .withParamId("dimRate")
            .withDescription("Dimming Rate")
            .withType(DeviceSettingsParamDefinition.ParamType.RANGE)
            .withZwaveParamNo(9)
            .withZwaveParamSize(1)
            .withMin(1)
            .withMax(99)
            .withDefaultValue(1)
            .build();

      settingsContext = new DeviceSettingsContext(Arrays.asList(ledParam, dimRateParam));
      handler = new DeviceSettingsConfigReportHandler(settingsContext);
      context = createNiceMock(DeviceDriverContext.class);
   }

   @Test
   public void testIgnoresNonProtocolMessage() throws Exception {
      replay(context);
      assertFalse(handler.handleEvent(context, "not a protocol message"));
      verify(context);
   }

   @Test
   public void testProcessesKnownParam1Byte() throws Exception {
      // Configuration Report: param=2, size=1, value=1
      // Raw bytes after CC+cmd: [paramNo, level, val1]
      ProtocolMessage protocolMessage = buildConfigReportMessage((byte) 2, 1, new byte[]{0x01});

      context.setVariable("devsettings:ledMode", 1L);
      expectLastCall();
      replay(context);

      boolean result = handler.handleEvent(context, protocolMessage);
      assertFalse(result);
      verify(context);
   }

   @Test
   public void testIgnoresUnknownParam() throws Exception {
      // Configuration Report for param 99 (not declared)
      ProtocolMessage protocolMessage = buildConfigReportMessage((byte) 99, 1, new byte[]{0x42});

      replay(context);
      boolean result = handler.handleEvent(context, protocolMessage);
      assertFalse(result);
      verify(context);
   }

   @Test
   public void testProcesses2ByteValue() throws Exception {
      DeviceSettingsParamDefinition timerParam = DeviceSettingsParamDefinition.builder()
            .withParamId("autoOff")
            .withDescription("Auto Off Timer")
            .withType(DeviceSettingsParamDefinition.ParamType.RANGE)
            .withZwaveParamNo(4)
            .withZwaveParamSize(2)
            .withMin(0)
            .withMax(65535)
            .withDefaultValue(0)
            .build();

      DeviceSettingsContext ctx2 = new DeviceSettingsContext(Arrays.asList(timerParam));
      DeviceSettingsConfigReportHandler handler2 = new DeviceSettingsConfigReportHandler(ctx2);

      // param=4, size=2, value=0x012C (300)
      ProtocolMessage protocolMessage = buildConfigReportMessage((byte) 4, 2, new byte[]{0x01, 0x2C});

      context.setVariable("devsettings:autoOff", 300L);
      expectLastCall();
      replay(context);

      assertFalse(handler2.handleEvent(context, protocolMessage));
      verify(context);
   }

   @Test
   public void testProcesses4ByteValue() throws Exception {
      DeviceSettingsParamDefinition bigParam = DeviceSettingsParamDefinition.builder()
            .withParamId("bigParam")
            .withDescription("Big Param")
            .withType(DeviceSettingsParamDefinition.ParamType.RANGE)
            .withZwaveParamNo(10)
            .withZwaveParamSize(4)
            .withMin(0)
            .withMax(0xFFFFFFFFL)
            .withDefaultValue(0)
            .build();

      DeviceSettingsContext ctx4 = new DeviceSettingsContext(Arrays.asList(bigParam));
      DeviceSettingsConfigReportHandler handler4 = new DeviceSettingsConfigReportHandler(ctx4);

      // param=10, size=4, value=0x00015180 (86400)
      ProtocolMessage protocolMessage = buildConfigReportMessage((byte) 10, 4,
            new byte[]{0x00, 0x01, 0x51, (byte) 0x80});

      context.setVariable("devsettings:bigParam", 86400L);
      expectLastCall();
      replay(context);

      assertFalse(handler4.handleEvent(context, protocolMessage));
      verify(context);
   }

   @Test
   public void testIgnoresNonConfigurationReport() throws Exception {
      // Build a Switch Binary Report (different command class)
      ZWaveCommandClass switchCC = ZWaveAllCommandClasses.getClass("switch_binary");
      ZWaveCommand switchReport = switchCC.commandsByName.get("report");

      ZWaveCommand command = new ZWaveCommand(switchReport);
      command.commandClass = switchCC.number;

      ZWaveCommandMessage zwaveMessage = new ZWaveCommandMessage();
      zwaveMessage.setDevice(new ZWaveNode((byte) 0x02));
      zwaveMessage.setCommand(command);

      ProtocolMessage protocolMessage = ProtocolMessage.builder()
            .from(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .to(com.iris.messages.address.Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, zwaveMessage)
            .create();

      replay(context);
      assertFalse(handler.handleEvent(context, protocolMessage));
      verify(context);
   }

   /**
    * Builds a ProtocolMessage containing a Z-Wave Configuration Report.
    * Constructs the raw payload bytes directly so they survive serialization round-trip.
    */
   private ProtocolMessage buildConfigReportMessage(byte paramNo, int size, byte[] valueBytes) {
      // Configuration Report payload: [paramNo, level(size), value bytes...]
      ZWaveCommand command = new ZWaveCommand(CONFIG_CC.number, REPORT_CMD.commandNumber,
            buildReportPayload(paramNo, size, valueBytes));
      command.commandClass = CONFIG_CC.number;

      // Also populate recvBytes for the handler's raw-bytes path
      byte[] recvPayload = buildReportPayload(paramNo, size, valueBytes);
      command.recvBytes = recvPayload;

      ZWaveCommandMessage zwaveMessage = new ZWaveCommandMessage();
      zwaveMessage.setDevice(new ZWaveNode((byte) 0x02));
      zwaveMessage.setCommand(command);

      return ProtocolMessage.builder()
            .from(com.iris.messages.address.Address.protocolAddress("ZWAV", new byte[]{0x02}))
            .to(com.iris.messages.address.Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, zwaveMessage)
            .create();
   }

   private byte[] buildReportPayload(byte paramNo, int size, byte[] valueBytes) {
      byte[] payload = new byte[2 + valueBytes.length];
      payload[0] = paramNo;
      payload[1] = (byte) (size & 0x07);
      System.arraycopy(valueBytes, 0, payload, 2, valueBytes.length);
      return payload;
   }
}
