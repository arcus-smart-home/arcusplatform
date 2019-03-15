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
package com.iris.platform.subscription;

import static com.iris.billing.client.BillingClient.RefundType.PARTIAL;
import static com.iris.billing.client.model.Constants.PLAN_CODE_PREMIUM_PROMON;
import static com.iris.billing.client.model.Constants.PLAN_CODE_PREMIUM_PROMON_ANNUAL;
import static com.iris.billing.client.model.Constants.STATE_ACTIVE;
import static com.iris.billing.client.model.Constants.STATE_EXPIRED;
import static com.iris.billing.client.model.SubscriptionAddon.buildAddonWithQuantity;
import static com.iris.messages.model.ServiceAddon.CELLBACKUP;
import static com.iris.messages.model.ServiceAddon.getAddonCode;
import static com.iris.messages.model.ServiceLevel.PREMIUM_PROMON;
import static com.iris.messages.model.ServiceLevel.PREMIUM_PROMON_ANNUAL;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.easymock.CaptureType.ALL;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.BillingClient.RefundType;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.request.SubscriptionRequest;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({AccountDAO.class, PlaceDAO.class, BillingClient.class, SubscriptionManager.class, PlatformMessageBus.class})
public class TestSubscriptionUpdaterImpl extends IrisMockTestCase
{
   private static final UUID ACCOUNT_ID = UUID.fromString("1e3d7ec8-1e20-4e36-b1e8-010108dbe453");

   private static final UUID PLACE_ID_1 = UUID.fromString("ffd1247a-481c-436b-b115-1808b544006b");
   private static final UUID PLACE_ID_2 = UUID.fromString("097e424c-8854-4a9a-9ba9-a2cb65acaea5");
   private static final UUID PLACE_ID_3 = UUID.fromString("8a9797a4-fb13-4128-984f-42a34a5990af");

   @Inject
   private AccountDAO mockAccountDao;

   @Inject
   private PlaceDAO mockPlaceDao;

   @Inject
   private BillingClient mockBillingClient;

   @Inject
   private SubscriptionManager mockSubscriptionManager;

   @Inject
   private PlatformMessageBus mockPlatformMessageBus;

   @Inject
   private SubscriptionUpdaterImpl componentUnderTest;

   @Test(expected = SubscriptionUpdateException.class)
   public void testUpdateSubscriptions_fail_duetonobilling() throws SubscriptionUpdateException
   {
      Account account = new Account();
      account.setId(ACCOUNT_ID);
      account.setPlaceIDs(ImmutableSet.of(PLACE_ID_1));

      Place place1 = new Place();
      place1.setId(PLACE_ID_1);
      place1.setServiceLevel(PREMIUM_PROMON);
      place1.setServiceAddons(ImmutableSet.of(CELLBACKUP.name()));

      Map<UUID, Place> places = ImmutableMap.of(place1.getId(), place1);

      Map<UUID, Place> affectedPlaces = ImmutableMap.of(place1.getId(), place1);

      IrisSubscription unaffectedPlacesSubscription = IrisSubscription.builder()
         .create();

      for (UUID placeId : account.getPlaceIDs())
      {
         expect(mockPlaceDao.findById(placeId)).andReturn(places.get(placeId));
      }

      // Must do this double streaming so that the final ordering of place ids matches the one generated in
      // SubscriptionUpdaterImpl.updateSubscriptions().
      UUID[] placeIds = affectedPlaces.values().stream().collect(toSet()).stream().map(Place::getId).toArray(UUID[]::new);
      expect(mockSubscriptionManager.collectPlacesForAccount(account, placeIds)).andReturn(null);

      expect(mockSubscriptionManager.extractSubscription(anyObject())).andReturn(unaffectedPlacesSubscription);

      for (Place affectedPlace : affectedPlaces.values())
      {
         mockPlaceDao.setUpdateFlag(affectedPlace.getId(), true);
         expectLastCall();
      }
      
      replay();
      
      componentUnderTest.updateSubscriptions(account, PREMIUM_PROMON, PREMIUM_PROMON_ANNUAL);
   }
   
