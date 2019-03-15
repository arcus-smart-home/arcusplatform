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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.capability.key.NamedKey;
import com.iris.capability.key.NamespacedKey;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.DriverConstants;
import com.iris.driver.capability.Capability;
import com.iris.driver.config.DriverConfigurationStateMachine;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.context.RequestHandlerDefinition;
import com.iris.driver.groovy.context.SetAttributesHandlerDefinition;
import com.iris.driver.groovy.handler.RequestEventHandler;
import com.iris.driver.groovy.handler.SetAttributesDefinitionConsumer;
import com.iris.driver.handler.AttributeBindingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.handler.ContextualEventHandlers;
import com.iris.driver.handler.GetAttributesHandler;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.driver.handler.GetReflexesHandler;
import com.iris.driver.handler.MessageBodyHandler;
import com.iris.driver.handler.SetAttributesConsumer;
import com.iris.driver.handler.SetAttributesHandler;
import com.iris.driver.metadata.EventMatcher;
import com.iris.driver.metadata.PlatformEventMatcher;
import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexActionBuiltin;
import com.iris.driver.reflex.ReflexDefinition;
import com.iris.driver.reflex.ReflexRunMode;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.CapabilityId;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.protocol.ProtocolMessage;
import com.iris.util.IrisCollections;
import com.iris.validators.ValidationException;

import groovy.lang.Script;

/**
 *
 */
// TODO remove this class and just use DriverBuilder, all the Groovy-ish stuff should be handled in the bindings
public class GroovyDriverBuilder extends GroovyBuilder {
   private static final Logger LOGGER = LoggerFactory.getLogger(GroovyDriverBuilder.class);

   private GroovyDriverFactory driverFactory;

   private Map<AttributeKey<?>, String> requiredAttributes;
   private Map<String, AttributeDefinition> matchAttributes = new HashMap<>();

   private String name;
   private List<String> populations = null;
   private List<Predicate<AttributeMap>> attributeMatchers = new ArrayList<Predicate<AttributeMap>>();
   private Map<String, Set<CapabilityDefinition>> instances = new LinkedHashMap<>();
   // linked has map because we iterate these all the time
   private Map<String, CapabilityDefinition> capabilityDefinitions = new LinkedHashMap<>();
   private Map<CapabilityId, Capability> capabilityImplementations = new LinkedHashMap<>();
   private ReflexRunMode reflexRunMode = null;
   private final List<ReflexDefinition> reflexDefinitions = new ArrayList<>();
   private final List<AttributeBindingHandler> attributeBindings = new ArrayList<>();
   private final List<SetAttributesHandlerDefinition> setAttrsDefinitions = new ArrayList<>();
   private final List<RequestHandlerDefinition> requestDefinitions = new ArrayList<>();
   private DriverConfigurationStateMachine configurationStateMachine;
   private long offlineTimeout = Long.MAX_VALUE;
   private List<String> defaultPopulations = ImmutableList.<String>of(Population.NAME_GENERAL);

   GroovyDriverBuilder(CapabilityRegistry registry, GroovyDriverFactory driverFactory, Map<AttributeKey<?>, String> requiredAttributes) {
      super(registry);
      this.driverFactory = driverFactory;
      this.requiredAttributes = requiredAttributes;
      //TODO - RemoveMe.  driverFactory is null in some existing unit tests.  We should really fix the tests, but it is time consuming
      if(driverFactory != null) {
      	this.defaultPopulations = driverFactory.getDefaultPopulations();
      }
   }

   public GroovyDriverBuilder addMatchAttribute(String name, AttributeDefinition definition) {
      AttributeDefinition old = matchAttributes.get(name);
      if(old != null && old.equals(definition)) {
         throw new IllegalArgumentException("Attempted to overwrite match attribute [" + name + "] from [" + matchAttributes.get(name) + "] to ["  + definition + "]");
      }
      matchAttributes.put(name, definition);
      return this;
   }

   public GroovyDriverBuilder addMatchAttributes(Map<String, AttributeDefinition> definitions) {
      for(Map.Entry<String, AttributeDefinition> e: definitions.entrySet()) {
         addMatchAttribute(e.getKey(), e.getValue());
      }
      return this;
   }

