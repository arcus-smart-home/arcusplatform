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
package com.iris.platform.history.appender;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.scene.SceneDao;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

@Mocks({PersonDAO.class, PlaceDAO.class, DeviceDAO.class, HubDAO.class, RuleDao.class, SceneDao.class})
public class TestObjectNameCache extends IrisMockTestCase {

   @Inject ObjectNameCache cache;
   
   @Inject PersonDAO personDao;
   @Inject PlaceDAO placeDao;
   @Inject DeviceDAO deviceDao;
   @Inject HubDAO hubDao;
   @Inject RuleDao ruleDao;
   @Inject SceneDao sceneDao;
   
   Place place = Fixtures.createPlace();
   Device device = Fixtures.createDevice();
   Person person = Fixtures.createPerson();

   Address placeAddress;
   Address deviceAddress;
   Address personAddress;
   Address agentAddress;
   
   String agentName;
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      place.setId(UUID.randomUUID());
      place.setName("My Place");
      placeAddress = Address.fromString(place.getAddress());
      device.setId(UUID.randomUUID());
      device.setAddress(Address.platformDriverAddress(device.getId()).getRepresentation());
      device.setName("My Device");
      deviceAddress = Address.fromString(device.getAddress());
      person.setId(UUID.randomUUID());
      personAddress = Address.fromString(person.getAddress());
   }
   
   @Test
   public void testGetPersonName() {
   	expectFindPerson();
   	replay();
   	
   	assertEquals(person.getFirstName(), cache.getPersonName(personAddress));
   	// second call should skip the dao
   	assertEquals(person.getFirstName(), cache.getPersonName(personAddress));
   	
   	verify();
   }
   
   @Test
   public void testGetThenUpdatePersonName() {
   	expectFindPerson();
   	replay();
   	
   	assertEquals(person.getFirstName(), cache.getPersonName(personAddress));
      cache.update(createValueChange(personAddress, PersonCapability.ATTR_FIRSTNAME, "Dude"));
               
      // second call should skip the dao
      assertEquals("Dude", cache.getPersonName(personAddress));
      
      verify();
   }
   
   @Test
   public void testGetPersonNameNotFound() {
      expectPersonNotFound();
      replay();
      
      assertEquals("Someone", cache.getPersonName(personAddress));
      
      verify();
   }
   
   @Test
   public void testGetPlaceName() {
      expectFindPlace();
      replay();
      
      assertEquals(place.getName(), cache.getPlaceName(placeAddress));
      // second call should skip the dao
      assertEquals(place.getName(), cache.getPlaceName(placeAddress));
      
      verify();
   }

   @Test
   public void testGetThenUpdatePlaceName() {
      expectFindPlace();
      replay();
      
      assertEquals(place.getName(), cache.getPlaceName(placeAddress));
      cache.update(createValueChange(placeAddress, PlaceCapability.ATTR_NAME, "A brand new name"));
               
      // second call should skip the dao
      assertEquals("A brand new name", cache.getPlaceName(placeAddress));
      
      verify();
   }

   @Test
   public void testGetPlaceNameNotFound() {
      expectPlaceNotFound();
      replay();
      
      assertEquals("UNKNOWN_PLACE", cache.getPlaceName(placeAddress));
      
      verify();
   }

   @Test
   public void testGetDeviceName() {
      expectFindDevice();
      replay();
      
      assertEquals(device.getName(), cache.getDeviceName(deviceAddress));
      // second call should skip the dao
      assertEquals(device.getName(), cache.getDeviceName(deviceAddress));
      
      verify();
   }

   @Test
   public void testGetThenUpdateDeviceName() {
      expectFindPlace();
      replay();
      
      assertEquals(place.getName(), cache.getPlaceName(placeAddress));
      cache.update(createValueChange(placeAddress, PlaceCapability.ATTR_NAME, "A brand new name"));
               
      // second call should skip the dao
      assertEquals("A brand new name", cache.getPlaceName(placeAddress));
      
      verify();
   }

   @Test
   public void testGetDeviceNameNotFound() {
      expectDeviceNotFound();
      replay();
      
      assertEquals("UNKNOWN_DEVICE", cache.getDeviceName(deviceAddress));
      
      verify();
   }

   private PlatformMessage createValueChange(Address source, String attribute, Object value) {
      return createValueChange(source, ImmutableMap.of(attribute, value));
   }

   private PlatformMessage createValueChange(Address source, Map<String, Object> attributes) {
      return
            PlatformMessage
               .builder()
               .from(source)
               .broadcast()
               .withPayload(Capability.EVENT_VALUE_CHANGE, attributes)
               .create()
               ;
   }
   
   protected void expectFindPerson() {
   	EasyMock.expect(personDao.findById(person.getId())).andReturn(person).once();
   }
   
   protected void expectFindPlace() {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place).once();
   }
   
   protected void expectPersonNotFound() {
   	EasyMock.expect(personDao.findById(person.getId())).andReturn(null).once();
   }
   
   protected void expectPlaceNotFound() {
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(null).once();
   }

   protected void expectFindDevice() {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device).once();
   }

   protected void expectDeviceNotFound() {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(null).once();
   }

}

