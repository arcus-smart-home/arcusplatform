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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.SchedulerService.DeleteCommandRequest;
import com.iris.messages.service.SchedulerService.FireCommandRequest;
import com.iris.messages.service.SchedulerService.GetSchedulerRequest;
import com.iris.messages.service.SchedulerService.GetSchedulerResponse;
import com.iris.messages.service.SchedulerService.ListSchedulersRequest;
import com.iris.messages.service.SchedulerService.ListSchedulersResponse;
import com.iris.messages.service.SchedulerService.ScheduleCommandsRequest;
import com.iris.messages.service.SchedulerService.ScheduleWeeklyCommandRequest;
import com.iris.messages.service.SchedulerService.UpdateWeeklyCommandRequest;

/**
 * Schedulers belong to a place and send commands with that place's header, so a client must not
 * reach another place's scheduler, either at its own address or through the scheduler service.
 * The registry and the scheduler are strict mocks: a refused request that loaded, created or
 * changed a scheduler fails the test.
 */
public class SchedulerCapabilityServiceScopeTest {
   /** Every service request that names a target, each of which loads (or creates) the target's scheduler. */
   private static final List<String> TARGETED_SERVICE_REQUESTS = ImmutableList.of(
         GetSchedulerRequest.NAME,
         ScheduleCommandsRequest.NAME,
         ScheduleWeeklyCommandRequest.NAME,
         UpdateWeeklyCommandRequest.NAME,
         FireCommandRequest.NAME,
         DeleteCommandRequest.NAME
   );

   /** What a client can send to a scheduler's own address. */
   private static final List<String> SCHEDULER_REQUESTS = ImmutableList.of(
         SchedulerCapability.FireCommandRequest.NAME,
         SchedulerCapability.AddWeeklyScheduleRequest.NAME,
         SchedulerCapability.DeleteRequest.NAME,
         SchedulerCapability.RecalculateScheduleRequest.NAME,
         Capability.CMD_GET_ATTRIBUTES,
         Capability.CMD_SET_ATTRIBUTES
   );

   private final UUID activePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();
   private final Address schedulerAddress = Address.platformService(UUID.randomUUID(), SchedulerCapability.NAMESPACE);
   private final Address device = Address.platformDriverAddress(UUID.randomUUID());

   private InMemoryPlatformMessageBus bus;
   private SchedulerRegistry registry;
   private SchedulerCapabilityDispatcher scheduler;
   private SchedulerCapabilityService service;

   @Before
   public void setUp() {
      bus = new InMemoryPlatformMessageBus();
      registry = EasyMock.createMock(SchedulerRegistry.class);
      scheduler = EasyMock.createMock(SchedulerCapabilityDispatcher.class);
      service = new SchedulerCapabilityService(bus, new SchedulerServiceConfig(), registry);
   }

   private void replay() {
      EasyMock.replay(registry, scheduler);
   }

   private void verifyAndReset() {
      EasyMock.verify(registry, scheduler);
      EasyMock.reset(registry, scheduler);
   }

   private static Model schedulerAt(UUID place) {
      return new SimpleModel(ImmutableMap.<String, Object>of(SchedulerCapability.ATTR_PLACEID, place.toString()));
   }

   private PlatformMessage request(Address destination, String type, Map<String, Object> attributes) {
      return PlatformMessage
            .request(destination)
            .from(Fixtures.createClientAddress())
            .withPlaceId(activePlace)
            .withPayload(MessageBody.buildMessage(type, attributes))
            .create();
   }

   private PlatformMessage serviceRequest(String type, Address target) {
      // every targeted request names its target with the same attribute
      return request(Address.platformService(SchedulerCapability.NAMESPACE), type, ImmutableMap.of(FireCommandRequest.ATTR_TARGET, target.getRepresentation()));
   }

   // requests addressed to SERV:scheduler:<id>

   @Test
   public void testRequestsToAnotherPlacesSchedulerAreRefused() {
      for(String type: SCHEDULER_REQUESTS) {
         EasyMock.expect(registry.loadByAddress(schedulerAddress)).andReturn(Optional.of(scheduler));
         EasyMock.expect(scheduler.getScheduler()).andReturn(schedulerAt(otherPlace));
         replay();

         try {
            service.handleSchedulerRequest(request(schedulerAddress, type, ImmutableMap.of()));
            fail("expected " + type + " to another place's scheduler to be refused");
         }
         catch(UnauthorizedRequestException e) {
            // expected
         }
         verifyAndReset();
      }
   }

