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
package com.iris.service.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubBackupCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubZigbeeCapability;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.messages.service.SchedulerService.FireCommandRequest;
import com.iris.messages.service.SchedulerService.ScheduleWeeklyCommandRequest;
import com.iris.messages.service.SchedulerService.ScheduleWeeklyCommandResponse;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.scheduler.SchedulerModelDao;
import com.iris.population.PlacePopulationCacheManager;

public class PlatformSchedulerCapabilityDispatcherTest {
   private static final Map<String, Object> NO_ATTRIBUTES = ImmutableMap.of();
   private static final Address SERVICE = Address.platformService(SchedulerCapability.NAMESPACE);
   private static final Address CLIENT = Address.clientAddress("android", "1");

   private final UUID placeId = UUID.randomUUID();
   private final UUID schedulerId = UUID.randomUUID();
   private final Address schedulerAddress = Address.platformService(schedulerId, SchedulerCapability.NAMESPACE);
   private final Address device = Address.platformDriverAddress(UUID.randomUUID());
   private final Address hub = Address.fromString("SERV:ABC-1234:hub");
   private final Address hubService = Address.fromString("SERV:hub:ABC-1234");

   private final InMemoryPlatformMessageBus bus = new InMemoryPlatformMessageBus();
   private final AtomicInteger saves = new AtomicInteger();

   @After
   public void tearDown() throws Exception {
      bus.destroy();
   }

