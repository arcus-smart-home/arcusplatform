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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.easymock.EasyMock;
import org.junit.Assert;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.AccountBalance;
import com.iris.billing.client.model.AccountNotes;
import com.iris.billing.client.model.Accounts;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Adjustments;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.Constants;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Invoices;
import com.iris.billing.client.model.PlanAddons;
import com.iris.billing.client.model.Plans;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.SubscriptionAddon;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.Transactions;
import com.iris.billing.client.model.request.AccountRequest;
import com.iris.billing.client.model.request.AdjustmentRequest;
import com.iris.billing.client.model.request.InvoiceRefundRequest;
import com.iris.billing.client.model.request.SubscriptionRequest;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.PagedResults;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.subscription.SubscriptionManager;
import com.iris.platform.subscription.SubscriptionManagerImpl;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.util.IrisCollections;

@Mocks({ PersonDAO.class })
public abstract class BillingTestCase extends IrisMockTestCase {
   protected BillingClient client = new FakeBillingClient();
   protected AccountDAO accountDao = new FakeAccountDao();
   protected PlaceDAO placeDao = new FakePlaceDao();
   @Inject protected PersonDAO personDao;
   protected AuthorizationGrantDAO grantDao = new FakeAuthorizationGrantDao();
   protected SubscriptionManager subManager = new SubscriptionManagerImpl(placeDao);

   protected com.iris.messages.model.Account account;
   protected Place firstPlace;
   protected BillingAccount billingAccount;
   protected Person person;

   @Inject
   protected InMemoryPlatformMessageBus platformBus;

   @Provides
   @Singleton
   public BillingClient provideBillingClient() {
      return client;
   }

   @Provides
   @Singleton
   public AccountDAO provideAccountDao() {
      return accountDao;
   }

   @Provides
   @Singleton
   public PlaceDAO providePlaceDao() {
      return placeDao;
   }

   @Provides
   @Singleton
   public AuthorizationGrantDAO provideGrantDao() {
      return grantDao;
   }

   protected static class Addresses {
      public static final String NOTIFICATION = Address.platformService(PlatformConstants.SERVICE_NOTIFICATION).getRepresentation();
      public static final String PERSON = Address.platformService(PlatformConstants.SERVICE_PEOPLE).getRepresentation();
      public static final String ACCOUNT = Address.platformService(PlatformConstants.SERVICE_ACCOUNTS).getRepresentation();
   }

   protected static class FakePerson {
      public static final String EMAIL = "jdoe@fake.com";
      public static final String FIRSTNAME = "John";
      public static final String LASTNAME = "Doe";
      public static final String MOBILENUMBER = "4223161584";
      public static final String PIN = "1516";
   }

   protected static class FakeLogin {
      public static final String PASSWORD = "m56keiym9uQASCST1yzpoyTNezaBK+/GBo86hfK0xXs=";
      public static final String PASSWORD_SALT = "XzyBUTPQ1Jefut6Kez6WHA==";
   }

   protected static class FakePlace {
      public static final String NAME = "Fake Place";
   }

   protected void initData() {
      billingAccount = ((FakeBillingClient)client).createBillingAccount(UUID.randomUUID());
      UUID accountId = UUID.fromString(billingAccount.getAccount().getAccountID());
      createAccount(accountId);
      person = createPerson(accountDao.findById(accountId), UUID.randomUUID());
      UUID placeId = createPlace(accountId, UUID.randomUUID(), "First Place").getId();
      createAndBindSubscription(accountId, placeId, ServiceLevel.BASIC);
      // Refresh Account, Place, and BillingAccount to pick up changes.
      billingAccount = ((FakeBillingClient)client).getBillingAccount(accountId);
      account = accountDao.findById(accountId);
      firstPlace = placeDao.findById(placeId);
      resetCalls();
   }

   protected void resetCalls() {
      ((Faker)client).resetCalls();
      ((Faker)accountDao).resetCalls();
      ((Faker)placeDao).resetCalls();
   }

