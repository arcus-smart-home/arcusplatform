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
package com.iris.common.subsystem.care.behavior;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.behavior.evaluators.BehaviorEvaluator;
import com.iris.common.subsystem.care.behavior.evaluators.InactivityEvaluator;
import com.iris.common.subsystem.care.behavior.evaluators.OpenCountEvaluator;
import com.iris.common.subsystem.care.behavior.evaluators.OpenEvaluator;
import com.iris.common.subsystem.care.behavior.evaluators.PresenceEvaluator;
import com.iris.common.subsystem.care.behavior.evaluators.TemperatureEvaluator;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertEvent;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.CareBehavior;
import com.iris.util.TypeMarker;

public class BehaviorMonitor {
   public final static SubsystemVariableKey BEHAVIOR_KEY = new SubsystemVariableKey("behavior");
   public final static SubsystemVariableKey WINDOW_START_KEY = new SubsystemVariableKey("startWindow");
   public final static SubsystemVariableKey WINDOW_END_KEY = new SubsystemVariableKey("endWindow");
   public final static SubsystemVariableKey ALERT_KEY = new SubsystemVariableKey("beginAlert");


	public void bind(final SubsystemContext<CareSubsystemModel> context) {
      context.logger().debug("binding to the behavior monitor");
      context.models().addListener(new Listener<ModelEvent>() {
         public void onEvent(ModelEvent event) {
            if (event instanceof ModelChangedEvent) {
               dispatchEventToEvalutator((ModelChangedEvent) event, context);
            }else if(event instanceof ModelRemovedEvent) {
            	dispatchEventToEvalutator((ModelRemovedEvent) event, context);
            }
         }
      });
      if(context.model().isAlarmModeON()){
         scheduleWindowEnable(context);
      }
   }

   public void changeMode(String mode, SubsystemContext<CareSubsystemModel> context) {
      if (CareSubsystemCapability.ALARMMODE_VISIT.equals(mode)) {
         for (String behaviorId : context.model().getBehaviors()){
            CareBehaviorTypeWrapper careBehavior = getBehavior(behaviorId, context);
            careBehavior.setActive(false);
            updateBehavior(careBehavior, context);
            clearBehaviorTimeouts(behaviorId, context);
         }
         syncActiveBehaviorList(context);
      }else if (CareSubsystemCapability.ALARMMODE_ON.equals(mode)) {
         scheduleWindowEnable(context);
         // enable
      }
   }

   public static void initBehavior(String id, SubsystemContext<CareSubsystemModel> context) {
      BehaviorEvaluator behavior = loadBehaviorEvaluator(id, context);
      if(behavior.getCareBehavior().getEnabled()){
         initBehavior(behavior, context);
      }else{
         disableBehavior(behavior, context);
      }
      syncActiveBehaviorList(context);
   }

   public static void removeBehavior(String id, SubsystemContext<CareSubsystemModel> context){
      try{
         BehaviorEvaluator behavior = loadBehaviorEvaluator(id, context);
         behavior.onRemoved(context);
      }
      catch(Exception e){
         context.logger().warn("Error cleaning vars for behavior {},id");
      }
   }

   private static void disableBehavior(BehaviorEvaluator behavior, SubsystemContext<CareSubsystemModel> context){
      CareBehaviorTypeWrapper behaviorType=behavior.getCareBehavior();
      clearBehaviorTimeouts(behaviorType.getId(), context);
      behaviorType.setActive(false);
      updateBehavior(behaviorType, context);
   }
   

