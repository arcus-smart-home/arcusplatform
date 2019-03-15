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
package com.iris.agent.alarm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.agent.addressing.HubAddr;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.alarm.lights.AlarmLEDConfig;
import com.iris.agent.alarm.lights.AlarmLEDValue;
import com.iris.agent.alarm.sounds.AlarmSoundConfig;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleListener;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.reflex.ReflexDevice;
import com.iris.agent.reflex.ReflexLocalProcessing;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.router.Router;
import com.iris.agent.router.SnoopingPortHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.errors.Errors;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.reflex.ReflexProtocol;
import com.iris.util.IrisUUID;
import com.netflix.governator.annotations.WarmUp;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;

public class AlarmController implements SnoopingPortHandler, LifeCycleListener {
   private static final Logger log = LoggerFactory.getLogger(AlarmController.class);

   public static final HubAddr ADDRESS = HubAddressUtils.service("alarm");
   public static final int REPORT_ATTRS_TTL = (int)TimeUnit.HOURS.toMillis(1);

   public static final Map<String,Integer> ALERT_STATE_TO_PRIORITY = ImmutableMap.<String,Integer>builder()
      .put(HubAlarmCapability.SECURITYALERTSTATE_INACTIVE, 0)
      .put(HubAlarmCapability.SECURITYALERTSTATE_DISARMED, 1)
      .put(HubAlarmCapability.SECURITYALERTSTATE_ARMING, 2)
      .put(HubAlarmCapability.SECURITYALERTSTATE_READY, 3)
      .put(HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR, 4)
      .put(HubAlarmCapability.SECURITYALERTSTATE_CLEARING, 5)
      .put(HubAlarmCapability.SECURITYALERTSTATE_PREALERT, 6)
      .put(HubAlarmCapability.SECURITYALERTSTATE_ALERT, 7)
      .build();

   public static final Map<Integer,String> PRIORITY_TO_ALARM_STATE = ImmutableMap.<Integer,String>builder()
      .put(0, HubAlarmCapability.ALARMSTATE_INACTIVE)
      .put(1, HubAlarmCapability.ALARMSTATE_READY)
      .put(2, HubAlarmCapability.ALARMSTATE_READY)
      .put(3, HubAlarmCapability.ALARMSTATE_READY)
      .put(4, HubAlarmCapability.ALARMSTATE_CLEARING)
      .put(5, HubAlarmCapability.ALARMSTATE_CLEARING)
      .put(6, HubAlarmCapability.ALARMSTATE_PREALERT)
      .put(7, HubAlarmCapability.ALARMSTATE_ALERTING)
      .build();

   private final HubAttributesService.Attribute<String> state;
   private final HubAttributesService.Attribute<String> alarmState;
   private final HubAttributesService.Attribute<Set<String>> activeAlerts;
   private final HubAttributesService.Attribute<Set<String>> availableAlerts;
   private final HubAttributesService.Attribute<String> currentIncident;

   private final ReflexLocalProcessing reflex;
   private final Timer timer;
   private final Router router;

   private final Map<String,Alarm> alarms;
   private Port port;

   private @Nullable Boolean isAlertingDevices;
   private @Nullable Set<Address> deviceAddresses;
   private volatile boolean reflexesProcessed = false;

