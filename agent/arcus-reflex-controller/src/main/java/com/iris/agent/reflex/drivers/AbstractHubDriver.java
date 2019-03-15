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
package com.iris.agent.reflex.drivers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.config.ConversionService;
import com.iris.agent.reflex.AbstractReflexProcessor;
import com.iris.agent.reflex.ReflexController;
import com.iris.agent.reflex.ReflexDao;
import com.iris.agent.util.Backoff;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.model.Version;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.ProtocolMessage;

public abstract class AbstractHubDriver extends AbstractReflexProcessor implements HubDriver {
   private static final Logger log = LoggerFactory.getLogger(AbstractHubDriver.class);
   private static final ConcurrentMap<String,ConcurrentMap<Version,Map<String,Class<?>>>> conversions = new ConcurrentHashMap<>();

   protected final ReflexController parent;
   protected final Address addr;
   protected final String saddr;

   private final Map<String,Object> state = new HashMap<>();
   private final Map<String,Object> dirty = new HashMap<>();
   private final Map<String,Object> report = new HashMap<>();
   private boolean inHandler;

   public AbstractHubDriver(ReflexController parent, Address addr) {
      this.parent = parent;
      this.addr = addr;
      this.saddr = addr.getRepresentation();
   }

   @Override
   public Address getAddress() {
      return addr;
   }

   @Override
   public Map<String,Object> getState() {
      return state;
   }

   @Override
   public @Nullable Object getAttribute(String name) {
      Map<String,Object> attrs = getState();
      return (attrs != null) ? attrs.get(name) : null;
   }

   @Override
   public Map<String,Object> getSyncState() {
      Map<String,Object> state = getState();
      ImmutableMap.Builder<String,Object> sync = ImmutableMap.builder();
      for (Map.Entry<String,Object> entry : state.entrySet()) {
         if (entry.getKey() != null && !entry.getKey().startsWith("_")) {
            sync.put(entry.getKey(), entry.getValue());
         }
      }

      return sync.build();
   }