   public com.iris.messages.model.Account createAccount(UUID id) {
      com.iris.messages.model.Account account = new com.iris.messages.model.Account();
      account.setId(id);
      account.setBillable(true);
      account.setCreated(new Date());
      account.setModified(new Date());
      account.setTaxExempt(false);
      account.setBillingCCLast4("1234");
      accountDao.save(account);
      return account;
   }

   public Person createPerson(com.iris.messages.model.Account account, UUID personId) {
      Person person = new Person();
      person.setId(personId);
      person.setAccountId(account.getId());
      person.setEmail(FakePerson.EMAIL);
      person.setFirstName(FakePerson.FIRSTNAME);
      person.setLastName(FakePerson.LASTNAME);
      person.setMobileNumber(FakePerson.MOBILENUMBER);
      person.setCurrPlace(UUID.randomUUID());
      person.setPinAtPlace(person.getCurrPlace(), FakePerson.PIN);
      person.setCreated(new Date());
      person.setModified(new Date());
      person.setHasLogin(true);

      EasyMock.expect(personDao.findById(personId)).andReturn(person).anyTimes();
      EasyMock.expect(personDao.findByEmail(person.getEmail())).andReturn(person).anyTimes();

      account.setOwner(personId);
      accountDao.save(account);
      return person;
   }

   public Place createPlace(UUID accountId, UUID placeId, String name) {
      Place place = new Place();
      place.setId(placeId);
      place.setAccount(accountId);
      place.setCreated(new Date());
      place.setModified(new Date());
      place.setName(name != null ? name : FakePlace.NAME);
      placeDao.save(place);

      com.iris.messages.model.Account account = accountDao.findById(accountId);
      Set<UUID> placeIds = new HashSet<>(account.getPlaceIDs());
      placeIds.add(placeId);
      account.setPlaceIDs(placeIds);
      accountDao.save(account);

      return place;
   }

   public Subscription createAndBindSubscription(UUID accountId, UUID placeId, ServiceLevel level) {
      Subscription sub = new Subscription();
      sub.setSubscriptionID(UUID.randomUUID().toString());
      sub.setPlanCode(Constants.PLAN_CODE_BASIC);
      sub.setQuantity("1");
      sub.setState(Constants.STATE_ACTIVE);
      sub.setCurrency(Constants.CURRENCY_USD);
      ((FakeBillingClient)client).getBillingAccount(accountId).addSubscription(sub);
      com.iris.messages.model.Account account = accountDao.findById(accountId);
      Map<ServiceLevel, String> subMap = new HashMap<>(account.getSubscriptionIDs());
      subMap.put(level, sub.getSubscriptionID());
      account.setSubscriptionIDs(subMap);
      accountDao.save(account);
      Place place = placeDao.findById(placeId);
      place.setServiceLevel(level);
      placeDao.save(place);
      return sub;
   }

   protected Place findPlaceByName(String name) {
      ((FakePlaceDao)placeDao).getPlaces().forEach(p -> System.out.println(p));
      return ((FakePlaceDao)placeDao).getPlaces().stream().filter(p -> name.equals(p.getName())).findFirst().get();
   }

   protected void assertAddonsEquals(Set<String> addons, String... expectedAddons) {
      Assert.assertEquals(expectedAddons.length, addons.size());
      Set<String> expectedSet = IrisCollections.setOf(expectedAddons);
      for (String addon : addons) {
         Assert.assertTrue("Addon " + addon + " not found in expected set", expectedSet.contains(addon));
      }
   }

   protected void assertAddonsEquals(List<SubscriptionAddon> addons, String[] expectedNames, Integer[] expectedCounts) {
      Assert.assertEquals(expectedNames.length, addons.size());
      Map<String, Integer> expected = new HashMap<>(expectedNames.length);
      for (int i = 0; i < expectedNames.length; i++) {
         expected.put(expectedNames[i], expectedCounts[i]);
      }
      for (SubscriptionAddon addon : addons) {
         Assert.assertTrue(expected.containsKey(addon.getAddonCode()));
         Assert.assertEquals(expected.get(addon.getAddonCode()), addon.getQuantity());
      }
   }

