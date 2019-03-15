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

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.capability.Capability;
import com.iris.driver.event.DeviceAssociatedEvent;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DeviceDisassociatedEvent;
import com.iris.driver.event.DeviceDisconnectedEvent;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.customizer.DriverCompilationCustomizer;
import com.iris.driver.groovy.zwave.ZWaveProtocolEventMatcher;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.driver.metadata.DriverEventMatcher;
import com.iris.driver.metadata.EventMatcher;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.driver.metadata.ProtocolEventMatcher;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.util.IrisCollections;

/**
 *
 */
public class TestGroovyDriverBindings extends GroovyDriverTestCase {

   @Test
   public void testEmptyDriver() throws Exception {
      DriverBinding binding = factory.loadBindings("Empty.driver");

      GroovyDriverBuilder builder = binding.getBuilder();
      // everything currently defaults to null
      assertEquals(null, builder.getName());
      assertEquals(null, builder.getVersion());
      assertEquals(null, builder.getDescription());

      AttributeMap attributes = builder.getAttributes();
      assertEquals(IrisCollections.setOf(DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE, DeviceConnectionCapability.NAMESPACE, com.iris.messages.capability.Capability.NAMESPACE), attributes.get(com.iris.messages.capability.Capability.KEY_CAPS));
      assertEquals(
            IrisCollections.setOf(com.iris.messages.capability.Capability.NAME, DeviceCapability.NAME, DeviceAdvancedCapability.NAME, DeviceConnectionCapability.NAME),
            builder.getSupportedCapabilityNames()
      );
   }

   @Test
   public void testInterpretMetaDataOnly() throws Exception {
      DriverBinding binding = factory.loadBindings("Metadata.driver");

      GroovyDriverBuilder builder = binding.getBuilder();
      assertEquals("Iris Nifty Switch", builder.getName());
      assertEquals(new Version(1), builder.getVersion());
      assertEquals("Driver for a nifty Iris switch", builder.getDescription());

      AttributeMap attributes = builder.getAttributes();
      assertEquals("Iris", attributes.get(DeviceCapability.KEY_VENDOR));
      assertEquals("nifty-001", attributes.get(DeviceCapability.KEY_MODEL));
      assertEquals("switch", attributes.get(DeviceCapability.KEY_DEVTYPEHINT));

      assertEquals("Iris Nifty Switch", attributes.get(DeviceAdvancedCapability.KEY_DRIVERNAME));
      assertEquals("1.0", attributes.get(DeviceAdvancedCapability.KEY_DRIVERVERSION));
      assertEquals("Z-Wave", attributes.get(DeviceAdvancedCapability.KEY_PROTOCOL));
      assertEquals("sub-protocol", attributes.get(DeviceAdvancedCapability.KEY_SUBPROTOCOL));
      assertEquals(
            IrisCollections.setOf(DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE, DeviceConnectionCapability.NAMESPACE, DevicePowerCapability.NAMESPACE, SwitchCapability.NAMESPACE, com.iris.messages.capability.Capability.NAMESPACE),
            attributes.get(com.iris.messages.capability.Capability.KEY_CAPS)
      );
   }

   @Test
   public void testInterpretCapabilityObjects() throws Exception {
      DriverBinding binding = factory.loadBindings("CapabilityObjects.driver");
      GroovyDriverBuilder builder = binding.getBuilder();


      assertEquals(
            IrisCollections.setOf(com.iris.messages.capability.Capability.NAME, DeviceCapability.NAME, DeviceAdvancedCapability.NAME, DeviceConnectionCapability.NAME, DevicePowerCapability.NAME, SwitchCapability.NAME, DoorLockCapability.NAME),
            builder.getSupportedCapabilityNames()
      );

      // no handler specified
      assertEquals(0, builder.getCapabilityImplementation(DevicePowerCapability.NAME).size());

      // handler name and version
      {
         Capability capability = builder.getCapabilityImplementation(SwitchCapability.NAME).iterator().next();
         assertEquals(SwitchCapability.NAME, capability.getCapabilityName());
         assertEquals("ZWaveSwitchHandler", capability.getName());
      }

      // handler name only
      {
         Capability capability = builder.getCapabilityImplementation(DoorLockCapability.NAME).iterator().next();
         assertEquals(DoorLockCapability.NAME, capability.getCapabilityName());
         assertEquals("ZWaveDoorLockHandler", capability.getName());
      }

   }

