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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.reflex.ReflexDevice;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.type.IncidentTrigger;
import com.iris.util.IrisUUID;

import static com.iris.agent.alarm.AlarmEvents.*;

import io.netty.util.Timeout;

public class AlarmSecurity extends AbstractAlarm {
   private static final Logger log = LoggerFactory.getLogger(AlarmSecurity.class);
   public static final String NAME = "security";

   private final HubAttributesService.Attribute<String> securityMode;
   private final HubAttributesService.Attribute<Long> securityArmTime;

   private final HubAttributesService.Attribute<Long> lastArmedTime;
   private final HubAttributesService.Attribute<String> lastArmedBy;
   private final HubAttributesService.Attribute<String> lastArmedFrom;

   private final HubAttributesService.Attribute<Long> lastDisarmedTime;
   private final HubAttributesService.Attribute<String> lastDisarmBy;
   private final HubAttributesService.Attribute<String> lastDisarmFrom;

   private final HubAttributesService.Attribute<Set<String>> devices;
   protected final HubAttributesService.Attribute<Set<String>> activeDevices;
   protected final HubAttributesService.Attribute<Set<String>> excludedDevices;

   private final HubAttributesService.Attribute<Integer> entranceDelay = HubAttributesService.persisted(Integer.class, HubAlarmCapability.ATTR_SECURITYENTRANCEDELAY, 30);
   private final HubAttributesService.Attribute<Integer> sensitivity = HubAttributesService.persisted(Integer.class, HubAlarmCapability.ATTR_SECURITYSENSITIVITY, 1);
   private final HubAttributesService.Attribute<Set<String>> currActive = HubAttributesService.persistedSet(String.class, HubAlarmCapability.ATTR_SECURITYCURRENTACTIVE, new LinkedHashSet<>());
   private final HubAttributesService.Attribute<Long> prealertEnd = HubAttributesService.persisted(Long.class, HubAlarmCapability.ATTR_SECURITYPREALERTENDTIME, 0L);

   private @Nullable UUID lastArmId;
   private @Nullable UUID lastEntryDelayId;
   private @Nullable String lastArmedByTmp;
   private @Nullable String lastArmedFromTmp;
   private @Nullable Timeout clearTriggersTimeout;

   private long exitDelay = TimeUnit.SECONDS.toMillis(30);
   private boolean soundsEnabled = true;

