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
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PresenceSubsystemCapability;
import com.iris.messages.capability.PresenceSubsystemCapability.ArrivedEvent;
import com.iris.messages.capability.PresenceSubsystemCapability.DepartedEvent;
import com.iris.messages.capability.PresenceSubsystemCapability.DeviceAssignedToPersonEvent;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.BaseHistoryAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.matcher.AnyEventMatcher;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.matcher.Matcher;
import com.iris.platform.history.appender.translator.EntryTemplate;
import com.iris.platform.history.appender.translator.Translator;

@Singleton
public class PresenceSubsystemAppender extends BaseHistoryAppender {
   private static final String SUBSYS_PRESENCE_PERSON_ARRIVED = "subsys.presence.personArrived";
   private static final String SUBSYS_PRESENCE_PERSON_DEPARTED = "subsys.presence.personDeparted";
   private static final String SUBSYS_PRESENCE_DEVICE_ARRIVED = "subsys.presence.deviceArrived";
   private static final String SUBSYS_PRESENCE_DEVICE_DEPARTED = "subsys.presence.deviceDeparted";
   private static final String SUBSYS_PRESENCE_DEVICE_UNASSIGNED_FROM_PERSON = "subsys.presence.deviceUnassignedFromPerson";
   private static final String SUBSYS_PRESENCE_DEVICE_ASSIGNED_TO_PERSON = "subsys.presence.deviceAssignedToPerson";

   private static final String[] STRING_ARRAY = new String[0];
   
   private static final Matcher matcher = new AnyEventMatcher(
		 PresenceSubsystemCapability.ArrivedEvent.NAME,
		 PresenceSubsystemCapability.DepartedEvent.NAME,
         PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME,
         PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.NAME);

   private static final Translator translator = new Translator() {

      @Override
      protected EntryTemplate selectTemplate(MatchResults matchResults) {
         switch(matchResults.getBody().getMessageType()) {
         case PresenceSubsystemCapability.ArrivedEvent.NAME:
        	 if (matchResults.getBody().getAttributes().get(PresenceSubsystemCapability.ArrivedEvent.ATTR_TYPE).equals(PresenceSubsystemCapability.ArrivedEvent.TYPE_PERSON)) {
        		 return new EntryTemplate(SUBSYS_PRESENCE_PERSON_ARRIVED, true);
        	 } else {
        		 return new EntryTemplate(SUBSYS_PRESENCE_DEVICE_ARRIVED, true);
        	 }
         case PresenceSubsystemCapability.DepartedEvent.NAME:
        	 if (matchResults.getBody().getAttributes().get(PresenceSubsystemCapability.DepartedEvent.ATTR_TYPE).equals(PresenceSubsystemCapability.DepartedEvent.TYPE_PERSON)) {
        		 return new EntryTemplate(SUBSYS_PRESENCE_PERSON_DEPARTED, true);
        	 } else {
        		 return new EntryTemplate(SUBSYS_PRESENCE_DEVICE_DEPARTED, true);
        	 }
         case PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME:
            return new EntryTemplate(SUBSYS_PRESENCE_DEVICE_ASSIGNED_TO_PERSON, true);
         case PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.NAME:
            return new EntryTemplate(SUBSYS_PRESENCE_DEVICE_UNASSIGNED_FROM_PERSON, true);
         default:
            throw new IllegalArgumentException("Invalid message type for match results.");
         }
      }

      @Override
      public List<String> generateValues(PlatformMessage message, MessageContext context, MatchResults matchResults) {
         String personName = getPersonName(context,  matchResults.getBody());
         if(personName==null){
            return ImmutableList.of();
         }
         return ImmutableList.of(personName);
      }
   };