   protected void verifyInitialSetup() {
      // Initial setup should be an account with one place that has a BASIC subscription and no addons.
      verifyAccount(account.getId(), firstPlace.getId());
      verifyPlace(firstPlace.getId(), ServiceLevel.BASIC);
      verifyBilling(ServiceLevel.BASIC, Constants.STATE_ACTIVE, 1);
   }

   protected void verifyAccount(UUID accountId, UUID... places) {
      com.iris.messages.model.Account myAccount = accountDao.findById(accountId);
      Map<ServiceLevel, String> subMap = myAccount.getSubscriptionIDs();
      Map<ServiceLevel, String> billingMap = billingAccount.getActiveSubscriptionMap();
      Assert.assertEquals("Account subscription map and recurly subscriptions should match", billingMap, subMap);

      if (places != null && places.length > 0) {
         Set<UUID> accountPlaceIds = myAccount.getPlaceIDs();
         Set<UUID> expectedPlaceIds = IrisCollections.setOf(places);
         Assert.assertEquals("Set of places ids should match expected set", expectedPlaceIds, accountPlaceIds);
      }
   }

   protected void verifyPlace(UUID placeId, ServiceLevel level, String... addons) {
      verifyPlace(placeId, null, level, addons);
   }

   protected void verifyPlace(UUID placeId, PlaceData data, ServiceLevel level, String... addons) {
      Place place = placeDao.findById(placeId);
      if (level != null) {
         Assert.assertEquals(level, place.getServiceLevel());
      }
      if (addons != null) {
         assertAddonsEquals(place.getServiceAddons(), addons);
      }
      if (data != null) {
         Assert.assertEquals(data.name, place.getName());
         Assert.assertEquals(data.addrValidated, place.getAddrValidated());
      }
   }

   protected void verifyBilling(ServiceLevel level, String state, int quantity) {
      verifyBilling(level, state, quantity, new String[]{});
   }

   protected void verifyBilling(ServiceLevel level, String state, int quantity, String[] addons, Integer... counts) {
      Subscription sub = billingAccount.getSubscription(level, state);
      Assert.assertEquals(level, sub.getServiceLevel());
      Assert.assertEquals(quantity, sub.getQuantity());
      assertAddonsEquals(sub.getSubscriptionAddOns(), addons, counts);
   }

   protected void verifyValueChangeEvent(PlatformMessage msg, String expectedSource) {
      verifyValueChangeEvent(msg, expectedSource, null);
   }
   
   protected void verifyValueChangeEvent(PlatformMessage msg, String expectedSource, String expectedPlaceId) {
      Assert.assertNotNull(msg);
      Assert.assertEquals(Address.broadcastAddress(), msg.getDestination());
      Assert.assertEquals(expectedSource, msg.getSource().getRepresentation());

      MessageBody body = msg.getValue();
      Assert.assertNotNull(body);
      Assert.assertEquals(Capability.EVENT_VALUE_CHANGE, body.getMessageType());
      
      if(expectedPlaceId != null) {
         assertEquals(expectedPlaceId, msg.getPlaceId());
      }
   }

   protected void verifyNoMoreMsgs() {
      Assert.assertNull(platformBus.poll());
   }

   protected void verifyNotificationMsg(PlatformMessage msg) {
      verifyNotificationMsg(msg, person.getId(), getDefaultPriority(), null);
   }

   protected void verifyNotificationMsg(PlatformMessage msg, Map<String, String> expectedParams) {
      verifyNotificationMsg(msg, person.getId(), getDefaultPriority(), expectedParams);
   }

   protected void verifyNotificationMsg(PlatformMessage msg, UUID personId, String priority, Map<String, String> expectedParams) {
      Assert.assertNotNull(msg);
      Assert.assertEquals(Addresses.NOTIFICATION, msg.getDestination().getRepresentation());
      Assert.assertEquals(getSourceAddressRepresentation(), msg.getSource().getRepresentation());

      MessageBody body = msg.getValue();
      Assert.assertNotNull(body);
      Assert.assertEquals(NotificationCapability.NotifyRequest.NAME, body.getMessageType());
      Assert.assertEquals(personId.toString(), NotificationCapability.NotifyRequest.getPersonId(body));
      Assert.assertEquals(getMsgKey(), NotificationCapability.NotifyRequest.getMsgKey(body));
      Assert.assertEquals(priority, NotificationCapability.NotifyRequest.getPriority(body));
      if (expectedParams != null && !expectedParams.isEmpty()) {
         Map<String, String> params = NotificationCapability.NotifyRequest.getMsgParams(body);
         expectedParams.keySet().forEach(k -> Assert.assertEquals(expectedParams.get(k), params.get(k)));
      }
   }

