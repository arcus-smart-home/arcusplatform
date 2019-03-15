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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverBuilder;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.Drivers;
import com.iris.driver.capability.Capability;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.context.AttributesContext;
import com.iris.driver.groovy.context.ConnectedClosure;
import com.iris.driver.groovy.context.DisconnectedClosure;
import com.iris.driver.groovy.context.EmitEventClosure;
import com.iris.driver.groovy.context.ForwardProtocolMessageClosure;
import com.iris.driver.groovy.context.LastProtocolMessageTimestampClosure;
import com.iris.driver.groovy.context.LogClosure;
import com.iris.driver.groovy.context.MisconfiguredClosure;
import com.iris.driver.groovy.context.MispairedClosure;
import com.iris.driver.groovy.context.NamespacedAttributeContext;
import com.iris.driver.groovy.context.NowClosure;
import com.iris.driver.groovy.context.ProvisionedClosure;
import com.iris.driver.groovy.context.ProvisioningClosure;
import com.iris.driver.groovy.context.SendPlatformMessageClosure;
import com.iris.driver.groovy.context.SendProtocolMessageClosure;
import com.iris.driver.groovy.context.SendResponseClosure;
import com.iris.driver.groovy.context.VariablesContext;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.util.IrisCollections;
import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

/**
 *
 */
@Singleton
public class GroovyDriverFactory {
   private static final Logger logger = LoggerFactory.getLogger(GroovyDriverBuilder.class);

   private final GroovyScriptEngine engine;
   private final CapabilityRegistry registry;
   private final Set<GroovyDriverPlugin> plugins;

   @Inject(optional=true) @Named("groovy.driver.factory.allow.precompiled")
   private boolean allowPrecompiledDrivers = true;

   @Inject(optional=true) @Named("driver.reflexes.run.lifecycle.actions")
   private boolean reflexesRunLifecycleActions = true;
   
   @Inject(optional=true) @Named("groovy.driver.default.populations")
   private String defaultPopulationsStr = "general,qa,beta";
   
   private final AtomicReference<List<String>> defaultPopulations = new AtomicReference<>();

   @Inject
   public GroovyDriverFactory(
         GroovyScriptEngine engine,
         CapabilityRegistry registry,
         Set<GroovyDriverPlugin> plugins
   ) {
      this.engine = engine;
      this.registry = registry;
      this.plugins = IrisCollections.unmodifiableCopy(plugins);
   }   

   // TODO cache this?
   protected DriverBinding loadBindings(String driver) throws Exception {
      DriverBinding binding = new DriverBinding(registry, this);
      beforeCompile(binding);

      Script script = null;
      if (allowPrecompiledDrivers) {
         try {
            if (driver != null && driver.endsWith(".driver")) {
               String clsname = driver.substring(0, driver.length() - ".driver".length())
                  .replace('.', '_');

               Class<?> cls = engine.getGroovyClassLoader().loadClass(clsname,false,false,true);
               script = InvokerHelper.createScript(cls, binding);
            }
         } catch (ClassNotFoundException ex) {
            // ignore
         } catch (Exception ex) {
            logger.info("failed to load precompiled class:", ex);
         }
      }

      if (script == null) {
         script = engine.createScript(driver, binding);
      }

      GroovyValidator.addDriverClassname(script.getClass().getName());

      binding
         .getBuilder()
         .withHash(getHash(script))
         ;

      DriverScriptMetaClass metaClass = new DriverScriptMetaClass(script.getClass());
      script.setMetaClass(metaClass);
      script.run();

      enhanceScript(script, binding);

      metaClass.freeze();
      return binding;
   }

   protected Capability loadCapability(String capabilityName) throws ValidationException {
      CapabilityEnvironmentBinding binding = null;
      try {
         binding = loadCapabilityBinding(capabilityName);
      } catch (ValidationException ex) {
         throw ex;
      } catch(Exception e) {
         throw new ValidationException("Unable to load capability " + capabilityName + ": " + e.getMessage(), e);
      }
      return createCapabilityFromBindings(binding);
   }

   protected CapabilityEnvironmentBinding loadCapabilityBinding(String capabilityName) throws Exception {
      return (CapabilityEnvironmentBinding)loadCapabilityScript(capabilityName).script.getBinding();
   }

