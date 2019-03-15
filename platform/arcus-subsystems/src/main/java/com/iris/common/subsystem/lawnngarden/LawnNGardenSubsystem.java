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
package com.iris.common.subsystem.lawnngarden;

import static com.iris.messages.type.ZoneWatering.ATTR_CONTROLLER;
import static com.iris.messages.type.ZoneWatering.ATTR_TRIGGER;
import static com.iris.messages.type.ZoneWatering.ATTR_ZONE;
import static com.iris.messages.type.ZoneWatering.ATTR_ZONENAME;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.scheduler.Scheduler;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.ZoneWatering;
import com.iris.common.subsystem.lawnngarden.model.ZoneWatering.Trigger;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.subsystem.lawnngarden.model.state.IrrigationScheduleState;
import com.iris.common.subsystem.lawnngarden.model.state.IrrigationScheduleState.STATE;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenValidation;
import com.iris.common.subsystem.util.VariableMapManager;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.IrrigationControllerCapability;
import com.iris.messages.capability.IrrigationSchedulableCapability;
import com.iris.messages.capability.LawnNGardenSubsystemCapability;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.CancelSkipResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.ConfigureIntervalScheduleRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.ConfigureIntervalScheduleResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.CreateScheduleEventRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.CreateScheduleEventResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.CreateWeeklyEventRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.CreateWeeklyEventResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.DisableSchedulingRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.DisableSchedulingResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.EnableSchedulingRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.EnableSchedulingResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.RemoveScheduleEventRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.RemoveScheduleEventResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.RemoveWeeklyEventRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.RemoveWeeklyEventResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SkipRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SkipResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.StopWateringRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.StopWateringResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SwitchScheduleModeRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SwitchScheduleModeResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SyncScheduleEventRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SyncScheduleEventResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SyncScheduleRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SyncScheduleResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.UpdateScheduleEventRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.UpdateScheduleEventResponse;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.UpdateWeeklyEventRequest;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.UpdateWeeklyEventResponse;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.IrrigationControllerModel;
import com.iris.messages.model.dev.IrrigationSchedulableModel;
import com.iris.messages.model.dev.IrrigationZoneModel;
import com.iris.messages.model.subs.LawnNGardenSubsystemModel;
import com.iris.messages.type.IrrigationScheduleStatus;
import com.iris.util.IrisCollections;

@Singleton
@Subsystem(LawnNGardenSubsystemModel.class)
@Version(1)
public class LawnNGardenSubsystem extends BaseSubsystem<LawnNGardenSubsystemModel> {

   private static final String QUERY_IRRIGATION_CONTROLLERS = "base:caps contains '" + IrrigationControllerCapability.NAMESPACE + "'";
   
   public static final String CONTROLLER_STATES_KEY = "controllerStates";

   private final ConcurrentMap<Address,LawnNGardenStateMachine> stateMachines = new ConcurrentHashMap<>();

   private final LawnNGardenConfig config;
   private final Scheduler scheduler;
   
   private final VariableMapManager<LawnNGardenSubsystemModel, IrrigationScheduleState>  controllerStates 
      = new VariableMapManager<LawnNGardenSubsystemModel, IrrigationScheduleState>(CONTROLLER_STATES_KEY, IrrigationScheduleState.class);
         
   @Inject
   public LawnNGardenSubsystem(LawnNGardenConfig config, Scheduler scheduler) {
      this.config = config;
      this.scheduler = scheduler;
   }

   @Override
   protected void onAdded(SubsystemContext<LawnNGardenSubsystemModel> context) {
      super.onAdded(context);
      context.model().setControllers(ImmutableSet.<String>of());
      context.model().setEvenSchedules(ImmutableMap.<String,Map<String,Object>>of());
      context.model().setIntervalSchedules(ImmutableMap.<String,Map<String,Object>>of());
      context.model().setOddSchedules(ImmutableMap.<String,Map<String,Object>>of());
      context.model().setScheduleStatus(ImmutableMap.<String,Map<String,Object>>of());
      context.model().setWeeklySchedules(ImmutableMap.<String,Map<String,Object>>of());
      context.model().setZonesWatering(ImmutableMap.<String,Map<String,Object>>of());
      controllerStates.putInMap(context, context.model().getAddress(),  IrrigationScheduleState.builder().withController(context.model().getAddress()).withCurrentState(STATE.INITIAL).build());
   }

