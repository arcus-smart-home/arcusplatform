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
package com.iris.agent.reflex;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.util.RxIris;
import com.iris.driver.reflex.ReflexDriverContext;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.protocol.ZWaveCommandClassFrame;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zwave.Protocol;
import com.iris.util.IrisAttributeLookup;

public final class ReflexDriverHubContext implements ReflexDriverContext {
   private static final Logger log = LoggerFactory.getLogger(ReflexDriverHubContext.class);
   private static final Logger logDriver = LoggerFactory.getLogger("driver");
   private static final boolean ENABLE_DEBUG_LOGGING = System.getenv("IRIS_AGENT_REFLEX_LOGGING") != null;

   final ReflexController parent;
   final Address addr;
   final String addrrep;
   final Map<String,Object> state;

   private @Nullable Map<String,Object> attrs;
   private @Nullable Map<String,Object> emitAttrs;
   private @Nullable Map<String,Object> variables;
   private @Nullable Set<String> setAttrsConsumed;
   private @Nullable MessageBody msgResponse;
   private boolean handled = false;

   public ReflexDriverHubContext(ReflexController parent, Address addr) {
      this.parent = parent;
      this.addr = addr;
      this.addrrep = addr.getRepresentation();
      this.state = new HashMap<>();
      this.variables = new HashMap<>();
   }

   @Override
   public Logger getDriverLogger() {
      return logDriver;
   }

   @Override
   public Address getProtocolAddress() {
      return addr;
   }

   @Override
   public String getProtocolName() {
      return ((DeviceProtocolAddress)addr).getProtocolName();
   }

   public Map<String,Object> getState() {
      return state;
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
		Set<String> results = setAttrsConsumed;
		setAttrsConsumed = null;
		return results;
	}

  	@Override
	public void setResponse(MessageBody rsp) {
	   msgResponse = rsp;
	}

	@Override
	public MessageBody getAndResetResponse() {
		MessageBody results = msgResponse;
		msgResponse = null;
		return results;
	}

   @Override
   public Map<String,Object> getAttributesToEmit() {
      boolean emitEmpty = (emitAttrs == null || emitAttrs.isEmpty());
      boolean attrEmpty = (attrs == null || attrs.isEmpty());

      if (emitEmpty && attrEmpty) {
         return ImmutableMap.of();
      }

      ImmutableMap.Builder<String,Object> bld = ImmutableMap.builder();
      if (!emitEmpty) {
         bld.putAll(emitAttrs);
      }

      if (!attrEmpty) {
         bld.putAll(attrs);
      }

      return bld.build();
   }

   @Override
   public void commit() {
      if (attrs != null && !attrs.isEmpty()) {
         debuglog("committing state to db for {}: {}", addrrep, attrs);
         ReflexDao.putDriverState(addrrep, attrs);
      }
   }

   @Override
   public void reset() {
      attrs = null;
      variables = null;
      emitAttrs = null;
      handled = false;
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
   
   void initializeAttributes(Map<String,String> attrs) {
      debuglog("setting initial state from db for {}: {}", addrrep, attrs);

      for (Map.Entry<String,String> entry : attrs.entrySet()) {
         if (entry.getValue() == null) {
            state.put(entry.getKey(), null);
            continue;
         }

         Object value;
         try {
            Object coerced = IrisAttributeLookup.coerce(entry.getKey(), entry.getValue());
            value = (coerced != null) ? coerced : entry.getValue();
         } catch (Exception ex) {
            log.warn("could not coerce value of {} to correct type: value={}", entry.getKey(), entry.getValue());
            value = entry.getValue();
         }

         state.put(entry.getKey(), value);
      }
   }

   @Override
   public void setAttribute(String key, Object value) {
      Object old = state.put(key, value);
      if (!Objects.equals(old,value)) {
         debuglog("setting attribute for {}: {} -> {}", addrrep, key, value);
         attrs = addto(attrs, key, value);
      }
   }

   @Override
   public void setAttributes(Map<String,Object> values) {
      for (Map.Entry<String,Object> entry : values.entrySet()) {
         debuglog("setting attributes for {}: {}", addrrep, values);
         setAttribute(entry.getKey(), entry.getValue());
      }
   }

   @Override
   public void emitAttribute(String attr, Object value) {
      debuglog("emitting attribute update for {}: {} -> {}", addrrep, attr, value);
      emitAttrs = addto(emitAttrs, attr, value);
   }

   @Override
   public void emitAttributes(Map<String,Object> values) {
      debuglog("emitting attribute updates for {}: {}", addrrep, values);
      emitAttrs = addto(emitAttrs, values);
   }

   @Override
   public void emit(MessageBody message) {
      debuglog("emitting message for {}: {}", addrrep, message);
      parent.emit(addr, message);
   }

   @Override
   public void zigbeeSend(ZigbeeMessage.Protocol msg) {
//      parent.zigbee().send(addr, msg).subscribe(RxIris.SWALLOW_ALL);
   }

   @Override
   public void zwaveSend(Protocol.Message msg) {
//      parent.zwave().send(addr, msg).subscribe(RxIris.SWALLOW_ALL);
   }

   @Override
   public void zwaveAddScheduledPoll(long time, TimeUnit unit, Collection<byte[]> polls) {
//      parent.zwave().addScheduledPoll(addr, time, unit, polls);
   }

   private static Map<String,Object> addto(@Nullable Map<String,Object> map, Map<String,Object> values) {
      Map<String,Object> m = map;
      if (m == null) {
         m = new HashMap<>();
      }

      m.putAll(values);
      return m;
   }

   private static Map<String,Object> addto(@Nullable Map<String,Object> map, String key, Object value) {
      Map<String,Object> m = map;
      if (m == null) {
         m = new HashMap<>();
      }

      m.put(key, value);
      return m;
   }

   private static void debuglog(String format, Object... args) {
      if (ENABLE_DEBUG_LOGGING) {
         log.debug(format, args);
      }
   }
}

