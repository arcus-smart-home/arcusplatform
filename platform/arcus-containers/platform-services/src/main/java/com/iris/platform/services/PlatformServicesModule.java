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
/**
 *
 */
package com.iris.platform.services;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.cassandra.CassandraDAOModule;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.platform.PlatformModule;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.address.validation.AddressValidator;
import com.iris.platform.address.validation.NoopAddressValidator;
import com.iris.platform.address.validation.smartystreets.SmartyStreetsValidator;
import com.iris.platform.pairing.customization.RuleTemplateRequestor;
import com.iris.platform.services.account.AccountService;
import com.iris.platform.services.hub.HubService;
import com.iris.platform.services.hub.handlers.HubEventListener;
import com.iris.platform.services.hub.handlers.OfflineHubRequestHandler;
import com.iris.platform.services.mobiledevice.MobileDeviceService;
import com.iris.platform.services.person.PersonService;
import com.iris.platform.services.place.PlaceService;
import com.iris.platform.services.population.PopulationService;
import com.iris.platform.services.productcatalog.ProductCatalogService;
import com.iris.prodcat.ProductCatalogReloadListener;
import com.iris.util.ThreadPoolBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines all the necessary stuff for the platform service.
 */
public class PlatformServicesModule extends AbstractModule {
   private static final Logger logger = LoggerFactory.getLogger(PlatformServicesModule.class);

   @Inject(optional = true) @Named("platform.service.threads.max")
   private int threads = 100;

   @Inject(optional = true) @Named("platform.service.threads.keepAliveMs")
   private int keepAliveMs = 10000;

   @Inject(optional=true) @Named("platform.address.validator")
   private String addressValidator = "default";

   private ExecutorService executor;
   
   @Inject
   public PlatformServicesModule(
         MessagesModule messages,
         KafkaModule kafka,
         CassandraDAOModule daos,
         PlatformModule platform,
         ServicesModule services
   ) {

   }

   @Override
   protected void configure() {
      executor =
            new ThreadPoolBuilder()
               .withBlockingBacklog()
               .withMaxPoolSize(threads)
               .withKeepAliveMs(keepAliveMs)
               .withNameFormat("platform-services-%d")
               .withMetrics("service.platform")
               .build()
               ;
      
		bind(RuleTemplateRequestor.class);

		switch (addressValidator) {
           default:
              logger.warn("unknown address validator {}: using default instead");
              // fall through
           case "default":
           case "smartystreets":
              logger.info("using smartystreets address validator");
              bind(AddressValidator.class).to(SmartyStreetsValidator.class);
              break;
           case "noop":
              logger.warn("using noop address validator");
              bind(AddressValidator.class).to(NoopAddressValidator.class);
              break;
        }
   }

   @PreDestroy
   public void stop() {
      executor.shutdown();
   }
   
   @Provides @Named(AccountService.PROP_THREADPOOL)
   public Executor accountExecutor() {
      return executor;
   }

   @Provides @Named(HubService.PROP_THREADPOOL)
   public Executor hubExecutor() {
      return executor;
   }

   @Provides @Named(MobileDeviceService.PROP_THREADPOOL)
   public Executor mobileDeviceExecutor() {
      return executor;
   }

   @Provides @Named(PersonService.PROP_THREADPOOL)
   public Executor personExecutor() {
      return executor;
   }

   @Provides @Named(PlaceService.PROP_THREADPOOL)
   public Executor placeExecutor() {
      return executor;
   }

   @Provides @Named(PopulationService.PROP_THREADPOOL)
   public Executor populationExecutor() {
      return executor;
   }

   @Provides @Named(ProductCatalogService.PROP_THREADPOOL)
   public Executor productCatalogExecutor() {
      return executor;
   }

   @Provides @Named(HubEventListener.PROP_THREADPOOL)
   public Executor hubEventListenerExecutor() {
      return executor;
   }
   
   @Provides @Named(OfflineHubRequestHandler.PROP_THREADPOOL)
   public Executor offlineHubExecutor() {
      return executor;
   }
   
   @Provides @Named(RuleTemplateRequestor.NAME_EXECUTOR)
   public Executor ruleTemplateRequestExecutor() {
      return executor;
   }
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString(MessageConstants.SERVICE + ":" + PlatformConstants.NAMESPACE + ":");
   }
}