   @Inject
   @SuppressWarnings("null")
   public AlarmController(Router router, ReflexLocalProcessing reflex) {
      this.reflex = reflex;
      this.timer = new HashedWheelTimer();
      this.router = router;

      this.alarms = ImmutableMap.of(
         AlarmPanic.NAME, new AlarmPanic(this),
         AlarmSmoke.NAME, new AlarmSmoke(this),
         AlarmCo.NAME, new AlarmCo(this),
         AlarmWater.NAME, new AlarmWater(this),
         AlarmSecurity.NAME, new AlarmSecurity(this)
      );

      this.isAlertingDevices = null;

      this.state = HubAttributesService.persisted(String.class, HubAlarmCapability.ATTR_STATE, HubAlarmCapability.STATE_SUSPENDED);
      this.currentIncident = HubAttributesService.persisted(String.class, HubAlarmCapability.ATTR_CURRENTINCIDENT, "");

      this.alarmState = HubAttributesService.computed(String.class, HubAlarmCapability.ATTR_ALARMSTATE, new Supplier<String>() {
         @Override
         public String get() {
            return computeAlarmState();
         }
      });

      this.activeAlerts = HubAttributesService.computedSet(String.class, HubAlarmCapability.ATTR_ACTIVEALERTS, new Supplier<Set<String>>() {
         @Override
         public Set<String> get() {
            return computeActiveAlerts();
         }
      });

      this.availableAlerts = HubAttributesService.computedSet(String.class, HubAlarmCapability.ATTR_AVAILABLEALERTS, new Supplier<Set<String>>() {
         @Override
         public Set<String> get() {
            Set<String> available = new LinkedHashSet<>();
            for(Map.Entry<String, Alarm> entry : alarms.entrySet()) {
               if(!AlarmCapability.ALERTSTATE_INACTIVE.equals(entry.getValue().getAlertState())) {
                  available.add(entry.getKey().toUpperCase());
               }
            }
            return available;
         }
      });

      this.state.setReportedOnConnect(false);
      this.alarmState.setReportedOnConnect(false);
      this.activeAlerts.setReportedOnConnect(false);
      this.availableAlerts.setReportedOnConnect(false);
      this.currentIncident.setReportedOnConnect(false);

      this.state.setReportedOnValueChange(false);
      this.alarmState.setReportedOnValueChange(false);
      this.activeAlerts.setReportedOnValueChange(false);
      this.availableAlerts.setReportedOnValueChange(false);
      this.currentIncident.setReportedOnValueChange(false);
   }

   @PostConstruct
   public void initialize() {
      reflex.setListener(new ReflexLocalProcessing.Listener() {
         @Override
         public void onReflexDevicesUpdated() {
            processReflexDevices();
         }

         @Override
         public void onReflexDeviceOnline(Address device) {
            alarms.values().forEach((a) -> a.onDeviceOnline(device));
            reportState(false);
         }

         @Override
         public void onReflexDeviceOffline(Address device) {
            alarms.values().forEach((a) -> a.onDeviceOffline(device));
            reportState(false);
         }
      });

      this.port = router.connect("alrm", this, ADDRESS, new PortHandler() {
         @Override @Nullable public Object recv(Port port, PlatformMessage message) throws Exception { return recvDirect(port,message); }
         @Override public void recv(Port port, ProtocolMessage message) { }
         @Override public void recv(Port port, Object message) { recvDirect(port, message); }
      });

      LifeCycleService.addListener(this);
   }

   @WarmUp
   public void start() {
      log.info("starting hub alarm controller...");
   }

   @PreDestroy
   public void shutdown() {
      log.info("stopping hub alarm controller...");
   }

   private boolean isActive() {
      return HubAlarmCapability.STATE_ACTIVE.equals(state.get());
   }

   private MessageBody activate(PlatformMessage msg) {
      try {
         if (!dispatch(AlarmEvents.activate(msg.getSource(), msg.getActor()))) {
            return Errors.fromCode("invalid.state", "cannot activate while in the current state");
         }

         state.set(HubAlarmCapability.STATE_ACTIVE);
         return HubAlarmCapability.ActivateResponse.instance();
      } finally {
         reportState(false);
      }
   }

   private MessageBody suspend(PlatformMessage msg) {
      try {
         if (!dispatch(AlarmEvents.suspend(msg.getSource(), msg.getActor()))) {
            return Errors.fromCode("invalid.state", "cannot suspend while in the current state");
         }

         state.set(HubAlarmCapability.STATE_SUSPENDED);
         return HubAlarmCapability.SuspendResponse.instance();
      } finally {
         reportState(false);
      }
   }

   private boolean dispatch(AlarmEvents.Event event) {
      return dispatch(event, true);
   }

