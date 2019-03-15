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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.agent.reflex.ReflexController;
import com.iris.agent.util.Backoff;
import com.iris.agent.util.Backoffs;
import com.iris.agent.util.RxIris;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.IdentifyCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.model.Version;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.Constants;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.zcl.IasAce;
import com.iris.protocol.zigbee.zcl.IasZone;
import com.iris.protocol.zigbee.zcl.Ota;
import com.iris.protocol.zigbee.zcl.PollControl;
import com.iris.protocol.zigbee.zcl.Power;
import com.iris.protocol.zigbee.zcl.TemperatureMeasurement;
import com.iris.util.IrisAttributeLookup;
import com.iris.util.IrisUUID;

import rx.Observable;

public class GreatStarKeyPad extends AbstractZigbeeHubDriver {
   private static final Logger log = LoggerFactory.getLogger(GreatStarKeyPad.class);
   private static enum ActiveRequestType { NONE, ARM, PARTIAL, DISARM }

   public static final String DRIVER_NAME = "ZBGreatStarKeyPad";
   public static final Version VERSION_2_12 = Version.fromRepresentation("2.12");

   public static final byte ENDPOINT = (byte)1;

   public static final Set<String> CAPS = ImmutableSet.of(
      KeyPadCapability.NAME,
      AlertCapability.NAME
   );

   private static final Variable<Boolean> BOUND = variable(DRIVER_NAME, VERSION_2_12, "bound", Boolean.class, false);
   private static final Variable<Boolean> CIE_WRITTEN = variable(DRIVER_NAME, VERSION_2_12, "ciewr", Boolean.class, false);
   private static final Variable<Boolean> ENROLLED = variable(DRIVER_NAME, VERSION_2_12, "znenrl", Boolean.class, false);
   private static final Variable<Boolean> CONF_POWER = variable(DRIVER_NAME, VERSION_2_12, "crpwr2", Boolean.class, false);
   private static final Variable<Boolean> CONF_TEMP = variable(DRIVER_NAME, VERSION_2_12, "crtemp2", Boolean.class, false);
   private static final Variable<Boolean> CONF_CHECKIN = variable(DRIVER_NAME, VERSION_2_12, "wapcci", Boolean.class, false);

   private static final Variable<String> KEYPAD_ALARMSTATE = attribute(DRIVER_NAME, VERSION_2_12, KeyPadCapability.ATTR_ALARMSTATE, String.class, KeyPadCapability.ALARMSTATE_DISARMED);
   private static final Variable<String> KEYPAD_ALARMMODE = attribute(DRIVER_NAME, VERSION_2_12, KeyPadCapability.ATTR_ALARMMODE, String.class, KeyPadCapability.ALARMMODE_ON);
   private static final Variable<String> KEYPAD_ALARMSOUNDER = attribute(DRIVER_NAME, VERSION_2_12, KeyPadCapability.ATTR_ALARMSOUNDER, String.class, KeyPadCapability.ALARMSOUNDER_ON);
   private static final Variable<Set> KEYPAD_ENABLEDSOUNDS = attribute(DRIVER_NAME, VERSION_2_12, KeyPadCapability.ATTR_ENABLEDSOUNDS, Set.class, new LinkedHashSet(Arrays.asList("BUTTONS", "DISARMED", "ARMED", "ARMING", "SOAKING", "ALERTING")));

   private static final Variable<String> ALERT_STATE = attribute(DRIVER_NAME, VERSION_2_12, AlertCapability.ATTR_STATE, String.class, AlertCapability.STATE_QUIET);
   private static final Variable<Integer> ALERT_MAXALERTSECS = attribute(DRIVER_NAME, VERSION_2_12, AlertCapability.ATTR_MAXALERTSECS, Integer.class, null);

   private ActiveRequestType activeRequestType = ActiveRequestType.NONE;
   private @Nullable ZigbeeMessage.Zcl activeRequest;
   private long panelStatusEndTime = Long.MIN_VALUE;

   private int lastBatteryPercent = Integer.MIN_VALUE;
   private int lastTemperatureValue = Integer.MIN_VALUE;

   public GreatStarKeyPad(ReflexController parent, Address addr) {
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
      return VERSION_2_12;
   }

