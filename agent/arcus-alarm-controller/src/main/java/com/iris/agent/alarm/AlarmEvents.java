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

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubAlarmCapability;

public final class AlarmEvents {
   public static enum Mode { PARTIAL, ON }
   public static enum Trigger { PANIC, CONTACT, MOTION, SMOKE, CO, WATER, GLASS, DOOR }

   private AlarmEvents() {
   }

   public static Event activate(Address addr, @Nullable Address actor) {
      return new ActivateEvent(addr, actor);
   }

   public static Event suspend(Address addr, @Nullable Address actor) {
      return new SuspendEvent(addr, actor);
   }
   
   public static Event arm(Address addr, @Nullable Address actor, @NonNull MessageBody armRequest) {
      String armModeRaw = HubAlarmCapability.ArmRequest.getMode(armRequest);
      AlarmEvents.Mode armMode = "PARTIAL".equalsIgnoreCase(armModeRaw) ? AlarmEvents.Mode.PARTIAL : AlarmEvents.Mode.ON;

      Boolean bypassRaw = HubAlarmCapability.ArmRequest.getBypassed(armRequest);
      boolean bypass = bypassRaw == null ? false : bypassRaw.booleanValue();

      Integer entranceDelayRaw = HubAlarmCapability.ArmRequest.getEntranceDelaySecs(armRequest);
      int entranceDelay = entranceDelayRaw == null ? 30 : entranceDelayRaw.intValue();

      Integer exitDelayRaw = HubAlarmCapability.ArmRequest.getExitDelaySecs(armRequest);
      int exitDelay = exitDelayRaw == null ? 30 : exitDelayRaw.intValue();

      Integer alarmSensitivityRaw = HubAlarmCapability.ArmRequest.getAlarmSensitivityDeviceCount(armRequest);
      int alarmSensitivity = alarmSensitivityRaw == null ? 1 : alarmSensitivityRaw.intValue();

      Boolean silentRaw = HubAlarmCapability.ArmRequest.getSilent(armRequest);
      boolean silent = silentRaw == null ? false : silentRaw.booleanValue();

      Boolean soundsRaw = HubAlarmCapability.ArmRequest.getSoundsEnabled(armRequest);
      boolean sounds = soundsRaw == null ? false : soundsRaw.booleanValue();

      Collection<String> activeRaw = HubAlarmCapability.ArmRequest.getActiveDevices(armRequest);
      Set<String> active = activeRaw == null ? ImmutableSet.of() : ImmutableSet.copyOf(activeRaw);

      return new ArmEvent(addr, actor, armMode, bypass, entranceDelay, exitDelay, alarmSensitivity, silent, sounds, active);
   }

   public static Event disarm(Address addr, @Nullable Address actor) {
      return new DisarmEvent(addr, actor);
   }

   public static Event clear(Address addr, @Nullable Address actor) {
      return new ClearEvent(addr, actor);
   }

   public static Event trigger(Address addr, @Nullable Address actor, Trigger trigger, boolean triggered) {
      return new TriggerEvent(addr, actor, trigger, triggered);
   }

   public static abstract class Event {
      private final String source;
      private final @Nullable String actor;

      public Event(Address source, @Nullable Address actor) {
         this.source = source.getRepresentation();
         this.actor = (actor != null) ? actor.getRepresentation() : null;
      }

      public String getSource() {
         return source;
      }

      public @Nullable String getActor() {
         return actor;
      }
   }

   public static final class ActivateEvent extends Event {
      public ActivateEvent(Address source, @Nullable Address actor) {
         super(source, actor);
      }
   }

   public static final class SuspendEvent extends Event {
      public SuspendEvent(Address source, @Nullable Address actor) {
         super(source, actor);
      }
   }

   public static final class ArmEvent extends Event {
      private final Mode mode;
      private final boolean bypass;
      private final int entranceDelaySecs;
      private final int exitDelaySecs;
      private final int alarmSensitivityDeviceCount;
      private final boolean silent;
      private final boolean soundsEnabled;
      private final Set<String> activeDevices;

      public ArmEvent(
         Address source,
         @Nullable Address actor,
         Mode mode,
         boolean bypass,
         int entranceDelaySecs,
         int exitDelaySecs,
         int alarmSensitivityDeviceCount,
         boolean silent,
         boolean soundsEnabled,
         Set<String> activeDevices
      ) {
         super(source, actor);
         this.mode = mode;
         this.bypass = bypass;
         this.entranceDelaySecs = entranceDelaySecs;
         this.exitDelaySecs = exitDelaySecs;
         this.alarmSensitivityDeviceCount = alarmSensitivityDeviceCount;
         this.silent = silent;
         this.soundsEnabled = soundsEnabled;
         this.activeDevices = activeDevices == null ? ImmutableSet.of() : ImmutableSet.copyOf(activeDevices);
      }

      public Mode getMode() {
         return mode;
      }

      public boolean isBypass() {
         return bypass;
      }

      public int getEntranceDelaySecs() {
         return entranceDelaySecs;
      }

      public int getExitDelaySecs() {
         return exitDelaySecs;
      }

      public int getAlarmSensitivityDeviceCount() {
         return alarmSensitivityDeviceCount;
      }

      public boolean isSilent() {
         return silent;
      }

      public boolean isSoundsEnabled() {
         return soundsEnabled;
      }

      public Set<String> getActiveDevices() {
         return activeDevices;
      }
   }

   public static final class DisarmEvent extends Event {
      public DisarmEvent(Address source, @Nullable Address actor) {
         super(source, actor);
      }
   }

   public static final class ClearEvent extends Event {
      public ClearEvent(Address source, @Nullable Address actor) {
         super(source, actor);
      }
   }
 
   public static final class TriggerEvent extends Event {
      private final Trigger trigger;
      private final boolean triggered;

      public TriggerEvent(Address source, @Nullable Address actor, Trigger trigger, boolean triggered) {
         super(source, actor);
         this.trigger = trigger;
         this.triggered = triggered;
      }

      public Trigger getTrigger() {
         return trigger;
      }

      public boolean isTriggered() {
         return triggered;
      }
   }
}

