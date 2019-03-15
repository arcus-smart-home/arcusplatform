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

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.agent.reflex.ReflexController;
import com.iris.agent.util.RxIris;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.IdentifyCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.model.Version;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.alertme.AMGeneral;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.Constants;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.util.IrisAttributeLookup;
import com.iris.util.IrisUUID;

import rx.Observable;

public class AlertmeKeyPad extends AbstractZigbeeHubDriver {
   private static final Logger log = LoggerFactory.getLogger(CentraLiteKeyPad.class);

   private static final long PIN_VALIDITY_TIME = TimeUnit.SECONDS.toNanos(10);
   private static final long UPDATE_KEYPAD_STATE_DWELL_TIME = TimeUnit.SECONDS.toNanos(5);

   private static final double VOLTS_NOMINAL = 3.0;
   private static final double VOLTS_MINIMUM = 2.1;

   public static final String DRIVER_NAME = "ZBAlertMeKeyPad";
   public static final Version VERSION_2_4 = Version.fromRepresentation("2.4");

   public static final byte ENDPOINT = (byte)1;

   public static final Set<String> CAPS = ImmutableSet.of(
      KeyPadCapability.NAME,
      AlertCapability.NAME
   );

   private static final int AME_ATTR_KEYPADSTATE = 0x0020;
   private static final int AME_ATTR_PIN = 0x0021;
   private static final int AME_ATTR_ACTIONKEYPRESS = 0x0022;
   private static final int AME_ATTR_ACTIONKEYRELEASE = 0x0023;
   private static final int AME_ATTR_HUBPOLLRATE = 0x0024;
   private static final int AME_ATTR_SOUNDS_MASK = 0x0025;
   private static final int AME_ATTR_SOUNDID = 0x0026;
   private static final int AME_ATTR_UNSUCCESSFUL_STATECHANGE = 0x0028;

   private static final byte NO_REPEAT = (byte)0x00;
   private static final byte VOL_MAX = (byte)0xFF;
   private static final byte VOL_MID = (byte)0x80;
   private static final byte VOL_LOW = (byte)0x40;
   private static final byte VOL_OFF = (byte)0x00;

   private static final byte SOUNDID_CUSTOM = 0x00;
   private static final byte SOUNDID_KEYCLICK = 0x01;
   private static final byte SOUNDID_LOSTHUB = 0x02;
   private static final byte SOUNDID_ARMING = 0x03;
   private static final byte SOUNDID_ARMED = 0x04;
   private static final byte SOUNDID_HOME = 0x05;
   private static final byte SOUNDID_NIGHT = 0x06;
   private static final byte SOUNDID_ALARM = 0x07;
   private static final byte SOUNDID_PANIC = 0x08;
   private static final byte SOUNDID_BADPIN = 0x09;
   private static final byte SOUNDID_OPENDOOR = 0x0A;
   private static final byte SOUNDID_LOCKED = 0x0B;

   private static final byte KEYPADSTATE_UNKNOWN = 0;
   private static final byte KEYPADSTATE_OFF = 1;
   private static final byte KEYPADSTATE_ARMED_ON = 2;
   private static final byte KEYPADSTATE_ARMED_PARTIAL = 3;
   private static final byte KEYPADSTATE_PANIC = 4;
   private static final byte KEYPADSTATE_ARMING_ON = 5;
   private static final byte KEYPADSTATE_SOAKING_ON = 5;
   private static final byte KEYPADSTATE_ALARMING_ON = 6;
   private static final byte KEYPADSTATE_ARMING_PARTIAL = 7;
   private static final byte KEYPADSTATE_SOAKING_PARTIAL = 7;
   private static final byte KEYPADSTATE_ALARMING_PARTIAL = 8;

   private static final int SOUNDIDX_CUSTOM                     = 0x0100;
   private static final int SOUNDIDX_KEYCLICK                   = 0x0200;
   private static final int SOUNDIDX_LOSTHUB                    = 0x0400;
   private static final int SOUNDIDX_ARMING                     = 0x0800;
   private static final int SOUNDIDX_ARMED                      = 0x1000;
   private static final int SOUNDIDX_HOME                       = 0x2000;
   private static final int SOUNDIDX_NIGHT                      = 0x4000;
   private static final int SOUNDIDX_ALARM                      = 0x8000;
   private static final int SOUNDIDX_PANIC                      = 0x0001;
   private static final int SOUNDIDX_BADPIN                     = 0x0002;
   private static final int SOUNDIDX_OPENDOOR                   = 0x0004;
   private static final int SOUNDIDX_LOCKED                     = 0x0008;

