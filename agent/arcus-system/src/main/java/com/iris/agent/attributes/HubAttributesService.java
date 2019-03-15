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
package com.iris.agent.attributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.iris.agent.config.ConfigService;
import com.iris.agent.config.ConversionService;
import com.iris.agent.hal.IrisHal;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.util.IrisUUID;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public final class HubAttributesService {
   private static final Logger log = LoggerFactory.getLogger(HubAttributesService.class);

   private static final Object START_LOCK = new Object();
   private static boolean hasStarted = false;

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private static final Subject<UpdatedAttribute,UpdatedAttribute> subject = new SerializedSubject(PublishSubject.create());
   private static final Map<String, AttributeImpl<?>> attributes = new ConcurrentHashMap<>();

   private static @Nullable Attribute<UUID> account;
   private static @Nullable Attribute<UUID> place;
   private static @Nullable Attribute<UUID> lastReset;
   private static @Nullable Attribute<UUID> lastDeviceAddRemove;

   @Nullable
   private static Attribute<String> lastRestartReason;
   @SuppressWarnings("unused")
   private static final Attribute<Long> lastRestartTime = persisted(Long.class, HubAdvancedCapability.ATTR_LASTRESTARTTIME, 0L);

   public static void start() {
      synchronized (START_LOCK) {
         if (hasStarted) {
            throw new IllegalStateException("hub attributes service already started");
         }

         hasStarted = true;
      }
   }

   public static void shutdown() {
      synchronized (START_LOCK) {
         attributes.clear();
         hasStarted = false;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Commonly accessed hub attributes
   /////////////////////////////////////////////////////////////////////////////

   public static void setAttributeConnections(Attribute<UUID> account, Attribute<UUID> place, Attribute<UUID> lastReset, Attribute<UUID> lastAddRemoveDevice, Attribute<String> lastRestartReason) {
      HubAttributesService.account = account;
      HubAttributesService.place = place;
      HubAttributesService.lastReset = lastReset;
      HubAttributesService.lastDeviceAddRemove = lastAddRemoveDevice;
      HubAttributesService.lastRestartReason = lastRestartReason;
   }

   @Nullable
   public static UUID getAccountId() {
      Attribute<UUID> acc = account;
      return (acc == null) ? null : acc.get();
   }

   @Nullable
   public static UUID getPlaceId() {
      Attribute<UUID> plc = place;
      return (plc == null) ? null : plc.get();
   }

   public static void setLastRestartReason(String reason) {
      Attribute<String> lrr = lastRestartReason;
      if (lrr != null) {
         log.warn("last reset reason: {}", reason);
         lrr.set(reason);

         lastRestartTime.set(System.currentTimeMillis());
      } else {
         log.warn("!!!!!!!!!!!!!!!! could not set last restart reason, attribute not connected");
      }
   }

   public static String getHubId() {
      return IrisHal.getHubId();
   }

   public static void pokeLastDeviceAddRemove() {
      Attribute<UUID> ldar = lastDeviceAddRemove;
      if (ldar != null) {
         ldar.set(IrisUUID.timeUUID());
      } else {
         log.warn("could not poke last device add remove attribute, not currently connected");
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static Map<String, Object> asAttributeMap() {
      return asAttributeMap(false);
   }

   public static Map<String, Object> asAttributeMap(boolean onlyChanged) {
      return asAttributeMap(onlyChanged, false);
   }

   public static Map<String, Object> asAttributeMap(boolean onlyChanged, boolean onlyOnConnected) {
      return asAttributeMap(onlyChanged, onlyOnConnected, false);
   }

   public static Map<String, Object> asAttributeMap(boolean onlyChanged, boolean onlyOnConnected, boolean onlyOnValueChange) {
      Map<String,Object> results = new HashMap<>();
      if (!onlyChanged) {
         safePut(results, HubCapability.ATTR_ID, getHubId());
         safePut(results, HubCapability.ATTR_VENDOR, IrisHal.getVendor());
         safePut(results, HubCapability.ATTR_MODEL, IrisHal.getModel());
         safePut(results, HubAdvancedCapability.ATTR_SERIALNUM, IrisHal.getSerialNumber());
         safePut(results, HubAdvancedCapability.ATTR_HARDWAREVER, IrisHal.getHardwareVersion());
         safePut(results, HubAdvancedCapability.ATTR_HWFLASHSIZE, IrisHal.getHardwareFlashSize());
         safePut(results, HubAdvancedCapability.ATTR_MAC, IrisHal.getMacAddress());
         safePut(results, HubAdvancedCapability.ATTR_MFGINFO, IrisHal.getManufacturingInfo());
         safePut(results, HubAdvancedCapability.ATTR_MFGBATCHNUMBER, IrisHal.getManufacturingBatchNumber());
         safePut(results, HubAdvancedCapability.ATTR_MFGDATE, IrisHal.getManufacturingDate());
         safePut(results, HubAdvancedCapability.ATTR_MFGFACTORYID, IrisHal.getManufacturingFactoryID());
         safePut(results, HubAdvancedCapability.ATTR_OSVER, IrisHal.getOperatingSystemVersion());
         safePut(results, HubAdvancedCapability.ATTR_AGENTVER, IrisHal.getAgentVersion());
         safePut(results, HubAdvancedCapability.ATTR_BOOTLOADERVER, IrisHal.getBootloaderVersion());
      }

      for (Map.Entry<String,AttributeImpl<?>> entry : HubAttributesService.attributes.entrySet()) {
         if (onlyOnConnected && !entry.getValue().isReportedOnConnect()) {
            continue;
         }

         if (onlyOnValueChange && !entry.getValue().isReportedOnValueChange()) {
            continue;
         }

         if (!onlyChanged || entry.getValue().isComputed() || (onlyChanged && entry.getValue().isValueChanged())) {
            if (onlyChanged) {
               entry.getValue().markReported();
            }

            safePut(results, entry.getKey(), entry.getValue().get());
         }
      }

      return results;
   }

   private static void safePut(Map<String,Object> result, @Nullable String key, @Nullable Object value) {
      if (key != null && value != null) {
         result.put(key,value);
      }
   }

   public static Map<String, Object> asAttributeMap(Iterable<String> requestedAttributes) {
      return asAttributeMap(requestedAttributes, false);
   }

   public static Map<String, Object> asAttributeMap(Iterable<String> requestedAttributes, boolean onlyChanged) {
      Map<String,Object> results = new HashMap<>();
      for (String match : requestedAttributes) {
         String cmatch = match + ":";

         for (Map.Entry<String,AttributeImpl<?>> entry : HubAttributesService.attributes.entrySet()) {
            String aname = entry.getKey();
            if (results.containsKey(aname)) {
               continue;
            }

            if (!results.containsKey(aname) && (aname.equals(match) || aname.startsWith(cmatch)) &&
                (!onlyChanged || (onlyChanged && entry.getValue().isValueChanged()))) {
               if (onlyChanged) {
                  entry.getValue().markReported();
               }

               results.put(entry.getKey(), entry.getValue().get());
            }
         }
      }

      return results;
   }
   @SuppressWarnings({ "unchecked" })
   public static boolean updateAttributes(Map<String, Object> attributes) {
      boolean updated = false;
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
         AttributeImpl<Object> attr = (AttributeImpl<Object>)HubAttributesService.attributes.get(entry.getKey());
         if (attr == null) {
            log.warn("ignoring update to unknown attribute: {}", entry.getKey());
            continue;
         }

         Object value = entry.getValue();
         if (value == null || attr.type().isAssignableFrom(value.getClass())) {
            attr.set(value);
            updated = true;
         } else if (value instanceof CharSequence) {
            String svalue = value.toString();
            attr.set(ConversionService.to(attr.type(), svalue));
            updated = true;
         }
      }

      return updated;
   }

   public static Observable<UpdatedAttribute> updates() {
      return subject;
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<Set<T>> persistedSet(Class<T> contained, String name, Set<T> def) {
      synchronized (attributes) {
         Attribute<Set<T>> existing = (Attribute<Set<T>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new PersistedAttribute(Set.class, name, (Set)def);
         attributes.put(name, attr);

         return (Attribute<Set<T>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<List<T>> persistedList(Class<T> contained, String name, List<T> def) {
      synchronized (attributes) {
         Attribute<List<T>> existing = (Attribute<List<T>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new PersistedAttribute(List.class, name, (List)def);
         attributes.put(name, attr);

         return (Attribute<List<T>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <K,V> Attribute<List<Map<K,V>>> persistedListMap(Class<K> containedKey, Class<V> containedValue, String name, List<Map<K,V>> def) {
      synchronized (attributes) {
         Attribute<List<Map<K,V>>> existing = (Attribute<List<Map<K,V>>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new PersistedAttribute(List.class, name, (List)def);
         attributes.put(name, attr);

         return (Attribute<List<Map<K,V>>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<T> persisted(Class<T> type, String name, T def) {
      synchronized (attributes) {
         Attribute<T> existing = (Attribute<T>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         AttributeImpl<?> attr = new PersistedAttribute<T>(type, name, def);
         attributes.put(name, attr);

         return (Attribute<T>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<Set<T>> ephemeralSet(Class<T> contained, String name, Set<T> def) {
      synchronized (attributes) {
         Attribute<Set<T>> existing = (Attribute<Set<T>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new SettableAttribute(Set.class, name, def);
         attributes.put(name, attr);

         return (Attribute<Set<T>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<List<T>> ephemeralList(Class<T> contained, String name, List<T> def) {
      synchronized (attributes) {
         Attribute<List<T>> existing = (Attribute<List<T>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new SettableAttribute(List.class, name, def);
         attributes.put(name, attr);

         return (Attribute<List<T>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<T> ephemeral(Class<T> type, String name, T def) {
      synchronized (attributes) {
         Attribute<T> existing = (Attribute<T>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         AttributeImpl<?> attr = new SettableAttribute<T>(type, name, def);
         attributes.put(name, attr);

         return (Attribute<T>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<Set<T>> computedSet(Class<T> contained, String name, Supplier<Set<T>> compute) {
      synchronized (attributes) {
         Attribute<Set<T>> existing = (Attribute<Set<T>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new ComputedAttribute(Set.class, name, compute);
         attributes.put(name, attr);

         return (Attribute<Set<T>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <K,V> Attribute<List<Map<K,V>>> computedListMap(Class<K> containedKey, Class<V> containedValue, String name, Supplier<List<Map<K,V>>> compute) {
      synchronized (attributes) {
         Attribute<List<Map<K,V>>> existing = (Attribute<List<Map<K,V>>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new ComputedAttribute(Map.class, name, compute);
         attributes.put(name, attr);

         return (Attribute<List<Map<K,V>>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <K,V> Attribute<Map<K,V>> computedMap(Class<K> key, Class<V> val, String name, Supplier<Map<K,V>> compute) {
      synchronized (attributes) {
         Attribute<Map<K,V>> existing = (Attribute<Map<K,V>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new ComputedAttribute(Map.class, name, compute);
         attributes.put(name, attr);

         return (Attribute<Map<K,V>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<List<T>> computedList(Class<T> contained, String name, Supplier<List<T>> compute) {
      synchronized (attributes) {
         Attribute<List<T>> existing = (Attribute<List<T>>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         @SuppressWarnings("rawtypes")
         AttributeImpl<?> attr = new ComputedAttribute(List.class, name, compute);
         attributes.put(name, attr);

         return (Attribute<List<T>>)attr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> Attribute<T> computed(Class<T> type, String name, Supplier<T> compute) {
      synchronized (attributes) {
         Attribute<T> existing = (Attribute<T>)attributes.get(name);
         if (existing != null) {
            return existing;
         }

         AttributeImpl<?> attr = new ComputedAttribute<T>(type, name, compute);
         attributes.put(name, attr);

         return (Attribute<T>)attr;
      }
   }

   public static interface Attribute<T> {
      String name();

      void poke();
      void markReported();
      boolean isValueChanged();

      boolean set(T value);
      boolean compareAndSet(T expect, T update);
      void persist();

      T getAndSet(T value);
      T get();

      boolean isReportedOnValueChange();
      void setReportedOnValueChange(boolean reported);

      boolean isReportedOnConnect();
      void setReportedOnConnect(boolean report);
   }

   private static abstract class AttributeImpl<T> implements Attribute<T> {
      protected abstract Class<T> type();
      private boolean reportedOnValueChange = true;
      private boolean reportedOnConnect = true;

      protected boolean isComputed() {
         return false;
      }

      @Override
      public boolean isReportedOnValueChange() {
         return reportedOnValueChange;
      }

      @Override
      public void setReportedOnValueChange(boolean reported) {
         this.reportedOnValueChange = reported;
      }

      @Override
      public boolean isReportedOnConnect() {
         return reportedOnConnect;
      }

      @Override
      public void setReportedOnConnect(boolean report) {
         this.reportedOnConnect = report;
      }
   }

   private static class ComputedAttribute<T> extends AttributeImpl<T> {
      private final String name;
      private final Class<T> type;

      private final Supplier<T> supplier;
      private @Nullable T lastReported = null;

      private ComputedAttribute(Class<T> type, String name, Supplier<T> supplier) {
         this.name = name;
         this.type = type;
         this.supplier = supplier;
      }

      @Override
      public String name() {
         return name;
      }

      @Override
      public Class<T> type() {
         return type;
      }

      @Override
      public T get() {
         return supplier.get();
      }

      @Override
      public void poke() {
         T value = supplier.get();
         if (!Objects.equals(value,lastReported)) {
            Object old = lastReported;
            lastReported = value;
            subject.onNext(new UpdatedAttribute(this,old));
         }
      }

      @Override
      public void markReported() {
      }

      @Override
      public boolean isValueChanged() {
         return false;
      }

      @Override
      public boolean set(T value) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean compareAndSet(T expect, T update) {
         throw new UnsupportedOperationException();
      }

      @Override
      public T getAndSet(T value) {
         throw new UnsupportedOperationException();
      }

      @Override
      protected boolean isComputed() {
         return true;
      }

      @Override
      public void persist() {
         throw new UnsupportedOperationException();
      }
   }

   private static class SettableAttribute<T> extends AttributeImpl<T> {
      protected final String name;
      protected final Class<T> type;
      protected long lastReportTime;
      protected long lastUpdateTime;

      protected @Nullable T value;

      private SettableAttribute(Class<T> type, String name, T def) {
         this.type = type;
         this.name = name;
         this.value = def;
         this.lastReportTime = Long.MIN_VALUE;
         this.lastUpdateTime = Long.MIN_VALUE;
      }

      @Override
      public String name() {
         return name;
      }

      @Override
      public Class<T> type() {
         return type;
      }

      @Override
      public boolean set(T value) {
         T old = getAndSet(value);
         return (old == null && value != null) ||
                (old != null && !old.equals(value));
      }

      @Override
      public void poke() {
      }

      @Override
      public void markReported() {
         lastReportTime = lastUpdateTime;
      }

      @Override
      public boolean isValueChanged() {
         return lastUpdateTime > lastReportTime;
      }

      @Override
      public T getAndSet(T value) {
         T old;
         synchronized (this) {
            old = this.value;
            this.value = value;
         }

         check(old, value);
         return old;
      }

      @Override
      public boolean compareAndSet(@Nullable T expect, @Nullable T update) {
         boolean updated = false;
         synchronized (this) {
            if (Objects.equals(this.value, expect)) {
               updated = true;
               this.value = update;
            }
         }

         if (updated) {
            check(expect, update);
         }

         return updated;
      }

      @Override
      public T get() {
         return value;
      }

      private void check(@Nullable T old, @Nullable T value) {
         if (!Objects.equals(old,value)) {
            lastUpdateTime = System.currentTimeMillis();
            updated(old, value);
         }
      }

      protected void updated(@Nullable T old, @Nullable T value) {
         subject.onNext(new UpdatedAttribute(this,old));
      }

      @Override
      public void persist() {
         throw new UnsupportedOperationException();
      }
   }

   private static class PersistedAttribute<T> extends SettableAttribute<T> {
      private PersistedAttribute(Class<T> type, String name, T def) {
         super(type, name, def);

         ConfigService.ValueWithTime<T> value = ConfigService.getWithTime(this.name, type, def);
         this.value = value.value;
         this.lastUpdateTime = value.lastUpdateTime;
         this.lastReportTime = value.lastReportTime;
      }

      @Override
      public void markReported() {
         super.markReported();
         ConfigService.put(name, this.value, lastUpdateTime, lastReportTime);
      }

      @Override
      protected void updated(@Nullable T old, @Nullable T value) {
         persist();
         super.updated(old, value);
      }

      @Override
      public void persist() {
         ConfigService.put(name, this.value, lastUpdateTime, lastReportTime);
      }
   }

   public static final class UpdatedAttribute {
      private final Attribute<?> attr;
      private final Object old;

      public UpdatedAttribute(Attribute<?> attr, Object old) {
         this.attr = attr;
         this.old = old;
      }

      public Attribute<?> getAttr() {
         return attr;
      }

      public Object getOld() {
         return old;
      }
   }
}