   @Override
   public String getDriver() {
      return DRIVER_NAME;
   }

   @Override
   public Version getVersion() {
      return VERSION_2_12;
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
      log.info("starting up hub greatstar keypad driver: {}", addr);

      List<Observable<?>> setupTasks = new ArrayList<>(6);
      setupTasks.add(bind());
      setupTasks.add(writecie());
      setupTasks.add(enroll());
      setupTasks.add(configurePower());
      setupTasks.add(configureTemperature());
      setupTasks.add(configureCheckin());
      subscribeAndLogResults(log, "greatstar keypad setup", Observable.concatDelayError(setupTasks));
   }
   
   @Override
   protected void doOnConnected() {
      log.info("greatstar keypad driver onconnected: {}", addr);

      set(CONF_CHECKIN, false);
      set(CONF_POWER, false);
      set(CONF_TEMP, false);

      List<Observable<?>> setupTasks = new ArrayList<>(6);
      setupTasks.add(bind());
      setupTasks.add(writecie());
      setupTasks.add(enroll());
      setupTasks.add(configurePower());
      setupTasks.add(configureTemperature());
      setupTasks.add(configureCheckin());
      subscribeAndLogResults(log, "greatstar keypad onconnected", Observable.concatDelayError(setupTasks));
   }
   
   private Observable<?> bind() {
      if (get(BOUND)) {
         return Observable.empty();
      }

      final Backoff backoff = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .random(0.10)
         .max(60, TimeUnit.MINUTES)
         .build();

      return markDoneOnComplete(BOUND, true, bindAndExpectSuccess(
         new Binding(HA_PROFILE_ID, ENDPOINT, PollControl.CLUSTER_ID, true),
         new Binding(HA_PROFILE_ID, ENDPOINT, Power.CLUSTER_ID, true),
         new Binding(HA_PROFILE_ID, ENDPOINT, TemperatureMeasurement.CLUSTER_ID, true),
         new Binding(HA_PROFILE_ID, ENDPOINT, IasZone.CLUSTER_ID, true))
         .retryWhen(RxIris.retry(backoff))
      );
   }

   private Observable<?> writecie() {
      if (get(CIE_WRITTEN)) {
         return Observable.empty();
      }

      final Backoff backoff = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .random(0.10)
         .max(60, TimeUnit.MINUTES)
         .build();

      return markDoneOnComplete(CIE_WRITTEN, true, write(HA_PROFILE_ID, ENDPOINT, IasZone.CLUSTER_ID, ImmutableMap.of(
         IasZone.ATTR_IAS_CIE_ADDRESS, ZclData.builder()
            .setIeee(hubEui64())
            .create()
         )).retryWhen(RxIris.retry(backoff))
      );
   }

   private Observable<?> enroll() {
      if (get(ENROLLED)) {
         return Observable.empty();
      }

      final Backoff backoff = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .random(0.10)
         .max(60, TimeUnit.MINUTES)
         .build();

      return markDoneOnComplete(ENROLLED, true, zcl(HA_PROFILE_ID, ENDPOINT, IasZone.CLUSTER_ID, IasZone.ZoneEnrollResponse.builder()
         .setEnrollResponseCode(0)
         .setZoneId(0xFF)
         .create(), false, true, false)
         .retryWhen(RxIris.retry(backoff))
      );
   }

   private Observable<?> configurePower() {
      if (get(CONF_POWER)) {
         return Observable.empty();
      }

      final Backoff backoff = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .random(0.10)
         .max(60, TimeUnit.MINUTES)
         .build();

      General.ZclAttributeReportingConfigurationRecord[] config = new General.ZclAttributeReportingConfigurationRecord[] {
         General.ZclAttributeReportingConfigurationRecord.builder()
            .setDirection(0)
            .setAttributeIdentifier(Power.ATTR_BATTERY_VOLTAGE)
            .setAttributeDataType(Constants.ZB_TYPE_UNSIGNED_8BIT)
            .setMinimumReportingInterval((int)TimeUnit.SECONDS.toSeconds(60))
            .setMaximumReportingInterval((int)TimeUnit.SECONDS.toSeconds(43200))
            .setReportableChange(new byte[] { 0x1 })
            .create()
      };

      return markDoneOnComplete(CONF_POWER, true, zcl(HA_PROFILE_ID, ENDPOINT, Power.CLUSTER_ID, General.ZclConfigureReporting.builder()
         .setAttributes(config)
         .create(), false, false, false)
         .retryWhen(RxIris.retry(backoff))
      );
   }

