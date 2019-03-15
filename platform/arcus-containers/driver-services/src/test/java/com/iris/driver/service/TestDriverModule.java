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
package com.iris.driver.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.common.scheduler.ExecutorScheduler;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.EmptyResourceBundle;
import com.iris.core.dao.ResourceBundleDAO;
import com.iris.driver.DeviceDriver;
import com.iris.driver.Drivers;
import com.iris.driver.platform.PlatformDriverExecutorRegistry;
import com.iris.driver.platform.PlatformDriverService;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.handler.DriverServiceRequestHandler;
import com.iris.driver.service.handler.MessageHandler;
import com.iris.driver.service.handler.UpgradeDriverRequestHandler;
import com.iris.driver.service.registry.CompositeDriverRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.driver.service.registry.MapDriverRegistry;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.DriverId;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.Modules;

@Modules(include = CapabilityRegistryModule.class)
public class TestDriverModule extends AbstractIrisModule {
   @Override
   protected void configure() {
      bind(DriverConfig.class);
      // required to inject drivers into ServiceLocatorDriverRegistry
      bindMapToInstancesOf(
            new TypeLiteral<Map<DriverId, DeviceDriver>> () {},
            new Function<DeviceDriver, DriverId>() {
               @Override
               public DriverId apply(DeviceDriver driver) {
                  return driver.getDriverId();
               }
            }
      );
      bind(PlatformDriverService.class)
         .asEagerSingleton();
      bind(ResourceBundleDAO.class)
         .to(EmptyResourceBundle.class);

      Multibinder<DriverServiceRequestHandler> handlers = bindSetOf(DriverServiceRequestHandler.class);
      handlers.addBinding().to(UpgradeDriverRequestHandler.class);
      handlers.addBinding().to(MessageHandler.class);

      bindSetOf(DriverRegistry.class)
         .addBinding()
         .to(MapDriverRegistry.class);

      bind(DriverExecutorRegistry.class).to(PlatformDriverExecutorRegistry.class);
   }
   
   @Provides @Singleton
   public Scheduler scheduler() {
      return new ExecutorScheduler(Executors.newScheduledThreadPool(1));
   }

   @Provides @Singleton @Named(DriverConfig.NAMED_EXECUTOR)
   public ThreadPoolExecutor driverExecutor(DriverConfig config) {
      return
            new ThreadPoolBuilder()
               .withBlockingBacklog()
               .withMaxPoolSize(config.getDriverThreadPoolSize())
               .withNameFormat("driver-thread-%d")
               .build()
               ;
   }
   
   @Provides @Singleton @Named("ProtocolMatchers")
   public Set<AddressMatcher> provideProtocolAddressMatchers() {
      return AddressMatchers.platformNamespaces(MessageConstants.BROADCAST, MessageConstants.DRIVER);
   }

   @Provides @Singleton @Named("PlatformMatchers")
   public Set<AddressMatcher> providePlatformAddressMatchers() {
      return AddressMatchers.platformNamespaces(MessageConstants.DRIVER);
   }

   @Provides @Singleton
   public DriverRegistry provideDriverRegistry(Set<DriverRegistry> registries) {
      return new CompositeDriverRegistry(registries.toArray(new DriverRegistry[registries.size()]));
   }
   
   @Provides
   @Named("Fallback")
   public DeviceDriver fallbackDriver(CapabilityRegistry registry) {
      return 
            Drivers
               .builder()
               .withName("Fallback")
               .withVersion(Version.fromRepresentation("1.0"))
               .withMatcher((a) -> false)
               .withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_BETA, Population.NAME_QA))
               .addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
               .create(true)
               ;
   }
}

