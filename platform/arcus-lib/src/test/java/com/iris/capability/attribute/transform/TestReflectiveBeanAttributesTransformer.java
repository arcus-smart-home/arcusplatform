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
package com.iris.capability.attribute.transform;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.model.Hub;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.services.PlatformConstants;
import com.iris.model.Version;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules(AttributeMapTransformModule.class)
public class TestReflectiveBeanAttributesTransformer extends IrisTestCase {

   @Inject private BeanAttributesTransformer<Account> accountTransformer;
   @Inject private BeanAttributesTransformer<Place> placeTransformer;
   @Inject private BeanAttributesTransformer<Person> personTransformer;
   @Inject private BeanAttributesTransformer<Device> deviceTransformer;
   @Inject private BeanAttributesTransformer<Hub> hubTransformer;
   @Inject private BeanAttributesTransformer<MobileDevice> mobileDeviceTransformer;

   @Test
   public void testAccountTransformations() throws Exception {
      Account account = new Account();
      account.setId(UUID.randomUUID());
      account.setState("state1");
      account.setImages(Collections.<String,UUID>singletonMap("icon", UUID.randomUUID()));
      account.setTags(Collections.<String>singleton("tag"));
      account.setBillingCCLast4("1234");
      account.setBillingCCType("Diner's Club");
      account.setBillingCity("Lawrence");
      account.setBillingFirstName("John");
      account.setBillingLastName("Doe");
      account.setBillingState("KS");
      account.setBillingStreet1("1611 St. Andrews Dr.");
      account.setBillingZip("66047");
      account.setBillingZipPlusFour("1701");
      account.setTaxExempt(true);

      Map<String,Object> attributes = accountTransformer.transform(account);
      assertEquals(PlatformConstants.SERVICE_ACCOUNTS, attributes.get("base:type"));
      assertEquals(MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_ACCOUNTS + ":" + account.getId(), attributes.get("base:address"));

      Account fromAttrs = accountTransformer.transform(attributes);
      assertEquals(account, fromAttrs);
   }

   @Test
   public void testPlaceTransformations() throws Exception {
      Place place = new Place();
      place.setAccount(UUID.randomUUID());
      place.setId(UUID.randomUUID());
      place.setImages(Collections.<String,UUID>singletonMap("icon", UUID.randomUUID()));
      place.setName("foobar");
      place.setState("away");
      place.setTags(Collections.<String>singleton("tag"));
      place.setStreetAddress1("1651 Naismith Drive");
      place.setCity("Lawrence");
      place.setStateProv("KS");
      place.setZipCode("66044");
      place.setServiceLevel(ServiceLevel.BASIC);
      place.setServiceAddons(Collections.singleton("foobarService"));

      Map<String,Object> attributes = placeTransformer.transform(place);

      assertEquals(PlatformConstants.SERVICE_PLACES, attributes.get("base:type"));
      assertEquals(MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_PLACES + ":" + place.getId(), attributes.get("base:address"));

      Place fromAttrs = placeTransformer.transform(attributes);
      assertEquals(place, fromAttrs);
   }

   @Test
   public void testPersonTransformations() throws Exception {
      Person person = createPerson();

      Map<String,Object> attributes = personTransformer.transform(person);

      assertEquals(PlatformConstants.SERVICE_PEOPLE, attributes.get("base:type"));
      assertEquals(MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_PEOPLE + ":" + person.getId(), attributes.get("base:address"));

      Person fromAttrs = personTransformer.transform(attributes);
      assertTrue(fromAttrs.getEmailVerified() == null);  //because it's readonly, so can not be set      
      person.setEmailVerified(null);
      assertEquals(person, fromAttrs);
   }
   
   @Test
   public void testPersonTransformationsWithSecurityAnswers() throws Exception {
   	Map<String, String> securityAnswers = ImmutableMap.<String, String>of("question1", "answer1", "question2", "answer2");
      Person person = createPerson();
      person.setSecurityAnswers(securityAnswers);

      Map<String,Object> attributes = personTransformer.transform(person);
      assertEquals(securityAnswers.size(), attributes.get(PersonCapability.ATTR_SECURITYANSWERCOUNT));
   }
   
