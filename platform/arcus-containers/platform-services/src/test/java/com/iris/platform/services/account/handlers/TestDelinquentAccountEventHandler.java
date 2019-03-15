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

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.notification.Notifications;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.services.BillingTestCase;
import com.iris.platform.subscription.IrisSubscription;
import com.iris.platform.subscription.SubscriptionManager;
import com.iris.platform.subscription.SubscriptionUpdater;
import com.iris.platform.subscription.SubscriptionUpdaterImpl;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules({InMemoryMessageModule.class})
@Mocks({SubscriptionManager.class, IrisSubscription.class})
public class TestDelinquentAccountEventHandler extends BillingTestCase {
   
   @Provides
   @Singleton
   public SubscriptionUpdater provideSubscriptionUpdater() {
      return new SubscriptionUpdaterImpl(accountDao, placeDao, client, subManager,platformBus);
   }
   
   @Inject
   private DelinquentAccountEventHandler handler;
   
   @Inject IrisSubscription irisSubscription;
   @Inject SubscriptionManager subscriptionManager;
 
   @Override
   public void setUp() throws Exception {
      super.setUp();
      initData();
   }
   
   @Test
   public void testHandleDelinquentAccount() throws Exception {
      EasyMock.expect(subscriptionManager.extractSubscription(ImmutableSet.of(firstPlace))).andReturn(irisSubscription);
      EasyMock.expect(irisSubscription.getServiceLevels()).andReturn(ImmutableSet.of(ServiceLevel.PREMIUM));
      replay();
      firstPlace.setServiceLevel(ServiceLevel.PREMIUM);
      
      
      assertEquals(ServiceLevel.PREMIUM,firstPlace.getServiceLevel());
      
      handler.handleEvent(account, createDelinquentAccountEvent(account));
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.BASIC.toString());
      verifyNotification(platformBus.take(), account.getOwner(), false);
      verifySubscriptionUpdates();
   }
   
   @Test
   public void testHandleDelinquentAccountPro() throws Exception {
      EasyMock.expect(subscriptionManager.extractSubscription(ImmutableSet.of(firstPlace))).andReturn(irisSubscription);
      EasyMock.expect(irisSubscription.getServiceLevels()).andReturn(ImmutableSet.of(ServiceLevel.PREMIUM_PROMON));
      replay();
      firstPlace.setServiceLevel(ServiceLevel.PREMIUM_PROMON);
      
      
      assertEquals(ServiceLevel.PREMIUM_PROMON,firstPlace.getServiceLevel());
      
      handler.handleEvent(account, createDelinquentAccountEvent(account));
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.BASIC.toString());
      verifyNotification(platformBus.take(), account.getOwner(), true);
      verifySubscriptionUpdates();
   }
   
   @Test
   public void testHandleDelinquentAccountProAnnual() throws Exception {
      EasyMock.expect(subscriptionManager.extractSubscription(ImmutableSet.of(firstPlace))).andReturn(irisSubscription);
      EasyMock.expect(irisSubscription.getServiceLevels()).andReturn(ImmutableSet.of(ServiceLevel.PREMIUM_PROMON_ANNUAL));
      replay();
      firstPlace.setServiceLevel(ServiceLevel.PREMIUM_PROMON_ANNUAL);
      
      
      assertEquals(ServiceLevel.PREMIUM_PROMON_ANNUAL,firstPlace.getServiceLevel());
      
      handler.handleEvent(account, createDelinquentAccountEvent(account));
      verifySubscriptionLevelChangeEvent(platformBus.take(),ServiceLevel.BASIC.toString());
      verifyNotification(platformBus.take(), account.getOwner(), true);
      verifySubscriptionUpdates();
   }
   
   private void verifySubscriptionUpdates(){
      assertTrue(firstPlace.getServiceAddons().isEmpty());
      assertEquals(ServiceLevel.BASIC,firstPlace.getServiceLevel());
   }
   
   private void verifyNotification(PlatformMessage msg, UUID personId, boolean isPro) {
      String messageKey = isPro ? Notifications.DelinquentAccountPro.KEY : Notifications.DelinquentAccount.KEY;
      Assert.assertNotNull(msg);
      Assert.assertEquals(Addresses.NOTIFICATION, msg.getDestination().getRepresentation());
      Assert.assertEquals(Addresses.PERSON, msg.getSource().getRepresentation());
      
      MessageBody body = msg.getValue();
      Assert.assertNotNull(msg);
      Assert.assertEquals(body.getMessageType(), NotificationCapability.NotifyRequest.NAME);
      Assert.assertEquals(personId.toString(), NotificationCapability.NotifyRequest.getPersonId(body));
      Assert.assertEquals(messageKey, NotificationCapability.NotifyRequest.getMsgKey(body));
      Assert.assertEquals(NotificationCapability.NotifyRequest.PRIORITY_LOW, NotificationCapability.NotifyRequest.getPriority(body));
   }   

   private PlatformMessage createDelinquentAccountEvent(Account account) {
      MessageBody msg = AccountCapability.DelinquentAccountEventRequest.builder()
              .withAccountId(account.getId().toString())
              .build();
      return PlatformMessage.createBroadcast(msg, Address.platformService("bogus")); // The source and destination are unnecessary for this test.
   }
}