   protected ScriptAndValidator loadCapabilityScript(String capabilityName) throws Exception {
      try (AutoCloseable pop = GroovyValidator.push()) {
         GroovyCapabilityBuilder builder = new GroovyCapabilityBuilder(registry);
         CapabilityEnvironmentBinding binding = new CapabilityEnvironmentBinding(builder);
         beforeCompile(binding);

         Script capabilityScript = null;
         if (allowPrecompiledDrivers) {
            try {
               if (capabilityName != null) {
                  String capname = capabilityName;
                  if (capname.endsWith(GroovyDrivers.GROOVY_CAPABILITY_EXTENSION)) {
                     capname = capname.substring(0, capname.length() - GroovyDrivers.GROOVY_CAPABILITY_EXTENSION.length());
                  }

                  int idx = capname.lastIndexOf('/');
                  if (idx > 0) {
                     capname = capname.substring(idx+1);
                  }

                  idx = capname.lastIndexOf('.');
                  if (idx > 0) {
                     capname = capname.substring(idx+1);
                  }

                  String clsname = capname
                     .replace('.', '_')
                     .replace('/', '.');

                  Class<?> cls = engine.getGroovyClassLoader().loadClass(clsname,false,false,true);
                  capabilityScript = InvokerHelper.createScript(cls, binding);
               }
            } catch (ClassNotFoundException ex) {
               // ignore
            } catch (Exception ex) {
               logger.info("failed to load precompiled capability class:", ex);
            }
         }

         if (capabilityScript == null) {
            capabilityScript = engine.createScript(capabilityName.replace('.', File.separatorChar) + "." + GroovyDrivers.GROOVY_CAPABILITY_EXTENSION, binding);
         }

         //Script capabilityScript = engine.createScript(capabilityName.replace('.', File.separatorChar) + "." + GroovyDrivers.GROOVY_CAPABILITY_EXTENSION, binding);
         String name = capabilityScript.getClass().getName();
         GroovyValidator.addDriverClassname(name);

         capabilityScript.setMetaClass(new CapabilityScriptMetaClass(capabilityScript.getClass()));
         capabilityScript.getMetaClass().initialize();

         builder
            .withName(name)
            .withHash(getHash(capabilityScript))
            ;
         capabilityScript.run();

         enhanceScript(capabilityScript, binding);
         return new ScriptAndValidator(capabilityScript, GroovyValidator.getValidator());
      }
   }

   protected Capability createCapabilityFromBindings(CapabilityEnvironmentBinding binding) throws ValidationException {
      beforeConstruction(binding);
      Capability capability = binding.getBuilder().create();
      afterBuilt(binding, capability);
      return capability;
   }

   private String getHash(Script script) {
      try {
         return (String) script.getClass().getField("_HASH").get(script);
      }
      catch (IllegalArgumentException | IllegalAccessException
            | NoSuchFieldException | SecurityException e) {
         logger.warn("Unable to get script hash {}", e);
         return null;
      }
   }

   private void enhanceScript(Script script, EnvironmentBinding binding) {
      // TODO push this all down to the MetaClass?
      // TODO load these all via injection
      script.setProperty("log", new LogClosure(script));

      if(binding instanceof DriverBinding) {
         Map<String, AttributeDefinition> definitions = ((DriverBinding) binding).getBuilder().getSupportedAttributes();
         script.setProperty("attributes", new AttributesContext(definitions));
      }
      else {
         CapabilityDefinition definition = ((CapabilityEnvironmentBinding) binding).getBuilder().getCapabilityDefinition();
         if(definition != null) {
            Map<String, AttributeDefinition> definitions = commonAttributes();
            definitions.putAll(definition.getAttributes());
            script.setProperty("attributes", new NamespacedAttributeContext(definitions, definition.getNamespace()));
         }
      }
      script.setProperty("now", new NowClosure(script));
      script.setProperty("vars", new VariablesContext());
      script.setProperty("sendToDevice", new SendProtocolMessageClosure(script));
      script.setProperty("forwardToDevice", new ForwardProtocolMessageClosure(script));
      script.setProperty("sendToPlatform", new SendPlatformMessageClosure(script));
      script.setProperty("sendResponse", new SendResponseClosure(script));
      script.setProperty("emit", new EmitEventClosure(script));
      script.setProperty("connected", new ConnectedClosure(script));
      script.setProperty("disconnected", new DisconnectedClosure(script));
      script.setProperty("misconfigured", new MisconfiguredClosure(script));
      script.setProperty("mispaired", new MispairedClosure(script));
      script.setProperty("provisioning", new ProvisioningClosure(script));
      script.setProperty("provisioned", new ProvisionedClosure(script));
      script.setProperty("lastProtocolMessageTimestamp", new LastProtocolMessageTimestampClosure(script));
   }

   private static final Set<String> COMMON_CAPS = ImmutableSet.of("base", "dev", "devadv", "devconn");