   @Override
   protected void onStarted(SubsystemContext<LawnNGardenSubsystemModel> context) {
      super.onStarted(context);
      controllerStates.init(context);
      syncDevices(context);
      syncWatering(context);
      syncAvailable(context);
   }

   @OnAdded(query = QUERY_IRRIGATION_CONTROLLERS)
   public void onControllerAdded(ModelAddedEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onControllerAdded {}", event.getAddress());
      Model m = context.models().getModelByAddress(event.getAddress());
      ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder().addAll(context.model().getControllers());
      builder.add(m.getAddress().getRepresentation());
      context.model().setControllers(builder.build());
      syncAvailable(context);
      initStateMachine(m.getAddress(), context);
   }

   @OnRemoved(query = QUERY_IRRIGATION_CONTROLLERS)
   public void onControllerRemoved(ModelRemovedEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onControllerRemoved {}", event.getAddress());
      LawnNGardenStateMachine stateMachine = stateMachines.remove(event.getAddress());
      if(stateMachine != null) {
         stateMachine.destroy();
      }
      context.logger().debug("controller {} removed, but no state machine existed", event.getAddress());
      purgeControllers(ImmutableSet.of(event.getAddress().getRepresentation()), context);

      Set<String> controllers = new HashSet<>(context.model().getControllers());
      if(controllers.remove(event.getAddress().getRepresentation())) {
         if(controllers.isEmpty()) {
            context.model().setNextEvent(ImmutableMap.<String,Object>of());
         } else {
            String controller = controllers.iterator().next();
            // will force update of global
            stateMachine(Address.fromString(controller), context).updateNextTransition();
         }
         context.model().setControllers(ImmutableSet.copyOf(controllers));
         syncAvailable(context);
      }
   }

   @OnValueChanged(attributes = { PlaceCapability.ATTR_TZID })
   public void onTimeZoneChange(ModelChangedEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onTimeZoneChanged {}", event);
      SubsystemUtils.refreshTimeZoneOnContext(context, TimeZone.getTimeZone("UTC"));
      Set<String> controllers = context.model().getControllers();
      for(String controller : controllers) {
         stateMachine(Address.fromString(controller), context).onTimezoneUpdate();
      }
   }

   @OnMessage(types = { IrrigationSchedulableCapability.ScheduleEnabledEvent.NAME })
   public void onSchedulingEnabled(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onSchedulingEnabled {}", message);
      stateMachine(message, context).onSchedulingEnabled();
   }

   @OnMessage(types = { IrrigationSchedulableCapability.ScheduleAppliedEvent.NAME })
   public void onScheduleApplied(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onScheduleApplied {}", message);
      stateMachine(message, context).onScheduleApplied(message.getValue());
   }

   @OnMessage(types = { IrrigationSchedulableCapability.ScheduleFailedEvent.NAME })
   public void onScheduleFailed(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().debug("onScheduleFailed {}", message);
      stateMachine(message, context).onScheduleFailed(message.getValue());
   }

   @OnMessage(types = { IrrigationSchedulableCapability.SetIntervalStartSucceededEvent.NAME })
   public void onSetIntervalStartSucceeded(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onSetIntervalStartSucceeded {}", message);
      stateMachine(message, context).onIntervalStartSet(message.getValue());
   }

   @OnMessage(types = { IrrigationSchedulableCapability.SetIntervalStartFailedEvent.NAME })
   public void onSetIntervalStartFailed(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().debug("onSetIntervalStartFailed{}", message);
      stateMachine(message, context).onIntervalStartSetFailed(message.getValue());
   }

   @OnMessage(types = { IrrigationSchedulableCapability.ScheduleClearedEvent.NAME })
   public void onScheduleCleared(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onScheduleCleared {}", message);
      stateMachine(message, context).onScheduleCleared(message.getValue());
   }

   @OnMessage(types = { IrrigationSchedulableCapability.ScheduleClearFailedEvent.NAME })
   public void onScheduleClearFailed(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().debug("onScheduleClearFailed {}", message);
      stateMachine(message, context).onScheduleClearFailed(message.getValue());
   }