   @Test
   public void testUpdateSubscriptions_promon_promonAnnual() throws SubscriptionUpdateException
   {
      Account account = new Account();
      account.setId(ACCOUNT_ID);
      account.setBillingCCLast4("1234");
      account.setPlaceIDs(ImmutableSet.of(PLACE_ID_1));

      Place place1 = new Place();
      place1.setId(PLACE_ID_1);
      place1.setServiceLevel(PREMIUM_PROMON);
      place1.setServiceAddons(ImmutableSet.of(CELLBACKUP.name()));

      Map<UUID, Place> places = ImmutableMap.of(place1.getId(), place1);

      Map<UUID, Place> affectedPlaces = ImmutableMap.of(place1.getId(), place1);

      IrisSubscription unaffectedPlacesSubscription = IrisSubscription.builder()
         .create();

      Subscriptions currentRecurlySubscriptionsForAccount = new Subscriptions();

      Subscription promonSubscription = new Subscription();
      promonSubscription.setSubscriptionID("SUB1");
      promonSubscription.setPlanCode(PLAN_CODE_PREMIUM_PROMON);
      promonSubscription.setState(STATE_ACTIVE);
      currentRecurlySubscriptionsForAccount.add(promonSubscription);

      Subscriptions updatedRecurlySubscriptionsForAccount = new Subscriptions();

      Subscription promonSubscriptionExpired = new Subscription();
      promonSubscriptionExpired.setSubscriptionID("SUB1");
      promonSubscriptionExpired.setPlanCode(PLAN_CODE_PREMIUM_PROMON);
      promonSubscriptionExpired.setState(STATE_EXPIRED);
      updatedRecurlySubscriptionsForAccount.add(promonSubscriptionExpired);

      Subscription sub = new Subscription();
       sub.setSubscriptionID("SUB2");
       sub.setPlanCode(PLAN_CODE_PREMIUM_PROMON_ANNUAL);
       sub.setState(STATE_ACTIVE);
      updatedRecurlySubscriptionsForAccount.add( sub);

      Map<String, RefundType> expectedSubscriptionTerminations = ImmutableMap.of("SUB1", PARTIAL);

      SubscriptionRequest expectedSubscriptionRequest = new SubscriptionRequest();
      expectedSubscriptionRequest.setPlanCode(PLAN_CODE_PREMIUM_PROMON_ANNUAL);
      expectedSubscriptionRequest.setQuantity(1);
      expectedSubscriptionRequest.addSubscriptionAddon(
         buildAddonWithQuantity(getAddonCode(CELLBACKUP, PREMIUM_PROMON_ANNUAL), 1));

      Map<ServiceLevel, String> expectedAccountSubscriptionIds =
         ImmutableMap.of(PREMIUM_PROMON_ANNUAL, "SUB2");

      testUpdateSubscriptions(account, places, PREMIUM_PROMON, PREMIUM_PROMON_ANNUAL,
         affectedPlaces, unaffectedPlacesSubscription,
         currentRecurlySubscriptionsForAccount, updatedRecurlySubscriptionsForAccount,
         expectedSubscriptionTerminations, expectedSubscriptionRequest, expectedAccountSubscriptionIds);
   }