   protected String getSourceAddressRepresentation() {
      return Addresses.ACCOUNT;
   }

   protected String getMsgKey() {
      return "";
   }

   protected String getDefaultPriority() {
      return NotificationCapability.NotifyRequest.PRIORITY_LOW;
   }

   public static class BillingAccount {
      private Account account;
      private List<Subscription> subscriptions = new ArrayList<>();
      private Set<Invoice> invoices = new HashSet<>();

      public BillingAccount(UUID id) {
         account = new Account();
         account.setAccountID(id.toString());
      }

      public Account getAccount() {
         return account;
      }

      public void setAccount(Account account) {
         this.account = account;
      }

      public void addSubscription(Subscription sub) {
         subscriptions.add(sub);
      }

      public Map<ServiceLevel, String> getActiveSubscriptionMap() {
         Map<ServiceLevel, String> map = new HashMap<>();
         for (Subscription sub : subscriptions) {
            if (sub.getState() == Constants.STATE_ACTIVE) {
               ServiceLevel level = sub.getServiceLevel();
               Assert.assertNull("There should not be more than one active subscription for service level " + level, map.get(level));
               map.put(level, sub.getSubscriptionID());
            }
         }
         return map;
      }

      public List<Subscription> getSubscriptions() {
         return subscriptions.stream().collect(Collectors.toList());
      }

      public List<Subscription> getSubscriptions(ServiceLevel level, String state) {
         return subscriptions
               .stream()
               .filter(s -> (s.getServiceLevel() == level && s.getState().equals(state)))
               .collect(Collectors.toList());
      }

      public Subscription getSubscription(ServiceLevel level, String state) {
         List<Subscription> list = getSubscriptions(level, state);
         Assert.assertEquals("Only expected one subscription with level " + level + " and state " + state, 1, list.size());
         return list.get(0);
      }

      public Set<Invoice> getInvoices() {
         return invoices;
      }

      public void setInvoices(Set<Invoice> invoices) {
         this.invoices = invoices;
      }
   }

   public static class Faker {
      protected final List<String> calls = new ArrayList<>();

      public List<String> getCalls() {
         return calls;
      }

      public void resetCalls() {
         calls.clear();
      }

      public int numberOfCalls() {
         return calls.size();
      }

      public boolean wasCalled(String call) {
         return calls.contains(call);
      }
   }

   public static class FakeBillingClient extends Faker implements BillingClient {
      private Map<String, BillingAccount> accounts = new HashMap<>();

      @Override
      public ListenableFuture<Accounts> getAccounts() {
         // TODO Auto-generated method stub
         return null;
      }

      // Create Billing Account with one active, basic subscriptions with no addons.
      public BillingAccount createBillingAccount(UUID accountId) {
         BillingAccount account = new BillingAccount(accountId);
         accounts.put(accountId.toString(), account);
         return account;
      }

      public BillingAccount getBillingAccount(String accountId) {
         return accounts.get(accountId);
      }

      public BillingAccount getBillingAccount(UUID accountId) {
         return accounts.get(accountId.toString());
      }

      @Override
      public ListenableFuture<Adjustment> issueCredit(String arg0, AdjustmentRequest arg1) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public ListenableFuture<Invoice> issueInvoiceRefund(String arg0, InvoiceRefundRequest arg1) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public ListenableFuture<Subscriptions> createSubscriptionForAccount(String accountID, SubscriptionRequest subscriptionRequest) {
         calls.add("createSubscriptionForAccount");
         BillingAccount billingAccount = accounts.get(accountID);
         Subscription sub = createSubscription(subscriptionRequest);
         billingAccount.addSubscription(sub);
         return createSubscriptionsFuture(sub);
      }

