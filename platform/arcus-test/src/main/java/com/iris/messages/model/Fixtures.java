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
package com.iris.messages.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ClientAddress;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.type.CardPreference;
import com.iris.messages.type.Population;
import com.iris.messages.type.Preferences;
import com.iris.model.Version;

import junit.framework.AssertionFailedError;

public class Fixtures {

   public final static String DEVICE_PROTOCOL_NAME = "Ipcd";
   public final static String DEVICE_PROTOCOL_ID = "Blackbox_Switch1_123456789";
   private static final AtomicInteger SESSION_ID = new AtomicInteger();
   public static final UUID placeId = UUID.randomUUID();
   private static Random rand = new Random(System.currentTimeMillis());
   
   private Fixtures() {}
   
   public static Population createPopulation() {
      Population population = new Population();
      population.setName("TestPopulation"+rand.nextInt());
      population.setDescription(population.getName() + " description");
      return population;
   }

   public static Account createAccount() {

      Set<String> tags = new HashSet<String>();
      tags.add("tag1");
      tags.add("tag2");

      Account account = new Account();
      account.setBillable(false);
      account.setTags(tags);
      account.setState("activated");
      account.setTaxExempt(false);
      account.setBillingCCLast4("1234");
      account.setBillingCCType("Diner's Club");
      account.setBillingCity("Lawrence");
      account.setBillingFirstName("John");
      account.setBillingLastName("Doe");
      account.setBillingState("KS");
      account.setBillingStreet1("1611 St. Andrews Dr.");
      account.setBillingZip("66047");
      account.setBillingZipPlusFour("1701");

      return account;
   }

   public static Account createAccountWithServiceLevelAndPlaces() {

      Set<String> tags = new HashSet<String>();
      tags.add("tag1");
      tags.add("tag2");

      Set<UUID> placeIDs = new HashSet<>();
      placeIDs.add(UUID.randomUUID());
      placeIDs.add(UUID.randomUUID());
      placeIDs.add(UUID.randomUUID());

      Map<ServiceLevel, String> subscriptionIDs = new HashMap<>();
      subscriptionIDs.put(ServiceLevel.BASIC,   "subscriptionIDForBASIC");
      subscriptionIDs.put(ServiceLevel.PREMIUM, "subscriptionIDForPREMIUM");

      Account account = new Account();
      account.setBillable(false);
      account.setTags(tags);
      account.setState("activated");
      account.setTaxExempt(false);
      account.setBillingCCLast4("1234");
      account.setBillingCCType("Diner's Club");
      account.setBillingCity("Lawrence");
      account.setBillingFirstName("John");
      account.setBillingLastName("Doe");
      account.setBillingState("KS");
      account.setBillingStreet1("1611 St. Andrews Dr.");
      account.setBillingZip("66047");
      account.setBillingZipPlusFour("1701");
      account.setPlaceIDs(placeIDs);
      account.setSubscriptionIDs(subscriptionIDs);


      return account;
   }

   public static Person createPerson() {
      Set<String> tags = new HashSet<String>();
      tags.add("tag1");
      tags.add("tag2");

      List<String> notificationEndpoints = new ArrayList<String>();
      notificationEndpoints.add("endpoint1");
      notificationEndpoints.add("endpoint2");

      Person person = new Person();
      person.setAccountId(UUID.randomUUID());
      person.setFirstName("Hu");
      person.setLastName("Man");
      person.setImages(Collections.<String,UUID>singletonMap("avatar", UUID.randomUUID()));
      person.setTags(tags);
      person.setEmail("rob@foobar.com");
      person.setEmailVerified(new Date());
      person.setMobileNumber("3125551234");
      person.setMobileVerified(new Date());
      person.setMobileNotificationEndpoints(notificationEndpoints);
      person.setCurrPlace(UUID.randomUUID());
      person.setCurrPlaceMethod("geofencing");
      person.setCurrLocation("lat, long");
      person.setCurrLocationTime(new Date());
      person.setCurrPlaceMethod("gps");
      person.setSecurityAnswers(ImmutableMap.of(
            "question1", "question 1 answer",
            "question2", "question 2 answer",
            "question3", "question 3 answer"));
      person.setEmailVerificationToken("12345");

      return person;
   }

