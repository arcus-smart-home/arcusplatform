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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability.AlertEvent;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmedEvent;
import com.iris.messages.capability.SecuritySubsystemCapability.DisarmedEvent;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.translator.EntryTemplate;
import com.iris.platform.history.appender.translator.Translator;

@Singleton
@Group(SecuritySubsystemCapability.NAMESPACE)
@Event(event=SecuritySubsystemCapability.ArmedEvent.NAME)
@Event(event=SecuritySubsystemCapability.DisarmedEvent.NAME)
@Event(event=SecuritySubsystemCapability.AlertEvent.NAME)
public class SecuritySubsystemEventsAppender extends AnnotatedAppender {

   private static final String TEMPLATE_SUBSYS_SECURITY_ARMED = "subsys.security.armed";
   private static final String TEMPLATE_SUBSYS_SECURITY_ARMED_BYPASS = "subsys.security.armedbypassed";
   private static final String TEMPLATE_SUBSYS_SECURITY_DISARMED = "subsys.security.disarmed";
   private static final String TEMPLATE_SUBSYS_SECURITY_ALERT = "subsys.security.alert";
   private static final String TEMPLATE_SUBSYS_SECURITY_PANIC = "subsys.security.panic";
      
   @Inject
   public SecuritySubsystemEventsAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
	   super(appender, cache);
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults) {
      List<HistoryLogEntry> entries = translator.generateEntries(message, context, matchResults);
      return entries;
   }

   private static final Translator translator = new Translator() {

      @Override
      protected EntryTemplate selectTemplate(MatchResults matchResults) {
         MessageBody event = matchResults.getBody();
         switch (event.getMessageType()) {
         case SecuritySubsystemCapability.ArmedEvent.NAME:
            Set<String> bypassedDevices = Optional.ofNullable(ArmedEvent.getBypassedDevices(event)).orElse(ImmutableSet.of());
            if(bypassedDevices.size()>0){
               return new EntryTemplate(TEMPLATE_SUBSYS_SECURITY_ARMED_BYPASS, true);
            }
            return new EntryTemplate(TEMPLATE_SUBSYS_SECURITY_ARMED, true);
         
         case SecuritySubsystemCapability.DisarmedEvent.NAME:
            return new EntryTemplate(TEMPLATE_SUBSYS_SECURITY_DISARMED, true);
         
         case SecuritySubsystemCapability.AlertEvent.NAME:
            if(AlertEvent.METHOD_PANIC.equals(AlertEvent.getMethod(event))){
               return new EntryTemplate(TEMPLATE_SUBSYS_SECURITY_PANIC, true);
            }
            else{
               return new EntryTemplate(TEMPLATE_SUBSYS_SECURITY_ALERT, true);
            }
         default:
            throw new IllegalArgumentException("Invalid message type for match results.");
         }
      }

      @Override
      public List<String> generateValues(PlatformMessage message, MessageContext context, MatchResults matchResults) {
         MessageBody event = message.getValue();
         switch (matchResults.getBody().getMessageType()) {
         
         case SecuritySubsystemCapability.ArmedEvent.NAME:
            String mode = ArmedEvent.getAlarmMode(message.getValue());
            Set<String> bypassedDevices = Optional.ofNullable(ArmedEvent.getBypassedDevices(message.getValue())).orElse(ImmutableSet.of());
            Set<String> participatingDevices = ArmedEvent.getParticipatingDevices(message.getValue());
            String armedBy = ArmedEvent.getBy(message.getValue());
            String armedMethod = ArmedEvent.getMethod(message.getValue());
            String armedActor = context.getActorAddress() != null ? context.getActorAddress().getRepresentation() : "";           
            return ImmutableList.of(
                  mode, 
                  String.valueOf(bypassedDevices.size()), 
                  StringUtils.join(participatingDevices, ','),
                  armedMethod, 
                  Optional.ofNullable(armedBy).orElse(""), 
                  armedActor
            );
         
         case SecuritySubsystemCapability.DisarmedEvent.NAME:
             String disarmedBy = DisarmedEvent.getBy(message.getValue());
             String disarmedMethod = DisarmedEvent.getMethod(message.getValue());            
             String disarmedActor = context.getActorAddress() != null ? context.getActorAddress().getRepresentation() : "";        	 
             return ImmutableList.of(disarmedMethod, disarmedBy, disarmedActor);
         
         case SecuritySubsystemCapability.AlertEvent.NAME:
            if(AlertEvent.METHOD_DEVICE.equals(AlertEvent.getMethod(event))){
               Map<String,Date>triggerDevices = Optional.ofNullable(AlertEvent.getTriggers(message.getValue())).orElse(ImmutableMap.of());
               List<Map.Entry<String, Date>>order = triggerDevices.entrySet().stream()
                  .sorted(Map.Entry.<String, Date>comparingByValue().reversed()) 
                  .collect(Collectors.toList());
               return ImmutableList.of(context.findName(Address.fromString(order.get(0).getKey())));
            }
            else{
               //PANIC
               return ImmutableList.of();
            }
         default:
            throw new IllegalArgumentException("Invalid message type for match results.");
         }
      }
   };
}

