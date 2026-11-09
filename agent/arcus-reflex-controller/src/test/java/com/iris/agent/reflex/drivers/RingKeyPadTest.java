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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.MotionCapability;

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
   public void aDifferentCommandReusingASupervisionSessionIsActedOn() {
      // As after the keypad restarts, or its session counter wraps
      keypad.handleZWaveCommand(NODE, CC_SUPERVISION, (byte) 0x01, new byte[] { 4, 6, CC_ENTRY_CONTROL, 0x01, 9, 0, 0x11, 0 });
      keypad.handleZWaveCommand(NODE, CC_SUPERVISION, (byte) 0x01, new byte[] { 4, 6, CC_ENTRY_CONTROL, 0x01, 10, 0, 0x05, 0 });

      assertEquals(2, keypad.emitted.size());
      assertEquals(KeyPadCapability.PanicPressedEvent.NAME, keypad.emitted.get(0).getMessageType());
      assertEquals(KeyPadCapability.ArmPressedEvent.NAME, keypad.emitted.get(1).getMessageType());
   }

   @Test
   public void reservedBitsAroundTheDataTypeLeaveAnAsciiPinReadable() {
      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 1, (byte) 0xFE, 0x01, 4, '1', '2', '3', '4' });
      press(2, 0x03);

      assertEquals(KeyPadCapability.DisarmPressedEvent.NAME, lastEmitted().getMessageType());
   }

   @Test
   public void aDisarmWithNoPinIsRefusedWithoutCountingAsAnInvalidPin() {
      press(1, 0x03);

      assertTrue(keypad.emitted.isEmpty());
      assertArrayEquals(CODE_REJECTED, keypad.sent.get(0));
   }

   @Test
   public void anArmThatStartsLeavesNothingForAnotherKeypadsRefusal() {
      press(1, 0x05);
      keypad.handleCommand(KeyPadCapability.BeginArmingRequest.builder()
         .withAlarmMode(KeyPadCapability.BeginArmingRequest.ALARMMODE_ON).withDelayInS(0).build());
      keypad.sent.clear();
      keypad.emitted.clear();

      // Another keypad or the app is refused while this one is arming
      keypad.handleCommand(KeyPadCapability.ArmingUnavailableRequest.instance());
      press(2, 0x02);

      assertTrue(keypad.sent.isEmpty());
      assertTrue(keypad.emitted.isEmpty());
   }

   private static final byte[] CODE_REJECTED = { (byte) 0x87, 0x01, 0, 1, 0x09, 0x01, (byte) 0xFF };

   private static byte[] typedPin() {
      return new byte[] { 1, 2, 0x01, 4, '1', '2', '3', '4' };
   }

   private void press(int seq, int event) {
      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { (byte) seq, 2, (byte) event, 0 });
   }

   private void typePin() {
      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, typedPin());
   }

   private MessageBody lastEmitted() {
      return keypad.emitted.get(keypad.emitted.size() - 1);
   }

   @Test
   public void aPinTypedThenDisarmDisarms() {
      typePin();
      press(2, 0x03);

      assertEquals(KeyPadCapability.DisarmPressedEvent.NAME, lastEmitted().getMessageType());
   }

   @Test
   public void aPinUsedToArmCanNotThenDisarm() {
      typePin();
      press(2, 0x05);
      keypad.sent.clear();
      press(3, 0x03);

      assertEquals(KeyPadCapability.ArmPressedEvent.NAME, lastEmitted().getMessageType());
      assertArrayEquals(CODE_REJECTED, keypad.sent.get(0));
   }

   @Test
   public void aPinIsUsedForOneDisarmOnly() {
      typePin();
      press(2, 0x03);
      keypad.sent.clear();
      press(3, 0x03);

      assertEquals(1, keypad.emitted.size());
      assertEquals(KeyPadCapability.DisarmPressedEvent.NAME, lastEmitted().getMessageType());
      assertArrayEquals(CODE_REJECTED, keypad.sent.get(0));
   }

   @Test
   public void aPinTypedBeforeArmAttributesTheArmToThatPerson() {
      typePin();
      press(2, 0x05);

      assertEquals(RecordingKeyPad.PERSON, keypad.lastPerson);
   }

   @Test
   public void aSilentAlarmSendsTheKeypadNothing() {
      keypad.handleSetAttributes(ImmutableMap.<String, Object>of(KeyPadCapability.ATTR_ENABLEDSOUNDS, ImmutableSet.of("ARMED", "DISARMED")));

      keypad.handleCommand(KeyPadCapability.AlertingRequest.builder().withAlarmMode(KeyPadCapability.AlertingRequest.ALARMMODE_ON).build());

      assertTrue(keypad.sent.isEmpty());
   }

   @Test
   public void soundsOffSilencesThePrompts() {
      keypad.handleSetAttributes(ImmutableMap.<String, Object>of(KeyPadCapability.ATTR_ENABLEDSOUNDS, ImmutableSet.of()));

      keypad.handleCommand(KeyPadCapability.BeginArmingRequest.builder()
         .withAlarmMode(KeyPadCapability.BeginArmingRequest.ALARMMODE_ON).withDelayInS(30).build());
      keypad.handleCommand(KeyPadCapability.ArmedRequest.builder().withAlarmMode(KeyPadCapability.ArmedRequest.ALARMMODE_ON).build());
      keypad.handleCommand(KeyPadCapability.DisarmedRequest.instance());

      assertTrue(keypad.sent.isEmpty());
   }

   @Test
   public void anAudibleAlarmSoundsOnceHoweverItArrives() {
      keypad.handleSetAttributes(ImmutableMap.<String, Object>of(AlertCapability.ATTR_STATE, AlertCapability.STATE_ALERTING));
      keypad.handleCommand(KeyPadCapability.AlertingRequest.builder().withAlarmMode(KeyPadCapability.AlertingRequest.ALARMMODE_ON).build());

      assertEquals(1, keypad.sent.size());
      assertArrayEquals(new byte[] { (byte) 0x87, 0x01, 0, 1, 0x0D, 0x01, (byte) 0xFF }, keypad.sent.get(0));
   }

   @Test
   public void aRepeatedSupervisionGetIsReportedButNotActedOn() {
      byte[] panic = { 4, 6, CC_ENTRY_CONTROL, 0x01, 9, 0, 0x11, 0 };

      keypad.handleZWaveCommand(NODE, CC_SUPERVISION, (byte) 0x01, panic);
      keypad.handleZWaveCommand(NODE, CC_SUPERVISION, (byte) 0x01, panic);

      assertEquals(2, keypad.sent.size());
      assertEquals(1, keypad.emitted.size());
   }

   @Test
   public void aDisarmFromAnAlarmDoesNotSayArmed() {
      keypad.handleCommand(KeyPadCapability.AlertingRequest.builder().withAlarmMode(KeyPadCapability.AlertingRequest.ALARMMODE_ON).build());
      keypad.sent.clear();

      // The order ReflexLocalProcessing.onDisarmed delivers them in
      keypad.handleCommand(KeyPadCapability.DisarmedRequest.instance());
      keypad.handleSetAttributes(ImmutableMap.<String, Object>of(AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET));

      assertEquals(1, keypad.sent.size());
      assertArrayEquals(new byte[] { (byte) 0x87, 0x01, 0, 1, 0x02, 0x01, (byte) 0xFF }, keypad.sent.get(0));
   }

   @Test
   public void aRefusedArmOnAnotherKeypadOffersNothingHere() {
      keypad.handleCommand(KeyPadCapability.ArmingUnavailableRequest.instance());
      press(1, 0x02);

      assertTrue(keypad.sent.isEmpty());
      assertTrue(keypad.emitted.isEmpty());
   }

   @Test
   public void powerAppliedDoesNotMeanMains() {
      keypad.handleZWaveCommand(NODE, CC_NOTIFICATION, (byte) 0x05, new byte[] { 0, 0, 0, (byte) 0xFF, 0x08, 0x01 });

      assertTrue(keypad.attributes.isEmpty());
   }

   @Test
   public void acReconnectedMeansMains() {
      keypad.handleZWaveCommand(NODE, CC_NOTIFICATION, (byte) 0x05, new byte[] { 0, 0, 0, (byte) 0xFF, 0x08, 0x03 });

      assertEquals(DevicePowerCapability.SOURCE_LINE, keypad.attributes.get(0).get(DevicePowerCapability.ATTR_SOURCE));
   }

   @Test
   public void okAfterARefusedArmAsksToBypassInTheSameMode() {
      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 1, 2, 0x06, 0 });
      keypad.handleCommand(KeyPadCapability.ArmingUnavailableRequest.instance());
      keypad.emitted.clear();

      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 2, 2, 0x02, 0 });

      assertEquals(1, keypad.emitted.size());
      assertEquals(KeyPadCapability.ArmPressedEvent.NAME, keypad.emitted.get(0).getMessageType());
      assertEquals(KeyPadCapability.ArmPressedEvent.MODE_PARTIAL, KeyPadCapability.ArmPressedEvent.getMode(keypad.emitted.get(0)));
      assertEquals(Boolean.TRUE, KeyPadCapability.ArmPressedEvent.getBypass(keypad.emitted.get(0)));
   }

   @Test
   public void okOnlyAnswersOneRefusedArm() {
      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 1, 2, 0x05, 0 });
      keypad.handleCommand(KeyPadCapability.ArmingUnavailableRequest.instance());
      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 2, 2, 0x02, 0 });
      keypad.emitted.clear();

      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 3, 2, 0x02, 0 });

      assertTrue(keypad.emitted.isEmpty());
   }

   @Test
   public void okWithNothingOfferedDoesNotArm() {
      keypad.handleZWaveCommand(NODE, CC_ENTRY_CONTROL, (byte) 0x01, new byte[] { 1, 2, 0x02, 0 });

      assertTrue(keypad.emitted.isEmpty());
   }

   @Test
   public void aDisarmDeliveredTwiceIsSpokenOnce() {
      keypad.handleCommand(KeyPadCapability.ArmedRequest.builder()
         .withAlarmMode(KeyPadCapability.ArmedRequest.ALARMMODE_ON)
         .build());
      keypad.sent.clear();

      keypad.handleCommand(KeyPadCapability.DisarmedRequest.instance());
      keypad.handleCommand(KeyPadCapability.DisarmedRequest.instance());

      assertEquals(1, keypad.sent.size());
      assertArrayEquals(new byte[] { (byte) 0x87, 0x01, 0, 1, 0x02, 0x01, (byte) 0xFF }, keypad.sent.get(0));
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
   public void proximityIsReportedAsMotionOnlyWhenItChanges() {
      byte[] detected = { 0, 0, 0, (byte) 0xFF, 0x07, 0x08 };
      byte[] cleared = { 0, 0, 0, (byte) 0xFF, 0x07, 0x00 };

      assertTrue(keypad.handleZWaveCommand(NODE, CC_NOTIFICATION, (byte) 0x05, detected));
      keypad.handleZWaveCommand(NODE, CC_NOTIFICATION, (byte) 0x05, detected);
      keypad.handleZWaveCommand(NODE, CC_NOTIFICATION, (byte) 0x05, cleared);

      assertEquals(2, keypad.attributes.size());
      assertEquals(MotionCapability.MOTION_DETECTED, keypad.attributes.get(0).get(MotionCapability.ATTR_MOTION));
      assertEquals(MotionCapability.MOTION_NONE, keypad.attributes.get(1).get(MotionCapability.ATTR_MOTION));
   }

   @Test
   public void motionStaysOutOfTheHubCapabilities() {
      // AlarmSecurity refuses to arm while a hub device with Motion is triggered
      assertFalse(RingKeyPad.CAPS.contains(MotionCapability.NAME));
   }

   private static final class RecordingKeyPad extends RingKeyPad {
      private final List<byte[]> sent = new ArrayList<>();
      private final List<MessageBody> emitted = new ArrayList<>();
      private final List<Map<String, Object>> attributes = new ArrayList<>();
      private static final UUID PERSON = UUID.randomUUID();
      private UUID lastPerson;

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
      public void emit(Map<String, Object> attrs) {
         attributes.add(attrs);
      }

      @Override
      public void emit(MessageBody msg) {
         emitted.add(msg);
      }

      @Override
      public void emit(MessageBody msg, java.util.UUID person) {
         emitted.add(msg);
         lastPerson = person;
      }

      @Override
      public UUID verifyPinCode(String pin) {
         return "1234".equals(pin) ? PERSON : null;
      }
   }
}