   private boolean dispatch(AlarmEvents.Event event, boolean allowStateUpdate) {
      for (Alarm alrm : alarms.values()) {
         if (!alrm.canHandleEvent(event)) {
            return false;
         }
      }

      for (Alarm alrm : alarms.values()) {
         alrm.handleEvent(event);
      }

      if (allowStateUpdate) {
         updateAndReportState();
      }

      return true;
   }

   void updateAndReportState() {
      port.queue(new Runnable() {
         @Override
         public void run() {
            if (isActive()) {
               boolean report = false;
               for (Alarm alarm : alarms.values()) {
                  report |= alarm.isReportingNeeded();
                  alarm.clearNeedsReporting();
               }

               if (report) {
                  processAlarmStates();
                  reportState(false);
               }
            }
         }
      });
   }

   void reportState(boolean reconnecting) {
      log.trace("reporting alarm state...");
      Map<String,Object> attrs = new HashMap<>();
      attrs.put(state.name(), state.get());
      attrs.put(alarmState.name(), alarmState.get());
      attrs.put(activeAlerts.name(), activeAlerts.get());
      attrs.put(availableAlerts.name(), availableAlerts.get());
      attrs.put(currentIncident.name(), currentIncident.get());
      attrs.put(HubAlarmCapability.ATTR_RECONNECTREPORT, reconnecting);

      for (Alarm alrm : alarms.values()) {
         alrm.updateReportAttributes(attrs);
      }

      port.sendEvent(
         MessageBody.messageBuilder(Capability.EVENT_REPORT)
            .withAttributes(attrs)
            .create(),
         REPORT_ATTRS_TTL
      );
   }

   private void processAlarmStates() {
      // Update the current incident by looking at the current alarm status
      String currentAlarmState = alarmState.get();
      switch (currentAlarmState) {
      case HubAlarmCapability.ALARMSTATE_INACTIVE:
      case HubAlarmCapability.ALARMSTATE_READY:
         currentIncident.set("");
         break;

      case HubAlarmCapability.ALARMSTATE_PREALERT:
      case HubAlarmCapability.ALARMSTATE_ALERTING:
         String incident = IrisUUID.toString(IrisUUID.timeUUID());
         if (currentIncident.compareAndSet("",incident)) {
            log.info("new alarm incident started: {}", incident);
         }
         break;

      case HubAlarmCapability.ALARMSTATE_CLEARING:
         break;

      default:
         // ignore
         break;
      }
   }

   private void processReflexDevices() {
      Collection<? extends ReflexDevice> devices = reflex.getDevices();
      Set<Address> addresses = devices.stream().map((dev) -> dev.getAddress()).collect(Collectors.toSet());
      if (!Objects.equals(addresses, deviceAddresses)) {
         deviceAddresses = addresses;
         alarms.values().forEach(Alarm::afterProcessReflexDevices);
         reportState(!reflexesProcessed);
      }
      reflexesProcessed = true;
   }
   
   private String computeAlarmState() {
      if (!isActive()) {
         return HubAlarmCapability.ALARMSTATE_INACTIVE;
      }

      int mode = 0;
      for (Alarm alrm : alarms.values()) {
         String state = alrm.getAlertState();
         Integer value = ALERT_STATE_TO_PRIORITY.get(state);
         if (value != null) {
            mode = Math.max(mode, value);
         } else {
            log.warn("unknown alert state: {}", state);
         }
      }

      return PRIORITY_TO_ALARM_STATE.get(mode);
   }

   private Set<String> computeActiveAlerts() {
      if (!isActive()) {
         return ImmutableSet.of();
      }

      ImmutableSet.Builder<String> bld = ImmutableSet.builder();
      for (Alarm alrm : alarms.values()) {
         String state = alrm.getAlertState();
         switch(state) {
            case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
            case HubAlarmCapability.COALERTSTATE_PENDING_CLEAR:
               bld.add(alrm.getName().toUpperCase());
               break;
            default:
               break;
         }
      }

      return bld.build();
   }

   /////////////////////////////////////////////////////////////////////////////
   // API for Alarms
   /////////////////////////////////////////////////////////////////////////////
   