   private static final int[] SOUNDMASK = new int[] {
      SOUNDIDX_CUSTOM,
      SOUNDIDX_KEYCLICK,
      SOUNDIDX_LOSTHUB,
      SOUNDIDX_ARMING,
      SOUNDIDX_ARMED,
      SOUNDIDX_HOME,
      SOUNDIDX_NIGHT,
      SOUNDIDX_ALARM,
      SOUNDIDX_PANIC,
      SOUNDIDX_BADPIN,
      SOUNDIDX_OPENDOOR,
      SOUNDIDX_LOCKED
   };

   private static final Variable<String> KEYPAD_ALARMSTATE = attribute(DRIVER_NAME, VERSION_2_4, KeyPadCapability.ATTR_ALARMSTATE, String.class, KeyPadCapability.ALARMSTATE_DISARMED);
   private static final Variable<String> KEYPAD_ALARMMODE = attribute(DRIVER_NAME, VERSION_2_4, KeyPadCapability.ATTR_ALARMMODE, String.class, KeyPadCapability.ALARMMODE_ON);
   private static final Variable<String> KEYPAD_ALARMSOUNDER = attribute(DRIVER_NAME, VERSION_2_4, KeyPadCapability.ATTR_ALARMSOUNDER, String.class, KeyPadCapability.ALARMSOUNDER_ON);
   private static final Variable<Set> KEYPAD_ENABLEDSOUNDS = attribute(DRIVER_NAME, VERSION_2_4, KeyPadCapability.ATTR_ENABLEDSOUNDS, Set.class, new LinkedHashSet(Arrays.asList("BUTTONS", "DISARMED", "ARMED", "ARMING", "SOAKING", "ALERTING")));

   private static final Variable<String> ALERT_STATE = attribute(DRIVER_NAME, VERSION_2_4, AlertCapability.ATTR_STATE, String.class, AlertCapability.STATE_QUIET);
   private static final Variable<Integer> ALERT_MAXALERTSECS = attribute(DRIVER_NAME, VERSION_2_4, AlertCapability.ATTR_MAXALERTSECS, Integer.class, null);

   private long panelStatusEndTime = Long.MIN_VALUE;
   private String code = "";
   private long lastCodeTime = System.nanoTime();

   private boolean keypadStateUpdateRequested = false;
   private long lastKeypadStateTime = System.nanoTime();

   private int lastBattery = Integer.MIN_VALUE;
   private int lastSignal = Integer.MIN_VALUE;

   public AlertmeKeyPad(ReflexController parent, Address addr) {
      super(parent, addr);
   }

   @Override
   public boolean isOffline() {
      return true;
      // return parent.zigbee().isOffline(getAddress());
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
      return VERSION_2_4;
   }

   @Override
   public String getDriver() {
      return DRIVER_NAME;
   }

   @Override
   public Version getVersion() {
      return VERSION_2_4;
   }