   private void testUpdateSubscriptions(Account account, Map<UUID, Place> places,
      ServiceLevel currentServiceLevel, ServiceLevel newServiceLevel,
      Map<UUID, Place> affectedPlaces, IrisSubscription unaffectedPlacesSubscription,
      Subscriptions currentRecurlySubscriptionsForAccount, Subscriptions updatedRecurlySubscriptionsForAccount,
      Map<String, RefundType> expectedSubscriptionTerminations, SubscriptionRequest expectedSubscriptionRequest,
      Map<ServiceLevel, String> expectedAccountSubscriptionIds)
      throws SubscriptionUpdateException
   {
      for (UUID placeId : account.getPlaceIDs())
      {
         expect(mockPlaceDao.findById(placeId)).andReturn(places.get(placeId));
      }

      // Must do this double streaming so that the final ordering of place ids matches the one generated in
      // SubscriptionUpdaterImpl.updateSubscriptions().
      UUID[] placeIds = affectedPlaces.values().stream().collect(toSet()).stream().map(Place::getId).toArray(UUID[]::new);
      expect(mockSubscriptionManager.collectPlacesForAccount(account, placeIds)).andReturn(null);

      expect(mockSubscriptionManager.extractSubscription(anyObject())).andReturn(unaffectedPlacesSubscription);

      for (Place affectedPlace : affectedPlaces.values())
      {
         mockPlaceDao.setUpdateFlag(affectedPlace.getId(), true);
         expectLastCall();
      }

      SettableFuture<Subscriptions> getSubscriptionsForAccountFuture = SettableFuture.create();
      getSubscriptionsForAccountFuture.set(currentRecurlySubscriptionsForAccount);
      expect(mockBillingClient.getSubscriptionsForAccount(account.getId().toString()))
         .andReturn(getSubscriptionsForAccountFuture);

      SettableFuture<Subscription> terminateSubscriptionFuture = SettableFuture.create();
      terminateSubscriptionFuture.set(null);
      for (Map.Entry<String, RefundType> expectedSubscriptionTermination : expectedSubscriptionTerminations.entrySet())
      {
         expect(mockBillingClient.terminateSubscription(
            expectedSubscriptionTermination.getKey(), expectedSubscriptionTermination.getValue()))
            .andReturn(terminateSubscriptionFuture);
      }

      Capture<SubscriptionRequest> actualSubscriptionRequestCapture = newCapture();
      SettableFuture<Subscriptions> createSubscriptionForAccountFuture = SettableFuture.create();
      createSubscriptionForAccountFuture.set(null);
      expect(mockBillingClient.createSubscriptionForAccount(eq(account.getId().toString()), capture(actualSubscriptionRequestCapture)))
         .andReturn(createSubscriptionForAccountFuture);

      if (affectedPlaces.size() < places.size())
      {
         SettableFuture<Subscriptions> updateSubscriptionFuture = SettableFuture.create();
         updateSubscriptionFuture.set(null);
         expect(mockBillingClient.updateSubscription(anyObject(SubscriptionRequest.class)))
            .andReturn(updateSubscriptionFuture);
      }

      SettableFuture<Subscriptions> getSubscriptionsForAccountFuture2 = SettableFuture.create();
      getSubscriptionsForAccountFuture2.set(updatedRecurlySubscriptionsForAccount);
      expect(mockBillingClient.getSubscriptionsForAccount(account.getId().toString()))
         .andReturn(getSubscriptionsForAccountFuture2);

      expect(mockAccountDao.findById(account.getId())).andReturn(account);
      expect(mockAccountDao.save(account)).andReturn(account);

      for (Place affectedPlace : affectedPlaces.values())
      {
         expect(mockPlaceDao.findById(affectedPlace.getId())).andReturn(affectedPlace);
         expect(mockPlaceDao.save(affectedPlace)).andReturn(affectedPlace);
         mockPlaceDao.setUpdateFlag(affectedPlace.getId(), false);
         expectLastCall();
      }

      Capture<PlatformMessage> messageCapture = newCapture(ALL);
      expect(mockPlatformMessageBus.send(capture(messageCapture))).andReturn(null).times(affectedPlaces.size());

      replay();

      componentUnderTest.updateSubscriptions(account, currentServiceLevel, newServiceLevel);

      SubscriptionRequest actualSubscriptionRequest = actualSubscriptionRequestCapture.getValue();
      assertThat(actualSubscriptionRequest.getPlanCode(), equalTo(expectedSubscriptionRequest.getPlanCode()));
      assertThat(actualSubscriptionRequest.getQuantity(), equalTo(expectedSubscriptionRequest.getQuantity()));
      assertThat(reflectionEquals(actualSubscriptionRequest.getSubscriptionAddons(),
         expectedSubscriptionRequest.getSubscriptionAddons()), equalTo(true));

      assertThat(account.getSubscriptionIDs(), equalTo(expectedAccountSubscriptionIds));

      List<PlatformMessage> messages = messageCapture.getValues();
      assertThat(messages, hasSize(affectedPlaces.size()));
      Map<String, Object> attributes = ImmutableMap.of(PlaceCapability.ATTR_SERVICELEVEL, newServiceLevel.name());
      for (PlatformMessage message : messages)
      {
         assertThat(message.getValue().getAttributes(), equalTo(attributes));
      }
   }

   @Override
   public void tearDown() throws Exception
   {
      verify();

      reset();

      super.tearDown();
   }
}