   @Test
   public void testInterpretCapabilityStrings() throws Exception {
      DriverBinding binding = factory.loadBindings("CapabilityStrings.driver");
      GroovyDriverBuilder builder = binding.getBuilder();
      builder.assertValid();

      assertEquals(
            IrisCollections.setOf(com.iris.messages.capability.Capability.NAME, DeviceCapability.NAME, DeviceAdvancedCapability.NAME, DeviceConnectionCapability.NAME, DevicePowerCapability.NAME, SwitchCapability.NAME, DoorLockCapability.NAME),
            builder.getSupportedCapabilityNames()
      );

      // no handler specified
      assertEquals(0, builder.getCapabilityImplementation(DevicePowerCapability.NAME).size());

      // handler name and version
      {
         Capability capability = builder.getCapabilityImplementation(SwitchCapability.NAME).iterator().next();
         assertEquals(SwitchCapability.NAME, capability.getCapabilityName());
         assertEquals("ZWaveSwitchHandler", capability.getName());
      }

      // handler name only
      {
         Capability capability = builder.getCapabilityImplementation(DoorLockCapability.NAME).iterator().next();
         assertEquals(DoorLockCapability.NAME, capability.getCapabilityName());
         assertEquals("ZWaveDoorLockHandler", capability.getName());
      }

   }

   @Test
   public void testInterpretPlatformObjectHandlers() throws Exception {
      DriverBinding binding = factory.loadBindings("PlatformMessageHandlerObjects.driver");

      List<EventMatcher> matchers = binding.getBuilder().getEventMatchers();
      System.out.println("Matchers: " + matchers);
      assertEquals(2, matchers.size());

      int idx = 0;
      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals("doorlock", matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals(null, matcher.getEvent());
         assertTrue(matcher.matchesAnyEvent());
      }

      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals(null, matcher.getCapability());
         assertTrue(matcher.matchesAnyCapability());
         assertEquals(null, matcher.getEvent());
         assertTrue(matcher.matchesAnyEvent());
      }

