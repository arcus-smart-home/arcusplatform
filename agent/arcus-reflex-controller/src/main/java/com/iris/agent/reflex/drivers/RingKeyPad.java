/*
 * Copyright 2020 Arcus Project
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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.agent.reflex.ReflexController;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.IdentifyCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.model.Version;
import com.iris.util.IrisAttributeLookup;
import com.iris.util.IrisUUID;

public class RingKeyPad extends AbstractZWaveHubDriver {
   private static final Logger log = LoggerFactory.getLogger(RingKeyPad.class);

   public static final String DRIVER_NAME = "ZWRingKeypad";
   public static final Version VERSION_1_0 = Version.fromRepresentation("1.0");

   public static final Set<String> CAPS = ImmutableSet.of(
      KeyPadCapability.NAME,
      AlertCapability.NAME
   );

   // Z-Wave Entry Control command class (0x6F = 111)
   private static final byte CC_ENTRY_CONTROL     = (byte) 0x6F;
   private static final byte EC_CMD_NOTIFICATION   = (byte) 0x01;
   private static final byte EC_CMD_CONFIG_SET     = (byte) 0x06;

   // Z-Wave Supervision command class (0x6C = 108)
   private static final byte CC_SUPERVISION        = (byte) 0x6C;
   private static final byte SUPERVISION_GET        = (byte) 0x01;
   private static final byte SUPERVISION_REPORT     = (byte) 0x02;
   private static final byte SUPERVISION_STATUS_SUCCESS = (byte) 0xFF;

   // Entry Control event types (Z-Wave Entry Control CC spec, SDS13891)
   private static final int EC_EVENT_CACHING_STARTED = 0x00;
   private static final int EC_EVENT_CACHING_ENDED   = 0x01;
   private static final int EC_EVENT_ENTER           = 0x02;
   private static final int EC_EVENT_DISARM          = 0x03;
   private static final int EC_EVENT_ARM_ALL         = 0x04;
   private static final int EC_EVENT_ARM_AWAY        = 0x05;
   private static final int EC_EVENT_ARM_HOME        = 0x06;
   private static final int EC_EVENT_EXIT_DELAY      = 0x07;
   private static final int EC_EVENT_FIRE            = 0x10;
   private static final int EC_EVENT_POLICE          = 0x11;
   private static final int EC_EVENT_ALERT_PANIC     = 0x12;
   private static final int EC_EVENT_ALERT_MEDICAL   = 0x13;
   private static final int EC_EVENT_CANCEL          = 0x19;

   // Entry Control data types
   private static final int EC_DATATYPE_ASCII = 0x02;

   // Z-Wave Battery command class (0x80)
   private static final byte CC_BATTERY       = (byte) 0x80;
   private static final byte BATTERY_REPORT   = (byte) 0x03;

   // Z-Wave Notification/Alarm command class (0x71)
   private static final byte CC_NOTIFICATION   = (byte) 0x71;
   private static final byte NOTIF_REPORT      = (byte) 0x05;

   // Notification types
   private static final int NOTIF_TYPE_HOME_SECURITY    = 0x07;
   private static final int NOTIF_TYPE_POWER_MANAGEMENT = 0x08;
   private static final int NOTIF_TYPE_SYSTEM           = 0x09;

   // Power management events (Notification CC event ids)
   private static final int POWER_EVENT_AC_DISCONNECTED   = 0x02;
   private static final int POWER_EVENT_AC_RECONNECTED    = 0x03;
   private static final int POWER_EVENT_REPLACE_BATTERY_SOON = 0x0A;
   private static final int POWER_EVENT_REPLACE_BATTERY_NOW  = 0x0B;
   private static final byte NOTIF_GET = (byte) 0x04;

   // Indicator CC V3 indicator ids (SDS13781 table, as zwave-js lists them). The
   // keypad only understands V3 indicator objects; a V1 Indicator Set value means
   // nothing to it. The ids drive both the keypad's lights and its voice prompts.
   private static final byte IND_DISARMED        = 0x02;   // Not armed / disarmed
   private static final byte IND_CODE_REJECTED   = 0x09;   // Code not accepted
   private static final byte IND_ARMED_STAY      = 0x0A;
   private static final byte IND_ARMED_AWAY      = 0x0B;
   private static final byte IND_ALARM_BURGLAR   = 0x0D;   // Alarming: Burglar
   private static final byte IND_BYPASS_NEEDED   = 0x10;   // Bypass challenge (sensors open)
   private static final byte IND_ENTRY_DELAY     = 0x11;
   private static final byte IND_EXIT_DELAY      = 0x12;
   private static final byte IND_NODE_IDENTIFY   = 0x50;
   private static final byte IND_CHIME           = 0x60;   // Generic sound 1: the keypad's "ding ding"

   // Volume for sounds, 10 to 100 on the keypad
   private static final byte CHIME_VOLUME        = 0x50;

   // Indicator CC V3 property ids
   private static final byte PROP_MULTILEVEL      = 0x01;
   private static final byte PROP_ON_OFF_PERIOD   = 0x03;
   private static final byte PROP_ON_OFF_CYCLES   = 0x04;
   private static final byte PROP_ON_TIME         = 0x05;
   private static final byte PROP_TIMEOUT_MINUTES = 0x06;
   private static final byte PROP_TIMEOUT_SECONDS = 0x07;
   private static final byte PROP_SOUND_LEVEL     = 0x09;

   // State variables
   private static final Variable<String> KEYPAD_ALARMSTATE = attribute(DRIVER_NAME, VERSION_1_0, KeyPadCapability.ATTR_ALARMSTATE, String.class, KeyPadCapability.ALARMSTATE_DISARMED);
   private static final Variable<String> KEYPAD_ALARMMODE = attribute(DRIVER_NAME, VERSION_1_0, KeyPadCapability.ATTR_ALARMMODE, String.class, KeyPadCapability.ALARMMODE_ON);
   private static final Variable<String> KEYPAD_ALARMSOUNDER = attribute(DRIVER_NAME, VERSION_1_0, KeyPadCapability.ATTR_ALARMSOUNDER, String.class, KeyPadCapability.ALARMSOUNDER_ON);
   private static final Variable<Set> KEYPAD_ENABLEDSOUNDS = attribute(DRIVER_NAME, VERSION_1_0, KeyPadCapability.ATTR_ENABLEDSOUNDS, Set.class, new LinkedHashSet(Arrays.asList("BUTTONS", "DISARMED", "ARMED", "ARMING", "SOAKING", "ALERTING")));

   private static final Variable<String> ALERT_STATE = attribute(DRIVER_NAME, VERSION_1_0, AlertCapability.ATTR_STATE, String.class, AlertCapability.STATE_QUIET);
   private static final Variable<Integer> ALERT_MAXALERTSECS = attribute(DRIVER_NAME, VERSION_1_0, AlertCapability.ATTR_MAXALERTSECS, Integer.class, null);

   // Cached PIN from Entry Control caching
   private String cachedPin = "";
   private long cachedPinTimestamp = 0;
   // Long enough to reach a button after the digits, and no longer: anyone can press Disarm
   private static final long PIN_CACHE_TIMEOUT_MS = 10 * 1000;

   // Remaining time tracking for arming/soaking delays
   private long panelStatusEndTime = Long.MIN_VALUE;

   // After a refused arm the keypad highlights its OK button, which then asks to
   // arm with bypass. The offer lapses so a stray OK later can't arm the alarm.
   private static final long BYPASS_OFFER_MS = 30 * 1000;
   private static final long ARM_PRESS_WINDOW_MS = 10 * 1000;
   private @Nullable String lastArmMode;
   private long lastArmPressAt;
   private @Nullable String bypassOfferMode;
   private long bypassOfferedAt;

   // The keypad repeats a Supervision Get whose report it missed, with the same session id
   private static final long SUPERVISION_REPEAT_MS = 60 * 1000;
   private int lastSupervisionSession = -1;
   private long lastSupervisionAt;

   // The alarm reaches this driver as alert:state and as AlertingRequest, in either
   // order relative to the sound settings, and the keypad should only sound once
   private boolean alertIndicatorSent;

   // Motion is reported to the platform, but stays out of CAPS on purpose. AlarmSecurity
   // refuses to arm while a hub device with Motion is triggered, whether or not it
   // participates, and anyone arming at the keypad is standing in front of this one.
   private static final int HOME_SECURITY_IDLE   = 0x00;
   private static final int HOME_SECURITY_MOTION = 0x08;
   private @Nullable String lastMotion;

   public RingKeyPad(ReflexController parent, Address addr) {
      super(parent, addr);
   }

   @Override
   public boolean isOffline() {
      return parent.zwave().isOffline(getAddress());
   }

   @Override
   public Set<String> getCapabilities() {
      return CAPS;
   }

   @Override
   public String getDriverName() {
      return DRIVER_NAME;
   }

   @Override
   public Version getDriverVersion() {
      return VERSION_1_0;
   }

   @Override
   public String getDriver() {
      return DRIVER_NAME;
   }

   @Override
   public Version getVersion() {
      return VERSION_1_0;
   }

   @Override
   public String getHash() {
      return "";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver Lifecycle
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public void start() {
      log.info("starting ring keypad hub driver: {}", addr);

      // Configure Entry Control to cache up to 4 digits with a 5-second timeout
      zwaveSend(CC_ENTRY_CONTROL, EC_CMD_CONFIG_SET, (byte) 0x04, (byte) 0x05);

      // Request initial battery level
      sendBatteryGet();
   }

   @Override
   protected void doOnConnected() {
      log.info("ring keypad driver onconnected: {}", addr);

      // Re-configure Entry Control on reconnect
      zwaveSend(CC_ENTRY_CONTROL, EC_CMD_CONFIG_SET, (byte) 0x04, (byte) 0x05);

      // Request battery level
      sendBatteryGet();

      // The keypad only reports power changes, so ask whether it is on mains now
      zwaveSend(CC_NOTIFICATION, NOTIF_GET, (byte) 0x00, (byte) NOTIF_TYPE_POWER_MANAGEMENT, (byte) 0x00);

      // Sync indicator to current alarm state
      updateIndicator();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Platform Message Handling
   /////////////////////////////////////////////////////////////////////////////

   @Override
   protected void handleSetAttributes(Map<String, Object> attrs) {
      boolean sendIndicator = false;
      for (Map.Entry<String, Object> attr : attrs.entrySet()) {
         Object value = attr.getValue();
         try {
            value = IrisAttributeLookup.coerce(attr.getKey(), value);
         } catch (Exception ex) {
            log.warn("could not coerce attribute to correct type: ", ex);
         }

         switch (attr.getKey()) {
         case KeyPadCapability.ATTR_ALARMSTATE:
            if (value != null) {
               sendIndicator |= set(KEYPAD_ALARMSTATE, value.toString());
            }
            break;
         case KeyPadCapability.ATTR_ALARMMODE:
            if (value != null) {
               sendIndicator |= set(KEYPAD_ALARMMODE, value.toString());
            }
            break;
         case KeyPadCapability.ATTR_ALARMSOUNDER:
            if (value != null) {
               set(KEYPAD_ALARMSOUNDER, value.toString());
               switch (value.toString()) {
               case KeyPadCapability.ALARMSOUNDER_ON:
                  set(KEYPAD_ENABLEDSOUNDS, new LinkedHashSet(Arrays.asList("BUTTONS", "DISARMED", "ARMED", "ARMING", "SOAKING", "ALERTING")));
                  break;
               case KeyPadCapability.ALARMSOUNDER_OFF:
                  set(KEYPAD_ENABLEDSOUNDS, new LinkedHashSet());
                  break;
               default:
                  break;
               }
            }
            break;
         case KeyPadCapability.ATTR_ENABLEDSOUNDS:
            if (value != null) {
               set(KEYPAD_ENABLEDSOUNDS, new LinkedHashSet((Collection) value));
               if (get(KEYPAD_ENABLEDSOUNDS).isEmpty()) {
                  set(KEYPAD_ALARMSOUNDER, KeyPadCapability.ALARMSOUNDER_OFF);
               } else {
                  set(KEYPAD_ALARMSOUNDER, KeyPadCapability.ALARMSOUNDER_ON);
               }
            }
            break;
         case AlertCapability.ATTR_STATE:
            set(ALERT_STATE, value.toString());

            // Sync the alarm state with the alert state
            if (value.toString().equals(AlertCapability.STATE_QUIET) && get(KEYPAD_ALARMSTATE).equals(KeyPadCapability.ALARMSTATE_ALERTING)) {
               alertIndicatorSent = false;
               if (get(KEYPAD_ALARMMODE).equals(KeyPadCapability.ALARMMODE_OFF)) {
                  set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_DISARMED);
               } else {
                  set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMED);
               }
               sendIndicator = true;
            } else if (value.toString().equals(AlertCapability.STATE_ALERTING) && !get(KEYPAD_ALARMSTATE).equals(KeyPadCapability.ALARMSTATE_ALERTING)) {
               set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ALERTING);
               sendIndicator = true;
            }
            break;
         case AlertCapability.ATTR_MAXALERTSECS:
            set(ALERT_MAXALERTSECS, ((Number) value).intValue());
            break;
         default:
            break;
         }
      }

      if (sendIndicator) {
         updateIndicator();
      }
   }

   @Override
   protected void handleCommand(MessageBody msg) {
      switch (msg.getMessageType()) {
      case KeyPadCapability.BeginArmingRequest.NAME:
         Integer exitDelay = KeyPadCapability.BeginArmingRequest.getDelayInS(msg);
         setRemainingTime(exitDelay);
         bypassOfferMode = null;
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMING);
         set(KEYPAD_ALARMMODE, KeyPadCapability.BeginArmingRequest.getAlarmMode(msg));
         if (soundEnabled(KeyPadCapability.ENABLEDSOUNDS_ARMING)) {
            sendCountdownIndicator(IND_EXIT_DELAY, exitDelay);
         }
         break;
      case KeyPadCapability.ArmedRequest.NAME:
         clearRemainingTime();
         clearBypassOffer();
         alertIndicatorSent = false;
         String armedMode = KeyPadCapability.ArmedRequest.getAlarmMode(msg);
         // The keypad speaks every indicator, so only a change is sent
         boolean armedChanged = set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMED);
         armedChanged |= set(KEYPAD_ALARMMODE, armedMode);
         if (armedChanged && soundEnabled(KeyPadCapability.ENABLEDSOUNDS_ARMED)) {
            setIndicator(KeyPadCapability.ALARMMODE_PARTIAL.equals(armedMode) ? IND_ARMED_STAY : IND_ARMED_AWAY);
         }
         break;
      case KeyPadCapability.DisarmedRequest.NAME:
         clearRemainingTime();
         clearBypassOffer();
         alertIndicatorSent = false;
         // The hub alarm delivers this more than once for a single disarm
         boolean disarmedChanged = set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_DISARMED);
         disarmedChanged |= set(KEYPAD_ALARMMODE, KeyPadCapability.ALARMMODE_OFF);
         if (disarmedChanged && soundEnabled(KeyPadCapability.ENABLEDSOUNDS_DISARMED)) {
            setIndicator(IND_DISARMED);
         }
         break;
      case KeyPadCapability.SoakingRequest.NAME:
         Integer entryDelay = KeyPadCapability.SoakingRequest.getDurationInS(msg);
         setRemainingTime(entryDelay);
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_SOAKING);
         set(KEYPAD_ALARMMODE, KeyPadCapability.SoakingRequest.getAlarmMode(msg));
         clearBypassOffer();
         if (soundEnabled(KeyPadCapability.ENABLEDSOUNDS_SOAKING)) {
            sendCountdownIndicator(IND_ENTRY_DELAY, entryDelay);
         }
         break;
      case KeyPadCapability.AlertingRequest.NAME:
         clearRemainingTime();
         String alertMode = KeyPadCapability.AlertingRequest.getAlarmMode(msg);
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ALERTING);
         set(KEYPAD_ALARMMODE, "PANIC".equals(alertMode) ? KeyPadCapability.ALARMMODE_OFF : alertMode);
         clearBypassOffer();
         sendAlertIndicator();
         break;
      case KeyPadCapability.ArmingUnavailableRequest.NAME:
         // The hub tells every keypad; only the one that was just pressed has anything to offer
         if (lastArmMode != null && System.currentTimeMillis() - lastArmPressAt < ARM_PRESS_WINDOW_MS) {
            bypassOfferMode = lastArmMode;
            bypassOfferedAt = System.currentTimeMillis();
            lastArmMode = null;
            if (soundEnabled(KeyPadCapability.ENABLEDSOUNDS_ARMING)) {
               setIndicator(IND_BYPASS_NEEDED);
            }
         }
         break;
      case IdentifyCapability.IdentifyRequest.NAME:
         sendIdentifyBlink();
         break;
      case KeyPadCapability.ChimeRequest.NAME:
         // the keypad starts its sounds from the sound level property alone
         sendIndicatorSetV3(IND_CHIME, PROP_SOUND_LEVEL, CHIME_VOLUME);
         break;
      default:
         break;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Z-Wave Protocol Message Handling
   /////////////////////////////////////////////////////////////////////////////

   @Override
   protected boolean handleZWaveCommand(byte nodeId, byte commandClassId, byte commandId, byte[] payload) {
      int cc = commandClassId & 0xFF;
      int cmd = commandId & 0xFF;

      switch (cc) {
      case 0x6F: // Entry Control
         if (cmd == 0x01) { // notification
            handleEntryControlNotification(payload);
            return true;
         }
         break;
      case 0x6C: // Supervision
         if (cmd == 0x01) { // Supervision Get
            handleSupervisionGet(nodeId, payload);
            return true;
         }
         break;
      case 0x80: // Battery
         if (cmd == 0x03) { // report
            handleBatteryReport(payload);
            return true;
         }
         break;
      case 0x71: // Notification/Alarm
         if (cmd == 0x05) { // report
            handleNotificationReport(payload);
            return true;
         }
         break;
      default:
         break;
      }

      log.trace("unhandled zwave command: cc=0x{}, cmd=0x{}", Integer.toHexString(cc), Integer.toHexString(cmd));
      return false;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Supervision CC Handling
   /////////////////////////////////////////////////////////////////////////////

   private void handleSupervisionGet(byte nodeId, byte[] payload) {
      // Supervision Get payload (Supervision CC v1):
      // [0] = properties: bit 7 status updates requested, bit 6 reserved, bits 0-5 session id
      // [1] = encapsulated command length, counting its command class and command bytes
      // [2] = encapsulated command class
      // [3] = encapsulated command
      // [4..] = encapsulated command payload
      if (payload.length < 4) {
         log.warn("supervision get too short: {} bytes", payload.length);
         return;
      }

      int sessionId = payload[0] & 0x3F;
      int encapLen  = payload[1] & 0xFF;
      long now = System.currentTimeMillis();
      boolean repeat = sessionId == lastSupervisionSession && now - lastSupervisionAt < SUPERVISION_REPEAT_MS;
      lastSupervisionSession = sessionId;
      lastSupervisionAt = now;
      int encapCC   = payload[2] & 0xFF;
      int encapCmd  = payload[3] & 0xFF;

      log.info("supervision get: sessionId={}, encapCC=0x{}, encapCmd=0x{}, encapLen={}",
         sessionId, String.format("%02X", encapCC), String.format("%02X", encapCmd), encapLen);

      // Send Supervision Report: session id in the low six bits, status=0xFF (success),
      // duration=0x00. A report with another session id is ignored and the keypad resends.
      zwaveSend(CC_SUPERVISION, SUPERVISION_REPORT,
         (byte) sessionId, SUPERVISION_STATUS_SUCCESS, (byte) 0x00);

      // Report again, since it is the report that went missing, but act only once:
      // a repeated Disarm has already used the PIN, and a repeated panic is a second alarm
      if (repeat) {
         return;
      }

      // The encapsulated payload follows its command class and command bytes
      int innerLen = Math.max(0, Math.min(encapLen - 2, payload.length - 4));
      byte[] innerPayload = new byte[innerLen];
      System.arraycopy(payload, 4, innerPayload, 0, innerLen);
      handleZWaveCommand(nodeId, (byte) encapCC, (byte) encapCmd, innerPayload);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Entry Control Handling
   /////////////////////////////////////////////////////////////////////////////

   private void handleEntryControlNotification(byte[] payload) {
      // Entry Control Notification payload format:
      // [0] = sequence number
      // [1] = data type (upper nibble) + event type (lower nibble) -- varies by version
      // Actually per the Z-Wave spec for Entry Control CC v1:
      // [0] = sequence number
      // [1] = data type
      // [2] = event type
      // [3] = data length
      // [4..] = data bytes
      if (payload.length < 4) {
         log.warn("entry control notification too short: {} bytes", payload.length);
         return;
      }

      int sequenceNumber = payload[0] & 0xFF;
      int dataType = payload[1] & 0xFF;
      int eventType = payload[2] & 0xFF;
      int dataLength = payload[3] & 0xFF;

      // The data bytes hold the PIN, so they must never reach the log
      log.info("entry control: seq={}, dataType={}, eventType=0x{}, dataLen={}",
         sequenceNumber, dataType, String.format("%02X", eventType), dataLength);

      // Extract PIN from data bytes
      String pin = extractPin(payload, dataType, dataLength);

      switch (eventType) {
      case EC_EVENT_ARM_AWAY:
         log.info("ring keypad: arm away pressed, pin={}", pin != null && !pin.isEmpty() ? "yes" : "no");
         handleArm(pin, KeyPadCapability.ArmPressedEvent.MODE_ON, false);
         break;
      case EC_EVENT_ARM_HOME:
         log.info("ring keypad: arm home pressed, pin={}", pin != null && !pin.isEmpty() ? "yes" : "no");
         handleArm(pin, KeyPadCapability.ArmPressedEvent.MODE_PARTIAL, false);
         break;
      case EC_EVENT_ARM_ALL:
         log.info("ring keypad: arm all pressed, pin={}", pin != null && !pin.isEmpty() ? "yes" : "no");
         handleArm(pin, KeyPadCapability.ArmPressedEvent.MODE_ON, false);
         break;
      case EC_EVENT_ENTER:
         log.info("ring keypad: enter pressed, pin={}", pin != null && !pin.isEmpty() ? "yes" : "no");
         String bypassMode = takeBypassOffer();
         if (bypassMode != null) {
            handleArm(pin, bypassMode, true);
         } else if (pin != null && !pin.isEmpty()) {
            handleDisarm(pin);
         }
         break;
      case EC_EVENT_DISARM:
         log.info("ring keypad: disarm pressed, pin={}", pin != null && !pin.isEmpty() ? "yes" : "no");
         handleDisarm(pin);
         break;
      case EC_EVENT_FIRE:
      case EC_EVENT_POLICE:
      case EC_EVENT_ALERT_PANIC:
      case EC_EVENT_ALERT_MEDICAL:
         // keypad:PanicPressed is the only panic event a keypad has
         log.info("ring keypad: panic button held, event=0x{}", String.format("%02X", eventType));
         emit(KeyPadCapability.PanicPressedEvent.instance());
         break;
      case EC_EVENT_CANCEL:
         log.info("ring keypad: cancel pressed");
         cachedPin = "";
         break;
      case EC_EVENT_CACHING_STARTED:
         log.debug("ring keypad: pin entry started");
         break;
      case EC_EVENT_CACHING_ENDED:
         log.info("ring keypad: pin cached, length={}", dataLength);
         if (pin != null && !pin.isEmpty()) {
            cachedPin = pin;
            cachedPinTimestamp = System.currentTimeMillis();
         }
         break;
      default:
         log.info("ring keypad: unknown entry control event: 0x{}, dataType={}, dataLen={}",
            String.format("%02X", eventType), dataType, dataLength);
         break;
      }
   }

   private String extractPin(byte[] payload, int dataType, int dataLength) {
      if (dataLength <= 0 || payload.length < 4 + dataLength) {
         return "";
      }

      byte[] pinBytes = new byte[dataLength];
      System.arraycopy(payload, 4, pinBytes, 0, dataLength);

      if (dataType == EC_DATATYPE_ASCII) {
         return new String(pinBytes, StandardCharsets.US_ASCII);
      } else {
         // Raw bytes - convert to digit string
         StringBuilder sb = new StringBuilder();
         for (byte b : pinBytes) {
            sb.append(Integer.toString(b & 0xFF));
         }
         return sb.toString();
      }
   }

   private @Nullable String takeBypassOffer() {
      String mode = bypassOfferMode;
      bypassOfferMode = null;
      return System.currentTimeMillis() - bypassOfferedAt < BYPASS_OFFER_MS ? mode : null;
   }

   private void clearBypassOffer() {
      bypassOfferMode = null;
      lastArmMode = null;
   }

   /**
    * The PIN typed just before a button, which arrives ahead of the button press.
    * It goes with that press whatever the press was, so it can't disarm later.
    */
   private String takeCachedPin() {
      String pin = System.currentTimeMillis() - cachedPinTimestamp < PIN_CACHE_TIMEOUT_MS ? cachedPin : "";
      cachedPin = "";
      return pin;
   }

   private void handleArm(String pin, String mode, boolean bypass) {
      lastArmMode = mode;
      lastArmPressAt = System.currentTimeMillis();

      String cached = takeCachedPin();
      String actorPin = (pin != null && !pin.isEmpty()) ? pin : cached;

      // Arm does not require PIN validation, but if provided, set the actor
      UUID user = null;
      if (!actorPin.isEmpty()) {
         user = verifyPinCode(actorPin);
      }

      log.info("arm at keypad: mode={}, bypass={}, user={}", mode, bypass, user == null ? null : IrisUUID.toString(user));

      emit(
         KeyPadCapability.ArmPressedEvent.builder()
            .withMode(mode)
            .withBypass(bypass)
            .build(),
         user
      );
   }

   private void handleDisarm(String pin) {
      // Use provided PIN, or fall back to cached PIN, which is used up either way
      String cached = takeCachedPin();
      if (pin == null || pin.isEmpty()) {
         pin = cached;
      }

      if (pin.isEmpty()) {
         log.warn("disarm at keypad failed: no pin provided");
         emit(KeyPadCapability.InvalidPinEnteredEvent.instance());
         signalCodeRejected();
         return;
      }

      UUID user = verifyPinCode(pin);
      if (user != null) {
         log.info("disarm at keypad: user={}", IrisUUID.toString(user));
         emit(
            KeyPadCapability.DisarmPressedEvent.instance(),
            user
         );
      } else {
         log.warn("disarm at keypad failed: could not verify pin");
         emit(KeyPadCapability.InvalidPinEnteredEvent.instance());
         signalCodeRejected();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Battery Handling
   /////////////////////////////////////////////////////////////////////////////

   private void handleBatteryReport(byte[] payload) {
      if (payload.length < 1) {
         return;
      }

      int level = payload[0] & 0xFF;
      if (level == 0xFF) {
         // 0xFF means battery low
         level = 1;
      } else if (level > 100) {
         level = 100;
      }

      log.debug("ring keypad battery: {}%", level);
      emit(ImmutableMap.of(DevicePowerCapability.ATTR_BATTERY, level));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Notification/Alarm Handling
   /////////////////////////////////////////////////////////////////////////////

   private void handleNotificationReport(byte[] payload) {
      // Notification Report v4+ payload:
      // [0] = alarm type (legacy)
      // [1] = alarm level (legacy)
      // [2] = reserved
      // [3] = notification status
      // [4] = notification type
      // [5] = event
      // [6..] = event parameters
      if (payload.length < 6) {
         log.trace("notification report too short: {} bytes", payload.length);
         return;
      }

      int notificationType = payload[4] & 0xFF;
      int event = payload[5] & 0xFF;

      log.debug("ring keypad notification: type={}, event={}", notificationType, event);

      switch (notificationType) {
      case NOTIF_TYPE_POWER_MANAGEMENT:
         switch (event) {
         case POWER_EVENT_AC_RECONNECTED:
            log.info("ring keypad: AC power connected");
            emit(ImmutableMap.of(DevicePowerCapability.ATTR_SOURCE, DevicePowerCapability.SOURCE_LINE));
            break;
         case POWER_EVENT_AC_DISCONNECTED:
            log.info("ring keypad: running on battery");
            emit(ImmutableMap.of(DevicePowerCapability.ATTR_SOURCE, DevicePowerCapability.SOURCE_BATTERY));
            break;
         case POWER_EVENT_REPLACE_BATTERY_SOON:
         case POWER_EVENT_REPLACE_BATTERY_NOW:
            log.warn("ring keypad: battery low");
            sendBatteryGet();
            break;
         default:
            log.trace("ring keypad: unhandled power event: {}", event);
            break;
         }
         break;
      case NOTIF_TYPE_HOME_SECURITY:
         if (event == HOME_SECURITY_MOTION) {
            reportMotion(MotionCapability.MOTION_DETECTED);
         } else if (event == HOME_SECURITY_IDLE) {
            reportMotion(MotionCapability.MOTION_NONE);
         } else {
            log.trace("ring keypad: home security event: {}", event);
         }
         break;
      case NOTIF_TYPE_SYSTEM:
         log.trace("ring keypad: system notification event: {}", event);
         break;
      default:
         log.trace("ring keypad: unhandled notification type: {}", notificationType);
         break;
      }
   }

   private void reportMotion(String motion) {
      if (!motion.equals(lastMotion)) {
         lastMotion = motion;
         emit(ImmutableMap.of(MotionCapability.ATTR_MOTION, motion, MotionCapability.ATTR_MOTIONCHANGED, new Date()));
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Indicator Management
   /////////////////////////////////////////////////////////////////////////////

   private boolean soundEnabled(String sound) {
      Set<?> sounds = get(KEYPAD_ENABLEDSOUNDS);
      return sounds == null || sounds.contains(sound);
   }

   private void signalCodeRejected() {
      if (soundEnabled(KeyPadCapability.ENABLEDSOUNDS_BUTTONS)) {
         setIndicator(IND_CODE_REJECTED);
      }
   }

   /**
    * Sounds the keypad's alarm once. A silent alarm takes ALERTING out of the
    * enabled sounds, and must send nothing: this indicator is loud enough to warn
    * an intruder, and the keypad has no silent form of it that we know of.
    */
   private void sendAlertIndicator() {
      if (!alertIndicatorSent && soundEnabled(KeyPadCapability.ENABLEDSOUNDS_ALERTING)) {
         alertIndicatorSent = true;
         setIndicator(IND_ALARM_BURGLAR);
      }
   }

   private void updateIndicator() {
      String alarmState = get(KEYPAD_ALARMSTATE);
      String alarmMode = get(KEYPAD_ALARMMODE);

      if (KeyPadCapability.ALARMSTATE_ALERTING.equals(alarmState)) {
         sendAlertIndicator();
      } else if (KeyPadCapability.ALARMSTATE_ARMED.equals(alarmState)) {
         if (soundEnabled(KeyPadCapability.ENABLEDSOUNDS_ARMED)) {
            setIndicator(KeyPadCapability.ALARMMODE_PARTIAL.equals(alarmMode) ? IND_ARMED_STAY : IND_ARMED_AWAY);
         }
      } else if (!KeyPadCapability.ALARMSTATE_ARMING.equals(alarmState) && !KeyPadCapability.ALARMSTATE_SOAKING.equals(alarmState)) {
         // During a delay the remaining time is unknown here; the next state refreshes the keypad
         if (soundEnabled(KeyPadCapability.ENABLEDSOUNDS_DISARMED)) {
            setIndicator(IND_DISARMED);
         }
      }
   }

   /**
    * Turns an indicator on: one V3 object with the multilevel property at 0xFF.
    */
   private void setIndicator(byte indicatorId) {
      sendIndicatorSetV3(indicatorId, PROP_MULTILEVEL, (byte) 0xFF);
   }

   /**
    * Starts an indicator's countdown, as zwave-js does for its "timeout" value:
    * minutes in property 0x06 and seconds in property 0x07. Nothing is sent for
    * a zero duration, since the state that follows carries its own indicator.
    */
   private void sendCountdownIndicator(byte indicatorId, @Nullable Integer durationSecs) {
      if (durationSecs == null || durationSecs <= 0) {
         return;
      }
      int minutes = Math.min(durationSecs / 60, 255);
      int seconds = durationSecs % 60;
      sendIndicatorSetV3(
         indicatorId, PROP_TIMEOUT_MINUTES, (byte) minutes,
         indicatorId, PROP_TIMEOUT_SECONDS, (byte) seconds
      );
   }

   /**
    * Blinks the keypad to identify it: Node Identify with a 1 s on/off period,
    * three cycles, half of each period on.
    */
   private void sendIdentifyBlink() {
      sendIndicatorSetV3(
         IND_NODE_IDENTIFY, PROP_ON_OFF_PERIOD, (byte) 0x0A,
         IND_NODE_IDENTIFY, PROP_ON_OFF_CYCLES, (byte) 0x03,
         IND_NODE_IDENTIFY, PROP_ON_TIME, (byte) 0x05
      );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Time Tracking Helpers
   /////////////////////////////////////////////////////////////////////////////

   private void clearRemainingTime() {
      panelStatusEndTime = Long.MIN_VALUE;
   }

   private void setRemainingTime(@Nullable Integer time) {
      panelStatusEndTime = (time == null)
         ? Long.MIN_VALUE
         : System.currentTimeMillis() + (1000L * time);
   }
}
