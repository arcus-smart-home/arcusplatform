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
package com.iris.platform.services.apikey.handlers;

import java.util.Objects;
import java.util.UUID;

import com.iris.core.dao.AccountDAO;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.security.authz.AuthzUtil;

/** Every apikey request is for the account owner only. */
final class AccountOwnerCheck {

   private AccountOwnerCheck() {
   }

   static void requireAccountOwner(AccountDAO accountDao, Place place, PlatformMessage msg) {
      if (msg.getActor() == null) {
         throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "actor is required");
      }

      Account account = accountDao.findById(place.getAccount());
      if (account == null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "account not found");
      }

      UUID actorId = (UUID) msg.getActor().getId();
      if (!Objects.equals(actorId, account.getOwner())) {
         throw new ErrorEventException(AuthzUtil.UNAUTHORIZED_CODE, "only the account owner can manage API keys");
      }
   }
}
