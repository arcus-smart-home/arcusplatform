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


import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.AccountDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.core.platform.handlers.CreateAccountHandler;
import com.iris.messages.model.Account;
import com.iris.platform.services.account.handlers.*;
import com.iris.platform.services.handlers.AddTagsHandler;
import com.iris.platform.services.handlers.RemoveTagsHandler;
import com.iris.platform.subscription.IrisSubscriptionModule;
import com.iris.platform.subscription.SubscriptionUpdater;
import com.iris.platform.subscription.SubscriptionUpdaterImpl;

public class AccountServiceModule extends AbstractIrisModule {

	@Inject
	public AccountServiceModule(IrisSubscriptionModule subscriptionModule) {}

   @Override
   protected void configure() {

      bind(SubscriptionUpdater.class).to(SubscriptionUpdaterImpl.class);

      Multibinder<ContextualRequestMessageHandler<Account>> handlerBinder = bindSetOf(new TypeLiteral<ContextualRequestMessageHandler<Account>>() {});
      handlerBinder.addBinding().to(AccountGetAttributesHandler.class);
      handlerBinder.addBinding().to(AccountSetAttributesHandler.class);
      handlerBinder.addBinding().to(CreateAccountHandler.class);
      handlerBinder.addBinding().to(ListDevicesHandler.class);
      handlerBinder.addBinding().to(ListHubsHandler.class);
      handlerBinder.addBinding().to(ListPlacesHandler.class);
      handlerBinder.addBinding().to(SignupTransitionHandler.class);
      handlerBinder.addBinding().to(UpdateBillingInfoCCHandler.class);
      handlerBinder.addBinding().to(UpdateBillingInfoHandler.class);
      handlerBinder.addBinding().to(CreateBillingAccountHandler.class);
      handlerBinder.addBinding().to(UpdateServicePlanHandler.class);
      handlerBinder.addBinding().to(ListInvoicesHandler.class);
      handlerBinder.addBinding().to(ListAdjustmentsHandler.class);
      handlerBinder.addBinding().to(AddPlaceHandler.class);
      handlerBinder.addBinding().to(AccountDeleteHandler.class);
      handlerBinder.addBinding().to(IssueCreditHandler.class);
      handlerBinder.addBinding().to(IssueInvoiceRefundHandler.class);
      handlerBinder.addBinding().to(SkipPremiumTrialHandler.class);
      handlerBinder.addBinding().to(new TypeLiteral<AddTagsHandler<Account>>() {});
      handlerBinder.addBinding().to(new TypeLiteral<RemoveTagsHandler<Account>>() {});
      handlerBinder.addBinding().to(AccountActivateHandler.class);
      
      Multibinder<ContextualEventMessageHandler<Account>> eventHandlerBinder = bindSetOf(new TypeLiteral<ContextualEventMessageHandler<Account>>() {});
      eventHandlerBinder.addBinding().to(DelinquentAccountEventHandler.class);

      Multibinder<PlatformService> serviceBinder = bindSetOf(PlatformService.class);
      serviceBinder.addBinding().to(AccountService.class);
      serviceBinder.addBinding().to(AccountMigrationService.class);

   }

   @Provides
   @Singleton
   public AddTagsHandler<Account> accountAddTags(AccountDAO accountDao, PlatformMessageBus platformBus) {
      return new AddTagsHandler<Account>(accountDao, platformBus);
   }

   @Provides
   @Singleton
   public RemoveTagsHandler<Account> accountRemoveTags(AccountDAO accountDao, PlatformMessageBus platformBus) {
      return new RemoveTagsHandler<Account>(accountDao, platformBus);
   }
}

