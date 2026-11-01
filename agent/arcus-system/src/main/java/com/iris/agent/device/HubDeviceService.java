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
package com.iris.agent.device;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.MessageBody;

public class HubDeviceService {
   private static final ConcurrentMap<String, DeviceProvider> deviceProviders = new ConcurrentHashMap<>();
   private static final Map<String,DeviceProvider> deviceView = Collections.unmodifiableMap(deviceProviders);
   private static final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();

   public static DeviceProvider find(String protocol) {
      DeviceProvider prov = deviceProviders.get(protocol);
      if (prov == null) {
         throw new IllegalStateException("no device information present for protocol '" + protocol + "'");
      }

      return prov;
   }

   public static Map<String,DeviceProvider> devices() {
      return deviceView;
   }

   public static void register(String protocol, DeviceProvider provider) {
      if (deviceProviders.putIfAbsent(protocol,provider) != null) {
         throw new IllegalStateException("attempted to register two device providers for protocol: " + protocol);
      }

      for (Listener listener : listeners) {
         listener.deviceProviderRegistered(protocol);
      }
   }

   public static void addListener(Listener listener) {
      listeners.add(listener);
   }

   public static void removeListener(Listener listener) {
      listeners.remove(listener);
   }

   /**
    * Told when a protocol's devices become available. A radio can register
    * long after startup, once it has finished bootstrapping.
    */
   public static interface Listener {
      void deviceProviderRegistered(String protocol);
   }

   public static interface DeviceInfo {
      String getProtocolAddress();
      @Nullable MessageBody getDeviceInfo(boolean allowBlockingUpdates);
      @Nullable Boolean isOnline();
   }

   public static interface DeviceProvider extends Iterable<DeviceInfo> {
   }
}

