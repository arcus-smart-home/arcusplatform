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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.scheduler.Scheduler;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.lawnngarden.model.ZoneDuration;
import com.iris.common.subsystem.lawnngarden.model.ZoneWatering;
import com.iris.common.subsystem.lawnngarden.model.ZoneWatering.Trigger;
import com.iris.common.subsystem.lawnngarden.model.operations.OperationSequence;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleStatus;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition;
import com.iris.common.subsystem.lawnngarden.model.schedules.even.EvenSchedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.interval.IntervalSchedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.odd.OddSchedule;
import com.iris.common.subsystem.lawnngarden.model.schedules.weekly.WeeklySchedule;
import com.iris.common.subsystem.lawnngarden.model.state.IrrigationScheduleState;
import com.iris.common.subsystem.lawnngarden.model.state.IrrigationScheduleState.STATE;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenValidation;
import com.iris.common.subsystem.util.VariableMapManager;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.IrrigationControllerCapability;
import com.iris.messages.capability.IrrigationSchedulableCapability;
import com.iris.messages.capability.LawnNGardenSubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.IrrigationControllerModel;
import com.iris.messages.model.subs.LawnNGardenSubsystemModel;


public class LawnNGardenStateMachine {

   private static final int NEXTFIRE_BUFFER = 5000;
   private static final int UNSET_RAINDELAY = -2;

   private final Address controller;
   private final SubsystemContext<LawnNGardenSubsystemModel> context;
   private final LawnNGardenConfig config;
   private final Scheduler scheduler;
   private final Map<ScheduleMode, Schedule<?, ?>> schedules = new HashMap<>();
   private final Queue<OperationSequence> pendingOperations = new LinkedList<>();
   private final List<ScheduledTask> retryTasks = new LinkedList<>();
   private final VariableMapManager<LawnNGardenSubsystemModel, IrrigationScheduleState>  controllerStates;

   private final Map<String, State> states = ImmutableMap.<String, State>builder()
   .put(STATE.APPLIED.name(), new Applied())
   .put(STATE.UPDATING.name(), new Updating())
   .put(STATE.INITIAL.name(), new Initial())
   .put(STATE.PAUSED.name(), new Paused())
   .build();

   private final Object lock = new Object();

   private volatile ScheduledTask timeoutTask = null;
   private volatile ScheduledTask nextTransitionTask = null;
   private volatile PendingOperation currentOp = null;
   private volatile ScheduleStatus currentStatus = null;
   private volatile ScheduleMode previousMode = null;
   private volatile State state;
   private volatile int expectedRainDelay = UNSET_RAINDELAY;

   public LawnNGardenStateMachine(Address controller, SubsystemContext<LawnNGardenSubsystemModel> context, LawnNGardenConfig config, Scheduler scheduler, VariableMapManager<LawnNGardenSubsystemModel, IrrigationScheduleState>  controllerStates) {
      this.controller = controller;
      this.context = context;
      this.config = config;
      this.scheduler = scheduler;
      this.controllerStates = controllerStates;
   }

   // for testing only
   List<PendingOperation> pendingOperations() {
      List<PendingOperation> operations = new ArrayList<>();
      for (OperationSequence sequence : pendingOperations) {
         operations.addAll(sequence.operations());
      }
      return operations;
   }

   public void init() {
      context.logger().debug("initializing state machine for {}", controller.getRepresentation());
      Map<String, IrrigationScheduleState> controllerStateMap = controllerStates.getValues(context);
      IrrigationScheduleState currentState = LawnNGardenTypeUtil.INSTANCE.coerce(IrrigationScheduleState.class, controllerStateMap.get(controller.getRepresentation()));
      this.state = getStateForContext(Optional.fromNullable(currentState));  
      
      syncSchedule(EvenSchedule.builder());
      syncSchedule(OddSchedule.builder());
      syncSchedule(WeeklySchedule.builder());
      syncSchedule(IntervalSchedule.builder());

      Map<Address, ScheduleStatus> statuses = LawnNGardenTypeUtil.scheduleStatus(context.model().getScheduleStatus());
      currentStatus = statuses.get(controller);
      
      transition(state.onEnter());
   }
   
   private void syncSchedule(Schedule.Builder<?, ?, ?> builder) {
      updateSchedule(reconstructOrCreateSchedule(builder));
   }
   
   private State getStateForContext(Optional<IrrigationScheduleState> contextModelState){
      if (contextModelState.isPresent()){
         return states.get(contextModelState.get().currentState().name());
      }
      
      // If there is no current state then default to the initial state
      return states.get(STATE.INITIAL.name());
   }