   @OnValueChanged(attributes= { DeviceConnectionCapability.ATTR_STATE })
   public void onConnectivityStateChange(ModelChangedEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onConnectivityStateChange {}", event);
      if(context.model().getControllers().contains(event.getAddress().getRepresentation())) {
         LawnNGardenStateMachine stateMachine = stateMachine(event, context);

         Object value = event.getAttributeValue();
         if(DeviceConnectionCapability.STATE_OFFLINE.equals(value)) {
            stateMachine.onDeviceOffline();
         } else if(DeviceConnectionCapability.STATE_ONLINE.equals(value)) {
            stateMachine.onDeviceOnline();
         }
      } else {
         context.logger().trace("ignoring event from non-irrigation device");
      }
   }

   @OnValueChanged(attributes = { IrrigationControllerCapability.ATTR_CONTROLLERSTATE })
   public void onControllerState(ModelChangedEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onControllerState{}", event);
      LawnNGardenValidation.validateController(event.getAddress(), context);
      syncWatering(context);

      Model m = context.models().getModelByAddress(event.getAddress());
      if(IrrigationControllerModel.isControllerStateNOT_WATERING(m)) {
         stateMachine(event.getAddress(), context).onStopWatering();
      }
   }

   @OnValueChanged(attributes = { IrrigationControllerCapability.ATTR_RAINDELAYDURATION })
   public void onRainDelayDuration(ModelChangedEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onRainDelayDuration {}", event);
      LawnNGardenStateMachine stateMachine = stateMachine(event, context);

      Model m = context.models().getModelByAddress(event.getAddress());
      Integer value = IrrigationControllerModel.getRainDelayDuration(m);
      Date start = IrrigationControllerModel.getRainDelayStart(context.models().getModelByAddress(event.getAddress()));
      stateMachine.onRainDelayDuration(value, start);
   }