      @Override
      public ListenableFuture<Subscriptions> createSubscriptionForAccount(AccountRequest account, SubscriptionRequest subscription) {
         calls.add("createSubscriptionForAccount");
         return createSubscriptionForAccount(account.getAccountID(), subscription);
      }

      @Override
      public ListenableFuture<Subscriptions> previewSubscriptionChange(
            String accountID, SubscriptionRequest subscriptionRequest) {
         throw new UnsupportedOperationException("previewSubscriptionChange should not be called.");
      }

      @Override
      public ListenableFuture<Subscriptions> getSubscriptionsForAccount(String accountID) {
         calls.add("getSubscriptionsForAccount");
         BillingAccount billingAccount = accounts.get(accountID);
         List<Subscription> list = billingAccount.getSubscriptions();
         return createSubscriptionsFuture(list != null
               ? list
               : Collections.emptyList());
      }

      @Override
      public ListenableFuture<Subscription> cancelSubscription(String subscriptionID) {
         calls.add("cancelSubscription");
         Subscription sub = getSubscription(subscriptionID);
         sub.setState(Constants.STATE_CANCELED);
         return createSubscriptionFuture(sub);
      }

      @Override
      public ListenableFuture<Subscription> terminateSubscription(
            String subscriptionID,
            RefundType refund) {
         calls.add("terminateSubscription");
         Subscription sub = getSubscription(subscriptionID);
         sub.setState(Constants.STATE_EXPIRED);
         return createSubscriptionFuture(sub);
      }

      @Override
      public ListenableFuture<Subscription> reactiviateSubscription(String subscriptionID) {
         calls.add("reactivateSubscription");
         Subscription sub = getSubscription(subscriptionID);
         sub.setState(Constants.STATE_ACTIVE);
         return createSubscriptionFuture(sub);
      }

      @Override
      public ListenableFuture<Subscriptions> updateSubscription(SubscriptionRequest subscriptionRequest) {
         calls.add("updateSubscription");
         Subscription sub = getSubscription(subscriptionRequest.getSubscriptionID());
         updateSubscription(sub, subscriptionRequest);
         return createSubscriptionsFuture(sub);
      }

      @Override
      public ListenableFuture<Account> updateAccount(AccountRequest request) {
         calls.add("updateAccount");
         Account account = accounts.get(request.getAccountID()).getAccount();
         updateAccount(account, request);
         return createAccountFuture(account);
      }

      @Override
      public ListenableFuture<Account> getAccount(String accountID) {
         throw new UnsupportedOperationException("getAccount should not be called.");
      }

      @Override
      public ListenableFuture<Account> createAccount(AccountRequest account) {
         throw new UnsupportedOperationException("createAccount should not be called.");
      }

      @Override
      public ListenableFuture<Boolean> closeAccount(String accountID) {
         throw new UnsupportedOperationException("closeAccount should not be called.");
      }

      @Override
      public ListenableFuture<Account> reopenAccount(String accountID) {
         throw new UnsupportedOperationException("reopenAccount should not be called.");
      }

      @Override
      public ListenableFuture<AccountNotes> getNotesForAccount(String accountID) {
         throw new UnsupportedOperationException("getNotesForAccount should not be called.");
      }

      @Override
      public ListenableFuture<AccountBalance> getAccountBalance(String accountID) {
         throw new UnsupportedOperationException("getAccountBalance should not be called.");
      }

      @Override
      public ListenableFuture<Plans> getPlans() {
         throw new UnsupportedOperationException("getPlans should not be called.");
      }

      @Override
      public ListenableFuture<PlanAddons> getPlanAddons(String planCode) {
         throw new UnsupportedOperationException("getPlanAddons should not be called.");
      }

      @Override
      public ListenableFuture<BillingInfo> getBillingInfoForAccount(String accountID) {
         throw new UnsupportedOperationException("getBillingInfoForAccount should not be called.");
      }

      @Override
      public ListenableFuture<BillingInfo> createOrUpdateBillingInfoForAccount(String accountID, String billingToken) {
         throw new UnsupportedOperationException("createOrUpdateBillingInfoForAccount should not be called.");
      }