   public static void initBehavior(BehaviorEvaluator evaluator, SubsystemContext<CareSubsystemModel> context) {
      if(!evaluator.getCareBehavior().getEnabled()){
         return;
      }
      BehaviorUtil.removeStringFromSet(evaluator.getBehaviorId(), CareSubsystemCapability.ATTR_ACTIVEBEHAVIORS, context.model());
      evaluator.onStart(context);
      if (evaluator.getWeeklyTimeWindows() == null || evaluator.getWeeklyTimeWindows().isEmpty()) {
         SubsystemUtils.setTimeout(0, context, WINDOW_START_KEY.create(evaluator.getCareBehavior().getId()));
         return;
      }
      WeeklyTimeWindow currentWindow = evaluator.getNextTimeWindow(context.getLocalTime().getTime(), context.getLocalTime().getTimeZone());
      Date startDate = currentWindow.nextOrCurrentStartDate(context.getLocalTime().getTime(), context.getLocalTime().getTimeZone());
      context.logger().debug("setting window start timeout at {} tz: {} for {} in window {} ", startDate, context.getLocalTime().getTimeZone().getID(), evaluator, currentWindow);
      SubsystemUtils.setTimeout(startDate, context, WINDOW_START_KEY.create(evaluator.getCareBehavior().getId()));
   }

   // A behavior has been updated or removed. Cleanup any existing timeout in
   // the system.
   public static void clearBehaviorTimeouts(String behaviorId, SubsystemContext<CareSubsystemModel> context) {
      SubsystemUtils.clearTimeout(context, WINDOW_START_KEY.create(behaviorId));
      SubsystemUtils.clearTimeout(context, WINDOW_END_KEY.create(behaviorId));
      SubsystemUtils.clearTimeout(context, ALERT_KEY.create(behaviorId));
   }

   private void scheduleWindowEnable(SubsystemContext<CareSubsystemModel> context) {
      List<BehaviorEvaluator> monitoredBehaviors = getEvaluators(context);
      for (BehaviorEvaluator evaluator : monitoredBehaviors){
         try{
            initBehavior(evaluator, context);
         }catch (Exception e){
            context.logger().warn("An error occurred initializing behavior [{}]", evaluator.getCareBehavior());
         }
      }
   }

   private void clearBehaviors(SubsystemContext<CareSubsystemModel> context) {
      List<BehaviorEvaluator> monitoredBehaviors = getEvaluators(context);
      for (BehaviorEvaluator evaluator : monitoredBehaviors){
         try{
            evaluator.onAlarmCleared(context);
         }catch (Exception e){
            context.logger().warn("An error occurred clearing the alert for behavior [{}]", evaluator.getCareBehavior());
         }
      }

   }

   public void dispatchEventToEvalutator(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      for (BehaviorEvaluator evaluator : getActiveEvaluators(context)){
         try{
            evaluator.onModelChange(event, context);
         }catch (Exception e){
            context.logger().warn("An error occurred processing event [{}] for behavior [{}]", event, evaluator.getCareBehavior());
         }
      }
   }
   
   public void dispatchEventToEvalutator(ModelRemovedEvent event, SubsystemContext<CareSubsystemModel> context) {
      for (BehaviorEvaluator evaluator : getActiveEvaluators(context)){
         try{
            evaluator.onModelRemove(event, context);
         }catch (Exception e){
            context.logger().warn("An error occurred processing event [{}] for behavior [{}]", event, evaluator.getCareBehavior());
         }
      }
   }

   public void handleTimeout(ScheduledEvent event, SubsystemContext<CareSubsystemModel> context) {
      Date eventTime = new Date(event.getScheduledTimestamp());
      List<BehaviorEvaluator> monitoredBehaviors = getEvaluators(context);
      for (BehaviorEvaluator evaluator : monitoredBehaviors){
         try{
            evaluator.onTimeout(event, context);
            if (SubsystemUtils.isMatchingTimeout(event, context, WINDOW_START_KEY.create(evaluator.getBehaviorId()))) {
               context.logger().debug("Window begin for behavior [{}]", evaluator.getCareBehavior());
               beginWindow(evaluator, eventTime, context);
            }else if (SubsystemUtils.isMatchingTimeout(event, context, WINDOW_END_KEY.create(evaluator.getBehaviorId()))) {
               context.logger().debug("Window end for behavior [{}]", evaluator.getCareBehavior());
               endWindow(evaluator, eventTime, context);
            }else if (SubsystemUtils.isMatchingTimeout(event, context, ALERT_KEY.create(evaluator.getBehaviorId()))) {
               context.logger().debug("Alert signal received for behavior [{}]", evaluator.getCareBehavior());
               raiseCareAlert(evaluator, context);
            }
         }catch (Exception e){
            context.logger().warn("An error occurred processing event [{}] for behavior [{}]", event, evaluator.getCareBehavior());
         }
      }
   }

