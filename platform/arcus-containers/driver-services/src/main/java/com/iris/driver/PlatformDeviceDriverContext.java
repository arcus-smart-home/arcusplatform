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
package com.iris.driver;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.bootstrap.ServiceLocator;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.pin.PinManager;
import com.iris.driver.pin.PlatformPinManager;
import com.iris.driver.reflex.ReflexDriverContext;
import com.iris.driver.reflex.ReflexDriverDefinition;
import com.iris.driver.service.executor.DriverExecutors;
import com.iris.driver.service.executor.PlatformMessageTimeout;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.services.PlatformConstants;
import com.iris.model.Version;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;

/**
 * A context which is exposed to DeviceDrivers and piece-meal to the
 * driver DSL in order to allow attributes to be edited, the device
 * to be edited, and variables to be stored.
 *
 * Currently attributes/device changes are persisted after every
 * message is handled.
 *
 * NOT THREAD SAFE: callers must enforce thread safety!
 */
public class PlatformDeviceDriverContext implements DeviceDriverContext {
   public static final String FALLBACK_DEVICE_NAME = "New Device";

   private static final Set<AttributeKey<?>> DEVICE_KEYS =
         ImmutableSet
            .<AttributeKey<?>>builder()
            .add(Capability.KEY_ADDRESS)
            .add(Capability.KEY_ID)
            .add(Capability.KEY_TYPE)
            .add(Capability.KEY_CAPS)
            .add(Capability.KEY_TAGS)
            .add(Capability.KEY_IMAGES)
            .add(DeviceCapability.KEY_ACCOUNT)
            .add(DeviceCapability.KEY_PLACE)
            .add(DeviceCapability.KEY_DEVTYPEHINT)
            .add(DeviceCapability.KEY_MODEL)
            .add(DeviceCapability.KEY_PRODUCTID)
            .add(DeviceCapability.KEY_NAME)
            .add(DeviceCapability.KEY_VENDOR)
            .add(DeviceAdvancedCapability.KEY_ADDED)
            .add(DeviceAdvancedCapability.KEY_DRIVERNAME)
            .add(DeviceAdvancedCapability.KEY_DRIVERSTATE)
            .add(DeviceAdvancedCapability.KEY_DRIVERVERSION)
            .add(DeviceAdvancedCapability.KEY_PROTOCOL)
            .add(DeviceAdvancedCapability.KEY_PROTOCOLID)
            .add(DeviceAdvancedCapability.KEY_HUBLOCAL)
            .add(DeviceAdvancedCapability.KEY_DEGRADED)
            .add(DeviceAdvancedCapability.KEY_DEGRADEDCODE)
            .build();

   private static final AttributeKey<String> ATTR_DEVICE_PRESENCE =
         AttributeKey.create(PresenceCapability.ATTR_PRESENCE, String.class);
   private static final AttributeKey<Date> ATTR_DEVICE_PRESENCE_CHANGED=
         AttributeKey.create(PresenceCapability.ATTR_PRESENCECHANGED, Date.class);
   
   // TODO make this configurable
   private long defaultTimeoutMs = 30000;
   private long actorContextTimeoutMs = 30000;

   private Device persisted;
   private Device device;
   // TODO replace with MessageContext once we have that
   private PlatformMessage currentMessage;
   private Address actor;
   private ScheduledTask timeout;
   private long lastProtocolMessageTimestamp;
   private boolean deleted = false;
   private boolean connected;

   // TODO I'm not sure we need to hold onto the driver
//   private final DeviceDriver driver;
   private final DriverId driverId;

   private final Set<AttributeKey<?>> attributeKeys;
   private final AttributeMap attributes;
   private final Set<AttributeKey<?>> supportedAttributes;
   private final Map<String,AttributeKey<?>> supportedAttributesByName;
   private final Set<AttributeKey<?>> dirtyAttributes;
   private final Map<String, Object> variables;
   private final Set<String> dirtyVariables;
   private final PinManager pinManager;
   private final PlatformDriverReflexContext reflexContext;

   private final Address platformAddress;
   private final Address protocolAddress;
   private final boolean isHubLocal;
   private String population;

   private Map<String, ActorContext> actorRequestMap;

   private final Logger logger;

   public PlatformDeviceDriverContext(Device device, DeviceDriver driver, PlacePopulationCacheManager populationCacheMgr) {
      this(device, driver.getDefinition(), new DeviceDriverStateHolder(AttributeMap.copyOf(driver.getBaseAttributes())), populationCacheMgr);
   }