   public Collection<? extends ReflexDevice> getDevices() {
      return reflex.getDevices();
   }

   public String getCurrentIncident() {
      return currentIncident.get();
   }
   
   public void defer(Object msg) {
      port.queue(msg);
   }
   
   public void submit(Runnable task) {
      schedule(task, 0, TimeUnit.NANOSECONDS);
   }

   public Timeout schedule(Runnable task, long time, TimeUnit unit) {
      return timer.newTimeout((to) -> port.queue(task), time, unit);
   }
   
   public void onArmFailed() {
      reflex.onArmFailed(alarmState.get());
   }

   public void onArmed(AlarmEvents.Mode mode, boolean soundsEnabled) {
      switch (mode) {
      case ON:
         reflex.onArmed(soundsEnabled, alarmState.get());
         updateAndReportState();
         break;
      case PARTIAL:
         reflex.onArmedPartial(soundsEnabled, alarmState.get());
         updateAndReportState();
         break;
      default:
         log.warn("unknown alarm mode: {}", mode);
      }
   }

   public void onArming(AlarmEvents.Mode mode, int exitDelay, boolean silent, boolean soundsEnabled) {
      switch (mode) {
      case ON:
         reflex.onArming(exitDelay, silent, soundsEnabled, alarmState.get());
         break;
      case PARTIAL:
         reflex.onArmingPartial(exitDelay, silent, soundsEnabled, alarmState.get());
         break;
      default:
         log.warn("unknown alarm mode: {}", mode);
      }
   }

   public void onPrealert(String mode, int entranceDelay, boolean soundsEnabled) {
      reflex.onPrealert(mode, entranceDelay, soundsEnabled, alarmState.get());
   }

   public void onDisarmed() {
      reflex.onDisarmed();
   }

   public void onDisarmFailed() {
      reflex.onDisarmFailed();
   }

   public void onAlert() {
	   Alarm alarm = getAlertAlarm();
	   if (alarm != null) {
	      SounderMode sound = getAlarmSound(alarm);
	      String keypad = getAlarmKeyPadMode(alarm);
	      AlarmLEDValue led = AlarmLEDConfig.get(alarm, IrisHal.isBatteryPowered());
	   
	      reflex.onAlerting(sound,keypad,led.getState(),led.getDuration());
	   }
	   updateAndReportState();
   }
   
   private @Nullable Alarm getAlertAlarm() {
      Set<String> active = activeAlerts.get();
      List<Alarm> ordered = new ArrayList<>(alarms.values());
      Collections.sort(ordered, Comparator.comparingInt(Alarm::getPriority));
      for (Alarm alarm : ordered) {
    	  if (!active.contains(alarm.getName().toUpperCase())) {
              continue;
           }
          if (!alarm.isSilent()) {
        	  return alarm;
          }
      }
      return null;
   }

   public  SounderMode getAlarmSound(Alarm alarm) {
	   return AlarmSoundConfig.getTriggered(alarm);
   }