   private PlatformSchedulerCapabilityDispatcher dispatcherFor(Address target) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(Capability.ATTR_ID, schedulerId.toString());
      attributes.put(Capability.ATTR_ADDRESS, schedulerAddress.getRepresentation());
      attributes.put(Capability.ATTR_TYPE, SchedulerCapability.NAMESPACE);
      attributes.put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, SchedulerCapability.NAMESPACE));
      attributes.put(Capability.ATTR_INSTANCES, ImmutableMap.of());
      attributes.put(SchedulerCapability.ATTR_PLACEID, placeId.toString());
      attributes.put(SchedulerCapability.ATTR_TARGET, target.getRepresentation());
      ModelEntity scheduler = new ModelEntity(attributes);
      Date created = new Date();
      scheduler.setCreated(created);
      scheduler.setModified(created);

      SchedulerContext context = new SchedulerContext();
      context.setScheduler(scheduler);
      context.setTimeZone(TimeZone.getTimeZone("UTC"));

      SchedulerModelDao dao = EasyMock.createNiceMock(SchedulerModelDao.class);
      EasyMock.expect(dao.save(EasyMock.anyObject(ModelEntity.class))).andAnswer(() -> {
         saves.incrementAndGet();
         return (ModelEntity) EasyMock.getCurrentArguments()[0];
      }).anyTimes();
      EventSchedulerService events = EasyMock.createNiceMock(EventSchedulerService.class);
      DefinitionRegistry registry = EasyMock.createNiceMock(DefinitionRegistry.class);
      PlacePopulationCacheManager populations = EasyMock.createNiceMock(PlacePopulationCacheManager.class);
      EasyMock.replay(dao, events, registry, populations);

      return new PlatformSchedulerCapabilityDispatcher(context, bus, dao, events, registry, populations);
   }

   private void handle(PlatformSchedulerCapabilityDispatcher dispatcher, MessageBody request) {
      dispatcher.handleEvent(new MessageReceivedEvent(
            PlatformMessage.buildRequest(request, CLIENT, SERVICE).withPlaceId(placeId).create()
      ));
   }

   private List<PlatformMessage> sent() {
      List<PlatformMessage> messages = new ArrayList<>();
      for(PlatformMessage message = bus.poll(); message != null; message = bus.poll()) {
         messages.add(message);
      }
      return messages;
   }

   private String schedule(PlatformSchedulerCapabilityDispatcher dispatcher, Address target, String messageType, Map<String, Object> attributes) {
      handle(dispatcher, MessageBody.buildMessage(ScheduleWeeklyCommandRequest.NAME, ImmutableMap.of(
            ScheduleWeeklyCommandRequest.ATTR_TARGET, target.getRepresentation(),
            ScheduleWeeklyCommandRequest.ATTR_SCHEDULE, "WEEKLY",
            ScheduleWeeklyCommandRequest.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
            ScheduleWeeklyCommandRequest.ATTR_TIME, "12:00:00",
            ScheduleWeeklyCommandRequest.ATTR_MESSAGETYPE, messageType,
            ScheduleWeeklyCommandRequest.ATTR_ATTRIBUTES, attributes
      )));
      for(PlatformMessage message : sent()) {
         if(ScheduleWeeklyCommandResponse.NAME.equals(message.getMessageType())) {
            return ScheduleWeeklyCommandResponse.getCommandId(message.getValue());
         }
      }
      throw new AssertionError("the command wasn't stored");
   }

   private List<PlatformMessage> fire(PlatformSchedulerCapabilityDispatcher dispatcher, Address target, String commandId) {
      handle(dispatcher, MessageBody.buildMessage(FireCommandRequest.NAME, ImmutableMap.of(
            FireCommandRequest.ATTR_TARGET, target.getRepresentation(),
            FireCommandRequest.ATTR_COMMANDID, commandId
      )));
      return sent();
   }

   private List<PlatformMessage> timer(PlatformSchedulerCapabilityDispatcher dispatcher) {
      Date nextFire = SchedulerModel.getNextFireTime(dispatcher.getScheduler());
      dispatcher.handleEvent(new ScheduledEvent(schedulerAddress, nextFire.getTime()));
      return sent();
   }

   private boolean reached(Address target, List<PlatformMessage> messages) {
      return messages.stream().anyMatch((message) -> target.equals(message.getDestination()));
   }

   private boolean refused(List<PlatformMessage> messages) {
      return messages.stream().anyMatch(PlatformMessage::isError);
   }

   private void assertFireIsRefused(Address target, String messageType) {
      PlatformSchedulerCapabilityDispatcher dispatcher = dispatcherFor(target);
      List<PlatformMessage> messages = fire(dispatcher, target, schedule(dispatcher, target, messageType, NO_ATTRIBUTES));
      assertTrue(messageType + " at " + target + " should return an error", refused(messages));
      assertFalse(messageType + " at " + target + " should not be sent", reached(target, messages));
   }

   private void assertFireGoesOut(Address target, String messageType, Map<String, Object> attributes) {
      PlatformSchedulerCapabilityDispatcher dispatcher = dispatcherFor(target);
      List<PlatformMessage> messages = fire(dispatcher, target, schedule(dispatcher, target, messageType, attributes));
      assertFalse(messageType + " at " + target + " should not return an error", refused(messages));
      assertTrue(messageType + " at " + target + " should be sent", reached(target, messages));
   }

   // what schedules are for

   @Test
   public void testFireSendsDeviceCommands() {
      assertFireGoesOut(device, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("swit:state", "ON"));
   }

   @Test
   public void testFireSendsSceneCommands() {
      assertFireGoesOut(Address.platformService(placeId, SceneCapability.NAMESPACE, 1), SceneCapability.FireRequest.NAME, NO_ATTRIBUTES);
   }

   @Test
   public void testFireSendsRuleCommands() {
      assertFireGoesOut(Address.platformService(placeId, RuleCapability.NAMESPACE, 1), RuleCapability.EnableRequest.NAME, NO_ATTRIBUTES);
   }

   @Test
   public void testFireSendsSubsystemCommands() {
      assertFireGoesOut(Address.platformService(placeId, "subcare"), Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("subcare:silent", true));
   }

   @Test
   public void testTimerSendsDeviceCommands() {
      PlatformSchedulerCapabilityDispatcher dispatcher = dispatcherFor(device);
      schedule(dispatcher, device, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("swit:state", "ON"));

      assertTrue(reached(device, timer(dispatcher)));
   }

   // what they never send

   @Test
   public void testFireRefusesHubMethods() {
      for(Address target : new Address[] { hub, hubService }) {
         assertFireIsRefused(target, HubCapability.DeleteRequest.NAME);
         assertFireIsRefused(target, HubAdvancedCapability.FactoryResetRequest.NAME);
         assertFireIsRefused(target, HubAdvancedCapability.RebootRequest.NAME);
         assertFireIsRefused(target, HubZigbeeCapability.FactoryResetRequest.NAME);
         assertFireIsRefused(target, HubZwaveCapability.FactoryResetRequest.NAME);
         assertFireIsRefused(target, HubZwaveCapability.ResetRequest.NAME);
         assertFireIsRefused(target, HubBackupCapability.RestoreRequest.NAME);
         assertFireIsRefused(target, Capability.CMD_SET_ATTRIBUTES);
      }
   }

   @Test
   public void testFireRefusesRiskyOperationsWhateverTheTarget() {
      assertFireIsRefused(device, PlaceCapability.DeleteRequest.NAME);
      assertFireIsRefused(Address.platformService(placeId, "subcare"), HubCapability.DeleteRequest.NAME);
   }

   @Test
   public void testTimerRefusesAHubMethodAndMovesToTheNextFireTime() {
      PlatformSchedulerCapabilityDispatcher dispatcher = dispatcherFor(hub);
      schedule(dispatcher, hub, HubCapability.DeleteRequest.NAME, NO_ATTRIBUTES);
      Date firstFire = SchedulerModel.getNextFireTime(dispatcher.getScheduler());
      int savedBefore = saves.get();

      assertFalse(reached(hub, timer(dispatcher)));

      assertTrue(SchedulerModel.getNextFireTime(dispatcher.getScheduler()).after(firstFire));
      assertTrue("the schedule should be committed", saves.get() > savedBefore);
   }
}