   public PlatformDeviceDriverContext(
         Device device,
         DeviceDriverDefinition driverDefinition,
         DeviceDriverStateHolder state,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this(device, driverDefinition, state, true, populationCacheMgr);
   }

   public PlatformDeviceDriverContext(
         Device device,
         DeviceDriverDefinition driverDefinition,
         DeviceDriverStateHolder state,
         boolean reconcileAttributes, 
         PlacePopulationCacheManager populationCacheMgr
   ) {
      Device updated = device;
      if (reconcileAttributes) {
         Set<CapabilityDefinition> caps = driverDefinition.getCapabilities();
         Set<String> capsString = Sets.newHashSetWithExpectedSize(caps.size());
         for (CapabilityDefinition cdef : caps) {
            capsString.add(cdef.getNamespace());
         }

         if (!capsString.equals(device.getCaps())) {
            updated = device.copy();
            updated.setCaps(capsString);
         }
      }

      this.persisted = device.copy();
      this.device = updated.copy();
      if(device.getPlace() != null) {
      	population = populationCacheMgr.getPopulationByPlaceId(device.getPlace());
      }
      this.driverId = driverDefinition.getId();

      this.supportedAttributesByName = loadSupportedAttributes(driverDefinition);
      this.supportedAttributes = ImmutableSet.copyOf(supportedAttributesByName.values());

      this.dirtyAttributes = new HashSet<>();
      this.attributes = AttributeMap.newMap();
      this.attributeKeys = Sets.union(DEVICE_KEYS, attributes.keySet());
      
      for(AttributeValue<?> av: state.getAttributes().entries()) {
        
       AttributeKey<?> key = av.getKey();
       
         if(DEVICE_KEYS.contains(key)) {
            setAttributeValue(av);
         }
         else {
            this.attributes.add(av);
         }
      }
      this.dirtyAttributes.clear();

      // may not need to be synchronized
      this.variables = Collections.synchronizedMap(new HashMap<String,Object>(state.getVariables()));
      this.dirtyVariables = new HashSet<>();

      this.actorRequestMap = new LinkedHashMap<String, ActorContext>();
      this.logger = LoggerFactory.getLogger("driver." + driverDefinition.getName());

      this.pinManager = new PlatformPinManager(ServiceLocator.getInstance(PersonDAO.class), ServiceLocator.getInstance(PersonPlaceAssocDAO.class));

      String devConn = (this.attributes != null) ? this.attributes.get(DeviceConnectionCapability.KEY_STATE)
                                                 : DeviceConnectionCapability.STATE_ONLINE;

      if (!isPresenceCapableDevice()) {
        this.connected = !DeviceConnectionCapability.STATE_OFFLINE.equals(devConn);
      } else {
        this.connected = !isAbsent();
      }

      this.platformAddress = Address.fromString(this.device.getAddress());
      this.protocolAddress = Address.fromString(this.device.getProtocolAddress());
      this.clearActor();

      ReflexDriverDefinition reflexDriverDefinition = driverDefinition.getReflexes();
      this.isHubLocal = reflexDriverDefinition.getDfa() != null ||
                        (reflexDriverDefinition.getReflexes() != null && 
                         !reflexDriverDefinition.getReflexes().isEmpty());
      this.reflexContext = this.isHubLocal ? new PlatformDriverReflexContext(this) : null;
      if (reconcileAttributes && this.device.isHubLocal() != isHubLocal) {
         this.device.setHubLocal(isHubLocal);
      }
   }

   // TODO move DeviceDao / Scheduler functionality onto the DeviceDriver to make it easier
   //      to inject singletons there and just pass in the context

   protected DeviceDAO getDeviceDao() {
      return ServiceLocator.getInstance(DeviceDAO.class);
   }

   protected Scheduler getScheduler() {
      // TODO named instance?
      return ServiceLocator.getInstance(Scheduler.class);
   }

   @Override
   public UUID getPlaceId() {
      return this.device.getPlace();
   }
   
   public String getPopulation() {
   	return this.population;
   }

   @Override
   public UUID getDeviceId() {
      return this.device.getId();
   }

   // TODO currently this is the correlation id from the current platform message
   //      when the protocol supports correlation ids, it should be the correlation
   //      id from the current message (protocol or platform)
   @Override
   public String getCorrelationId() {
      Message message = this.currentMessage;
      if(message == null) {
         return null;
      }
      return message.getCorrelationId();
   }

   @Override
   public Address getDriverAddress() {
      return platformAddress;
   }

   @Override
   public Address getProtocolAddress() {
      return protocolAddress;
   }

   @Override
   public Address getActor() {
      return this.actor;
   }