   @Override
   public boolean isDegraded() {
      return false;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver Lifecycle
   /////////////////////////////////////////////////////////////////////////////
   
   public void start() {
   }
   
   @Override
   public void start(Map<String,String> restore, State curState) {
      ConcurrentMap<Version,Map<String,Class<?>>> dconvs = conversions.computeIfAbsent(getDriverName(), (k) -> new ConcurrentHashMap<>());
      Map<String,Class<?>> convs = dconvs.computeIfAbsent(getDriverVersion(), (v) -> new ConcurrentHashMap<>());
      for (Map.Entry<String,String> entry : restore.entrySet()) {
         Class<?> type = convs.get(entry.getKey());
         if (type == null) {
            log.warn("cannot restore driver state {} for driver {} {}: data type not registered", entry.getKey(), getDriverName(), getDriverVersion());
            continue;
         }

         Object value = ConversionService.to(type, entry.getValue());
         state.put(entry.getKey(), value);
         if (log.isTraceEnabled()) {
            log.trace("restore driver state {}={} for driver {} {}", entry.getKey(), value, getDriverName(), getDriverVersion());
         }
      }

      super.start(curState);
      start();
   }

   @Override
   public void shutdown() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver APIs
   /////////////////////////////////////////////////////////////////////////////
   
   public @Nullable UUID verifyPinCode(byte[] ascii) {
      try {
         return verifyPinCode(new String(ascii, StandardCharsets.US_ASCII));
      } catch (Exception ex) {
         log.warn("could not decode pin code: ", ex);
         return null;
      }
   }
   
   public @Nullable UUID verifyPinCode(String code) {
      return parent.verifyPinCode(code);
   }
   
   public void submit(Runnable task) {
      parent.submit(addr, task);
   }

   public void schedule(Runnable task, long time, TimeUnit unit) {
      parent.schedule(addr, task, time, unit);
   }

   public void periodic(Runnable task, long delay, long period, TimeUnit unit) {
      parent.periodic(addr, task, delay, period, unit);
   }

   public void periodic(Runnable task, Backoff backoff) {
      parent.periodic(addr, task, backoff);
   }

   public void cancelPeriodicTask() {
      parent.cancelPeriodicTask();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver Lifecycle
   /////////////////////////////////////////////////////////////////////////////
   
   protected void doOnAdded() {
   }
   
   protected void doOnConnected() {
   }
   
   protected void doOnDisconnected() {
   }
   
   protected void doOnRemoved() {
   }
   
   @Override
   protected void onAdded() {
      try {
         doOnAdded();
      } finally {
         setCurrentState(State.ADDED);
      }
   }

   @Override
   protected void onConnected() {
      try {
         doOnConnected();
      } finally {
         setCurrentState(State.CONNECTED);
      }
   }

   @Override
   protected void onDisconnected() {
      try {
         doOnDisconnected();
      } finally { 
         setCurrentState(State.DISCONNECTED);
      }
   }

   @Override
   protected void onRemoved() {
      doOnRemoved();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver Message Processing
   /////////////////////////////////////////////////////////////////////////////
   
   protected void handleValueChanges(Map<String,Object> attrs) {
   }
   
   protected void handleSetAttributes(Map<String,Object> attrs) {
   }
   
   protected void handleCommand(MessageBody msg) {
   }

   protected void handle(MessageBody msg) {
      switch (msg.getMessageType()) {
      case Capability.EVENT_VALUE_CHANGE:
         try {
            handleValueChanges(msg.getAttributes());
         } catch (Exception ex) {
            log.info("exception while processing value changes: ", ex);
         }
         break;
      case Capability.CMD_SET_ATTRIBUTES:
         try {
            handleSetAttributes(msg.getAttributes());
         } catch (Exception ex) {
            log.info("exception while processing set attributes: ", ex);
         }
         break;
      default:
         try {
            handleCommand(msg);
         } catch (Exception ex) {
            log.info("exception while processing set attributes: ", ex);
         }
         break;
      }
   }
   
   protected boolean handle(String type, byte[] msg) {
      return false;
   }
  
   @Override
   public boolean handle(PlatformMessage msg) {
      try {
         inHandler = true;
         handle(msg.getValue());
      } catch (Exception ex) {
         log.warn("{} failed to process platform message: ", getDriverName(), ex);
      } finally {
         commit();
         inHandler = false;
      }

      return true;
   }

   @Override
   public boolean handle(ProtocolMessage msg) {
      try {
         inHandler = true;
         return handle(msg.getMessageType(), msg.getBuffer());
      } catch (Exception ex) {
         log.warn("{} failed to process protocol message: ", getDriverName(), ex);
      } finally {
         commit();
         inHandler = false;
      }

      return false;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver Messages
   /////////////////////////////////////////////////////////////////////////////
   
   public void emit(MessageBody msg) {
      parent.emit(addr, msg);
   }
   
   public void emit(MessageBody msg, @Nullable UUID person) {
      if (person == null) {
         emit(msg);
      } else {
         emit(msg, Address.platformService(person, "person"));
      }
   }

   public void emit(MessageBody msg, @Nullable Address actor) {
      if (actor == null) {
         parent.emit(addr, msg);
      } else {
         parent.emit(addr, msg, actor);
      }
   }

   public void emit(String attr, Object value) {
      emit(ImmutableMap.of(attr, value));
   }

   public void emit(Map<String,Object> attrs) {
      if (attrs != null && !attrs.isEmpty()) {
         MessageBody message = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.copyOf(attrs));
         parent.emit(addr, message);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Driver State
   /////////////////////////////////////////////////////////////////////////////
   
   private void commit() {
      if (!report.isEmpty()) {
         MessageBody message = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.copyOf(report));
         parent.emit(addr, message);
         report.clear();
      }

      if (!dirty.isEmpty()) {
         ReflexDao.putDriverState(saddr, dirty);
         dirty.clear();
      }
   }
   
   private void persist(String key, Object value, boolean attr) {
      if (inHandler) {
         dirty.put(key, value);
         if (attr) {
            report.put(key, value);
         }
      } else {
         ReflexDao.putDriverState(saddr, key, value);
      }
   }
   
   protected <T> T get(Variable<T> var) {
      return var.get(state);
   }
   
   protected <T> boolean set(Variable<T> var, T val) {
      T old = var.set(state, val);
      if (!Objects.equals(old,val)) {
         persist(var.key, val, var.isAttr);
         return true;
      }

      return false;
   }
   
   protected <T> boolean setWithoutReport(Variable<T> var, T val) {
      T old = var.set(state, val);
      if (!Objects.equals(old,val)) {
         ReflexDao.putDriverState(saddr, var.key, val);
         return true;
      }

      return false;
   }
   
   protected static final class Variable<T> {
      private final Class<T> type;
      private final String key;
      private final T def;
      private final boolean isAttr;

      public Variable(Class<T> type, String key, T def, boolean isAttr) {
         this.type = type;
         this.key = key;
         this.def = def;
         this.isAttr = isAttr;
      }

      public String getKey() {
         return key;
      }

      @SuppressWarnings("unchecked")
      private T get(Map<String,Object> state) {
         T val = (T)state.get(key);
         return (val != null) ? val : def;
      }

      @SuppressWarnings("unchecked")
      private T set(Map<String,Object> state, T updated) {
         T old = (T)state.put(key, updated);
         return (old != null) ? old : def;
      }

      protected boolean isAttribute() {
         return isAttr;
      }
   }

   protected static <T> Variable<T> variable(String driver, Version version, String name, Class<T> type, T def) {
      return mkvar(driver, version, "_hdv:" + name, type, def, false);
   }

   protected static <T> Variable<T> attribute(String driver, Version version, String name, Class<T> type, T def) {
      return mkvar(driver, version, name, type, def, true);
   }

   protected static <T> Variable<T> mkvar(String driver, Version version, String name, Class<T> type, T def, boolean attr) {
      Variable<T> var = new Variable<T>(type, name, def, attr);

      ConcurrentMap<Version,Map<String,Class<?>>> dconvs = conversions.computeIfAbsent(driver, (k) -> new ConcurrentHashMap<>());
      Map<String,Class<?>> convs = dconvs.computeIfAbsent(version, (v) -> new ConcurrentHashMap<>());
      convs.put(var.key, type);

      return var;
   }
}

