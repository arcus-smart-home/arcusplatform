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
package com.iris.driver.pin;

import java.util.Collections;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Person;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({PersonDAO.class, PersonPlaceAssocDAO.class})
public class TestPlatformPinManager extends IrisMockTestCase {

   private static final long TIMEOUT = 100;
   private static final UUID place = UUID.randomUUID();

   private static final byte ZERO = (byte)'0';
   private static final byte ONE = (byte)'1';
   private static final byte TWO = (byte)'2';

   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;

   private PinManager pinManager;
   private Person person;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      pinManager = new PlatformPinManager(ServiceLocator.getInstance(PersonDAO.class), ServiceLocator.getInstance(PersonPlaceAssocDAO.class), TIMEOUT);
      person = new Person();
      person.setId(UUID.randomUUID());
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testGetPin() {
      person.setPinAtPlace(place, "1111");
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();

      byte[] pin = pinManager.getPin(place, person.getId());
      assertArrayEquals(new byte[] { ONE, ONE, ONE, ONE}, pin);
   }

   @Test
   public void testGetPinNoUser() {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(null);
      replay();

      byte[] pin = pinManager.getPin(place, person.getId());
      assertEquals(0, pin.length);
   }

   @Test
   public void testGetPinUserHasNoPin() {
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();

      byte[] pin = pinManager.getPin(place, person.getId());
      assertEquals(0, pin.length);
   }

   @Test
   public void testValidatePin() {
      person.setPinAtPlace(place, "1111");
      EasyMock.expect(personPlaceAssocDao.findPersonIdsByPlace(place)).andReturn(Collections.<UUID>singleton(person.getId()));
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();

      UUID id = pinManager.validatePin(place, new byte[] { ONE, ONE, ONE, ONE});
      assertEquals(person.getId(), id);
   }

   @Test
   public void testValidatePinInvalid() {
      person.setPinAtPlace(place, "1111");
      EasyMock.expect(personPlaceAssocDao.findPersonIdsByPlace(place)).andReturn(Collections.<UUID>singleton(person.getId()));
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();

      try {
         pinManager.validatePin(place, new byte[] { ONE, ONE, ONE, TWO});
      } catch(ErrorEventException eee) {
         assertEquals("InvalidPin", eee.getCode());
      }
   }

   @Test
   public void testAccumulatePin() {
      person.setPinAtPlace(place, "1111");
      EasyMock.expect(personPlaceAssocDao.findPersonIdsByPlace(place)).andReturn(Collections.<UUID>singleton(person.getId()));
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();

      UUID personId = null;

      while(personId == null) {
         personId = pinManager.accumulatePin(place, ONE);
      }
      assertEquals(person.getId(), personId);
   }

   @Test
   public void testAccumulatePinInvalid() {
      person.setPinAtPlace(place, "1111");
      EasyMock.expect(personPlaceAssocDao.findPersonIdsByPlace(place)).andReturn(Collections.<UUID>singleton(person.getId()));
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();

      try {
         for(int i = 0; i < 4; i++) {
            pinManager.accumulatePin(place, ZERO + i);
         }
      } catch(ErrorEventException eee) {
         assertEquals("InvalidPin", eee.getCode());
      }
   }

   @Test
   public void testAccumulatePinInvalidDigit() {
      replay();
      try {
         pinManager.accumulatePin(place, 10);
      } catch(ErrorEventException eee) {
         assertEquals("InvalidPin", eee.getCode());
      }
   }

   @Test
   public void testAccumulateResets() throws Exception {
      person.setPinAtPlace(place, "0123");
      EasyMock.expect(personPlaceAssocDao.findPersonIdsByPlace(place)).andReturn(Collections.<UUID>singleton(person.getId()));
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      replay();

      for(int i = 0; i < 2; i++) {
         pinManager.accumulatePin(place, ZERO + i);
      }
      Thread.sleep(TIMEOUT + 10);

      UUID personId = null;

      for(int i = 0; i < 4; i++) {
         personId = pinManager.accumulatePin(place, ZERO + i);
      }
      assertEquals(person.getId(), personId);
   }

}