   @Request(LawnNGardenSubsystemCapability.SwitchScheduleModeRequest.NAME)
   public MessageBody switchScheduleMode(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("switchScheduleMode {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, SwitchScheduleModeRequest.ATTR_CONTROLLER, context);
      ScheduleMode mode = LawnNGardenValidation.getAndValidateMode(body, SwitchScheduleModeRequest.ATTR_MODE);
      stateMachine(controller, context).switchScheduleMode(mode);
      return SwitchScheduleModeResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.EnableSchedulingRequest.NAME)
   public MessageBody enableScheduling(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("enableScheduling {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, EnableSchedulingRequest.ATTR_CONTROLLER, context);
      stateMachine(controller, context).enableSchedule();
      return EnableSchedulingResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.DisableSchedulingRequest.NAME)
   public MessageBody disableScheduling(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("disableScheduling {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, DisableSchedulingRequest.ATTR_CONTROLLER, context);
      stateMachine(controller, context).disableSchedule(-1);
      return DisableSchedulingResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.CreateScheduleEventRequest.NAME)
   public MessageBody createScheduleEvent(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().debug("createScheduleEvent {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, CreateScheduleEventRequest.ATTR_CONTROLLER, context);
      ScheduleMode mode = LawnNGardenValidation.getAndValidateMode(body, CreateScheduleEventRequest.ATTR_MODE, ScheduleMode.WEEKLY);
      TimeOfDay timeOfDay = LawnNGardenValidation.validateTimeOfDay(CreateScheduleEventRequest.getTimeOfDay(body));
      List<ZoneDuration> durations = LawnNGardenTypeUtil.zoneDurations(CreateScheduleEventRequest.getZoneDurations(body));
      stateMachine(controller, context).createSchedule(mode, timeOfDay, durations);
      emitScheduleUpdateEvent(context, controller, mode);
      return CreateScheduleEventResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.CreateWeeklyEventRequest.NAME)
   public MessageBody createWeeklyEvent(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("createWeeklyEvent {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, CreateWeeklyEventRequest.ATTR_CONTROLLER, context);
      TimeOfDay timeOfDay = LawnNGardenValidation.validateTimeOfDay(CreateWeeklyEventRequest.getTimeOfDay(body));
      List<ZoneDuration> durations = LawnNGardenTypeUtil.zoneDurations(CreateWeeklyEventRequest.getZoneDurations(body));
      stateMachine(controller, context).createSchedule(ScheduleMode.WEEKLY, timeOfDay, durations, CreateWeeklyEventRequest.getDays(body));
      emitScheduleUpdateEvent(context, controller, ScheduleMode.WEEKLY);
      return CreateWeeklyEventResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.UpdateScheduleEventRequest.NAME)
   public MessageBody updateScheduleEvent(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("updateScheduleEvent {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, UpdateScheduleEventRequest.ATTR_CONTROLLER, context);
      ScheduleMode mode = LawnNGardenValidation.getAndValidateMode(body, UpdateScheduleEventRequest.ATTR_MODE, ScheduleMode.WEEKLY);
      TimeOfDay timeOfDay = LawnNGardenValidation.validateTimeOfDay(UpdateScheduleEventRequest.getTimeOfDay(body));
      String eventId = LawnNGardenValidation.getAndValidateEventId(body, UpdateScheduleEventRequest.ATTR_EVENTID);
      List<ZoneDuration> durations = LawnNGardenTypeUtil.zoneDurations(UpdateScheduleEventRequest.getZoneDurations(body));
      stateMachine(controller, context).updateSchedule(mode, eventId, timeOfDay, durations);
      emitScheduleUpdateEvent(context, controller, mode);
      return UpdateScheduleEventResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.UpdateWeeklyEventRequest.NAME)
   public MessageBody updateWeeklyEvent(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("updateWeeklyEvent {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, UpdateWeeklyEventRequest.ATTR_CONTROLLER, context);
      TimeOfDay timeOfDay = LawnNGardenValidation.validateTimeOfDay(UpdateWeeklyEventRequest.getTimeOfDay(body));
      String eventId = LawnNGardenValidation.getAndValidateEventId(body, UpdateWeeklyEventRequest.ATTR_EVENTID);
      List<ZoneDuration> durations = LawnNGardenTypeUtil.zoneDurations(UpdateWeeklyEventRequest.getZoneDurations(body));
      Set<String> days = UpdateWeeklyEventRequest.getDays(body);
      String day = (String) body.getAttributes().get(UpdateWeeklyEventRequest.ATTR_DAY);
      stateMachine(controller, context).updateSchedule(ScheduleMode.WEEKLY, eventId, timeOfDay, durations, day, days);
      emitScheduleUpdateEvent(context, controller, ScheduleMode.WEEKLY);
      return UpdateWeeklyEventResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.RemoveScheduleEventRequest.NAME)
   public MessageBody removeScheduleEvent(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("removeScheduleEvent {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, RemoveScheduleEventRequest.ATTR_CONTROLLER, context);
      ScheduleMode mode = LawnNGardenValidation.getAndValidateMode(body, RemoveScheduleEventRequest.ATTR_MODE, ScheduleMode.WEEKLY);
      String eventId = LawnNGardenValidation.getAndValidateEventId(body, RemoveScheduleEventRequest.ATTR_EVENTID);
      stateMachine(controller, context).removeSchedule(mode, eventId);
      emitScheduleUpdateEvent(context, controller, mode);
      return RemoveScheduleEventResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.RemoveWeeklyEventRequest.NAME)
   public MessageBody removeWeeklyEvent(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("removeWeeklyEvent {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, RemoveWeeklyEventRequest.ATTR_CONTROLLER, context);
      String eventId = LawnNGardenValidation.getAndValidateEventId(body, RemoveWeeklyEventRequest.ATTR_EVENTID);
      String day = (String) body.getAttributes().get(RemoveWeeklyEventRequest.ATTR_DAY);
      stateMachine(controller, context).removeSchedule(ScheduleMode.WEEKLY, eventId, day);
      emitScheduleUpdateEvent(context, controller, ScheduleMode.WEEKLY);
      return RemoveWeeklyEventResponse.instance();
   }

   @OnValueChanged(attributes = { IrrigationSchedulableCapability.ATTR_REFRESHSCHEDULE })
   public void onRefreshScheduleChange(ModelChangedEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("onRefreshScheduleChange {}", event);
      if (!Boolean.TRUE.equals(event.getAttributeValue())) {
         context.logger().trace("ignoring sync value change from irrigation device");
         return;
      }

      Address controller = event.getAddress();
      String controllerRepresentation = controller.getRepresentation();
      if(context.model().getControllers().contains(controllerRepresentation)) {
         syncSchedule(controller, controllerRepresentation, context);
      } else {
         context.logger().trace("ignoring event from non-irrigation device");
      }
   }

   @Request(LawnNGardenSubsystemCapability.SyncScheduleRequest.NAME)
   public MessageBody syncSchedule(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("syncSchedule {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, SyncScheduleRequest.ATTR_CONTROLLER, context);
      ScheduleMode mode = LawnNGardenValidation.getAndValidateMode(body, SyncScheduleRequest.ATTR_MODE);
      stateMachine(controller, context).syncSchedule(mode);
      return SyncScheduleResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.SyncScheduleEventRequest.NAME)
   public MessageBody syncScheduleEvent(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("syncScheduleEvent {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, SyncScheduleEventRequest.ATTR_CONTROLLER, context);
      ScheduleMode mode = LawnNGardenValidation.getAndValidateMode(body, SyncScheduleEventRequest.ATTR_MODE);
      String eventId = LawnNGardenValidation.getAndValidateEventId(body, SyncScheduleEventRequest.ATTR_EVENTID);
      stateMachine(controller, context).syncScheduleEvent(mode, eventId);
      return SyncScheduleEventResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.SkipRequest.NAME)
   public MessageBody skip(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("skip {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, SkipRequest.ATTR_CONTROLLER, context);
      Integer duration = LawnNGardenTypeUtil.integer(SkipRequest.getHours(body));
      LawnNGardenValidation.validateRequiredParam(duration, "hours");
      stateMachine(controller, context).skip(duration);
      context.broadcast(
      			LawnNGardenSubsystemCapability.SkipWateringEvent.builder()
      					.withController(controller.getRepresentation())
      					.withHours(duration)
      					.build()
      		);
      return SkipResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.CancelSkipRequest.NAME)
   public MessageBody cancelSkip(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("cancelSkip {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, SkipRequest.ATTR_CONTROLLER, context);
      stateMachine(controller, context).cancelSkip();
      return CancelSkipResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.StopWateringRequest.NAME)
   public MessageBody stopWatering(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("stopWatering {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, StopWateringRequest.ATTR_CONTROLLER, context);
      Boolean currentOnly = LawnNGardenTypeUtil.bool(StopWateringRequest.getCurrentOnly(body), true);
      stateMachine(controller, context).stopWatering(currentOnly);
      return StopWateringResponse.instance();
   }

   @Request(LawnNGardenSubsystemCapability.ConfigureIntervalScheduleRequest.NAME)
   public MessageBody configureIntervalSchedule(PlatformMessage message, SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("configureIntervalSchedule {}", message);
      MessageBody body = message.getValue();
      Address controller = LawnNGardenValidation.getAndValidateController(body, ConfigureIntervalScheduleRequest.ATTR_CONTROLLER, context);
      Date start = LawnNGardenTypeUtil.date(ConfigureIntervalScheduleRequest.getStartTime(body));
      LawnNGardenValidation.validateRequiredParam(start, "startTime");
      Integer days = LawnNGardenTypeUtil.integer(ConfigureIntervalScheduleRequest.getDays(body));
      LawnNGardenValidation.validateDays(days);
      stateMachine(controller, context).configureIntervalSchedule(start, days);
      return ConfigureIntervalScheduleResponse.instance();
   }

   private void emitScheduleUpdateEvent(SubsystemContext<LawnNGardenSubsystemModel> context, Address controller, ScheduleMode mode) {
   	context.broadcast(
   				LawnNGardenSubsystemCapability.UpdateScheduleEvent.builder()
   					.withMode(mode.toString())
   					.build()
   			);
   }

   private void syncDevices(SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("syncing irrigation controllers");
      Set<String> existingControllers = context.model().getControllers();
      ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();

      for (Model model : context.models().getModels()){
         if (model.supports(IrrigationControllerCapability.NAMESPACE)) {
            builder.add(model.getAddress().getRepresentation());
         }
      }

      Set<String> controllers = builder.build();
      context.model().setControllers(controllers);

      purgeControllers(Sets.difference(existingControllers, controllers), context);
      addControllers(controllers, context);
      syncSchedule(controllers, context);
   }

   private void syncAvailable(SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("syncing lawn and garden availability");
      context.model().setAvailable(!context.model().getControllers().isEmpty());
   }

   private void syncWatering(SubsystemContext<LawnNGardenSubsystemModel> context) {
      context.logger().trace("syncing watering zones");
      ImmutableMap.Builder<String,Map<String,Object>> builder = ImmutableMap.builder();
      Set<String> controllers = context.model().getControllers();

      for(String controller : controllers) {
         Model m = context.models().getModelByAddress(Address.fromString(controller));
         for(String zone : m.getInstances().keySet()) {
            if(IrrigationZoneModel.isZoneStateWATERING(zone, m)) {
               builder.put(controller, createWatering(m, zone, context).mapify());
               break;
            }
         }
      }
      Map<String, Map<String,Object>> oldZonesWaterings = context.model().getZonesWatering();
      Map<String, Map<String,Object>> newZonesWaterings = builder.build();
      context.model().setZonesWatering(newZonesWaterings);
      emitZonesWateringEvent(context, newZonesWaterings, oldZonesWaterings);
   }

   private void syncSchedule(Set<String> controllers, SubsystemContext<LawnNGardenSubsystemModel> context) {
      for (String controllerRepresentation : controllers) {
         try {
            Address controller = Address.fromString(controllerRepresentation);
            Model model = context.models().getModelByAddress(controller);
            
            if (Boolean.TRUE.equals(IrrigationSchedulableModel.getRefreshSchedule(model))) {
               syncSchedule(controller, controllerRepresentation, context);
            }
         } catch (Exception ex) {
            context.logger().warn("unexpected error while synchronizing irrigation schedules", ex);
         }
      }
   }

   private void syncSchedule(Address controller, String controllerRepresentation, SubsystemContext<LawnNGardenSubsystemModel> context) {
      String mode = new IrrigationScheduleStatus(context.model().getScheduleStatus().get(controllerRepresentation)).getMode();
      if (mode != null) {
         context.logger().debug("syncing schedule with irrigation device");

         stateMachine(controller, context).syncSchedule(ScheduleMode.valueOf(mode));
         MessageBody message = MessageBody.buildMessage(
            Capability.CMD_SET_ATTRIBUTES,
            ImmutableMap.<String, Object>of(IrrigationSchedulableCapability.ATTR_REFRESHSCHEDULE, false)
         );
      
         context.request(controller, message);
      } else {
         context.logger().trace("not syncing schedule with irrigation device that has no current mode");
      }
   }

   private ZoneWatering createWatering(Model m, String zoneId, SubsystemContext<LawnNGardenSubsystemModel> context) {
      Date start = LawnNGardenTypeUtil.date(IrrigationZoneModel.getWateringStart(zoneId, m));
      Integer duration = IrrigationZoneModel.getWateringDuration(zoneId, m);
      String zoneName = IrrigationZoneModel.getZonename(zoneId, m);
      ZoneWatering.Trigger trigger = Trigger.MANUAL;
      if(duration == null || duration == 0) {
         trigger = Trigger.SCHEDULED;
         Transition transition = stateMachine(m.getAddress(), context).findTransition(zoneId, start);
         duration = transition == null ? null : transition.duration();
      }

      return ZoneWatering.builder()
            .withController(m.getAddress())
            .withZone(zoneId)
            .withZoneName(zoneName)
            .withStartTime(start)
            .withDuration(duration)
            .withTrigger(trigger)
            .build();
   }

   private void emitZonesWateringEvent(SubsystemContext<LawnNGardenSubsystemModel> context,
   		Map<String, Map<String,Object>> newZW,
   		Map<String, Map<String,Object>> oldZW) {
   	Map<String, Map<String, Object>> oldZones = remapToZones(oldZW);
		Map<String, Map<String, Object>> newZones = remapToZones(newZW);

	   // Emit events for starting watering a zone.
		Set<String> startedZones = IrisCollections.addedKeys(newZones, oldZones);
		for (String key : startedZones) {
			context.broadcast(
						LawnNGardenSubsystemCapability.StartWateringEvent.builder()
							.withController(newZones.get(key).get(ATTR_CONTROLLER).toString())
							.withZone(newZones.get(key).get(ATTR_ZONE).toString())
	                  .withZoneName(newZones.get(key).get(ATTR_ZONENAME).toString())
							.withTrigger(newZones.get(key).get(ATTR_TRIGGER).toString())
							.build()
					);
		}

		// Emit events for stopping watering a zone.
		Set<String> stoppedZones = IrisCollections.removedKeys(newZones, oldZones);
		for (String key : stoppedZones) {
			context.broadcast(
						LawnNGardenSubsystemCapability.StopWateringEvent.builder()
							.withController(oldZones.get(key).get(ATTR_CONTROLLER).toString())
							.withZone(oldZones.get(key).get(ATTR_ZONE).toString())
                     .withZoneName(newZones.get(key).get(ATTR_ZONENAME).toString())
							.withTrigger(oldZones.get(key).get(ATTR_TRIGGER).toString())
							.build()
					);
		}
   }

   private void addControllers(Set<String> controllers, SubsystemContext<LawnNGardenSubsystemModel> context) {
      for(String controller : controllers) {
         initStateMachine(Address.fromString(controller), context);
      }
   }

   private void purgeControllers(Set<String> controllers, SubsystemContext<LawnNGardenSubsystemModel> context) {
      if(controllers.isEmpty()) {
         return;
      }

      Map<String,Map<String,Object>> even = new HashMap<>(context.model().getEvenSchedules());
      Map<String,Map<String,Object>> odd = new HashMap<>(context.model().getOddSchedules());
      Map<String,Map<String,Object>> interval = new HashMap<>(context.model().getIntervalSchedules());
      Map<String,Map<String,Object>> weekly = new HashMap<>(context.model().getWeeklySchedules());
      Map<String,Map<String,Object>> status = new HashMap<>(context.model().getScheduleStatus());
      Map<String,Map<String,Object>> watering = new HashMap<>(context.model().getZonesWatering());
     

      for(String controller : controllers) {
         even.remove(controller);
         odd.remove(controller);
         interval.remove(controller);
         weekly.remove(controller);
         status.remove(controller);
         watering.remove(controller);
         controllerStates.removeFromMap(context,controller);
      }

      context.model().setEvenSchedules(ImmutableMap.copyOf(even));
      context.model().setOddSchedules(ImmutableMap.copyOf(odd));
      context.model().setIntervalSchedules(ImmutableMap.copyOf(interval));
      context.model().setWeeklySchedules(ImmutableMap.copyOf(weekly));
      context.model().setScheduleStatus(ImmutableMap.copyOf(status));
      context.model().setZonesWatering(ImmutableMap.copyOf(watering));
   }

   private void initStateMachine(Address controller, SubsystemContext<LawnNGardenSubsystemModel> context) {
      LawnNGardenStateMachine stateMachine = new LawnNGardenStateMachine(controller, context, config, scheduler, controllerStates);
      stateMachines.put(controller, stateMachine);
      stateMachine.init();
   }

   private LawnNGardenStateMachine stateMachine(ModelEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      Address controller = LawnNGardenValidation.getAndValidateController(event, context);
      return stateMachine(controller, context);
   }

   private LawnNGardenStateMachine stateMachine(PlatformMessage msg, SubsystemContext<LawnNGardenSubsystemModel> context) {
      Address addr = LawnNGardenValidation.getAndValidateController(msg, context);
      return stateMachine(addr, context);
   }

   LawnNGardenStateMachine stateMachine(Address controller, SubsystemContext<LawnNGardenSubsystemModel> context) {
      // get the state machine for the controller
      LawnNGardenStateMachine stateMachine = stateMachines.get(controller);
      if(stateMachine == null) {
         context.logger().warn("received create schedule message for an existing controller, but no state machine exists");
         throw new ErrorEventException(Errors.CODE_GENERIC, "server error:  no state machine found for " + controller.getRepresentation());
      }
      return stateMachine;
   }

   private static Map<String, Map<String,Object>> remapToZones(Map<String, Map<String, Object>> controllers) {
		if (controllers == null) {
			return null;
		}
		Map<String, Map<String,Object>> remap = new HashMap<>(controllers.size());
		for (Map<String, Object> item : controllers.values()) {
			remap.put(makeZone(item), item);
		}
		return remap;
	}

	private static String makeZone(Map<String, Object> controller) {
		return controller.get(ATTR_CONTROLLER) + ":" + controller.get(ATTR_ZONE);
	}
	
	
}

