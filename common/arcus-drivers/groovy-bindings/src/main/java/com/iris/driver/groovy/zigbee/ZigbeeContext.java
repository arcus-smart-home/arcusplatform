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

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.inject.Singleton;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.zigbee.cluster.alertme.ZigbeeAlertmeClusters;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclClusters;
import com.iris.driver.groovy.zigbee.cluster.zdp.ZigbeeZdpClusters;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.DeviceOtaCapability;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protocol.zigbee.ZigbeeBindEvent;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.IasZone;


@Singleton
public class ZigbeeContext extends GroovyObjectSupport {
   final static byte GROUP_CLIENT = 0x01;
   final static byte GROUP_SERVER = 0x02;
   final static byte GROUP_GENERAL = 0x00;

   private static final Serializer<MessageBody> CONTROL_SERIALIZER = JSON.createSerializer(MessageBody.class);

   private static final short AM_PROFILE_ID = (short)0xC216;
   private static final byte AM_ENDPOINT_ID = (short)0x02;

   private final static String DATA_PROPERTY = "Data";
   private final static String MESSAGE_PROPERTY = "Message";
   private final static String TYPE_ZCL_PROPERTY = "TYPE_ZCL";
   private final static String TYPE_ZDP_PROPERTY = "TYPE_ZDP";
   private final static String ZDP_PROPERTY = "Zdp";
   private final static String GROUP_CLIENT_PROPERTY = "GROUP_CLIENT";
   private final static String GROUP_SERVER_PROPERTY = "GROUP_SERVER";
   private final static String GROUP_GENERAL_PROPERTY = "GROUP_GENERAL";
   private final static String HUB_PROPERTY = "Hub";

   private final static String PROTOCOL_ATTRIBUTES_ENDPOINTS_KEY = "hubzbprofile:endpoints";
   private final static String PROTOCOL_ATTRIBUTES_ENDPOINT_ID_KEY = "hubzbendpoint:id";
   private final static String PROTOCOL_ATTRIBUTES_PROFILE_ID_KEY = "hubzbprofile:id";
   private final static byte TYPE_ZCL = ZigbeeMessage.Zcl.ID;
   private final static byte TYPE_ZDP = ZigbeeMessage.Zdp.ID;
   private final static String CLUSTER_NAME = "cluster";
   private final static String MESSAGE_NAME = "command";
   private final static String BYTE_VALUES_NAME = "data";
   private final static String ENDPOINT_NAME = "endpoint";
   private final static String PROFILE_ID_NAME = "profile";
   private final static String CLUSTERSPECIFIC_NAME = "clusterspecific";
   private final static String FROMSERVER_NAME = "fromserver";
   private final static String DEFAULTRESPONSE_NAME = "defaultresponse";
   private final static String MSP_NAME = "msp";

   private final ZigbeeConfigContext configContext = new ZigbeeConfigContext();
   private final Map<String, Object> properties = new HashMap<>();
   private final Map<Integer, Endpoint> endpoints = new HashMap<>();

   private final Data data = new Data();
   private final Hub hub = new Hub();
   private final Message messageHelper = new Message();
   private final Zdp zdp = new Zdp();

   public ZigbeeContext() {
      properties.put(DATA_PROPERTY, data);
      properties.put(MESSAGE_PROPERTY, messageHelper);
      properties.put(ZDP_PROPERTY, zdp);
      properties.put(TYPE_ZCL_PROPERTY, TYPE_ZCL);
      properties.put(TYPE_ZDP_PROPERTY, TYPE_ZDP);
      properties.put(GROUP_CLIENT_PROPERTY, GROUP_CLIENT);
      properties.put(GROUP_SERVER_PROPERTY, GROUP_SERVER);
      properties.put(GROUP_GENERAL_PROPERTY, GROUP_GENERAL);
      properties.put(HUB_PROPERTY, hub);
   }

   public void processReflexes(DriverBinding binding) {
      configContext.processReflexes(binding);
   }

   public void processConfiguration(DriverBinding binding) {
      configContext.processConfiguration(binding);
   }

   public void call(Closure<?> configClosure) {
      configClosure.setDelegate(configContext);
      configClosure.call();
   }

   public void call(GroovyCapabilityDefinition.ActionAndClosure action) {
      ZigbeeActionContext ctx = new ZigbeeActionContext(action.getClosure().getOwner(), this);
      action.getClosure().setDelegate(ctx);
      action.getClosure().call();
   }

   @Override
   public Object getProperty(String property) {
      Object value = properties.get(property);
      return value != null ? value : super.getProperty(property);
   }

