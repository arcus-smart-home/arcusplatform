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
package com.iris.driver.groovy;

import com.iris.driver.event.ScheduledDriverEvent;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;

import java.io.Closeable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.runtime.InvokerHelper;

import com.google.common.collect.ImmutableList;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DriverConstants;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.context.OnCapabilityClosure;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.util.IrisCollections;

// TODO use a different binding post-compile to drop builder references
public class DriverBinding extends EnvironmentBinding {
   private static final Map<AttributeKey<?>, String> REQUIRED_FIELDS =
         IrisCollections
            .<AttributeKey<?>, String>immutableMap()
               .put(DriverConstants.DEVADV_ATTR_DRIVERNAME, "driver")
               .put(DriverConstants.DEVADV_ATTR_DRIVERVERSION, "version")
               .put(DriverConstants.DEV_ATTR_VENDOR, "vendor")
               .put(DriverConstants.DEV_ATTR_MODEL, "model")
               .put(DriverConstants.DEV_ATTR_DEVTYPEHINT, "deviceTypeHint")
               .create();
   private static final Map<String, AttributeDefinition> MATCH_ATTRIBUTES =
         IrisCollections
            .<String, AttributeDefinition>immutableMap()
            .put("vendor", DriverConstants.DEV_ATTR_DEF_VENDOR)
            .put("model", DriverConstants.DEV_ATTR_DEF_MODEL)
            .put("protocol", DriverConstants.DEVADV_ATTR_DEF_PROTOCOL)
            .put("subprotocol", DriverConstants.DEVADV_ATTR_DEF_SUBPROTOCOL)
            .put("protocolid", DriverConstants.DEVADV_ATTR_DEF_PROTOCOLID)
            .create();

   private CapabilityRegistry capabilityRegistry;

   public DriverBinding(CapabilityRegistry registry, GroovyDriverFactory driverFactory) {
      super(
         new GroovyDriverBuilder(registry, driverFactory, REQUIRED_FIELDS)
            .addDefaultCapabilities()
            .addMatchAttributes(MATCH_ATTRIBUTES)
      );
      this.capabilityRegistry = registry;
      init();
   }

   // TODO maybe this would make more sense on the MetaClass
   private void init() {
      // by default our getMetaClass will be generate from this class, but we
      // don't want to expose member variable, so drop down to Binding
      setMetaClass(InvokerHelper.getMetaClass(Binding.class));
      // TODO move this to DriverBindingMetaClass
      setProperty("driver", new SetName());
      setProperty("description", new SetDescription());
      setProperty("version", new SetVersion());
      setProperty("commit", new SetCommit());
      setProperty("populations", new SetPopulations());

      setProperty("deviceTypeHint", new SetAttribute(DriverConstants.DEV_ATTR_DEF_DEVTYPEHINT));
      setProperty("productId", new SetAttribute(DriverConstants.DEV_ATTR_DEF_PRODUCTID));
      setProperty("vendor", new SetAttribute(DriverConstants.DEV_ATTR_DEF_VENDOR));
      setProperty("model", new SetAttribute(DriverConstants.DEV_ATTR_DEF_MODEL));
      setProperty("protocol", new SetAttribute(DriverConstants.DEVADV_ATTR_DEF_PROTOCOL));
      setProperty("subprotocol", new SetAttribute(DriverConstants.DEVADV_ATTR_DEF_SUBPROTOCOL));

      setProperty("matcher", new SetMatcher());
      setProperty("capabilities", new SetCapabilities());
      setProperty("configure", new ConfigureAttributes());
      setProperty("instance", new AddInstanceMethod());

      ImportCapability importer = new ImportCapability();
      setProperty("importCapability", importer);
      setProperty("uses", importer);

      // most event handlers are added as meta function on DriverScriptMetaClass
      // these are added here because they are closures with attributes, not just
      // methods, and full closures have to be assigned for each object
      for(CapabilityDefinition definition: capabilityRegistry.listCapabilityDefinitions()) {
         setProperty("on" + definition.getCapabilityName(), new OnCapabilityClosure(definition, this));
      }
   }

   public CapabilityDefinition getCapabilityDefinition(String capabilityName) {
      return capabilityRegistry.getCapabilityDefinitionByName(capabilityName);
   }

   @Override
   public GroovyDriverBuilder getBuilder() {
      return (GroovyDriverBuilder) super.getBuilder();
   }

   public void addDriverMatchers(Map<String, Object> matchers) {
      getBuilder().addMatchers(matchers);
   }

   public static ContextualEventHandler<Object> wrapAsHandler(Object [] arguments) {
      if(arguments.length == 0) {
         throw new IllegalArgumentException("No handler function specified, try adding a { } block.");
      }
      Object value = arguments[arguments.length - 1];
      if(value instanceof Closure) {
         ContextualEventHandler<Object> handler = wrapAsHandler((Closure<?>) value);
         //GroovyTypeChecking.invokeLifecycleClosure((Closure<?>)value);
         return handler;
      }
      else {
         throw new IllegalArgumentException("No handler function specified, try adding a { } block.");
      }
   }

