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
package com.iris.platform.subsystem.incident;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.common.alarm.AlertType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.AlarmUtil;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.hub.HubConnectionModel;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.serv.RuleModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.service.AlarmService;
import com.iris.messages.type.IncidentTrigger;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.util.IrisUUID;

@Singleton
public class PlatformIncidentHistoryListener implements AlarmIncidentHistoryListener {
   private static final Logger logger = LoggerFactory.getLogger(PlatformIncidentHistoryListener.class);
   
   private static final String KEY_CONFIRM = "alarm.confirm";
   private static final String KEY_PANIC = "alarm.panic";
   private static final String KEY_TRIGGER = "alarm.%s";
   private static final String KEY_CANCELLED = "alarm.cancelled";

   private static final String NAME_WATER = "Water Leak";
   private static final String NAME_CO = "Carbon Monoxide";
   
   private static final String METHOD_KEYPAD = "keypad";
   private static final String METHOD_APP = "Iris application";
   
   private final BeanAttributesTransformer<HistoryLogEntry> transformer;
   private final PlatformMessageBus platformBus;
   private final RuleDao ruleDao;
   private final String ruleNamePrefix = "the rule ";
   
   @Inject
   public PlatformIncidentHistoryListener(
         BeanAttributesTransformer<HistoryLogEntry> transformer,
         PlatformMessageBus platformBus,
         RuleDao ruleDao
   ) {
      this.transformer = transformer;
      this.platformBus = platformBus;
      this.ruleDao = ruleDao;
   }
   
   @Override
   public void onHubConnectivityChanged(SubsystemContext<AlarmSubsystemModel> context, Address incidentAddress) {
      Model hub = AlarmUtil.getHubModel(context);
      Model place = SubsystemUtils.getPlace(context);
      if(place == null && hub == null) {
         context.logger().warn("Got a hub connectivity change for a context with no place or hub place id: [{}]", context.getPlaceId(), new RuntimeException("Invalid state for hub connectivity change"));
      }
      else {
         Map<String, Object> history = hubConnectivityToHistory(incidentAddress, hub, PlaceModel.getName(place, ""));
         if(history != null) {
            emitHistoryAdded(context.getPlaceId(), context.getPopulation(), incidentAddress, ImmutableList.of(history));
         }
      }
   }

   @Override
   public void onTriggersAdded(SubsystemContext<?> context, Address incidentAddress, List<IncidentTrigger> triggers) {
      List<Map<String, Object>> events = new ArrayList<>(triggers.size());
      for(IncidentTrigger trigger: triggers) {
         Address curSource = Address.fromString(trigger.getSource());
         Model m = null;
         List<String> historyEntryValues = null;
         boolean found = false;
         if(RuleCapability.NAMESPACE.equals(curSource.getGroup())) {
            RuleDefinition ruleDef = ruleDao.findById((UUID) curSource.getId(), ((PlatformServiceAddress) curSource).getContextQualifier());
            if(ruleDef != null) {
               historyEntryValues = ImmutableList.<String>of(ruleNamePrefix+ruleDef.getName());
               found = true;
            }
         }else {
            m = context.models().getModelByAddress(curSource);      
            if(m != null) {
               historyEntryValues = getValues(m);
               found = true;
            }
         }
         if(!found) {
            logger.warn("Could not load model [{}] -- dropping incident trigger [{}]", trigger.getSource(), trigger);
            continue;
         }
         events.add(triggerToHistory(incidentAddress, trigger, historyEntryValues));
      }
      emitHistoryAdded(context.getPlaceId(), context.getPopulation(), incidentAddress, events);
   }
   
