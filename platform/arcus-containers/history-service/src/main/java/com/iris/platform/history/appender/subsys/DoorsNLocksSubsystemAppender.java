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
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability;
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
public class DoorsNLocksSubsystemAppender extends BaseHistoryAppender {

   private static final Matcher matcher = new AnyEventMatcher(
      DoorsNLocksSubsystemCapability.PersonAuthorizedEvent.NAME,
      DoorsNLocksSubsystemCapability.PersonDeauthorizedEvent.NAME,
      DoorsNLocksSubsystemCapability.LockJammedEvent.NAME,
      DoorsNLocksSubsystemCapability.LockUnjammedEvent.NAME,
      DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.NAME,
      DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.NAME
   );

   private static final Translator translator = new Translator() {

      @Override
      protected EntryTemplate selectTemplate(MatchResults matchResults) {
         switch(matchResults.getBody().getMessageType()) {
         case DoorsNLocksSubsystemCapability.PersonAuthorizedEvent.NAME:
            return new EntryTemplate("subsys.doorsnlocks.authorized", true);
         case DoorsNLocksSubsystemCapability.PersonDeauthorizedEvent.NAME:
            return new EntryTemplate("subsys.doorsnlocks.deauthorized", true);
         case DoorsNLocksSubsystemCapability.LockJammedEvent.NAME:
            return new EntryTemplate("device.error.doorlock.jam", true);
         case DoorsNLocksSubsystemCapability.LockUnjammedEvent.NAME:
            return new EntryTemplate("device.error.doorlock.jam.cleared", true);
         case DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.NAME:
            return new EntryTemplate("device.error.motdoor.obstruction", true);
         case DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.NAME:
            return new EntryTemplate("device.error.motdoor.obstruction.cleared", true);
         default:
            throw new IllegalArgumentException("Invalid message type for match results.");
         }
      }

      @Override
      public List<String> generateValues(PlatformMessage message, MessageContext context, MatchResults matchResults) {
         String personName = getPersonName(context,  matchResults.getBody());
         if(personName == null) {
            return ImmutableList.of();
         }
         return ImmutableList.of(personName);
      }
   };

   @Inject
   public DoorsNLocksSubsystemAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
	   super(appender, cache);
   }

	@Override
   protected MatchResults matches(PlatformMessage message) {
      boolean match = ((String)message.getSource().getGroup()).equals(DoorsNLocksSubsystemCapability.NAMESPACE);
      if (!match) {
         return MatchResults.FALSE;
      }
      return matcher.matches(message.getValue());
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
      context.setDeviceName(getDeviceName(context, matchResults.getBody()));
      context.setDeviceId(getDeviceId(matchResults.getBody()));
      return translator.generateEntries(message, context, matchResults);
   }

   @Override
   protected Address getSubjectAddress(PlatformMessage message) {
      // spoof the device as the subject to ensure entries are added to the device detailed logs
      MessageBody body = message.getValue();
      switch(body.getMessageType()) {
         case DoorsNLocksSubsystemCapability.LockJammedEvent.NAME:
            return Address.fromString(DoorsNLocksSubsystemCapability.LockJammedEvent.getLock(body));
         case DoorsNLocksSubsystemCapability.LockUnjammedEvent.NAME:
            return Address.fromString(DoorsNLocksSubsystemCapability.LockUnjammedEvent.getLock(body));
         case DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.NAME:
            return Address.fromString(DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.getDoor(body));
         case DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.NAME:
            return Address.fromString(DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.getDoor(body));
         default:
            return super.getSubjectAddress(message);
      }
   }

   private static String getPersonName(MessageContext context, MessageBody body) {
      switch(body.getMessageType()) {
      case DoorsNLocksSubsystemCapability.PersonAuthorizedEvent.NAME:
         return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.PersonAuthorizedEvent.getPerson(body)));
      case DoorsNLocksSubsystemCapability.PersonDeauthorizedEvent.NAME:
         return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.PersonDeauthorizedEvent.getPerson(body)));
      default: return null;
      }
   }

   private static String getDeviceName(MessageContext context, MessageBody body) {
     switch(body.getMessageType()) {
     case DoorsNLocksSubsystemCapability.PersonAuthorizedEvent.NAME:
        return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.PersonAuthorizedEvent.getLock(body)));
     case DoorsNLocksSubsystemCapability.PersonDeauthorizedEvent.NAME:
        return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.PersonDeauthorizedEvent.getLock(body)));
     case DoorsNLocksSubsystemCapability.LockJammedEvent.NAME:
        return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.LockJammedEvent.getLock(body)));
     case DoorsNLocksSubsystemCapability.LockUnjammedEvent.NAME:
        return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.LockUnjammedEvent.getLock(body)));
     case DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.NAME:
        return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.getDoor(body)));
     case DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.NAME:
        return context.findName(Address.fromString(DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.getDoor(body)));
     default: return null;
     }
   }

   private static UUID getDeviceId(MessageBody body) {
      switch(body.getMessageType()) {
         case DoorsNLocksSubsystemCapability.LockJammedEvent.NAME:
            return deviceIdFromAddress(DoorsNLocksSubsystemCapability.LockJammedEvent.getLock(body));
         case DoorsNLocksSubsystemCapability.LockUnjammedEvent.NAME:
            return deviceIdFromAddress(DoorsNLocksSubsystemCapability.LockUnjammedEvent.getLock(body));
         case DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.NAME:
            return deviceIdFromAddress(DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.getDoor(body));
         case DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.NAME:
            return deviceIdFromAddress(DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.getDoor(body));
         default:
            return null;
      }
   }

   private static UUID deviceIdFromAddress(String address) {
      Address addr = Address.fromString(address);
      if(addr instanceof DeviceDriverAddress) {
         return ((DeviceDriverAddress) addr).getId();
      }
      return null;
   }
}

