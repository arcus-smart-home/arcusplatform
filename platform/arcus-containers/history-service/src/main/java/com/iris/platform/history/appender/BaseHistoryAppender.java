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
/**
 * 
 */
package com.iris.platform.history.appender;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.CompositeId;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.platform.history.appender.matcher.MatchResults;

/**
 * 
 */
public abstract class BaseHistoryAppender implements HistoryAppender {
   private static final IrisMetricSet METRICS = IrisMetrics.metrics("history.events");
   
   private final HistoryAppenderDAO appender;
   private final ObjectNameCache cache;
   
   public BaseHistoryAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
   	this.appender = appender;
   	this.cache = cache;
   }

   protected abstract MatchResults matches(PlatformMessage message);
   
   protected abstract List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults);

   protected UUID getPlaceIdFromHeader(PlatformMessage message) {
      return UUID.fromString(message.getPlaceId());
   }

   protected UUID getIdFromSource(PlatformMessage message) {
      try {
         return (UUID) message.getSource().getId();
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException("Address source " + message.getSource() + " is not a UUID based service address");
      }
   }
   
   protected String getHubIdFromSource(PlatformMessage message) {
	   try {
		   return message.getSource().getHubId();
	   } 
	   catch (ClassCastException e) {
		   throw new IllegalArgumentException("Address source " + message.getSource() + " is not a hub service address");
	   }
   }
   
   protected String getNameFromAddress(String addressString) {
   	return getNameFromAddress(Address.fromString(addressString));
   }
   
   protected String getNameFromAddress(Address address) {
   	return cache.getName(address);
   }
   
   protected String getDeviceNameFromAddress(String addressString) {
   	return getDeviceNameFromAddress(Address.fromString(addressString));
   }
   
   protected String getDeviceNameFromAddress(Address address) {
   		return cache.getDeviceName(address);
   }
   
   protected String getHubNameFromAddress(Address address) {
	   return cache.getHubName(address);
   }
   
   protected String getPlaceNameFromHeader(PlatformMessage message) {
      UUID placeId = getPlaceIdFromHeader(message);
      Address address = Address.platformService(placeId, PlaceCapability.NAMESPACE);
      return cache.getPlaceName(address);
   }

   protected String getDeviceNameFromSource(PlatformMessage message) {
      return getDeviceNameFromAddress(message.getSource());
   }
   
   protected String getHubNameFromSource(PlatformMessage message) {
	   return getHubNameFromAddress(message.getSource());
   }
   
   protected Address getHubAddress(PlatformMessage message) {
	   return message.getSource();
   }
   
   protected Address getSubjectAddress (PlatformMessage message) {
   	return message.getSource();
   }
   
   protected Address getActorAddress(PlatformMessage message) {
	return message.getActor();
   }

   protected String getActorName(PlatformMessage message) {
      if (message.getActor() != null && message.getActor().getGroup().equals(PersonCapability.NAMESPACE)) {
    	  return cache.getPersonName(message.getActor());
      } else if (message.getActor() != null && message.getActor().getGroup().equals(RuleCapability.NAMESPACE)) {
    	  return cache.getRuleName(message.getActor());
      }	  else if (message.getActor() != null && message.getActor().getGroup().equals(SceneCapability.NAMESPACE)) {
    	  return cache.getSceneName(message.getActor());
      }
      // TODO subsystems
      return "";
   }

   protected String getPersonName(Address personAddress) {
      return cache.getPersonName(personAddress);
   }

   protected String getMethodName(PlatformMessage message) {
      // TODO
      return "";
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.history.appender.HistoryAppender#append(com.iris.messages.PlatformMessage)
    */
   @Override
   public boolean append(PlatformMessage message) {
      MatchResults matchResults = matches(message);
      
   	if(!matchResults.isMatch()) {
         return false;
      }
   	
   	MessageContext context = new MessageContext(
   			message.getTimestamp().getTime(),
   	   	  getPlaceIdFromHeader(message),
   	   	  getPlaceNameFromHeader(message),
   	      getSubjectAddress(message),
   	      getActorAddress(message),
   	      getActorName(message),
   	      getMethodName(message),
   	      cache);
   	
   	
      List<HistoryLogEntry> events = translate(message, context, matchResults);
      if(events != null && events.size() > 0) {
         Set<String> messageKeys = new HashSet<String>(4);
         for (HistoryLogEntry entry : events){
         	appender.appendHistoryEvent(entry);
         	messageKeys.add(entry.getMessageKey());
         }
         messageKeys.stream().forEach((key) -> METRICS.counter(key).inc());
      }
      return true;
   }
   
	static HistoryLogEntry event(long timestamp, Object id, String messageKey, Address subjectAddress, String [] values) {
      HistoryLogEntry event = new HistoryLogEntry();
      event.setTimestamp(timestamp);
      event.setId(id);
      event.setMessageKey(messageKey);
      event.setSubjectAddress(subjectAddress.getRepresentation());
      event.setValues(values);
      return event;
   }
	
	public static HistoryLogEntry detailedPlaceEvent(long timestamp, UUID placeId, String messageKey, Address subjectAddress, String... values) {
      HistoryLogEntry event = event(timestamp, placeId, messageKey, subjectAddress, values);
      event.setType(HistoryLogEntryType.DETAILED_PLACE_LOG);
      return event;
   }

   public static HistoryLogEntry criticalPlaceEvent(long timestamp, UUID placeId, String messageKey, Address subjectAddress, String... values) {
      HistoryLogEntry event = event(timestamp, placeId, messageKey, subjectAddress, values);
      event.setType(HistoryLogEntryType.CRITICAL_PLACE_LOG);
      return event;
   }

   public static HistoryLogEntry detailedDeviceEvent(long timestamp, UUID deviceId, String messageKey, Address subjectAddress, String... values) {
      HistoryLogEntry event = event(timestamp, deviceId, messageKey, subjectAddress, values);
      event.setType(HistoryLogEntryType.DETAILED_DEVICE_LOG);
      return event;
   }

   public static HistoryLogEntry detailedHubEvent(long timestamp, String hubId, String messageKey, Address subjectAddress, String... values) {
	   HistoryLogEntry event = event(timestamp, hubId, messageKey, subjectAddress, values);
	   event.setType(HistoryLogEntryType.DETAILED_HUB_LOG);
	   return event;
   }
   
   public static HistoryLogEntry detailedPersonEvent(long timestamp, UUID personId, String messageKey, Address subjectAddress, String... values) {
      HistoryLogEntry event = event(timestamp, personId, messageKey, subjectAddress, values);
      event.setType(HistoryLogEntryType.DETAILED_PERSON_LOG);
      return event;
   }

   public static HistoryLogEntry detailedRuleEvent(long timestamp, CompositeId<UUID, Integer> ruleId, String messageKey, Address subjectAddress, String... values) {
      HistoryLogEntry event = event(timestamp, ruleId, messageKey, subjectAddress, values);
      event.setType(HistoryLogEntryType.DETAILED_RULE_LOG);
      return event;
   }
   
   public static HistoryLogEntry detailedSubsystemEvent(long timestamp, CompositeId<UUID, String> subsystemId, String messageKey, Address subjectAddress, String... values) {
	  HistoryLogEntry event = event(timestamp, subsystemId, messageKey, subjectAddress, values);
	  event.setType(HistoryLogEntryType.DETAILED_SUBSYSTEM_LOG);
	  return event;
   }

   public static HistoryLogEntry detailedAlarmEvent(long timestamp, UUID incidentId, String messageKey, Address subjectAddress, String... values) {
      HistoryLogEntry event = event(timestamp, incidentId, messageKey, subjectAddress, values);
      event.setType(HistoryLogEntryType.DETAILED_ALARM_LOG);
      return event;
   }

}