   public AlarmSecurity(AlarmController parent) {
      super(parent, log, NAME, HubAlarmCapability.SECURITYALERTSTATE_INACTIVE);

      this.securityMode = HubAttributesService.persisted(String.class, HubAlarmCapability.ATTR_SECURITYMODE, HubAlarmCapability.SECURITYMODE_INACTIVE);
      this.securityArmTime = HubAttributesService.persisted(Long.class, HubAlarmCapability.ATTR_SECURITYARMTIME, null);

      this.lastArmedTime = HubAttributesService.persisted(Long.class, HubAlarmCapability.ATTR_LASTARMEDTIME, null);
      this.lastArmedBy = HubAttributesService.persisted(String.class, HubAlarmCapability.ATTR_LASTARMEDBY, "");
      this.lastArmedFrom = HubAttributesService.persisted(String.class, HubAlarmCapability.ATTR_LASTARMEDFROM, "");

      this.lastDisarmedTime = HubAttributesService.persisted(Long.class, HubAlarmCapability.ATTR_LASTDISARMEDTIME, null);
      this.lastDisarmBy = HubAttributesService.persisted(String.class, HubAlarmCapability.ATTR_LASTDISARMEDBY, "");
      this.lastDisarmFrom = HubAttributesService.persisted(String.class, HubAlarmCapability.ATTR_LASTDISARMEDFROM, "");

      this.devices = HubAttributesService.computedSet(String.class, HubAlarmCapability.ATTR_SECURITYDEVICES, () -> getDevices()
         .map((dev) -> dev.getAddress().getRepresentation())
         .collect(Collectors.toSet()));

      this.activeDevices = HubAttributesService.computedSet(String.class, HubAlarmCapability.ATTR_SECURITYACTIVEDEVICES, new Supplier<Set<String>>() {
         @Override
         public Set<String> get() {
            return getDevices()
               .filter((dev) -> currActive.get().contains(dev.getAddress().getRepresentation()) && !dev.isOffline() && !isTriggered(dev))
               .map((dev) -> dev.getAddress().getRepresentation())
               .collect(Collectors.toSet());
         }
      });

      this.excludedDevices = HubAttributesService.persistedSet(String.class, HubAlarmCapability.ATTR_SECURITYEXCLUDEDDEVICES, new LinkedHashSet<>());

      this.securityMode.setReportedOnConnect(false);
      this.securityArmTime.setReportedOnConnect(false);
      this.lastArmedTime.setReportedOnConnect(false);
      this.lastArmedFrom.setReportedOnConnect(false);
      this.lastArmedBy.setReportedOnConnect(false);
      this.lastDisarmedTime.setReportedOnConnect(false);
      this.lastDisarmFrom.setReportedOnConnect(false);
      this.lastDisarmBy.setReportedOnConnect(false);
      this.devices.setReportedOnConnect(false);
      this.activeDevices.setReportedOnConnect(false);
      this.excludedDevices.setReportedOnConnect(false);
      this.entranceDelay.setReportedOnConnect(false);
      this.sensitivity.setReportedOnConnect(false);
      this.currActive.setReportedOnConnect(false);
      this.prealertEnd.setReportedOnConnect(false);

      this.securityMode.setReportedOnValueChange(false);
      this.securityArmTime.setReportedOnValueChange(false);
      this.lastArmedTime.setReportedOnValueChange(false);
      this.lastArmedFrom.setReportedOnValueChange(false);
      this.lastArmedBy.setReportedOnValueChange(false);
      this.lastDisarmedTime.setReportedOnValueChange(false);
      this.lastDisarmFrom.setReportedOnValueChange(false);
      this.lastDisarmBy.setReportedOnValueChange(false);
      this.devices.setReportedOnValueChange(false);
      this.activeDevices.setReportedOnValueChange(false);
      this.excludedDevices.setReportedOnValueChange(false);
      this.entranceDelay.setReportedOnValueChange(false);
      this.sensitivity.setReportedOnValueChange(false);
      this.currActive.setReportedOnValueChange(false);
      this.prealertEnd.setReportedOnValueChange(false);

      // TODO: need to decide what the policy for moving out of ARMING after reboot is
      if (HubAlarmCapability.SECURITYALERTSTATE_ARMING.equals(alertState.get())) {
         if (HubAlarmCapability.SECURITYMODE_DISARMED.equals(securityMode.get())) {
            alertState.compareAndSet(HubAlarmCapability.SECURITYALERTSTATE_ARMING, HubAlarmCapability.SECURITYALERTSTATE_DISARMED);
         } else {
            alertState.compareAndSet(HubAlarmCapability.SECURITYALERTSTATE_ARMING, HubAlarmCapability.SECURITYALERTSTATE_READY);
         }
      } else if (HubAlarmCapability.SECURITYALERTSTATE_CLEARING.equals(alertState.get())) {
         if (isAttributeSetEmpty(triggeredDevices)) {
            alertState.compareAndSet(HubAlarmCapability.SECURITYALERTSTATE_CLEARING, HubAlarmCapability.SECURITYALERTSTATE_DISARMED);
         }
      } else if(HubAlarmCapability.SECURITYALERTSTATE_PREALERT.equals(alertState.get())) {
         alertState.compareAndSet(HubAlarmCapability.SECURITYALERTSTATE_PREALERT, HubAlarmCapability.SECURITYALERTSTATE_ALERT);
      }
   }

   @Override
   protected boolean isExcluded(String source) {
      return excludedDevices.get().contains(source);
   }

