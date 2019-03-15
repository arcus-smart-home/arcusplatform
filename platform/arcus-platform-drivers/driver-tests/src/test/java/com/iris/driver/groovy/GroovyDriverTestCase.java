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
package com.iris.driver.groovy;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.attribute.transform.ReflectiveBeanAttributesTransformer;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.common.scheduler.ExecutorScheduler;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.driver.DeviceDriver;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.customizer.DriverCompilationCustomizer;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.scheduler.SchedulerPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import groovy.util.GroovyScriptEngine;

/**
 *
 */
@Mocks({DeviceDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PlacePopulationCacheManager.class})
@Modules({ CapabilityRegistryModule.class, MessagesModule.class })
public abstract class GroovyDriverTestCase extends IrisMockTestCase {
   @Inject
   protected GroovyDriverFactory factory;
   @Inject
   protected CapabilityRegistry registry;
   @Inject
   protected DeviceDAO mockDeviceDao;
   @Inject
   protected PlaceDAO mockPlaceDao;
   @Inject
   protected PlacePopulationCacheManager mockPopulationCacheMgr;

   protected static String TMP_DIR = "build/tmp/groovy-driver";

   protected Set<GroovyDriverPlugin> getPlugins() {
      return ImmutableSet.of(new SchedulerPlugin(), new ControlProtocolPlugin(), new ZWaveProtocolPlugin());
   }

   protected GroovyScriptEngine scriptEngine() {
      return new GroovyScriptEngine(new ClasspathResourceConnector());
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      GroovyValidator.clear();

      Capture<Device> deviceRef = Capture.newInstance();
      EasyMock
         .expect(mockDeviceDao.save(EasyMock.capture(deviceRef)))
         .andAnswer(() -> {
            Device device = deviceRef.getValue().copy();
            if(device.getId() == null) {
               device.setId(UUID.randomUUID());
            }
            if(device.getCreated() == null) {
               device.setCreated(new Date());
            }
            device.setModified(new Date());
            return device;
         })
         .anyTimes();
      mockDeviceDao.updateDriverState(EasyMock.notNull(), EasyMock.notNull());
      EasyMock.expectLastCall().anyTimes();
      EasyMock.replay(mockDeviceDao);
   }

   @Override
   protected void configure(Binder binder) {
      binder
         .bind(new Key<Set<GroovyDriverPlugin>>() {})
         .toInstance(getPlugins());

      binder.bind(InMemoryPlatformMessageBus.class).in(Singleton.class);
      binder.bind(InMemoryProtocolMessageBus.class).in(Singleton.class);
      binder.bind(PlatformMessageBus.class).to(InMemoryPlatformMessageBus.class);
      binder.bind(ProtocolMessageBus.class).to(InMemoryProtocolMessageBus.class);
      
      Multibinder.newSetBinder(binder, CompilationCustomizer.class)
         .addBinding()
         .to(DriverCompilationCustomizer.class);

      binder
         .bind(Scheduler.class)
         .toInstance(new ExecutorScheduler(Executors.newScheduledThreadPool(1)))
         ;
   }

   @Provides @Singleton
   public BeanAttributesTransformer<Device> deviceAttributeTransformer(CapabilityRegistry capabilityRegistry) {
      return new ReflectiveBeanAttributesTransformer<Device>(capabilityRegistry, new HashSet<String>(Arrays.asList("dev", "devadv", "base")), Device.class);
   }

   @Provides
   @Singleton
   public GroovyScriptEngine scriptEngine(Set<CompilationCustomizer> customizers) {
      GroovyScriptEngine engine = scriptEngine();
      for(CompilationCustomizer customizer: customizers) {
         engine.getConfig().addCompilationCustomizers(customizer);
      }
      engine.getConfig().setScriptExtensions(ImmutableSet.of("driver", "capability", "groovy"));
      return engine;
   }

   protected Device createDevice(DeviceDriver driver) {
      Device device = Fixtures.createDevice();
      device.setDrivername(driver.getDefinition().getName());
      device.setDriverversion(driver.getDefinition().getVersion());
      device.setCaps(driver.getBaseAttributes().get(Capability.KEY_CAPS));
      device.setDevtypehint(driver.getBaseAttributes().get(DeviceCapability.KEY_DEVTYPEHINT));
      device.setVendor(driver.getBaseAttributes().get(DeviceCapability.KEY_VENDOR));
      device.setModel(driver.getBaseAttributes().get(DeviceCapability.KEY_MODEL));
      device.setProtocol(driver.getBaseAttributes().get(DeviceAdvancedCapability.KEY_PROTOCOL));
      device.setProtocolid(driver.getBaseAttributes().get(DeviceAdvancedCapability.KEY_PROTOCOLID));
      return device;
   }

}

