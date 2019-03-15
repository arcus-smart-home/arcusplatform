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
package com.iris.platform.services.account.handlers;

import static com.iris.billing.client.model.Constants.STATE_ACTIVE;
import static com.iris.billing.client.model.Constants.STATE_EXPIRED;
import static com.iris.messages.model.ServiceLevel.BASIC;
import static com.iris.messages.model.ServiceLevel.PREMIUM;
import static com.iris.messages.model.ServiceLevel.PREMIUM_ANNUAL;
import static com.iris.messages.model.ServiceLevel.PREMIUM_FREE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.billing.client.model.Subscription;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.notification.ServiceUpateNotifications;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceAddon;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.services.BillingTestCase;
import com.iris.platform.subscription.SubscriptionUpdater;
import com.iris.platform.subscription.SubscriptionUpdaterImpl;
import com.iris.test.Modules;
import com.iris.util.IrisCollections;

@Modules({InMemoryMessageModule.class})
public class TestUpdateServicePlanHandler extends BillingTestCase {
   private enum AddonType {
      ONLY_TRUE,  // Send only listed addons as true.
      ONLY_FALSE, // Send only listed addons as false.
      ALL_TRUE,   // Send listed addons as true, send all others as false.
      ALL_FALSE } // Send listed addons as false, send all others as true.
   private final String[] aoCare1 = { ServiceAddon.CELLBACKUP.name() };

   private final String[] aoNone = {};

   @Inject
   private UpdateServicePlanHandler handler;

   @Provides
   @Singleton
   public SubscriptionUpdater provideSubscriptionUpdater() {
      return new SubscriptionUpdaterImpl(accountDao, placeDao, client, subManager,platformBus);
   }

   @Override
   public void setUp() throws Exception {
      super.setUp();
      initData();
   }

   @Test
   public void testInitialConditions() throws Exception {
      // If this fails, then the test isn't getting set up correctly.
      verifyInitialSetup();
   }