   @Override
   public void setActor(Address actor) {
      this.actor = actor;
   }

   private void clearActor() {
      this.actor = null;
   }

   @Override
   public AttributeMap getProtocolAttributes() {
      return device.getProtocolAttributes();
   }

   @Override
   public String getDeviceClientId() {
      return this.device.getProtocolid();
   }

   @Override
   public DriverId getDriverId() {
      return driverId;
   }

   @Override
   public Logger getLogger() {
      return logger;
   }

   // TODO load on demand?
   // TODO figure out the threading model for device access
   public Device getDevice() {
      return device;
   }
   
   public DeviceDriverStateHolder getDriverState() {
      return new DeviceDriverStateHolder(attributes, variables);
   }

   @Override
   public boolean hasDirtyAttributes() {
      return !dirtyAttributes.isEmpty();
   }

   @Override
   public AttributeMap getDirtyAttributes() {
      AttributeMap attributes = AttributeMap.newMap();
      for(AttributeKey<?> key: dirtyAttributes) {
         attributes.add(key.coerceToValue(getAttributeValue(key)));
      }
      return attributes;
   }

   @Override
   public long getLastProtocolMessageTimestamp() {
      return lastProtocolMessageTimestamp;
   }

   @Override
   public void setLastProtocolMessageTimestamp(long lastProtocolMessageTimestamp) {
      this.lastProtocolMessageTimestamp = lastProtocolMessageTimestamp;
   }

   /**
    * Gets all the attributes that may be set on
    * this device.
    * @return
    */
   @Override
   public Set<AttributeKey<?>> getSupportedAttributes() {
      return supportedAttributes;
   }

   /**
    * Gets all the attributes that may be set on
    * this device by name.
    * @return
    */
   @Override
   public Map<String,AttributeKey<?>> getSupportedAttributesByName() {
      return supportedAttributesByName;
   }

   /**
    * Gets the keys that have values associated with them.
    * @return
    */
   @Override
   public Set<AttributeKey<?>> getAttributeKeys() {
      return attributeKeys;
   }

   @Override
   @SuppressWarnings("unchecked")
   public <V> V getAttributeValue(AttributeKey<V> key) {
      switch(key.getName()) {
      case Capability.ATTR_ADDRESS:
         return (V) device.getAddress();

      case Capability.ATTR_ID:
         return (V) toStringId(device.getId());

      case Capability.ATTR_TYPE:
         return (V) DeviceCapability.NAMESPACE;

      case Capability.ATTR_CAPS: // FIXME why isn't this read-only?
         return (V) device.getCaps();

      case Capability.ATTR_TAGS:
         return (V) device.getTags();

      case Capability.ATTR_IMAGES:
         return (V) device.getImages();

      case DeviceCapability.ATTR_ACCOUNT:
         return (V) toStringId(device.getAccount());

      case DeviceCapability.ATTR_PLACE:
         return (V) toStringId(device.getPlace());

      case DeviceCapability.ATTR_DEVTYPEHINT:
         return (V) device.getDevtypehint();

      case DeviceCapability.ATTR_MODEL:
         return (V) device.getModel();

      case DeviceCapability.ATTR_PRODUCTID:
         return (V) device.getProductId();

      case DeviceCapability.ATTR_NAME:
         return (V) device.getName();

      case DeviceCapability.ATTR_VENDOR:
         return (V) device.getVendor();

      case DeviceAdvancedCapability.ATTR_ADDED:
         return (V) device.getAdded();

      case DeviceAdvancedCapability.ATTR_DRIVERNAME:
         return (V) device.getDrivername(); // TODO should this be read directly off of driver?

      case DeviceAdvancedCapability.ATTR_DRIVERVERSION:
         return (V) toStringVersion(device.getDriverversion()); // TODO should this be read directly off of driver?

      case DeviceAdvancedCapability.ATTR_DRIVERSTATE:  // coercing through the attribute aligns casing (upper V lower)
         return (V) DeviceAdvancedCapability.KEY_DRIVERSTATE.coerceToValue(device.getState()).getValue();

      case DeviceAdvancedCapability.ATTR_PROTOCOL:
         return (V) device.getProtocol();

      case DeviceAdvancedCapability.ATTR_PROTOCOLID:
         return (V) device.getProtocolid();

      case DeviceAdvancedCapability.ATTR_HUBLOCAL:
         return (V) Boolean.valueOf(isHubLocal);

      case DeviceAdvancedCapability.ATTR_DEGRADED:
         String degradedCode = device.getDegradedCode();
         return (V)Boolean.valueOf(degradedCode != null && !Device.DEGRADED_CODE_NONE.equals(degradedCode));

      case DeviceAdvancedCapability.ATTR_DEGRADEDCODE:
         return (V) device.getDegradedCode();

      default:
         return attributes.get(key);
      }
   }

