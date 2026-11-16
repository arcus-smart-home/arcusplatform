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
package com.iris.platform.services;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.iris.platform.services.account.AccountService;
import com.iris.platform.services.mobiledevice.MobileDeviceService;
import com.iris.platform.services.person.PersonService;
import com.iris.platform.services.place.PlaceService;
import com.iris.platform.services.population.PopulationService;

/**
 * Nothing on a hub talks to these services, and hub-bridge forwards a hub's messages to them with no
 * authorizer, so each one refuses hubs through HubExcludingDispatcher. PlaceServiceHubRequestTest
 * covers what that means for requests, events and broadcasts.
 */
public class HubExcludedServicesTest {

   @Test
   public void testServicesHubsNeverUseRefuseHubs() {
      for(Class<?> service : List.of(PersonService.class, AccountService.class, PlaceService.class, MobileDeviceService.class, PopulationService.class)) {
         assertTrue(service.getSimpleName(), HubExcludingDispatcher.class.isAssignableFrom(service));
      }
   }
}
