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

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.SubsystemValueChangeAppender;
import com.iris.platform.history.appender.matcher.EnumValueChangeMatcher;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.matcher.Matcher;
import com.iris.platform.history.appender.translator.EntryTemplate;
import com.iris.platform.history.appender.translator.EnumValueChangeTranslator;
import com.iris.platform.history.appender.translator.Translator;

@Singleton
public class SafetySubsystemAppender extends SubsystemValueChangeAppender {

	private enum AlarmType { WATER, SMOKE, CO, GAS };
	
	private static final Matcher matcher = new EnumValueChangeMatcher(
			SafetySubsystemCapability.ATTR_ALARM,
			SafetySubsystemCapability.ALARM_ALERT
	);

	private static final Translator translator = 
			new EnumValueChangeTranslator() {

				@Override
				public List<String> generateValues(PlatformMessage message, MessageContext context, MatchResults matchResults) {
					// could be multiple alarm causes, find the first
					List<Map<String,Object>> triggers = SafetySubsystemCapability.getTriggers(message.getValue());
					Double earliest = null;
					Address addr = null;
					for (Map<String,Object> trigger : triggers) {						
						Object timeObj = trigger.get("time");
						Double time = null;
						if(timeObj instanceof Double) {
							time = (Double)timeObj;
						}else if(timeObj instanceof Long) {
							time = ((Long)timeObj).doubleValue();
						}
						String device = (String)trigger.get("device");
						if(device != null) {
							if (earliest == null) {
								earliest = time;
								addr = Address.fromString(device);
							} else if (time < earliest) {
								addr = Address.fromString(device);
							}
						}
					}
					if(addr != null) {
						String deviceName = context.findName(addr);
						return ImmutableList.of(deviceName, String.valueOf(triggers.size()));
					}else {
						return ImmutableList.<String>of();
					}
				}

				@Override 
				public EntryTemplate selectTemplate(MatchResults matchResults) {
					AlarmType type = getAlarmType(matchResults);
					
					switch (type) {
						case WATER:
							return new EntryTemplate("subsys.safety.alert.water", true);
						case SMOKE:
							return new EntryTemplate("subsys.safety.alert.smoke", true);							
						case CO:
							return new EntryTemplate("subsys.safety.alert.co", true);
						case GAS:
							return new EntryTemplate("subsys.safety.alert.gas", true);
					}
					return super.selectTemplate(matchResults);
				}
				
			}
			;
			
	private static AlarmType getAlarmType(MatchResults matchResults) {
		List<Map<String,Object>> triggers = SafetySubsystemCapability.getTriggers(matchResults.getBody());
		if(triggers != null) {
   		Double earliest = null;
   		AlarmType type = null;
   		for (Map<String,Object> trigger : triggers) {
   			Object timeObj = trigger.get("time");
   			Double time = null;
   			if(timeObj instanceof Double) {
   				time = (Double)timeObj;
   			}else if(timeObj instanceof Long) {
   				time = ((Long)timeObj).doubleValue();
   			}
   			
   			if (earliest == null) {
   				earliest = time;
   				type = AlarmType.valueOf((String)trigger.get("type"));
   			} else if (time < earliest) {
   				type = AlarmType.valueOf((String)trigger.get("type"));
   			}
   		}
		return type;
		}else{
		   return null;
		}
	}
	
	@Inject
	public SafetySubsystemAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
		super(appender, cache, matcher, translator);
	}

	@Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
	   // The appender only handles alerts so there's no need to filter.
	   return super.translate(message, context, matchResults);
   }	
}

