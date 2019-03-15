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
/**
 *
 */
package com.iris.client.proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.capability.Account;
import com.iris.client.capability.Account.ListDevicesResponse;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Device;
import com.iris.client.capability.DevicePower;
import com.iris.client.capability.Place;
import com.iris.client.capability.Place.RegisterHubResponse;
import com.iris.client.capability.Switch;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.model.AccountModel;
import com.iris.client.model.DeviceModel;
import com.iris.client.model.Model;
import com.iris.client.model.PlaceModel;
import com.iris.client.model.proxy.ProxyModelFactory;

/**
 *
 */
public class TestProxyModelFactory extends Assert {
   IrisClient client;
   ProxyModelFactory factory;

   @Before
   public void setUp() {
      client = EasyMock.createMock(IrisClient.class);
      factory = new ProxyModelFactory(client);
   }

   protected void replay() {
      EasyMock.replay(client);
   }

   protected void verify() {
      EasyMock.verify(client);
   }

   @Test
   public void testRawModel() {
      replay();
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(Capability.ATTR_ID, "id");
      attributes.put(Capability.ATTR_TYPE, "test");
      attributes.put(Capability.ATTR_ADDRESS, "SERV:test:id");
      attributes.put("vendor:id", "arcus");

      Model m = factory.create(attributes, Collections.<Class<? extends Capability>>emptyList());

      assertEquals("id", m.getId());
      assertEquals("test", m.getType());
      assertEquals("SERV:test:id", m.getAddress());

      assertEquals("arcus", m.get("vendor:id"));

      assertEquals(attributes, m.toMap());

      // test that object functions don't fail
      m.hashCode();
      System.out.println(m.toString());
      // weird side-affect, equals has to be specially handled
      assertEquals(m, m);

      verify();
   }

   @Test
   public void testTypedCapability() {
      replay();
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(Capability.ATTR_ID, "id");
      attributes.put(Capability.ATTR_TYPE, "test");
      attributes.put(Capability.ATTR_ADDRESS, "SERV:test:id");
      attributes.put(Account.ATTR_STATE, "registered");

      Model m = factory.create(attributes, Arrays.<Class<? extends Capability>>asList(Account.class));

      assertEquals("id", m.getId());
      assertEquals("test", m.getType());
      assertEquals("SERV:test:id", m.getAddress());

      Account a = (Account) m;
      assertEquals("registered", a.getState());

      a.setState("new");

      assertTrue(m.isDirty(Account.ATTR_STATE));

      verify();
   }

   @Test
   public void testTypedModel() {
      replay();
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(Capability.ATTR_ID, "id");
      attributes.put(Capability.ATTR_TYPE, "test");
      attributes.put(Capability.ATTR_ADDRESS, "SERV:test:id");
      attributes.put(Account.ATTR_STATE, "registered");

      AccountModel m = factory.create(attributes, AccountModel.class);

      assertEquals("id", m.getId());
      assertEquals("test", m.getType());
      assertEquals("SERV:test:id", m.getAddress());
      assertEquals("registered", m.getState());

      m.setState("new");

      assertTrue(m.isDirty(Account.ATTR_STATE));

      verify();
   }

   @Test
   public void testMultiModel() {
      replay();
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(Capability.ATTR_ID, "id");
      attributes.put(Capability.ATTR_TYPE, "test");
      attributes.put(Capability.ATTR_ADDRESS, "SERV:test:id");
      attributes.put(Capability.ATTR_CAPS, Arrays.asList("cont", "swit"));
      attributes.put(Device.ATTR_MODEL, "model");
      attributes.put(Device.ATTR_VENDOR, "vendor");
      attributes.put(Device.ATTR_DEVTYPEHINT, "cont");
      attributes.put(DevicePower.ATTR_BATTERY, 100);
      attributes.put(DevicePower.ATTR_LINECAPABLE, false);
      attributes.put(DevicePower.ATTR_SOURCE, DevicePower.SOURCE_BATTERY);
      attributes.put(Switch.ATTR_STATE, Switch.STATE_ON);

      DeviceModel m = factory.create(attributes, DeviceModel.class, Arrays.<Class<? extends Capability>>asList(DevicePower.class, Switch.class));

      assertEquals("id", m.getId());
      assertEquals("test", m.getType());
      assertEquals("SERV:test:id", m.getAddress());

      DevicePower p = (DevicePower) m;
      assertEquals(Integer.valueOf(100), p.getBattery());
      assertEquals(false, p.getLinecapable());
      assertEquals("BATTERY", p.getSource());

      Switch s = (Switch) m;
      assertEquals("ON", s.getState());

      s.setState(Switch.STATE_OFF);

      assertTrue(m.isDirty(Switch.ATTR_STATE));

      verify();
   }

   @Test
   @Ignore
   public void testRequestWithNoInput() throws Exception {
      SettableClientFuture<ClientEvent> expected = new SettableClientFuture<>();
      expected.setValue(new ClientEvent("account:ListDevicesResponse", "SERV:account:id"));

      ClientRequest request = new ClientRequest();
      request.setAddress("SERV:test:id");
      request.setCommand(Account.CMD_LISTDEVICES);
      EasyMock
         .expect(client.request(request))
         .andReturn(expected)
         ;

      replay();
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(Capability.ATTR_ID, "id");
      attributes.put(Capability.ATTR_TYPE, "test");
      attributes.put(Capability.ATTR_ADDRESS, "SERV:test:id");
      attributes.put(Account.ATTR_STATE, "registered");

      AccountModel m = factory.create(attributes, AccountModel.class);

      ClientFuture<ListDevicesResponse> actual = m.listDevices();
      assertNotNull(actual.get());
      assertTrue(actual.get() instanceof ListDevicesResponse);

      verify();
   }

   @Test
   @Ignore // Attribute map coming up null - working on
   public void testRequestWithInput() throws Exception {
      SettableClientFuture<ClientEvent> response = new SettableClientFuture<>();
      response.setValue(new ClientEvent("place:RegisterHubResponse", "SERV:place:id"));

      ClientRequest request = new ClientRequest();
      request.setAddress("SERV:place:id");
      request.setCommand(Place.CMD_REGISTERHUB);
      request.setAttribute("hubId", "ABC-1234");
      EasyMock
         .expect(client.request(request))
         .andReturn(response)
         ;

      replay();
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(Capability.ATTR_ID, "id");
      attributes.put(Capability.ATTR_TYPE, Place.NAMESPACE);
      attributes.put(Capability.ATTR_ADDRESS, "SERV:place:id");

      PlaceModel m = factory.create(attributes, PlaceModel.class);

      ClientFuture<RegisterHubResponse> actual = m.registerHub("ABC-1234");
      assertTrue(actual.get() instanceof RegisterHubResponse);

      verify();
   }

}