   private Observable<?> configureTemperature() {
      if (get(CONF_TEMP)) {
         return Observable.empty();
      }

      final Backoff backoff = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .random(0.10)
         .max(60, TimeUnit.MINUTES)
         .build();

      General.ZclAttributeReportingConfigurationRecord[] config = new General.ZclAttributeReportingConfigurationRecord[] {
         General.ZclAttributeReportingConfigurationRecord.builder()
            .setDirection(0)
            .setAttributeIdentifier(TemperatureMeasurement.ATTR_MEASURED_VALUE)
            .setAttributeDataType(Constants.ZB_TYPE_SIGNED_16BIT)
            .setMinimumReportingInterval((int)TimeUnit.SECONDS.toSeconds(300))
            .setMaximumReportingInterval((int)TimeUnit.SECONDS.toSeconds(1800))
            .setReportableChange(new byte[] { 0x0A, 0x00 })
            .create()
      };

      return markDoneOnComplete(CONF_TEMP, true, zcl(HA_PROFILE_ID, ENDPOINT, TemperatureMeasurement.CLUSTER_ID, General.ZclConfigureReporting.builder()
         .setAttributes(config)
         .create(), false, false, false)
         .retryWhen(RxIris.retry(backoff))
      );
   }

   private Observable<?> configureCheckin() {
      if (get(CONF_CHECKIN)) {
         return Observable.empty();
      }

      final Backoff backoff = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(5, TimeUnit.SECONDS)
         .random(0.10)
         .max(60, TimeUnit.MINUTES)
         .build();

      General.ZclWriteAttributeRecord[] config = new General.ZclWriteAttributeRecord[] {
         General.ZclWriteAttributeRecord.builder()
            .setAttributeIdentifier(PollControl.ATTR_CHECKIN_INTERVAL)
            .setAttributeData(ZclData.builder().set32BitUnsigned(480).create())
            .create()
      };

      return markDoneOnComplete(CONF_CHECKIN, true, zcl(HA_PROFILE_ID, ENDPOINT, PollControl.CLUSTER_ID, General.ZclWriteAttributes.builder()
         .setAttributes(config)
         .create(), false, false, false)
         .retryWhen(RxIris.retry(backoff))
      );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Platform Message Handling
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void handleSetAttributes(Map<String,Object> attrs) {
      boolean sendPanelStatus = false;
      for (Map.Entry<String,Object> attr : attrs.entrySet()) {
         Object value = attr.getValue();
         try {
            value = IrisAttributeLookup.coerce(attr.getKey(), value);
         } catch (Exception ex) {
            log.warn("could not coerce attribute to correct type: ", ex);
         }

         switch (attr.getKey()) {
         case KeyPadCapability.ATTR_ALARMSTATE:
            if (value != null) {
               set(KEYPAD_ALARMSTATE, value.toString());
               sendPanelStatus = true;
            }
            break;
         case KeyPadCapability.ATTR_ALARMMODE:
            if (value != null) {
               set(KEYPAD_ALARMMODE, value.toString());
               sendPanelStatus = true;
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
                  // ignore
                  break;
               }
            }
            break;
         case KeyPadCapability.ATTR_ENABLEDSOUNDS:
            if (value != null) {
               set(KEYPAD_ENABLEDSOUNDS, new LinkedHashSet((Collection)value));
               sendPanelStatus = true;

               if (get(KEYPAD_ENABLEDSOUNDS).isEmpty()) {
                  set(KEYPAD_ALARMSOUNDER, KeyPadCapability.ALARMSOUNDER_OFF);
               } else {
                  set(KEYPAD_ALARMSOUNDER, KeyPadCapability.ALARMSOUNDER_ON);
               }
            }
            break;
         case AlertCapability.ATTR_STATE:
            set(ALERT_STATE, value.toString());

            //sync the alarm state with the alert state
            if (value.toString().equals(AlertCapability.STATE_QUIET) && get(KEYPAD_ALARMSTATE).equals(KeyPadCapability.ALARMSTATE_ALERTING)) {
               if (get(KEYPAD_ALARMMODE).equals(KeyPadCapability.ALARMMODE_OFF)) {
                  set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_DISARMED);
                  sendPanelStatus = true;
               } else {
                  set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMED);
                  sendPanelStatus = true;
               }
            } else if (value.toString().equals(AlertCapability.STATE_ALERTING) && !get(KEYPAD_ALARMSTATE).equals(KeyPadCapability.ALARMSTATE_ALERTING)) {
               set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ALERTING);
               sendPanelStatus = true;
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
      
      if (sendPanelStatus) {
         sendPanelStatus();
      }
   }
   