   public String getAlarmKeyPadMode(Alarm alarm) {
	   return AlarmSecurity.NAME.equals(alarm.getName()) ? ((AlarmSecurity) alarm).getSecurityMode().get() : KeyPadCapability.AlertingRequest.ALARMMODE_PANIC;
   }

   
   /////////////////////////////////////////////////////////////////////////////
   // Message handling
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   public Object recvDirect(Port port, PlatformMessage message) throws Exception {
      MessageBody msg = message.getValue();
      log.trace("alarm processing platform message: {} -> {}", message, msg);

      Address addr = message.getSource();
      Address actor = message.getActor();
      switch (message.getMessageType()) {
      case HubAlarmCapability.ActivateRequest.NAME:
         return activate(message);

      case HubAlarmCapability.SuspendRequest.NAME:
         return suspend(message);

      // Some messages are only handled if we are active...
      case HubAlarmCapability.PanicRequest.NAME:
      case HubAlarmCapability.ArmRequest.NAME:
      case HubAlarmCapability.DisarmRequest.NAME:
      case HubAlarmCapability.ClearIncidentRequest.NAME:
      case HubAlarmCapability.VerifiedEvent.NAME:
         if (!isActive()) {
            return Errors.fromCode("alarm.invalidState", "hub local alarms are currently suspended");
         }
         break;

      default:
         // fall through
         break;
      }

      try {
         int duration;
         String mode, alertName;
         Alarm eventAlarm;

         switch(message.getMessageType()) {
            case HubAlarmCapability.PanicRequest.NAME:
               String source = HubAlarmCapability.PanicRequest.getSource(message.getValue());
               Address sourceAddr = source == null ? Address.broadcastAddress() : Address.fromString(source);
               return dispatch(AlarmEvents.trigger(sourceAddr, actor, AlarmEvents.Trigger.PANIC, true))
                  ? HubAlarmCapability.PanicResponse.instance()
                  : Errors.fromCode("alarm.invalidState", "cannot panic in current state");

            case HubAlarmCapability.ArmRequest.NAME:
               return onArmRequest(message);

            case HubAlarmCapability.DisarmRequest.NAME:
               String disarmedBy = HubAlarmCapability.DisarmRequest.getDisarmedBy(msg);
               String disarmedFrom = HubAlarmCapability.DisarmRequest.getDisarmedFrom(msg);
               boolean dispatched = dispatch(AlarmEvents.disarm(Address.fromString(disarmedFrom), disarmedBy == null ? null : Address.fromString(disarmedBy)));
               if(dispatched) {
                  onDisarmed();
                  return HubAlarmCapability.DisarmResponse.instance();
               }
               return Errors.fromCode("security.invalidState", "cannot disarm");

            case HubAlarmCapability.ClearIncidentRequest.NAME:
               currentIncident.set("");
               for(Alarm alrm : alarms.values()) {
                  alrm.clearTriggers();
               }

               dispatch(AlarmEvents.clear(addr, actor));
               reportState(false);
               return HubAlarmCapability.ClearIncidentResponse.instance();

            case HubAlarmCapability.VerifiedEvent.NAME:
               alarms.values().forEach(Alarm::onVerified);
               return Port.HANDLED;

               // Sound/LED state support
            case HubAlarmCapability.PrealertTriggeredEvent.NAME:
               duration = HubAlarmCapability.PrealertTriggeredEvent.getDuration(msg);
               IrisHal.setSounderMode(SounderMode.ARMING_GRACE_ENTER, duration);
               IrisHal.setLedState(LEDState.ALARM_GRACE_ENTER, duration);
               return Port.HANDLED;

            case HubAlarmCapability.AlertTriggeredEvent.NAME:
               alertName = HubAlarmCapability.AlertTriggeredEvent.getAlertName(msg);
               boolean promon = HubAlarmCapability.AlertTriggeredEvent.getPromon(msg);
               eventAlarm = alarms.get(alertName.toLowerCase());

               // Stop previous sound that might be playing
               IrisHal.setSounderMode(SounderMode.NO_SOUND);

               if (eventAlarm != null) {
                  // Use monitored sounds in promon case
                  if (promon) {
                     IrisHal.setSounderMode(AlarmSoundConfig.getMonitored(eventAlarm));
                  } else {
                     IrisHal.setSounderMode(AlarmSoundConfig.getTriggered(eventAlarm));
                  }
                  AlarmLEDValue led = AlarmLEDConfig.get(eventAlarm, IrisHal.isBatteryPowered());
                  IrisHal.setLedState(led.getState(), led.getDuration());
               }
               return Port.HANDLED;

            case HubAlarmCapability.AlertCancelledEvent.NAME:
               alertName = HubAlarmCapability.AlertCancelledEvent.getAlertName(msg);
               eventAlarm = alarms.get(alertName.toLowerCase());

               // Stop previous sound that might be playing
               IrisHal.setSounderMode(SounderMode.NO_SOUND);

               if (eventAlarm != null) {
                  IrisHal.setSounderMode(AlarmSoundConfig.getCleared(eventAlarm));
                  IrisHal.setLedState(LEDState.ALL_OFF);
               }
               return Port.HANDLED;

            case HubAlarmCapability.SecurityArmingEvent.NAME:
               mode = HubAlarmCapability.SecurityArmingEvent.getMode(msg);
               duration = HubAlarmCapability.SecurityArmingEvent.getDuration(msg);
               if (mode == KeyPadCapability.BeginArmingRequest.ALARMMODE_PARTIAL) {
                  IrisHal.setSounderMode(SounderMode.ARMING_GRACE_EXIT_PARTIAL, duration);
               } else {
                  IrisHal.setSounderMode(SounderMode.ARMING_GRACE_EXIT, duration);
               }
               IrisHal.setLedState(LEDState.ALARM_GRACE_EXIT, duration);
               return Port.HANDLED;

            case HubAlarmCapability.SecurityArmedEvent.NAME:
               mode = HubAlarmCapability.SecurityArmedEvent.getMode(msg);

               // Stop previous sound that might be playing
               IrisHal.setSounderMode(SounderMode.NO_SOUND);

               switch (mode) {
               case KeyPadCapability.ArmedRequest.ALARMMODE_PARTIAL:
                  IrisHal.setSounderMode(SounderMode.SECURITY_ALARM_PARTIAL);
                  break;
               case KeyPadCapability.ArmedRequest.ALARMMODE_ON:
                  IrisHal.setSounderMode(SounderMode.SECURITY_ALARM_ON);
                  break;
               default:
                  // No sound
                  break;
               }
               IrisHal.setLedState(LEDState.ALARM_ON);
               return Port.HANDLED;

            case HubAlarmCapability.SecurityDisarmedEvent.NAME:
               IrisHal.setSounderMode(SounderMode.SECURITY_ALARM_OFF);
               IrisHal.setLedState(LEDState.ALARM_OFF);
               return Port.HANDLED;

            default:
               return Errors.unsupportedMessageType(message.getMessageType());
         }
      } catch(Exception e) {
         log.error("failed to handle {}", message.getValue(), e);
         return Errors.fromException(e);
      }
   }

