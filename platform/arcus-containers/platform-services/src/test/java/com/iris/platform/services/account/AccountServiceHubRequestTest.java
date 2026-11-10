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
package com.iris.platform.services.account;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.iris.core.dao.AccountDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.model.Account;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * hub-bridge runs no authorizer and forwards a hub's requests to any account, which would let a
 * hub do what RoleAuthorizer keeps to an account's owner, such as deleting it.
 */
@Mocks({AccountDAO.class})
@Modules(InMemoryMessageModule.class)
public class AccountServiceHubRequestTest extends IrisMockTestCase {
   @Inject private AccountDAO accountDao;
   @Inject private InMemoryPlatformMessageBus bus;

   private final Account account = new Account();
   private PlatformMessage handled;
   private AccountService service;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      account.setId(UUID.randomUUID());
      ContextualRequestMessageHandler<Account> delete = new ContextualRequestMessageHandler<Account>() {
         @Override
         public String getMessageType() {
            return AccountCapability.DeleteRequest.NAME;
         }

         @Override
         public MessageBody handleRequest(Account context, PlatformMessage msg) {
            handled = msg;
            return AccountCapability.DeleteResponse.instance();
         }
      };
      service = new AccountService(bus, MoreExecutors.directExecutor(), ImmutableSet.of(delete), ImmutableSet.<ContextualEventMessageHandler<Account>>of(), accountDao);
   }

   private PlatformMessage delete(Address source) {
      return PlatformMessage
            .buildRequest(AccountCapability.DeleteRequest.builder().build(), source, Address.platformService(account.getId(), AccountCapability.NAMESPACE))
            .withPlaceId(UUID.randomUUID())
            .withCorrelationId("test")
            .create();
   }

   @Test
   public void testHubCannotSendAccountRequests() throws Exception {
      replay();

      service.handleMessage(delete(Address.hubService("ABC-1234", "hub")));

      verify(); // strict mock: the account wasn't even loaded
      assertNull(handled);
      PlatformMessage response = bus.poll();
      assertNotNull(response);
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
   }

   @Test
   public void testClientRequestsStillReachTheHandler() throws Exception {
      // the owner check for clients is RoleAuthorizer's
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      PlatformMessage message = delete(Address.clientAddress("android", "1"));
      service.handleMessage(message);

      verify();
      assertEquals(message, handled);
      assertFalse(bus.poll().isError());
   }
}
