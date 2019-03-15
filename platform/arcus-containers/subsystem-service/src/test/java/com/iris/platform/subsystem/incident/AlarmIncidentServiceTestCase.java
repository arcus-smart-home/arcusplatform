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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.common.alarm.AlertType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmIncidentCapability.COAlertEvent;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.service.AlarmService;
import com.iris.messages.service.AlarmService.AddAlarmRequest;
import com.iris.messages.type.IncidentTrigger;
import com.iris.messages.type.Population;
import com.iris.messages.type.TrackerEvent;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncident.AlertState;
import com.iris.platform.alarm.incident.AlarmIncident.MonitoringState;
import com.iris.platform.alarm.incident.AlarmIncident.TrackerState;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.subsystem.SubsystemRegistry;
import com.iris.platform.subsystem.impl.PlatformSubsystemContext;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.util.IrisCollections;

@Mocks({ AlarmIncidentDAO.class, AlarmIncidentHistoryListener.class, SubsystemRegistry.class, PlacePopulationCacheManager.class })
@Modules(InMemoryMessageModule.class)
public class AlarmIncidentServiceTestCase extends IrisMockTestCase {

   protected SubsystemContext<AlarmSubsystemModel> context;
   protected SimpleModelStore models = new SimpleModelStore();
   protected UUID placeId;
   protected Address cancelledBy = Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE);
   protected ServiceLevel serviceLevel;
   
   @Inject
   protected InMemoryPlatformMessageBus bus;
   @Inject
   protected AlarmIncidentDAO incidentDao;
   @Inject
   protected AlarmIncidentHistoryListener incidentHistory;
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;

   public AlarmIncidentServiceTestCase(String serviceLevel) {
      this.serviceLevel = ServiceLevel.valueOf(serviceLevel);
   }

   @Before
   public void createSubsystemContext() throws Exception {
      Map<String, Object> place = ModelFixtures.createPlaceAttributes();
      place.put(PlaceCapability.ATTR_SERVICELEVEL, serviceLevel.name());      
      models.addModel(place);
      placeId = UUID.fromString((String)place.get(Capability.ATTR_ID));
      context =
            PlatformSubsystemContext
               .builder()
               .withAccountId(UUID.randomUUID())
               .withPlaceId(placeId)
               .withPopulation(Population.NAME_GENERAL)
               .withLogger(LoggerFactory.getLogger(TestPlatformAlarmIncidentService.class))
               .withPlatformBus(bus)
               .withModels(models)
               .build(AlarmSubsystemModel.class, new ModelEntity(ModelFixtures.buildSubsystemAttributes(placeId, AlarmSubsystemCapability.NAMESPACE).create()));
      
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }

   protected void expectCurrentAndReturnNull() {
      expectCurrentAndReturn(null);
   }

   protected void expectCurrentAndReturn(AlarmIncident incident) {
      EasyMock
         .expect(incidentDao.current(placeId))
         .andReturn(incident);
   }

   protected void expectGetActiveAndReturnNull(UUID incidentId) {
      expectFindByAndReturnNull(incidentId);
   }

   protected void expectGetActiveAndReturn(AlarmIncident incident) {
      expectFindByAndReturn(incident);
   }

   protected void expectFindByAndReturnNull(UUID incidentId) {
      EasyMock
         .expect(incidentDao.findById(placeId, incidentId))
         .andReturn(null);
   }

   protected void expectFindByAndReturn(AlarmIncident incident) {
      EasyMock
         .expect(incidentDao.findById(placeId, incident.getId()))
         .andReturn(incident);
   }

   protected void expectFindByAndReturn(UUID incidentId, Capture<AlarmIncident> incident) {
      EasyMock
         .expect(incidentDao.findById(placeId, incidentId))
         .andAnswer(() -> incident.getValue());
   }

   protected Capture<AlarmIncident> expectUpdate() {
      Capture<AlarmIncident> captor = EasyMock.newCapture();
      incidentDao.upsert(EasyMock.capture(captor));
      EasyMock.expectLastCall();
      return captor;
   }

   protected List<Capture<AlarmIncident>> expectUpdate(int times) {
      ArrayList<Capture<AlarmIncident>> list = new ArrayList<Capture<AlarmIncident>>(times);
      for(int i=0; i<times; i++) {
         Capture<AlarmIncident> captor = EasyMock.newCapture();
         incidentDao.upsert(EasyMock.capture(captor));
         EasyMock.expectLastCall();
         list.add(captor);
      }     
      return list;
   }

   protected void expectTriggersAdded(AlarmIncident incident, List<IncidentTrigger> triggers) {
      incidentHistory.onTriggersAdded(context, incident.getAddress(), triggers);
      EasyMock.expectLastCall();
   }

   protected void expectCancelling(AlarmIncident incident, Address cancelledBy, String method) {
      incidentHistory.onCancelled(context, incident, cancelledBy, method);
      EasyMock.expectLastCall();
   }

   protected void assertBroadcastAdded(AlarmIncident incident) {
      PlatformMessage message = bus.poll();
      assertEquals(Capability.EVENT_ADDED, message.getMessageType());
      assertAlarmIncidentBroadcaseValues(incident, message.getValue());
   }

   protected void assertBroadcastChanged(AlarmIncident incident) {
      PlatformMessage message = bus.poll();
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
   }

   protected void assertAlarmIncidentBroadcaseValues(AlarmIncident incident, MessageBody body) {
      //assertEquals(incident.getMonitoringState().name(), AlarmIncidentCapability.getMonitoringState(body) );
      assertEquals(incident.isMonitored(), AlarmIncidentCapability.getMonitored(body));   
   }

   protected void assertBroadcastAlert(AlertType alert, List<IncidentTrigger> triggers) {
      String eventType = AlarmIncident.toEvent(alert);
      
      PlatformMessage message = bus.poll();
      assertEquals(eventType, message.getMessageType());
      Object expectedTriggers = JSON.fromJson(JSON.toJson(IrisCollections.transform(triggers, IncidentTrigger::toMap)), Object.class);
      assertEquals(expectedTriggers, COAlertEvent.getTriggers(message.getValue()));
   }

   protected void assertAddAlarm(AlertType type) {
      PlatformMessage message = bus.poll();
      assertNotNull(message);
      assertEquals(AlarmService.AddAlarmRequest.NAME, message.getMessageType());
      assertEquals(type.name(), AddAlarmRequest.getAlarm(message.getValue()));
      // FIXME other alarms and triggers should be verified
   }
   
   protected void assertNoMessages() {
      assertNull(bus.poll());
   }

   protected void assertAddAlarm(AlarmIncident incident, AlertType primary, List<AlertType> expectedAlerts, List<IncidentTrigger> triggers) {
      PlatformMessage message = bus.poll();
      MessageBody value = message.getValue();
      assertEquals(AlarmService.AddAlarmRequest.NAME, message.getMessageType());
      assertEquals(primary.name(), AlarmService.AddAlarmRequest.getAlarm(value));
      {
         List<String> expectedAlarms = expectedAlerts.stream().map(AlertType::name).collect(Collectors.toList());
         List<String> actualAlarms = AlarmService.AddAlarmRequest.getAlarms(value);
         assertEquals(expectedAlarms, actualAlarms);
      }
      {
         List<Map<String, Object>> expectedTriggers = (List<Map<String, Object>>) JSON.fromJson(JSON.toJson(triggers.stream().map(IncidentTrigger::toMap).collect(Collectors.toList())), Object.class);
         List<Map<String, Object>> actualTriggers = AlarmService.AddAlarmRequest.getTriggers(value);
         assertEquals(expectedTriggers, actualTriggers);
      }
   }

   protected void assertIncidentTrackers(AlarmIncident incident, TrackerState... states) {
      int i = 0;
      for(TrackerEvent event: incident.getTracker()) {
         if(i >= states.length) {
            fail("Unexpected tracker events: " + incident.getTracker().subList(states.length, incident.getTracker().size()));
         }
         assertEquals(String.format("Expected: %s but was: %s at index: %d", states[i], event.getState(), i), states[i].name(), event.getState());
         assertTrackerEvent(event);
         i++;
      }
      if(i < states.length) {
         fail("Missing tracker events: " + Arrays.asList(states).subList(i, states.length));
      }
   }

   protected void assertTrackerEvent(TrackerEvent event) {
      assertNotNull(event);
      assertNotNull(event.getState());
      assertNotNull(event.getTime());
      assertTrue(StringUtils.isNotBlank(event.getKey()));
      if(StringUtils.isBlank(event.getMessage())) {
         if(StringUtils.isNotBlank(PlatformAlarmIncidentService.getEventMessage(event.getKey()))) {
            fail("message should not be null for key "+event.getKey());
         }
      }
      
   }

   protected void assertIncidentPreAlert(AlarmIncident incident, Date prealertEndTime) {
      assertEquals(AlertType.SECURITY, incident.getAlert());
      assertEquals(AlertState.PREALERT, incident.getAlertState());
      assertEquals(AlertState.PREALERT, incident.getPlatformAlertState());
      assertEquals(MonitoringState.NONE, incident.getMonitoringState());
      assertEquals(prealertEndTime, incident.getPrealertEndTime());
      
      assertNotCancelled(incident);
   }

   protected void assertIncidentAlert(AlarmIncident incident, AlertType primary, AlertType... additional) {
      assertEquals(AlertState.ALERT, incident.getAlertState());
      assertEquals(AlertState.ALERT, incident.getPlatformAlertState());
      assertEquals(primary, incident.getAlert());
      assertEquals(ImmutableSet.copyOf(additional), incident.getAdditionalAlerts());
      
      assertNotCancelled(incident);
   }

   protected void assertNotCancelled(AlarmIncident incident) {
      assertNull(incident.getCancelledBy());
      assertNull(incident.getEndTime());
   }

   protected void assertIncidentMonitored(AlarmIncident incident) {
      if(ServiceLevel.isPromon(this.serviceLevel)) {
         assertTrue(incident.isMonitored());
      }else {
         assertFalse(incident.isMonitored());
      }     
   }

}