   // FIXME change this to Object
   @Override
   public <V> V setAttributeValue(AttributeKey<V> key, @Nullable V rawValue) {
      V oldValue;
      
      // shouldn't be necessary, but it appears some code is by-passing the type checking
      V newValue = key.coerceToValue(rawValue).getValue();

      switch(key.getName()) {
      case Capability.ATTR_ADDRESS:
      case Capability.ATTR_ID:
      case Capability.ATTR_TYPE:
      case DeviceCapability.ATTR_ACCOUNT: // not writable from the DSL
      case DeviceCapability.ATTR_PLACE:
      case DeviceAdvancedCapability.ATTR_ADDED:
      case DeviceAdvancedCapability.ATTR_DEGRADED:
         throw new IllegalArgumentException("Attribute [" + key + "] is read-only");

      case DeviceAdvancedCapability.ATTR_PROTOCOL:   // FIXME mark as read-only, currently pass through to allow setting via constructor
         oldValue = key.coerceToValue(device.getProtocol()).getValue();
         device.setProtocol((String) newValue);
         break;

      case DeviceAdvancedCapability.ATTR_PROTOCOLID:   // FIXME mark as read-only, currently pass through to allow setting via constructor
         oldValue = key.coerceToValue(device.getProtocolid()).getValue();
         device.setProtocolid((String) newValue);
         break;

      case Capability.ATTR_CAPS: // FIXME why isn't this read-only?
         checkNotNull(newValue, key);
         oldValue = key.coerceToValue(device.getCaps()).getValue();
         device.setCaps((Set<String>) newValue);
         break;

      case Capability.ATTR_TAGS:
         checkNotNull(newValue, key);
         oldValue = key.coerceToValue(device.getTags()).getValue();
         device.setTags((Set<String>) newValue);
         break;

      case Capability.ATTR_IMAGES:
         oldValue = key.coerceToValue(toStringMap(device.getImages())).getValue();
         device.setImages(toIdMap((Map<String, String>) newValue));
         break;

      case DeviceCapability.ATTR_DEVTYPEHINT:
         checkNotNull(newValue, key);
         oldValue = key.coerceToValue(device.getDevtypehint()).getValue();
         device.setDevtypehint((String) newValue);
         break;

      case DeviceCapability.ATTR_MODEL:
         oldValue = key.coerceToValue(device.getModel()).getValue();
         device.setModel((String) newValue);
         break;

      case DeviceCapability.ATTR_PRODUCTID:
         oldValue = key.coerceToValue(device.getProductId()).getValue();
         device.setProductId((String) newValue);
         break;

      case DeviceCapability.ATTR_NAME:
         oldValue = key.coerceToValue(device.getName()).getValue();
         if(newValue == null) {
            logger.warn("Ignoring request to set device [{}] name to null", device.getAddress(), new IllegalArgumentException());
         }
         else {
            device.setName((String) newValue);
         }
         break;

      case DeviceCapability.ATTR_VENDOR:
         oldValue = key.coerceToValue(device.getVendor()).getValue();
         device.setVendor((String) newValue);
         break;

      case DeviceAdvancedCapability.ATTR_DRIVERNAME: // TODO should this be read-only?
         checkNotNull(newValue, key);
         oldValue = key.coerceToValue(device.getDrivername()).getValue();
         device.setDrivername((String) newValue);
         break;

      case DeviceAdvancedCapability.ATTR_DRIVERVERSION: // TODO should this be read-only?
         checkNotNull(newValue, key);
         Version version = device.getDriverversion();
         oldValue = key.coerceToValue(version != null ? version.getRepresentation() : null).getValue();
         String representation = (String) newValue;
         device.setDriverversion(StringUtils.isEmpty(representation) ? null : Version.fromRepresentation(representation));
         break;

      case DeviceAdvancedCapability.ATTR_DRIVERSTATE:  // should not be writable from drivers directly, but the provisioning, provisioned, misconfigured, mispaired hooks are currently using this
         oldValue = key.coerceToValue(device.getState()).getValue();
         device.setState(newValue.toString().toLowerCase());
         break;

      case DeviceAdvancedCapability.ATTR_HUBLOCAL:   // Should be read-only, currently pass through to allow setting via PlatformDriveExecutorRegistry.associate
         oldValue = key.coerceToValue(device.isHubLocal()).getValue();
         device.setHubLocal(newValue != null && ((Boolean) newValue));
         break;

      case DeviceAdvancedCapability.ATTR_DEGRADEDCODE:
         oldValue = key.coerceToValue(device.getDegradedCode()).getValue();
         device.setDegradedCode((String) newValue);
         break;

      default:
         oldValue = attributes.set(key, newValue);
      }
      if(!Objects.equal(oldValue, newValue)) {
         dirtyAttributes.add(key);
         switch(key.getName()) {
         case DeviceAdvancedCapability.ATTR_DEGRADEDCODE:
            boolean oldDegraded = (oldValue != null && !Device.DEGRADED_CODE_NONE.equals(oldValue));
            boolean newDegraded = (newValue != null && !Device.DEGRADED_CODE_NONE.equals(newValue));
            if (oldDegraded != newDegraded) {
               dirtyAttributes.add(DeviceAdvancedCapability.KEY_DEGRADED);
            }
            break;

         default:
            // no additional updates
            break;
         }
      }
      return oldValue;
   }