   private Person createPerson() {
   	Person person = new Person();
      person.setId(UUID.randomUUID());
      person.setFirstName("foo");
      person.setLastName("bar");
      person.setEmail("foobar@foo.com");
      person.setImages(Collections.<String,UUID>singletonMap("icon", UUID.randomUUID()));
      person.setMobileNumber("555-555-5555");
      person.setTags(Collections.<String>singleton("tag"));
      person.setEmailVerified(new Date());
      return person;
   }

   @Test
   public void testPersonPin() throws Exception {
      Person person = new Person();
      person.setId(UUID.randomUUID());
      person.setCurrPlace(UUID.randomUUID());

      Map<String,Object> attributes = personTransformer.transform(person);
      assertEquals(Boolean.FALSE, attributes.get(PersonCapability.ATTR_HASPIN));

      person.setPinAtPlace(person.getCurrPlace(), "1111");

      attributes = personTransformer.transform(person);
      assertEquals(Boolean.TRUE, attributes.get(PersonCapability.ATTR_HASPIN));
   }

   @Test
   public void testDeviceTransformations() throws Exception {
      Device d = new Device();
      d.setId(UUID.randomUUID());
      d.setAccount(UUID.randomUUID());
      d.setPlace(UUID.randomUUID());
      d.setCaps(new HashSet<String>(Arrays.asList("dev", "devadv", "swit", "base")));
      d.setDevtypehint("swit");
      d.setName("foobar");
      d.setImages(Collections.<String,UUID>singletonMap("icon", UUID.randomUUID()));
      d.setVendor("vendor");
      d.setModel("model");
      d.setDrivername("foobardriver");
      d.setAddress(Address.platformDriverAddress(d.getId()).getRepresentation());
      d.setDriverId(new DriverId("foobardriver", Version.fromRepresentation("1.0")));
      d.setProtocol("zwave");
      d.setSubprotocol("zwave subprotocol");
      d.setProtocolid("the protocol id");
      d.setTags(Collections.<String>singleton("tag"));
      d.setCreated(new Date());

      Map<String,Object> attributes = deviceTransformer.transform(d);
      assertEquals("dev", attributes.get("base:type"));
      assertEquals(MessageConstants.DRIVER + ":dev:" + d.getId(), attributes.get("base:address"));

      Device fromAttrs = deviceTransformer.transform(attributes);
      assertEquals(d, fromAttrs);
   }

   @Test
   public void testHubTransformations() throws Exception {
      Hub h = new Hub();
      h.setAccount(UUID.randomUUID());
      h.setAgentver("1.0");
      h.setBootloaderVer("1.0");
      h.setCaps(new HashSet<>(Arrays.asList("hub", "hubadv", "base")));
      h.setFirmwareGroup("firmwareGroup");
      h.setHardwarever("1.0");
      h.setId("ABC-1234");
      h.setImages(Collections.<String,UUID>singletonMap("icon", UUID.randomUUID()));
      h.setMac("00:00:00:00:00:00");
      h.setMfgInfo("mfgInfo");
      h.setModel("model");
      h.setName("hub");
      h.setOsver("1.0");
      h.setPlace(UUID.randomUUID());
      h.setSerialNum("1234");
      h.setState("NORMAL");
      h.setVendor("vendor");
      h.setTags(Collections.<String>singleton("tag"));

      Map<String,Object> attributes = hubTransformer.transform(h);
      assertEquals(PlatformConstants.SERVICE_HUB, attributes.get("base:type"));
      assertEquals(MessageConstants.SERVICE + ":" + h.getId() + ":" + PlatformConstants.SERVICE_HUB, attributes.get("base:address"));

      Hub fromAttrs = hubTransformer.transform(attributes);
      assertEquals(h, fromAttrs);
   }

