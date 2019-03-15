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

import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.model.Account;

@Singleton
public class SkipPremiumTrialHandler implements ContextualRequestMessageHandler<Account> {

   private final AccountDAO accountDao;

   @Inject
   public SkipPremiumTrialHandler(AccountDAO accountDao) {
      this.accountDao = accountDao;
   }

   @Override
   public String getMessageType() {
      return AccountCapability.SkipPremiumTrialRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      context.setTrialEnd(new Date());
      accountDao.save(context);
      return AccountCapability.SkipPremiumTrialResponse.instance();
   }
}

