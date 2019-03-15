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
package com.iris.driver.groovy.zigbee;

import java.util.HashMap;
import java.util.Map;

import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.zigbee.cluster.alertme.ZigbeeAlertmeClusters;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclClusters;
import com.iris.driver.groovy.zigbee.cluster.zdp.ZigbeeZdpClusters;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;

@SuppressWarnings("serial")
public class OnZigbeeClosure extends Closure<Object> {
   private final EnvironmentBinding binding;
   private final Map<String, Object> properties = new HashMap<>();

   public OnZigbeeClosure(EnvironmentBinding binding) {
      super(binding);
      this.binding = binding;
      setResolveStrategy(TO_SELF);
      properties.put("Zcl", new ZclMessageType(binding));
      properties.put("Alertme", new AmeMessageType(binding));
      properties.put("Zdp", new ZdpMessageType(binding));
   }

   protected void doCall(Byte type, String clusterName, String commandName, Closure<?> closure) {
      if (type.byteValue() == (byte)ZigbeeMessage.Zcl.ID) {
         ClusterDescriptor cluster = ZigbeeZclClusters.descriptorsByName.get(clusterName.toLowerCase());
         MessageDescriptor message = cluster.getMessageByName(commandName.toLowerCase());
         addHandler(type, cluster.getId(), message.getIdAsByte(), message.getGroup(), closure);
         return;
      }
      throw new IllegalArgumentException("Invalid message type for this matcher: " + type);
   }

   protected void doCall(Byte type, String clusterName, Closure<?> closure) {
      if (type.byteValue() == (byte)ZigbeeMessage.Zcl.ID) {
         ClusterDescriptor cluster = ZigbeeZclClusters.descriptorsByName.get(clusterName.toLowerCase());
         addHandler(type, cluster.getId(), null, null, closure);
         return;
      }
      else if (type.byteValue() == (byte)ZigbeeMessage.Zdp.ID) {
         MessageDescriptor msg = getZdpMessageDescriptorByName(clusterName.toLowerCase());
         addHandler(type, msg.getIdAsShort(), null, null, closure);
         return;
      }
      throw new IllegalArgumentException("Unknown message type: " + type);
   }

   protected void doCall(Byte type, Short clusterId, Byte commandId, Byte group, Closure<?> closure) {
      addHandler(type, clusterId, commandId, group, closure);
   }

   protected void doCall(Byte type, Short clusterOrMessageId, Closure<?> closure) {
      addHandler(type, clusterOrMessageId, null, null, closure);
   }

   protected void doCall(Byte type, Closure<?> closure) {
      addHandler(type, null, null, null, closure);
   }

   protected void doCall(Closure<?> closure) {
      addHandler(null, null, null, null, closure);
   }

   protected void addHandler(Byte messageType, Short clusterOrMessageId, Byte zclMessageId, Byte group, Closure<?> closure) {
      ZigbeeProtocolEventMatcher matcher = new ZigbeeProtocolEventMatcher();
      matcher.setProtocolName(ZigbeeProtocol.NAMESPACE);
      matcher.setMessageType(messageType);
      matcher.setClusterOrMessageId(clusterOrMessageId);
      matcher.setZclMessageId(zclMessageId);
      matcher.setGroup(group);
      matcher.setHandler(DriverBinding.wrapAsHandler(closure));
      binding.getBuilder().addEventMatcher(matcher);
   }

   @Override
   public Object getProperty(String property) {
      Object o = properties.get(property);
      if(o != null) {
         return o;
      }
      return super.getProperty(property);
   }

   @Override
   public String toString() {
      return "onZigbeeZclMessage([clusterId], [commandId], [clusterSpecific]) { <function> }";
   }

   private MessageDescriptor getZdpMessageDescriptorByName(String name) {
      for (ClusterDescriptor cluster : ZigbeeZdpClusters.descriptorsByName.values()) {
         MessageDescriptor msg = cluster.getMessageByName(name);
         if (msg != null) {
            return msg;
         }
      }
      throw new IllegalArgumentException("Unrecognized ZDP command: " + name);
   }

   private class ZdpMessageType extends GroovyObjectSupport {
      private final Map<String, Object> properties;

      public ZdpMessageType(EnvironmentBinding binding) {
         properties = new HashMap<String, Object>();
         for (ClusterDescriptor cluster : ZigbeeZdpClusters.descriptorsByName.values()) {
            properties.put(cluster.getName().toLowerCase(), new OnZdpClusterClosure(cluster, binding));
         }
      }

      @Override
      public Object getProperty(String property) {
         Object o = properties.get(property);
         if(o != null) {
            return o;
         }
         return super.getProperty(property);
      }

      @Override
      public void setProperty(String property, Object newValue) {
         throw new UnsupportedOperationException("Message type objects cannot have properties set");
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         Object o = properties.get(name);
         if(o != null && o instanceof Closure<?>) {
            return ((Closure<?>) o).call((Object[]) args);
         }
         return super.invokeMethod(name, args);
      }
   }

   private class ZclMessageType extends GroovyObjectSupport {
      private final Map<String, Object> properties;

      public ZclMessageType(EnvironmentBinding binding) {
         properties = new HashMap<String, Object>();
         // Process cluster bindings
         for (ClusterDescriptor cluster : ZigbeeZclClusters.descriptorsByName.values()) {
            properties.put(cluster.getName().toLowerCase(), new OnZigbeeClusterClosure(cluster, binding));
         }
      }