   @Override
   public Object setAttributeValue(AttributeDefinition definition, Object value) {
      return setAttributeValue(definition.coerceToValue(value));
   }

   @Override
   public <V> V setAttributeValue(AttributeValue<V> attribute) {
      return setAttributeValue(attribute.getKey(), attribute.getValue());
   }

   @Override
   public <V> V removeAttribute(AttributeKey<V> key) {
      return setAttributeValue(key, null);
   }

   @Override
   public Iterable<AttributeValue<?>> attributes() {
      return
            Iterables
               .concat(
                     Iterables.<AttributeKey<?>, AttributeValue<?>>transform(DEVICE_KEYS, (key) -> new AttributeValue(key, getAttributeValue(key))),
                     attributes.entries()
               );
   }

   @Override
   public Set<String> getVariableNames() {
      return Collections.unmodifiableSet(variables.keySet());
   }

   @Override
   public Object getVariable(String name) {
      return variables.get(name);
   }

   @Override
   public void setVariable(String name, Object value) {
      dirtyVariables.add(name);
      variables.put(name, value);
   }

   @Override
   public <M> void sendToDevice(Protocol<M> protocol, M payload, int timeoutMs) {
   	UUID placeId = getPlaceId();
      ProtocolMessage.Builder builder = protocolMessageBuilder()
            .withPlaceId(placeId)
            .withPopulation(population)
            .withTimeToLive(timeoutMs)
            .withPayload(protocol, payload);
      Address act = getEventActor();
      if(act != null) {
         builder.withActor(act);
      }
      ProtocolMessage message = builder.create();
      ServiceLocator.getInstance(ProtocolMessageBus.class).send(message);
   }

   @Override
   public <M> void sendToDevice(String protocolName, byte[] buffer, int timeoutMs) {
   	UUID placeId = getPlaceId();
      ProtocolMessage.Builder builder = protocolMessageBuilder()
            .withPlaceId(placeId)
            .withPopulation(population)
            .withTimeToLive(timeoutMs)
            .withPayload(protocolName, buffer);
      Address act = getEventActor();
      if(act != null) {
         builder.withActor(act);
      }
      ProtocolMessage message = builder.create();
      ServiceLocator.getInstance(ProtocolMessageBus.class).send(message);
   }

   private Address getEventActor(){
      if(this.actor != null) {
         if(this.actor.toString().contains(".")) {  // Pass on the actor header if the event is part of a rule/scene
            return this.actor;
         }
      }
      if(currentMessage != null) {
         return currentMessage.getActor();
      }
      return null;
   }

   @Override
   public <M> void forwardToDevice(DeviceProtocolAddress dest, Protocol<M> protocol, M payload, int timeoutMs) {
   	UUID placeId = getPlaceId();
   	ProtocolMessage message = protocolMessageBuilder()
            .withPlaceId(placeId)
            .withPopulation(population)
            .withTimeToLive(timeoutMs)
            .withPayload(protocol, payload)
            .from(dest)
            .to(Address.broadcastAddress())
            .create();
      ServiceLocator.getInstance(ProtocolMessageBus.class).send(message);

   }

   @Override
   public void forwardToDevice(DeviceProtocolAddress dest, String protocol, byte[] buffer, int timeoutMs) {
   	UUID placeId = getPlaceId();
      ProtocolMessage message =
            protocolMessageBuilder()
               .withPlaceId(placeId)
               .withPopulation(population)
               .withTimeToLive(timeoutMs)
               .withPayload(protocol, buffer)
               .from(dest)
               .to(Address.broadcastAddress())
               .create();
      ServiceLocator.getInstance(ProtocolMessageBus.class).send(message);
   }