   public State transition(State next) {
      if (state.equals(next)) { updateCurrentState(state); return state; }

      try { state.onExit(); }
      catch (Exception e) { context.logger().warn("Error exiting state [{}]", state, e); }

      state = next;

      try { 
         transition(next.onEnter());
      	if ( next instanceof Applied) {
      		context.broadcast(LawnNGardenSubsystemCapability.ApplyScheduleToDeviceEvent.builder()
         			.withController(controller.getRepresentation())
         			.build()
         		);
      	}
      }
      catch (Exception e) { 
      	context.logger().warn("Error entering state [{}]", next, e);
      	context.broadcast(LawnNGardenSubsystemCapability.ApplyScheduleToDeviceFailedEvent.builder()
      			.withController(controller.getRepresentation())
      			.build()
      		);
      }
      
      updateCurrentState(state);
      return state;
   }

   public void destroy() {
      context.logger().debug("destroying state machine for {}", controller.getRepresentation());
      if(timeoutTask != null) { timeoutTask.cancel(); }
      cleanupOperations();
      for (ScheduleMode mode : ScheduleMode.values()) { clearSchedule(mode); }
      clearStatus();
   }

   public Transition findTransition(String zoneId, Date time) {
      Calendar cal = localTime();
      cal.setTime(time);
      Schedule<?, ?> current = currentSchedule();
      return current.findTransition(zoneId, cal);
   }

   public void createSchedule(ScheduleMode mode, TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
      context.logger().debug("createSchedule on {}: {}, {}, {}, {}", controller.getRepresentation(), mode, timeOfDay, durations, args);
      transition(state.onCreateSchedule(mode, timeOfDay, durations, args));
   }

   public void removeSchedule(ScheduleMode mode, String eventId, Object... args) {
      context.logger().debug("removeSchedule from {}: {}, {}, {}", controller.getRepresentation(), mode, eventId, args);
      transition(state.onRemoveSchedule(mode, eventId, args));
   }

   public void updateSchedule(ScheduleMode mode, String eventId, TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
      context.logger().debug("updateSchedule on {}: {}, {}, {}, {}, {}", controller.getRepresentation(), mode, eventId, timeOfDay, durations, args);
      transition(state.onUpdateSchedule(mode, eventId, timeOfDay, durations, args));
   }

   public void syncSchedule(ScheduleMode mode) {
      context.logger().debug("syncSchedule to {}: {}", controller.getRepresentation(), mode);
      transition(state.onSyncSchedule(mode));
   }

   public void syncScheduleEvent(ScheduleMode mode, String eventId) {
      context.logger().debug("syncScheduleEvent to {}: {}, {}", controller.getRepresentation(), mode, eventId);
      transition(state.onSyncScheduleEvent(mode, eventId));
   }

   public void switchScheduleMode(ScheduleMode mode) {
      context.logger().debug("switchScheduleMode on {}: {}", mode);
      transition(state.onSwitchScheduleType(mode));
   }

   public void enableSchedule() {
      if (!currentStatus.enabled()) {
         context.logger().debug("enableSchedule on {}: {}", controller.getRepresentation(), currentStatus.mode());
         if (!currentSchedule().hasEvents()) {
            throw new ErrorEventException(LawnNGardenValidation.CODE_NOEVENTS, "at least one event must be present to enable a schedule");
         }
         sendEnable();
      } else {
         context.logger().debug("enableSchedule on {}: ignored because scheduling is already enabled", controller.getRepresentation());
      }
   }

   public void onSchedulingEnabled() {
      context.logger().debug("onSchedulingEnabled for {}", controller.getRepresentation());
      Schedule<?, ?> schedule = currentSchedule();

      if (schedule.hasEvents()) {
         updateStatus(ScheduleStatus.builder(currentStatus)
               .withEnabled(true)
               .withSkippedUntil(null)
               .build());
         updateNextTransition();
      }
   }

   public void onRainDelayDuration(Integer duration, Date rainDelayStart) {
      ScheduleStatus curStatus = currentStatus;

      context.logger().debug("onRainDelayDuration curStatus = {}, duration = {}, rainDelayStart = {}", curStatus, duration, rainDelayStart);

      if (duration != null && expectedRainDelay != UNSET_RAINDELAY && duration == expectedRainDelay) {
         if (duration != 0) {
            onSchedulingDisabled(duration, rainDelayStart);
         }
         expectedRainDelay = UNSET_RAINDELAY;
      } else {
         context.logger().debug("ignoring change in rain delay, because it did not meet expected value");
      }
   }