   private Map<String,AttributeDefinition> commonAttributes() {
      Map<String, AttributeDefinition> definitions = new HashMap<>();
      for(String namespace : COMMON_CAPS) {
         CapabilityDefinition cap = registry.getCapabilityDefinitionByNamespace(namespace);
         if(cap != null) {
            definitions.putAll(cap.getAttributes());
         }
      }
      return definitions;
   }

   private void beforeCompile(EnvironmentBinding binding) {
      for(GroovyDriverPlugin plugin: plugins) {
         plugin.enhanceEnvironment(binding);
      }
   }

   private void beforeConstruction(EnvironmentBinding binding) {
      for(GroovyDriverPlugin plugin: plugins) {
         plugin.postProcessEnvironment(binding);
      }
   }

   private void afterBuilt(DriverBinding binding, DeviceDriver driver) {
      for(GroovyDriverPlugin plugin: plugins) {
         plugin.enhanceDriver(binding, driver);
      }
      binding.freeze();
   }

   private void afterBuilt(CapabilityEnvironmentBinding binding, Capability capability) {
      for(GroovyDriverPlugin plugin: plugins) {
         plugin.enhanceCapability(binding, capability);
      }
      binding.freeze();
   }
   
   public List<String> getDefaultPopulations() {
   	List<String> popList = defaultPopulations.get();
		if(popList != null) {
         return popList;
      }

      // only do one re-build at a time
      synchronized(defaultPopulations) {
      	popList = defaultPopulations.get();
         if(popList == null) {
         	String[] populationArray = defaultPopulationsStr.split(",");
            defaultPopulations.set(Arrays.asList(populationArray));
         }
      }
      return defaultPopulations.get();
   }   

   public DeviceDriverDefinition parse(String driver) throws ValidationException {
      try (AutoCloseable pop = GroovyValidator.push()) {
         DeviceDriverDefinition def = null;
         try {
            DriverBinding bindings = loadBindings(driver);
            def = createDefinitionFromBindings(bindings);
         } catch (Exception ex) {
            GroovyValidator.error("Unable to load driver " + driver + ": " + ex.getMessage(), ex);
         }
         
         GroovyValidator.throwIfErrors();
         return def;
      } catch (ValidationException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new ValidationException("could not close validator", ex);
      }
   }

   public DeviceDriver load(String driver) throws ValidationException {
      try (AutoCloseable pop = GroovyValidator.push()) {
         DeviceDriver deviceDriver = null;
         try {
            DriverBinding bindings = loadBindings(driver);
            deviceDriver = createDriverFromBindings(bindings);
         } catch (Exception ex) {
            GroovyValidator.error("Unable to load driver " + driver + ": " + ex.getMessage(), ex);
         }

         GroovyValidator.throwIfErrors();
         return deviceDriver;
      } catch (ValidationException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new ValidationException("could not close validator", ex);
      }
   }

   protected DeviceDriver createDriverFromBindings(DriverBinding bindings) throws ValidationException {
      beforeConstruction(bindings);

      DeviceDriverDefinition definition = bindings.getBuilder().createDefinition();
      Predicate<AttributeMap> matcher = bindings.getBuilder().doCreateMatchers();
      AttributeMap baseAttributes = bindings.getBuilder().getAttributes();
      DeviceDriverBuilder builder =
            Drivers
               .builder()
               .withName(definition.getName())
               .withVersion(definition.getVersion())
               .withDescription(definition.getDescription())
               .withCommit(definition.getCommit())
               .withHash(definition.getHash())
               .addCapabilityDefinitions(definition.getCapabilities())
               .withOfflineTimeout(definition.getOfflineTimeout())
               .withAttributeValues(baseAttributes)
               .withMatcher(matcher)
               .withAttributeBindingHandler(bindings.getBuilder().createAttributeBindingHandler())
               .addPlatformMessageHandler(bindings.getBuilder().createPlatformMessageHandler(definition))
               .addProtocolMessageHandler(bindings.getBuilder().createProtocolMessageHandler())
               .addDriverEventHandler(bindings.getBuilder().createDriverEventHandler())
               ;

      for(Capability capability: bindings.getBuilder().getCapabilityImplementations()) {
         builder.addCapability(capability);
      }

      DeviceDriver driver = builder.create(definition, reflexesRunLifecycleActions);
      afterBuilt(bindings, driver);
      return driver;
   }

   protected DeviceDriverDefinition createDefinitionFromBindings(DriverBinding bindings) throws ValidationException {
      return bindings.getBuilder().createDefinition();
   }

   public static final class ScriptAndValidator {
      final Script script;
      final Validator validator;

      public ScriptAndValidator(Script script, Validator validator) {
         this.script = script;
         this.validator = validator;
      }
   }
}