   @Test
   public void testRequestsToTheActivePlacesSchedulerReachIt() {
      for(String type: SCHEDULER_REQUESTS) {
         PlatformMessage message = request(schedulerAddress, type, ImmutableMap.of());
         EasyMock.expect(registry.loadByAddress(schedulerAddress)).andReturn(Optional.of(scheduler));
         EasyMock.expect(scheduler.getScheduler()).andReturn(schedulerAt(activePlace));
         scheduler.onPlatformMessage(message);
         EasyMock.expectLastCall();
         replay();

         service.handleSchedulerRequest(message);
         verifyAndReset();
      }
   }

   // service requests to SERV:scheduler:, which name the target in the body

   @Test
   public void testServiceRequestsForAnotherPlacesDeviceSchedulerAreRefused() {
      for(String type: TARGETED_SERVICE_REQUESTS) {
         EasyMock.expect(registry.loadOrCreateByAddress(activePlace, device)).andReturn(scheduler);
         EasyMock.expect(scheduler.getScheduler()).andReturn(schedulerAt(otherPlace));
         replay();

         try {
            service.handleServiceRequest(serviceRequest(type, device));
            fail("expected " + type + " for another place's scheduler to be refused");
         }
         catch(UnauthorizedRequestException e) {
            // expected
         }
         verifyAndReset();
      }
      assertNull("a refused request must not answer with the scheduler", bus.poll());
   }

   @Test
   public void testServiceRequestsTargetingAnotherPlaceAreRefusedBeforeAnySchedulerIsLoaded() {
      Address otherPlacesScene = Address.platformService(otherPlace, SceneCapability.NAMESPACE, 1);
      for(String type: TARGETED_SERVICE_REQUESTS) {
         replay();

         try {
            service.handleServiceRequest(serviceRequest(type, otherPlacesScene));
            fail("expected " + type + " targeting another place to be refused");
         }
         catch(ErrorEventException e) {
            // expected; no scheduler was created for the other place's target
         }
         verifyAndReset();
      }
   }

   @Test
   public void testServiceRequestsAtTheActivePlaceReachTheScheduler() throws Exception {
      for(String type: TARGETED_SERVICE_REQUESTS) {
         PlatformMessage message = serviceRequest(type, device);
         Model model = schedulerAt(activePlace);
         EasyMock.expect(registry.loadOrCreateByAddress(activePlace, device)).andReturn(scheduler);
         if(GetSchedulerRequest.NAME.equals(type)) {
            EasyMock.expect(scheduler.getScheduler()).andReturn(model).times(2);
         }
         else {
            EasyMock.expect(scheduler.getScheduler()).andReturn(model);
            scheduler.onPlatformMessage(message);
            EasyMock.expectLastCall();
         }
         replay();

         service.handleServiceRequest(message);
         verifyAndReset();
      }
      // only GetScheduler answers; the others hand the request to the scheduler
      assertEquals(GetSchedulerResponse.NAME, bus.take().getMessageType());
      assertNull(bus.poll());
   }

   @Test
   public void testListSchedulersForAnotherPlaceIsEmpty() throws Exception {
      replay();

      service.handleServiceRequest(request(
            Address.platformService(SchedulerCapability.NAMESPACE),
            ListSchedulersRequest.NAME,
            ImmutableMap.of(ListSchedulersRequest.ATTR_PLACEID, otherPlace.toString())));

      assertTrue(ListSchedulersResponse.getSchedulers(bus.take().getValue()).isEmpty());
      verifyAndReset();
   }

   @Test
   public void testListSchedulersForTheActivePlace() throws Exception {
      EasyMock.expect(registry.loadByPlace(activePlace, true)).andReturn(ImmutableList.of(scheduler));
      EasyMock.expect(scheduler.getScheduler()).andReturn(schedulerAt(activePlace));
      replay();

      service.handleServiceRequest(request(
            Address.platformService(SchedulerCapability.NAMESPACE),
            ListSchedulersRequest.NAME,
            ImmutableMap.of(ListSchedulersRequest.ATTR_PLACEID, activePlace.toString())));

      assertEquals(1, ListSchedulersResponse.getSchedulers(bus.take().getValue()).size());
      verifyAndReset();
   }
}