   public void onStopWatering() {
      ScheduleStatus curStatus = currentStatus;
      if(curStatus.skippedUntil() != null) {
         Date now = new Date();
         long diff = currentStatus.skippedUntil().getTime() - now.getTime();
         int mins = Math.round(diff / 1000.0f / 60.0f);
         if(mins <= 0) {
            updateStatus(ScheduleStatus.builder(currentStatus).withSkippedUntil(null).build());
         } else {
            sendDisable(mins);
         }
      } else if(!curStatus.enabled()) {
         sendDisable(-1);
      }
   }

   public void disableSchedule(int duration) {
      Model m = context.models().getModelByAddress(controller);
      if(m != null && IrrigationControllerModel.getRainDelayDuration(m, UNSET_RAINDELAY) == -1) {
         onSchedulingDisabled(-1, null);
         return;
      }
      if (currentStatus.enabled()) {
         context.logger().debug("disableSchedule on {}: {}", controller.getRepresentation(), currentStatus.mode());
         sendDisable(duration);
      } else {
         context.logger().debug("disableSchedule on {}: ignored because scheduling is already disabled", controller.getRepresentation());
      }
   }

   public void onSchedulingDisabled(int duration, Date rainDelayStart) {
      context.logger().debug("onSchedulingDisabled for {}: duration = ", controller.getRepresentation(), duration);
      ScheduleStatus.Builder builder = ScheduleStatus.builder(currentStatus);
      if (duration > 0) {
         Calendar current = localTime();
         current.setTime(rainDelayStart);
         current.add(Calendar.MINUTE, duration);
         builder.withSkippedUntil(current.getTime());
      } else {
         builder.withSkippedUntil(null);
         builder.withEnabled(false);
      }
      updateStatus(builder.build());
      updateNextTransition();
   }

   public void skip(int hours) {
      context.logger().debug("skip on {} for {} hours", controller.getRepresentation(), hours);
      sendDisable((int) TimeUnit.MINUTES.convert(hours, TimeUnit.HOURS));
   }

   public void cancelSkip() {
      context.logger().debug("cancelSkip for {}", controller.getRepresentation());
      if (currentStatus.enabled()) {
         sendEnable();
      } else {
         sendDisable(-1);
      }
   }

   public void stopWatering(boolean currentOnly) {
      context.logger().debug("stopWatering on {}:  currentOnly = {}", controller.getRepresentation(), currentOnly);
      Map<String, Map<String, Object>> zonesWatering = context.model().getZonesWatering();
      Map<String, Object> activeZone = zonesWatering.get(controller.getRepresentation());
      ZoneWatering watering = LawnNGardenTypeUtil.INSTANCE.coerce(ZoneWatering.class, activeZone);
      if (watering == null) { return; }
      int minRemaining = currentSchedule().minutesRemaining(context.getLocalTime());
      if (watering.trigger() == Trigger.SCHEDULED && !currentOnly && minRemaining > 0) {
         context.logger().debug("stopWatering on {}: found {} minutes remaining", controller.getRepresentation(), minRemaining);
         sendDisable(minRemaining);
      } else {
         sendStop(watering.zone());
      }
   }

   public void configureIntervalSchedule(Date startDate, int days) {
      context.logger().debug("configureIntervalSchedule for {}: {}, {}", controller.getRepresentation(), startDate, days);
      IntervalSchedule schedule = (IntervalSchedule) scheduleFor(ScheduleMode.INTERVAL);
      updateSchedule(IntervalSchedule.builder(schedule).withDays(days).withStartDate(startDate).build());
      if (currentStatus.mode() == ScheduleMode.INTERVAL) {
         transition(state.onSyncSchedule(ScheduleMode.INTERVAL));
      }
   }

   public void onTimezoneUpdate() {
      context.logger().debug("onTimezoneUpdate for {}", controller.getRepresentation());
      updateNextTransition();
   }

   public void onScheduleCleared(MessageBody body) {
      context.logger().debug("onScheduleCleared on {}: {}", controller.getRepresentation(), body);
      transition(state.onScheduleCleared(body));
   }

   public void onScheduleClearFailed(MessageBody body) {
      context.logger().debug("onScheduleClearFailed for {}: {}", controller.getRepresentation(), body);
      transition(state.onScheduleClearFailed(body));
   }

   public void onScheduleApplied(MessageBody body) {
      context.logger().debug("onScheduleApplied on {}: {}", controller.getRepresentation(), body);
      transition(state.onScheduleApplied(body));
   }

   public void onScheduleFailed(MessageBody body) {
      context.logger().debug("onScheduleFailed for {}: {}", controller.getRepresentation(), body);
      transition(state.onScheduleFailed(body));
   }

   public void onIntervalStartSet(MessageBody body) {
      context.logger().debug("onIntervalStartSet for {}: {}", controller.getRepresentation(), body);
      transition(state.onIntervalStartSet(body));
   }