   private void beginWindow(BehaviorEvaluator behavior, Date eventTime, SubsystemContext<CareSubsystemModel> context) {
      WeeklyTimeWindow wtw = behavior.getNextTimeWindow(eventTime, context.getLocalTime().getTimeZone());
      if (wtw != null) {
         Date endDate = wtw.calculateEndDate(eventTime);
         SubsystemUtils.setTimeout(endDate, context, WINDOW_END_KEY.create(behavior.getBehaviorId()));
      }

      behavior.onWindowStart(null, context);

      CareBehaviorTypeWrapper behaviorType = behavior.getCareBehavior();
      behaviorType.setLastActivated(eventTime);
      behaviorType.setActive(true);
      updateBehavior(behaviorType, context);
      syncActiveBehaviorList(context);
   }

   private void endWindow(BehaviorEvaluator behavior, Date eventTime, SubsystemContext<CareSubsystemModel> context) {
      context.logger().debug("Window has ended for behavior [{}].", behavior.getCareBehavior());
      WeeklyTimeWindow wtw = behavior.getNextTimeWindow(eventTime, context.getLocalTime().getTimeZone());
      if (wtw != null) {
         Date nextStartDate = wtw.nextWeekStartDate(context.getLocalTime().getTime(), context.getLocalTime().getTimeZone());
         SubsystemUtils.setTimeout(nextStartDate, context, WINDOW_START_KEY.create(behavior.getBehaviorId()));
         context.logger().debug("Scheduling next window start for behavior [{}] window will next wake at {}", behavior.getCareBehavior(), nextStartDate);
      }

      behavior.onWindowEnd(null, context);

      CareBehaviorTypeWrapper behaviorType = behavior.getCareBehavior();
      behaviorType.setActive(false);
      updateBehavior(behaviorType, context);
      syncActiveBehaviorList(context);
   }

   private static void syncActiveBehaviorList(SubsystemContext<CareSubsystemModel> context) {
      Set<String> activeBehaviors = new HashSet<>();
      boolean alarmOn = CareSubsystemCapability.ALARMMODE_ON.equals(context.model().getAlarmMode());
      for (String behaviorId : context.model().getBehaviors()){
         CareBehaviorTypeWrapper behavior = getBehavior(behaviorId, context);
         if (alarmOn && behavior.getActive() && behavior.getEnabled()) {
            activeBehaviors.add(behavior.getId());
         }
      }
      context.model().setActiveBehaviors(activeBehaviors);
   }

   public void careAlarmCleared(SubsystemContext<CareSubsystemModel> context) {
      // TODO: on care behaviors if the alarm is disarmed then it should restart
      // all the behaviors as if it was the start of a new window
      clearBehaviors(context);
      if(context.model().isAlarmModeON()){
         scheduleWindowEnable(context);
      }
   }

   private void raiseCareAlert(BehaviorEvaluator evaluator, SubsystemContext<CareSubsystemModel> context) {
      CareBehaviorTypeWrapper behaviorType = evaluator.getCareBehavior();
      behaviorType.setLastFired(context.getLocalTime().getTime());
      updateBehavior(behaviorType, context);
      context.broadcast(BehaviorAlertEvent.builder()
            .withBehaviorId(evaluator.getBehaviorId())
            .withBehaviorName(behaviorType.getName())
            .withTriggeredDevices(ImmutableSet.<String> of()).build());
   }