   public static Place createPlace() {
      Set<String> serviceAddons = new HashSet<String>();
      serviceAddons.add("addon1");
      serviceAddons.add("addon2");

      Map<String,UUID> images = new HashMap<>();
      images.put("icon", UUID.randomUUID());
      images.put("picture", UUID.randomUUID());

      Set<String> tags = new HashSet<String>();
      tags.add("tag1");
      tags.add("tag2");

      Place place = new Place();
      place.setPopulation(Population.NAME_GENERAL);
      place.setAccount(UUID.randomUUID());
      place.setName("Home");
      place.setImages(images);
      place.setTags(tags);
      place.setState("home");
      place.setStreetAddress1("1651 Naismith Drive");
      place.setStreetAddress2("Suite 1000");
      place.setCity("Lawrence");
      place.setStateProv("KS");
      place.setZipCode("66044");
      place.setZipPlus4("4069");
      place.setTzName("Central");
      place.setTzOffset(-6.0);
      place.setTzUsesDST(true);
      place.setAddrType("S");
      place.setAddrZipType("Unique");
      place.setAddrLatitude(38.9543d);
      place.setAddrLongitude(-95.25134d);
      place.setAddrGeoPrecision("Zip9");
      place.setAddrRDI("Commercial");
      place.setAddrCounty("Douglas");
      place.setAddrCountyFIPS("20045");
      place.setCountry("US");
      place.setAddrValidated(true);

      place.setServiceLevel(ServiceLevel.PREMIUM);
      place.setServiceAddons(serviceAddons);

      return place;
   }

   public static Device createDevice() {
      return createDevice(DeviceCapability.NAMESPACE);
   }

   public static Device createDevice(String... caps) {
      return createDevice(Arrays.asList(caps));
   }

   public static Device createDevice(Collection<String> caps) {
      Device device = new Device();
      device.setId(UUID.randomUUID());
      device.setAccount(UUID.randomUUID());
      device.setName("Test Device");
      device.setDriverId(new DriverId("foo", new Version(1)));
      device.setProtocol(DEVICE_PROTOCOL_NAME);
      device.setProtocolid(ProtocolDeviceId.hashDeviceId(DEVICE_PROTOCOL_ID).getRepresentation());
      device.setAddress(Address.platformDriverAddress(device.getId()).getRepresentation());
      device.setProtocolAddress(Address.protocolAddress(DEVICE_PROTOCOL_NAME, ProtocolDeviceId.hashDeviceId(DEVICE_PROTOCOL_ID)).getRepresentation());
      device.setCaps(new HashSet<String>(caps));
      return device;
   }

   public static Hub createHub() {
	   Hub hub = new Hub();
	   hub.setId("PIE-1234");
	   hub.setTags(new HashSet<String>());
	   hub.setState(Hub.STATE_CREATED);
	   hub.setAccount(UUID.randomUUID());
	   hub.setPlace(UUID.randomUUID());
	   hub.setCaps(new HashSet<String>());
	   hub.setName("My hoob");
	   hub.setImages(Collections.<String,UUID>singletonMap("icon", UUID.randomUUID()));
	   hub.setVendor("CentraLite");
	   hub.setModel("IH200");
	   hub.setSerialNum("99959595");
	   hub.setHardwarever("2.0");
	   hub.setMac("aa:bb:cc:dd:ee:ff");
	   hub.setMfgInfo("12346483201791234");
	   hub.setFirmwareGroup("main");
	   hub.setOsver("3.18.29");
	   hub.setAgentver("1.24");
	   hub.setBootloaderVer("1.1");
	   return hub;
   }

   public static Map<String, Object> createPreferences() {
      Map<String, Object> prefs = new HashMap<>();

      prefs.put(Preferences.ATTR_HIDETUTORIALS, true);

      List<Map<String, Object>> dashboardCards = new ArrayList<>();

      dashboardCards.add(ImmutableMap.of(
         CardPreference.ATTR_SERVICENAME, CardPreference.SERVICENAME_LIGHTS_N_SWITCHES,
         CardPreference.ATTR_HIDECARD, true));

      dashboardCards.add(ImmutableMap.of(
         CardPreference.ATTR_SERVICENAME, CardPreference.SERVICENAME_CLIMATE,
         CardPreference.ATTR_HIDECARD, false));

      prefs.put(Preferences.ATTR_DASHBOARDCARDS, dashboardCards);

      return prefs;
   }

   public static DeviceDriverAddress createDeviceAddress() {
      return Address.platformDriverAddress(UUID.randomUUID());
   }

   public static DeviceProtocolAddress createProtocolAddress() {
      return createProtocolAddress("IPCD");
   }