   @Override
   public String getHash() {
      return "";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Standard Keypad Message Retries
   /////////////////////////////////////////////////////////////////////////////
   
   private RxIris.Retry standardRetry() {
      return RxIris.retry()
         .attempts(10)
         .initial(0, TimeUnit.SECONDS)
         .delay(1, TimeUnit.SECONDS)
         .max(5, TimeUnit.SECONDS)
         .build();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver Lifecycle
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   public void start() {
      log.info("starting up hub alertme keypad driver: {}", addr);

      List<Observable<?>> setupTasks = new ArrayList<>(3);
      setupTasks.add(verifySoundMask());
      setupTasks.add(verifyKeypadState());
      subscribeAndLogResults(log, "alertme keypad setup", Observable.concat(setupTasks));
   }

   private Observable<?> verifyKeypadState() {
      return verifyAttribute(AME_ATTR_KEYPADSTATE);
   }

   private Observable<?> verifySoundMask() {
      return verifyAttribute(AME_ATTR_SOUNDS_MASK);
   }

   private Observable<?> verifyAttribute(int attr) {
      ProtocMessage req = General.ZclReadAttributes.builder()
         .setAttributes(new short[] { (short)attr })
         .create();

      return zcl(AME_PROFILE_ID, AME_ENDPOINT_ID, AME_ATTR_CLUSTER_ID, req, false, false, false)
         .retryWhen(standardRetry());
   }

   /////////////////////////////////////////////////////////////////////////////
   // Platform Message Handling
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void handleSetAttributes(Map<String,Object> attrs) {
      boolean writeKeypadState = false;
      Set enabledSounds = null;
      for (Map.Entry<String,Object> attr : attrs.entrySet()) {
         Object value = attr.getValue();
         if (value == null) {
            continue;
         }

         try {
            value = IrisAttributeLookup.coerce(attr.getKey(), value);
         } catch (Exception ex) {
            log.warn("could not coerce attribute to correct type: ", ex);
         }

         switch (attr.getKey()) {
         case KeyPadCapability.ATTR_ALARMSTATE:
            writeKeypadState |= set(KEYPAD_ALARMSTATE, value.toString());
            break;
         case KeyPadCapability.ATTR_ALARMMODE:
            writeKeypadState |= set(KEYPAD_ALARMMODE, value.toString());
            break;
         case KeyPadCapability.ATTR_ALARMSOUNDER:
            switch (value.toString()) {
            case KeyPadCapability.ALARMSOUNDER_ON:
               enabledSounds = new LinkedHashSet(Arrays.asList("BUTTONS", "DISARMED", "ARMED", "ARMING", "SOAKING", "ALERTING"));
               break;
            case KeyPadCapability.ALARMSOUNDER_OFF:
               enabledSounds = new LinkedHashSet();
               break;
            default:
               // ignore
               break;
            }
            break;
         case KeyPadCapability.ATTR_ENABLEDSOUNDS:
            enabledSounds = new LinkedHashSet((Collection)value);
            break;
         case AlertCapability.ATTR_STATE:
            set(ALERT_STATE, value.toString());

            //sync the alarm state with the alert state
            if (value.toString().equals(AlertCapability.STATE_QUIET) && get(KEYPAD_ALARMSTATE).equals(KeyPadCapability.ALARMSTATE_ALERTING)) {
               if (get(KEYPAD_ALARMMODE).equals(KeyPadCapability.ALARMMODE_OFF)) {
                  writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_DISARMED);
               } else {
                  writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMED);
               }
            } else if (value.toString().equals(AlertCapability.STATE_ALERTING) && !get(KEYPAD_ALARMSTATE).equals(KeyPadCapability.ALARMSTATE_ALERTING)) {
               writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ALERTING);
            }

            break;
         case AlertCapability.ATTR_MAXALERTSECS:
            set(ALERT_MAXALERTSECS, ((Number)value).intValue());
            break;
         default:
            // ignore
            break;
         }
      }

      if (enabledSounds != null) {
         boolean writeSoundMask = set(KEYPAD_ENABLEDSOUNDS, enabledSounds);
         if (get(KEYPAD_ENABLEDSOUNDS).isEmpty()) {
            set(KEYPAD_ALARMSOUNDER, KeyPadCapability.ALARMSOUNDER_OFF);
         } else {
            set(KEYPAD_ALARMSOUNDER, KeyPadCapability.ALARMSOUNDER_ON);
         }

         if (writeSoundMask && !writeKeypadState) {
            writeSoundMask();
         }
      }

