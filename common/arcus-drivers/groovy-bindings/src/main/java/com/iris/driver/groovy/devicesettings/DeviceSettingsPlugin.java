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
package com.iris.driver.groovy.devicesettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.capability.Capability;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyDriverBuilder;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.metadata.DriverEventMatcher;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.messages.capability.DeviceSettingsCapability;

import groovy.lang.Closure;

/**
 * Plugin that registers the {@code deviceSettings { }} DSL block and wires up
 * all the handlers (GetAttributes, SetParam, GetParam, ConfigReport, Connected).
 */
public class DeviceSettingsPlugin implements GroovyDriverPlugin {
   private static final Logger log = LoggerFactory.getLogger(DeviceSettingsPlugin.class);

   @Override
   public void enhanceEnvironment(EnvironmentBinding binding) {
      binding.setProperty("deviceSettings", new DeviceSettingsDSLClosure(binding));
   }

   @Override
   public void postProcessEnvironment(EnvironmentBinding binding) {
      if (!(binding instanceof DriverBinding)) {
         return;
      }
      DriverBinding driverBinding = (DriverBinding) binding;
      GroovyDriverBuilder builder = driverBinding.getBuilder();
      DeviceSettingsContext ctx = builder.getDeviceSettingsContext();
      if (ctx == null) {
         return;
      }

      log.debug("Registering DeviceSettings capability with {} params", ctx.getParams().size());

      // Auto-add the DeviceSettings capability definition
      CapabilityDefinition capDef = builder.getCapabilityDefinitionByName(DeviceSettingsCapability.NAME);
      if (capDef != null) {
         builder.addCapabilityDefinition(capDef);
      } else {
         log.warn("DeviceSettings capability definition not found in registry");
      }

      // Register GetAttributes provider
      builder.addGetAttributesProvider(new DeviceSettingsGetAttributesProvider(ctx));

      // Register SetParam command handler
      PlatformEventMatcher setParamMatcher = new PlatformEventMatcher();
      setParamMatcher.setCapability(DeviceSettingsCapability.NAMESPACE);
      setParamMatcher.setEvent("SetParam");
      setParamMatcher.setHandler(new DeviceSettingsSetParamHandler(ctx));
      builder.addEventMatcher(setParamMatcher);

      // Register GetParam command handler
      PlatformEventMatcher getParamMatcher = new PlatformEventMatcher();
      getParamMatcher.setCapability(DeviceSettingsCapability.NAMESPACE);
      getParamMatcher.setEvent("GetParam");
      getParamMatcher.setHandler(new DeviceSettingsGetParamHandler(ctx));
      builder.addEventMatcher(getParamMatcher);

      // Register Z-Wave Configuration Report handler at the protocol level.
      // Using addProtocolHandler instead of addEventMatcher(ZWaveProtocolEventMatcher)
      // because the latter requires the matcher to be present before ZWaveProtocolPlugin
      // processes event matchers, but Guice Set iteration order is not guaranteed.
      DeviceSettingsConfigReportHandler configReportHandler = new DeviceSettingsConfigReportHandler(ctx);
      builder.addProtocolHandler(com.iris.protocol.zwave.ZWaveProtocol.NAMESPACE, configReportHandler);

      // Register DeviceConnected handler to read all params on connect
      DriverEventMatcher connectedMatcher = new DriverEventMatcher(DeviceConnectedEvent.class);
      connectedMatcher.setHandler(new DeviceSettingsConnectedHandler(ctx));
      builder.addEventMatcher(connectedMatcher);
   }

   @Override
   public void enhanceDriver(DriverBinding bindings, DeviceDriver driver) {
      // no-op
   }

   @Override
   public void enhanceCapability(CapabilityEnvironmentBinding bindings, Capability capability) {
      // no-op
   }

   /**
    * Closure implementation for the {@code deviceSettings { }} DSL block.
    * When called in the driver script, it creates a DeviceSettingsBlockContext,
    * runs the closure body, and stores the result on the builder.
    */
   @SuppressWarnings("serial")
   private static class DeviceSettingsDSLClosure extends Closure<Object> {
      private final EnvironmentBinding binding;

      DeviceSettingsDSLClosure(EnvironmentBinding binding) {
         super(binding);
         this.binding = binding;
      }

      protected void doCall(Closure<?> body) {
         DeviceSettingsBlockContext blockCtx = new DeviceSettingsBlockContext();
         body.setDelegate(blockCtx);
         body.setResolveStrategy(Closure.DELEGATE_FIRST);
         body.call();

         if (binding instanceof DriverBinding) {
            GroovyDriverBuilder builder = ((DriverBinding) binding).getBuilder();
            builder.setDeviceSettingsContext(blockCtx.build());
         }
      }
   }
}
