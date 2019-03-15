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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.pin.PinManager;
import com.iris.driver.reflex.ReflexDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.model.DriverId;
import com.iris.protocol.Protocol;

// TODO should you be able to get the driver from here? why not?
public interface DeviceDriverContext {

   public UUID getPlaceId();
   
   public String getPopulation();

   public UUID getDeviceId();

   // TODO currently this is the correlation id from the current platform message
   //      when the protocol supports correlation ids, it should be the correlation
   //      id from the current message (protocol or platform)
   public String getCorrelationId();

   public Address getDriverAddress();

   public Address getProtocolAddress();

   public AttributeMap getProtocolAttributes();

   public String getDeviceClientId();

   public DriverId getDriverId();

   public Logger getLogger();

   public Address getActor();

   public void setActor(Address actor);

   public boolean hasDirtyAttributes();

   public AttributeMap getDirtyAttributes();

   public long getLastProtocolMessageTimestamp();

   public void setLastProtocolMessageTimestamp(long lastProtocolMessageTimestamp);

   /**
    * Gets all the attributes that may be set on
    * this device.
    * @return
    */
   // TODO optimize this call
   public Set<AttributeKey<?>> getSupportedAttributes();

   /**
    * Gets all the attributes that may be set on
    * this device as a map from attribute name to
    * attribute key.
    * @return
    */
   public Map<String,AttributeKey<?>> getSupportedAttributesByName();

   /**
    * Gets the keys that have values associated with them.
    * @return
    */
   public Set<AttributeKey<?>> getAttributeKeys();

   public <V> V getAttributeValue(AttributeKey<V> key);

   public <V> V setAttributeValue(AttributeKey<V> key, V value);

   public Object setAttributeValue(AttributeDefinition definition, Object value);

   public <V> V setAttributeValue(AttributeValue<V> attribute);

   public <V> V removeAttribute(AttributeKey<V> key);

   public Iterable<AttributeValue<?>> attributes();

   public Set<String> getVariableNames();

   public Object getVariable(String name);

   public void setVariable(String name, Object value);

   public <M> void sendToDevice(Protocol<M> protocol, M payload, int timeoutMs);

   public <M> void sendToDevice(String protocolName, byte[] buffer, int timeoutMs);

   public <M> void forwardToDevice(DeviceProtocolAddress dest, Protocol<M> protocol, M payload, int timeoutMs);

   public void forwardToDevice(DeviceProtocolAddress dest, String protocolName, byte[] buffer, int timeoutMs);

   public void respondToPlatform(MessageBody response);

   public void sendToPlatform(PlatformMessage msg);

   public ListenableFuture<?> broadcast(MessageBody event);

   public boolean hasMessageContext();

   public void setMessageContext(PlatformMessage message);

   public boolean isDeleted();

   public boolean isTombstoned();
   
   public boolean isConnected();

   public void setConnected();

   public void setDisconnected();

   public void create();

   public void delete();
   
   public void tombstone();

   /**
    * Saves any changes to the device and to attributes.  If any
    * attributes have been changed this will also create an
    * DeviceValueChange event.
    */
   public void commit();

   public void clearDirty();

   public void cancel(PlatformMessage message);

   public PinManager getPinManager();

   public ReflexDriverContext getReflexContext();
}

