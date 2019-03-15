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
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.base.Supplier;
import com.iris.agent.alarm.sounds.AlarmSoundConfig;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.Model;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.reflex.ReflexDevice;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.type.IncidentTrigger;

import static com.iris.agent.alarm.AlarmEvents.*;

import io.netty.util.Timeout;

public abstract class AbstractAlarm implements Alarm {
   public static final int MAX_TRIGGERS = 100;

   private final Logger log;
   private final AlarmController parent;
   protected final HubAttributesService.Attribute<String> alertState;
   protected final HubAttributesService.Attribute<Set<String>> offlineDevices;
   protected final HubAttributesService.Attribute<Set<String>> triggeredDevices;
   protected final HubAttributesService.Attribute<List<Map<String,Object>>> triggers;
   protected final HubAttributesService.Attribute<Boolean> silent;

   private boolean needsToReport = false;

   public AbstractAlarm(AlarmController parent, Logger log, String name, String defaultState) {
      this.parent = parent;
      this.log = log;
      this.alertState = HubAttributesService.persisted(String.class, "hubalarm:" + name + "AlertState", defaultState);
      this.silent = HubAttributesService.persisted(Boolean.class, "hubalarm:" + name + "Silent", Boolean.FALSE);

      this.offlineDevices = HubAttributesService.computedSet(String.class, "hubalarm:" + name + "OfflineDevices", new Supplier<Set<String>>() {
         @Override
         public Set<String> get() {
            return getParticipatingDevices()
               .filter((dev) -> dev.isOffline())
               .map((dev) -> dev.getAddress().getRepresentation())
               .collect(Collectors.toSet());
         }
      });

      this.triggeredDevices = HubAttributesService.computedSet(String.class, "hubalarm:" + name + "TriggeredDevices", new Supplier<Set<String>>() {
         @Override
         public Set<String> get() {
            return getParticipatingDevices()
               .filter((dev) -> !dev.isOffline() && isTriggered(dev))
               .map((dev) -> dev.getAddress().getRepresentation())
               .collect(Collectors.toSet());
         }
      });

      this.triggers = HubAttributesService.persistedListMap(String.class, Object.class, "hubalarm:" + name + "Triggers", new ArrayList<>());

      this.alertState.setReportedOnConnect(false);
      this.offlineDevices.setReportedOnConnect(false);
      this.triggeredDevices.setReportedOnConnect(false);
      this.silent.setReportedOnConnect(false);
      this.triggers.setReportedOnConnect(false);

      this.alertState.setReportedOnValueChange(false);
      this.offlineDevices.setReportedOnValueChange(false);
      this.triggeredDevices.setReportedOnValueChange(false);
      this.silent.setReportedOnValueChange(true); // This is a writable attribute so report on value change
      this.triggers.setReportedOnValueChange(false);
   }

   protected Stream<? extends ReflexDevice> getDevices() {
      return parent.getDevices().stream().filter((dev) -> dev != null && isSupported(dev));
   }

   protected Stream<? extends ReflexDevice> getParticipatingDevices() {
      return getDevices();
   }

   protected abstract boolean isSupported(ReflexDevice device);
   protected abstract boolean isTriggered(ReflexDevice device);
   protected abstract String getIncidentAlarmType();

   /////////////////////////////////////////////////////////////////////////////
   // LifeCycle API for Alarms
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public void reloaded(Map<String,String> state) {
      log.trace("reloaded with initial state: {}", state);
   }

   @Override
   public void markNeedsReporting() {
      this.needsToReport = true;
   }

   @Override
   public void clearNeedsReporting() {
      this.needsToReport = false;
   }

   @Override
   public boolean isReportingNeeded() {
      return this.needsToReport;
   }

   @Override
   public void clearTriggers() {
      triggers.set(new ArrayList<>());
   }

   /////////////////////////////////////////////////////////////////////////////
   // Incident API for Alarms
   /////////////////////////////////////////////////////////////////////////////
   
