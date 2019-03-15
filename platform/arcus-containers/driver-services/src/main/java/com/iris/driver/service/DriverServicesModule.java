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
package com.iris.driver.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.cassandra.CassandraDAOModule;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.platform.PlatformModule;
import com.iris.core.platform.PlatformService;
import com.iris.core.protocol.ipcd.IpcdDeviceDaoModule;
import com.iris.driver.groovy.GroovyDriverModule;
import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.driver.registry.GroovyDriverRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.population.PlacePopulationCacheModule;

/**
 * Includes everything needed to run the full driver service.
 */
public class DriverServicesModule extends AbstractIrisModule {

   @Inject
   public DriverServicesModule(
         DriverModule driver,
         MessagesModule messages,
         KafkaModule kafka,
         GroovyProtocolPluginModule protocolPlugins,
         CassandraDAOModule daos,
         IpcdDeviceDaoModule ipcdDao,
         AttributeMapTransformModule attrTransforms,
         CapabilityRegistryModule capabilityRegistry,
         GroovyDriverModule groovy,
         PlatformModule platform,
         PlacePopulationCacheModule populationCache
   ) {
   }

   /* (non-Javadoc)
    * @see com.google.inject.AbstractModule#configure()
    */
   @Override
   protected void configure() {
      // add groovy suppport
      bindSetOf(DriverRegistry.class)
         .addBinding()
         .to(GroovyDriverRegistry.class);

      Multibinder<PlatformService> serviceBinder = bindSetOf(PlatformService.class);
      serviceBinder.addBinding().to(DeviceServiceHandler.class);
   }

   @Provides
   @Named(GroovyDriverModule.NAME_GROOVY_DRIVER_DIRECTORIES)
   public Set<URL> groovyDriverUrls(DriverConfig config) throws MalformedURLException {
      Set<URL> urls = new LinkedHashSet<>();
      urls.add(new File(config.evaluateAbsoluteDriverDirectory()).toURI().toURL());
      return urls;
   }

}