      if (writeKeypadState) {
         writeKeypadState();
      }
   }
   
   @Override
   protected void handleCommand(MessageBody msg) {
      boolean writeKeypadState = false;
      switch (msg.getMessageType()) {
      case KeyPadCapability.BeginArmingRequest.NAME:
         setRemainingTime(KeyPadCapability.BeginArmingRequest.getDelayInS(msg));
         writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMING);
         writeKeypadState |= set(KEYPAD_ALARMMODE, KeyPadCapability.BeginArmingRequest.getAlarmMode(msg));
         break;
      case KeyPadCapability.ArmedRequest.NAME:
         clearRemainingTime();
         writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMED);
         writeKeypadState |= set(KEYPAD_ALARMMODE, KeyPadCapability.ArmedRequest.getAlarmMode(msg));
         break;
      case KeyPadCapability.DisarmedRequest.NAME:
         clearRemainingTime();
         writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_DISARMED);
         writeKeypadState |= set(KEYPAD_ALARMMODE, KeyPadCapability.ALARMMODE_OFF);
         break;
      case KeyPadCapability.SoakingRequest.NAME:
         setRemainingTime(KeyPadCapability.SoakingRequest.getDurationInS(msg));
         writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_SOAKING);
         writeKeypadState |= set(KEYPAD_ALARMMODE, KeyPadCapability.SoakingRequest.getAlarmMode(msg));
         break;
      case KeyPadCapability.AlertingRequest.NAME:
         clearRemainingTime();
         String mode = KeyPadCapability.AlertingRequest.getAlarmMode(msg);
         writeKeypadState |= set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ALERTING);
         writeKeypadState |= set(KEYPAD_ALARMMODE, "PANIC".equals(mode) ? KeyPadCapability.ALARMMODE_OFF : mode);
         break;
      case KeyPadCapability.ArmingUnavailableRequest.NAME:
         writeStateChangeFailed();
         break;
      case IdentifyCapability.IdentifyRequest.NAME:
      case KeyPadCapability.ChimeRequest.NAME:
         playSound(SOUNDID_HOME, (byte)0x00, VOL_MAX);
         break;
      default:
         // ignore
         break;
      }

      if (writeKeypadState) {
         writeKeypadState();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Translate protocol messages into platform messages
   /////////////////////////////////////////////////////////////////////////////

   protected void handlePanic(String code) {
      UUID user = verifyPinCode(code);
      log.info("panic at keypad: user={}", user == null ? null : IrisUUID.toString(user));

      emit(KeyPadCapability.PanicPressedEvent.instance(), user);
   }

   protected boolean handleArmAll(String code) {
      UUID user = verifyPinCode(code);
      log.info("arm all at keypad: user={}", user == null ? null : IrisUUID.toString(user));

      emit(
         KeyPadCapability.ArmPressedEvent.builder()
            .withMode(KeyPadCapability.ArmPressedEvent.MODE_ON)
            .withBypass(false)
            .build(),
         user
      );

      return true;
   }

   protected boolean handleArmPartial(String code) {
      UUID user = verifyPinCode(code);
      log.info("arm partial at keypad: user={}", user == null ? null : IrisUUID.toString(user));

      emit(
         KeyPadCapability.ArmPressedEvent.builder()
            .withMode(KeyPadCapability.ArmPressedEvent.MODE_PARTIAL)
            .withBypass(false)
            .build(),
         user
      );
      return true;
   }

   protected boolean handleDisarm(String code) {
      UUID user = verifyPinCode(code);
      if (user != null) {
         log.info("disarm at keypad: user={}", IrisUUID.toString(user));
         emit(
            KeyPadCapability.DisarmPressedEvent.instance(),
            user
         );

         return true;
      } else {
         log.info("disarm at keypad failed: could not verify pin");
         if (!KeyPadCapability.ALARMSTATE_DISARMED.equals(get(KEYPAD_ALARMSTATE))) {
            writeStateChangeFailed();
         }

         emit(
            KeyPadCapability.InvalidPinEnteredEvent.instance()
         );

         return false;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Sending Protocol Messsages to the Device
   /////////////////////////////////////////////////////////////////////////////

   public void aggregatePinCode(@Nullable String code) {
      if (code == null || code.isEmpty()) {
         return;
      }

      this.lastCodeTime = System.nanoTime();
      this.code = this.code + code;
      if (code.length() > 4) {
         this.code = code.substring(code.length()-4);
      }
   }

   public void clearPinCode() {
      this.code = "";
   }

   public String getPinCode() {
      long elapsed = System.nanoTime() - lastCodeTime;
      if (elapsed > PIN_VALIDITY_TIME) {
         return "";
      }

      String result = code;

      code = "";
      return result;
   }

   public int getRemainingTime() {
      long time = panelStatusEndTime;
      if (time < 0) {
         return 0;
      }

      long now = System.currentTimeMillis();
      long remaining = Math.max(0,time-now);
      return (int)Math.ceil(remaining / 1000.0);
   }

   public void clearRemainingTime() {
      panelStatusEndTime = Long.MIN_VALUE;
   }

   public void setRemainingTime(@Nullable Integer time) {
      panelStatusEndTime = (time == null)
         ? Long.MIN_VALUE
         : System.currentTimeMillis() + (1000*time);
   }

   int getSoundMask() {
      return getSoundMask(get(KEYPAD_ENABLEDSOUNDS), get(KEYPAD_ALARMSTATE), get(KEYPAD_ALARMMODE));
   }

   int getSoundMask(Set<String> enabledSounds, String alarmState, String alarmMode) {
      int mask = 0;
      if (enabledSounds.contains("BUTTONS")) {
         mask |= SOUNDIDX_CUSTOM ;
         mask |= SOUNDIDX_KEYCLICK ;
         mask |= SOUNDIDX_LOSTHUB ;
         mask |= SOUNDIDX_BADPIN ;
         mask |= SOUNDIDX_OPENDOOR ;
         mask |= SOUNDIDX_LOCKED ;
      }

      if (enabledSounds.contains("DISARMED")) {
         mask |= SOUNDIDX_HOME ;
      }

      if (enabledSounds.contains("ARMED")) {
         mask |= SOUNDIDX_ARMED ;
         mask |= SOUNDIDX_NIGHT ;
      }

      if((enabledSounds.contains("ARMING") && KeyPadCapability.ALARMSTATE_ARMING.equals(alarmState)) ||
         (enabledSounds.contains("SOAKING") && KeyPadCapability.ALARMSTATE_SOAKING.equals(alarmState))) {
         mask |= SOUNDIDX_ARMING ;
      }

      if (enabledSounds.contains("ALERTING")) {
         mask |= SOUNDIDX_PANIC ;
         mask |= SOUNDIDX_ALARM ;

         // the state transition sounds must be enabled to properly disable the alert siren
         mask |= SOUNDIDX_HOME ;
         mask |= SOUNDIDX_ARMED ;
         mask |= SOUNDIDX_NIGHT ;
      }

      return mask;
   }

   private byte getKeypadStateCode() {
      return getKeypadStateCode(get(KEYPAD_ALARMSTATE), get(KEYPAD_ALARMMODE));
   }

   private byte getKeypadStateCode(@Nullable String alarmState, @Nullable String alarmMode) {
      String state = alarmState == null
         ? KeyPadCapability.ALARMSTATE_DISARMED
         : alarmState;

      String mode = alarmMode == null
         ? KeyPadCapability.ALARMMODE_OFF
         : alarmMode;

      boolean isPartial = KeyPadCapability.ALARMMODE_PARTIAL.equals(mode);
      switch(state) {
      case KeyPadCapability.ALARMSTATE_DISARMED:
         return KEYPADSTATE_OFF;

      case KeyPadCapability.ALARMSTATE_ARMING:
         return isPartial ? KEYPADSTATE_ARMING_PARTIAL : KEYPADSTATE_ARMING_ON;

      case KeyPadCapability.ALARMSTATE_ARMED:
         return isPartial ? KEYPADSTATE_ARMED_PARTIAL : KEYPADSTATE_ARMED_ON;

      case KeyPadCapability.ALARMSTATE_SOAKING:
         return isPartial ? KEYPADSTATE_SOAKING_PARTIAL : KEYPADSTATE_SOAKING_ON;

      case KeyPadCapability.ALARMSTATE_ALERTING:
         switch(mode) {
         case KeyPadCapability.ALARMMODE_OFF: return KEYPADSTATE_PANIC;
         case KeyPadCapability.ALARMMODE_PARTIAL: return KEYPADSTATE_ALARMING_PARTIAL;
         case KeyPadCapability.ALARMMODE_ON: return KEYPADSTATE_ALARMING_ON;
         default: // fall through
         }
         // fall through

      default:
         return KEYPADSTATE_UNKNOWN;
      }
   }

   private void writeKeypadState() {
      writeKeypadState(false, get(KEYPAD_ALARMSTATE), get(KEYPAD_ALARMMODE));
   }

   private void writeKeypadStateResponse() {
      writeKeypadState(true, get(KEYPAD_ALARMSTATE), get(KEYPAD_ALARMMODE));
   }

   private void writeKeypadState(boolean isPollResponse, String alarmState, String alarmMode) {
      byte code = getKeypadStateCode(alarmState, alarmMode);
      int soundMask = getSoundMask(get(KEYPAD_ENABLEDSOUNDS), alarmState, alarmMode);
      writeKeypadState(isPollResponse, code, soundMask);
   }

   private void writeKeypadState(boolean isPollResponse, byte code, int soundMask) {
      long now = System.nanoTime();
      if (isPollResponse) {
         long elapsed = now - lastKeypadStateTime;
         if (elapsed <= UPDATE_KEYPAD_STATE_DWELL_TIME) {
            log.trace("alertme keypad ignoring first device poll because state was recently written");
            lastKeypadStateTime = 0;
            return;
         }
      } else {
         lastKeypadStateTime = now;
      }

      if (code == KEYPADSTATE_PANIC || code == KEYPADSTATE_ALARMING_ON || code == KEYPADSTATE_ALARMING_PARTIAL) {
         set(ALERT_STATE, AlertCapability.STATE_ALERTING);
      } else {
         set(ALERT_STATE, AlertCapability.STATE_QUIET);
      }

      int sm = soundMask;
      if ((KEYPADSTATE_ARMED_PARTIAL == code) || (KEYPADSTATE_ARMED_ON == code)) {
         sm |= ( SOUNDIDX_ARMED | SOUNDIDX_HOME | SOUNDIDX_NIGHT);
      }


      short rsm = Short.reverseBytes((short)sm);
      General.ZclWriteAttributeRecord[] recs = new General.ZclWriteAttributeRecord[] {
         General.ZclWriteAttributeRecord.builder()
            .setAttributeIdentifier(AME_ATTR_SOUNDS_MASK)
            .setAttributeData(ZclData.builder().set16BitBitmap(rsm).create())
            .create(),
         General.ZclWriteAttributeRecord.builder()
            .setAttributeIdentifier(AME_ATTR_KEYPADSTATE)
            .setAttributeData(ZclData.builder().set8BitEnum(code).create())
            .create()
      };

      ProtocMessage req = General.ZclWriteAttributesNoResponse.builder()
         .setAttributes(recs)
         .create();

      zcl(AME_PROFILE_ID, AME_ENDPOINT_ID, AME_ATTR_CLUSTER_ID, req, false, false, false)
         .retryWhen(standardRetry())
         .subscribe(RxIris.SWALLOW_ALL);
   }

   private void writeSoundMask() {
      writeSoundMask(get(KEYPAD_ALARMSTATE), get(KEYPAD_ALARMMODE));
   }

   private void writeSoundMask(String alarmState, String alarmMode) {
      byte code = getKeypadStateCode(alarmState, alarmMode);
      int soundMask = getSoundMask(get(KEYPAD_ENABLEDSOUNDS), alarmState, alarmMode);
      writeSoundMask(code, soundMask);
   }

   private void writeSoundMask(byte code, int soundMask) {
      int sm = soundMask;
      if ((KEYPADSTATE_ARMED_PARTIAL == code) || (KEYPADSTATE_ARMED_ON == code)) {
         sm |= ( SOUNDIDX_ARMED | SOUNDIDX_HOME | SOUNDIDX_NIGHT);
      }


      short rsm = Short.reverseBytes((short)sm);
      General.ZclWriteAttributeRecord[] recs = new General.ZclWriteAttributeRecord[] {
         General.ZclWriteAttributeRecord.builder()
            .setAttributeIdentifier(AME_ATTR_SOUNDS_MASK)
            .setAttributeData(ZclData.builder().set16BitBitmap(rsm).create())
            .create(),
      };

      ProtocMessage req = General.ZclWriteAttributesNoResponse.builder()
         .setAttributes(recs)
         .create();

      zcl(AME_PROFILE_ID, AME_ENDPOINT_ID, AME_ATTR_CLUSTER_ID, req, false, false, false)
         .retryWhen(standardRetry())
         .subscribe(RxIris.SWALLOW_ALL);
   }

   private void writeStateChangeFailed() {
      writeStateChangeFailed(get(KEYPAD_ALARMSTATE), get(KEYPAD_ALARMMODE));
   }

   private void writeStateChangeFailed(String alarmState, String alarmMode) {
      byte code = getKeypadStateCode(alarmState, alarmMode);
      General.ZclWriteAttributeRecord[] recs = new General.ZclWriteAttributeRecord[] {
         General.ZclWriteAttributeRecord.builder()
            .setAttributeIdentifier(AME_ATTR_UNSUCCESSFUL_STATECHANGE)
            .setAttributeData(ZclData.builder().set8BitEnum(code).create())
            .create()
      };

      ProtocMessage req = General.ZclWriteAttributesNoResponse.builder()
         .setAttributes(recs)
         .create();

      zcl(AME_PROFILE_ID, AME_ENDPOINT_ID, AME_ATTR_CLUSTER_ID, req, false, false, false)
         .retryWhen(standardRetry())
         .subscribe(RxIris.SWALLOW_ALL);
   }

   private void playSound(int soundId, int repeatCount, int volume) {
      int mask = getSoundMask();
      int snd = ((volume & 0xFF) << 16) | ((repeatCount & 0xFF) << 8) | (soundId & 0xFF);
      General.ZclWriteAttributeRecord[] recs;
      if( (mask & SOUNDMASK[soundId]) == 0 ) {
         int tempMask = mask | SOUNDMASK[soundId];
         recs = new General.ZclWriteAttributeRecord[] {
            General.ZclWriteAttributeRecord.builder()
               .setAttributeIdentifier(AME_ATTR_SOUNDS_MASK)
               .setAttributeData(ZclData.builder().set16BitBitmap((short)tempMask).create())
               .create(),
            General.ZclWriteAttributeRecord.builder()
               .setAttributeIdentifier(AME_ATTR_SOUNDID)
               .setAttributeData(ZclData.builder().set24Bit(snd).create())
               .create(),
            General.ZclWriteAttributeRecord.builder()
               .setAttributeIdentifier(AME_ATTR_SOUNDS_MASK)
               .setAttributeData(ZclData.builder().set16BitBitmap((short)mask).create())
               .create(),
         };
      } else {
         recs = new General.ZclWriteAttributeRecord[] {
            General.ZclWriteAttributeRecord.builder()
               .setAttributeIdentifier(AME_ATTR_SOUNDID)
               .setAttributeData(ZclData.builder().set24Bit(snd).create())
               .create()
         };
      }

      ProtocMessage req = General.ZclWriteAttributesNoResponse.builder()
         .setAttributes(recs)
         .create();

      zcl(AME_PROFILE_ID, AME_ENDPOINT_ID, AME_ATTR_CLUSTER_ID, req, false, false, false)
         .retryWhen(standardRetry())
         .subscribe(RxIris.SWALLOW_ALL);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Receive Protocol Messages from the Device
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void finishAttributesUpdated() {
      if (keypadStateUpdateRequested) {
         keypadStateUpdateRequested = false;
         writeKeypadState();
      }
   }
   
   @Override
   protected boolean handleAttributeUpdated(short profile, byte endpoint, short cluster, short id, ZclData data) {
      switch (id & 0xFFFF) {
      case AME_ATTR_KEYPADSTATE:
         byte kps = ((Number)data.getDataValue()).byteValue();
         if (getKeypadStateCode() != kps) {
            log.info("alertme keypad state did not match, updating: id={}, data={}, ({})", ProtocUtil.toHexString(id), data.getDataValue(), data.getDataValue().getClass());
            keypadStateUpdateRequested = true;
         }
         break;
      case AME_ATTR_SOUNDS_MASK:
         int sm = Short.reverseBytes(((Number)data.getDataValue()).shortValue()) & 0xFFFF;
         if (getSoundMask() != sm) {
            log.info("alertme keypad sound mask did not match, updating: id={}, data={} ({})", ProtocUtil.toHexString(id), data.getDataValue(), data.getDataValue().getClass());
            keypadStateUpdateRequested = true;
         }
         break;
      case AME_ATTR_ACTIONKEYPRESS:
         int prKeyTime = ((Number)data.getDataValue()).shortValue() & 0xFFFF;
         char prKey = (char)(prKeyTime >> 8);
         int prSec = (prKeyTime & 0xFF);
         if (prSec == 0) {
            try {
               switch (prKey) {
               case 'H':   // disarm
                  handleDisarm(getPinCode());
                  break;
               case 'A':   // on
                  handleArmAll(getPinCode());
                  break;
               case 'N':   // partial
                  handleArmPartial(getPinCode());
                  break;
               case 'P':   // panic
                  handlePanic(getPinCode());
                  break;
               default:
                  // ignore
                  break;
               }
            } finally {
               clearPinCode();
            }
         }
         break;
      case AME_ATTR_ACTIONKEYRELEASE:
         // ignore
         break;
      case AME_ATTR_PIN:
         aggregatePinCode((String)data.getDataValue());
         break;
      default:
         log.trace("unknown attribute updated: id={}, data={}", ProtocUtil.toHexString(id), data);
         break;
      }

      return false;
   }

   @Override
   protected boolean handleReadAttributes(ZigbeeMessage.Zcl zcl) {
      String alarmState = get(KEYPAD_ALARMSTATE);
      switch (alarmState) {
      case KeyPadCapability.ALARMSTATE_ARMING:
      case KeyPadCapability.ALARMSTATE_SOAKING:
         // ignore polling while arming or soaking since
         // these polls make the sound stutter.
         log.trace("alertme keypad ignoring device poll in arming state");
         break;

      default:
         writeKeypadStateResponse();
      }

      return true;
   }
   
   protected boolean handleAlertmeLifesignFromServer(ZigbeeMessage.Zcl zcl) {
      AMGeneral.Lifesign ls = AMGeneral.Lifesign.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());

      ImmutableMap.Builder<String,Object> bld = ImmutableMap.builder();
      if ((ls.getStatusFlags() & AMGeneral.Lifesign.LIFESIGN_HAS_VOLTAGE) != 0) {
         int milliVolts = ls.getPsuVoltage();
         double volts = milliVolts / 1000.0;
         double battery = Math.max(0,Math.min(100,(100.0 / (VOLTS_NOMINAL - VOLTS_MINIMUM) * (volts - VOLTS_MINIMUM))));

         int bat = (((int)Math.round(battery))/5)*5;
         if (lastBattery != bat) {
            lastBattery = bat;
            bld.put(DevicePowerCapability.ATTR_BATTERY, bat);
         }
      }

      if ((ls.getStatusFlags() & AMGeneral.Lifesign.LIFESIGN_HAS_LQI) != 0) {
         int lqi = ls.getLqi();
         double signal = Math.max(0.0, Math.min(100.0,(lqi * 100.0) / 255.0));

         int sig = (((int)Math.round(signal)/5))*5;
         if (lastSignal != sig) {
            lastSignal = sig;
            bld.put(DeviceConnectionCapability.ATTR_SIGNAL, sig);
         }
      }

      emit(bld.build());
      return true;
   }

   protected boolean handleAlertmeGeneralFromServer(ZigbeeMessage.Zcl zcl) {
      switch (zcl.getZclMessageId()) {
      case AMGeneral.Lifesign.ID:
         return handleAlertmeLifesignFromServer(zcl);
      default:
         zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_CLUSTER_COMMAND);
         return false;
      }
   }
   
   @Override
   protected boolean handleZclClusterSpecificFromServer(ZigbeeMessage.Zcl zcl) {
      if (zcl.rawProfileId() == AME_PROFILE_ID && zcl.rawEndpoint() == AME_ENDPOINT_ID && zcl.rawClusterId() == AMGeneral.CLUSTER_ID) {
         return handleAlertmeGeneralFromServer(zcl);
      }

      return super.handleZclClusterSpecificFromServer(zcl);
   }
}

