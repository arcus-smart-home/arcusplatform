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
package com.iris.common.subsystem.care.behavior.evaluators;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.behavior.BehaviorMonitor;
import com.iris.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.iris.common.subsystem.care.behavior.SubsystemVariableKey;
import com.iris.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.TimeWindow;
import com.iris.util.TypeMarker;

public abstract class BaseBehaviorEvaluator implements BehaviorEvaluator{

   public final static SubsystemVariableKey BEHAVIOR_DEVICE_EXCLUSION_KEY = new SubsystemVariableKey("behaviorDeviceExclusion");

   public enum BEHAVIOR_TYPE {INACTIVITY, PRESENCE, TEMPURATURE, OPEN, OPEN_COUNT}
   
   @Override
   public String getBehaviorId() {
      return getCareBehavior().getId();
   }

   
   @Override
   public WeeklyTimeWindow getNextTimeWindow(Date asOfDate,TimeZone tz){
      return WeeklyTimeWindow.nextOrCurrentWindow(getWeeklyTimeWindows(), asOfDate, tz);
   }

   public boolean inTimeWindow(List<WeeklyTimeWindow>windows,Date date,SubsystemContext<CareSubsystemModel>context){
      WeeklyTimeWindow window = locateTimeWindow(windows, date, context);
      return window==null?false:true;
   }
   
   @Override
   public void onWindowStart(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      // no op
   }
   
   @Override
   public void onWindowEnd(WeeklyTimeWindow window, SubsystemContext<CareSubsystemModel> context) {
      // no op
   }
   
   @Override
   public void onModelChange(ModelChangedEvent event, SubsystemContext<CareSubsystemModel> context) {
      // no op
   }
   
   

   @Override
	public void onModelRemove(ModelRemovedEvent event, SubsystemContext<CareSubsystemModel> context) {
   	boolean updated = false;
   	CareBehaviorTypeWrapper curBehavior = getCareBehavior();
   	String addressStr = event.getAddress().getRepresentation();
   	Set<String> devices = curBehavior.getDevices();
   	if(devices != null && devices.size() > 0) {  		
   		if(devices.remove(addressStr)) {
   			curBehavior.setDevices(new ArrayList<>(devices));
   			updated = true;
   		}
   	}
   	Set<String> availableDevices = curBehavior.getAvailableDevices();
   	if(availableDevices != null && availableDevices.size() > 0) {
   		if(availableDevices.remove(addressStr)) {
   			curBehavior.setAvailbleDevices(new ArrayList<>(availableDevices));
   			updated = true;
   		}
   	}
   	if(updated) {
   		context.setVariable(BehaviorMonitor.BEHAVIOR_KEY.create(curBehavior.getId()), curBehavior.toMap());
   	}
		
	}


	@Override
   public void onAlarmCleared(SubsystemContext<CareSubsystemModel> context) {
      // no op
   }

   @Override
   public void onTimeout(ScheduledEvent event,SubsystemContext<CareSubsystemModel> context) {
      // no op
   }

   @Override
   public void onAlarmModeChange(SubsystemContext<CareSubsystemModel> context) {
      // no op
   }

   @Override
   public void onStart(SubsystemContext<CareSubsystemModel> context) {
      // no op
   }

   @Override
   public void validateConfig(SubsystemContext<CareSubsystemModel> context) {
      //no op
   }
   
   @Override
   public void onRemoved(SubsystemContext<CareSubsystemModel> context) {

   }
   
   protected void forEachBehaviorDevice(SubsystemContext<CareSubsystemModel>context,ContextModelConsumer consumer){
      forEachBehaviorDevice(context, Predicates.<Model>alwaysTrue(),consumer);
   }
   protected void forEachBehaviorDevice(SubsystemContext<CareSubsystemModel>context,Predicate<Model>filter,ContextModelConsumer consumer){
      for (String device : getCareBehavior().getDevices()){
         Model model = context.models().getModelByAddress(Address.fromString(device));
         try{
            consumer.accept(context, model);
         }
         catch(Exception e){
            context.logger().warn("Unknown Exception thrown for model [{}]",model.getAddress());
         }
      }   
   }


   public WeeklyTimeWindow locateTimeWindow(List<WeeklyTimeWindow>windows,Date date,SubsystemContext<CareSubsystemModel>context){
      for(WeeklyTimeWindow wtw:windows){
         if(wtw.isDateInTimeWindow(date,context.getLocalTime().getTimeZone())){
            return wtw;
         }
      }
      return null;
   }
   protected void scheduleMonitorAlertTimeout(long timeout,SubsystemContext<CareSubsystemModel>context){
      Date timeoutDate = new Date(context.getLocalTime().getTimeInMillis()+timeout);
      context.logger().debug("scheduling a new alert timeout at {}",timeoutDate);
      SubsystemUtils.setTimeout(timeoutDate, context,BehaviorMonitor.ALERT_KEY.create(getCareBehavior().getId()));
   }
   
