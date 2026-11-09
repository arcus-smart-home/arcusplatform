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
package com.iris.platform.subsystem.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.Subsystem;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.context.PlaceContext;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.service.SubsystemService;
import com.iris.messages.service.SubsystemService.ListSubsystemsRequest;
import com.iris.messages.service.SubsystemService.ListSubsystemsResponse;
import com.iris.platform.subsystem.SubsystemFactory;

/**
 * After the resolver picks an executor, the executor checks ListSubsystems again: the place named
 * in the body has to match the place header. It doesn't compare either with its own place.
 */
public class PlatformSubsystemExecutorScopeTest {
   private final UUID activePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();

   private PlatformSubsystemExecutor executor;

   @Before
   public void setUp() {
      PlaceContext context = EasyMock.createNiceMock(PlaceContext.class);
      EasyMock.expect(context.getPlaceId()).andReturn(activePlace).anyTimes();
      EasyMock.expect(context.models()).andReturn(new SimpleModelStore()).anyTimes();
      PlatformMessageBus bus = EasyMock.createMock(PlatformMessageBus.class);
      SubsystemFactory factory = EasyMock.createMock(SubsystemFactory.class);
      EasyMock.replay(context, bus, factory);

      executor = new PlatformSubsystemExecutor(bus, null, factory, context, Collections.<Subsystem<?>>emptyList(), 10);
   }

   private static PlatformMessage listSubsystems(UUID bodyPlace, UUID placeHeader) {
      return PlatformMessage
            .buildRequest(
                  ListSubsystemsRequest.NAME,
                  ImmutableMap.of(ListSubsystemsRequest.ATTR_PLACEID, bodyPlace.toString()),
                  Address.clientAddress("android", "1"),
                  Address.platformService(SubsystemService.NAMESPACE))
            .withPlaceId(placeHeader)
            .create();
   }

   @Test
   public void testListSubsystemsForAnotherPlaceIsRefused() {
      try {
         executor.handleServiceRequest(listSubsystems(otherPlace, activePlace));
         fail("expected ListSubsystems for another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
   }

   @Test
   public void testListSubsystemsForTheActivePlace() {
      MessageBody response = executor.handleServiceRequest(listSubsystems(activePlace, activePlace));
      assertEquals(ListSubsystemsResponse.NAME, response.getMessageType());
   }
}
