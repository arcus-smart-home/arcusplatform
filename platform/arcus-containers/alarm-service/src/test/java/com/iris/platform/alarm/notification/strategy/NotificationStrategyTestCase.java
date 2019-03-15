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
package com.iris.platform.alarm.notification.strategy;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.common.alarm.AlertType;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.service.RuleService;
import com.iris.messages.type.CallTreeEntry;
import com.iris.messages.type.Population;
import com.iris.platform.alarm.history.ModelLoader;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.alarm.incident.Trigger;
import com.iris.platform.alarm.notification.calltree.CallTreeContext;
import com.iris.platform.alarm.notification.calltree.CallTreeDAO;
import com.iris.platform.alarm.notification.calltree.CallTreeExecutor;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.StatefulRuleDefinition;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.util.IrisUUID;

@Mocks({CallTreeExecutor.class, DeviceDAO.class, CallTreeDAO.class, AlarmIncidentDAO.class, RuleDao.class, PersonDAO.class, PlacePopulationCacheManager.class})
@Modules(InMemoryMessageModule.class)
public class NotificationStrategyTestCase extends IrisMockTestCase {

   protected static final UUID placeId = UUID.randomUUID();
   protected static final UUID incidentId = IrisUUID.timeUUID();
   protected static final Address incidentAddress = Address.platformService(incidentId, AlarmIncidentCapability.NAMESPACE);

   @Inject
   protected CallTreeExecutor callTreeExecutor;
   @Inject
   protected DeviceDAO deviceDao;
   @Inject
   protected CallTreeDAO callTreeDao;
   @Inject
   protected AlarmIncidentDAO incidentDao;
   @Inject
   protected RuleDao ruleDao;
   
   @Inject
   protected PersonDAO personDao;
   
   @Inject
   protected ModelLoader modelLoader;
   
   @Inject
   protected PlacePopulationCacheManager mockPopulationCacheMgr;

   @Before
   public void initMocks() {
   	EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }
   protected void assertCallTreeContext(CallTreeContext context, String msgKey, String priority) {
      assertEquals(msgKey, context.getMsgKey());
      assertEquals(priority, context.getPriority());
      assertEquals(placeId, context.getPlaceId());
   }

   protected AlarmIncident.Builder incidentBuilder() {
      return AlarmIncident.builder()
            .withId(incidentId)
            .withPlaceId(placeId);
   }

   protected Trigger setupTrigger(UUID deviceId, AlertType type, String name, String devType, int times) {
      ModelEntity m = new ModelEntity(ImmutableMap.of(
            Capability.ATTR_ID, deviceId.toString(),
            Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, DeviceCapability.NAMESPACE),
            Capability.ATTR_TYPE, DeviceCapability.NAMESPACE,
            DeviceCapability.ATTR_NAME, name,
            DeviceCapability.ATTR_DEVTYPEHINT, devType
      ));
      if(times > 0) {
         EasyMock.expect(deviceDao.modelById(deviceId)).andReturn(m).times(times);
      } else if(times < 0) {
         EasyMock.expect(deviceDao.modelById(deviceId)).andReturn(m).anyTimes();
      }
      return Trigger.builder()
            .withSource(Address.platformDriverAddress(deviceId))
            .withTime(new Date())
            .withAlarm(type)
            .withEvent(eventFromAlarm(type))
            .withSignalled(false)
            .build();
   }
   
   protected Trigger setupRuleTrigger(UUID placeId, int actionId, AlertType type, int times) {
	      RuleDefinition m = new StatefulRuleDefinition();
	      m.setPlaceId(placeId);
	      m.setSequenceId(actionId);
	      m.setName("testRule");
	      m.setRuleTemplate("panic-pendent");
	      m.setCreated(new Date());
	      Address ruleAddr = Address.platformService(placeId, RuleService.NAMESPACE, actionId);
	      m.setVariables(ImmutableMap.<String, Object>of(Capability.ATTR_ADDRESS, ruleAddr.getRepresentation()));
	      if(times > 0) {
	         EasyMock.expect(ruleDao.findById(placeId, actionId)).andReturn(m).times(times);
	      } else if(times < 0) {
	         EasyMock.expect(ruleDao.findById(placeId, actionId)).andReturn(m).anyTimes();
	      }
	      return Trigger.builder()
	            .withSource(ruleAddr)
	            .withTime(new Date())
	            .withAlarm(type)
	            .withEvent(Trigger.Event.RULE)
	            .withSignalled(false)
	            .build();
	   }

   private Trigger.Event eventFromAlarm(AlertType type) {
      switch(type) {
         case CARE: return Trigger.Event.BEHAVIOR;
         case CO: return Trigger.Event.CO;
         case SECURITY: return Trigger.Event.CONTACT;
         case SMOKE: return Trigger.Event.SMOKE;
         case WATER: return Trigger.Event.LEAK;
         case PANIC: return Trigger.Event.KEYPAD;
         default: return Trigger.Event.RULE;
      }
   }

   protected void setupCallTree(int times, CallTreeEntry... entries) {
      List<CallTreeEntry> entryList = Arrays.asList(entries);
      EasyMock.expect(callTreeDao.callTreeForPlace(placeId)).andReturn(entryList).times(times);
   }

   protected CallTreeEntry callTreeEntry(UUID personId, boolean enabled) {
      CallTreeEntry entry = new CallTreeEntry();
      entry.setPerson(Address.platformService(personId, PersonCapability.NAMESPACE).getRepresentation());
      entry.setEnabled(enabled);
      return entry;
   }
   
   protected AlarmIncident stageCancelAlert(AlertType type) {
      Person cancelledByPerson = Fixtures.createPerson();
      cancelledByPerson.setId(UUID.randomUUID());

      AlarmIncident incident = incidentBuilder()
         .withAlert(type)
         .withCancelledBy(cancelledByPerson.getAddress())
         .build();
      EasyMock.expect(personDao.findById(cancelledByPerson.getId())).andReturn(cancelledByPerson).anyTimes();
      callTreeExecutor.stopSequential(incidentAddress, NotificationConstants.SECURITY_KEY);
      EasyMock.expectLastCall();
      callTreeExecutor.stopSequential(incidentAddress, NotificationConstants.PANIC_KEY);
      EasyMock.expectLastCall();
      
      return incident;
   }
   
   protected AlarmIncident stagingCancelAlert(AlertType type, Capture<CallTreeContext> contextCaptureForCancel, Capture<CallTreeContext> contextCaptureForCancel2) {	   	
      AlarmIncident incident = stageCancelAlert(type);
      
      callTreeExecutor.notifyParallel(EasyMock.capture(contextCaptureForCancel));
      EasyMock.expectLastCall();
      callTreeExecutor.notifyOwner(EasyMock.capture(contextCaptureForCancel2));
      EasyMock.expectLastCall();
      return incident;
   }

}