   @Override
   protected boolean isTriggerInteresting(TriggerEvent event) {
      return (event.getTrigger() == Trigger.CONTACT ||
              event.getTrigger() == Trigger.MOTION ||
              event.getTrigger() == Trigger.GLASS ||
              event.getTrigger() == Trigger.DOOR) &&
              currActive.get().contains(event.getSource());
   }

   @Override
   protected boolean isSupported(ReflexDevice device) {
      return device.getCapabilities().contains(ContactCapability.NAME) ||
             device.getCapabilities().contains(MotionCapability.NAME) ||
             device.getCapabilities().contains(GlassCapability.NAME) ||
             device.getCapabilities().contains(MotorizedDoorCapability.NAME);
   }

   @Override
   protected boolean isTriggered(ReflexDevice device) {
      return ContactCapability.CONTACT_OPENED.equals(device.getAttribute(ContactCapability.ATTR_CONTACT)) ||
             MotionCapability.MOTION_DETECTED.equals(device.getAttribute(MotionCapability.ATTR_MOTION)) ||
             GlassCapability.BREAK_DETECTED.equals(device.getAttribute(GlassCapability.ATTR_BREAK)) ||
             MotorizedDoorCapability.DOORSTATE_OPEN.equals(device.getAttribute(MotorizedDoorCapability.ATTR_DOORSTATE)) ||
             MotorizedDoorCapability.DOORSTATE_OPENING.equals(device.getAttribute(MotorizedDoorCapability.ATTR_DOORSTATE)) ||
             MotorizedDoorCapability.DOORSTATE_OBSTRUCTION.equals(device.getAttribute(MotorizedDoorCapability.ATTR_DOORSTATE));
   }

   @Override
   protected String getIncidentAlarmType() {
      return IncidentTrigger.ALARM_SECURITY;
   }

   @Override
   public void updateReportAttributes(Map<String,Object> attrs) {
      super.updateReportAttributes(attrs);

      attrs.put(securityMode.name(), securityMode.get());
      attrs.put(securityArmTime.name(), securityArmTime.get());

      attrs.put(lastArmedTime.name(), lastArmedTime.get());
      attrs.put(lastArmedBy.name(), lastArmedBy.get());
      attrs.put(lastArmedFrom.name(), lastArmedFrom.get());

      attrs.put(lastDisarmedTime.name(), lastDisarmedTime.get());
      attrs.put(lastDisarmBy.name(), lastDisarmBy.get());
      attrs.put(lastDisarmFrom.name(), lastDisarmFrom.get());

      attrs.put(devices.name(), devices.get());
      attrs.put(activeDevices.name(), activeDevices.get());
      attrs.put(excludedDevices.name(), excludedDevices.get());

      attrs.put(prealertEnd.name(), prealertEnd.get());
   }

   @Override
   protected Stream<? extends ReflexDevice> getParticipatingDevices() {
      return getDevices().filter((d) -> currActive.get().contains(d.getAddress().getRepresentation()));
   }

   @Override
   public void afterProcessReflexDevices() {
      if(isAttributeSetEmpty(devices)) {
         transitionTo(AlarmCapability.ALERTSTATE_INACTIVE);
      } else if(Objects.equals(AlarmCapability.ALERTSTATE_INACTIVE, getAlertState())) {
         transitionTo(AlarmCapability.ALERTSTATE_DISARMED);
      }
   }

   @Override
   public void onDeviceOnline(Address device) {
      clearExcluded(device.getRepresentation());
   }

   @Override
   protected boolean canStateAddIncident() {
      return !Objects.equals(HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR, getAlertState()) && super.canStateAddIncident();
   }

