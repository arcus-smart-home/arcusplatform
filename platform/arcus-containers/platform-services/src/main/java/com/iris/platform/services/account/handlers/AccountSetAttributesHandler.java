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

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.AccountDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Account;
import com.iris.platform.services.AbstractSetAttributesPlatformMessageHandler;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class AccountSetAttributesHandler extends AbstractSetAttributesPlatformMessageHandler<Account> {

   private final AccountDAO accountDao;
   
   @Inject
   public AccountSetAttributesHandler(
         CapabilityRegistry capabilityRegistry,
         BeanAttributesTransformer<Account> accountTransformer,
         PlatformMessageBus platformBus,
         AccountDAO accountDao,
         PlacePopulationCacheManager populationCacheMgr) {

      super(capabilityRegistry, accountTransformer, platformBus, populationCacheMgr);
      this.accountDao = accountDao;
   }

	@Override
   protected void save(Account bean) {
      accountDao.save(bean);
   }

   @Override
   protected void sendValueChangeEvent(Account context, PlatformMessage request,
			Map<String, Object> changes) {
		//send value change event for each place under the account
	   context.getPlaceIDs().forEach((curPlaceId) -> {
		   sendValueChangeEventForPlace(request, changes, curPlaceId.toString());
	   });
   }

}