   public void onIntervalStartSetFailed(MessageBody body) {
      context.logger().debug("onIntervalStartSetFailed for {}: {}", controller.getRepresentation(), body);
      transition(state.onIntervalStartSetFailed(body));
   }

   public void onOperationTimeout(PendingOperation operation) {
      context.logger().debug("onOperationTimeout for {}: {}, {}", controller.getRepresentation(), operation.message(), operation.attributes());
      transition(state.onTimeout(operation));
   }

   public void onOperationRetry(OperationSequence sequence) {
      context.logger().debug("onOperationRetry for {}", controller.getRepresentation());
      transition(state.onOperationRetry(sequence));
   }

   public void onDeviceOffline() {
      context.logger().debug("onDeviceOffline {}", controller.getRepresentation());
      transition(state.onDeviceOffline());
   }

   public void onDeviceOnline() {
      context.logger().debug("onDeviceOnline {}", controller.getRepresentation());
      transition(state.onDeviceOnline());
   }

   private Calendar localTime() {
      return (Calendar) context.getLocalTime().clone();
   }

   private Schedule<?, ?> currentSchedule() {
      return schedules.get(currentStatus.mode());
   }

   private void clearSchedule(ScheduleMode mode) {
      Map<String, Object> contextSchedules = new HashMap<>(contextSchedules(mode));
      contextSchedules.remove(controller.getRepresentation());
      context.model().setAttribute(mode.getModelAttribute(), ImmutableMap.copyOf(contextSchedules));
      schedules.remove(mode);
   }

   private void clearStatus() {
      Map<String, Map<String, Object>> statuses = new HashMap<>(context.model().getScheduleStatus());
      statuses.remove(controller.getRepresentation());
      context.model().setScheduleStatus(ImmutableMap.copyOf(statuses));
      this.currentStatus = null;
   }

   private void updateStatus(ScheduleStatus status) {
      Map<String, Map<String, Object>> statuses = new HashMap<>(context.model().getScheduleStatus());
      statuses.put(controller.getRepresentation(), status.mapify());
      context.model().setScheduleStatus(ImmutableMap.copyOf(statuses));
      this.currentStatus = status;
   }

   private void updateSchedule(Schedule<?, ?> schedule) {
      Map<String, Object> contextSchedules = new HashMap<>(contextSchedules(schedule.mode()));
      contextSchedules.put(controller.getRepresentation(), schedule.mapify());
      context.model().setAttribute(schedule.mode().getModelAttribute(), ImmutableMap.copyOf(contextSchedules));
      schedules.put(schedule.mode(), schedule);
   }
   
   private void updateCurrentState(State state) {
      IrrigationScheduleState irrigationScheduleState = IrrigationScheduleState.builder().withController(controller).withCurrentState(STATE.valueOf(state.name())).build();
      controllerStates.putInMap(context, controller, irrigationScheduleState);
   }

   @SuppressWarnings("unchecked")
   private Map<String, Object> contextSchedules(ScheduleMode mode) {
      return (Map<String, Object>) context.model().getAttribute(mode.getModelAttribute());
   }

   @SuppressWarnings("unchecked")
   private Schedule<?, ?> reconstructOrCreateSchedule(Schedule.Builder<?, ?, ?> builder) {
      Map<String, Object> contextSchedules = contextSchedules(builder.getType());
      if (contextSchedules != null) {
         Map<String, Object> contextSchedule = (Map<String, Object>) contextSchedules.get(controller.getRepresentation());
         Schedule<?, ?> schedule = LawnNGardenTypeUtil.schedule(contextSchedule);
         if (schedule == null) {
            context.logger().debug("creating new schedule for {}: {}", controller.getRepresentation(), builder.getType());
            schedule = builder.withController(controller).withStatus(Status.APPLIED).build();
         }

         return schedule;
      }

      throw new IllegalStateException("no schedule map found for " + builder.getType() + "...should exist at subsystem add time");
   }

   public void updateNextTransition() {
      updateNextTransition(currentStatus.mode());
   }