   protected void addIncident(String source, Trigger trigger) {
      String event = null;
      switch (trigger) {
      case PANIC:
         if(source.contains(RuleCapability.NAMESPACE)) {
            event = IncidentTrigger.EVENT_RULE;
         } else if(source.startsWith("PROT")) {
            event = IncidentTrigger.EVENT_KEYPAD;
         } else {
            event = IncidentTrigger.EVENT_VERIFIED_ALARM;
         }
         break;
      case CONTACT:
         event = IncidentTrigger.EVENT_CONTACT;
         break;
      case MOTION:
         event = IncidentTrigger.EVENT_MOTION;
         break;
      case SMOKE:
         event = IncidentTrigger.EVENT_SMOKE;
         break;
      case CO:
         event = IncidentTrigger.EVENT_CO;
         break;
      case WATER:
         event = IncidentTrigger.EVENT_LEAK;
         break;
      case GLASS:
         event = IncidentTrigger.EVENT_GLASS;
         break;
      case DOOR:
         event = IncidentTrigger.EVENT_CONTACT;
         break;
      default:
         // ignore
         break;
      }

      if (event != null) {
         addIncident(source, event);
      }
   }

   protected void addIncident(String source, String event) {
      if(!canStateAddIncident()) {
         return;
      }

      List<Map<String,Object>> currentTriggers = triggers.get();
      if (currentTriggers.size() < MAX_TRIGGERS) {
         IncidentTrigger trigger = new IncidentTrigger();
         trigger.setSource(source);
         trigger.setTime(new Date());
         trigger.setEvent(event);
         trigger.setAlarm(getIncidentAlarmType());
         currentTriggers.add(trigger.toMap());
         triggers.persist();

         markNeedsReporting();
      }
   }

