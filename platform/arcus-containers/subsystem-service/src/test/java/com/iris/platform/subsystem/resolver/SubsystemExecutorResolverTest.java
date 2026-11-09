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
package com.iris.platform.subsystem.resolver;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.service.SubsystemService;
import com.iris.messages.service.SubsystemService.ListSubsystemsRequest;
import com.iris.platform.subsystem.SubsystemRegistry;

public class SubsystemExecutorResolverTest {
   private final UUID activePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();

   private SubsystemRegistry registry;
   private SubsystemExecutor executor;
   private Function<? super PlatformMessage, ?> resolver;

   @Before
   public void setUp() {
      // strict, so an unexpected load fails the test
      registry = EasyMock.createStrictMock(SubsystemRegistry.class);
      executor = EasyMock.createNiceMock(SubsystemExecutor.class);
      resolver = new SubsystemExecutorResolver(registry).getResolverForParameter(null, SubsystemExecutor.class, new Annotation[0]);
   }

   private PlatformMessage request(String type, Address destination, UUID placeHeader) {
      PlatformMessage.Builder builder = PlatformMessage.buildRequest(type, ImmutableMap.of(), Address.clientAddress("android", "1"), destination);
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private void expectLoad(UUID place) {
      EasyMock.expect(registry.loadByPlace(place)).andReturn(Optional.of(executor));
   }

   private void assertRefused(PlatformMessage message) {
      EasyMock.replay(registry);
      try {
         resolver.apply(message);
         fail("expected the request to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      EasyMock.verify(registry);
   }

   @Test
   public void testPlaceMustMatchTheHeader() {
      assertRefused(request(Capability.CMD_GET_ATTRIBUTES, Address.platformService(otherPlace, AlarmSubsystemCapability.NAMESPACE), activePlace));
   }

   @Test
   public void testPlaceMustMatchTheHeaderOnTheSecuritySubsystem() {
      assertRefused(request(Capability.CMD_GET_ATTRIBUTES, Address.platformService(otherPlace, SecuritySubsystemCapability.NAMESPACE), activePlace));
   }

   @Test
   public void testLoadsThePlaceInTheHeader() {
      expectLoad(activePlace);
      EasyMock.replay(registry);

      assertSame(executor, resolver.apply(request(Capability.CMD_GET_ATTRIBUTES, Address.platformService(activePlace, AlarmSubsystemCapability.NAMESPACE), activePlace)));
      EasyMock.verify(registry);
   }

   @Test
   public void testAnInternalMessageWithoutAPlaceHeaderStillWorks() {
      expectLoad(otherPlace);
      EasyMock.replay(registry);

      assertSame(executor, resolver.apply(request(Capability.CMD_GET_ATTRIBUTES, Address.platformService(otherPlace, AlarmSubsystemCapability.NAMESPACE), null)));
      EasyMock.verify(registry);
   }

   @Test
   public void testAnIncidentRequestWithoutAPlaceHeaderIsNotFound() {
      EasyMock.replay(registry);
      try {
         resolver.apply(request(AlarmIncidentCapability.CancelRequest.NAME, Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE), null));
         fail("expected an incident request without a place header to be refused");
      }
      catch(com.iris.messages.errors.NotFoundException e) {
         // expected
      }
      EasyMock.verify(registry);
   }

   @Test
   public void testAnIncidentRequestWithAMalformedPlaceHeaderIsNotFound() {
      EasyMock.replay(registry);
      PlatformMessage message = PlatformMessage
            .buildRequest(AlarmIncidentCapability.CancelRequest.NAME, ImmutableMap.of(), Address.clientAddress("android", "1"), Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE))
            .withPlaceId("not-a-place-id")
            .create();
      try {
         resolver.apply(message);
         fail("expected an incident request with a malformed place header to be refused");
      }
      catch(com.iris.messages.errors.NotFoundException e) {
         // expected
      }
      EasyMock.verify(registry);
   }

   @Test
   public void testIncidentsAreLoadedFromTheActivePlaceWhateverIdIsAddressed() {
      expectLoad(activePlace);
      EasyMock.replay(registry);

      assertSame(executor, resolver.apply(request(AlarmIncidentCapability.CancelRequest.NAME, Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE), activePlace)));
      EasyMock.verify(registry);
   }

   private PlatformMessage listSubsystems(UUID place) {
      return PlatformMessage
            .buildRequest(
                  ListSubsystemsRequest.NAME,
                  ImmutableMap.of(ListSubsystemsRequest.ATTR_PLACEID, place.toString()),
                  Address.clientAddress("android", "1"),
                  Address.platformService(SubsystemService.NAMESPACE))
            .withPlaceId(activePlace)
            .create();
   }

   @Test
   public void testListSubsystemsForAnotherPlaceIsRefused() {
      assertRefused(listSubsystems(otherPlace));
   }

   @Test
   public void testListSubsystemsLoadsTheActivePlace() {
      expectLoad(activePlace);
      EasyMock.replay(registry);

      assertSame(executor, resolver.apply(listSubsystems(activePlace)));
      EasyMock.verify(registry);
   }
}