   @SuppressWarnings("deprecation")
   private void updateNextTransition(ScheduleMode mode) {
      if (mode != currentStatus.mode()) {
         context.logger().debug("ignoring request to update next transition time for {}, current mode is {}", mode, currentStatus.mode());
         return;
      }

      if (!currentStatus.enabled() || !currentSchedule().hasEvents()) {
         context.logger().debug("scheduling disabled, setting next fire to null");
         handleNoNextTransition();
         return;
      }

      Calendar cal = localTime();
      context.logger().debug("finding next fire time from {}", cal.getTime());
      if (currentStatus.skippedUntil() != null) {
         cal.setTime(currentStatus.skippedUntil());
         context.logger().debug("sheduling skipped, readjusting next fire time start to {}", cal.getTime());
      }

      Transition transition = currentSchedule().nextTransition(cal);

      if (transition == null) {
         context.logger().debug("no next event found, setting next fire to null");
         handleNoNextTransition();
         return;
      }

      if (!ObjectUtils.equals(transition, currentStatus.nextTransition())) {
         cancelNextTransitionTask();
         if (transition.controller() == null) {
            transition = Transition.builder(transition).withController(controller).build();
         }
         updateStatus(ScheduleStatus.builder(currentStatus).withNextTransition(transition).build());
         updateGlobalNextTransition();

         nextTransitionTask = scheduler.scheduleAt(new Runnable() {
            @Override
            public void run() { LawnNGardenStateMachine.this.updateNextTransition(); }
         }, new Date(transition.startTime().getTime() + NEXTFIRE_BUFFER));
      }
   }

   private void handleNoNextTransition() {
      cancelNextTransitionTask();
      updateStatus(ScheduleStatus.builder(currentStatus).withNextTransition(null).build());
      updateGlobalNextTransition();
   }

   private void updateGlobalNextTransition() {
      Map<Address, ScheduleStatus> statuses = LawnNGardenTypeUtil.scheduleStatus(context.model().getScheduleStatus());
      Transition earliestTransition = null;
      for (Map.Entry<Address, ScheduleStatus> entry : statuses.entrySet()) {
         if (entry.getValue().nextTransition() == null) {
            continue;
         }
         if (earliestTransition == null || entry.getValue().nextTransition().startTime().before(earliestTransition.startTime())) {
            Transition transition = entry.getValue().nextTransition();
            if (transition.controller() == null) {
               transition = Transition.builder(transition).withController(entry.getKey()).build();
            }
            earliestTransition = transition;
         }
      }
      context.logger().debug("new global next event starts at {}", earliestTransition == null ? null : earliestTransition.startTime());
      context.model().setNextEvent(earliestTransition == null ? ImmutableMap.<String, Object> of() : earliestTransition.mapify());
   }

   private void cancelNextTransitionTask() {
      if (nextTransitionTask != null) { nextTransitionTask.cancel(); }
      nextTransitionTask = null;
   }

   private void sendEnable() {
      expectedRainDelay = 0;
      context.request(controller, IrrigationSchedulableCapability.EnableScheduleRequest.instance());
   }

   private void sendDisable(int duration) {
      expectedRainDelay = duration;
      context.request(controller,
            IrrigationSchedulableCapability.DisableScheduleRequest.builder().withDuration(duration).build());
   }

   private void sendStop(String zone) {
      context.request(controller,
            IrrigationControllerCapability.CancelV2Request.builder().withZone(zone).build());
   }

   private void updateOnFailure() {
      // synchronized(lock) already around method bodies that invoke this
      if (currentOp.retryCount() >= config.retryCount()) {
         context.logger().debug("operation {} retry attempts exceeded {}, marking failed", currentOp.message(), config.retryCount());
         currentOp.setState(PendingOperation.State.FAILED);
      } else {
         currentOp.setState(PendingOperation.State.RETRYING);
         currentOp.incRetry();
         context.logger().debug("operation {} retrying for the {} time", currentOp.message(), currentOp.retryCount());
      }
   }

   private void cleanupOperations() {
      synchronized (lock) {
         List<ScheduledTask> retries = retryTasks;
         for (ScheduledTask task : retries) { task.cancel(); }
         retryTasks.clear();
         pendingOperations.clear();
         currentOp = null;
      }
   }

   private Schedule<?, ?> scheduleFor(ScheduleMode mode) {
      return schedules.get(mode);
   }

   private void applyOperation(final PendingOperation operation) {
      // synchronized(lock) already around body of method that invokes this
      operation.setState(PendingOperation.State.INPROGRESS);
      currentOp = operation;
      context.logger().debug("sending requst {} - {}", operation.message(), operation.attributes());
      context.request(controller, MessageBody.buildMessage(operation.message(), operation.attributes()));
      if (timeoutTask != null) { timeoutTask.cancel(); }
      timeoutTask = scheduler.scheduleDelayed(new Runnable() {
         @Override
         public void run() { LawnNGardenStateMachine.this.onOperationTimeout(operation); }
      }, config.timeoutSeconds(), TimeUnit.SECONDS);
   }

   private void updateState(PendingOperation operation) {
      updateSchedule(scheduleFor(operation.mode()).updateTransitionState(operation));
   }

