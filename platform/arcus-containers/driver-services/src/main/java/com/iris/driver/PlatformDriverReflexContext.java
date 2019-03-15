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

import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.reflex.ReflexDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.ZWaveExternalProtocol;

public class PlatformDriverReflexContext implements ReflexDriverContext {
   private final PlatformDeviceDriverContext parent;
   private @Nullable Map<String,Object> emitAttrs; 
   private @Nullable Map<String,Object> variables; 
   private @Nullable Set<String> setAttrsConsumed; 
   private @Nullable MessageBody msgResponse;
   private boolean handled;

   public PlatformDriverReflexContext(PlatformDeviceDriverContext parent) {
      this.parent = parent;
      this.emitAttrs = null;
      this.variables = null;
      this.handled = false;
   }

   @Override
   public Logger getDriverLogger() {
      return parent.getLogger();
   }

   @Override
   public void commit() {
      // Ignored, the device driver implementation is responsible for committing
   }

   @Override
   public void reset() {
      this.handled = false;
      this.emitAttrs = null;
      this.variables = null;
   }

   @Override
   public void markMessageHandled(boolean handled) {
      this.handled = handled;
   }

   @Override
   public boolean wasMessageHandled() {
      return handled;
   }

   @Override
   public Address getProtocolAddress() {
      return parent.getProtocolAddress();
   }

   @Override
   public String getProtocolName() {
      return ((DeviceProtocolAddress)getProtocolAddress()).getProtocolName();
   }

   @Override
   public void markSetAttributeConsumed(String key) {
      if (setAttrsConsumed == null) {
         setAttrsConsumed = new HashSet<>();
      }

      setAttrsConsumed.add(key);
   }

   @Override
   public Set<String> getAndResetSetAttributesConsumed() {
      Set<String> result = setAttrsConsumed;
      setAttrsConsumed = null;
      return result;
   }

   @Override
   public void setResponse(MessageBody rsp) {
      msgResponse = rsp;
   }

   @Override
   public MessageBody getAndResetResponse() {
      MessageBody result = msgResponse;
      msgResponse = null;
      return result;
   }

   @Override
   public void setAttribute(String attr, Object value) {
      Map<String,AttributeKey<?>> supported = parent.getSupportedAttributesByName();
      AttributeKey<?> key = supported.get(attr);
      if (key == null) {
         parent.getLogger().warn("device reflexes set attribute '{}' that is not supported.", attr);
         return;
      }

      parent.setAttributeValue((AttributeKey<Object>)key, value);
   }

   @Override
   public void setAttributes(Map<String, Object> attrs) {
      for (Map.Entry<String,Object> entry : attrs.entrySet()) {
         setAttribute(entry.getKey(), entry.getValue());
      }
   }

   @Override
   public void emitAttribute(String attr, Object value) {
      Map<String,Object> emit = emitAttrs;
      if (emit == null) {
         emit = new HashMap<>();
         emitAttrs = emit;
      }

      emit.put(attr, value);
   }

   @Override
   public void emitAttributes(Map<String, Object> attrs) {
      Map<String,Object> emit = emitAttrs;
      if (emit == null) {
         emit = new HashMap<>();
         emitAttrs = emit;
      }

      emit.putAll(attrs);
   }

   @Override
   public Map<String, Object> getAttributesToEmit() {
      AttributeMap dirty = parent.getDirtyAttributes();

      boolean emitEmpty = (emitAttrs == null || emitAttrs.isEmpty());
      boolean attrEmpty = (dirty == null || dirty.isEmpty());
      if (emitEmpty && attrEmpty) {
         return ImmutableMap.of();
      }

      return null;
   }

	@Override
	public @Nullable Object setVariable(String name, @Nullable Object value) {
		Map<String,Object> v = variables;
      if (v == null) {
      	if (value == null) {
         	return null;
         }

       	v = new HashMap<>();
        	variables = v;
      }

      if (value == null) {
      	return v.remove(name);
      }

      return v.put(name, value);
	}

	@Override
	public @Nullable <T> T getVariable(String name) {
      Map<String,Object> v = variables;
      if (v == null) {
         return (T)null;
      }

		return (T)v.get(name);
	}

   @Override
	public <T> T getVariable(String name, T defaultValue) {
		Map<String,Object> v = variables;
      if (v == null) {
         return defaultValue;
      }

		T val = (T)v.get(name);
      return (val != null) ? val : defaultValue;
   }

   @Override
   public void zigbeeSend(com.iris.protocol.zigbee.msg.ZigbeeMessage.Protocol msg) {
      try {
         parent.sendToDevice(ZigbeeProtocol.INSTANCE, msg, -1);
      } catch (Exception ex) {
         parent.getLogger().debug("failed to transmit zigbee message to device", ex);
      }
   }

   @Override
   public void zwaveSend(Protocol.Message msg) {
      try {
         parent.sendToDevice(ZWaveExternalProtocol.INSTANCE, msg, -1);
      } catch (Exception ex) {
         parent.getLogger().debug("failed to transmit zwave message to device", ex);
      }
   }

   @Override
   public void zwaveAddScheduledPoll(long period, TimeUnit unit, Collection<byte[]> payloads) {
      if (payloads == null || payloads.isEmpty()) {
         return;
      }

      try {
         int i = 0;
         com.iris.protocol.zwave.Protocol.Schedule[] schds = new com.iris.protocol.zwave.Protocol.Schedule[payloads.size()];
         for (byte[] payload : payloads) {
            schds[i++] = com.iris.protocol.zwave.Protocol.Schedule.builder()
               .setPayload(payload)
               .create();
         }

         long seconds = Math.max(1,unit.toSeconds(period));
         com.iris.protocol.zwave.Protocol.SetSchedule sch = com.iris.protocol.zwave.Protocol.SetSchedule.builder()
            .setNodeId(0)
            .setSeconds((int)seconds)
            .setSchedule(schds)
            .create();

         com.iris.protocol.zwave.Protocol.Message msg = com.iris.protocol.zwave.Protocol.Message.builder()
            .setType(com.iris.protocol.zwave.Protocol.SetSchedule.ID)
            .setPayload(ByteOrder.BIG_ENDIAN, sch)
            .create();

         parent.sendToDevice(ZWaveExternalProtocol.INSTANCE, msg, -1);
      } catch (Exception ex) {
         parent.getLogger().debug("failed to transmit set scheduled poll to device", ex);
      }
   }

   @Override
   public void emit(MessageBody msg) {
      parent.sendToPlatform(
         PlatformMessage.buildEvent(msg, parent.getDriverAddress())
            .withPlaceId(parent.getPlaceId())
            .withPopulation(parent.getPopulation())
            .create()
      );
   }
}