   @Override
   public void respondToPlatform(MessageBody response) {
      ScheduledTask timeout = this.timeout;
      PlatformMessage request = this.currentMessage;
      this.currentMessage = null;
      if(timeout != null) {
         timeout.cancel();
      }
      if(request == null) {
         logger.warn("Received response [{}] with no pending request, broadcasting message", response);
         broadcast(response);
      }
      else {
      	UUID placeId = getPlaceId();
         PlatformMessage.Builder builder =
               PlatformMessage
                  .buildResponse(request, response, getDriverAddress())
                  .withPlaceId(placeId)
                  .withPopulation(population);
         if(pinManager.getActor() != null) {
            builder.withActor(Address.platformService(pinManager.getActor(), PlatformConstants.SERVICE_PEOPLE));
         }
         ServiceLocator.getInstance(PlatformMessageBus.class).send(builder.create());
      }
   }

   @Override
   public void sendToPlatform(PlatformMessage arg0) {
      ServiceLocator.getInstance(PlatformMessageBus.class).send(arg0);
   }

   @Override
   public ListenableFuture<?> broadcast(MessageBody event) {
      String correlationId = getCorrelationId();
      if(correlationId != null) {
         // TODO keep a count?
         correlationId += "-0";
      }
      UUID placeId = getPlaceId();
      PlatformMessage.Builder builder =
            PlatformMessage
               .buildBroadcast(event, this.getDriverAddress())
               .withPlaceId(placeId)
               .withPopulation(population)
               .withCorrelationId(correlationId);
      if(pinManager.getActor() != null) {
         builder.withActor(Address.platformService(pinManager.getActor(), PlatformConstants.SERVICE_PEOPLE));
      } else if (event.getMessageType().equals(Capability.EVENT_VALUE_CHANGE)) {
         ActorContext bestMatch = null;
        for(String attributeName: event.getAttributes().keySet()) {
           ActorContext actor = null;

           //if this is not the final value, keep the actor context for the final attribute value
           if (IntermediateAttributeValueRegistry.isIntermediateValue(attributeName, event.getAttributes().get(attributeName))) {
              actor = actorRequestMap.get(attributeName);
           } else {
              actor = actorRequestMap.remove(attributeName);
           }

           if(actor == null || isExpired(actor)) {
              continue;
           }

           if(bestMatch == null) {
              bestMatch = actor;
           }
           else if(actor.getRequestTime().after(bestMatch.getRequestTime())) {
              bestMatch = actor;
           }
        }
        if(bestMatch != null) {
           builder.withActor(bestMatch.getActorAddress());
        }
      }
      return ServiceLocator.getInstance(PlatformMessageBus.class).send(builder.create());
   }

   @Override
   public boolean hasMessageContext() {
      return this.currentMessage != null;
   }

   @Override
   public void setMessageContext(PlatformMessage message) {
      this.currentMessage = message;
      if (currentMessage.getActor() != null && currentMessage.getMessageType().equals(Capability.CMD_SET_ATTRIBUTES)) {
        ActorContext context = new ActorContext(message);
        for (String attribName: currentMessage.getValue().getAttributes().keySet()) {
           actorRequestMap.put(attribName, context);
        }
      }
      this.setActor(message.getActor());
   }

   @Override
   public boolean isTombstoned() {
      return device.isTombstoned();
   }
   
   @Override
   public boolean isDeleted() {
      return deleted;
   }

   @Override
   public boolean isConnected() {
      return connected;
   }

   @Override
   public void setConnected() {
      connected = true;
   }

   @Override
   public void setDisconnected() {
      connected = false;
   }

   @Override
   public void create() {
      if(StringUtils.isEmpty(device.getName())) {
         logger.warn("Missing name for device [{}]", device.getAddress(), new IllegalStateException());
         device.setName(FALLBACK_DEVICE_NAME);
      }

      DeviceDAO deviceDao = getDeviceDao();
      saveDeviceIfDirty(deviceDao);
      deviceDao.replaceDriverState(device, new DeviceDriverStateHolder(attributes, variables));
      Map<String,Object> deviceAttrs = new HashMap<>();
      for(AttributeKey<?> key: getAttributeKeys()) {
         Object value = getAttributeValue(key);
         if(value != null) {
            deviceAttrs.put(key.getName(), value);
         }
      }

      dirtyAttributes.clear();
      dirtyVariables.clear();

      MessageBody event = MessageBody.buildMessage(Capability.EVENT_ADDED, deviceAttrs);
      broadcast(event);
   }

