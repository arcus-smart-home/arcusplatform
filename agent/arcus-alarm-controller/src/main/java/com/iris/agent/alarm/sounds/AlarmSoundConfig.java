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
package com.iris.agent.alarm.sounds;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.alarm.Alarm;
import com.iris.agent.alarm.AlarmCo;
import com.iris.agent.alarm.AlarmPanic;
import com.iris.agent.alarm.AlarmSecurity;
import com.iris.agent.alarm.AlarmSmoke;
import com.iris.agent.alarm.AlarmWater;
import com.iris.agent.hal.SounderMode;
import com.iris.messages.capability.HubAlarmCapability;

public class AlarmSoundConfig {
	private static final Map<String,SounderMode> triggered = new ImmutableMap.Builder<String,SounderMode>()
			.put(AlarmSecurity.NAME,  SounderMode.SECURITY_ALARM_TRIGGERED)
			.put(AlarmPanic.NAME, 	  SounderMode.PANIC_ALARM)
			.put(AlarmSmoke.NAME,     SounderMode.SMOKE_ALARM_TRIGGERED)
			.put(AlarmCo.NAME, 		  SounderMode.CO_TRIGGERED)
			.put(AlarmWater.NAME,  	  SounderMode.WATER_LEAK_DETECTED)
			// TODO: Care
			.build();

	private static final Map<String,SounderMode> monitored = new ImmutableMap.Builder<String,SounderMode>()
			.put(AlarmSecurity.NAME,  SounderMode.SECURITY_TRIGGERED_MONITORING_NOTIFIED)
			.put(AlarmPanic.NAME, 	  SounderMode.PANIC_TRIGGERED_MONITORING_NOTIFIED)
			.put(AlarmSmoke.NAME,     SounderMode.SMOKE_TRIGGERED_MONITORING_NOTIFIED)
			.put(AlarmCo.NAME, 		  SounderMode.CO_TRIGGERED_MONITORING_NOTIFIED)
			.put(AlarmWater.NAME,  	  SounderMode.WATER_LEAK_DETECTED)		// Not Monitored
			// TODO: Care
			.build();

	
	private static final Map<String,SounderMode> cleared = new ImmutableMap.Builder<String,SounderMode>()
			.put(AlarmSecurity.NAME,  SounderMode.SECURITY_ALARM_OFF)
			.put(AlarmPanic.NAME, 	  SounderMode.PANIC_ALARM_CANCELLED)
			.put(AlarmSmoke.NAME,     SounderMode.SMOKE_ALARM_CANCELLED)
			.put(AlarmCo.NAME, 		  SounderMode.CO_ALARM_CANCELLED)
			.put(AlarmWater.NAME,  	  SounderMode.WATER_LEAK_ALARM_CANCELLED)
			// TODO:  Care
			.build();

	public static SounderMode get(boolean isTriggered, boolean isMonitored, Alarm alarm) {
		if (isTriggered && isMonitored) {
			return monitored.getOrDefault(alarm.getName(),SounderMode.NO_SOUND);
		} else if (isTriggered) {
			return triggered.getOrDefault(alarm.getName(),SounderMode.NO_SOUND);
		} else {
			return cleared.getOrDefault(alarm.getName(), SounderMode.NO_SOUND);
		}
	}
	
	public static SounderMode getMonitored(Alarm alarm) {
		return get(true,true,alarm);
	}

	public static SounderMode getTriggered(Alarm alarm) {
		return get(true,false,alarm);
	}

	public static SounderMode getTriggered(boolean isMonitored, Alarm alarm) {
		return getMonitored(alarm);
	}
	
	public static SounderMode getCleared(Alarm alarm) {
		return get(false,false,alarm);
	}

	// TODO Make this a table lookup, although not too complex, at the moment.
	public static SounderMode getTransition (final String curstate, final String state,Alarm alarm) {
		   // Current State
		   switch (curstate) {
		      case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
		      case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
		        switch (state) {
		         case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
		            return getTriggered(alarm);
		        }
		        break;
		      case HubAlarmCapability.SECURITYALERTSTATE_READY:
		    	  switch (state) {
		    	  	case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
		    	  		return getTriggered(alarm);
               case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
                  return getCleared(alarm);
		    	  }
		    	  break;
		      case HubAlarmCapability.SECURITYALERTSTATE_ARMING:
		    	  switch (state) {
		    	  	case HubAlarmCapability.SECURITYALERTSTATE_READY:
		    	  		return SounderMode.NO_SOUND;  // Armed and Partial gets played by the reflex driver.
		    	  	case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
				    case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
		    	  		return getCleared(alarm);
		    	  }
		    	  break;
		      case HubAlarmCapability.SECURITYALERTSTATE_PREALERT:
		      case HubAlarmCapability.SECURITYALERTSTATE_ALERT:
		      case HubAlarmCapability.SECURITYALERTSTATE_PENDING_CLEAR:
		      case HubAlarmCapability.SECURITYALERTSTATE_CLEARING:		      	
		    	  switch (state) {
		    	  	case HubAlarmCapability.SECURITYALERTSTATE_READY:
		    	  	case HubAlarmCapability.SECURITYALERTSTATE_DISARMED:
				    case HubAlarmCapability.SECURITYALERTSTATE_INACTIVE:
		    	  		return getCleared(alarm);
		    	  }
		    	  break;
		   }
		   return SounderMode.NO_SOUND;
	   }	
}