   protected boolean canStateAddIncident() {
      String state = alertState.get();
      switch (state) {
         case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
         case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
         case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
            return false;

         default:
            return true;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // State Machine API for Alarms
   /////////////////////////////////////////////////////////////////////////////
   
   protected void exitInactive() {
   }
   
   protected void exitDisarmed() {
   }
   
   protected void exitArming() {
   }
   
   protected void exitReady() {
   }
   
   protected void exitPrealert() {
   }
   
   protected void exitAlert() {
   }

   protected void exitPendingClear() {
   }

   protected void exitClearing() {
   }
   
   protected void enterInactive() {
      clearTriggers();
   }
   
   protected void enterDisarmed() {
   }
   
   protected void enterArming() {
   }
   
   protected void enterReady() {
   }
   
   protected void enterPrealert() {
   }
   
   protected void enterAlert() {
      onAlert();
   }

   protected void enterPendingClear() {
   }

   protected void enterClearing() {
   }

   protected boolean canHandleInactiveEvent(Event event) {
      return true;
   }
   
   protected boolean canHandleDisarmedEvent(Event event) {
      return true;
   }
   
   protected boolean canHandleArmingEvent(Event event) {
      if((event instanceof DisarmEvent)) {
         return true;
      }
      return false;
   }
   
   protected boolean canHandleReadyEvent(Event event) {
      return true;
   }
   
   protected boolean canHandlePrealertEvent(Event event) {
      if((event instanceof DisarmEvent)) {
         return true;
      }
      return false;
   }
   
   protected boolean canHandleAlertEvent(Event event) {
      if((event instanceof DisarmEvent)) {
         return true;
      }
      return false;
   }

   protected boolean canHandlePendingClearEvent(Event event) {
      if((event instanceof ClearEvent) ||
         (event instanceof DisarmEvent)) {
         return true;
      }
      return false;
   }

   protected boolean canHandleClearingEvent(Event event) {
      return false;
   }
   
   protected void handleInactiveEvent(Event event) {
   }
   
   protected void handleDisarmedEvent(Event event) {
   }
   
   protected void handleArmingEvent(Event event) {
   }
   
   protected void handleReadyEvent(Event event) {
   }
   
   protected void handlePrealertEvent(Event event) {
   }
   
   protected void handleAlertEvent(Event event) {
   }

   protected void handlePendingClearEvent(Event event) {
   }

   protected void handleClearingEvent(Event event) {
   }

   protected void transitionTo(final String from, final String state) {
      if (from.equals(alertState.get())) {
         transitionTo(state);
      }
   }

   protected void transitionTo(final String state) {
      switch (state) {
      case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
      case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
      case HubAlarmCapability.SECURITYALERTSTATE_READY:
      case HubAlarmCapability.SECURITYALERTSTATE_PREALERT:
      case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
      case HubAlarmCapability.SECURITYALERTSTATE_CLEARING:
         // valid
         break;

      case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
         // Show disabled LEDs
         IrisHal.setLedState(LEDState.ALARM_OFF);
         break;

      case HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR:
         // Turn off alarm LEDs on v3 hub
         if (Model.isV3(IrisHal.getModel())) {
            IrisHal.setLedState(LEDState.ALL_OFF);
         }
         break;

      default:
         throw new IllegalStateException("unknown state '" + state + "'");
      }

      // If we were in a repeating alert sound state, stop sound so we can cleanly play
      //  next sound without a collision
      if (getAlertState().contentEquals(HubAlarmCapability.SECURITYALERTSTATE_ARMING) ||
            getAlertState().contentEquals(HubAlarmCapability.SECURITYALERTSTATE_PREALERT) ||
            getAlertState().contentEquals(HubAlarmCapability.SECURITYALERTSTATE_ALERT)) {
         IrisHal.setSounderMode(SounderMode.NO_SOUND);
      }

      // Only need to play these sounds in hubv3 case...
      if (Model.isV3(IrisHal.getModel())) {
         IrisHal.setSounderMode(AlarmSoundConfig.getTransition(alertState.get(),state,this));
      }
      
      log.debug("{} transitioning {} -> {}", getName(), getAlertState(), state);
      exitExistingState(alertState.get());
      enterNewState(state);
      markNeedsReporting();
   }

   private void exitExistingState(String state) {
      switch (state) {
      case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
         exitInactive();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
         exitDisarmed();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
         exitArming();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_READY:
         exitReady();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_PREALERT:
         exitPrealert();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
         exitAlert();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR:
         exitPendingClear();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_CLEARING:
         exitClearing();
         break;
      default:
         throw new IllegalStateException("unknown state '" + state + "'");
      }
   }

   private void enterNewState(String state) {
      switch (state) {
      case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
         alertState.set(state);
         enterInactive();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
         alertState.set(state);
         enterDisarmed();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
         alertState.set(state);
         enterArming();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_READY:
         alertState.set(state);
         enterReady();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_PREALERT:
         alertState.set(state);
         enterPrealert();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
         alertState.set(state);
         enterAlert();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR:
         alertState.set(state);
         enterPendingClear();
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_CLEARING:
         alertState.set(state);
         enterClearing();
         break;
      default:
         throw new IllegalStateException("unknown state '" + state + "'");
      }
   }

   private boolean currentStateCanHandleEvent(Event event) {
      String state = alertState.get();
      switch (state) {
      case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
         return canHandleInactiveEvent(event);
      case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
         return canHandleDisarmedEvent(event);
      case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
         return canHandleArmingEvent(event);
      case HubAlarmCapability.SECURITYALERTSTATE_READY:
         return canHandleReadyEvent(event);
      case HubAlarmCapability.SECURITYALERTSTATE_PREALERT:
         return canHandlePrealertEvent(event);
      case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
         return canHandleAlertEvent(event);
      case HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR:
        return canHandlePendingClearEvent(event);
      case HubAlarmCapability.SECURITYALERTSTATE_CLEARING:
         return canHandleClearingEvent(event);
      default:
         log.warn("unknown state '" + state + "'");
         return false;
      }
   }

   private void currentStateHandleEvent(Event event) {
      String state = alertState.get();
      switch (state) {
      case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
         handleInactiveEvent(event);
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
         handleDisarmedEvent(event);
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
         handleArmingEvent(event);
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_READY:
         handleReadyEvent(event);
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_PREALERT:
         handlePrealertEvent(event);
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
         handleAlertEvent(event);
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR:
         handlePendingClearEvent(event);
         break;
      case HubAlarmCapability.SECURITYALERTSTATE_CLEARING:
         handleClearingEvent(event);
         break;
      default:
         log.warn("unknown state '" + state + "'");
         break;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Event API for Alarms
   /////////////////////////////////////////////////////////////////////////////
   
   protected abstract boolean isTriggerInteresting(TriggerEvent event);

   protected boolean isExcluded(String source) {
      return false;
   }

   @Override
   public boolean canHandleEvent(AlarmEvents.Event event) {
      if (event instanceof TriggerEvent) {
         return true;
      }

      return currentStateCanHandleEvent(event);
   }

   @Override
   public void handleEvent(AlarmEvents.Event event) {
      if (event instanceof TriggerEvent) {
         TriggerEvent tevent = (TriggerEvent)event;
         if (isTriggerInteresting(tevent)) {
            if (tevent.isTriggered()) {
               addIncident(tevent.getSource(), tevent.getTrigger());
            }

            markNeedsReporting();
            currentStateHandleEvent(event);
         }
      } else {
         currentStateHandleEvent(event);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Set APIs for Alarms
   /////////////////////////////////////////////////////////////////////////////

   protected boolean addToAttributeSet(HubAttributesService.Attribute<Set<String>> attr, String value) {
      Set<String> triggered = new LinkedHashSet<>();
      triggered.addAll(attr.get());
      boolean result = triggered.add(value);

      attr.set(triggered);
      return result;
   }

   protected boolean removeFromAttributeSet(HubAttributesService.Attribute<Set<String>> attr, String value) {
      Set<String> triggered = new LinkedHashSet<>();
      triggered.addAll(attr.get());
      boolean result = triggered.remove(value);

      attr.set(triggered);
      return result;
   }

   protected void setAttributeSet(HubAttributesService.Attribute<Set<String>> attr, Set<String> values) {
      attr.set(new LinkedHashSet<>(values));
   }

   protected void clearAttributeSet(HubAttributesService.Attribute<Set<String>> attr) {
      attr.set(new LinkedHashSet<>());
   }

   protected boolean isAttributeSetEmpty(HubAttributesService.Attribute<Set<String>> attr) {
      Set<String> value = attr.get();
      return value == null || value.isEmpty();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Misc APIs for Alarms
   /////////////////////////////////////////////////////////////////////////////
   
   public String incident() {
      return parent.getCurrentIncident();
   }

   public void defer(Object msg) {
      parent.defer(msg);
   }
   
   public void submit(Runnable task) {
      parent.submit(task);
   }

   public Timeout schedule(Runnable task, long time, TimeUnit unit) {
      return parent.schedule(task, time, unit);
   }

   public void onArmFailed() {
      parent.onArmFailed();
   }
   
   public void onArmed(AlarmEvents.Mode mode, boolean soundsEnabled) {
      parent.onArmed(mode, soundsEnabled);
   }

   public void onArming(AlarmEvents.Mode mode, int exitDelay, boolean soundsEnabled) {
      parent.onArming(mode, exitDelay, silent.get(), soundsEnabled);
   }

   public void onPrealert(String mode, int entranceDelay, boolean soundsEnabled) {
      parent.onPrealert(mode, entranceDelay, soundsEnabled);
   }

   public void onAlert() {
      parent.onAlert();
   }

   @Override
   public void updateReportAttributes(Map<String,Object> attrs) {
      attrs.put(alertState.name(), alertState.get());
      attrs.put(silent.name(), silent.get());
      attrs.put(offlineDevices.name(), offlineDevices.get());
      attrs.put(triggeredDevices.name(), triggeredDevices.get());
      attrs.put(triggers.name(), triggers.get());
   }

   @Override
   public String getAlertState() {
      return alertState.get();
   }

   @Override
   public boolean isSilent() {
      return silent.get();
   }

   @Override
   public void onDeviceOnline(Address device) {
   }

   @Override
   public void onDeviceOffline(Address device) {
   }

   @Override
   public void onVerified() {
   }
}

