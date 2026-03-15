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

import org.junit.Test;

import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Verifies that DeviceSettingsUtil.buildCommand produces byte-identical
 * output to the Groovy DSL's ZWaveUtil.doSendZWaveCommand approach
 * (which builds fresh commands with synthetic "sendN" variable names).
 *
 * This ensures no regression from the refactor of the DeviceSettings handlers.
 */
public class TestDeviceSettingsBuildCommandCompat {

   private static final ZWaveCommandClass CONFIG_CC = ZWaveAllCommandClasses.getClass("configuration");

   // ---------------------------------------------------------------
   // Reproduce the OLD (pre-refactor) command building approach
   // This matches what ZWaveUtil.doSendZWaveCommand(protocol, cc, cmd, sendVars...) does
   // ---------------------------------------------------------------
   private ZWaveCommand buildOldWay(byte commandClass, byte commandNumber, byte... sendVars) {
      ZWaveCommand command = new ZWaveCommand();
      command.commandClass = commandClass;
      command.commandNumber = commandNumber;
      for (int i = 0; i < sendVars.length; i++) {
         String name = "send" + i;
         command.addSendVariable(name);
         command.setSend(name, sendVars[i]);
      }
      return command;
   }

   // ---------------------------------------------------------------
   // Configuration GET
   // ---------------------------------------------------------------

   @Test
   public void testConfigGetMatchesOldApproach() {
      byte paramNo = 2;

      // Old way: ZWaveUtil.doSendZWaveCommand(proto, 0x70, 0x05, paramNo)
      ZWaveCommand oldCmd = buildOldWay((byte) 0x70, (byte) 0x05, paramNo);

      // New way: DeviceSettingsUtil.buildCommand
      ZWaveCommand newCmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "get", paramNo);

      assertArrayEquals(
            "config.get bytes must match old approach",
            oldCmd.toBytes(), newCmd.toBytes());
   }

   @Test
   public void testConfigGetBytesAreExact() {
      byte paramNo = 9;
      ZWaveCommand cmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "get", paramNo);
      byte[] bytes = cmd.toBytes();

      // Must be exactly [CC, CMD, paramNo] — 3 bytes, no trailing zeros
      assertEquals(3, bytes.length);
      assertEquals((byte) 0x70, bytes[0]);
      assertEquals((byte) 0x05, bytes[1]);
      assertEquals(paramNo, bytes[2]);
   }

   // ---------------------------------------------------------------
   // Configuration SET — 1-byte value
   // ---------------------------------------------------------------

   @Test
   public void testConfigSet1ByteMatchesOldApproach() {
      byte paramNo = 2;
      byte size = 1;
      byte value = 3;

      // Old way: ZWave.configuration.set(paramNo, size, value)
      // which calls doSendZWaveCommand(proto, 0x70, 0x04, paramNo, size, value)
      ZWaveCommand oldCmd = buildOldWay((byte) 0x70, (byte) 0x04, paramNo, size, value);

      // New way
      ZWaveCommand newCmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set", paramNo, size, value);

      assertArrayEquals(
            "config.set (1-byte) bytes must match old approach",
            oldCmd.toBytes(), newCmd.toBytes());
   }

   @Test
   public void testConfigSet1ByteBytesAreExact() {
      byte paramNo = 6;
      byte size = 1;
      byte value = 5;
      ZWaveCommand cmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set", paramNo, size, value);
      byte[] bytes = cmd.toBytes();

      // Must be exactly [CC, CMD, paramNo, size, value] — 5 bytes
      assertEquals(5, bytes.length);
      assertEquals((byte) 0x70, bytes[0]);
      assertEquals((byte) 0x04, bytes[1]);
      assertEquals(paramNo, bytes[2]);
      assertEquals(size, bytes[3]);
      assertEquals(value, bytes[4]);
   }

   // ---------------------------------------------------------------
   // Configuration SET — 2-byte value
   // ---------------------------------------------------------------

   @Test
   public void testConfigSet2ByteMatchesOldApproach() {
      byte paramNo = 4;
      byte size = 2;
      // value = 300 → 0x01, 0x2C
      byte val1 = 0x01;
      byte val2 = 0x2C;

      ZWaveCommand oldCmd = buildOldWay((byte) 0x70, (byte) 0x04, paramNo, size, val1, val2);
      ZWaveCommand newCmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set", paramNo, size, val1, val2);

      assertArrayEquals(
            "config.set (2-byte) bytes must match old approach",
            oldCmd.toBytes(), newCmd.toBytes());
   }

   @Test
   public void testConfigSet2ByteBytesAreExact() {
      ZWaveCommand cmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set",
            (byte) 4, (byte) 2, (byte) 0x01, (byte) 0x2C);
      byte[] bytes = cmd.toBytes();

      assertEquals(6, bytes.length);
      assertEquals((byte) 0x70, bytes[0]);
      assertEquals((byte) 0x04, bytes[1]);
      assertEquals((byte) 4, bytes[2]);    // paramNo
      assertEquals((byte) 2, bytes[3]);    // size
      assertEquals((byte) 0x01, bytes[4]); // val MSB
      assertEquals((byte) 0x2C, bytes[5]); // val LSB
   }

   // ---------------------------------------------------------------
   // Configuration SET — 4-byte value
   // ---------------------------------------------------------------

   @Test
   public void testConfigSet4ByteMatchesOldApproach() {
      byte paramNo = 10;
      byte size = 4;
      // value = 86400 → 0x00, 0x01, 0x51, 0x80
      byte v1 = 0x00, v2 = 0x01, v3 = 0x51, v4 = (byte) 0x80;

      ZWaveCommand oldCmd = buildOldWay((byte) 0x70, (byte) 0x04, paramNo, size, v1, v2, v3, v4);
      ZWaveCommand newCmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set", paramNo, size, v1, v2, v3, v4);

      assertArrayEquals(
            "config.set (4-byte) bytes must match old approach",
            oldCmd.toBytes(), newCmd.toBytes());
   }

   @Test
   public void testConfigSet4ByteBytesAreExact() {
      ZWaveCommand cmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set",
            (byte) 10, (byte) 4, (byte) 0x00, (byte) 0x01, (byte) 0x51, (byte) 0x80);
      byte[] bytes = cmd.toBytes();

      assertEquals(8, bytes.length);
      assertEquals((byte) 0x70, bytes[0]);
      assertEquals((byte) 0x04, bytes[1]);
      assertEquals((byte) 10, bytes[2]);   // paramNo
      assertEquals((byte) 4, bytes[3]);    // size
      assertEquals((byte) 0x00, bytes[4]);
      assertEquals((byte) 0x01, bytes[5]);
      assertEquals((byte) 0x51, bytes[6]);
      assertEquals((byte) 0x80, bytes[7]);
   }

   // ---------------------------------------------------------------
   // Verify no send variable count mismatch
   // ---------------------------------------------------------------

   @Test
   public void testConfigGetHasExactly1SendVar() {
      ZWaveCommand cmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "get", (byte) 1);
      assertEquals(1, cmd.sendVariables.size());
   }

   @Test
   public void testConfigSet1ByteHasExactly3SendVars() {
      ZWaveCommand cmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set",
            (byte) 1, (byte) 1, (byte) 0);
      assertEquals(3, cmd.sendVariables.size());
   }

   @Test
   public void testConfigSet4ByteHasExactly6SendVars() {
      ZWaveCommand cmd = DeviceSettingsUtil.buildCommand(CONFIG_CC, "set",
            (byte) 1, (byte) 4, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
      assertEquals(6, cmd.sendVariables.size());
   }
}
