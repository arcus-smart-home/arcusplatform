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
package com.iris.common.subsystem.safety;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.CallTree;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.LeakGasCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SafetySubsystemCapability.ClearRequest;
import com.iris.messages.capability.SafetySubsystemCapability.ClearResponse;
import com.iris.messages.capability.SafetySubsystemCapability.TriggerRequest;
import com.iris.messages.capability.SafetySubsystemCapability.TriggerResponse;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.dev.CarbonMonoxideModel;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DevicePowerModel;
import com.iris.messages.model.dev.LeakGasModel;
import com.iris.messages.model.dev.LeakH2OModel;
import com.iris.messages.model.dev.SmokeModel;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.messages.type.CallTreeEntry;
import com.iris.messages.type.TriggerEvent;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.util.IrisCollections;

@Singleton
@Subsystem(SafetySubsystemModel.class)
@Version(1)
public class SafetySubsystemV1 extends BaseSubsystem<SafetySubsystemModel> {
   public static final String SAFETY_ALERT_PREFIX_KEY = "safety";
   public static final String SAFETY_ALERT_SUFFIX_KEY = ".alert";

   public static final String SAFETY_CLEAR_KEY = "safety.clear";

   public static final String WARN_OFFLINE = "warning.offline";
   public static final String WARN_POOR_SIGNAL = "warning.poor_signal";
   public static final String WARN_LOW_BATTERY = "warning.low_battery";

   public static final int BATTERY_WARNING_THRESHOLD = 5;
   public static final int SIGNAL_WARNING_THRESHOLD = 5;

   public static final String SENSOR_TYPE_SMOKE = "SMOKE";
   public static final String SENSOR_TYPE_CO = "CO";
   public static final String SENSOR_TYPE_WATER = "WATER";
   public static final String SENSOR_TYPE_GAS = "GAS";

   // these messages should last a bit longer than normal
   public static final int VALVE_CLOSE_TIMEOUT_MS = (int) TimeUnit.MINUTES.toMillis(5);

   private static final String QUERY_SAFETY_DEVICES =
         "base:caps contains '" + SmokeCapability.NAMESPACE + "' OR " +
         "base:caps contains '" + CarbonMonoxideCapability.NAMESPACE + "' OR " +
         "base:caps contains '" + LeakH2OCapability.NAMESPACE + "' OR " +
         "base:caps contains '" + LeakGasCapability.NAMESPACE + "'"
         ;
 
   private static final String QUERY_PEOPLE =
         "base:caps contains '" + PersonCapability.NAMESPACE + "'";

   private static final Predicate<Model> IS_SAFETY_DEVICE = ExpressionCompiler.compile(QUERY_SAFETY_DEVICES);

   private CallTree callTreeHelper = new CallTree(SafetySubsystemCapability.ATTR_CALLTREE);
   
   public SafetySubsystemV1() {
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.AnnotatedSubsystem#onStarted(com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   protected void onStarted(SubsystemContext<SafetySubsystemModel> context) {
      super.onStarted(context);

      // make sure everything is in sync
      Set<String> safetyDevices = new HashSet<>();
      Set<String> activeDevices = new HashSet<>();
      List<Map<String,Object>> triggerDevices = new LinkedList<>();
      List<Map<String,Object>> pendingClear = IrisCollections.copyOf(context.model().getPendingClear());
      Map<String, String> warnings = new HashMap<>();

      List<Map<String,Object>> existingCallTree = context.model().getCallTree();
      List<Map<String,Object>> callTree = existingCallTree == null ? new ArrayList<Map<String,Object>>() : new ArrayList<Map<String,Object>>(existingCallTree);

      for(Model model: context.models().getModels()) {
         if(model.getType().equals(PlaceCapability.NAMESPACE)) {
            context.model().setCallTreeEnabled(ServiceLevel.isPremiumOrPromon(PlaceModel.getServiceLevel(model)));
         }
         if(model.getType().equals(AccountCapability.NAMESPACE)) {
            // if the call tree is empty make sure the account owner is set
            String owner = AccountModel.getOwner(model);
            if(callTree.isEmpty() && owner != null) {
               CallTreeEntry cte = new CallTreeEntry();
               cte.setEnabled(true);
               cte.setPerson("SERV:" + PersonCapability.NAMESPACE + ":" + owner);
               callTree.add(cte.toMap());
            }
         }

         if(!isSafetyDevice(model)) {
            continue;
         }

         safetyDevices.add(model.getAddress().getRepresentation());

         List<TriggerEvent> events = getTriggerEvents(model);
         if(!events.isEmpty()) {
            addTriggerEvents(events, triggerDevices);
         } else {
            removeAllTriggerEvents(model, pendingClear);
         }

         if(!isDisconnected(model)) {
            activeDevices.add(model.getAddress().getRepresentation());
         }

         String warning = getWarning(model);
         if(warning != null) {
            warnings.put(model.getAddress().getRepresentation(), warning);
         }
         
         if(model.supports(LeakH2OCapability.NAMESPACE) && LeakH2OModel.isStateLEAK(model)) {
            shutOffWater(context);
         }
         
      }

      callTree = NotificationsUtil.fixCallTree(callTree);

      context.model().setTotalDevices(safetyDevices);
      context.model().setActiveDevices(activeDevices);
      context.model().setTriggers(triggerDevices);
      context.model().setPendingClear(pendingClear);
      context.model().setWarnings(warnings);
      context.model().setQuietPeriodSec(0);
      context.model().setAvailable(!safetyDevices.isEmpty());
      context.model().setCallTree(callTree);

      // water shut-off
      setIfNull(context.model(), SafetySubsystemCapability.ATTR_WATERSHUTOFF, Boolean.TRUE);

      // remove any ignored devices that are no longer around
      Set<String> ignoredDevices = new HashSet<>(context.model().getIgnoredDevices());
      ignoredDevices.retainAll(safetyDevices);
      context.model().setIgnoredDevices(ignoredDevices);

      State state = state(context.model().getAlarm());
      String next = state.onEnter(context);
      transition(state, next, context);
   }