   public static DeviceProtocolAddress createProtocolAddress(String protocol) {
      return Address.protocolAddress(protocol, "device-" + SESSION_ID.incrementAndGet());
   }

   public static ClientAddress createClientAddress() {
      return Address.clientAddress("test-server", "session-" + SESSION_ID.incrementAndGet());
   }

   public static Address createObjectAddress(String objectType) {
      return Address.platformService(UUID.randomUUID(), objectType);
   }

   public static void assertMapEquals(Map<?, ?> expected, Map<?, ?> actual) {
      StringBuilder errors = new StringBuilder();
      for(Map.Entry<?, ?> e: expected.entrySet()) {
         Object expectedValue = e.getValue();
         Object actualValue = actual.get(e.getKey());
         if(!Objects.equal(expectedValue, actualValue)) {
            if(errors.length() > 0) {
               errors.append("\n");
            }
            errors.append("Expected: ")
               .append(e.getKey())
               .append("=")
               .append(expectedValue)
               .append(" but was: ")
               .append(actualValue);
         }
      }
      for(Object key: actual.keySet()) {
         if(!expected.containsKey(key)) {
            if(errors.length() > 0) {
               errors.append("\n");
            }
            errors.append("Unexpected key: " + key + " with value: " + actual.get(key));
         }
      }
      if(errors.length() != 0) {
         throw new AssertionFailedError(errors.toString());
      }
   }
   
   public static PlatformMessage createAddedMessage(
         Address address,
         Map<String, Object> attributes
   ) {
      return newAddedMessageBuilder(UUID.randomUUID(), address, attributes).create();
   }

   public static PlatformMessage createAddedMessage(
   		UUID placeId,
         Address address,
         Map<String, Object> attributes
   ) {
      return newAddedMessageBuilder(placeId, address, attributes).create();
   }
   
   public static PlatformMessage createDeletedMessage(
         Address address,
         Map<String, Object> attributes
   ) {
      return newDeletedMessageBuilder(UUID.randomUUID(), address, attributes).create();
   }

   public static PlatformMessage createGetAttributes(UUID devId, String... names) {
      ImmutableSet.Builder<String> namesBuilder = ImmutableSet.builder();
      if(names != null) {
         namesBuilder.add(names);
      }

      MessageBody body = Capability.GetAttributesRequest.builder()
         .withNames(names == null ? null : namesBuilder.build())
         .build();
      return PlatformMessage.buildMessage(
            body,
            Address.clientAddress("android", "1"),
            Address.platformDriverAddress(devId))
            .create();
   }  

   public static PlatformMessage.Builder newAddedMessageBuilder(
   		UUID placeId,
         Address address,
         Map<String, Object> attributes
   ) {
      return newBroadcastMessageBuilder(placeId, address, Capability.EVENT_ADDED, attributes);
   }
   
   public static PlatformMessage.Builder newDeletedMessageBuilder(
   		UUID placeId,
         Address address,
         Map<String, Object> attributes
   ) {
      return newBroadcastMessageBuilder(placeId, address, Capability.EVENT_DELETED, attributes);
   }
   
   public static PlatformMessage createValueChangeMessage(Address address, Map<String, Object> attributes) {
      return newValueChangeBuilder(UUID.randomUUID(), address, attributes).create();
   }

   public static PlatformMessage createValueChangeMessage(UUID placeId, Address address, Map<String, Object> attributes) {
      return newValueChangeBuilder(placeId, address, attributes).create();
   }

   public static PlatformMessage.Builder newValueChangeBuilder(UUID placeId, Address address, Map<String, Object> attributes) {
      return newBroadcastMessageBuilder(placeId, address, Capability.EVENT_VALUE_CHANGE, attributes);
   }
   
   public static PlatformMessage createDeletedMessage(Address address) {
      return newDeletedBuilder(UUID.randomUUID(), address).create();
   }

   public static PlatformMessage createDeletedMessage(UUID placeId, Address address) {
      return newDeletedBuilder(placeId, address).create();
   }

   public static PlatformMessage.Builder newDeletedBuilder(UUID placeId, Address address) {
      return newBroadcastMessageBuilder(placeId, address, Capability.EVENT_DELETED, ImmutableMap.of());
   }

   public static PlatformMessage.Builder newBroadcastMessageBuilder(
   		UUID placeId,
         Address address,
         String eventType,
         Map<String, Object> attributes
   ) {
      return
            PlatformMessage
               .builder()
               .withPlaceId(placeId)
               .from(address)
               .broadcast()
               .withPayload(eventType, attributes)
               ;
   }

}