   private MessageBody onArmRequest(PlatformMessage message) {
      MessageBody msg = message.getValue();
      String by = HubAlarmCapability.ArmRequest.getArmedBy(msg);
      String from = HubAlarmCapability.ArmRequest.getArmedFrom(msg);

      AlarmSecurity security = (AlarmSecurity) alarms.get(AlarmSecurity.NAME);

      try {
         return dispatch(AlarmEvents.arm(Address.fromString(from), by == null ? null : Address.fromString(by), msg))
            ? HubAlarmCapability.ArmResponse.builder()
               .withSecurityArmTime(new Date(security.getSecurityArmTime().get()))
               .build()
            : Errors.fromCode("security.invalidState", "cannot arm");
      } catch(Exception e) {
         if(from.startsWith("PROT")) {
            onArmFailed();
         }
         throw e;
      }
   }

   @Override
   @Nullable
   public Object recv(Port port, PlatformMessage message) throws Exception {
      return null;
   }

   @Override
   public boolean isInterestedIn(PlatformMessage message) {
      return false;
   }

   @Override
   public boolean isInterestedIn(ProtocolMessage message) {
      // The alarms controller is only interested in reflex protocol messages
      return ReflexProtocol.NAMESPACE.equals(message.getMessageType());
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
      if (!isActive()) {
         return;
      }

      MessageBody msg = message.getValue(ReflexProtocol.INSTANCE);
      if (log.isTraceEnabled()) {
         log.trace("alarm processing protocol message from {}: {}", message.getSource(), msg);
      }

      Address addr = message.getSource();
      Address actor = message.getActor();
      String type = msg.getMessageType();
      switch (type) {
      case KeyPadCapability.PanicPressedEvent.NAME:
         dispatch(AlarmEvents.trigger(addr, actor, AlarmEvents.Trigger.PANIC, true));
         break;

      case KeyPadCapability.DisarmPressedEvent.NAME:
         if (!dispatch(AlarmEvents.disarm(addr, actor))) {
            onDisarmFailed();
         } else {
            onDisarmed();
         }
         break;

      case Capability.CMD_SET_ATTRIBUTES:
      case Capability.EVENT_VALUE_CHANGE:
         Map<String,Object> attrs = msg.getAttributes();

         boolean doUpdate = false;
         try {
            for (Map.Entry<String,Object> entry : attrs.entrySet()) {
               Object value = null;
               AlarmEvents.Trigger trigger = null;
               boolean triggered = false;

               switch (entry.getKey()) {
               case ContactCapability.ATTR_CONTACT:
                  value = attrs.get(ContactCapability.ATTR_CONTACT);
                  triggered = ContactCapability.CONTACT_OPENED.equals(value);
                  trigger = AlarmEvents.Trigger.CONTACT;
                  break;
               case MotionCapability.ATTR_MOTION:
                  value = attrs.get(MotionCapability.ATTR_MOTION);
                  triggered = MotionCapability.MOTION_DETECTED.equals(value);
                  trigger = AlarmEvents.Trigger.MOTION;
                  break;
               case GlassCapability.ATTR_BREAK:
                  value = attrs.get(GlassCapability.ATTR_BREAK);
                  triggered = GlassCapability.BREAK_DETECTED.equals(value);
                  trigger = AlarmEvents.Trigger.GLASS;
                  break;
               case MotorizedDoorCapability.ATTR_DOORSTATE:
                  value = attrs.get(MotorizedDoorCapability.ATTR_DOORSTATE);
                  triggered = MotorizedDoorCapability.DOORSTATE_OPEN.equals(value) ||
                              MotorizedDoorCapability.DOORSTATE_OPENING.equals(value) ||
                              MotorizedDoorCapability.DOORSTATE_OBSTRUCTION.equals(value);
                  trigger = AlarmEvents.Trigger.DOOR;
                  break;
               case LeakH2OCapability.ATTR_STATE:
                  value = attrs.get(LeakH2OCapability.ATTR_STATE);
                  triggered = LeakH2OCapability.STATE_LEAK.equals(value);
                  trigger = AlarmEvents.Trigger.WATER;
                  break;
               case CarbonMonoxideCapability.ATTR_CO:
                  value = attrs.get(CarbonMonoxideCapability.ATTR_CO);
                  triggered = CarbonMonoxideCapability.CO_DETECTED.equals(value);
                  trigger = AlarmEvents.Trigger.CO;
                  break;
               case SmokeCapability.ATTR_SMOKE:
                  value = attrs.get(SmokeCapability.ATTR_SMOKE);
                  triggered = SmokeCapability.SMOKE_DETECTED.equals(value);
                  trigger = AlarmEvents.Trigger.SMOKE;
                  break;
               default:
                  // uninterested
                  break;
               }

               if (trigger != null) {
                  doUpdate |= dispatch(AlarmEvents.trigger(addr, actor, trigger, triggered), false);
               }
            }
         } finally {
            if (doUpdate) {
               updateAndReportState();
            }
         }
         break;

      default:
         break;
      }
   }

   public void recvDirect(Port port, Object message) {
      if (message instanceof Runnable) {
         try {
            ((Runnable)message).run();
         } catch (Exception ex) {
            log.debug("exception while processing message:", ex);
         }

         return;
      }

      log.warn("received unknown custom message: {}", message);
   }

   @Override
   public void recv(Port port, Object message) {
      throw new UnsupportedOperationException();
   }

   /////////////////////////////////////////////////////////////////////////////
   // LifeCycle
   /////////////////////////////////////////////////////////////////////////////


   @Override
   public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {
      if (oldState != LifeCycle.AUTHORIZED && newState == LifeCycle.AUTHORIZED && reflexesProcessed) {
         reportState(true);
      }
   }

   @Override
   public void hubAccountIdUpdated(@Nullable UUID oldAcc, @Nullable UUID newAcc) {
   }

   @Override
   public void hubReset(LifeCycleService.Reset type) {
   }

   @Override
   public void hubDeregistered() {
   }
}