   public void assertValid() throws ValidationException {
      // check for missing stuff
      if(requiredAttributes != null) {
         for(Map.Entry<AttributeKey<?>, String> e: requiredAttributes.entrySet()) {
            String fieldName = e.getValue();
            Object value = getAttributes().get(e.getKey());
            if(value == null || "".equals(value)) {
               GroovyValidator.error("Required header " + fieldName + " wasn't specified, please add: " + fieldName + " <value>");
            }
         }
      }
   }

   public DeviceDriverDefinition createDefinition() throws ValidationException {
      assertValid();
      return doCreateDefinition();
   }

   //-------- Simple Properties ---------------

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
      this.addAttributeValue(DriverConstants.DEVADV_ATTR_DRIVERNAME, name);
   }

   public List<String> getPopulations() {
   	if(populations == null) {
   		return defaultPopulations;
   	}else{
   		return populations;
   	}
   }

   public void setPopulations(List<String> pops) {
      this.populations = pops;
   }

   @Override
   public void setVersion(Version version) {
      super.setVersion(version);
      this.addAttributeValue(DriverConstants.DEVADV_ATTR_DRIVERVERSION, version.getRepresentation());
   }

   @Override
   public GroovyDriverBuilder withVersion(Object version) {
      super.withVersion(version);
      return this;
   }
   
   public GroovyDriverBuilder addMatcher(String name, Object value) {
      Predicate<AttributeMap> p = toMatcher(name, value);
      if(p != null) {
         attributeMatchers.add(p);
      }
      return this;
   }

   public GroovyDriverBuilder addMatchers(Map<String, Object> values) {
      if(values != null && !values.isEmpty()) {
         List<Predicate<AttributeMap>> predicators = new LinkedList<Predicate<AttributeMap>>();
         for(Map.Entry<String, Object> e: values.entrySet()) {
            Predicate<AttributeMap> predicated = toMatcher(e.getKey(), e.getValue());
            if(predicated != null) {
               predicators.add(predicated);
            }
         }
         if(predicators.size() == 1) {
            attributeMatchers.add(predicators.get(0));
         }
         else if(predicators.size() > 1) {
            attributeMatchers.add(Predicates.and(predicators));
         }
      }
      return this;
   }
   
   private Predicate<AttributeMap> toMatcher(String name, Object value) {
      AttributeDefinition definition = matchAttributes.get(name);
      if(definition == null) {
         definition = getAttributeDefinition(name);
      }
      if(definition == null) {
         GroovyValidator.error("Unsupported matcher property [" + name + "]");
         return null;
      }
      return DriverMatchers.toMatcher(definition, value);
   }
   
   public Map<String, AttributeDefinition> getSupportedAttributes() {
      Map<String, AttributeDefinition> attributeDefinitions = new HashMap<>();
      // singleton attributes
      for(CapabilityDefinition definition: capabilityDefinitions.values()) {
         attributeDefinitions.putAll(definition.getAttributes());
      }
      // multi-instance attributes
      for(Map.Entry<String, Set<CapabilityDefinition>> instance: instances.entrySet()) {
         String instanceId = instance.getKey();
         for(CapabilityDefinition capability: instance.getValue()) {
            for(AttributeDefinition attribute: capability.getAttributes().values()) {
               String attributeName = attribute.getKey().instance(instanceId).getName();
               attributeDefinitions.put(attributeName, attribute);
            }
         }
      }
      return attributeDefinitions;
   }

   //-------- Capability Definitions ---------------

   public Set<String> getSupportedCapabilityNames() {
      return capabilityDefinitions.keySet();
   }

   public List<Capability>  getCapabilityImplementations() {
      return new ArrayList<>(capabilityImplementations.values());
   }

   public Set<Capability> getCapabilityImplementation(String capabilityName) {
	   Set<Capability> retVal = new HashSet<Capability>();
	   if(capabilityImplementations != null && !capabilityImplementations.isEmpty()) {
		   for(CapabilityId curId: capabilityImplementations.keySet()) {
			   if(curId.getCapabilityName().equals(capabilityName)) {
				   retVal.add(capabilityImplementations.get(curId));
			   }
		   }
	   }	   
      return retVal;
   }

   public GroovyDriverBuilder addDefaultCapabilities() {
      importCapability(GroovyDrivers.PLATFORM_BASE_CAPABILITY);
      importCapability(GroovyDrivers.PLATFORM_DEVICE_CAPABILITY);
      importCapability(GroovyDrivers.PLATFORM_DEVICE_ADVANCED_CAPABILITY);
      importCapability(GroovyDrivers.PLATFORM_DEVICE_CONNECTION_CAPABILITY);
      return this;
   }
   
   public Set<String> getInstanceIds() {
      return instances.keySet();
   }
   
   public Set<CapabilityDefinition> getInstanceCapabilities(String instanceId) {
      return instances.get(instanceId);
   }
   
   public GroovyDriverBuilder addInstance(String instanceId, Set<CapabilityDefinition> definitions) {
      if(instances.containsKey(instanceId)) {
         GroovyValidator.error("Re-defined instance [" + instanceId + "]");
         return this;
      }
      instances.put(instanceId, definitions);
      return this;
   }

   public GroovyDriverBuilder addCapability(Object capability) {
      if(capability == null) {
         return this;
      }

      if(capability instanceof String) {
         return addCapabilityDefinitionByName((String) capability);
      }

      if(capability instanceof GroovyCapabilityDefinition) {
         return addCapabilityDefinition(((GroovyCapabilityDefinition) capability).getDefinition());
      }

      if(capability instanceof CapabilityDefinition) {
         return addCapabilityDefinition((CapabilityDefinition) capability);
      }

      GroovyValidator.error("Unsupported capability type [" + capability.getClass() + "], must be a capability name or capability object");
      return this;
   }

   public GroovyDriverBuilder addCapabilityDefinitionByName(String name) {
      CapabilityDefinition capabilityDefinition = getCapabilityDefinition(name);
      if(capabilityDefinition == null) {
         GroovyValidator.error("No capability named [" + name + "] was found in the system.");
         return this;
      }

      return addCapabilityDefinition(capabilityDefinition);
   }

   public GroovyDriverBuilder addCapabilityDefinition(CapabilityDefinition definition) {
      if(definition == null) {
         GroovyValidator.error("Unable to resolve the capability definition");
         return this;
      }
      capabilityDefinitions.put(definition.getCapabilityName(), definition);
      return this;
   }

   public GroovyDriverBuilder addAttributeBinding(AttributeKey<?> source, AttributeKey<? super Date> bound) {
      this.attributeBindings.add(new AttributeBindingHandler.SingleBindingsHandler(source,bound));
      return this;
   }

   public GroovyDriverBuilder addAttributeBinding(AttributeBindingHandler handler) {
      this.attributeBindings.add(handler);
      return this;
   }

   public GroovyDriverBuilder addSetAttributeHandler(SetAttributesHandlerDefinition handler) {
      this.setAttrsDefinitions.add(handler);
      return this;
   }

   public GroovyDriverBuilder addRequestHandler(RequestHandlerDefinition handler) {
      this.requestDefinitions.add(handler);
      return this;
   }

   public GroovyDriverBuilder addReflexDefinition(ReflexDefinition definition) {
      if(definition == null) {
         GroovyValidator.error("cannot add a null reflex definition");
         return this;
      }
      for(ReflexAction action: definition.getActions()) {
         if(action instanceof ReflexActionBuiltin) {
            GroovyValidator.assertTrue(getReflexRunMode() == null || getReflexRunMode() == ReflexRunMode.HUB, "Driver builtin requires a reflexMode of HUB_REQUIRED, but " + getReflexRunMode() + " was specified");
            withReflexRunMode(ReflexRunMode.HUB);
         }
      }

      reflexDefinitions.add(definition);
      return this;
   }

   public GroovyDriverBuilder addConfigurationStateMachine(DriverConfigurationStateMachine configurationStateMachine) {
      if (this.configurationStateMachine != null) {
         // TODO: lift this constraint by combining multiple state machines into a single one.
         GroovyValidator.error("drivers currently cannot have more than one configuration state machine");
         return this;
      }

      this.configurationStateMachine = configurationStateMachine;
      return this;
   }

   @Nullable
   public ReflexRunMode getReflexRunMode() {
      return reflexRunMode;
   }
   
   public GroovyDriverBuilder withReflexRunMode(ReflexRunMode mode) {
      this.reflexRunMode = mode;
      return this;
   }

   public GroovyDriverBuilder withOfflineTimeout(long offlineTimeout) {
      this.offlineTimeout = offlineTimeout;
      return this;
   }

   //-------- Attribute Values (definitions are part of capabilities) ---------------

   @Override
   public AttributeMap getAttributes() {
      syncCapabilities();
      AttributeMap attributes = AttributeMap.newMap();
      for(Capability capability: capabilityImplementations.values()) {
         attributes.addAll(capability.getAttributes());
      }
      attributes.addAll(super.getAttributes());
      return attributes;
   }

   @Override
   public GroovyDriverBuilder addAttributeValue(String name, Object value) {
      super.addAttributeValue(name, value);
      return this;
   }

   @Override
   public GroovyDriverBuilder addAttributeValues(Map<String, Object> values) {
      super.addAttributeValues(values);
      return this;
   }

   public Script importCapability(String capabilityName) {
      try {
         GroovyDriverFactory.ScriptAndValidator sav = driverFactory.loadCapabilityScript(capabilityName);
         CapabilityEnvironmentBinding bindings = (CapabilityEnvironmentBinding)sav.script.getBinding();

         if(sav.validator.hasErrors()) {
            GroovyValidator.addErrors(sav.validator);
            return null;
         }

         Capability capability = driverFactory.createCapabilityFromBindings(bindings);

         // TODO don't allow capabilities to be redefined?
         //capability.getCapabilityId().getRepresentation()
         addCapabilityDefinition(capability.getCapabilityDefinition());
         capabilityImplementations.put(capability.getCapabilityId(), capability);

         GroovyValidator.addDriverClassname(sav.script.getClass().getName());
         return sav.script;
      }
      catch(Exception e) {
         LOGGER.warn("Unable to import capability {}:", capabilityName, e);
         GroovyValidator.error("Unable to import capability " + capabilityName + ": " + e.getMessage());
      }

      return null;
   }

   protected DeviceDriverDefinition doCreateDefinition() {
      DeviceDriverDefinition.Builder builder =
            DeviceDriverDefinition
               .builder()
               .withName(getName())
               .withDescription(getDescription())
               .withVersion(getVersion())
               .withCommit(getCommit())
               .withHash(getHash())
               .withOfflineTimeout(offlineTimeout)
               .withPopulations(getPopulations())
               .withReflexRunMode(reflexRunMode == null ? ReflexRunMode.defaultMode() : reflexRunMode)
               ;
      for(CapabilityDefinition definition: capabilityDefinitions.values()) {
         builder.addCapability(definition);
      }

      for(ReflexDefinition definition: reflexDefinitions) {
         builder.addReflex(definition);
      }

      if (configurationStateMachine != null) {
         builder.addConfigurationStateMachine(configurationStateMachine);
      }

      return builder.create();
   }

   protected Predicate<AttributeMap> doCreateMatchers() {
      if(attributeMatchers.isEmpty()) {
         GroovyValidator.error("No valid match conditions were found, this driver won't match any devices");
         return Predicates.alwaysFalse();
      }
      return Predicates.or(attributeMatchers);
   }

   protected AttributeBindingHandler createAttributeBindingHandler () {

      switch (attributeBindings.size()) {
      case 0:
         return AttributeBindingHandler.NoBindingsHandler.INSTANCE;
      case 1:
         return attributeBindings.get(0);
      default:
         return new AttributeBindingHandler.MultipleBindingsHandler(ImmutableList.copyOf(attributeBindings));
      }
   }

   @Override
   protected ContextualEventHandler<DriverEvent> createDriverEventHandler() {
      List<ContextualEventHandler<DriverEvent>> listeners = new ArrayList<>();
      if (configurationStateMachine != null) {
         IrisCollections.addIfNotNull(listeners, configurationStateMachine.createEventHandler());
      }

      IrisCollections.addIfNotNull(listeners, super.createDriverEventHandler());
      for(Capability capability: capabilityImplementations.values()) {
         IrisCollections.addIfNotNull(listeners, capability.getDriverEventHandler());
      }
      return ContextualEventHandlers.marshalSender(listeners);
   }

   @Override
   protected ContextualEventHandler<PlatformMessage> createPlatformMessageHandler() {
      throw new UnsupportedOperationException();
   }

   protected ContextualEventHandler<PlatformMessage> createPlatformMessageHandler(DeviceDriverDefinition def) {
      List<ContextualEventHandler<PlatformMessage>> handlers = new ArrayList<>();
      try {
         MessageBodyHandler.Builder builder = MessageBodyHandler.builder();
         builder.addHandler(
               NamespacedKey.parse(com.iris.messages.capability.Capability.CMD_GET_ATTRIBUTES),
               createGetAttributesHandler());
         builder.addHandler(
               NamespacedKey.parse(com.iris.messages.capability.Capability.CMD_SET_ATTRIBUTES),
               createSetAttributesHandler());
         builder.addHandler(
               NamespacedKey.parse(com.iris.messages.capability.DeviceAdvancedCapability.GetReflexesRequest.NAME),
               createGetReflexesHandler(def));

         Map<NamedKey,List<RequestHandlerDefinition>> reqHandlers = new LinkedHashMap<>();
         for (RequestHandlerDefinition rhdef : requestDefinitions) {
            for (RequestHandlerDefinition.MatchRequest mreq : rhdef.getMatches()) {
               List<RequestHandlerDefinition> defs = reqHandlers.get(mreq.getCommandKey());
               if (defs == null) {
                  defs = new ArrayList<>();
                  reqHandlers.put(mreq.getCommandKey(), defs);
               }

               if (!defs.contains(rhdef)) {
                  defs.add(rhdef);
               }
            }
         }

         for (Map.Entry<NamedKey,List<RequestHandlerDefinition>> entry : reqHandlers.entrySet()) {
            builder.addHandler(
               entry.getKey(),
               createRequestHandler(entry.getValue())
            );
         }

         for(EventMatcher matcher: getEventMatchers()) {
            if(PlatformEventMatcher.class.equals(matcher.getClass())) {
               PlatformEventMatcher pMatcher = (PlatformEventMatcher) matcher;
               if(pMatcher.matchesAnyCapability()) {
                  builder.addWildcardHandler(pMatcher.getHandler());
               }
               else {
                  builder.addHandler(pMatcher.getMethodName(), pMatcher.getHandler());
               }
            }
         }
         if(builder.hasAnyHandlers()) {
            handlers.add(builder.build());
         }
      }
      catch(Exception e) {
         //GroovyValidator.error("Unable to create platform handlers: " + e.getMessage());
         LOGGER.debug("Unable to create platform handlers", e);
         return null;
      }
      for(Capability capability: capabilityImplementations.values()) {
         IrisCollections.addIfNotNull(handlers, capability.getPlatformMessageHandler());
      }
      return ContextualEventHandlers.marshalDispatcher(handlers);
   }

   @Override
   protected ContextualEventHandler<ProtocolMessage> createProtocolMessageHandler() {
      List<ContextualEventHandler<ProtocolMessage>> handlers = new ArrayList<>();
      if (configurationStateMachine != null) {
         IrisCollections.addIfNotNull(handlers, configurationStateMachine.createProtocolMessageHandler());
      }

      IrisCollections.addIfNotNull(handlers, super.createProtocolMessageHandler());
      for(Capability capability: capabilityImplementations.values()) {
         IrisCollections.addIfNotNull(handlers, capability.getProtocolMessageHandler());
      }
      return ContextualEventHandlers.marshalSender(handlers);
   }

   protected ContextualEventHandler<MessageBody> createGetAttributesHandler() {
      List<GetAttributesProvider> providers = new ArrayList<>(this.getAttributeProviders());
      for(Capability capability: capabilityImplementations.values()) {
         providers.addAll(capability.getAttributeProviders());
      }
      Map<String, CapabilityDefinition> definitionByNamespace = new HashMap<>(capabilityDefinitions.size() + 1);
      for(CapabilityDefinition definition: capabilityDefinitions.values()) {
         definitionByNamespace.put(definition.getNamespace(), definition);
      }
      for(GetAttributesProvider provider: providers) {
         String namespace = provider.getNamespace();
         if(!definitionByNamespace.keySet().contains(namespace)) {
            GroovyValidator.error("Invalid getAttributes(" + namespace + "), this driver does not support capability " + namespace);
         }
      }
      return new GetAttributesHandler(definitionByNamespace, providers);
   }

   protected ContextualEventHandler<MessageBody> createGetReflexesHandler(DeviceDriverDefinition def) {
      return new GetReflexesHandler(def);
   }

   protected ContextualEventHandler<MessageBody> createRequestHandler(List<RequestHandlerDefinition> handlers) {
      return new RequestEventHandler(handlers);
   }

   protected ContextualEventHandler<MessageBody> createSetAttributesHandler() {
      List<SetAttributesConsumer> consumers = new ArrayList<>();
      if (!setAttrsDefinitions.isEmpty()) {
         consumers.add(new SetAttributesDefinitionConsumer(setAttrsDefinitions));
      }

      consumers.addAll(this.getAttributeConsumers());

      for(Capability capability: capabilityImplementations.values()) {
         consumers.addAll(capability.getAttributeConsumers());
      }
            
      Map<String, AttributeDefinition> attributeDefinitions = getSupportedAttributes();
      Set<String> supportedNamespaces = new HashSet<>(capabilityDefinitions.size() + 1);
      for(CapabilityDefinition definition: capabilityDefinitions.values()) {
         supportedNamespaces.add(definition.getNamespace());
      }
      
      Set<String> unsupportedNamespaces = new HashSet<>(supportedNamespaces);
      for(SetAttributesConsumer consumer: consumers) {
         String namespace = consumer.getNamespace();
         if(namespace == null) {
            // handles everything
            unsupportedNamespaces.clear();
         }
         // TODO catch this when it happens...
         else if(!supportedNamespaces.contains(namespace)) {
            //GroovyValidator.error("Invalid setAttributes, this driver does not support capability " + namespace + ": supported capabilities=" + supportedNamespaces, consumer.getDeclarationSite());
         }
         else {
            unsupportedNamespaces.remove(namespace);
         }
      }
      // TODO: Should this still work?
      //if(!unsupportedNamespaces.isEmpty()) {
         //GroovyValidator.error("Missing setAttributes for namespace(s): " + unsupportedNamespaces);
      //}
      return new SetAttributesHandler(attributeDefinitions, consumers);
   }

   private void syncCapabilities() {
      Set<String> caps = getNamespaces(this.capabilityDefinitions.values());
      addAttributeValue(DriverConstants.BASE_ATTR_CAPS, caps);
      if(this.instances.isEmpty()) {
         addAttributeValue(DriverConstants.BASE_ATTR_INSTANCES, ImmutableMap.of());
      }
      else {
         Map<String, Set<String>> inst = new HashMap<>(this.instances.size());
         for(Map.Entry<String, Set<CapabilityDefinition>> entry: this.instances.entrySet()) {
            inst.put(entry.getKey(), getNamespaces(entry.getValue()));
         }
         addAttributeValue(DriverConstants.BASE_ATTR_INSTANCES, inst);
      }
   }

   private Set<String> getNamespaces(Collection<CapabilityDefinition> values) {
      Set<String> namespaces = new HashSet<String>(Math.max(1, values.size()));
      for(CapabilityDefinition definition: values) {
         if(definition == null) {
            continue;
         }
         namespaces.add(definition.getNamespace());
      }
      return namespaces;
   }
}

