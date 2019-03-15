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
package com.iris.platform.history.appender.subsys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.AutoTranslate;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.annotation.Values;
import com.iris.messages.capability.LawnNGardenSubsystemCapability;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.StartWateringEvent;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.StopWateringEvent;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.SkipWateringEvent;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.UpdateScheduleEvent;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.ApplyScheduleToDeviceEvent;
import com.iris.messages.capability.LawnNGardenSubsystemCapability.ApplyScheduleToDeviceFailedEvent;

@Singleton
@AutoTranslate()
@Group(LawnNGardenSubsystemCapability.NAMESPACE)
@Event(event=StartWateringEvent.NAME, tpl="subsys.lawnngarden.zonestart", critical=true )
@Event(event=StopWateringEvent.NAME, tpl="subsys.lawnngarden.zonestop", critical=true )
@Event(event=SkipWateringEvent.NAME, tpl="subsys.lawnngarden.skipirrigation", critical=true )
@Event(event=UpdateScheduleEvent.NAME, tpl="subsys.lawnngarden.scheduleupdated", critical=true )
@Event(event=ApplyScheduleToDeviceEvent.NAME, tpl="subsys.lawnngarden.scheduleapplied", critical=true )
@Event(event=ApplyScheduleToDeviceFailedEvent.NAME, tpl="subsys.lawnngarden.schedulefailed", critical=true )
@Values({LawnNGardenSubsystemAppender.CONTROLLER_NAME, 
	LawnNGardenSubsystemAppender.ZONE, 
	LawnNGardenSubsystemAppender.SCHEDULE_TYPE, 
	LawnNGardenSubsystemAppender.TIME,
	LawnNGardenSubsystemAppender.ZONE_NAME})
public class LawnNGardenSubsystemAppender extends AnnotatedAppender {
	public final static String CONTROLLER_NAME = "controller_name";
	public final static String SCHEDULE_TYPE = "schedule_type";
	public final static String TIME = "time";
	public final static String ZONE = "zone";
	public final static String ZONE_NAME = "zone_name";
	
	@Override
   protected void init() {
	   super.init();
	   registerGetter(CONTROLLER_NAME, 
	   		StartWateringEvent.NAME, 
	   		(msg, cxt, results) -> getDeviceNameFromAddress(StartWateringEvent.getController(msg.getValue())));
	   registerGetter(CONTROLLER_NAME, 
	   		StopWateringEvent.NAME, 
	   		(msg, cxt, results) -> getDeviceNameFromAddress(StopWateringEvent.getController(msg.getValue())));
	   registerGetter(CONTROLLER_NAME, 
	   		ApplyScheduleToDeviceEvent.NAME, 
	   		(msg, cxt, results) -> getDeviceNameFromAddress(ApplyScheduleToDeviceEvent.getController(msg.getValue())));
	   registerGetter(CONTROLLER_NAME, 
	   		ApplyScheduleToDeviceFailedEvent.NAME, 
	   		(msg, cxt, results) -> getDeviceNameFromAddress(ApplyScheduleToDeviceFailedEvent.getController(msg.getValue())));
	   registerGetter(CONTROLLER_NAME,
	   		SkipWateringEvent.NAME,
	   		(msg, cxt, results) -> getDeviceNameFromAddress(SkipWateringEvent.getController(msg.getValue()))); 
	   
	   registerGetter(ZONE,
	   		StartWateringEvent.NAME,
	   		(msg, cxt, results) -> StartWateringEvent.getZone(msg.getValue()));
	   registerGetter(ZONE,
	   		StopWateringEvent.NAME,
	   		(msg, cxt, results) -> StopWateringEvent.getZone(msg.getValue()));
	   
	   registerGetter(ZONE_NAME,
            StartWateringEvent.NAME,
            (msg, cxt, results) -> StartWateringEvent.getZoneName(msg.getValue()));
      registerGetter(ZONE_NAME,
            StopWateringEvent.NAME,
            (msg, cxt, results) -> StopWateringEvent.getZoneName(msg.getValue()));
	   
	   registerGetter(SCHEDULE_TYPE,
	   		UpdateScheduleEvent.NAME,
	   		(msg, cxt, results) -> UpdateScheduleEvent.getMode(msg.getValue()));
	   
	   registerGetter(TIME,
	   		SkipWateringEvent.NAME,
	   		(msg, cxt, results) -> String.valueOf(SkipWateringEvent.getHours(msg.getValue()))); 
   }

	@Inject
	public LawnNGardenSubsystemAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
	   super(appender, cache);
   }
}