   @Override
   public void delete() {
      this.deleted = true;
      doDelete(false);
   }
   
   @Override
   public void tombstone() {
      doDelete(true);
   }
   
   /**
    * Saves any changes to the device and to attributes.  If any
    * attributes have been changed this will also create an
    * DeviceValueChange event.
    */
   @Override
   public void commit() {
      if(isDeleted()) {
         logger.warn("Ignoring update to deleted driver {}", this);
         return;
      }

      // ---------------  ITWO-7101 Fix null connection state information ----------------
      if(StringUtils.isEmpty(attributes.get(DeviceConnectionCapability.KEY_STATE))) {
         if(connected) {
            setAttributeValue(DeviceConnectionCapability.KEY_STATE, DeviceConnectionCapability.STATE_ONLINE);
         }
         else {
            setAttributeValue(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_OFFLINE));
         }
         setAttributeValue(DeviceConnectionCapability.KEY_LASTCHANGE, device.getAdded());
      }
      if(isPresenceCapableDevice() && StringUtils.isEmpty(attributes.get(ATTR_DEVICE_PRESENCE))) {
         if(connected) {
            setAttributeValue(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
         }
         else {
            setAttributeValue(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
         }
         setAttributeValue(ATTR_DEVICE_PRESENCE_CHANGED, device.getAdded());
      }
      // ------------------------------------------------------------------------------------
      
      DeviceDAO deviceDao = getDeviceDao();
      Map<String,Object> dirtyMap = new HashMap<>();
      for (AttributeKey<?> key : dirtyAttributes) {
         dirtyMap.put(key.getName(),getAttributeValue(key));
      }

      saveDeviceIfDirty(deviceDao);
      if(hasMessageContext()) {
         setTimeout();
      }
      AttributeMap dirty = AttributeMap.filterKeys(attributes, dirtyAttributes);
      if(!dirty.isEmpty() || !dirtyVariables.isEmpty()) {
         deviceDao.updateDriverState(device, new DeviceDriverStateHolder(dirty, dirtyVariables.isEmpty() ? Collections.emptyMap() : variables));
         dirtyVariables.clear();
      }
      // not this isn't in the if block b/c the dirty attributes might have been on the device
      dirtyAttributes.clear();

      if(!dirtyMap.isEmpty()) {
         MessageBody event = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, dirtyMap);
         broadcast(event);
      }
      pinManager.setActor(null);

      this.clearActor();
   }

   private boolean isAbsent() {
      return PresenceCapability.PRESENCE_ABSENT.equals(attributes.get(ATTR_DEVICE_PRESENCE));
   }
   private boolean isPresenceCapableDevice() {
      return device.getCaps().contains(PresenceCapability.NAMESPACE);
   }
   
   @Override
   public void clearDirty() {
      dirtyAttributes.clear();
   }

   @Override
   public void cancel(PlatformMessage message) {
      if(this.currentMessage != message) {
         return;
      }
      logger.debug("Timing out request {}", this.currentMessage);
      respondToPlatform(Errors.requestTimeout());
   }

   @Override
   public PinManager getPinManager() {
      return pinManager;
   }

   @Override
   public ReflexDriverContext getReflexContext() {
      return reflexContext;
   }

   private void setTimeout() {
      ScheduledTask timeout = this.timeout;
      PlatformMessage currentMessage = this.currentMessage;
      if(timeout != null || currentMessage == null) {
         return;
      }

      long timeoutMs;
      if(currentMessage.getTimeToLive() > 0) {
         timeoutMs = currentMessage.getTimestamp().getTime() + currentMessage.getTimeToLive();
      }
      else {
         timeoutMs = System.currentTimeMillis() + defaultTimeoutMs;
      }

      Date date = new Date(timeoutMs);
      logger.debug("No response for [{}] scheduling timeout for [{}]", currentMessage.getMessageType(), date);
      DriverExecutors.get().defer(
            new PlatformMessageTimeout(currentMessage),
            date
      );
   }

   private boolean saveDeviceIfDirty(DeviceDAO deviceDao) {
      Device original = this.persisted;
      if(device.equals(original)) {
         return false;
      }

      this.persisted = deviceDao.save(device);
      this.device = this.persisted.copy();
      return true;
   }