   private Set<String> getAllZones() {
      Model m = context.models().getModelByAddress(controller);
      Map<String, Set<String>> instances = m.getInstances();
      Set<String> zones = new HashSet<>();
      for (Map.Entry<String, Set<String>> entry : instances.entrySet()) {
         if (entry.getValue().contains("irr")) {
            zones.add(entry.getKey());
         }
      }
      return zones;
   }

   // ///////////////////////////////////////////////////////////////////////////////////////////////
   // states
   // ///////////////////////////////////////////////////////////////////////////////////////////////

   public abstract class State {

      public abstract State onEnter();

      public abstract String name();
      
      public State onCreateSchedule(ScheduleMode mode, TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
         Schedule<?, ?> newSchedule = scheduleFor(mode).addEvent(timeOfDay, durations, args);
         Integer maxDaily = IrrigationControllerModel.getMaxdailytransitions(context.models().getModelByAddress(controller));
         newSchedule.validate(maxDaily == null ? config.defaultMaxTransitions() : maxDaily);
         return doSyncSchedule(newSchedule);
      }

      public State onRemoveSchedule(ScheduleMode mode, String eventId, Object... args) {
         return doSyncSchedule(scheduleFor(mode).removeEvent(eventId, args));
      }

      public State onUpdateSchedule(ScheduleMode mode, String eventId, TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
         Schedule<?, ?> newSchedule = scheduleFor(mode).updateEvent(eventId, timeOfDay, durations, args);
         newSchedule.validate(IrrigationControllerModel.getMaxdailytransitions(context.models().getModelByAddress(controller)));
         return doSyncSchedule(newSchedule);
      }

      public State onSyncSchedule() {
         return onSyncSchedule(currentStatus.mode());
      }

      public State onSyncSchedule(ScheduleMode mode) {
         return doSyncSchedule(scheduleFor(mode));
      }

      private State doSyncSchedule(Schedule<?, ?> schedule) {
         synchronized (lock) {
            beforeSync();
            schedule = schedule.markPending();
            updateSchedule(schedule);

            if (currentStatus.mode() == schedule.mode()) {
               updateNextTransition(schedule.mode());
               List<OperationSequence> ops = schedule.generateSyncOperations(getAllZones());
               context.logger().debug("synchronization schedule {} using {} operations", schedule.mode(), ops.size());
               pendingOperations.addAll(ops);
               return states.get(STATE.UPDATING.name());
            } else {
               context.logger().debug("skipping synching schedule {} to device, {} is the current mode", schedule.mode(), currentStatus.mode());
            }
            return this;
         }
      }

      public State onSyncScheduleEvent(ScheduleMode mode, String eventId) {
         synchronized (lock) {
            Schedule<?, ?> schedule = scheduleFor(mode).markEventPending(eventId);
            updateSchedule(schedule);
            if (currentStatus.mode() == mode) {
               List<OperationSequence> ops = schedule.generateSyncOperations(getAllZones());
               context.logger().debug("synchronization schedule {} using {} operations", schedule.mode(), ops.size());
               pendingOperations.addAll(ops);
               return states.get(STATE.UPDATING.name());
            } else {
               context.logger().debug("skipping synching schedule {} to device, {} is the current mode", schedule.mode(), currentStatus.mode());
            }
            return this;
         }
      }

      public State onSwitchScheduleType(ScheduleMode mode) {
         synchronized (lock) {
            if (mode == currentStatus.mode()) {
               context.logger().debug("ignoring request to switch to {}, scheduling mode is already {}", mode, currentStatus.mode());
               return this;
            }

            previousMode = currentStatus.mode();

            ScheduleStatus curStatus = currentStatus;
            ScheduleStatus.Builder newStatus = ScheduleStatus.builder(curStatus).withMode(mode);

            Schedule<?, ?> current = currentSchedule();
            Schedule<?, ?> newSchedule = scheduleFor(mode);

            beforeSync();

            context.logger().debug("adding clear operations for {} to the pending operation list", current.mode());
            pendingOperations.addAll(current.clear(getAllZones()));

            if (newSchedule.hasEvents()) {
               context.logger().debug("new schedule mode {} has events, adding sync operations to the pending operation list", newSchedule.mode());
               pendingOperations.addAll(newSchedule.generateSyncOperations(getAllZones()));
            } else {
               context.logger().debug("new schedule mode {} has no events, marking disabled", newSchedule.mode());
               pendingOperations.addAll(newSchedule.clear(getAllZones()));
               newStatus.withEnabled(false);
            }

            updateStatus(newStatus.build());
            updateNextTransition(mode);

            context.logger().debug("{} operations required to switch from {} to {}", pendingOperations.size(), current.mode(), newSchedule.mode());

            if (pendingOperations.isEmpty()) {
               return states.get(STATE.APPLIED.name());
            }

            return states.get(STATE.UPDATING.name());
         }
      }

