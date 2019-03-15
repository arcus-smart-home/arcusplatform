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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorActionEvent;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertEvent;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.AnnotatedAppender;
import com.iris.platform.history.appender.MessageContext;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.platform.history.appender.annotation.EnumValue;
import com.iris.platform.history.appender.annotation.Event;
import com.iris.platform.history.appender.annotation.Group;
import com.iris.platform.history.appender.matcher.MatchResults;
import com.iris.platform.history.appender.translator.EntryTemplate;
import com.iris.platform.history.appender.translator.Translator;

@Singleton
@Group(CareSubsystemCapability.NAMESPACE)
@Event(event=CareSubsystemCapability.BehaviorAlertEvent.NAME)
@Event(event=CareSubsystemCapability.BehaviorAlertClearedEvent.NAME)
@Event(event=CareSubsystemCapability.BehaviorAlertAcknowledgedEvent.NAME)
@Event(event=CareSubsystemCapability.BehaviorActionEvent.NAME)
@EnumValue(attr=CareSubsystemCapability.ATTR_ALARMMODE, 
           val={CareSubsystemCapability.ALARMMODE_ON, CareSubsystemCapability.ALARMMODE_VISIT})
public class CareSubsystemEventsAppender extends AnnotatedAppender {
	private static final String TEMPLATE_SUBSYS_CARE_CLEAR = "subsys.care.clear";
   private static final String TEMPLATE_SUBSYS_CARE_ALERT = "subsys.care.alert";
   private static final String TEMPLATE_SUBSYS_CARE_READY = "subsys.care.ready";
   private static final String TEMPLATE_SUBSYS_CARE_VISIT = "subsys.care.visit";
   private static final String TEMPLATE_SUBSYS_CARE_ACKNOWLEDGED = "subsys.care.acknowledged";
   private static final String TEMPLATE_SUBSYS_CARE_BEHAVIOR = "subsys.care.behaviorAction";
      
   @Inject
   public CareSubsystemEventsAppender(HistoryAppenderDAO appender, ObjectNameCache cache) {
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
         case CareSubsystemCapability.BehaviorAlertEvent.NAME:
            return new EntryTemplate(TEMPLATE_SUBSYS_CARE_ALERT, true);
         case CareSubsystemCapability.BehaviorAlertClearedEvent.NAME:
            return new EntryTemplate(TEMPLATE_SUBSYS_CARE_CLEAR, true);
         case CareSubsystemCapability.BehaviorAlertAcknowledgedEvent.NAME:
            return new EntryTemplate(TEMPLATE_SUBSYS_CARE_ACKNOWLEDGED, true);
         case CareSubsystemCapability.BehaviorActionEvent.NAME:
            return new EntryTemplate(TEMPLATE_SUBSYS_CARE_BEHAVIOR, true);            
         case Capability.EVENT_VALUE_CHANGE:
         	return CareSubsystemCapability.ALARMMODE_ON.equals(matchResults.getFoundValue()) 
         				? new EntryTemplate(TEMPLATE_SUBSYS_CARE_READY, true)
         	         : new EntryTemplate(TEMPLATE_SUBSYS_CARE_VISIT, true);
               
         default:
            throw new IllegalArgumentException("Invalid message type for match results: " + event.getMessageType());
         }
      }

      @Override
      public List<String> generateValues(PlatformMessage message, MessageContext context, MatchResults matchResults) {
         MessageBody event = message.getValue();
         switch (matchResults.getBody().getMessageType()) {
         case CareSubsystemCapability.BehaviorAlertEvent.NAME:
            String name = BehaviorAlertEvent.getBehaviorName(event);
            return ImmutableList.of(name);
         case CareSubsystemCapability.BehaviorAlertClearedEvent.NAME:
            return ImmutableList.of();
         case CareSubsystemCapability.BehaviorAlertAcknowledgedEvent.NAME:
            return ImmutableList.of();
         case CareSubsystemCapability.BehaviorActionEvent.NAME:
            String action = BehaviorActionEvent.getBehaviorAction(event);
            String actionBehaviorName = BehaviorActionEvent.getBehaviorName(event);
            return ImmutableList.of(actionBehaviorName,"",action.toLowerCase());
         case Capability.EVENT_VALUE_CHANGE:
            return ImmutableList.of();            
         default:
            throw new IllegalArgumentException("Invalid message type for match results.");
         }
      }
   };
}