   public static ContextualEventHandler<Object> wrapAsHandler(final Closure<?> closure) {
      return new ContextualEventHandler<Object>() {
         @Override
         public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
            return doHandleEvent(closure, context, event);
         }
      };
   }

   public static ContextualEventHandler<Object> wrapAsSchedulerHandler(final Closure<?> closure) {
      return new ContextualEventHandler<Object>() {
         @Override
         public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
            context.setActor(((ScheduledDriverEvent)event).getActor());
            return doHandleEvent(closure, context, event);
         }
      };
   }

   private static boolean doHandleEvent(Closure<?> closure, DeviceDriverContext context, Object event) throws Exception {
      GroovyContextObject.setContext(context);

      Closeable c = EnvironmentBinding.setRuntimeVar("message", event);
      try {
         Object o = closure.call();
         return !Boolean.FALSE.equals(o);
      }
      finally {
         c.close();
         GroovyContextObject.clearContext();
      }
   }

   class DriverBindingClosure extends Closure<Object> {

      protected DriverBindingClosure() {
         super(DriverBinding.this);
      }

   }

   class SetAttribute extends DriverBindingClosure {
      private final AttributeKey<?> key;

      SetAttribute(AttributeDefinition definition) {
         this(definition.getKey());
      }

      SetAttribute(AttributeKey<?> key) {
         this.key = key;
      }

      public void doCall(Object value) {
         getBuilder().addAttributeValue(key, value);
      }
   }

   class SetName extends DriverBindingClosure {
      public void doCall(String name) {
         getBuilder().setName(name);
         DriverBinding.this.setProperty("NAME", name);
         DriverBinding.this.setProperty("DEVICE_NAME", name);
      }
   }

   class SetDescription extends DriverBindingClosure {
      public void doCall(String description) {
         getBuilder().setDescription(description);
      }
   }

   class SetVersion extends DriverBindingClosure {
      public void doCall(Object version) {
         getBuilder().withVersion(version);
      }
   }

   class SetCommit extends DriverBindingClosure {
      public void doCall(String commit) {
         getBuilder().withCommit(commit);
      }
   }

   class SetPopulations extends DriverBindingClosure {
      public void doCall(String pop) {
         getBuilder().setPopulations(ImmutableList.of(pop));
      }

      public void doCall(List<String> pops) {
         getBuilder().setPopulations(pops);
      }

      public void doCall(String... pops) {
         getBuilder().setPopulations(Arrays.asList(pops));
      }

      public void getAt(List<String> pops) {
         getBuilder().setPopulations(pops);
      }
   }

   class SetMatcher extends DriverBindingClosure {
      public void doCall(Map<String, Object> matchers) {
         getBuilder().addMatchers(matchers);
      }
   }

   class SetCapabilities extends DriverBindingClosure {
      public void doCall(Object [] capabilities) {
         for(Object capability: capabilities) {
            getBuilder().addCapability(capability);
         }
      }
   }

   class ImportCapability extends DriverBindingClosure {
      public void doCall(String capability) {
         Script script = getBuilder().importCapability(capability);
         if (script != null) {
            Class<?> clazz = script.getMetaClass().getTheClass();
            setProperty(clazz.getSimpleName(), script);
         }
      }
   }

   class ConfigureAttributes extends DriverBindingClosure {
      public void doCall(Map<String, Object> attributes) {
         getBuilder().addAttributeValues(attributes);
      }
   }

   class AddInstanceMethod extends DriverBindingClosure {

      public void doCall(Object[] arguments) {
         GroovyDriverBuilder builder = getBuilder();

         String instanceId = Arguments.extractOptionalString(0, arguments);
         if(StringUtils.isEmpty(instanceId)) {
            GroovyValidator.error("First argument to instance must be the instance id");
            return;
         }

         if(arguments.length < 2) {
            GroovyValidator.error("Must specify at least one capability for instance [" + instanceId + "] to implement");
            return;
         }

         Set<CapabilityDefinition> definitions = new HashSet<>(arguments.length - 1);
         for(int i=1; i<arguments.length; i++) {
            CapabilityDefinition definition;
            Object o = arguments[i];
            if(o instanceof String) {
               definition = builder.getCapabilityDefinition((String) o);
            }
            else if(o instanceof CapabilityDefinition) {
               definition = (CapabilityDefinition) o;
            }
            else if(o instanceof GroovyCapabilityDefinition) {
               definition = ((GroovyCapabilityDefinition) o).getDefinition();
            }
            else {
               GroovyValidator.error("Invalid type [" + o.getClass() + "] expected string or capability");
               return;
            }
            if(definition == null) {
               GroovyValidator.error("Unrecognized capability: " + o);
               return;
            }
            definitions.add(definition);
         }
         builder.addInstance(instanceId, definitions);
      }

   }

}