      public void onExit() {
      }
      
      public State onScheduleCleared(MessageBody body) { return this; }
      public State onScheduleClearFailed(MessageBody body) { return this; }
      public State onScheduleApplied(MessageBody body) { return this; }
      public State onScheduleFailed(MessageBody body) { return this; }
      public State onTimeout(PendingOperation operation) { return this; }
      public State onOperationRetry(OperationSequence operation) { return this; }
      public State onDeviceOffline() { return this; }
      public State onIntervalStartSet(MessageBody body) { return this; }
      public State onIntervalStartSetFailed(MessageBody body) { return this; }

      public State onDeviceOnline() {
         synchronized (lock) {
            return pendingOperations.isEmpty() && retryTasks.isEmpty()  ? states.get(STATE.APPLIED.name()) : states.get(STATE.UPDATING.name());
         }
      }

      protected void beforeSync() {}
   }

   private class Initial extends State {

      @Override
      public State onEnter() {
         if (currentStatus == null) {
            updateStatus(ScheduleStatus.builder()
                  .withController(controller)
                  .withEnabled(false)
                  .withMode(ScheduleMode.WEEKLY)
                  .build());
            previousMode = ScheduleMode.WEEKLY;
            updateNextTransition();
            return states.get(STATE.APPLIED.name());
         } else {
            if (currentStatus.skippedUntil() != null && currentStatus.skippedUntil().before(localTime().getTime())) {
               currentStatus = ScheduleStatus.builder(currentStatus).withSkippedUntil(null).build();
               updateStatus(currentStatus);
            }
            previousMode = currentSchedule().mode();
            updateNextTransition();
            if (currentSchedule().status() == Status.UPDATING) {
               return states.get(STATE.UPDATING.name());
            }
         }

         return states.get(STATE.APPLIED.name());
      }

      @Override
      public State onCreateSchedule(ScheduleMode mode, TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
         context.logger().warn("attempt to create an event while the state machine is initializing");
         return this;
      }

      @Override
      public State onRemoveSchedule(ScheduleMode mode, String eventId, Object... args) {
         context.logger().warn("attempt to remove an event while the state machine is initializing");
         return this;
      }

      @Override
      public State onUpdateSchedule(ScheduleMode mode, String eventId, TimeOfDay timeOfDay, List<ZoneDuration> durations, Object... args) {
         context.logger().warn("attempt to update an event while the state machine is initializing");
         return this;
      }

      @Override
      public State onSyncSchedule(ScheduleMode mode) {
         context.logger().warn("attempt to sync a schedule while the state machine is initializing");
         return this;
      }

      @Override
      public State onSyncScheduleEvent(ScheduleMode mode, String eventId) {
         context.logger().warn("attempt to sync an event while the state machine is initializing");
         return this;
      }

      @Override
      public State onSwitchScheduleType(ScheduleMode mode) {
         context.logger().warn("attempt to switch the schedule mode while the state machine is initializing");
         return this;
      }
      
      @Override
      public String name() {
         return STATE.INITIAL.name();
      }      
   }

   private class Applied extends State {

      @Override
      public State onEnter() {
         context.logger().debug("entering applied");
         boolean disableSent = false;
         Schedule<?, ?> schedule = scheduleFor(currentStatus.mode());
         if (!schedule.hasEvents()) {
            sendDisable(-1);
            disableSent = true;
         }

         // clear rain delay on switch scheduling
         if (!disableSent && previousMode != currentStatus.mode() && currentStatus.skippedUntil() != null) {
            context.logger().debug("clearing rain delay because {} != {} and skipped until = {}", previousMode, currentStatus.mode(), currentStatus.skippedUntil());
            sendEnable();
         }
         previousMode = currentStatus.mode();

         return this;
      }
      
      @Override
      public String name() {
         return STATE.APPLIED.name();
      }       
   }

   private class Updating extends State {

      @Override
      public State onEnter() {
         context.logger().debug("entering updating, will attempt to execute {} operations", pendingOperations.size());
         synchronized (lock) {
            return updateOperationQueue();
         }
      }

      @Override public void onExit() { cleanupOperations(); }
      @Override protected void beforeSync() { cleanupOperations(); }

      @Override
      public State onScheduleCleared(MessageBody body) {
         synchronized (lock) {
            if (currentOp != null && currentOp.eventMatches(body)) {
               currentOp.setState(PendingOperation.State.SUCCESSFUL);
            }
            return updateOperationQueue();
         }
      }