   private static void updateBehavior(CareBehaviorTypeWrapper careBehavior, SubsystemContext<CareSubsystemModel> context) {
      context.setVariable(BEHAVIOR_KEY.create(careBehavior.getId()), careBehavior.toMap());
   }

   private static CareBehaviorTypeWrapper getBehavior(String behaviorId, SubsystemContext<CareSubsystemModel> context) {
      Map<String, Object> behavior = context.getVariable(BEHAVIOR_KEY.create(behaviorId)).as(TypeMarker.mapOf(String.class, Object.class));
      return new CareBehaviorTypeWrapper(behavior);
   }

   protected List<BehaviorEvaluator> getEvaluators(SubsystemContext<CareSubsystemModel> context) {
      List<BehaviorEvaluator> behaviors = new ArrayList<>();
      for (String behaviorId : context.model().getBehaviors()){
         BehaviorEvaluator careBehavior = validateBehavior(behaviorId, context);
         if (careBehavior != null) {
            behaviors.add(careBehavior);
         }
      }
      return behaviors;
   }

   protected List<BehaviorEvaluator> getActiveEvaluators(SubsystemContext<CareSubsystemModel> context) {
      List<BehaviorEvaluator> behaviors = new ArrayList<>();
      for (String behaviorId : context.model().getActiveBehaviors()){
         BehaviorEvaluator careBehavior = validateBehavior(behaviorId, context);
         if (careBehavior != null) {
            behaviors.add(careBehavior);
         }
      }
      return behaviors;
   }

   private static BehaviorEvaluator validateBehavior(String behaviorId, SubsystemContext<CareSubsystemModel> context) {
      try{
         BehaviorEvaluator careBehavior = loadBehaviorEvaluator(behaviorId, context);
         return careBehavior;
      }catch (Exception e){
         context.logger().error("bad behavior configuration for behavior " + behaviorId, e);
         BehaviorUtil.removeStringFromSet(behaviorId, CareSubsystemCapability.ATTR_ACTIVEBEHAVIORS, context.model());
         BehaviorUtil.removeStringFromSet(behaviorId, CareSubsystemCapability.ATTR_BEHAVIORS, context.model());
         context.setVariable(BehaviorMonitor.BEHAVIOR_KEY.create(behaviorId), null);
         return null;
      }
   }

   public static BehaviorEvaluator loadBehaviorEvaluator(CareBehaviorTypeWrapper cb, SubsystemContext<CareSubsystemModel> context) {
      Map<String, Object> behavior = cb.getAttributes();
      String type = cb.getType();
      switch (type) {
      case CareBehavior.TYPE_INACTIVITY:
         InactivityEvaluator careBehavior = new InactivityEvaluator(behavior);
         return careBehavior;
      case CareBehavior.TYPE_OPEN:
         OpenEvaluator openBehavior = new OpenEvaluator(behavior);
         return openBehavior;
      case CareBehavior.TYPE_TEMPERATURE:
         TemperatureEvaluator tempBehavior = new TemperatureEvaluator(behavior);
         return tempBehavior;
      case CareBehavior.TYPE_PRESENCE:
         PresenceEvaluator presenceBehavior = new PresenceEvaluator(behavior);
         return presenceBehavior;
      case CareBehavior.TYPE_OPEN_COUNT:
         OpenCountEvaluator openCountBehavior = new OpenCountEvaluator(behavior);
         return openCountBehavior;
      default:
         throw new IllegalArgumentException("unknown behavior type: " + type);
      }  
   }
   
   private static BehaviorEvaluator loadBehaviorEvaluator(String behaviorId, SubsystemContext<CareSubsystemModel> context) {
      Map<String, Object> behavior = getBehavior(behaviorId, context).toMap();
      CareBehaviorTypeWrapper cb = new CareBehaviorTypeWrapper(behavior);
      return loadBehaviorEvaluator(cb, context);
   }

}