   private void doDelete(boolean tombstone) {
      String deviceName = device.getName();
      if(tombstone) {
         logger.debug("Tombstoning device [{}] named [{}]", device.getId(), device.getName());
         device.setState(Device.STATE_TOMBSTONED);
         this.persisted = getDeviceDao().save(device);
         this.device = persisted.copy();
      }
      else {
         logger.debug("Deleting device [{}] named [{}]", device.getId(), device.getName());
         // TODO set persisted to null?
         getDeviceDao().delete(device);
      }
      Map<String, Object> attributeMap = new HashMap<>(2);
      if(deviceName != null) {
      	attributeMap.put(DeviceCapability.ATTR_NAME, deviceName);
      }
      if(device.getDevtypehint() != null) {
      	attributeMap.put(DeviceCapability.ATTR_DEVTYPEHINT, device.getDevtypehint());
      }
      MessageBody event = MessageBody.buildMessage(Capability.EVENT_DELETED, Collections.unmodifiableMap(attributeMap));
      broadcast(event);
      if(hasMessageContext()) {
         respondToPlatform(DeviceErrors.deviceDeleted());
      }
   }

   private ProtocolMessage.Builder protocolMessageBuilder() {
      return
         ProtocolMessage
            .builder()
            .from(getDriverAddress())
            .to(getProtocolAddress())
            .withCorrelationId(getCorrelationId())
            ;
   }

   private boolean isExpired(ActorContext context) {
      return ((context.getRequestTime().getTime() + actorContextTimeoutMs) < System.currentTimeMillis());
   }


   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "PlatformDeviceDriverContext [persisted=" + persisted
            + ", device=" + device + ", deleted=" + deleted + ", driverId="
            + driverId + "]";
   }

   private static <V> void checkNotNull(V value, AttributeKey<V> key) {
      if(value == null) {
         throw new IllegalArgumentException("Attribute [" + key + "] may not be null / removed");
      }
   }

   private static Map<String,AttributeKey<?>> loadSupportedAttributes(DeviceDriverDefinition driverDefinition) {
      Set<AttributeDefinition> attributeDefs = driverDefinition.getAttributes();
      ImmutableMap.Builder<String,AttributeKey<?>> attributeKeys = ImmutableMap.builder();
      for(AttributeDefinition attributeDef: attributeDefs) {
         AttributeKey<?> key = attributeDef.getKey();
         attributeKeys.put(attributeDef.getName(), attributeDef.getKey());
      }
      return attributeKeys.build();
   }

   private static Map<String,String> toStringMap(Map<String, UUID> images) {
      if(images == null || images.isEmpty()) {
         return Collections.emptyMap();
      }
      Map<String, String> result = new HashMap<String, String>(images.size());
      for(Map.Entry<String, UUID> entry: images.entrySet()) {
         if(entry.getValue() == null) {
            continue;
         }
         result.put(entry.getKey(), entry.getValue().toString());
      }
      return result;
   }

   private static Map<String,UUID> toIdMap(Map<String, String> images) {
      if(images == null || images.isEmpty()) {
         return Collections.emptyMap();
      }
      Map<String, UUID> result = new HashMap<String, UUID>(images.size());
      for(Map.Entry<String, String> entry: images.entrySet()) {
         if(entry.getValue() == null) {
            continue;
         }
         result.put(entry.getKey(), UUID.fromString(entry.getValue()));
      }
      return result;
   }

   private static String toStringId(UUID id) {
      return id != null ? id.toString() : null;
   }

   private static String toStringVersion(Version version) {
      return version != null ? version.getRepresentation() : null;
   }

   private static <V> void setIfNotNull(
         AttributeMap attributes,
         AttributeKey<V> key,
         V value
   ) {
      if(value != null) {
         attributes.set(key, value);
      }
   }

   private static void setIfNotNullId(
         AttributeMap attributes,
         AttributeKey<String> key,
         Object value
   ) {
      if(value != null) {
         attributes.set(key, value.toString());
      }
   }

   private static <V> DeviceAttributeValue<V> createAttribute(AttributeKey<V> key, Supplier<V> supplier) {
      return new DeviceAttributeValue<>(key, supplier);
   }

   private static class DeviceAttributeValue<V> extends AttributeValue<V> {

      private final Supplier<V> supplier;

      public DeviceAttributeValue(AttributeKey<V> key, Supplier<V> supplier) {
         super(key, null);
         this.supplier = supplier;
      }

      public V getValue() {
         return supplier.get();
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = super.hashCode();
         result = prime * result
               + ((supplier == null) ? 0 : supplier.hashCode());
         return result;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (!super.equals(obj)) return false;
         if (getClass() != obj.getClass()) return false;
         DeviceAttributeValue other = (DeviceAttributeValue) obj;
         if (supplier == null) {
            if (other.supplier != null) return false;
         }
         else if (!supplier.equals(other.supplier)) return false;
         return true;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "DeviceAttributeValue [supplier=" + supplier + "]";
      }

   }
}

