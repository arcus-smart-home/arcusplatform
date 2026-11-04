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
package com.iris.platform.pairing.resolver;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.Function;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.reflect.TypeToken;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Fixtures;
import com.iris.platform.model.PersistentModelWrapper;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.population.PlacePopulationCacheManager;

/**
 * Pairing devices are addressed SERV:pairdev:<place>.<n>; both kinds of parameter the resolver
 * fills must refuse a client whose active place is another place, before anything is loaded.
 */
public class PairingDeviceResolverScopeTest {
   private static final Type WRAPPER = new TypeToken<PersistentModelWrapper<PairingDevice>>() { }.getType();

   private final UUID activePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();

   private PairingDeviceDao dao;
   private PlacePopulationCacheManager populations;
   private PairingDeviceResolver resolver;

   @Before
   public void setUp() {
      // strict, so a refused request that loaded the device fails the test
      dao = EasyMock.createMock(PairingDeviceDao.class);
      populations = EasyMock.createMock(PlacePopulationCacheManager.class);
      resolver = new PairingDeviceResolver(EasyMock.createNiceMock(PlatformMessageBus.class), dao, populations);
   }

   @After
   public void tearDown() {
      EasyMock.verify(dao, populations);
   }

   private Function<? super PlatformMessage, ?> resolverFor(Type parameter) {
      return resolver.getResolverForParameter(null, parameter, new Annotation[0]);
   }

   private static Address pairingDevice(UUID place) {
      return Address.platformService(place, PairingDeviceCapability.NAMESPACE, 1);
   }

   private PlatformMessage request(Address destination, UUID placeHeader) {
      PlatformMessage.Builder builder = PlatformMessage
            .buildRequest(Capability.CMD_GET_ATTRIBUTES, java.util.Collections.<String, Object>emptyMap(), Fixtures.createClientAddress(), destination);
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private void assertRefused(Type parameter) {
      EasyMock.replay(dao, populations);
      try {
         resolverFor(parameter).apply(request(pairingDevice(otherPlace), activePlace));
         fail("expected a pairing device at another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
   }

   @Test
   public void testPairingDeviceAtAnotherPlaceIsRefused() {
      assertRefused(PairingDevice.class);
   }

   @Test
   public void testWrappedPairingDeviceAtAnotherPlaceIsRefused() {
      assertRefused(WRAPPER);
   }

   @Test
   public void testPairingDeviceAtTheActivePlaceIsLoaded() {
      PairingDevice device = new PairingDevice();
      EasyMock.expect(dao.findByAddress(pairingDevice(activePlace))).andReturn(device);
      EasyMock.replay(dao, populations);

      assertSame(device, resolverFor(PairingDevice.class).apply(request(pairingDevice(activePlace), activePlace)));
   }

   @Test
   public void testInternalRequestWithoutAPlaceHeaderIsLoaded() {
      PairingDevice device = new PairingDevice();
      EasyMock.expect(dao.findByAddress(pairingDevice(otherPlace))).andReturn(device);
      EasyMock.replay(dao, populations);

      assertSame(device, resolverFor(PairingDevice.class).apply(request(pairingDevice(otherPlace), null)));
   }
}