   @Inject
   public PresenceSubsystemAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
	   super(appender, cache);
   }

	@Override
   protected MatchResults matches(PlatformMessage message) {
      boolean match = ((String)message.getSource().getGroup()).equals(PresenceSubsystemCapability.NAMESPACE);
      if (!match) {
         return MatchResults.FALSE;
      }
      return matcher.matches(message.getValue());
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
      context.setDeviceName(getDeviceName(context, matchResults.getBody()));
      List<HistoryLogEntry>entries=translator.generateEntries(message, context, matchResults);

      String[] values=entries.get(0).getValues().toArray(STRING_ARRAY);
      
      if(message.getMessageType().equals(PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME) ||
         message.getMessageType().equals(PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.NAME)) {
         Address person = Address.fromString(DeviceAssignedToPersonEvent.getPerson(message.getValue()));
         Address device = Address.fromString(DeviceAssignedToPersonEvent.getDevice(message.getValue()));
         String key = message.getMessageType().equals(PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME)?SUBSYS_PRESENCE_DEVICE_ASSIGNED_TO_PERSON:SUBSYS_PRESENCE_DEVICE_UNASSIGNED_FROM_PERSON;
         entries.add(detailedPersonEvent(context.getTimestamp(), (UUID)person.getId(), key, person, values));
         entries.add(detailedDeviceEvent(context.getTimestamp(), (UUID)device.getId(), key, device, values));
      }
      
      
      if (message.getMessageType().equals(PresenceSubsystemCapability.ArrivedEvent.NAME)) {
         Address target = Address.fromString(ArrivedEvent.getTarget(message.getValue()));
         Address device = Address.fromString(ArrivedEvent.getDevice(message.getValue()));
         String key = SUBSYS_PRESENCE_DEVICE_ARRIVED;
         if (DepartedEvent.TYPE_PERSON.equals(DepartedEvent.getType(message.getValue()))) {
            // a person has arrived. Note: When a person arrives, the device also arrives.
            key = SUBSYS_PRESENCE_PERSON_ARRIVED;
            entries.add(detailedPersonEvent(context.getTimestamp(), (UUID) target.getId(), key, target, values));
         }
         // the device has arrived.
         entries.add(detailedDeviceEvent(context.getTimestamp(), (UUID) device.getId(), key, device, values));
      }

      if (message.getMessageType().equals(PresenceSubsystemCapability.DepartedEvent.NAME)) {
         Address target = Address.fromString(DepartedEvent.getTarget(message.getValue()));
         Address device = Address.fromString(DepartedEvent.getDevice(message.getValue()));
         String key = SUBSYS_PRESENCE_DEVICE_DEPARTED;
         if (DepartedEvent.TYPE_PERSON.equals(DepartedEvent.getType(message.getValue()))) {
            // a person has departed. Note: When a person departs, the device also departs.
            key = SUBSYS_PRESENCE_PERSON_DEPARTED;
            entries.add(detailedPersonEvent(context.getTimestamp(), (UUID) target.getId(), key, target, values));
         }
         // the device has departed.
         entries.add(detailedDeviceEvent(context.getTimestamp(), (UUID) device.getId(), key, device, values));
      }

      return entries;
   }
   
   private static String getPersonName(MessageContext context, MessageBody body) {
      switch(body.getMessageType()) {
         case PresenceSubsystemCapability.ArrivedEvent.NAME:
        	 if (body.getAttributes().get(PresenceSubsystemCapability.DepartedEvent.ATTR_TYPE).equals(PresenceSubsystemCapability.ArrivedEvent.TYPE_PERSON))
        		 return context.findName(Address.fromString(PresenceSubsystemCapability.ArrivedEvent.getTarget(body)));
        	 else
        		 return null;
         case PresenceSubsystemCapability.DepartedEvent.NAME:
        	 if (body.getAttributes().get(PresenceSubsystemCapability.DepartedEvent.ATTR_TYPE).equals(PresenceSubsystemCapability.DepartedEvent.TYPE_PERSON))
        		 return context.findName(Address.fromString(PresenceSubsystemCapability.DepartedEvent.getTarget(body)));
        	 else
        		 return null;
         case PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME:
            return context.findName(Address.fromString(PresenceSubsystemCapability.DeviceAssignedToPersonEvent.getPerson(body)));
         case PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.NAME:
            return context.findName(Address.fromString(PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.getPerson(body)));
         default: return null;
      }
   }
   
   private static String getDeviceName(MessageContext context, MessageBody body) {
     switch(body.getMessageType()) {
        case PresenceSubsystemCapability.ArrivedEvent.NAME:
           return context.findName(Address.fromString(PresenceSubsystemCapability.ArrivedEvent.getDevice(body)));
        case PresenceSubsystemCapability.DepartedEvent.NAME:
           return context.findName(Address.fromString(PresenceSubsystemCapability.DepartedEvent.getDevice(body)));
        case PresenceSubsystemCapability.DeviceAssignedToPersonEvent.NAME:
           return context.findName(Address.fromString(PresenceSubsystemCapability.DeviceAssignedToPersonEvent.getDevice(body)));
        case PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.NAME:
           return context.findName(Address.fromString(PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent.getDevice(body)));
        default: return null;
     }
   }
}