   @Override
   public int getPriority() {
      return 4;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Disarmed State
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void enterDisarmed() {
      super.enterDisarmed();

      securityMode.set(HubAlarmCapability.SECURITYMODE_DISARMED);
      prealertEnd.set(0L);
      clearAttributeSet(currActive);
      clearTriggers();

      clearAttributeSet(excludedDevices);
   }
   
   protected void handleDisarmedEvent(Event event) {
      if (event instanceof ArmEvent) {
         doArm((ArmEvent)event);
      } else {
         super.handleDisarmedEvent(event);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Arming State
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void enterArming() {
      super.enterArming();

      final UUID armId = IrisUUID.randomUUID();
      lastArmId = armId;

      schedule(new Runnable() {
         @Override
         public void run() {
            if (Objects.equals(lastArmId, armId)) {
               transitionTo(HubAlarmCapability.SECURITYALERTSTATE_ARMING, HubAlarmCapability.SECURITYALERTSTATE_READY);
            }
         }
      }, exitDelay, TimeUnit.MILLISECONDS);

      securityArmTime.set(System.currentTimeMillis() + exitDelay);
      onArming(HubAlarmCapability.SECURITYMODE_ON.equals(securityMode.get()) ? Mode.ON : Mode.PARTIAL, (int) TimeUnit.MILLISECONDS.toSeconds(exitDelay), soundsEnabled);
   }
   
   @Override
   protected void handleArmingEvent(Event event) {
      if (event instanceof DisarmEvent) {
         doDisarm((DisarmEvent)event);
      } else if (event instanceof TriggerEvent) {
         TriggerEvent trigger = (TriggerEvent) event;
         if(!trigger.isTriggered()) {
            clearExcluded(event.getSource());
         } else {
            Set<String> excluded = excludedDevices.get();
            if(excluded.add(event.getSource())) {
               excludedDevices.set(excluded);
            }
         }
      } else {
         super.handleArmingEvent(event);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Ready State
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void enterReady() {
      super.enterReady();

      lastArmedBy.set(lastArmedByTmp);
      lastArmedByTmp = "";
      lastArmedFrom.set(lastArmedFromTmp);
      lastArmedFromTmp = "";
      lastArmedTime.set(System.currentTimeMillis());
      onArmed(HubAlarmCapability.SECURITYMODE_ON.equals(securityMode.get()) ? Mode.ON : Mode.PARTIAL, soundsEnabled);
   }
   
   @Override
   protected void handleReadyEvent(Event event) {
      if(event instanceof ArmEvent || event instanceof ClearEvent) {
         return;
      }

      if (event instanceof DisarmEvent) {
         // clear triggers because some may have been motion sensors but the sensitivity never met
         clearTriggers();
         doDisarm((DisarmEvent)event);
         return;
      }

      if(clearExcluded(event)) {
         super.handleReadyEvent(event);
         return;
      }

      if(event instanceof TriggerEvent) {
         TriggerEvent ev = (TriggerEvent) event;
         if(!ev.isTriggered() && wasMotionTrigger(event.getSource()) && allMotionCleared()) {
            if(clearTriggersTimeout != null) {
               clearTriggersTimeout.cancel();
            }
            clearTriggersTimeout = schedule(() -> {
               log.debug("clearing triggered because no motion was detected for five minutes");
               clearTriggers();
            }, 5, TimeUnit.MINUTES);
            return;
         }
      }

      if(!isAttributeSetEmpty(triggeredDevices)) {
         boolean wasMotion = wasMotionTrigger(event.getSource());
         if(wasMotion) {
            int count = (int) triggers.get().stream()
               .filter((m) -> Objects.equals(m.get(IncidentTrigger.ATTR_EVENT), IncidentTrigger.EVENT_MOTION))
               .map((m) -> m.get(IncidentTrigger.ATTR_SOURCE))
               .distinct()
               .count();
            if(count >= sensitivity.get()) {
               transitionTo(HubAlarmCapability.SECURITYALERTSTATE_PREALERT);
            }
         } else {
            transitionTo(HubAlarmCapability.SECURITYALERTSTATE_PREALERT);
         }
      } else {
         super.handleReadyEvent(event);
      }
   }

   @Override
   protected void exitReady() {
      if(clearTriggersTimeout != null) {
         clearTriggersTimeout.cancel();
         clearTriggersTimeout = null;
      }
      super.exitReady();
   }

   private boolean allMotionCleared() {
      int count = (int) triggers.get().stream()
         .filter((m) -> {
            if(Objects.equals(m.get(IncidentTrigger.ATTR_EVENT), IncidentTrigger.EVENT_MOTION)) {
               Optional<? extends ReflexDevice> device = getParticipatingDevices().filter((d) -> Objects.equals(d.getAddress().getRepresentation(), m.get(IncidentTrigger.ATTR_SOURCE))).findFirst();
               if(!device.isPresent()) {
                  return false;
               }
               return Objects.equals(MotionCapability.MOTION_DETECTED, device.get().getAttribute(MotionCapability.ATTR_MOTION));
            }
            return false;
         })
         .count();
      return count == 0;
   }

   private boolean wasMotionTrigger(String source) {
      return getParticipatingDevices().anyMatch((d) -> Objects.equals(d.getAddress().getRepresentation(), source) && d.getCapabilities().contains(MotionCapability.NAME));
   }

   private boolean clearExcluded(Event event) {
      if (event instanceof TriggerEvent && !((TriggerEvent) event).isTriggered()) {
         return clearExcluded(event.getSource());
      }
      return false;
   }

   private boolean clearExcluded(String addr) {
      if(addr == null) {
         return false;
      }
      Set<String> excluded = excludedDevices.get();
      if(excluded.remove(addr)) {
         excludedDevices.set(excluded);
         return true;
      }
      return false;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Prealert State
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void enterPrealert() {
      super.enterPrealert();

      final UUID entryDelayId = IrisUUID.randomUUID();
      lastEntryDelayId = entryDelayId;

      schedule(new Runnable() {
         @Override
         public void run() {
            if (Objects.equals(lastEntryDelayId, entryDelayId)) {
               transitionTo(HubAlarmCapability.SECURITYALERTSTATE_PREALERT, HubAlarmCapability.SECURITYALERTSTATE_ALERT);
            }
         }
      }, entranceDelay.get(), TimeUnit.SECONDS);
      prealertEnd.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(entranceDelay.get()));

      boolean sounds = soundsEnabled && !silent.get();
      onPrealert(securityMode.get(), entranceDelay.get(), sounds);
   }
   
   @Override
   protected void handlePrealertEvent(Event event) {
      if(event instanceof ClearEvent) {
         return;
      }
      if (event instanceof DisarmEvent) {
         doDisarm((DisarmEvent)event);
      } else {
         clearExcluded(event);
         super.handlePrealertEvent(event);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Alert State
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void handleAlertEvent(Event event) {
      if(event instanceof ClearEvent) {
         return;
      }
      if (event instanceof DisarmEvent) {
         doDisarm((DisarmEvent)event);
      } else {
         clearExcluded(event);
         super.handleAlertEvent(event);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // PendingClear State
   /////////////////////////////////////////////////////////////////////////////

   @Override
   protected void enterPendingClear() {
      super.enterPendingClear();
   }

   @Override
   protected void handlePendingClearEvent(Event event) {
      if(event instanceof ClearEvent) {
         clearTriggers();
         prealertEnd.set(0L);
         transitionTo(HubAlarmCapability.SECURITYALERTSTATE_DISARMED);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation Details
   /////////////////////////////////////////////////////////////////////////////

   private boolean doArm(ArmEvent event) {

      Set<String> prevActive = currActive.get();
      currActive.set(new LinkedHashSet<String>(event.getActiveDevices()));

      if(!event.isBypass() && (!isAttributeSetEmpty(triggeredDevices) || !isAttributeSetEmpty(offlineDevices))) {
         Set<String> allDevices = new HashSet<>(triggeredDevices.get());
         allDevices.addAll(offlineDevices.get());
         currActive.set(prevActive);
         throw new ErrorEventException("security.triggeredDevices", StringUtils.join(allDevices, ','));
      }

      if(!hasSufficientDevices(event.getAlarmSensitivityDeviceCount())) {
         currActive.set(prevActive);
         onArmFailed();
         throw new ErrorEventException("security.insufficientDevices", "There are less than " + event.getAlarmSensitivityDeviceCount() + " motion sensors online and clear");
      }

      switch (event.getMode()) {
         case ON:
            securityMode.set(HubAlarmCapability.SECURITYMODE_ON);
            break;

         case PARTIAL:
            securityMode.set(HubAlarmCapability.SECURITYMODE_PARTIAL);
            break;

         default:
            log.warn("unknown arm mode: {}", event.getMode());
            currActive.set(prevActive);
            throw new ErrorEventException(Errors.invalidParam("mode"));
      }

      if(event.isBypass()) {
         Set<String> excluded = new HashSet<>(triggeredDevices.get());
         excluded.addAll(offlineDevices.get());
         setAttributeSet(excludedDevices, excluded);
      }

      lastArmedFromTmp = event.getSource() == null ? "" : event.getSource();
      lastArmedByTmp = event.getActor() == null ? "" : event.getActor();
      exitDelay = TimeUnit.SECONDS.toMillis(event.getExitDelaySecs());
      entranceDelay.set(event.getEntranceDelaySecs());
      sensitivity.set(event.getAlarmSensitivityDeviceCount() > 0 ? event.getAlarmSensitivityDeviceCount() : 1);
      silent.set(event.isSilent());
      soundsEnabled = event.isSoundsEnabled();

      transitionTo(HubAlarmCapability.SECURITYALERTSTATE_ARMING);
      return true;
   }

   private boolean hasSufficientDevices(int sensitivity) {
      Set<String> ready = activeDevices.get();
      if(ready.isEmpty() || ready.size() < sensitivity) {
         return false;
      }

      return getDevices()
         .filter((d) -> ready.contains(d.getAddress().getRepresentation()))
         .anyMatch((d) -> !d.getCapabilities().contains(MotionCapability.NAMESPACE));
   }

   private boolean doDisarm(DisarmEvent event) {
      lastDisarmFrom.set(event.getSource() == null ? "" : event.getSource());
      lastDisarmBy.set(event.getActor() == null ? "" : event.getActor());
      lastDisarmedTime.set(System.currentTimeMillis());
      clearAttributeSet(excludedDevices);

      if(triggers.get().isEmpty()) {
         transitionTo(HubAlarmCapability.SECURITYALERTSTATE_DISARMED);
      } else {
         transitionTo(HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR);
      }

      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Misc APIs
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public String getName() {
      return NAME;
   }

   public HubAttributesService.Attribute<String> getSecurityMode() {
      return securityMode;
   }

   public HubAttributesService.Attribute<Long> getSecurityArmTime() {
      return securityArmTime;
   }

   public HubAttributesService.Attribute<Long> getLastArmedTime() {
      return lastArmedTime;
   }

   public HubAttributesService.Attribute<String> getLastArmedBy() {
      return lastArmedBy;
   }

   public HubAttributesService.Attribute<String> getLastArmedFrom() {
      return lastArmedFrom;
   }

   public HubAttributesService.Attribute<Long> getLastDisarmedTime() {
      return lastDisarmedTime;
   }

   public HubAttributesService.Attribute<String> getLastDisarmBy() {
      return lastDisarmBy;
   }

   public HubAttributesService.Attribute<String> getLastDisarmFrom() {
      return lastDisarmFrom;
   }

   @Override
   public void onVerified() {
      if(HubAlarmCapability.SECURITYALERTSTATE_PREALERT.equals(getAlertState())) {
         transitionTo(HubAlarmCapability.PANICALERTSTATE_ALERT);
      }
   }
}