      @Override
      public ListenableFuture<BillingInfo> createOrUpdateBillingInfoNonCC(String accountID, BillingInfo billingInfo) {
         throw new UnsupportedOperationException("createOrUpdateBillingInfoNonCC should not be called.");
      }

      @Override
      public ListenableFuture<Boolean> clearBillingInfoFromAccount(String accountID) {
         throw new UnsupportedOperationException("clearBillingInfoFromAccount should not be called.");
      }

      @Override
      public ListenableFuture<Adjustments> getAdjustmentsForAccount(String accountID) {
         throw new UnsupportedOperationException("getAdjustmentsForAccount should not be called.");
      }

      @Override
      public ListenableFuture<Transactions> getTransactionsForAccount(String accountID) {
         throw new UnsupportedOperationException("getTransactionsForAccount should not be called.");
      }

      @Override
      public ListenableFuture<Transactions> getTransactionsForAccount(String accountID, Date beginTime, Date endTime) {
         throw new UnsupportedOperationException("getTransactionsForAccount should not be called.");
      }

      @Override
      public ListenableFuture<Invoices> getInvoicesForAccount(String accountID) {
         calls.add("getInvoicesForAccount");
         BillingAccount account = accounts.get(accountID);
         return createInvoicesFuture(account.getInvoices());
      }

      @Override
      public ListenableFuture<Invoice> getInvoice(String invoiceID) {
         throw new UnsupportedOperationException("getInvoice should not be called.");
      }

      @Override
      public ListenableFuture<Subscription> getSubscriptionDetails(String subscriptionID) {
         throw new UnsupportedOperationException("getSubscriptionDetails should not be called.");
      }

      private Subscription getSubscription(String subscriptionId) {
         for (BillingAccount account : accounts.values()) {
            for (Subscription subscription: account.getSubscriptions()) {
               if (subscriptionId.equals(subscription.getSubscriptionID())) {
                  return subscription;
               }
            }
         }
         return null;
      }

      private static Account updateAccount(Account account, AccountRequest request) {
         account.setEmail(request.getEmail());
         return account;
      }

      private static Subscription updateSubscription(Subscription sub, SubscriptionRequest request) {
         sub.setPlanCode(request.getPlanCode());
         sub.setQuantity(String.valueOf(request.getQuantity()));
         sub.clearSubscriptionAddOns();
         List<SubscriptionAddon> list = request.getSubscriptionAddons();
         sub.setSubscriptionAddOns(list != null ? list.stream().collect(Collectors.toList()) : Collections.emptyList());
         return sub;
      }

      private static Subscription createSubscription(SubscriptionRequest request) {
         Subscription sub = new Subscription();
         sub.setSubscriptionID(UUID.randomUUID().toString());
         sub.setCurrency("USD");
         sub.setPlanCode(request.getPlanCode());
         sub.setQuantity(request.getQuantity().toString());
         sub.setSubscriptionAddOns(request.getSubscriptionAddons());
         sub.setState(Constants.STATE_ACTIVE);
         return sub;
      }

      private static ListenableFuture<Invoices> createInvoicesFuture(Set<Invoice> invoices) {
         SettableFuture<Invoices> future = SettableFuture.create();
         Invoices invoiceList = new Invoices();
         invoiceList.addAll(invoices.stream().map(i -> i.copy()).collect(Collectors.toList()));
         future.set(invoiceList);
         return future;
      }

      private static ListenableFuture<Subscriptions> createSubscriptionsFuture(List<Subscription> subList) {
         SettableFuture<Subscriptions> future = SettableFuture.create();
         Subscriptions subs = new Subscriptions();
         subs.addAll(subList.stream().map(s -> s.copy()).collect(Collectors.toList()));
         future.set(subs);
         return future;
      }

      private static ListenableFuture<Account> createAccountFuture(Account account) {
         SettableFuture<Account> future = SettableFuture.create();
         future.set(account.copy());
         return future;
      }

      private static ListenableFuture<Subscriptions> createSubscriptionsFuture(Subscription sub) {
         Subscriptions subs = new Subscriptions();
         subs.add(sub.copy());
         return createSubscriptionsFuture(subs);
      }