   @Override
   public void setProperty(String property, Object newValue) {
      throw new UnsupportedOperationException("Properties may not be set on the Zigbee context object");
   }

   public void setOfflineTimeout(int seconds) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      ZigbeeMessage.SetOfflineTimeout message = ZigbeeMessage.SetOfflineTimeout.builder().setSeconds(seconds).create();
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   public void updateFirmware(String url, String priority) {
      MessageBody body = DeviceOtaCapability.FirmwareUpdateRequest.builder()
         .withUrl(url)
         .withPriority(priority)
         .build();

      ZigbeeMessage.Control message = ZigbeeMessage.Control.builder()
         .setPayload(CONTROL_SERIALIZER.serialize(body))
         .create();

      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   public void updateFirmwareCancel() {
      MessageBody body = DeviceOtaCapability.FirmwareUpdateCancelRequest.instance();
      ZigbeeMessage.Control message = ZigbeeMessage.Control.builder()
         .setPayload(CONTROL_SERIALIZER.serialize(body))
         .create();

      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   public void iasZoneEnroll(Map<String,Object> config) {
      int ep = 1;
      int pr = ZigbeeNaming.HA_PROFILE_ID;
      int cl = IasZone.CLUSTER_ID;

      if (config.containsKey("endpoint")) {
         ep = ((Number)config.get("endpoint")).intValue();
      }

      if (config.containsKey("profile")) {
         pr = ((Number)config.get("profile")).intValue();
      }

      if (config.containsKey("cluster")) {
         cl = ((Number)config.get("cluster")).intValue();
      }

      ProtocMessage message = ZigbeeMessage.IasZoneEnroll.builder()
         .setEndpoint(ep)
         .setProfile(pr)
         .setCluster(cl)
         .create();

      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   public void sendOrdered(ProtocMessage... messages) {
      sendOrdered(Arrays.asList(messages));
   }

   public void sendDelayed(long time, TimeUnit unit, ProtocMessage... messages) {
      sendDelayed(time, unit, Arrays.asList(messages));
   }

   public void sendOrdered(List<ProtocMessage> messages) {
      try {
         int i = 0;
         ZigbeeMessage.Protocol[] msgs = new ZigbeeMessage.Protocol[messages.size()];
         for (ProtocMessage msg : messages) {
            msgs[i] = ZigbeeMessage.Protocol.builder()
               .setType(msg.getMessageId())
               .setPayload(ByteOrder.LITTLE_ENDIAN, msg)
               .create();
         }

         ZigbeeMessage.Ordered msg = ZigbeeMessage.Ordered.builder()
            .setPayload(msgs)
            .create();

         DeviceDriverContext context = GroovyContextObject.getContext();
         ZigbeeMessageUtil.doSendMessage(context, msg);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public void sendDelayed(long time, TimeUnit unit, List<ProtocMessage> messages) {
      try {
         int i = 0;
         ZigbeeMessage.Protocol[] msgs = new ZigbeeMessage.Protocol[messages.size()];
         for (ProtocMessage msg : messages) {
            msgs[i] = ZigbeeMessage.Protocol.builder()
               .setType(msg.getMessageId())
               .setPayload(ByteOrder.LITTLE_ENDIAN, msg)
               .create();
         }

         ZigbeeMessage.Delay msg = ZigbeeMessage.Delay.builder()
            .setDelay(unit.toNanos(time))
            .setPayload(msgs)
            .create();

         DeviceDriverContext context = GroovyContextObject.getContext();
         ZigbeeMessageUtil.doSendMessage(context, msg);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public void bindAll() {
      MessageBody body = ZigbeeBindEvent.createAllBindings();

      ZigbeeMessage.Control message = ZigbeeMessage.Control.builder()
         .setPayload(CONTROL_SERIALIZER.serialize(body))
         .create();

      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   public void bindEndpoints(Map.Entry<Integer,ZigbeeBindEvent.Binding>... bindings) {
      Map<Integer,List<ZigbeeBindEvent.Binding>> binds = new HashMap<>();
      for (Map.Entry<Integer,ZigbeeBindEvent.Binding> entry : bindings) {
         Integer endpoint = entry.getKey();
         ZigbeeBindEvent.Binding binding = entry.getValue();
         if (binding == null) {
            binds.put(endpoint, new ArrayList<ZigbeeBindEvent.Binding>());
            continue;
         }

         List<ZigbeeBindEvent.Binding> existing = binds.get(endpoint);
         if (existing == null && !binds.containsKey(endpoint)) {
            existing = new ArrayList<>();
            binds.put(endpoint, existing);
         }

         if (existing != null) {
            existing.add(binding);
         }
      }

      MessageBody body = ZigbeeBindEvent.createEndpointBindings(binds);

      ZigbeeMessage.Control message = ZigbeeMessage.Control.builder()
         .setPayload(CONTROL_SERIALIZER.serialize(body))
         .create();

      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   public void bindProfiles(Integer... profiles) {
      MessageBody body = ZigbeeBindEvent.createProfileBindings(Arrays.asList(profiles));

      ZigbeeMessage.Control message = ZigbeeMessage.Control.builder()
         .setPayload(CONTROL_SERIALIZER.serialize(body))
         .create();

      DeviceDriverContext context = GroovyContextObject.getContext();
      context.sendToDevice(ZigbeeProtocol.INSTANCE, ZigbeeProtocol.packageMessage(message), -1);
   }

   public Endpoint endpoint(byte endpoint) {
      return getEndpoint(endpoint, null);
   }

   public Endpoint getAlertme() {
      return alertme(AM_ENDPOINT_ID);
   }

   public Endpoint alertme(byte endpoint) {
      return getEndpoint(endpoint, AM_PROFILE_ID);
   }

   public Endpoint endpoint(byte endpoint, short profileId) {
      return getEndpoint(endpoint, profileId);
   }

   public void sendZdpCommand(int messageId, byte... bytes) {
      ZigbeeMessageUtil.doSendZdpZigbeeCommand(messageId, bytes);
   }

   public void send(Map<String, Object> args) {
      Object mspObj = args.get(MSP_NAME);
      Object clusterObj = args.get(CLUSTER_NAME);
      Object messageIdObj = args.get(MESSAGE_NAME);
      Object endpointObj = args.get(ENDPOINT_NAME);
      Object clusterSpecificObj = args.get(CLUSTERSPECIFIC_NAME);
      Object fromServerObj = args.get(FROMSERVER_NAME);
      if (clusterObj == null || endpointObj == null || messageIdObj == null || clusterSpecificObj == null) {
         throw new IllegalArgumentException("Must specify '" + CLUSTER_NAME + "', '" + ENDPOINT_NAME + "', '" + CLUSTERSPECIFIC_NAME + "', and '" + MESSAGE_NAME + "'");
      }
      int mspCode = mspObj == null ? 0 : convertToInt(MSP_NAME, mspObj);
      int clusterId = convertToInt(CLUSTER_NAME, clusterObj);
      Object byteObj = args.get(BYTE_VALUES_NAME);
      byte[] bytes = byteObj != null ? (byte[])byteObj : new byte[0];
      int messageId = convertToInt(MESSAGE_NAME, messageIdObj);
      int endpoint = convertToInt(ENDPOINT_NAME, endpointObj);
      boolean clusterSpecific = convertToBool(CLUSTERSPECIFIC_NAME, clusterSpecificObj);
      boolean fromServer = (fromServerObj == null) ? false : convertToBool(FROMSERVER_NAME, fromServerObj);

      Object profileObj = args.get(PROFILE_ID_NAME);
      int profile = profileObj == null ? getProfileIdForEndpoint(endpoint) : convertToInt(PROFILE_ID_NAME, profileObj);

      Object sendDefaultResponseObj = args.get(DEFAULTRESPONSE_NAME);
      boolean sendDefaultResponse = sendDefaultResponseObj == null ? false : convertToBool(DEFAULTRESPONSE_NAME, sendDefaultResponseObj);

      if (mspObj != null) {
         ZigbeeMessageUtil.doSendMspZclZigbeeCommand(mspCode, clusterId, messageId, profile, endpoint, sendDefaultResponse, clusterSpecific, fromServer, bytes);
      } else {
         ZigbeeMessageUtil.doSendZclZigbeeCommand(clusterId, messageId, profile, endpoint, sendDefaultResponse, clusterSpecific, fromServer, bytes);
      }
   }

   @SuppressWarnings("unchecked")
   private static short getProfileIdForEndpoint(int endpointId) {
      //TODO: Cache a map of endpoints to profileIds in the DeviceDriverContext
      byte endpointValue = (byte)endpointId;
      DeviceDriverContext context = GroovyContextObject.getContext();
      AttributeMap attrs = context.getProtocolAttributes();
      if (attrs != null) {
         Set<Object> profiles = attrs.get(AttributeKey.createSetOf(ZigbeeProtocol.ATTR_PROFILES, Object.class));
         if (profiles != null) {
            for (Object profileObj : profiles) {
               Map<String, Object> profile = (Map<String, Object>)profileObj;
               Object endpointsObj = profile.get(PROTOCOL_ATTRIBUTES_ENDPOINTS_KEY);
               if (endpointsObj instanceof List) {
                  List<Object> endpoints = (List<Object>)endpointsObj;
                  for (Object endpointObj : endpoints) {
                     Map<String, Object> endpoint = (Map<String, Object>)endpointObj;
                     Object endpointIdObj = endpoint.get(PROTOCOL_ATTRIBUTES_ENDPOINT_ID_KEY);
                     if (endpointIdObj instanceof Number) {
                        if (endpointValue == ((Number)endpointIdObj).byteValue()) {
                           Object profileIdObj = profile.get(PROTOCOL_ATTRIBUTES_PROFILE_ID_KEY);
                           if (profileIdObj instanceof Number) {
                              return ((Number)profileIdObj).shortValue();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      throw new IllegalArgumentException("The Zigbee profile id cannot be determined for the given endpoint: " + endpointId);
   }

   //
   // Zdp Commands
   //
   public static class Zdp extends GroovyObjectSupport {
      private final Map<String, ClusterBinding> clusters;

      Zdp() {
         clusters = Collections.unmodifiableMap(ZigbeeContext.buildZdpBindings());
      }

      @Override
      public Object getProperty(String property) {
         ClusterBinding cluster = clusters.get(property);
         if (cluster != null) {
            return cluster;
         }
         return super.getProperty(property);
      }

      @Override
      public void setProperty(String property, Object newValue) {
         throw new IllegalArgumentException("Properties may not be set on an endpoint object");
      }

      public void send(short messageId, byte... bytes) {
         ZigbeeMessageUtil.doSendZdpZigbeeCommand(messageId, bytes);
      }
   }

   //
   // Zcl Endpoints
   //
   public static class Endpoint extends GroovyObjectSupport {
      private final byte endpoint;
      private final Map<String, ClusterBinding> clusters;
      private final Short profileId;

      Endpoint(byte endpoint, Short profileId) {
         this.endpoint = endpoint;
         this.profileId = profileId;
         clusters = Collections.unmodifiableMap(ZigbeeContext.buildClusterBindings(this));
      }

      @Override
      public Object getProperty(String property) {
         ClusterBinding cluster = clusters.get(property);
         if (cluster != null) {
            return cluster;
         }
         return super.getProperty(property);
      }

      @Override
      public void setProperty(String property, Object newValue) {
         throw new IllegalArgumentException("Properties may not be set on an endpoint object");
      }

      public void send(byte flags, short clusterId, byte messageId) {
         ZigbeeMessageUtil.doSendZclZigbeeCommand(clusterId, messageId, getProfileId(), endpoint, flags, new byte[0]);
      }

      public void send(byte flags, short clusterId, byte messageId, byte... bytes) {
         ZigbeeMessageUtil.doSendZclZigbeeCommand(clusterId, messageId, getProfileId(), endpoint, flags,  bytes);
      }

      public void send(boolean clusterspecific, boolean fromServer, short clusterId, byte messageId) {
         ZigbeeMessageUtil.doSendZclZigbeeCommand(clusterId, messageId, getProfileId(), endpoint, false, clusterspecific, fromServer, new byte[0]);
      }

      public void send(boolean clusterspecific, boolean fromServer, short clusterId, byte messageId, byte... bytes) {
         ZigbeeMessageUtil.doSendZclZigbeeCommand(clusterId, messageId, getProfileId(), endpoint, false, clusterspecific, fromServer, bytes);
      }

      public void send(boolean clusterspecific, boolean fromServer, boolean sendDefaultResponse, short clusterId, byte messageId) {
         ZigbeeMessageUtil.doSendZclZigbeeCommand(clusterId, messageId, getProfileId(), endpoint, sendDefaultResponse, clusterspecific, fromServer, new byte[0]);
      }

      public void send(boolean clusterspecific, boolean fromServer, boolean sendDefaultResponse, short clusterId, byte messageId, byte... bytes) {
         ZigbeeMessageUtil.doSendZclZigbeeCommand(clusterId, messageId, getProfileId(), endpoint, sendDefaultResponse, clusterspecific, fromServer, bytes);
      }

      public void sendmsp(int code, byte flags, short clusterId, byte messageId) {
         ZigbeeMessageUtil.doSendMspZclZigbeeCommand(code, clusterId, messageId, getProfileId(), endpoint, flags, new byte[0]);
      }

      public void sendmsp(int code, byte flags, short clusterId, byte messageId, byte... bytes) {
         ZigbeeMessageUtil.doSendMspZclZigbeeCommand(code, clusterId, messageId, getProfileId(), endpoint, flags,  bytes);
      }

      public void sendmsp(int code, boolean clusterspecific, boolean fromServer, short clusterId, byte messageId) {
         ZigbeeMessageUtil.doSendMspZclZigbeeCommand(code, clusterId, messageId, getProfileId(), endpoint, false, clusterspecific, fromServer, new byte[0]);
      }

      public void sendmsp(int code, boolean clusterspecific, boolean fromServer, short clusterId, byte messageId, byte... bytes) {
         ZigbeeMessageUtil.doSendMspZclZigbeeCommand(code, clusterId, messageId, getProfileId(), endpoint, false, clusterspecific, fromServer, bytes);
      }

      public void sendmsp(int code, boolean clusterspecific, boolean fromServer, boolean sendDefaultResponse, short clusterId, byte messageId) {
         ZigbeeMessageUtil.doSendMspZclZigbeeCommand(code, clusterId, messageId, getProfileId(), endpoint, sendDefaultResponse, clusterspecific, fromServer, new byte[0]);
      }

      public void sendmsp(int code, boolean clusterspecific, boolean fromServer, boolean sendDefaultResponse, short clusterId, byte messageId, byte... bytes) {
         ZigbeeMessageUtil.doSendMspZclZigbeeCommand(code, clusterId, messageId, getProfileId(), endpoint, sendDefaultResponse, clusterspecific, fromServer, bytes);
      }

      public short getProfileId() {
         return profileId == null ? getProfileIdForEndpoint(endpoint) : profileId;
      }

      public byte getEndpoint() {
         return endpoint;
      }

      public Map.Entry<Integer,ZigbeeBindEvent.Binding> bindAll() {
         return new AbstractMap.SimpleImmutableEntry<>(endpoint & 0xFF, null);
      }

      public Map.Entry<Integer,ZigbeeBindEvent.Binding> bindClientCluster(int cluster) {
         return new AbstractMap.SimpleImmutableEntry<>(endpoint & 0xFF, new ZigbeeBindEvent.Binding(cluster,true));
      }

      public Map.Entry<Integer,ZigbeeBindEvent.Binding> bindServerCluster(int cluster) {
         return new AbstractMap.SimpleImmutableEntry<>(endpoint & 0xFF, new ZigbeeBindEvent.Binding(cluster,false));
      }
   }

   private Endpoint getEndpoint(byte endpointId, Short profileId) {
      Endpoint endpoint = endpoints.get(makeEndpointKey(endpointId, profileId));
      if (endpoint == null) {
         endpoint = new Endpoint(endpointId, profileId);
         endpoints.put(makeEndpointKey(endpointId, profileId), endpoint);
      }
      return endpoint;
   }

   private int makeEndpointKey(byte endpoint, Short profileId) {
      return (profileId != null ? 0x10000000 | (0x0000ffff & profileId) << 8 : 0x00000000) | (0x000000ff & endpoint);
   }

   private static Map<String, ClusterBinding> buildClusterBindings(ZigbeeContext.Endpoint endpoint) {
      Map<String, ClusterBinding> map = new HashMap<>();

      if (endpoint.profileId != null && endpoint.profileId == AM_PROFILE_ID) {
         for (ClusterBinding.Factory factory : ZigbeeAlertmeClusters.factories) {
            ClusterBinding binding = factory.create(endpoint);
            map.put(binding.getName(), binding);
         }
      } else {
         for (ClusterBinding.Factory factory : ZigbeeZclClusters.factories) {
            ClusterBinding binding = factory.create(endpoint);
            map.put(binding.getName(), binding);
         }
      }

      return map;
   }

   private static Map<String, ClusterBinding> buildZdpBindings() {
      Map<String, ClusterBinding> map = new HashMap<>();
      for (ClusterBinding.Factory factory : ZigbeeZdpClusters.factories) {
         ClusterBinding binding = factory.create(null);
         map.put(binding.getName(), binding);
      }
      return map;
   }

   private static int convertToInt(String fieldName, Object obj) {
      if (obj instanceof Number) {
         return ((Number)obj).intValue();
      }
      else if (obj instanceof String) {
         return Integer.parseInt((String)obj);
      }
      throw new IllegalArgumentException("Expected numeric value for '" + fieldName + "'.");
   }

   private static boolean convertToBool(String fieldName, Object obj) {
      if (obj instanceof Boolean) {
         return ((Boolean)obj).booleanValue();
      }
      else if (obj instanceof String) {
         return ((String)obj).equalsIgnoreCase("true");
      }
      throw new IllegalArgumentException("Expected boolean value for '" + fieldName + "'.");
   }

}