   @Test
   public void testFiltering() throws Exception {
      Map<String,Object> attributes = new LinkedHashMap<>();
      attributes.put("hub:state", "NORMAL");
      attributes.put("hubzigbee:state", "UP");

      Hub fromAttrs = hubTransformer.transform(attributes);
      assertEquals("NORMAL", fromAttrs.getState());

      attributes.clear();
      // reorder
      attributes.put("hubzigbee:state", "UP");
      attributes.put("hub:state", "NORMAL");

      fromAttrs = hubTransformer.transform(attributes);
      assertEquals("NORMAL", fromAttrs.getState());
   }

   @Test
   public void testMobileDeviceTransformations() throws Exception {
      MobileDevice m = new MobileDevice();
      m.setAssociated(new Date());
      m.setDeviceIdentifier("deviceidentifier");
      m.setDeviceIndex(1);
      m.setDeviceModel("deviceModel");
      m.setDeviceVendor("deviceVendor");
      m.setFormFactor("phone");
      m.setLastLatitude(38.97);
      m.setLastLongitude(95.23);
      m.setLastLocationTime(new Date());
      m.setNotificationToken("token");
      m.setOsType("android");
      m.setOsVersion("kitkat");
      m.setPersonId(UUID.randomUUID());
      m.setPhoneNumber("555-555-5555");
      // fix
      m.setResolution("1024 by 768");
      // adding new fields
      m.setName("iPad");
      m.setAppVersion("UNKNOWN");

      Map<String,Object> attributes = mobileDeviceTransformer.transform(m);
      assertEquals(PlatformConstants.SERVICE_MOBILEDEVICES, attributes.get("base:type"));
      assertEquals(MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_MOBILEDEVICES + ":" + m.getId(), attributes.get("base:address"));

      MobileDevice fromAttrs = mobileDeviceTransformer.transform(attributes);
      assertEquals(m, fromAttrs);
   }

   @Test
   public void testMobileDeviceMissingNameTransformations() throws Exception {
      MobileDevice m = new MobileDevice();
      m.setAssociated(new Date());
      m.setDeviceIdentifier("deviceidentifier");
      m.setDeviceIndex(1);
      m.setDeviceModel("deviceModel");
      m.setDeviceVendor("Apple");
      m.setFormFactor("phone");
      m.setLastLatitude(38.97);
      m.setLastLongitude(95.23);
      m.setLastLocationTime(new Date());
      m.setNotificationToken("token");
      m.setOsType("android");
      m.setOsVersion("kitkat");
      m.setPersonId(UUID.randomUUID());
      m.setPhoneNumber("555-555-5555");
      // fix
      m.setResolution("1024 by 768");
      // adding new fields
      // MIssing!
      m.setName("");
      m.setAppVersion("UNKNOWN");

      Map<String,Object> attributes = mobileDeviceTransformer.transform(m);
      MobileDevice fromAttrs = mobileDeviceTransformer.transform(attributes);      
      assertEquals(fromAttrs.getName(), MobileDeviceAttributesTransformer.AppleDeviceType.IPHONE.getFriendlyName());
   }

   @Test
   public void testMobileDeviceMissingAppVersionTransformations() throws Exception {
      MobileDevice m = new MobileDevice();
      m.setAssociated(new Date());
      m.setDeviceIdentifier("deviceidentifier");
      m.setDeviceIndex(1);
      m.setDeviceModel("deviceModel");
      m.setDeviceVendor("Apple");
      m.setFormFactor("tablet");
      m.setLastLatitude(38.97);
      m.setLastLongitude(95.23);
      m.setLastLocationTime(new Date());
      m.setNotificationToken("token");
      m.setOsType("android");
      m.setOsVersion("kitkat");
      m.setPersonId(UUID.randomUUID());
      m.setPhoneNumber("555-555-5555");
      // fix
      m.setResolution("1024 by 768");
      // adding new fields
      m.setName("test");
      // missing! 
      m.setAppVersion("");

      Map<String,Object> attributes = mobileDeviceTransformer.transform(m);
      MobileDevice fromAttrs = mobileDeviceTransformer.transform(attributes);      
      assertEquals(fromAttrs.getAppVersion(), "UNKNOWN");
   }     
      
 
}

