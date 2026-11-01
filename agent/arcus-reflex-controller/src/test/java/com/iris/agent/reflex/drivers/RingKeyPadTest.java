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
package com.iris.agent.reflex.drivers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.KeyPadCapability;

public class RingKeyPadTest {
   private static final byte NODE = 10;
   private static final byte CC_ENTRY_CONTROL = 0x6F;
   private static final byte CC_SUPERVISION = 0x6C;
   private static final byte CC_CONFIGURATION = 0x70;
   private static final byte CC_NOTIFICATION = 0x71;

   private final RecordingKeyPad keypad = new RecordingKeyPad();

   @Test
   public void everyPanicButtonRaisesAPanic() {
      for (int event : new int[] { 0x10, 0x11, 0x12, 0x13 }) {
         keypad.emitted.clear();
         keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 1, 0, (byte) event, 0 });
         assertEquals(1, keypad.emitted.size());
         assertEquals(KeyPadCapability.PanicPressedEvent.NAME, keypad.emitted.get(0).getMessageType());
      }
   }

   @Test
   public void aSupervisedArmPressIsAcknowledgedWithItsSessionIdThenHandled() {
      // [properties: status updates + session 3][length][Entry Control Notification][seq][type][arm away][no data]
      byte[] get = { (byte) 0x83, 6, CC_ENTRY_CONTROL, 0x01, 9, 0, 0x05, 0 };

      keypad.handleZWaveCommand(NODE, CC_SUPERVISION, (byte) 0x01, get);

      assertEquals(1, keypad.sent.size());
      assertArrayEquals(new byte[] { CC_SUPERVISION, 0x02, 3, (byte) 0xFF, 0 }, keypad.sent.get(0));
      assertEquals(1, keypad.emitted.size());
      assertEquals(KeyPadCapability.ArmPressedEvent.NAME, keypad.emitted.get(0).getMessageType());
      assertEquals(KeyPadCapability.ArmPressedEvent.MODE_ON, KeyPadCapability.ArmPressedEvent.getMode(keypad.emitted.get(0)));
   }

   @Test
   public void aChimePlaysDingDingFromTheSoundLevelProperty() {
      keypad.handleCommand(KeyPadCapability.ChimeRequest.instance());

      assertEquals(1, keypad.sent.size());
      assertArrayEquals(new byte[] { (byte) 0x87, 0x01, 0, 1, 0x60, 0x09, 0x32 }, keypad.sent.get(0));
   }

   @Test
   public void anExitDelaySendsACountdownNotAnAlarm() {
      keypad.handleCommand(KeyPadCapability.BeginArmingRequest.builder()
         .withAlarmMode(KeyPadCapability.BeginArmingRequest.ALARMMODE_ON)
         .withDelayInS(90)
         .build());

      assertEquals(1, keypad.sent.size());
      assertArrayEquals(new byte[] { (byte) 0x87, 0x01, 0, 2, 0x12, 0x06, 1, 0x12, 0x07, 30 }, keypad.sent.get(0));
   }

   @Test
   public void configurationReportsAreLeftForThePlatformDriver() {
      // Device settings live in the Groovy driver, so the hub must forward these
      boolean handled = keypad.handleZWaveCommand(NODE, CC_CONFIGURATION, (byte) 0x06, new byte[] { 6, 1, 4 });

      assertEquals(false, handled);
   }

   @Test
   public void proximityReportsStayOnTheHub() {
      boolean handled = keypad.handleZWaveCommand(NODE, CC_NOTIFICATION, (byte) 0x05,
         new byte[] { 0, 0, 0, (byte) 0xFF, 0x07, 0x08 });

      assertTrue(handled);
      assertTrue(keypad.emitted.isEmpty());
   }

   private static final class RecordingKeyPad extends RingKeyPad {
      private final List<byte[]> sent = new ArrayList<>();
      private final List<MessageBody> emitted = new ArrayList<>();

      private RecordingKeyPad() {
         super(null, Address.fromString("PROT:ZWAV-ABC-1234:AAAAAAAAAAAAAAAAAAAAAAAAAAA="));
      }

      @Override
      protected void zwaveSend(byte commandClassId, byte commandId, byte... payload) {
         byte[] frame = new byte[payload.length + 2];
         frame[0] = commandClassId;
         frame[1] = commandId;
         System.arraycopy(payload, 0, frame, 2, payload.length);
         sent.add(frame);
      }

      @Override
      public void emit(MessageBody msg) {
         emitted.add(msg);
      }

      @Override
      public void emit(MessageBody msg, java.util.UUID person) {
         emitted.add(msg);
      }
   }
}
