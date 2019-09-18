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

import com.google.common.collect.ImmutableSet;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import groovy.lang.Binding;
import groovy.lang.Script;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import groovy.util.GroovyScriptEngine;
import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Test;

import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.driver.capability.Capability;
import com.iris.driver.event.DeviceAssociatedEvent;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DeviceDisassociatedEvent;
import com.iris.driver.event.DeviceDisconnectedEvent;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.customizer.DriverCompilationCustomizer;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.driver.handler.SetAttributesConsumer;
import com.iris.driver.metadata.DriverEventMatcher;
import com.iris.driver.metadata.EventMatcher;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.driver.metadata.ProtocolEventMatcher;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.model.Version;
import com.iris.validators.ValidationException;

/**
 *
 */
public class TestGroovyCapabilityBindings extends GroovyDriverTestCase {

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return Collections.<GroovyDriverPlugin>singleton(new ZWaveProtocolPlugin());
   }

   @Test
   public void testEmptyDriver() throws Exception {
      try {
         factory.loadCapability("Empty");
         fail("Laoded an empty capability");
      }
      catch(ValidationException e) {
         e.printStackTrace(System.out);
      }
   }

   @Test
   public void testMetaDataOnly() throws Exception {
      Capability capability = factory.loadCapability("Metadata");

      assertNotNull(capability.getHash());
      assertEquals(DeviceCapability.NAMESPACE, capability.getNamespace());
      assertEquals(DeviceCapability.NAME, capability.getCapabilityName());
      assertEquals("Metadata", capability.getName());
      assertEquals(new Version(1), capability.getVersion());
   }

   @Test
   public void testCapabilityObjects() throws Exception {
      Capability capability = factory.loadCapability("CapabilityObjects");

      assertNotNull(capability.getHash());
      assertEquals(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE), capability.getCapabilityDefinition());
      assertEquals("test-vendor", capability.getAttributes().get(DeviceCapability.KEY_VENDOR));
      assertEquals("test-model", capability.getAttributes().get(DeviceCapability.KEY_MODEL));
   }

   @Test
   public void testCapabilityStrings() throws Exception {
      Capability capability = factory.loadCapability("CapabilityStrings");

      assertNotNull(capability.getHash());
      assertEquals(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE), capability.getCapabilityDefinition());
      assertEquals("test-vendor", capability.getAttributes().get(DeviceCapability.KEY_VENDOR));
      assertEquals("test-model", capability.getAttributes().get(DeviceCapability.KEY_MODEL));
   }

   @Test
   public void testPlatformObjectMatchers() throws Exception {
      GroovyCapabilityBuilder builder = factory.loadCapabilityBinding("PlatformMessageHandlerObjects").getBuilder();

      List<EventMatcher> matchers = builder.getEventMatchers();
      System.out.println("Matchers: " + matchers);
      assertEquals(3, matchers.size());

      int idx = 0;
      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals(DoorLockCapability.NAMESPACE, matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals("AuthorizePerson",  matcher.getEvent());
         assertFalse(matcher.matchesAnyEvent());
      }

      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals(DoorLockCapability.NAMESPACE, matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals("DeauthorizePerson", matcher.getEvent());
         assertFalse(matcher.matchesAnyEvent());
      }

      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals(DoorLockCapability.NAMESPACE, matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals(null, matcher.getEvent());
         assertTrue(matcher.matchesAnyEvent());
      }

      List<GetAttributesProvider> providers = builder.getAttributeProviders();
      assertEquals(2, providers.size());
      assertEquals("doorlock", providers.get(0).getNamespace());
      assertEquals("doorlock", providers.get(1).getNamespace());

      List<SetAttributesConsumer> consumers = builder.getAttributeConsumers();
      assertEquals(2, consumers.size());
      assertEquals("doorlock", consumers.get(0).getNamespace());
      assertEquals("doorlock", consumers.get(1).getNamespace());

   }

   @Test
   public void testPlatformStringHandlers() throws Exception {
      GroovyCapabilityBuilder builder = factory.loadCapabilityBinding("PlatformMessageHandlerStrings").getBuilder();

      List<EventMatcher> matchers = builder.getEventMatchers();
      System.out.println("Matchers: " + matchers);
      assertEquals(3, matchers.size());

      int idx = 0;
      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals("doorlock", matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals("AuthorizePerson", matcher.getEvent());
         assertFalse(matcher.matchesAnyEvent());
      }

      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals("doorlock", matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals("DeauthorizePerson", matcher.getEvent());
         assertFalse(matcher.matchesAnyEvent());
      }

      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals("doorlock", matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals(null, matcher.getEvent());
         assertTrue(matcher.matchesAnyEvent());
      }

      List<GetAttributesProvider> providers = builder.getAttributeProviders();
      assertEquals(2, providers.size());
      assertEquals("doorlock", providers.get(0).getNamespace());
      assertEquals("doorlock", providers.get(1).getNamespace());

      List<SetAttributesConsumer> consumers = builder.getAttributeConsumers();
      assertEquals(2, consumers.size());
      assertEquals("doorlock", consumers.get(0).getNamespace());
      assertEquals("doorlock", consumers.get(1).getNamespace());
   }

   @Test
   public void testProtocolMessageHandlers() throws Exception {
      GroovyCapabilityBuilder builder = factory.loadCapabilityBinding("ProtocolMessageHandler").getBuilder();

      List<EventMatcher> matchers = builder.getEventMatchers();
      System.out.println("Matchers: " + matchers);
      assertEquals(2, matchers.size());

      int idx = 0;
      {
         ProtocolEventMatcher matcher = (ProtocolEventMatcher) matchers.get(idx++);
         assertEquals("ZWAV", matcher.getProtocolName());
         assertFalse(matcher.matchesAnyProtocol());
      }

      {
         ProtocolEventMatcher matcher = (ProtocolEventMatcher) matchers.get(idx++);
         assertEquals(null, matcher.getProtocolName());
         assertTrue(matcher.matchesAnyProtocol());
      }
   }

   @Test
   public void testDriverEventHandler() throws Exception {
      GroovyCapabilityBuilder builder = factory.loadCapabilityBinding("DriverEventHandlerCapability").getBuilder();

      List<EventMatcher> matchers = builder.getEventMatchers();
      System.out.println("Matchers: " + matchers);
      assertEquals(4, matchers.size());

      int idx = 0;
      {
         DriverEventMatcher matcher = (DriverEventMatcher) matchers.get(idx++);
         assertEquals(DeviceAssociatedEvent.class, matcher.getEventType());
      }

      {
         DriverEventMatcher matcher = (DriverEventMatcher) matchers.get(idx++);
         assertEquals(DeviceConnectedEvent.class, matcher.getEventType());
      }

      {
         DriverEventMatcher matcher = (DriverEventMatcher) matchers.get(idx++);
         assertEquals(DeviceDisconnectedEvent.class, matcher.getEventType());
      }

      {
         DriverEventMatcher matcher = (DriverEventMatcher) matchers.get(idx++);
         assertEquals(DeviceDisassociatedEvent.class, matcher.getEventType());
      }
   }

   @Test
   public void testCompile() throws Exception {
      CompilerConfiguration config = new CompilerConfiguration();
      config.setTargetDirectory(new File(TMP_DIR));
      config.addCompilationCustomizers(new DriverCompilationCustomizer(ServiceLocator.getInstance(CapabilityRegistry.class)));
      org.codehaus.groovy.tools.Compiler compiler = new org.codehaus.groovy.tools.Compiler(config);
      compiler.compile(new File("src/test/resources/Metadata.capability"));
      ClassLoader loader = new ClassLoader() {

         @Override
         protected Class<?> findClass(String name) throws ClassNotFoundException {
            File f = new File(TMP_DIR + "/" + name.replaceAll("\\.", "/") + ".class");
            if(!f.exists()) {
               throw new ClassNotFoundException();
            }
            try (FileInputStream is = new FileInputStream(f)) {
               byte [] bytes = IOUtils.toByteArray(is);
               return defineClass(name, bytes, 0, bytes.length);
            }
            catch(Exception e) {
               throw new ClassNotFoundException("Unable to load " + name, e);
            }
         }

      };
      Class<?> metadataClass = loader.loadClass("Metadata");
      System.out.println(metadataClass);
      System.out.println("Superclass: " + metadataClass.getSuperclass());
      System.out.println("Interfaces: " + Arrays.asList(metadataClass.getInterfaces()));
      System.out.println("Methods: ");
      for(Method m: Arrays.asList(metadataClass.getMethods())) {
         System.out.println("\t" + m);
      }

      GroovyCapabilityBuilder builder =
            new GroovyCapabilityBuilder(ServiceLocator.getInstance(CapabilityRegistry.class))
               .withName("Metadata");
      CapabilityEnvironmentBinding binding = new CapabilityEnvironmentBinding(builder);
      Script s = (Script) metadataClass.getConstructor(Binding.class).newInstance(binding);
      s.setMetaClass(new CapabilityScriptMetaClass(s.getClass()));
      s.getMetaClass().initialize();
      s.setBinding(binding);
      s.run();
      System.out.println("Definition: " + builder.create().getCapabilityDefinition());
   }

   @Test
   public void testPlatformDeviceConnectionImport() {
      CapabilityRegistry registry = ServiceLocator.getInstance(CapabilityRegistry.class);

      CompilerConfiguration config = new CompilerConfiguration();
      config.setTargetDirectory(new File(TMP_DIR));
      config.addCompilationCustomizers(new DriverCompilationCustomizer(registry));

      GroovyScriptEngine engine = new GroovyScriptEngine(new ClasspathResourceConnector());
      engine.setConfig(config);
      DriverBinding binding = new DriverBinding(
              ServiceLocator.getInstance(CapabilityRegistry.class),
              new GroovyDriverFactory(engine, registry, ImmutableSet.of(new ControlProtocolPlugin()))
      );
      GroovyDriverBuilder builder = binding.getBuilder();
      assertNotNull(builder.importCapability(GroovyDrivers.PLATFORM_DEVICE_CONNECTION_CAPABILITY));
   }
}