   @Override
   protected void handleCommand(MessageBody msg) {
      switch (msg.getMessageType()) {
      case KeyPadCapability.BeginArmingRequest.NAME:
         setRemainingTime(KeyPadCapability.BeginArmingRequest.getDelayInS(msg));
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMING);
         set(KEYPAD_ALARMMODE, KeyPadCapability.BeginArmingRequest.getAlarmMode(msg));
         sendPanelStatus();
         break;
      case KeyPadCapability.ArmedRequest.NAME:
         clearRemainingTime();
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ARMED);
         set(KEYPAD_ALARMMODE, KeyPadCapability.ArmedRequest.getAlarmMode(msg));
         sendPanelStatus();
         break;
      case KeyPadCapability.DisarmedRequest.NAME:
         clearRemainingTime();
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_DISARMED);
         set(KEYPAD_ALARMMODE, KeyPadCapability.ALARMMODE_OFF);
         sendPanelStatus();
         break;
      case KeyPadCapability.SoakingRequest.NAME:
         setRemainingTime(KeyPadCapability.SoakingRequest.getDurationInS(msg));
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_SOAKING);
         set(KEYPAD_ALARMMODE, KeyPadCapability.SoakingRequest.getAlarmMode(msg));
         sendPanelStatus();
         break;
      case KeyPadCapability.AlertingRequest.NAME:
         clearRemainingTime();
         String mode = KeyPadCapability.AlertingRequest.getAlarmMode(msg);
         set(KEYPAD_ALARMSTATE, KeyPadCapability.ALARMSTATE_ALERTING);
         set(KEYPAD_ALARMMODE, "PANIC".equals(mode) ? KeyPadCapability.ALARMMODE_OFF : mode);
         sendPanelStatus();
         break;
      case KeyPadCapability.ArmingUnavailableRequest.NAME:
         sendPanelStatus();
         break;
      case IdentifyCapability.IdentifyRequest.NAME:
      case KeyPadCapability.ChimeRequest.NAME:
         sendChime();
         break;
      default:
         // ignore
         break;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Translate protocol messages into platform messages
   /////////////////////////////////////////////////////////////////////////////

   protected void handlePanic() {
      log.info("panic at keypad");
      emit(KeyPadCapability.PanicPressedEvent.instance());
   }

   protected boolean handleArmAll(byte[] code) {
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

   protected boolean handleArmPartial(byte[] code) {
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

   protected boolean handleDisarm(byte[] code) {
      UUID user = verifyPinCode(code);
      if (user != null) {
         log.warn("disarm at keypad: user={}", IrisUUID.toString(user));
         emit(
            KeyPadCapability.DisarmPressedEvent.instance(),
            user
         );

         return true;
      } else {
         log.warn("disarm at keypad failed: could not verify pin");
         emit(
            KeyPadCapability.InvalidPinEnteredEvent.instance()
         );

         return false;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Sending Protocol Messsages to the Device
   /////////////////////////////////////////////////////////////////////////////

   public int getStatus() {
      String status = get(KEYPAD_ALARMSTATE);
      String mode = get(KEYPAD_ALARMMODE);
      if (status == null) {
         return IasAce.GetPanelStatusResponse.STATUS_DISARMED;
      }

      switch (status) {
      case KeyPadCapability.ALARMSTATE_DISARMED:
         return IasAce.GetPanelStatusResponse.STATUS_DISARMED;
      case KeyPadCapability.ALARMSTATE_ARMING:
         return KeyPadCapability.ALARMMODE_ON.equals(mode)
            ?  IasAce.GetPanelStatusResponse.STATUS_ARMING_AWAY
            :  IasAce.GetPanelStatusResponse.STATUS_ARMING_STAY;
      case KeyPadCapability.ALARMSTATE_ARMED:
         return KeyPadCapability.ALARMMODE_ON.equals(mode)
            ?  IasAce.GetPanelStatusResponse.STATUS_ARMED_AWAY
            :  IasAce.GetPanelStatusResponse.STATUS_ARMED_STAY;
      case KeyPadCapability.ALARMSTATE_SOAKING:
         return IasAce.GetPanelStatusResponse.STATUS_ENTRY_DELAY;
      case KeyPadCapability.ALARMSTATE_ALERTING:
         return IasAce.GetPanelStatusResponse.STATUS_IN_ALARM;
      default:
         return IasAce.GetPanelStatusResponse.STATUS_DISARMED;
      }
   }

   public int getAlarmStatus() {
      String mode = get(KEYPAD_ALARMMODE);
      if (mode == null) {
         return IasAce.GetPanelStatusResponse.ALARM_STATUS_NO_ALARM;
      }

      switch (mode) {
      case KeyPadCapability.ALARMMODE_OFF:
         return IasAce.GetPanelStatusResponse.ALARM_STATUS_POLICE_PANIC;
      case KeyPadCapability.ALARMMODE_ON:
      case KeyPadCapability.ALARMMODE_PARTIAL:
         return IasAce.GetPanelStatusResponse.ALARM_STATUS_BURGLAR;
      default:
         return IasAce.GetPanelStatusResponse.ALARM_STATUS_NO_ALARM;
      }
   }

   public int getEnabledSounds() {
      Set<?> enabled = get(KEYPAD_ENABLEDSOUNDS);
      String state = get(KEYPAD_ALARMSTATE);
      return (enabled != null && enabled.contains(state)) ? 0x01 : 0x00;
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

   public boolean isStatusArmed(int sta) {
      return (sta == IasAce.PanelStatusChanged.STATUS_ARMED_AWAY) ||
             (sta == IasAce.PanelStatusChanged.STATUS_ARMED_STAY) ||
             (sta == IasAce.PanelStatusChanged.STATUS_ARMED_NIGHT) ||
             (sta == IasAce.PanelStatusChanged.STATUS_ARMING_AWAY) ||
             (sta == IasAce.PanelStatusChanged.STATUS_ARMING_STAY) ||
             (sta == IasAce.PanelStatusChanged.STATUS_ARMING_NIGHT) ||
             (sta == IasAce.PanelStatusChanged.STATUS_IN_ALARM) ||
             (sta == IasAce.PanelStatusChanged.STATUS_ENTRY_DELAY) ||
             (sta == IasAce.PanelStatusChanged.STATUS_EXIT_DELAY);
   }

    // XXX not implemented yet, and should not be implemented like this...
   public void sendChime() {
      zclmsp(
         0x104E,           // manufacturer id
         (short)0x0104,    // profile id
         (short)0x01,      // endpoint id
         (short)0xFC04,    // cluster id
         0x00,             // command
         new byte[0],      // payload
         false,            // from server
         true,             // cluster speciifc
         true              // disable default response
      ).subscribe(RxIris.SWALLOW_ALL);
   }

   public void sendPanelStatus() {
      int sta = getStatus();
      if (activeRequest != null) {
         boolean armed = isStatusArmed(sta);
         switch (activeRequestType) {
         case ARM:
            zclrsp(activeRequest, IasAce.ArmResponse.builder()
               .setArmNotification(armed ? IasAce.ArmResponse.ARM_NOTIFICATION_ALL_ZONES_ARMED : IasAce.ArmResponse.ARM_NOTIFICATION_INVALID_CODE)
               .create()
            ).subscribe(RxIris.SWALLOW_ALL);
            break;

         case PARTIAL:
            zclrsp(activeRequest, IasAce.ArmResponse.builder()
               .setArmNotification(armed ? IasAce.ArmResponse.ARM_NOTIFICATION_DAY_ZONES_ARMED : IasAce.ArmResponse.ARM_NOTIFICATION_INVALID_CODE)
               .create()
            ).subscribe(RxIris.SWALLOW_ALL);
            break;

         case DISARM:
            zclrsp(activeRequest, IasAce.ArmResponse.builder()
               .setArmNotification(armed ? IasAce.ArmResponse.ARM_NOTIFICATION_INVALID_CODE : IasAce.ArmResponse.ARM_NOTIFICATION_ALL_ZONES_DISARMED)
               .create()
            ).subscribe(RxIris.SWALLOW_ALL);
            break;

         default:
            // no action needed
            break;
         }
      }

      activeRequest = null;
      activeRequestType = ActiveRequestType.NONE;

      zcl(HA_PROFILE_ID, ENDPOINT, IasAce.CLUSTER_ID, IasAce.PanelStatusChanged.builder()
         .setStatus(sta)
         .setSeconds(getRemainingTime())
         .setAlarmStatus(getAlarmStatus())
         .setAudibleNotification(getEnabledSounds())
         .create()
         , true, true, true
      ).subscribe(RxIris.SWALLOW_ALL);
   }

   public void sendPanelStatusResponse(ZigbeeMessage.Zcl zcl) {
      zclrsp(zcl, IasAce.GetPanelStatusResponse.builder()
         .setStatus(getStatus())
         .setSeconds(getRemainingTime())
         .setAlarmStatus(getAlarmStatus())
         .setAudibleNotification(getEnabledSounds())
         .create()
      ).subscribe(RxIris.SWALLOW_ALL);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Receive Protocol Messages from the Device
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected boolean handleAttributeUpdated(short profile, byte endpoint, short cluster, short id, ZclData data) {
      switch (cluster) {
      case Power.CLUSTER_ID:
         if (id == Power.ATTR_BATTERY_VOLTAGE) {
            int percent;
            int volts = ((Number)data.getDataValue()).intValue();
            if (volts >= 60) percent = 100;
            else if (volts == 59) percent = 97;
            else if (volts == 58) percent = 95;
            else if (volts == 57) percent = 90;
            else if (volts == 56) percent = 85;
            else if (volts == 55) percent = 80;
            else if (volts == 54) percent = 75;
            else if (volts == 53) percent = 70;
            else if (volts == 52) percent = 65;
            else if (volts == 51) percent = 60;
            else if (volts == 50) percent = 50;
            else if (volts == 49) percent = 40;
            else if (volts == 48) percent = 30;
            else if (volts == 47) percent = 20;
            else if (volts == 46) percent = 10;
            else if (volts == 45) percent = 1;
            else percent = 0;

            if (percent != lastBatteryPercent) {
               lastBatteryPercent = percent;
               emit(ImmutableMap.of(DevicePowerCapability.ATTR_BATTERY, percent));
            }
            return true;
         }
         break;
      case TemperatureMeasurement.CLUSTER_ID:
         if (id == TemperatureMeasurement.ATTR_MEASURED_VALUE) {
            int measured = ((Number)data.getDataValue()).intValue();
            double celsius = measured / 100.0;
            double farenheit = (celsius*9.0/5.0) + 32;

            int report = (int)Math.round(farenheit);
            if (lastTemperatureValue != report) {
               lastTemperatureValue = report;
               emit(ImmutableMap.of(TemperatureCapability.ATTR_TEMPERATURE, (report-32)*5.0/9.0));
            }
            return true;
         }
         break;
      default:
         // ignore
         break;
      }

      return false;
   }

   protected void handleIasAce(ZigbeeMessage.Zcl zcl) {
      switch (zcl.getZclMessageId()) {
      case IasAce.Arm.ID:
         IasAce.Arm arm = IasAce.Arm.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());

         switch (arm.getArmMode()) {
         case IasAce.Arm.ARM_MODE_ALL:
            activeRequest = zcl;
            activeRequestType = ActiveRequestType.ARM;
            handleArmAll(arm.getCode());
            break;
         case IasAce.Arm.ARM_MODE_DAY:
            activeRequest = zcl;
            activeRequestType = ActiveRequestType.PARTIAL;
            handleArmPartial(arm.getCode());
            break;
         case IasAce.Arm.ARM_MODE_DISARM:
            if (handleDisarm(arm.getCode())) {
               activeRequest = zcl;
               activeRequestType = ActiveRequestType.DISARM;
            } else {
               zclrsp(zcl, IasAce.ArmResponse.builder()
                  .setArmNotification(IasAce.ArmResponse.ARM_NOTIFICATION_INVALID_CODE)
                  .create()
               ).subscribe(RxIris.SWALLOW_ALL);
            }
            break;
         default:
            log.warn("arm pressed with unknown mode: {}", arm);
            break;
         }
         break;

      case IasAce.Bypass.ID:
         IasAce.Bypass bypass = IasAce.Bypass.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());
         zclrsp(zcl, IasAce.BypassResponse.builder()
            .setBypassResult(IasAce.BypassResponse.BYPASS_RESULT_NOT_ALLOWED)
            .setNumberOfZones(bypass.getNumberOfZones())
            .create()
         ).subscribe(RxIris.SWALLOW_ALL);
         break;

      case IasAce.Panic.ID:
         handlePanic();
         zcldefrsp(zcl, 0x00).subscribe(RxIris.SWALLOW_ALL);
         break;

      case IasAce.GetPanelStatus.ID:
         sendPanelStatusResponse(zcl);
         break;

      default:
         log.info("greatstar keypad driver unknown ias ace message: {}", zcl);
         zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_CLUSTER_COMMAND).subscribe(RxIris.SWALLOW_ALL);
         break;
      }
   }

   protected void handleIasZone(ZigbeeMessage.Zcl zcl) {
      switch (zcl.getZclMessageId()) {
      case IasZone.ZoneEnrollRequest.ID:
         log.info("Responding to ZoneEnrollRequest from Keypad, it must have ignored the unsolicited ones");
         zcl(HA_PROFILE_ID, ENDPOINT, IasZone.CLUSTER_ID, IasZone.ZoneEnrollResponse.builder()
                 .setEnrollResponseCode(0)
                 .setZoneId(0xFF)
                 .create(), false, true, false);
         break;
      case IasZone.ZoneStatusChangeNotification.ID:
         zcldefrsp(zcl, Constants.ZB_STATUS_SUCCESS);
         break;
      default:
         zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_CLUSTER_COMMAND);
         break;
      }
   }

   @Override
   protected boolean handleZclGeneralFromServer(ZigbeeMessage.Zcl zcl) {
      switch (zcl.getZclMessageId()) {
      case General.ZclReadAttributes.ID:
         return handleReadAttributes(zcl);
      case General.ZclReadAttributesResponse.ID:
         return handleReadAttributesResponse(zcl);
      case General.ZclReportAttributes.ID:
         return handleReportAttributes(zcl);
      case General.ZclConfigureReportingResponse.ID:
         return handleConfigureReportingResponse(zcl);
      case General.ZclWriteAttributes.ID:
         return handleWriteAttributes(zcl);
      case General.ZclWriteAttributesResponse.ID:
         return handleWriteAttributesResponse(zcl);
      default:
         zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_GENERAL_COMMAND);
         return false;
      }
   }

   @Override
   protected boolean handleZclGeneralFromClient(ZigbeeMessage.Zcl zcl) {
      zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_GENERAL_COMMAND);
      return true;
   }
   
   @Override
   protected boolean handleZclClusterSpecificFromServer(ZigbeeMessage.Zcl zcl) {
      switch (zcl.rawClusterId()) {
      case IasZone.CLUSTER_ID:
         handleIasZone(zcl);
         return true;

      case Ota.CLUSTER_ID:
         return false;

      default:
         zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_CLUSTER_COMMAND);
         return true;
      }
   }
   
   @Override
   protected boolean handleZclClusterSpecificFromClient(ZigbeeMessage.Zcl zcl) {
      switch (zcl.rawClusterId()) {
      case IasAce.CLUSTER_ID:
         handleIasAce(zcl);
         return true;

      case Ota.CLUSTER_ID:
         return false;

      default:
         log.info("greatstar keypad driver unknown message: {}", zcl);
         zcldefrsp(zcl, Constants.ZB_STATUS_UNSUP_CLUSTER_COMMAND);
         return true;
      }
   }
   
   @Override
   protected boolean handleZclManufSpecific(ZigbeeMessage.Zcl zcl) {
      log.info("centralite keypad driver zcl manuf specific: {}", zcl);
      return true;
   }
}

