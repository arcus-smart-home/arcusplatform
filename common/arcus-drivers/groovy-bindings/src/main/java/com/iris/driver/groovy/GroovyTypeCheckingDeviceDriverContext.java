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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.pin.PinManager;
import com.iris.driver.reflex.ReflexDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.model.DriverId;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.protocol.Protocol;
import com.iris.util.IrisUUID;

public class GroovyTypeCheckingDeviceDriverContext implements DeviceDriverContext {
   private static final Logger logger = LoggerFactory.getLogger(GroovyTypeCheckingDeviceDriverContext.class);
   private static final UUID PLACE_ID = IrisUUID.nilUUID();
   private static final UUID DEVICE_ID = IrisUUID.nilUUID();
   private static final UUID CORRELATION_ID = IrisUUID.nilUUID();
   private static final String POPULATION = Population.NAME_GENERAL;

   private static final Address DRIVER_ADDRESS = Address.platformDriverAddress(DEVICE_ID);
   private static final Address PROTOCOL_ADDRESS = Address.protocolAddress("TYPC", new byte[0]);
   private static final DriverId DRIVER_ID = new DriverId("TYPC", Version.UNVERSIONED);

   @Override
   public Address getActor() {
      return null;
   }

   @Override
   public void setActor(Address actor) {
   }
   
   @Override
   public UUID getPlaceId() {
      return PLACE_ID;
   }
   
   @Override
	public String getPopulation() {
		return POPULATION;
	}

   @Override
   public UUID getDeviceId() {
      return DEVICE_ID;
   }

   @Override
   public String getCorrelationId() {
      return CORRELATION_ID.toString();
   }

   @Override
   public Address getDriverAddress() {
      return DRIVER_ADDRESS;
   }

   @Override
   public Address getProtocolAddress() {
      return PROTOCOL_ADDRESS;
   }

   @Override
   public AttributeMap getProtocolAttributes() {
      return AttributeMap.newMap();
   }

   @Override
   public String getDeviceClientId() {
      return "CLNT:typc:0";
   }

   @Override
   public DriverId getDriverId() {
      return DRIVER_ID;
   }

   @Override
   public Logger getLogger() {
      return logger;
   }

   @Override
   public boolean hasDirtyAttributes() {
      return false;
   }

   @Override
   public AttributeMap getDirtyAttributes() {
      return AttributeMap.newMap();
   }

   @Override
   public long getLastProtocolMessageTimestamp() {
      return 0;
   }

   @Override
   public void setLastProtocolMessageTimestamp(long lastProtocolMessageTimestamp) {
   }

   @Override
   public Set<AttributeKey<?>> getSupportedAttributes() {
      return null;
   }

   @Override
   public Map<String,AttributeKey<?>> getSupportedAttributesByName() {
      return null;
   }

   @Override
   public Set<AttributeKey<?>> getAttributeKeys() {
      return null;
   }

   @Override
   public <V> V getAttributeValue(AttributeKey<V> key) {
      return null;
   }

   @Override
   public <V> V setAttributeValue(AttributeKey<V> key, V value) {
      return null;
   }

   @Override
   public Object setAttributeValue(AttributeDefinition definition, Object value) {
      return null;
   }

   public Object setAttributeValue(com.iris.capability.definition.AttributeDefinition definition, Object value) {
      return null;
   }

   @Override
   public <V> V setAttributeValue(AttributeValue<V> attribute) {
      return null;
   }

   @Override
   public <V> V removeAttribute(AttributeKey<V> key) {
      return null;
   }

   @Override
   public Iterable<AttributeValue<?>> attributes() {
      return null;
   }

   @Override
   public Set<String> getVariableNames() {
      return null;
   }

   @Override
   public Object getVariable(String name) {
      return null;
   }

   @Override
   public void setVariable(String name, Object value) {
   }

   @Override
   public <M> void sendToDevice(Protocol<M> protocol, M payload, int timeoutMs) {
   }

   @Override
   public <M> void sendToDevice(String protocolName, byte[] buffer, int timeoutMs) {
   }

   @Override
   public <M> void forwardToDevice(DeviceProtocolAddress dest, Protocol<M> protocol, M payload, int timeoutMs) {
   }

   @Override
   public void forwardToDevice(DeviceProtocolAddress dest, String protocolName, byte[] buffer, int timeoutMs) {
   }

   @Override
   public void respondToPlatform(MessageBody response) {
   }

   @Override
   public void sendToPlatform(PlatformMessage msg) {
   }

   @Override
   public ListenableFuture<?> broadcast(MessageBody event) {
      return Futures.immediateFuture(true);
   }

   @Override
   public boolean hasMessageContext() {
      return false;
   }

   @Override
   public void setMessageContext(PlatformMessage message) {
   }

   @Override
   public boolean isDeleted() {
      return false;
   }

   @Override
   public boolean isTombstoned() {
      return false;
   }

   @Override
   public boolean isConnected() {
      return false;
   }

   @Override
   public void setConnected() {
   }

   @Override
   public void setDisconnected() {
   }

   @Override
   public void create() {
   }

   @Override
   public void delete() {
   }

   @Override
   public void tombstone() {
   }

   @Override
   public void commit() {
   }

   @Override
   public void clearDirty() {
   }

   @Override
   public void cancel(PlatformMessage message) {
   }

   @Override
   public PinManager getPinManager() {
      return TypeCheckingPinManager.INSTANCE;
   }

   @Override
   public ReflexDriverContext getReflexContext() {
      return null;
   }

   public static enum TypeCheckingPinManager implements PinManager {
      INSTANCE;

      private static final byte[] PIN = "1234".getBytes(StandardCharsets.UTF_8);
      private static final UUID USER_ID = IrisUUID.nilUUID();

      @Override
      public byte[] getPin(UUID placeId, UUID personId) {
         return PIN;
      }

      @Override
      public UUID validatePin(UUID placeId, byte[] pin) {
         return USER_ID;
      }

      @Override
      public UUID validatePin(UUID placeId, String pin) {
         return USER_ID;
      }

      @Override
      public void setActor(UUID actor) {
      }

      @Override
      public UUID getActor() {
         return USER_ID;
      }

      @Override
      public UUID accumulatePin(UUID placeId, int code) {
         return USER_ID;
      }
   }

	
}