   @Override
   public void onCancelled(SubsystemContext<?> context, AlarmIncident incident, Address cancelledBy, String method) {
      Model m = context.models().getModelByAddress(cancelledBy);
      if(m == null) {
         logger.warn("Could not load model [{}] -- dropping cancelled event", cancelledBy);
         return;
      }

      ImmutableList.Builder<String> builder = ImmutableList.builder();
      builder.addAll(getValues(m));
      builder.add(getAlarmName(incident.getAlert()));
      builder.add(getMethodName(method));

      HistoryLogEntry entry = new HistoryLogEntry();
      entry.setTimestamp(IrisUUID.timeUUID());
      entry.setType(HistoryLogEntryType.DETAILED_ALARM_LOG);
      entry.setMessageKey(KEY_CANCELLED);
      entry.setSubjectAddress(cancelledBy.getRepresentation());
      entry.setValues(builder.build());

      Map<String, Object> attributes = new HashMap<>(transformer.transform(entry));
      attributes.put("values", entry.getValues());
      emitHistoryAdded(context.getPlaceId(), context.getPopulation(), incident.getAddress(), ImmutableList.of(attributes));
   }


   
   private void emitHistoryAdded(UUID placeId, String population, Address incidentAddress, List<Map<String, Object>> events) {
      MessageBody history =
            AlarmIncidentCapability.HistoryAddedEvent
               .builder()
               .withEvents(events)
               .build();
      PlatformMessage message =
            PlatformMessage
               .broadcast()
               .from(incidentAddress)
               .withPlaceId(placeId)
               .withPopulation(population)
               .withPayload(history)
               .create();
      platformBus.send(message);
   }
   
   private Map<String, Object> triggerToHistory(Address incidentAddress, IncidentTrigger trigger, List<String> values) {
         String msgKey = getMessageKey(trigger);
         if(msgKey == null) {
            return null;
         }
         
         HistoryLogEntry entry = new HistoryLogEntry();
         entry.setId(incidentAddress.getId());
         entry.setTimestamp(trigger.getTime().getTime());
         entry.setType(HistoryLogEntryType.DETAILED_ALARM_LOG);
         entry.setMessageKey(msgKey);
         entry.setSubjectAddress(trigger.getSource());
         entry.setValues(values);
         
         Map<String, Object> attributes = new HashMap<>(transformer.transform(entry));
         attributes.put("values", values);
         return attributes;
      }

   private String getMessageKey(IncidentTrigger t) {
      if(IncidentTrigger.EVENT_VERIFIED_ALARM.equals(t.getEvent())) {
         return KEY_CONFIRM;
      }
      if(IncidentTrigger.ALARM_PANIC.equals(t.getAlarm())) {
         return KEY_PANIC;
      }

      return String.format(KEY_TRIGGER, t.getEvent().toLowerCase());
   }

   private Map<String, Object> hubConnectivityToHistory(Address incidentAddress, Model hub, String placeName) {
      String msgKey = getMessageKeyForHubState(HubConnectionModel.getState(hub, ""));
      if(msgKey == null) {
         return null;
      }
      
      List<String> values = Arrays.asList(HubModel.getName(hub, "My Hub"), null, null, null, placeName);
      HistoryLogEntry entry = new HistoryLogEntry();
      entry.setId(incidentAddress.getId());
      entry.setTimestamp(HubConnectionModel.getLastchange(hub, new Date()).getTime());
      entry.setType(HistoryLogEntryType.DETAILED_ALARM_LOG);
      entry.setMessageKey(msgKey);
      entry.setSubjectAddress(hub.getAddress().getRepresentation());
      entry.setValues(values);
      
      Map<String, Object> attributes = new HashMap<>(transformer.transform(entry));
      attributes.put("values", values);
      return attributes;
   }

   private String getMessageKeyForHubState(String state) {
      if(StringUtils.isEmpty(state)) {
         return null;
      }
      
      switch(state) {
      case HubConnectionCapability.STATE_ONLINE:
         return "hub.connection.online";
      case HubConnectionCapability.STATE_OFFLINE:
         return "hub.connection.offline";
      default:
         return null;
      }
   }

   private List<String> getValues(Model m) {
      if(m.getCapabilities().contains(DeviceCapability.NAMESPACE)) {
         return ImmutableList.of(DeviceModel.getName(m));
      }
      if(m.getCapabilities().contains(RuleCapability.NAMESPACE)) {
         return ImmutableList.of(ruleNamePrefix + RuleModel.getName(m));
      }
      if(m.getCapabilities().contains(PersonCapability.NAMESPACE)) {
         return ImmutableList.of(PersonModel.getFirstName(m), PersonModel.getLastName(m));
      }
      return ImmutableList.of();
   }
   
   private String getAlarmName(AlertType alarm) {
      switch(alarm) {
      case WATER:
         return NAME_WATER;
         
      case CO:
         return NAME_CO;
         
      default:
         return StringUtils.capitalize(alarm.name().toLowerCase());
      }
   }

   private String getMethodName(String method) {
      switch (method) {
      case AlarmService.CancelAlertRequest.METHOD_KEYPAD:
         return METHOD_KEYPAD;
         
      default:
         return METHOD_APP;
      }
   }
      
}