   @Override
   public void onAdded(SubsystemContext<SafetySubsystemModel> context) {
      super.onAdded(context);
      SafetySubsystemModel model = context.model();
      model.setAlarm(SafetySubsystemCapability.ALARM_READY);
      model.setAlarmSensitivityDeviceCount(1);
      model.setAlarmSensitivitySec(0);
      model.setSilentAlarm(false);
      model.setWaterShutOff(true);
      model.setTotalDevices(new HashSet<String>());
      model.setIgnoredDevices(new HashSet<String>());
      model.setTriggers(new LinkedList<Map<String,Object>>());
      model.setPendingClear(new LinkedList<Map<String,Object>>());

      // TODO override setAttributes to enforce people are associated with the current place
   }
   
   public void onStopped(SubsystemContext<SafetySubsystemModel> context) {
   	// no-op?
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.iris.common.subsystem.BaseSubsystem#setAttributes(com.iris.messages
    * .MessageBody, com.iris.common.subsystem.SubsystemContext)
    */
   @Override
   @Request(Capability.CMD_SET_ATTRIBUTES)
   public MessageBody setAttributes(PlatformMessage request, SubsystemContext<SafetySubsystemModel> context) {
      try{
         return super.setAttributes(request, context);
      }finally{
         Set<String> attributes = request.getValue().getAttributes().keySet();
         if (attributes.contains(SafetySubsystemCapability.ATTR_CALLTREE)){
            callTreeHelper.syncCallTree(context);
         }
      }
   }
   // TODO satisfiable / activate / deactivate / timeouts

   @OnValueChanged(attributes = {
         PlaceCapability.ATTR_SERVICELEVEL
   })
   public void onSubscriptionLevelChange(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      context.logger().info("Detected a subscription level change {}", event);
      syncCallTreeEnabled(context);
   }
   
