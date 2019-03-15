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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.config.DriverConfigurationStateMachine;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZclDataUtil;
import com.iris.protocol.zigbee.ZigbeeBindEvent;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public class ZigbeeConfigurationContext extends GroovyObjectSupport {
   private static final Logger log = LoggerFactory.getLogger(ZigbeeConfigurationContext.class);
   private static final Serializer<MessageBody> CONTROL_SERIALIZER = JSON.createSerializer(MessageBody.class);
   private static final Deserializer<ZigbeeMessage.Protocol> DESERIALIZER = ZigbeeProtocol.INSTANCE.createDeserializer();

   private final DriverConfigurationStateMachine.Builder builder;

   public ZigbeeConfigurationContext() {
      this.builder = DriverConfigurationStateMachine.builder();
   }

   public DriverConfigurationStateMachine getConfigurationStateMachine() {
      return builder.build();
   }

   @Override
   public Object getProperty(String name) {
      if (name == null) {
         return super.getProperty(name);
      }

      Object prot = ZigbeeConfigProtocol.getProtocolProperty(name);
      if (prot != null) {
         return prot;
      }

      return super.getProperty(name);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Bind Configuration State Machines
   /////////////////////////////////////////////////////////////////////////////
   
   public void bind(Map<String,Object> config) {
      String name = (String)config.get("name");
      if (name == null) {
         GroovyValidator.error("zigbee bind configuration must define 'name'");
      }

      Integer endpoint = null;
      Integer cluster = null;
      Boolean server = null;

      endpoint = (Integer)config.get("endpoint");
      server = (Boolean)config.get("server");

      Object cls = config.get("cluster");
      if (cls instanceof Number) {
         cluster = ((Number)cls).intValue();
      } else if (cls instanceof ClusterBinding) {
         ClusterBinding cb = (ClusterBinding)cls;
         cluster = cb.getId() & 0xFFFF;
         if (endpoint == null) {
            endpoint = cb.getEndpoint();
         }
      } else if (cls != null) {
         GroovyValidator.error("unknown type for zigbee bind 'cluster': " + cls);
      }

      if (endpoint == null) {
         GroovyValidator.error("zigbee bind must define 'endpoint'");
      }

      if (cluster == null) {
         GroovyValidator.error("zigbee bind must define 'cluster'");
      }

      if (server == null) {
         GroovyValidator.error("zigbee bind must define 'server'");
      }

      if (endpoint == null || cluster == null || server == null) {
         return;
      }

      builder.step(new ZigbeeBindConfigurationState(name, endpoint, cluster, server));
   }
   
   private static final class ZigbeeBindConfigurationState extends DriverConfigurationStateMachine.AbstractState {
      private final int endpoint;
      private final int cluster;
      private final boolean server;

      private ZigbeeBindConfigurationState(String name, int endpoint, int cluster, boolean server) {
         super(name);
         this.endpoint = endpoint;
         this.cluster = cluster;
         this.server = server;
      }

      @Override
      protected void start() {
         sendBindRequest();
         finish();
      }

      @Override
      protected void check(ProtocolMessage msg) {
         // TODO: The hub currently swallows most bind responses
         
         /*
         try {
            if (!ZigbeeProtocol.NAMESPACE.equals(msg.getMessageType())) {
               return;
            }

            ZigbeeMessage.Protocol zmsg = msg.getValue(ZigbeeProtocol.INSTANCE);
            if (zmsg.getType() != ZigbeeMessage.Zdp.ID) {
               return;
            }

            ZigbeeMessage.Zdp zdp = ZigbeeMessage.Zdp.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zmsg.getPayload());
            if (zdp.getZdpMessageId() != com.iris.protocol.zigbee.zdp.Bind.ZdpBindRsp.ID) {
               return;
            }

            com.iris.protocol.zigbee.zdp.Bind.ZdpBindRsp rsp = com.iris.protocol.zigbee.zdp.Bind.ZdpBindRsp.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zdp.getPayload());

            if (rsp.getStatus() == 0) {
               finish();
            }
         } catch (Exception ex) {
            log.warn("failed to check protocol message for binding completion: ", ex);
         }
         */
      }

      private void sendBindRequest() {
         ZigbeeBindEvent.Binding binding = new ZigbeeBindEvent.Binding(cluster, !server);
         List<ZigbeeBindEvent.Binding> bindings = ImmutableList.of(binding);

         MessageBody body = ZigbeeBindEvent.createEndpointBindings(ImmutableMap.of(endpoint,bindings));
         ZigbeeMessage.Protocol msg = ZigbeeProtocol.packageMessage(
            ZigbeeMessage.Control.builder()
               .setPayload(CONTROL_SERIALIZER.serialize(body))
               .create()
         );

         sendToDevice(ZigbeeProtocol.INSTANCE, msg);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Configure Reporting State Machines
   /////////////////////////////////////////////////////////////////////////////
   
   public void report(Map<String,Object> config) {
      try {
         String name = (String)config.get("name");
         if (name == null) {
            GroovyValidator.error("zigbee bind configuration must define 'name'");
         }

         int min = 0; // no minimum by default
         if (config.containsKey("min")) {
            min = ((Number)config.get("min")).intValue();
         }

         int max = 0xFFFE; // longest time between reports by default
         if (config.containsKey("max")) {
            max = ((Number)config.get("max")).intValue();
         }

         int attr = -1;
         if (config.containsKey("attr")) {
            attr = ((Number)config.get("attr")).intValue();
         } else {
            GroovyValidator.error("zigbee configure reporting configuration must define 'attr'");
         }

         Integer type = null;
         if (config.containsKey("type")) {
            type = ((Number)config.get("type")).intValue();
         }

         byte[] rchange = null;
         Object change = config.get("change");
         if (change instanceof ZclData) {
            if (type != null) {
               GroovyValidator.error("zigbee configure reporting configuration should not define 'type' when defining 'change'");
            }

            ZclData data = (ZclData)change;
            type = data.getDataType();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            ZclDataUtil.encode(dos, (byte)data.getDataType(), data.getDataValue());
            IOUtils.closeQuietly(dos);
            IOUtils.closeQuietly(baos);

            rchange = baos.toByteArray();
         } else if (change != null) {
            GroovyValidator.error("zigbee report should define 'change' as a zcl data type");
         }

         if (type == null) {
            type = 0;
            GroovyValidator.error("zigbee configure reporting configuration must define 'type'");
         }

         int pro = ZigbeeNaming.HA_PROFILE_ID;
         Integer cls = null;
         Integer end = null;

         if (config.containsKey("endpoint")) {
            end = ((Number)config.get("endpoint")).intValue();
         }

         if (config.containsKey("profile")) {
            pro = ((Number)config.get("profile")).intValue();
         }

         if (config.containsKey("cluster")) {
            Object ocls = config.get("cluster");
            if (ocls instanceof Number) {
               cls = ((Number)ocls).intValue();
            } else if (ocls instanceof ClusterBinding) {
               ClusterBinding cb = (ClusterBinding)ocls;
               cls = cb.getId() & 0xFFFF;
               if (end == null) {
                  end = cb.getEndpoint();
               }
            } else if (ocls != null) {
               GroovyValidator.error("unknown data type for zigbee configure reporting configuration 'cluster': " + ocls);
            }
         } else {
            GroovyValidator.error("zigbee configure reporting configuration must define 'cluster'");
         }

         if (cls == null) {
            GroovyValidator.error("zigbee configure reporting configuration must define 'cluster' the same for all reports included in a single message");
         }

         if (end == null) {
            GroovyValidator.error("zigbee configure reporting configuration must define 'endpoint' the same for all reports included in a single message");
         }

         if (cls == null || end == null) {
            return;
         }

         com.iris.protocol.zigbee.zcl.General.ZclAttributeRecord[] rrecs = 
            new com.iris.protocol.zigbee.zcl.General.ZclAttributeRecord[] {
            com.iris.protocol.zigbee.zcl.General.ZclAttributeRecord.builder()
               .setDirection(0)
               .setAttributeIdentifier(attr)
               .create()
         };

         com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord[] recs = 
            new com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord[1];

         if (rchange != null) {
            recs[0] = com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord.builder()
               .setDirection(0)
               .setAttributeIdentifier(attr)
               .setAttributeDataType(type)
               .setReportableChange(rchange)
               .setMinimumReportingInterval(min)
               .setMaximumReportingInterval(max)
               .create();
         } else {
            recs[0] = com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord.builder()
               .setDirection(0)
               .setAttributeIdentifier(attr)
               .setAttributeDataType(type)
               .setMinimumReportingInterval(min)
               .setMaximumReportingInterval(max)
               .create();
         }

         com.iris.protocol.zigbee.zcl.General.ZclConfigureReporting configureReporting = 
            com.iris.protocol.zigbee.zcl.General.ZclConfigureReporting.builder()
            .setAttributes(recs)
            .create();

         com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfiguration readReporting = 
            com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfiguration.builder()
            .setAttributes(rrecs)
            .create();

         builder.step(new ZigbeeConfigureReportingConfigurationState(name, end, pro, cls, readReporting, configureReporting));
      } catch (Exception ex) {
         GroovyValidator.error("could not process zigbee configure reporting configuration", ex);
      }
   }
   
   private static final class ZigbeeConfigureReportingConfigurationState extends DriverConfigurationStateMachine.AbstractState {
      private static enum State { READ, CONFIGURE }

      private final int endpoint;
      private final int profile;
      private final int cluster;
      private final com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfiguration readReporting;
      private final com.iris.protocol.zigbee.zcl.General.ZclConfigureReporting configureReporting;

      private ZigbeeConfigureReportingConfigurationState(
         String name,
         int endpoint,
         int profile,
         int cluster,
         com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfiguration readReporting,
         com.iris.protocol.zigbee.zcl.General.ZclConfigureReporting configureReporting
      ) {
         super(name);

         this.endpoint = endpoint;
         this.profile = profile;
         this.cluster = cluster;
         this.readReporting = readReporting;
         this.configureReporting = configureReporting;
      }

      private State getState() {
         try {
            String stateName = (String)getVariable(getName() + "-state", State.READ.name());
            return State.valueOf(stateName);
         } catch (Exception ex) {
            return State.READ;
         }
      }

      private void setState(State state) {
         setVariable(getName() + "-state", state.name());
      }

      @Override
      protected void start() {
         final State state = getState();

         try {
            switch (state) {
            case READ:
               sendReadReporting();
               break;
            case CONFIGURE:
               sendConfigureReporting();
               sendReadReporting();
               break;
            }

            scheduleRetry();
         } catch (Exception ex) {
            log.warn("failed to send read reporting configuration to device: ", ex);
            finish();
         }
      }

      @Override
      protected void check(ProtocolMessage msg) {
         final State state = getState();

         try {
            if (!ZigbeeProtocol.NAMESPACE.equals(msg.getMessageType())) {
               return;
            }

            ZigbeeMessage.Protocol zmsg = msg.getValue(ZigbeeProtocol.INSTANCE);
            if (zmsg.getType() != ZigbeeMessage.Zcl.ID) {
               return;
            }

            ZigbeeMessage.Zcl zcl = ZigbeeMessage.Zcl.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zmsg.getPayload());
            switch (zcl.getZclMessageId()) {
               case com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfigurationResponse.ID: {
                  com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfigurationResponse rsp = com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfigurationResponse.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, zcl.getPayload());

                  boolean matched = false;
                  boolean sendConfigure = false;
                  for (com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfigurationRecord rec : rsp.getAttributes()) {
                     for (com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord arec : configureReporting.getAttributes()) {
                        if (rec.getAttributeIdentifier() == arec.getAttributeIdentifier()) {
                           matched = true;
                           if (rec.getStatus() == 0) {
                              byte[] reqChange = arec.getReportableChange();
                              byte[] actChange = rec.getReportableChange();
                              boolean changeMatches = (reqChange == null && actChange == null) ||
                                                      (reqChange != null && actChange != null && Arrays.equals(reqChange, actChange));

                              sendConfigure = arec.getMinimumReportingInterval() != rec.getMinimumReportingInterval() ||
                                              arec.getMaximumReportingInterval() != rec.getMaximumReportingInterval() ||
                                              !changeMatches;
                           } else {
                              sendConfigure = true;
                           }

                           break;
                        }
                     }

                     if (matched) {
                        if (sendConfigure) {
                           if (state == State.READ) {
                              setState(State.CONFIGURE);
                              sendConfigureReporting();
                              sendReadReporting();
                           }
                        } else {
                           setState(State.READ);
                           finish();
                        }
                     }

                  }
               }
               break;
            default:
               // ignore
               return;
            }
         } catch (Exception ex) {
            log.warn("failed to check protocol message for configure reporting completion: ", ex);
         }
      }

      private void sendReadReporting() throws IOException {
         ZigbeeMessage.Protocol msg = ZigbeeProtocol.packageMessage(
            ZigbeeMessage.Zcl.builder()
               .setProfileId(profile)
               .setEndpoint(endpoint)
               .setClusterId(cluster)
               .setPayload(ByteOrder.LITTLE_ENDIAN, readReporting)
               .setZclMessageId(readReporting.getMessageId())
               .create()
         );

         sendToDevice(ZigbeeProtocol.INSTANCE, msg);
      }

      private void sendConfigureReporting() throws IOException {
         ZigbeeMessage.Protocol msg = ZigbeeProtocol.packageMessage(
            ZigbeeMessage.Zcl.builder()
               .setProfileId(profile)
               .setEndpoint(endpoint)
               .setClusterId(cluster)
               .setPayload(ByteOrder.LITTLE_ENDIAN, configureReporting)
               .setZclMessageId(configureReporting.getMessageId())
               .create()
         );

         sendToDevice(ZigbeeProtocol.INSTANCE, msg);
      }
   }
}