      @Override
      public Object getProperty(String property) {
         Object o = properties.get(property);
         if(o != null) {
            return o;
         }
         return super.getProperty(property);
      }

      @Override
      public void setProperty(String property, Object newValue) {
         throw new UnsupportedOperationException("Message type objects cannot have properties set");
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         Object o = properties.get(name);
         if(o != null && o instanceof Closure<?>) {
            return ((Closure<?>) o).call((Object[]) args);
         }
         return super.invokeMethod(name, args);
      }

   }

   private class OnZdpClusterClosure extends Closure<Object> {
      private final ClusterDescriptor cluster;

      public OnZdpClusterClosure(ClusterDescriptor cluster, EnvironmentBinding binding) {
         super(binding);
         setResolveStrategy(TO_SELF);
         this.cluster = cluster;
      }

      @SuppressWarnings("unused")
      protected void doCall(String commandName, Closure<?> closure) {
         MessageDescriptor msg = cluster.getMessageByName(commandName.toLowerCase());
         addHandler((byte)ZigbeeMessage.Zdp.ID, msg.getIdAsShort(), null, null, closure);
      }

      @SuppressWarnings("unused")
      protected void doCall(Short commandId, Closure<?> closure) {
         addHandler((byte)ZigbeeMessage.Zdp.ID, commandId, null, null, closure);
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         MessageDescriptor message = cluster.getMessageByName(name.toLowerCase());
         Object[] arguments = (Object[])args;
         if (arguments.length == 1 && arguments[0] instanceof Closure) {
            addHandler((byte)ZigbeeMessage.Zdp.ID, message.getIdAsShort(), null, null, (Closure<?>)arguments[0]);
            return null;
         }
         else {
            throw new MissingMethodException(name, getClass(), arguments);
         }
      }
   }

   private class AmeMessageType extends GroovyObjectSupport {
      private final Map<String, Object> properties;

      public AmeMessageType(EnvironmentBinding binding) {
         properties = new HashMap<String, Object>();
         // Process cluster bindings
         for (ClusterDescriptor cluster : ZigbeeAlertmeClusters.descriptorsByName.values()) {
            properties.put(cluster.getName().toLowerCase(), new OnAlertmeClusterClosure(cluster, binding));
         }
      }

      @Override
      public Object getProperty(String property) {
         Object o = properties.get(property);
         if(o != null) {
            return o;
         }
         return super.getProperty(property);
      }

      @Override
      public void setProperty(String property, Object newValue) {
         throw new UnsupportedOperationException("Message type objects cannot have properties set");
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         Object o = properties.get(name);
         if(o != null && o instanceof Closure<?>) {
            return ((Closure<?>) o).call((Object[]) args);
         }
         return super.invokeMethod(name, args);
      }

   }

   private class OnZigbeeClusterClosure extends Closure<Object> {
      private final ClusterDescriptor cluster;

      public OnZigbeeClusterClosure(ClusterDescriptor cluster, EnvironmentBinding binding) {
         super(binding);
         setResolveStrategy(TO_SELF);
         this.cluster = cluster;
      }

      @SuppressWarnings("unused")
      protected void doCall(Closure<?> closure) {
         addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), null, null, closure);
      }

      @SuppressWarnings("unused")
      protected void doCall(String commandName, Closure<?> closure) {
         MessageDescriptor message = cluster.getMessageByName(commandName.toLowerCase());
         addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), message.getIdAsByte(), message.getGroup(), closure);
      }

      @SuppressWarnings("unused")
      protected void doCall(Byte commandId, Byte group, Closure<?> closure) {
         addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), commandId, group, closure);
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         MessageDescriptor message = cluster.getMessageByName(name.toLowerCase());
         Object[] arguments = (Object[])args;
         if (arguments.length == 1 && arguments[0] instanceof Closure) {
            addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), message.getIdAsByte(), message.getGroup(), (Closure<?>)arguments[0]);
            return null;
         }
         else {
            throw new MissingMethodException(name, getClass(), arguments);
         }
      }
   }

   private class OnAlertmeClusterClosure extends Closure<Object> {
      private final ClusterDescriptor cluster;

      public OnAlertmeClusterClosure(ClusterDescriptor cluster, EnvironmentBinding binding) {
         super(binding);
         setResolveStrategy(TO_SELF);
         this.cluster = cluster;
      }

      @SuppressWarnings("unused")
      protected void doCall(Closure<?> closure) {
         addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), null, null, closure);
      }

      @SuppressWarnings("unused")
      protected void doCall(String commandName, Closure<?> closure) {
         MessageDescriptor message = cluster.getMessageByName(commandName.toLowerCase());
         addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), message.getIdAsByte(), message.getGroup(), closure);
      }

      @SuppressWarnings("unused")
      protected void doCall(Byte commandId, Byte group, Closure<?> closure) {
         addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), commandId, group, closure);
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         MessageDescriptor message = cluster.getMessageByName(name.toLowerCase());
         Object[] arguments = (Object[])args;
         if (arguments.length == 1 && arguments[0] instanceof Closure) {
            addHandler((byte)ZigbeeMessage.Zcl.ID, cluster.getId(), message.getIdAsByte(), message.getGroup(), (Closure<?>)arguments[0]);
            return null;
         }
         else {
            throw new MissingMethodException(name, getClass(), arguments);
         }
      }
   }
}