   protected void scheduleWindowStart(long timeout,SubsystemContext<CareSubsystemModel>context){
      Date timeoutDate = new Date(context.getLocalTime().getTimeInMillis()+timeout);
      context.logger().debug("scheduling a new window end timeout at {} for behavior {}",timeoutDate);
      SubsystemUtils.setTimeout(timeoutDate, context,BehaviorMonitor.WINDOW_START_KEY.create(getCareBehavior().getId()));
   }

   protected void scheduleWindowEnd(long timeout,SubsystemContext<CareSubsystemModel>context){
      context.logger().debug("scheduling a new window end timeout at {} for behavior {}",new Date(System.currentTimeMillis()+timeout),getCareBehavior());
      scheduleWindowEnd(new Date(context.getLocalTime().getTimeInMillis()+timeout), context);
   }
   
   protected void scheduleWindowEnd(Date date,SubsystemContext<CareSubsystemModel>context){
      SubsystemUtils.setTimeout(date, context,BehaviorMonitor.WINDOW_END_KEY.create(getCareBehavior().getId()));
   }

   protected void addToLastTriggeredDevice(String address,SubsystemContext<CareSubsystemModel>context){
      Map<String,Date>lastAlertDevices=new HashMap<String,Date>();
      lastAlertDevices.put(address, context.getLocalTime().getTime());
      //TODO: Do something with these.  put in var and give the alert message.
   }
   
   protected void clearAlertTimeout(SubsystemContext<CareSubsystemModel>context){
      SubsystemUtils.clearTimeout(context,BehaviorMonitor.ALERT_KEY.create(getCareBehavior().getId()));
   }
   
   protected List<WeeklyTimeWindow>convertTimeWindows(List<Map<String,Object>>windows){
      List<WeeklyTimeWindow>careWindows=new ArrayList<WeeklyTimeWindow>();
      if(windows!=null){
         for(Map<String,Object>window:windows){
            TimeWindow timeWindow = new TimeWindow(window);
            careWindows.add(new WeeklyTimeWindow(DayOfWeek.valueOf(timeWindow.getDay()),
                  TimeOfDay.fromString(timeWindow.getStartTime()),
                  timeWindow.getDurationSecs()==null?0:timeWindow.getDurationSecs(),
                  TimeUnit.SECONDS));
         }
      }
      return careWindows;
   }
   
   protected long dateDiffMilli(Date date1,Date date2){
      return date1.getTime()-date2.getTime();
   }
   
   protected Date max(Date date1,Date date2){
      return date1.after(date2)?date1:date2;
   }
   
   @Override
   public List<WeeklyTimeWindow> getWeeklyTimeWindows() {
      return convertTimeWindows(getCareBehavior().getTimeWindows());
   }
   protected void clearVar(String variableName,SubsystemContext<CareSubsystemModel> context){
      context.setVariable(variableName, null);
   }
   //Exclusion lists are used to keep specific devices from re-triggering the alarm until the window is reset,
   //they are re-enabled or the enter back into some state or threshold
   //they methods below just manage those lists
   protected void addToAlertExclusionList(Address address, SubsystemContext<CareSubsystemModel> context) {
      SubsystemUtils.addStringToVariableList(address.getRepresentation(), BEHAVIOR_DEVICE_EXCLUSION_KEY.create(getBehaviorId()), context);
   }

   protected boolean removeFromExclusionList(Address address, SubsystemContext<CareSubsystemModel> context) {
      return SubsystemUtils.removeStringFromVariableList(address.getRepresentation(), BEHAVIOR_DEVICE_EXCLUSION_KEY.create(getBehaviorId()), context);
   }

   protected boolean existsInExclusionList(Address address, SubsystemContext<CareSubsystemModel> context) {
      List<String> exclusion = context.getVariable(BEHAVIOR_DEVICE_EXCLUSION_KEY.create(getBehaviorId())).as(TypeMarker.listOf(String.class), new ArrayList<String>());
      return exclusion.contains(address.getRepresentation());
   }
   
   protected void clearExclusionList(SubsystemContext<CareSubsystemModel> context) {
      context.setVariable(BEHAVIOR_DEVICE_EXCLUSION_KEY.create(getBehaviorId()),new ArrayList<String>());
   }
   
   protected void removeExclusionList(SubsystemContext<CareSubsystemModel> context) {
      context.setVariable(BEHAVIOR_DEVICE_EXCLUSION_KEY.create(getBehaviorId()),null);
   }

   
}