      private static ListenableFuture<Subscription> createSubscriptionFuture(Subscription sub) {
         SettableFuture<Subscription> future = SettableFuture.create();
         future.set(sub.copy());
         return future;
      }
   }

   public static class FakeAccountDao extends Faker implements AccountDAO {
      private Map<UUID, com.iris.messages.model.Account> accounts = new HashMap<>();

      @Override
      public com.iris.messages.model.Account save(com.iris.messages.model.Account entity) {
         calls.add("save");
         if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
         }
         accounts.put(entity.getId(), entity);
         return entity;
      }

      @Override
      public com.iris.messages.model.Account create(
            com.iris.messages.model.Account account) {
         calls.add("create");
         return save(account);
      }



      @Override
      public com.iris.messages.model.Account findById(UUID id) {
         calls.add("findById");
         return accounts.get(id);
      }

      @Override
      public void delete(com.iris.messages.model.Account entity) {
         calls.add("delete");
      }

      @Override
      public ModelEntity findAccountModelById(UUID id) {
         throw new UnsupportedOperationException();
      }

      @Override
      public PagedResults<com.iris.messages.model.Account> listAccounts(AccountQuery query) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Stream<com.iris.messages.model.Account> streamAll() {
        throw new UnsupportedOperationException();
      }
   }

   public static class FakePlaceDao extends Faker implements PlaceDAO {
      private Map<UUID, Place> places = new HashMap<>();
      private Map<UUID, Boolean> updateFlags = new HashMap<>();

      @Override
      public Place save(Place entity) {
         calls.add("save");
         if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
         }
         places.put(entity.getId(), entity);
         return entity;
      }

      @Override
      public Place create(Place place) {
         calls.add("create");
         return save(place);
      }

      @Override
      public Place findById(UUID id) {
         calls.add("findById");
         return places.get(id);
      }

      @Override
      public void delete(Place entity) {
         calls.add("delete");
         // TODO Auto-generated method stub

      }

      @Override
      public Stream<Place> streamAll() {
         calls.add("streamAll");
         return places.values().stream();
      }

      @Override
      public List<Place> findByPlaceIDIn(Set<UUID> placeIDs) {
         calls.add("findByPlaceIDIn");
         return placeIDs.stream().map(pid -> places.get(pid)).collect(Collectors.toList());
      }

      @Override
      public void setUpdateFlag(UUID placeId, boolean updateFlag) {
         calls.add("setUpdateFlag");
         updateFlags.put(placeId, updateFlag);
      }

      @Override
      public boolean getUpdateFlag(UUID placeId) {
         calls.add("getUpdateFlag");
         return updateFlags.get(placeId) != null ? updateFlags.get(placeId) : false;
      }

      public Collection<Place> getPlaces() {
         return places.values();
      }

      @Override
      public ModelEntity findPlaceModelById(UUID placeId) {
         throw new UnsupportedOperationException();
      }

      @Override
      public PagedResults<Place> listPlaces(PlaceQuery query) {
         throw new UnsupportedOperationException();
      }

      @Override
      public Stream<Place> streamByPartitionId(int partitionId) {
         throw new UnsupportedOperationException();
      }

      @Override
      public Stream<Map<UUID,UUID>> streamPlaceAndAccountByPartitionId(int partitionId) {
         throw new UnsupportedOperationException();
      }

      @Override
      public UUID getAccountById(UUID placeId) {
         throw new UnsupportedOperationException();
      }

		@Override
		public String getPopulationById(UUID placeId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ServiceLevel getServiceLevelById(UUID placeId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Stream<Triple<UUID, UUID, ServiceLevel>> streamPlaceAndAccountAndServiceLevelByPartitionId(
				int partitionId) {
			throw new UnsupportedOperationException();
		}

   }

   public static class FakeAuthorizationGrantDao extends Faker implements AuthorizationGrantDAO {
      private List<AuthorizationGrant> grants = new ArrayList<>();

      public AuthorizationGrant findGrant(UUID entityId, UUID placeId) {
         return grants.stream().filter(g -> g.getEntityId() == entityId && g.getPlaceId() == placeId).findFirst().orElse(null);
      }

      private void deleteGrant(UUID entityId, UUID placeId) {
         AuthorizationGrant foundGrant = findGrant(entityId, placeId);
         if (foundGrant != null) {
            grants.remove(foundGrant);
         }
      }

      @Override
      public void save(AuthorizationGrant grant) {
         calls.add("save");
         deleteGrant(grant.getEntityId(), grant.getPlaceId());
         grants.add(grant);
      }

      @Override
      public List<AuthorizationGrant> findForEntity(UUID entityId) {
         calls.add("findForEntity");
         return grants.stream().filter(g -> g.getEntityId() == entityId).collect(Collectors.toList());
      }

      @Override
      public List<AuthorizationGrant> findForPlace(UUID placeId) {
         calls.add("findForPlace");
         return grants.stream().filter(g -> g.getPlaceId() == placeId).collect(Collectors.toList());
      }

      @Override
      public void removeGrant(UUID entityId, UUID placeId) {
         calls.add("removeGrant");
         deleteGrant(entityId, placeId);
      }

      @Override
      public void removeGrantsForEntity(UUID entityId) {
         calls.add("removeGrantsForEntity");
         grants.stream().filter(g -> g.getEntityId() == entityId).collect(Collectors.toList())
            .forEach(g -> grants.remove(g));
      }

      @Override
      public void removeForPlace(UUID placeId) {
         calls.add("removeGrantsForPlace");
         grants.stream().filter(g -> g.getPlaceId() == placeId).collect(Collectors.toList())
            .forEach(g -> grants.remove(g));
      }
   }

   protected void verifySubscriptionLevelChangeEvent(PlatformMessage msg, String serviceLevel) {
      verifySubscriptionLevelChangeEvent(msg, serviceLevel, (Set<String>) null);
   }

   protected void verifySubscriptionLevelChangeEvent(PlatformMessage msg, Set<String> addons) {
      verifySubscriptionLevelChangeEvent(msg, null, addons);
   }

   protected void verifySubscriptionLevelChangeEvent(PlatformMessage msg, String[] addons) {
      verifySubscriptionLevelChangeEvent(msg, null, addons);
   }

   protected void verifySubscriptionLevelChangeEvent(PlatformMessage msg, String serviceLevel, String[] addons) {
      Set<String> addonSet = null;
      if(addons != null) {
         addonSet = new HashSet<>(Arrays.asList(addons));
      }
      verifySubscriptionLevelChangeEvent(msg, serviceLevel, addonSet);
   }

   protected void verifySubscriptionLevelChangeEvent(PlatformMessage msg, String serviceLevel, Set<String> addons) {
      Assert.assertNotNull(msg);
      Assert.assertTrue(msg.getDestination().isBroadcast());
      Assert.assertTrue(msg.getSource().getRepresentation().startsWith("SERV:place:"));
      MessageBody body = msg.getValue();
      Assert.assertEquals(Capability.EVENT_VALUE_CHANGE, body.getMessageType());
      Assert.assertEquals(serviceLevel, body.getAttributes().get(PlaceCapability.ATTR_SERVICELEVEL));
      Set<String> actualAddons = null;
      Collection<String> tmp = (Collection<String>) body.getAttributes().get(PlaceCapability.ATTR_SERVICEADDONS);
      if(tmp != null) {
         actualAddons = new HashSet<>(tmp);
      }
      Assert.assertEquals(addons, actualAddons);

   }
   public static class PlaceData {
      public static final String NEW_PLACE_NAME = "New Place";
      public static final Boolean NEW_PLACE_VALIDATED = Boolean.TRUE;

      public String name = NEW_PLACE_NAME;
      public Boolean addrValidated = NEW_PLACE_VALIDATED;
      public Map<String, Object> attrs = new HashMap<>();


      public Map<String, Object> asAttributes() {
         return IrisCollections.<String, Object>map()
                  .put(PlaceCapability.ATTR_NAME, name)
                  .put(PlaceCapability.ATTR_ADDRVALIDATED, addrValidated)
                  .putAll(attrs)
                  .create();
      }
   }
}