      @Override
      public State onScheduleClearFailed(MessageBody body) {
         synchronized (lock) {
            if (currentOp != null && currentOp.eventMatches(body)) {
               updateOnFailure();
            }
            return updateOperationQueue();
         }
      }

      @Override
      public State onScheduleApplied(MessageBody body) {
         synchronized (lock) {
            if (currentOp != null && currentOp.eventMatches(body)) {
               currentOp.setState(PendingOperation.State.SUCCESSFUL);
            }
            return updateOperationQueue();
         }
      }

      @Override
      public State onScheduleFailed(MessageBody body) {
         synchronized (lock) {
            if (currentOp != null && currentOp.eventMatches(body)) {
               updateOnFailure();
            }
            return updateOperationQueue();
         }
      }

      @Override
      public State onIntervalStartSet(MessageBody body) {
         synchronized (lock) {
            if (currentOp != null && currentOp.eventMatches(body)) {
               currentOp.setState(PendingOperation.State.SUCCESSFUL);
            }
            return updateOperationQueue();
         }
      }

      @Override
      public State onIntervalStartSetFailed(MessageBody body) {
         synchronized (lock) {
            if (currentOp != null && currentOp.eventMatches(body)) {
               updateOnFailure();
            }
            return updateOperationQueue();
         }
      }

      @Override
      public State onTimeout(PendingOperation operation) {
         synchronized (lock) {
            if (currentOp != null && operation.opId().equals(currentOp.opId())) {
               updateOnFailure();
            }
            return updateOperationQueue();
         }
      }

      @Override
      public State onOperationRetry(OperationSequence operation) {
         synchronized (lock) {
            pendingOperations.add(operation);
            return updateOperationQueue();
         }
      }

      private State updateOperationQueue() {
         // synchronized(lock) in body methods that invoke this
         PendingOperation op = currentOp;

         if (op != null) {
            context.logger().debug("current op type = {}, attrs = {}, state = {}", op.message(), op.attributes(), op.state());
            switch (op.state()) {
            case INPROGRESS: return this;
            case SUCCESSFUL:
            case FAILED:
               currentOp = null;
               updateState(op);
               break;
            case RETRYING:
               currentOp = null;
               updateState(op);
               scheduleRetry(op);
            default: /* no op */
            }
         }

         OperationSequence sequence = pendingOperations.peek();
         if (sequence == null) {
            currentOp = null;
            context.logger().debug(retryTasks.isEmpty() ? "exiting updating all operations applied" : "staying in updating, retryies pending");
            return retryTasks.isEmpty() ? states.get(STATE.APPLIED.name()) : this;
         }

         if (sequence.completed()) {
            pendingOperations.poll();
            sequence = pendingOperations.peek();
         }

         if (sequence == null) {
            currentOp = null;
            context.logger().debug(retryTasks.isEmpty() ? "exiting updating all operations applied" : "staying in updating, retryies pending");
            return retryTasks.isEmpty() ? states.get(STATE.APPLIED.name()) : this;
         }

         applyOperation(sequence.next());
         return this;
      }

      private void scheduleRetry(final PendingOperation op) {
         final OperationSequence sequence = pendingOperations.peek();
         if (sequence != null) {
            pendingOperations.poll();
            final OperationSequence newSequence = OperationSequence.builder(sequence).build();
            retryTasks.add(scheduler.scheduleDelayed(new Runnable() {
               @Override
               public void run() {
                  LawnNGardenStateMachine.this.onOperationRetry(newSequence);
               }
            }, config.retryTimeoutSeconds(), TimeUnit.SECONDS));
         }
      }

      @Override
      public State onDeviceOffline() {
         return states.get(STATE.PAUSED.name());
      }
      
      @Override
      public String name() {
         return STATE.UPDATING.name();
      }      
   }

   private class Paused extends State {

      @Override public State onEnter() {
         context.logger().debug("entering paused state");
         synchronized(lock) {
            return pendingOperations.isEmpty() && retryTasks.isEmpty() ? states.get(STATE.APPLIED.name()) : states.get(STATE.UPDATING.name());
         }
      }

      @Override
      public State onTimeout(PendingOperation operation) {
         synchronized (lock) {
            if (currentOp != null && currentOp.equals(operation)) {
               updateOnFailure();
            }
            return this;
         }
      }

      @Override
      public State onOperationRetry(OperationSequence operation) {
         synchronized (lock) {
            pendingOperations.add(operation);
            return this;
         }
      }

      @Override
      protected void beforeSync() {
         cleanupOperations();
      }
      
      @Override
      public String name() {
         return STATE.PAUSED.name();
      } 
   }
}