   @Test
   public void testUpdateAccount() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
   }

   @Test
   public void testAddAddonSpecifyAll() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);

      // Add an additional addon
      String[] addons = {};

      handleMsg(firstPlace, PREMIUM, AddonType.ALL_TRUE, addons);
      verifyPlace(firstPlace.getId(), PREMIUM, addons);

      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, addons, 1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),addons);
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   //TODO - put the test back in after the 2nd Add-on is added to ServiceAddon
   //@Test
   public void testAddAddonSpecifyOne() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);

      String[] newAddOn = {};

      handleMsg(firstPlace, PREMIUM, AddonType.ONLY_TRUE, newAddOn);

      String[] addons = aoCare1;
      verifyAccount(account.getId());
      verifyPlace(firstPlace.getId(), PREMIUM, addons);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, aoNone, 1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM.toString());
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   //TODO - put the test back in after the 2nd Add-on is added to ServiceAddon
   //@Test
   public void testAddAllAddons() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);

      String[] addons = aoCare1;

      handleMsg(firstPlace, PREMIUM, AddonType.ONLY_TRUE, aoNone);
      verifyAccount(account.getId());
      verifyPlace(firstPlace.getId(), PREMIUM, addons);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, addons, 1, 1, 1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM.toString());
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testTwoPlacesBothPremium() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);

      // Create second place
      Place secondPlace = createPlace(account.getId(), UUID.randomUUID(), "Second Place");;
      handleMsg(secondPlace, PREMIUM, AddonType.ALL_TRUE, aoCare1);

      // Check account
      verifyAccount(account.getId());

      // Check first place.
      verifyPlace(firstPlace.getId(), PREMIUM, aoCare1);

      // Check second place
      verifyPlace(secondPlace.getId(), PREMIUM, aoCare1);

      // Check subscriptions. Should be two. Expired basic and active premium.
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      String[] addons = {
            ServiceAddon.CELLBACKUP.name()
      };
      verifySubscription(PREMIUM, STATE_ACTIVE, 2, addons, 2, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM.toString(), ImmutableSet.of(ServiceAddon.CELLBACKUP.name()));

      verifyNotification(platformBus.take(), account.getOwner(), secondPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testThreePlacesOneBasicTwoPremium() throws Exception {
      createAndVerifyTwoPremiumAndOneBasic();
   }

   @Test
   public void testSwitchBasicToPremiumWithOnePlace() throws Exception {
      updateOnePlaceAndVerify(BASIC);
      handleMsg(firstPlace, PREMIUM, AddonType.ALL_TRUE, aoCare1);

      verifyPlace(firstPlace.getId(), PREMIUM, aoCare1);
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      // Check for expired basic subscription
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoCare1, 1, 1);
      // Check for active premium subscription
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, aoCare1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM.toString());
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }
   
   @Test
   public void testSwitchBasicToPremiumAnnualWithOnePlace() throws Exception {
      updateOnePlaceAndVerify(BASIC);
      handleMsg(firstPlace, PREMIUM_ANNUAL, AddonType.ALL_TRUE, aoCare1);

      verifyPlace(firstPlace.getId(), PREMIUM_ANNUAL, aoCare1);
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      // Check for expired basic subscription
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoCare1, 1, 1);
      // Check for active premium subscription
      verifySubscription(PREMIUM_ANNUAL, STATE_ACTIVE, 1, aoCare1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM_ANNUAL.toString());
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testSwithBasicToPremiumWithThreePlaces() throws Exception {
      List<Place> places = createAndVerifyTwoPremiumAndOneBasic();
      handleMsg(places.get(2), PREMIUM, AddonType.ALL_TRUE, aoCare1);
      // Verify the account
      verifyAccount(account.getId());

      // Verify all the places just in case.
      verifyPlace(places.get(0).getId(), PREMIUM, aoCare1);
      verifyPlace(places.get(1).getId(), PREMIUM, aoCare1);
      verifyPlace(places.get(2).getId(), PREMIUM, aoCare1);

      // Verify subscriptions. Should be three. Two expired basics and one active premium.
      Assert.assertEquals(3, billingAccount.getSubscriptions().size());
      // Subscription from initial conditions.
      verifySubscription(0, BASIC, STATE_EXPIRED, 1, aoNone);
      // Check for expired basic subscription
      verifySubscription(1, BASIC, STATE_EXPIRED, 1, aoCare1, 1, 1);
      // Check for updated premium subscription
      String[] addons = aoCare1;
      verifySubscription(PREMIUM, STATE_ACTIVE, 3, addons, 3, 1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM.toString());
      verifyNotification(platformBus.take(), account.getOwner(), places.get(2));
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testRemoveAddOn() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      String[] addons = {};
      handleMsg(firstPlace, PREMIUM, AddonType.ALL_TRUE, addons);

      // Verify place
      verifyPlace(firstPlace.getId(), PREMIUM, addons);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, addons, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(), ImmutableSet.of());
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testRemoveAddOnSpecifyOne() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      String[] addonsToRemove = aoCare1;
      handleMsg(firstPlace, PREMIUM, AddonType.ONLY_FALSE, addonsToRemove);

      String[] addons = {};
      verifyAccount(account.getId());
      // Verify place
      verifyPlace(firstPlace.getId(), PREMIUM, addons);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, addons, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(), addons);

      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testRemoveAllAddOns() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      String[] addons = {};
      handleMsg(firstPlace, PREMIUM, AddonType.ALL_TRUE, addons);

      //Verify place
      verifyPlace(firstPlace.getId(), PREMIUM, addons);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, addons);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ImmutableSet.of());

      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testRemoveAllAddOnsSpecifically() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      handleMsg(firstPlace, PREMIUM, AddonType.ONLY_FALSE, aoCare1);

      String[] addons = {};
      verifyAccount(account.getId());
      //Verify place
      verifyPlace(firstPlace.getId(), PREMIUM, addons);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, addons);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ImmutableSet.of());
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testNoServiceLevelSpecified() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      String[] addons = aoCare1;
      handleMsg(firstPlace, null, AddonType.ALL_TRUE, addons);
      // Verify account
      verifyAccount(account.getId());
      // Verify place
      verifyPlace(firstPlace.getId(), PREMIUM, addons);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, addons, 1);
      // no value change or notification because nothing has changed
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testNoAddonsSpecified() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      handleMsg(firstPlace, BASIC, AddonType.ONLY_TRUE, null);

      // Verify account
      verifyAccount(account.getId());
      // Verify place
      verifyPlace(firstPlace.getId(), BASIC, aoCare1);
      // Verify subscriptions (should be three, expired basic, expired premium, and active basic)
      Assert.assertEquals(3, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(BASIC, STATE_ACTIVE, 1, aoCare1, 1, 1);
      verifySubscription(PREMIUM, STATE_EXPIRED, 1, aoCare1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.BASIC.toString());

      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testUpdateToPremiumFree() throws Exception {
      updateOnePlaceAndVerify(ServiceLevel.PREMIUM);
      handleMsg(firstPlace, ServiceLevel.PREMIUM_FREE, AddonType.ONLY_TRUE, null);
      verifyAccount(account.getId());
      verifyPlace(firstPlace.getId(), ServiceLevel.PREMIUM_FREE, aoCare1);

      // Verify subscriptions (should be three, expired basic, expired premium and active premium free)
      Assert.assertEquals(3, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_EXPIRED, 1, aoCare1, 1, 1);
      verifySubscription(PREMIUM_FREE, STATE_ACTIVE, 1, aoCare1, 1, 1);

      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM_FREE.toString());

      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }
   @Test
   public void testNoServicePlanOrAddonsSpecified() throws Exception {
      // This is pointless, but it shouldn't mess up anything if someone does it.
      updateOnePlaceAndVerify(PREMIUM);
      ((Faker)client).resetCalls();
      ((Faker)placeDao).resetCalls();
      handleMsg(firstPlace, null, AddonType.ONLY_TRUE, null);

      Assert.assertEquals("No calls should have been made to placeDao", 0,((Faker)placeDao).numberOfCalls());
      Assert.assertEquals("No calls should be made to the billing client.", 0, ((Faker)client).numberOfCalls());

      // Verify account
      verifyAccount(account.getId());
      // Verify place
      verifyPlace(firstPlace.getId(), PREMIUM, aoCare1);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, aoCare1, 1, 1);
      // There should be no notification
      Assert.assertNull(platformBus.poll());
   }

   @Test
   public void testNoChangesSpecified() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      ((Faker)client).resetCalls();
      ((Faker)placeDao).resetCalls();
      handleMsg(firstPlace, PREMIUM, AddonType.ONLY_TRUE, aoCare1);

      Assert.assertEquals("Only one calls should have been made to placeDao", 1,((Faker)placeDao).numberOfCalls());
      Assert.assertEquals("No calls should be made to the billing client.", 0, ((Faker)client).numberOfCalls());

      // Verify account
      verifyAccount(account.getId());
      // Verify place
      verifyPlace(firstPlace.getId(), PREMIUM, aoCare1);
      // Verify subscriptions. Should be two, expired Basic and active Premium
      Assert.assertEquals(2, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      verifySubscription(PREMIUM, STATE_ACTIVE, 1, aoCare1, 1, 1);
      // There should be no notification
      Assert.assertNull(platformBus.poll());
   }

   private void handleMsg(Place place, ServiceLevel serviceLevel, AddonType type, String[] addons) {
      PlatformMessage msg = createUpdateServicePlanRequest(place,
            serviceLevel,
            type,
            addons);
      handler.handleRequest(account, msg);
   }

   private void verifyNotification(PlatformMessage msg, UUID personId, Place place) {
      Assert.assertNotNull(msg);
      Assert.assertEquals(Addresses.NOTIFICATION, msg.getDestination().getRepresentation());
      Assert.assertEquals(Addresses.ACCOUNT, msg.getSource().getRepresentation());

      MessageBody body = msg.getValue();
      Assert.assertNotNull(msg);
      Assert.assertEquals(body.getMessageType(), NotificationCapability.NotifyRequest.NAME);
      Assert.assertEquals(personId.toString(), NotificationCapability.NotifyRequest.getPersonId(body));
      Assert.assertEquals(ServiceUpateNotifications.SERVICE_UPDATED.getKey(), NotificationCapability.NotifyRequest.getMsgKey(body));
      Assert.assertEquals(NotificationCapability.NotifyRequest.PRIORITY_LOW, NotificationCapability.NotifyRequest.getPriority(body));
      Map<String, String> params = NotificationCapability.NotifyRequest.getMsgParams(body);
      Assert.assertEquals(place.getName(), params.get(ServiceUpateNotifications.SERVICE_UPDATED.getParameter()));
   }

   private List<Place> createAndVerifyTwoPremiumAndOneBasic() throws Exception {
      updateOnePlaceAndVerify(PREMIUM);
      List<Place> places = new ArrayList<>(3);
      places.add(firstPlace);

      // Create second place
      Place secondPlace = createPlace(account.getId(), UUID.randomUUID(), "Second Place");
      handleMsg(secondPlace, PREMIUM, AddonType.ALL_TRUE, aoCare1);
      places.add(secondPlace);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.PREMIUM.toString(), aoCare1);

      verifyNotification(platformBus.take(), account.getOwner(), secondPlace);
      Assert.assertNull(platformBus.poll());

      // Create third place
      Place thirdPlace = createPlace(account.getId(), UUID.randomUUID(), "Third Place");
      handleMsg(thirdPlace, BASIC, AddonType.ALL_TRUE, aoCare1);
      places.add(thirdPlace);
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.BASIC.toString(), aoCare1);

      verifyNotification(platformBus.take(), account.getOwner(), thirdPlace);
      Assert.assertNull(platformBus.poll());

      // Check account
      verifyAccount(account.getId());

      // Check places
      verifyPlace(firstPlace.getId(), PREMIUM, aoCare1);
      verifyPlace(secondPlace.getId(), PREMIUM, aoCare1);
      verifyPlace(thirdPlace.getId(), BASIC, aoCare1);

      // Verify subscriptions. Should be three, expired Basic, active Basic, and active Premium
      Assert.assertEquals(3, billingAccount.getSubscriptions().size());
      verifySubscription(BASIC, STATE_EXPIRED, 1, aoNone);
      // Basic Subscription Verify
      String[] basicAddons = aoCare1;
      verifySubscription(BASIC, STATE_ACTIVE, 1, basicAddons, 1, 1);
      // Premium Subscription Verify
      String[] premAddons = aoCare1;
      verifySubscription(PREMIUM, STATE_ACTIVE, 2, premAddons, 2, 1, 1);

      return places;
   }

   private void updateOnePlaceAndVerify(ServiceLevel level) throws Exception {
      // Update the subscription
      ServiceLevel curLvl = firstPlace.getServiceLevel();
      handleMsg(firstPlace, level, AddonType.ALL_TRUE, aoCare1);

      Set<String> addons = new HashSet<>(Arrays.asList(aoCare1));
      // Check update.
      verifyAccount(account.getId());
      verifyPlace(firstPlace.getId(), level, aoCare1);
      verifySubscription(level, STATE_ACTIVE, 1, aoCare1, 1, 1);
      verifySubscriptionLevelChangeEvent(platformBus.take(), curLvl == level ? null : level.toString(), addons);
      verifyNotification(platformBus.take(), account.getOwner(), firstPlace);
      Assert.assertNull(platformBus.poll());
   }

   private void verifySubscription(ServiceLevel level, String state, int quantity, String[] addons, Integer... counts) {
      Subscription sub = billingAccount.getSubscription(level, state);
      Assert.assertEquals(level, sub.getServiceLevel());
      Assert.assertEquals(quantity, sub.getQuantity());
      assertAddonsEquals(sub.getSubscriptionAddOns(), addons, counts);
   }

   private void verifySubscription(int index, ServiceLevel level, String state, int quantity, String[] addons, Integer... counts) {
      List<Subscription> subs = billingAccount.getSubscriptions(level, state);
      Subscription sub = subs.get(index);
      Assert.assertEquals(level, sub.getServiceLevel());
      Assert.assertEquals(quantity, sub.getQuantity());
      assertAddonsEquals(sub.getSubscriptionAddOns(), addons, counts);
   }

   private PlatformMessage createUpdateServicePlanRequest(Place place, ServiceLevel level, AddonType type, String... addons) {
      UUID placeId = place.getId();

      String serviceLevel=null;
      if(level!=null){
         switch(level){
            case PREMIUM:
               serviceLevel=AccountCapability.UpdateServicePlanRequest.SERVICELEVEL_PREMIUM;
               break;
            case PREMIUM_FREE:
               serviceLevel=AccountCapability.UpdateServicePlanRequest.SERVICELEVEL_PREMIUM_FREE;
               break;
            case PREMIUM_ANNUAL:
               serviceLevel=AccountCapability.UpdateServicePlanRequest.SERVICELEVEL_PREMIUM_ANNUAL;
               break;
            default:
               serviceLevel=AccountCapability.UpdateServicePlanRequest.SERVICELEVEL_BASIC;
         }
      }

      Map<String, Object> map = new HashMap<>();
      Set<String> addonSet = addons != null ? IrisCollections.setOf(addons) : new HashSet<>();
      putAddonValue(map, ServiceAddon.CELLBACKUP.name(), addonSet, type);

      AccountCapability.UpdateServicePlanRequest.Builder builder = AccountCapability.UpdateServicePlanRequest.builder();
      builder.withPlaceID(placeId.toString());
      if (serviceLevel != null) {
         builder.withServiceLevel(serviceLevel);
      }
      if (!map.isEmpty()) {
         builder.withAddons(map);
      }
      MessageBody msg = builder.build();

      return PlatformMessage.createBroadcast(msg, Address.platformService("bogus")); // The source and destination are unnecessary for this test.

   }

   private void putAddonValue(Map<String, Object> map, String code, Set<String> included, AddonType type) {
      if (included.contains(code) || type == AddonType.ALL_TRUE || type == AddonType.ALL_FALSE) {
         Boolean includedValue = (type == AddonType.ONLY_TRUE || type == AddonType.ALL_TRUE) ;
         Boolean excludedValue = !includedValue.booleanValue();
         map.put(code, included.contains(code) ? includedValue : excludedValue);
      }
   }
}