   private void syncCallTreeEnabled(SubsystemContext<SafetySubsystemModel> context){
      String serviceLevel = String.valueOf(context.models().getAttributeValue(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE), PlaceCapability.ATTR_SERVICELEVEL));
      context.model().setCallTreeEnabled(ServiceLevel.isPremiumOrPromon(serviceLevel));
   }
   
   @OnAdded(query = QUERY_PEOPLE)
   public void onPersonAdded(ModelAddedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      List<Map<String,Object>>callTree=IrisCollections.copyOf(context.model().getCallTree());
      NotificationsUtil.removeCTEByAddress(event.getAddress().getRepresentation(), callTree);
      callTree.add(NotificationsUtil.createCallEntry(event.getAddress(),false).toMap());
      context.model().setCallTree(callTree);
      context.logger().info("A new person was added to the call tree {}", event);
   }

   @OnRemoved(query = QUERY_PEOPLE)
   public void onPersonRemoved(ModelRemovedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      List<Map<String,Object>>callTree=IrisCollections.copyOf(context.model().getCallTree());
      NotificationsUtil.removeCTEByAddress(event.getAddress().getRepresentation(), callTree);
      context.model().setCallTree(callTree);
      context.logger().info("A person was removed from the call tree {}", event);
   }

   @OnAdded(query=QUERY_SAFETY_DEVICES)
   public void onDeviceAdded(ModelAddedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      if(addSafetyDevice(event.getAddress().getRepresentation(), context)) {
         context.logger().info("A new safety device was added {}", event);
      }
   }
   
   @OnValueChanged(attributes=Capability.ATTR_CAPS)
   public void onCapsChanged(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      Model device = context.models().getModelByAddress(event.getAddress());
      if(device == null) {
         context.logger().debug("Received a ValueChange on an unrecognized device [{}]", event);
         return;
      }

      boolean isSafetyDevice = isSafetyDevice(device);
      boolean isKnownDevice = context.model().getTotalDevices().contains(device.getAddress().getRepresentation());
      if(isSafetyDevice && !isKnownDevice) {
         addSafetyDevice(device.getAddress().getRepresentation(), context);
      }
      else if(!isSafetyDevice && isKnownDevice) {
         removeSafetyDevice(device, context);
      }
   }

   @OnRemoved(query=QUERY_SAFETY_DEVICES)
   public void onDeviceRemoved(ModelRemovedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      if(removeSafetyDevice(event.getModel(), context)) {
         context.logger().info("A safety device was removed {}", event);
      }
   }

   @OnValueChanged(attributes=SmokeCapability.ATTR_SMOKE)
   public void onSmoke(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      String value = (String) event.getAttributeValue();
      if(value == null) {
         context.logger().warn("Received invalid value for smoke [{}] on [{}]", value, event.getAddress());
         return;
      }
      switch(value) {
      case SmokeCapability.SMOKE_DETECTED:
         triggerAlarm(event.getAddress(), SENSOR_TYPE_SMOKE, context);
         break;
      case SmokeCapability.SMOKE_SAFE:
         clearAlarm(event.getAddress(), SENSOR_TYPE_SMOKE, context);
         break;
      default:
         context.logger().warn("Received invalid value for smoke [{}] on [{}]", value, event.getAddress());
      }
   }

   @OnValueChanged(attributes=CarbonMonoxideCapability.ATTR_CO)
   public void onCO(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      String value = (String) event.getAttributeValue();
      if(value == null) {
         context.logger().warn("Received invalid value for co [{}] on [{}]", value, event.getAddress());
         return;
      }
      switch(value) {
      case CarbonMonoxideCapability.CO_DETECTED:
         triggerAlarm(event.getAddress(), SENSOR_TYPE_CO, context);
         break;
      case CarbonMonoxideCapability.CO_SAFE:
         clearAlarm(event.getAddress(), SENSOR_TYPE_CO, context);
         break;
      default:
         context.logger().warn("Received invalid value for co [{}] on [{}]", value, event.getAddress());
      }
   }

   @OnValueChanged(attributes=LeakH2OCapability.ATTR_STATE)
   public void onWaterLeak(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      String value = (String) event.getAttributeValue();
      if(value == null) {
         context.logger().warn("Received invalid value for leakh2o [{}] on [{}]", value, event.getAddress());
         return;
      }
      switch(value) {
      case LeakH2OCapability.STATE_LEAK:
         shutOffWater(context);
         triggerAlarm(event.getAddress(), SENSOR_TYPE_WATER,context);
         break;
      case LeakH2OCapability.STATE_SAFE:
         clearAlarm(event.getAddress(), SENSOR_TYPE_WATER, context);
         break;
      default:
         context.logger().warn("Received invalid value for leakh2o [{}] on [{}]", value, event.getAddress());
      }
   }

   @OnValueChanged(attributes=LeakGasCapability.ATTR_STATE)
   public void onGasLeak(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      String value = (String) event.getAttributeValue();
      if(value == null) {
         context.logger().warn("Received invalid value for leakgas [{}] on [{}]", value, event.getAddress());
         return;
      }
      switch(value) {
      case LeakGasCapability.STATE_LEAK:
         triggerAlarm(event.getAddress(), SENSOR_TYPE_GAS, context);
         break;
      case LeakGasCapability.STATE_SAFE:
         clearAlarm(event.getAddress(), SENSOR_TYPE_GAS, context);
         break;
      default:
         context.logger().warn("Received invalid value for leakgas [{}] on [{}]", value, event.getAddress());
      }
   }

   @OnValueChanged(attributes={
         DeviceConnectionCapability.ATTR_STATE,
         DeviceConnectionCapability.ATTR_SIGNAL,
         DevicePowerCapability.ATTR_BATTERY
   })
   public void onConnectivityStateChange(ModelChangedEvent event, SubsystemContext<SafetySubsystemModel> context) {
      String address = event.getAddress().getRepresentation();
      if(!context.model().getTotalDevices().contains(address)) {
         context.logger().debug("Ignoring value change from non-safety device {}", event);
         return;
      }
      if(context.model().getIgnoredDevices().contains(address)) {
         context.logger().debug("Ignoring value change from ignored safety device {}", event);
         return;
      }
      Model model = context.models().getModelByAddress(event.getAddress());
      if(model == null) {
         context.logger().warn("Unable to retrieve model for safety device {}", address);
         return;
      }
      String currentWarning = context.model().getWarnings().get(address);
      // this is in priority order of the warnings
      if(isDisconnected(model) && !WARN_OFFLINE.equals(currentWarning)) {
         addWarning(address, WARN_OFFLINE, context);
      }
      else if(isLowBattery(model) && !WARN_LOW_BATTERY.equals(currentWarning)) {
         addWarning(address, WARN_LOW_BATTERY, context);
      }
      else if(isPoorSignal(model) && !WARN_POOR_SIGNAL.equals(currentWarning)) {
         addWarning(address, WARN_POOR_SIGNAL, context);
      }
      else if(currentWarning != null) {
         clearWarning(address, context);
      }

      updateActive(address, !isDisconnected(model), context);
   }

   @OnScheduledEvent
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<SafetySubsystemModel> context) {
      State state = state(context.model().getAlarm());
      if(!SubsystemUtils.isMatchingTimeout(event, context)) {
         return;
      }

      String next = state.timeout(context);
      transition(state, next, context);
   }

   @Request(TriggerRequest.NAME)
   public MessageBody trigger(
         PlatformMessage message,
         SubsystemContext<SafetySubsystemModel> context
   ) {
      State state = state(context.model().getAlarm());
      String next = state.triggerAlarm(message, context);
      transition(state, next, context);
      return TriggerResponse.instance();
   }

   @Request(ClearRequest.NAME)
   public MessageBody clear(PlatformMessage message, SubsystemContext<SafetySubsystemModel> context) {
      State state = state(context.model().getAlarm());
      String next = state.clearAlarm(message, context);
      transition(state, next, context);
      return ClearResponse.instance();
   }
   
   protected State state(String state) {
      switch(state) {
      case SafetySubsystemCapability.ALARM_READY:
         return ready;
      case SafetySubsystemCapability.ALARM_WARN:
         return warning;
      case SafetySubsystemCapability.ALARM_SOAKING:
         return soaking;
      case SafetySubsystemCapability.ALARM_ALERT:
         return alert;
      case SafetySubsystemCapability.ALARM_CLEARING:
         return clearing;
      default:
         throw new IllegalStateException("Unrecognized state: " + state);
      }
   }

   protected void transition(State state, String next, SubsystemContext<SafetySubsystemModel> context) {
      if(next.equals(state.getName())) {
         return;
      }
      try {
         state.onExit(context);
      }
      catch(Exception e) {
         context.logger().warn("Error exiting state [{}]", state, e);
      }
      context.model().setAlarm(next);
      try {
         state = state(next);
         next = state.onEnter(context);
      }
      catch(Exception e) {
         context.logger().warn("Error entering state [{}]", next, e);
      }
      transition(state, next, context);
   }

   protected void updateActive(String address, boolean online, SubsystemContext<SafetySubsystemModel> context) {
      Set<String> active = new HashSet<>(context.model().getActiveDevices());
      if(online && active.add(address)) {
         context.model().setActiveDevices(active);
      }
      if(!online && active.remove(address)) {
         context.model().setActiveDevices(active);
      }
   }

   private List<String> getSensorTypes(Model model) {
      List<String> sensors = new LinkedList<>();
      if(model.getCapabilities().contains(SmokeCapability.NAMESPACE)) { sensors.add(SENSOR_TYPE_SMOKE); }
      if(model.getCapabilities().contains(CarbonMonoxideCapability.NAMESPACE)) { sensors.add(SENSOR_TYPE_CO); }
      if(model.getCapabilities().contains(LeakH2OCapability.NAMESPACE)) { sensors.add(SENSOR_TYPE_WATER); }
      if(model.getCapabilities().contains(LeakGasCapability.NAMESPACE)) { sensors.add(SENSOR_TYPE_GAS); }
      return sensors;
   }

   private List<TriggerEvent> getTriggerEvents(Model model) {
      List<TriggerEvent> events = new LinkedList<>();
      if(model.getCapabilities().contains(SmokeCapability.NAMESPACE) &&
            SmokeModel.isSmokeDETECTED(model)) {
         events.add(createTriggerEvent(model.getAddress(), SENSOR_TYPE_SMOKE, SmokeModel.getSmokechanged(model)));
      }
      if(model.getCapabilities().contains(CarbonMonoxideCapability.NAMESPACE) &&
            CarbonMonoxideModel.isCoDETECTED(model)) {
         events.add(createTriggerEvent(model.getAddress(), SENSOR_TYPE_CO, CarbonMonoxideModel.getCochanged(model)));
      }
      if(model.getCapabilities().contains(LeakH2OCapability.NAMESPACE) &&
            LeakH2OModel.isStateLEAK(model)) {
         events.add(createTriggerEvent(model.getAddress(), SENSOR_TYPE_WATER, LeakH2OModel.getStatechanged(model)));
      }
      if(model.getCapabilities().contains(LeakGasCapability.NAMESPACE) &&
            LeakGasModel.isStateLEAK(model)) {
         events.add(createTriggerEvent(model.getAddress(), SENSOR_TYPE_GAS, LeakGasModel.getStatechanged(model)));
      }
      return events;
   }

   private TriggerEvent createTriggerEvent(Address address, String sensor, Date time) {
      TriggerEvent event = new TriggerEvent();
      event.setDevice(address.getRepresentation());
      event.setType(sensor);
      event.setTime(time);
      return event;
   }

   protected boolean addTriggerEvents(List<TriggerEvent> triggers, List<Map<String,Object>> events) {
      if(events == null || triggers.isEmpty()) { return false; }
      boolean changed = false;
      for(TriggerEvent event : triggers) {
         changed |= addTriggerEvent(event, events);
      }
      return changed;
   }

   static boolean addTriggerEvent(TriggerEvent event, List<Map<String,Object>> events) {
      if(event == null) {
         return false;
      }

      for(Map<String,Object> trigger : events) {
         if(event.getDevice().equals(trigger.get(TriggerEvent.ATTR_DEVICE)) &&
            event.getType().equals(trigger.get(TriggerEvent.ATTR_TYPE))) {

            return false;
         }
      }

      return events.add(event.toMap());
   }

   protected List<TriggerEvent> removeAllTriggerEvents(Model m, List<Map<String,Object>> events) {
      List<TriggerEvent> removed = new LinkedList<>();
      List<String> sensors = getSensorTypes(m);
      for(String sensor : sensors) {
         TriggerEvent removedTrigger = removeTriggerEvent(m.getAddress().getRepresentation(), sensor, events);
         if(removedTrigger != null) { removed.add(removedTrigger); }
      }
      return removed;
   }

   static TriggerEvent removeTriggerEvent(String address, String sensor, List<Map<String,Object>> events) {
      if(address == null) {
         return null;
      }

      Map<String,Object> toRemove = null;
      for(Map<String,Object> trigger : events) {
         if(address.equals(trigger.get(TriggerEvent.ATTR_DEVICE)) &&
            sensor.equals(trigger.get(TriggerEvent.ATTR_TYPE))) {
            toRemove = trigger;
            break;
         }
      }

      if(toRemove == null) {
         return null;
      }

      if(events.remove(toRemove)) {
         return new TriggerEvent(toRemove);
      }

      return null;
   }

   protected String getWarning(Model model) {
      if(isDisconnected(model)) {
         return WARN_OFFLINE;
      }
      if(isLowBattery(model)) {
         return WARN_LOW_BATTERY;
      }
      if(isPoorSignal(model)) {
         return WARN_POOR_SIGNAL;
      }
      return null;
   }

   protected boolean addSafetyDevice(String address, SubsystemContext<SafetySubsystemModel> context) {
      Set<String> devices = new HashSet<>(context.model().getTotalDevices());
      if(devices.add(address)) {
         context.model().setAvailable(true);
         context.model().setTotalDevices(devices);

         Model m = context.models().getModelByAddress(Address.fromString(address));
         updateActive(address, !isDisconnected(m), context);

         List<TriggerEvent> events = getTriggerEvents(m);
         if(!events.isEmpty()) {
            for(TriggerEvent event : events) {
               triggerAlarm(Address.fromString(address), event.getType(), context);
            }
         }
         String warning = getWarning(m);
         if(warning != null) {
            addWarning(address, warning, context);
         }

         context.logger().debug("Discovered new safety device {}", address);
         return true;
      }
      return false;
   }

   protected boolean removeSafetyDevice(Model m, SubsystemContext<SafetySubsystemModel> context) {
      boolean response = false;
      String address = m.getAddress().getRepresentation();
      Set<String> devices = new HashSet<>(context.model().getTotalDevices());
      if(devices.remove(address)) {
         context.model().setTotalDevices(devices);
         context.logger().info("Removed a safety device {}", address);
         response = true;
      }

      Set<String> ignored = new HashSet<>(context.model().getIgnoredDevices());
      if(ignored.remove(address)) {
         context.model().setIgnoredDevices(ignored);
      }
      updateActive(address, false, context);
      if(devices.isEmpty()) {
         context.model().setAvailable(false);
      }

      List<String> sensors = getSensorTypes(m);
      for(String s : sensors) {
         clearAlarm(Address.fromString(address), s, context);
      }

      clearWarning(address, context);

      return response;
   }

   protected void addWarning(String address, String warning, SubsystemContext<SafetySubsystemModel> context) {
      State state = state(context.model().getAlarm());
      String next = state.addWarning(address, warning, context);
      transition(state, next, context);
   }

   protected void clearWarning(String address, SubsystemContext<SafetySubsystemModel> context) {
      State state = state(context.model().getAlarm());
      String next = state.clearWarning(address, context);
      transition(state, next, context);
   }

   protected void triggerAlarm(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
      State state = state(context.model().getAlarm());
      String next = state.triggerDevice(device, sensor, context);
      transition(state, next, context);
   }
   
   protected void shutOffWater(SubsystemContext<SafetySubsystemModel> context) {
      if(Boolean.FALSE.equals(context.model().getWaterShutOff())) {
         context.logger().warn("Detected a leak but not closing the valve because water shut off is false");
         return;
      }
      
      context.logger().warn("Attempting to shut off water...");
      for(String valveAddress: context.model().getWaterShutoffValves()) {
         Model model = context.models().getModelByAddress(Address.fromString(valveAddress));
         MessageBody shutoff =
               MessageBody
                  .buildMessage(
                        Capability.CMD_SET_ATTRIBUTES,
                        ImmutableMap.<String, Object>of(
                              ValveCapability.ATTR_VALVESTATE,
                              ValveCapability.VALVESTATE_CLOSED
                        )
                  );
         // TODO handle retries...
         context.request(model.getAddress(), shutoff, VALVE_CLOSE_TIMEOUT_MS);
      }
   }

   protected void clearAlarm(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
      State state = state(context.model().getAlarm());
      String next = state.clearDevice(device, sensor, context);
      transition(state, next, context);
   }

   protected boolean isSafetyDevice(Model m) {
      return IS_SAFETY_DEVICE.apply(m);
   }

   protected boolean isDisconnected(Model model) {
      return DeviceConnectionModel.isStateOFFLINE(model);
   }

   protected boolean isLowBattery(Model model) {
      if(!DevicePowerModel.isSourceBATTERY(model)) {
         return false;
      }

      Integer level = DevicePowerModel.getBattery(model, 100);
      return level < BATTERY_WARNING_THRESHOLD;
   }

   protected boolean isPoorSignal(Model model) {
      Integer level = DeviceConnectionModel.getSignal(model, 100);
      return level < BATTERY_WARNING_THRESHOLD;
   }

   protected void sendAlert(SubsystemContext<SafetySubsystemModel> context) {
	  String lastAlertCause = context.model().getLastAlertCause();
      String alertKey = SAFETY_ALERT_PREFIX_KEY + 
                  (lastAlertCause!=null?"."+lastAlertCause.toLowerCase():"") +
                  SAFETY_ALERT_SUFFIX_KEY;
      
      Map<String, String> alertParams = ImmutableMap.<String, String> of();
      List<Map<String, Object>> alertTriggers = context.model().getTriggers();
      String curCause = null;
      String triggerDevice = null;
      if(alertTriggers != null && lastAlertCause != null) {
    	  for(Map<String, Object>curTrigger : alertTriggers) {
    		  curCause = (String) curTrigger.get(TriggerEvent.ATTR_TYPE);
    		  if(lastAlertCause.equalsIgnoreCase(curCause)) {
    			  triggerDevice = (String) curTrigger.get(TriggerEvent.ATTR_DEVICE);
    			  break;
    		  }
    	  }
      }
      Model triggerDeviceModel = null;
      if(triggerDevice != null) {
    	  triggerDeviceModel = context.models().getModelByAddress(Address.fromString(triggerDevice));
    	  if(triggerDeviceModel != null) {
    		  alertParams = ImmutableMap.<String, String> of(
    				  NotificationsUtil.SafetyAlertCommon.PARAM_DEVICE_NAME, DeviceModel.getName(triggerDeviceModel, ""),
    				  NotificationsUtil.SafetyAlertCommon.PARAM_DEVICE_TYPE, DeviceModel.getDevtypehint(triggerDeviceModel, ""));
    	  }
      }
      
      
      if(SafetySubsystemModel.getCallTreeEnabled(context.model())){
         callTreeHelper.notifyParallel(context, alertKey, alertParams);
      }else{
         String ownerAddress = NotificationsUtil.getAccountOwnerAddress(context);
         NotificationsUtil.sendNotification(context, alertKey, ownerAddress, NotifyRequest.PRIORITY_CRITICAL, alertParams);
      }
      if(!SafetySubsystemModel.getSilentAlarm(context.model(), false)) {
         NotificationsUtil.soundTheAlarms(context);
      }
   }

   protected void clearAlert(SubsystemContext<SafetySubsystemModel> context) {
      String ownerAddress = NotificationsUtil.getAccountOwnerAddress(context);
      String lastClearBy = context.model().getLastClearedBy();
      String lastClearByFirstName = "";
      String lastClearByLastName = "";
      if(lastClearBy != null) {
    	  Model curPerson = context.models().getModelByAddress(Address.fromString(lastClearBy));
    	  lastClearByFirstName = PersonModel.getFirstName(curPerson, "");
    	  lastClearByLastName = PersonModel.getLastName(curPerson, "");
      }
      NotificationsUtil.sendNotification(context, NotificationsUtil.SafetyClear.KEY, ownerAddress, NotifyRequest.PRIORITY_MEDIUM, 
    		  ImmutableMap.<String, String> of(NotificationsUtil.SafetyClear.PARAM_CANCELL_BY_FIRSTNAME, lastClearByFirstName,
    		  NotificationsUtil.SafetyClear.PARAM_CANCELL_BY_LASTNAME, lastClearByLastName));
      NotificationsUtil.stopTheAlarms(context);
   }


   private static class State {
      final String name;
      State(String name) {
         this.name = name;
      }

      public String getName() { return name; }

      public String onEnter(SubsystemContext<SafetySubsystemModel> context) {
         context.logger().debug("Entering state: [{}]", getName());
         return getName();
      }

      public void onExit(SubsystemContext<SafetySubsystemModel> context) {
         context.logger().debug("Exiting state: [{}]", getName());
         clearTimeout(context);
      }

      public String triggerDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         String address = device.getRepresentation();
         List<Map<String,Object>> triggers = context.model().getTriggers();
         TriggerEvent event = new TriggerEvent();
         event.setDevice(address);
         event.setTime(new Date());
         event.setType(sensor);
         if(addTriggerEvent(event, triggers)) {
            context.model().setTriggers(triggers);
         }
         return getName();
      }

      public String addWarning(String address, String warning, SubsystemContext<SafetySubsystemModel> context) {
         Map<String, String> warnings = new HashMap<String, String>(context.model().getWarnings());
         warnings.put(address, warning);
         context.model().setWarnings(warnings);
         return getName();
      }

      public String clearWarning(String address, SubsystemContext<SafetySubsystemModel> context) {
         Map<String, String> warnings = new HashMap<String, String>(context.model().getWarnings());
         warnings.remove(address);
         context.model().setWarnings(warnings);
         return getName();
      }

      public String clearDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         String address = device.getRepresentation();
         List<Map<String,Object>> triggers = new ArrayList<>(context.model().getTriggers());
         List<Map<String,Object>> pendingClear = new ArrayList<>(context.model().getPendingClear());
         if(removeTriggerEvent(address, sensor, triggers) != null) {
            context.model().setTriggers(triggers);
         }
         if(removeTriggerEvent(address, sensor, pendingClear) != null) {
            context.model().setPendingClear(pendingClear);
         }
         return getName();
      }

      public String triggerAlarm(PlatformMessage message, SubsystemContext<SafetySubsystemModel> context) {
         context.logger().info("Triggering the alarm was requested {}", message);
         context.model().setLastAlertCause(SafetySubsystemCapability.TriggerRequest.getCause(message.getValue()));
         return SafetySubsystemCapability.ALARM_ALERT;
      }

      public String clearAlarm(PlatformMessage message, SubsystemContext<SafetySubsystemModel> context) {
         context.logger().info("Clearing the alarm was requested {}", message);
         return getName();
      }

      protected String timeout(SubsystemContext<SafetySubsystemModel> context) {
         context.logger().debug("Received timeout for state {} which does not support timeouts", getName());
         return getName();
      }

      protected void setTimeout(int timeoutMs, SubsystemContext<SafetySubsystemModel> context) {
         SubsystemUtils.setTimeout(timeoutMs, context);
      }

      protected void clearTimeout(SubsystemContext<SafetySubsystemModel> context) {
         SubsystemUtils.clearTimeout(context);
      }

      protected boolean hasWarnings(SubsystemContext<SafetySubsystemModel> context) {
         return context.model().getWarnings().size() > 0;
      }

      protected boolean hasTriggers(SubsystemContext<SafetySubsystemModel> context) {
         return !isClear(context);
      }

      protected boolean isClear(SubsystemContext<SafetySubsystemModel> context) {
         return context.model().getTriggers().isEmpty();
      }

      protected boolean readyToFire(SafetySubsystemModel model) {
         Integer deviceCount = model.getAlarmSensitivityDeviceCount();
         int triggers = model.getTriggers().size();
         if(triggers > 0 && (deviceCount == null || deviceCount <= triggers)) {
            return true;
         }
         return false;
      }

   }

   private final State ready = new State(SafetySubsystemCapability.ALARM_READY) {

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#onEnter(com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String onEnter(SubsystemContext<SafetySubsystemModel> context) {
         if(hasWarnings(context)) {
            return SafetySubsystemCapability.ALARM_WARN;
         }
         if(hasTriggers(context)) {
            return SafetySubsystemCapability.ALARM_SOAKING;
         }
         return SafetySubsystemCapability.ALARM_READY;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#addWarning(com.iris.messages.address.Address, java.lang.String, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String addWarning(
            String address,
            String warning,
            SubsystemContext<SafetySubsystemModel> context
      ) {
         super.addWarning(address, warning, context);
         return hasWarnings(context) ? SafetySubsystemCapability.ALARM_WARN : SafetySubsystemCapability.ALARM_READY;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#clearWarning(com.iris.messages.address.Address, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String clearWarning(
            String address,
            SubsystemContext<SafetySubsystemModel> context
      ) {
         super.clearWarning(address, context);
         return hasWarnings(context) ? SafetySubsystemCapability.ALARM_WARN : SafetySubsystemCapability.ALARM_READY;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#triggerAlarm(com.iris.messages.address.Address, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String triggerDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         super.triggerDevice(device, sensor, context);
         return hasTriggers(context) ? SafetySubsystemCapability.ALARM_SOAKING : SafetySubsystemCapability.ALARM_READY;
      }

   };

   private final State warning = new State(SafetySubsystemCapability.ALARM_READY) {

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#triggerAlarm(com.iris.messages.address.Address, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String triggerDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         super.triggerDevice(device, sensor, context);
         return hasTriggers(context) ? SafetySubsystemCapability.ALARM_SOAKING : SafetySubsystemCapability.ALARM_WARN;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#addWarning(com.iris.messages.address.Address, java.lang.String, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String addWarning(
            String address,
            String warning,
            SubsystemContext<SafetySubsystemModel> context
      ) {
         super.addWarning(address, warning, context);
         return hasWarnings(context) ? SafetySubsystemCapability.ALARM_WARN : SafetySubsystemCapability.ALARM_READY;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#clearWarning(com.iris.messages.address.Address, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String clearWarning(
            String address,
            SubsystemContext<SafetySubsystemModel> context
      ) {
         super.clearWarning(address, context);
         return hasWarnings(context) ? SafetySubsystemCapability.ALARM_WARN : SafetySubsystemCapability.ALARM_READY;
      }

   };

   private final State soaking = new State(SafetySubsystemCapability.ALARM_SOAKING) {

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#onEnter(com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String onEnter(SubsystemContext<SafetySubsystemModel> context) {
         if(isClear(context)) {
            return context.model().getPendingClear().isEmpty() ? SafetySubsystemCapability.ALARM_READY : SafetySubsystemCapability.ALARM_CLEARING;
         }

         Integer soakPeriodSec = context.model().getAlarmSensitivitySec();
         if(soakPeriodSec != null && soakPeriodSec > 0) {
            setTimeout(soakPeriodSec * 1000, context);
            return this.getName();
         }
         return readyToFire(context.model()) ?
               SafetySubsystemCapability.ALARM_ALERT :
               SafetySubsystemCapability.ALARM_SOAKING;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#triggerAlarm(com.iris.messages.address.Address, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String triggerDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         super.triggerDevice(device, sensor, context);
         return SafetySubsystemCapability.ALARM_SOAKING;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#timeout(com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      protected String timeout(SubsystemContext<SafetySubsystemModel> context) {
         return readyToFire(context.model()) ?
               SafetySubsystemCapability.ALARM_ALERT :
               SafetySubsystemCapability.ALARM_SOAKING;
      }

   };

   private final State alert = new State(SafetySubsystemCapability.ALARM_ALERT) {

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#onEnter(com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String onEnter(SubsystemContext<SafetySubsystemModel> context) {
         if(isClear(context)) {
            return context.model().getPendingClear().isEmpty() ? SafetySubsystemCapability.ALARM_READY : SafetySubsystemCapability.ALARM_CLEARING;
         }
         context.model().setLastAlertCause(getLastAlertCause(context));
         context.model().setLastAlertTime(new Date());
         // TODO have a special "firing" event
         sendAlert(context);
         return super.onEnter(context);
      }
      
      protected String getLastAlertCause(SubsystemContext<SafetySubsystemModel> context){
         for(Map<String,Object>triggerEvent:context.model().getTriggers()){
            Object eventType = triggerEvent.get(TriggerEvent.ATTR_TYPE);
            if(eventType != null){
               return eventType.toString();
            }
         }
         return null;
      }
      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#onExit(com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public void onExit(SubsystemContext<SafetySubsystemModel> context) {
         super.onExit(context);
         clearAlert(context);
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#triggerAlarm(com.iris.messages.PlatformMessage, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String triggerAlarm(
            PlatformMessage message,
            SubsystemContext<SafetySubsystemModel> context) {
         // already triggered, no-op
         context.logger().info("Ignoring trigger request when alarm is already triggered {}", message);
         return SafetySubsystemCapability.ALARM_ALERT;
      }

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#clearAlarm(com.iris.messages.PlatformMessage, com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String clearAlarm(
            PlatformMessage message,
            SubsystemContext<SafetySubsystemModel> context) {
         context.logger().info("Attempting to clear alarm {}", message);
         context.model().setLastClearedBy(message.getActor().getRepresentation());
         return SafetySubsystemCapability.ALARM_CLEARING;
      }

      @Override
      public String clearDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         List<Map<String,Object>> triggers = new ArrayList<>(context.model().getTriggers());
         TriggerEvent event = removeTriggerEvent(device.getRepresentation(), sensor, triggers);
         String name = super.clearDevice(device, sensor, context);

         // we want to maintain the cleared device in the pending clear until the user has clicked
         // cancel otherwise we'll stay in alert but no know what the trigger was
         Set<String> active = new HashSet<>(context.model().getActiveDevices());
         
         if(event != null && active.contains(device.getRepresentation())) {
            List<Map<String,Object>> pendingClear = new ArrayList<>(context.model().getPendingClear());
            if(addTriggerEvent(event, pendingClear)) {
               context.model().setPendingClear(pendingClear);
            }
         }

         return name;
      }
   };

   private final State clearing = new State(SafetySubsystemCapability.ALARM_CLEARING) {

      /* (non-Javadoc)
       * @see com.iris.common.subsystem.safety.SafetySubsystem.State#onEnter(com.iris.common.subsystem.SubsystemContext)
       */
      @Override
      public String onEnter(SubsystemContext<SafetySubsystemModel> context) {
         List<Map<String,Object>> triggers = new ArrayList<>(context.model().getTriggers());
         List<Map<String,Object>> pendingClear = new ArrayList<>(context.model().getPendingClear());
         boolean pendingChanged = false;
         for(Model m : context.models().getModels()) {
            if(isSafetyDevice(m)) {
               List<TriggerEvent> events = getTriggerEvents(m);
               if(events.isEmpty()) {
                  List<String> sensors = getSensorTypes(m);
                  for(String s : sensors) {
                     removeTriggerEvent(m.getAddress().getRepresentation(), s, triggers);
                     pendingChanged |= removeTriggerEvent(m.getAddress().getRepresentation(), s, pendingClear) != null;
                  }
               }
            }
         }
         for(Map<String,Object> trigger : triggers) {
            pendingChanged |= addTriggerEvent(new TriggerEvent(trigger), pendingClear);
         }

         triggers.clear();
         context.model().setTriggers(triggers);

         if(pendingChanged) {
            context.model().setPendingClear(pendingClear);
         }

         Integer value = context.model().getQuietPeriodSec();
         if(value == null || value.intValue () <= 0) {
            return pendingClear.isEmpty() ? SafetySubsystemCapability.ALARM_READY : SafetySubsystemCapability.ALARM_CLEARING;
         } else {
            setTimeout(value.intValue() * 1000, context);
            return SafetySubsystemCapability.ALARM_CLEARING;
         }
      }

      @Override
      protected String timeout(SubsystemContext<SafetySubsystemModel> context) {
         if(isClear(context)) {
            return context.model().getPendingClear().isEmpty() ? SafetySubsystemCapability.ALARM_READY : SafetySubsystemCapability.ALARM_CLEARING;
         }
         return SafetySubsystemCapability.ALARM_SOAKING;
      }

      @Override
      public String clearDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         String name = super.clearDevice(device, sensor, context);
         Integer value = context.model().getQuietPeriodSec();
         if(value == null || value.intValue () <= 0) {
            return context.model().getPendingClear().isEmpty() ? SafetySubsystemCapability.ALARM_READY : SafetySubsystemCapability.ALARM_CLEARING;
         } else {
            return name;
         }
      }

      @Override
      public String triggerDevice(Address device, String sensor, SubsystemContext<SafetySubsystemModel> context) {
         String name = super.triggerDevice(device, sensor, context);
         Integer value = context.model().getQuietPeriodSec();
         if(value == null || value.intValue () <= 0) {
            return SafetySubsystemCapability.ALARM_SOAKING;
         } else {
            return name;
         }
      }
   };

   // TODO suspended?
   // TODO unsatisfiable?
}