      List<GetAttributesProvider> providers = binding.getBuilder().getAttributeProviders();
      assertEquals(1, providers.size());
      assertEquals(DeviceCapability.NAMESPACE, providers.get(0).getNamespace());
   }

   @Test
   public void testInterpretPlatformStringHandlers() throws Exception {
      DriverBinding binding = factory.loadBindings("PlatformMessageHandlerStrings.driver");

      List<EventMatcher> matchers = binding.getBuilder().getEventMatchers();
      System.out.println("Matchers: " + matchers);
      assertEquals(2, matchers.size());

      int idx = 0;
      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals("doorlock", matcher.getCapability());
         assertFalse(matcher.matchesAnyCapability());
         assertEquals(null, matcher.getEvent());
         assertTrue(matcher.matchesAnyEvent());
      }

      {
         PlatformEventMatcher matcher = (PlatformEventMatcher) matchers.get(idx++);
         assertEquals(null, matcher.getCapability());
         assertTrue(matcher.matchesAnyCapability());
         assertEquals(null, matcher.getEvent());
         assertTrue(matcher.matchesAnyEvent());
      }

      List<GetAttributesProvider> providers = binding.getBuilder().getAttributeProviders();
      assertEquals(1, providers.size());
      assertEquals(DeviceCapability.NAMESPACE, providers.get(0).getNamespace());
   }

   @Test
   public void testInterpretProtocolMessageHandlers() throws Exception {
      DriverBinding binding = factory.loadBindings("ProtocolMessageHandler.driver");

      List<EventMatcher> matchers = binding.getBuilder().getEventMatchers();
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
   public void testInterpretZWaveMessageHandlers() throws Exception {
      DriverBinding binding = factory.loadBindings("ZWaveMessageHandler.driver");

      List<EventMatcher> matchers = binding.getBuilder().getEventMatchers();
      System.out.println("Matchers: " + matchers);
      assertEquals(5, matchers.size());

      int idx = 0;
      {
         ZWaveProtocolEventMatcher matcher = (ZWaveProtocolEventMatcher) matchers.get(idx++);
         assertEquals("ZWAV", matcher.getProtocolName());
         assertFalse(matcher.matchesAnyProtocol());
         assertEquals(Byte.valueOf((byte) 0xef), matcher.getCommandClass());
         assertFalse(matcher.matchesAnyCommandClass());
         assertEquals(Byte.valueOf((byte) 3), matcher.getCommandId());
         assertFalse(matcher.matchesAnyCommandId());
         assertNotNull(matcher.getHandler());
      }

      {
         ZWaveProtocolEventMatcher matcher = (ZWaveProtocolEventMatcher) matchers.get(idx++);
         assertEquals("ZWAV", matcher.getProtocolName());
         assertFalse(matcher.matchesAnyProtocol());
         assertEquals(Byte.valueOf((byte) 0xef), matcher.getCommandClass());
         assertFalse(matcher.matchesAnyCommandClass());
         assertEquals(null, matcher.getCommandId());
         assertTrue(matcher.matchesAnyCommandId());
         assertNotNull(matcher.getHandler());
      }

      {
         ZWaveProtocolEventMatcher matcher = (ZWaveProtocolEventMatcher) matchers.get(idx++);
         assertEquals("ZWAV", matcher.getProtocolName());
         assertFalse(matcher.matchesAnyProtocol());
         assertEquals(Byte.valueOf((byte) 0x25), matcher.getCommandClass());
         assertFalse(matcher.matchesAnyCommandClass());
         assertEquals(Byte.valueOf((byte) 1), matcher.getCommandId());
         assertFalse(matcher.matchesAnyCommandId());
         assertNotNull(matcher.getHandler());
      }

      {
         ZWaveProtocolEventMatcher matcher = (ZWaveProtocolEventMatcher) matchers.get(idx++);
         assertEquals("ZWAV", matcher.getProtocolName());
         assertFalse(matcher.matchesAnyProtocol());
         assertEquals(Byte.valueOf((byte) 0x25), matcher.getCommandClass());
         assertFalse(matcher.matchesAnyCommandClass());
         assertEquals(null, matcher.getCommandId());
         assertTrue(matcher.matchesAnyCommandId());
         assertNotNull(matcher.getHandler());
      }

      {
         ZWaveProtocolEventMatcher matcher = (ZWaveProtocolEventMatcher) matchers.get(idx++);
         assertEquals("ZWAV", matcher.getProtocolName());
         assertFalse(matcher.matchesAnyProtocol());
         assertEquals(null, matcher.getCommandClass());
         assertTrue(matcher.matchesAnyCommandClass());
         assertEquals(null, matcher.getCommandId());
         assertTrue(matcher.matchesAnyCommandId());
         assertNotNull(matcher.getHandler());
      }
   }

   @Test
   public void testInterpretDriverEvents() throws Exception {
      DriverBinding binding = factory.loadBindings("DriverEventHandler.driver");

      List<EventMatcher> matchers = binding.getBuilder().getEventMatchers();
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
//   @Ignore
   public void testCompile() throws Exception {
      CapabilityRegistry registry = ServiceLocator.getInstance(CapabilityRegistry.class);

      CompilerConfiguration config = new CompilerConfiguration();
      config.setTargetDirectory(new File(TMP_DIR));
      config.addCompilationCustomizers(new DriverCompilationCustomizer(registry));
      org.codehaus.groovy.tools.Compiler compiler = new org.codehaus.groovy.tools.Compiler(config);
      compiler.compile(new File("src/test/resources/Metadata.driver"));
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

      GroovyScriptEngine engine = new GroovyScriptEngine(new ClasspathResourceConnector());
      engine.setConfig(config);
      DriverBinding binding = new DriverBinding(
            ServiceLocator.getInstance(CapabilityRegistry.class),
            new GroovyDriverFactory(engine, registry, ImmutableSet.of(new ControlProtocolPlugin()))
      );
      Script s = (Script) metadataClass.getConstructor(Binding.class).newInstance(binding);
      s.setMetaClass(new DriverScriptMetaClass(s.getClass()));
      s.setBinding(binding);
      s.run();
      System.out.println("Definition: " + binding.getBuilder().createDefinition());
   }
   
   @Test
   public void testTestDefaultPopulations() {
   	List<String> popList = factory.getDefaultPopulations();
   	assertNotNull(popList);
   	assertTrue(popList.contains(Population.NAME_GENERAL));
   	assertTrue(popList.contains(Population.NAME_BETA));
   	assertTrue(popList.contains(Population.NAME_QA));
   }
}

