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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableList;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.reflex.ReflexMatchContext;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protoc.runtime.ProtocNamingFields;
import com.iris.protoc.runtime.ProtocStruct;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZclDataUtil;
import com.iris.protocol.zigbee.ZigbeeBindEvent;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public enum ZigbeeReflex {
   INSTANCE;

   private final Serializer<MessageBody> CONTROL_SERIALIZER = JSON.createSerializer(MessageBody.class);

   private ZigbeeReflex() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Zigbee Convenience Methods
   /////////////////////////////////////////////////////////////////////////////
   
   public ProtocMessage bind(Map<String,Object> config) {
      return bind(ImmutableList.of(config));
   }

   public ProtocMessage bind(List<Map<String,Object>> configs) {
      Map<Integer,List<ZigbeeBindEvent.Binding>> binds = new HashMap<>();
      for (Map<String,Object> config : configs) {
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
            continue;
         }

         ZigbeeBindEvent.Binding binding = new ZigbeeBindEvent.Binding(cluster, !server);
         List<ZigbeeBindEvent.Binding> existing = binds.get(endpoint);
         if (existing == null) {
            existing = new ArrayList<>();
            binds.put(endpoint, existing);
         }

         existing.add(binding);
      }

      MessageBody body = ZigbeeBindEvent.createEndpointBindings(binds);

      return ZigbeeMessage.Control.builder()
         .setPayload(CONTROL_SERIALIZER.serialize(body))
         .create();
   }

   public ProtocMessage report(Map<String,Object> config) {
      return report(ImmutableList.of(config));
   }

   public ProtocMessage report(List<Map<String,Object>> configs) {
      try {
         Integer profile = null;
         Integer endpoint = null;
         Integer cluster = null;
         Integer manufCode = null;
         Boolean clsSpec = null;
         Boolean fromServ = null;
         Boolean disableRsp = null;

         if (configs.isEmpty()) {
            GroovyValidator.error("zigbee report should not be applied to empty lists");
         }

         int i = 0;
         com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord[] recs = new com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord[configs.size()];
         for (Map<String,Object> config : configs) {
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
               GroovyValidator.error("zigbee report must define 'attr'");
            }

            Integer type = null;
            if (config.containsKey("type")) {
               type = ((Number)config.get("type")).intValue();
            }

            byte[] rchange = null;
            Object change = config.get("change");
            if (change instanceof ZclData) {
               if (type != null) {
                  GroovyValidator.error("zigbee report should not define 'type' when defining 'change'");
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
               GroovyValidator.error("zigbee report must define 'type'");
            }

            int pro = ZigbeeNaming.HA_PROFILE_ID;
            Integer cls = null;
            Integer end = null;
            Integer manuf = null;
            Boolean clsspc= null;
            Boolean fromsrv = null;
            Boolean ddr = null;

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
                  GroovyValidator.error("unknown data type for zigbee report 'cluster': " + ocls);
               }
            } else {
               GroovyValidator.error("zigbee report must define 'cluster'");
            }

            if (config.containsKey("manufacturer")) {
               manuf = ((Number)config.get("manufacturer")).intValue();
            }

            if (config.containsKey("clusterSpecific")) {
               clsspc = (Boolean)config.get("clusterSpecific");
            }

            if (config.containsKey("fromServer")) {
               fromsrv = (Boolean)config.get("fromServer");
            }

            if (config.containsKey("disableDefaultResponse")) {
               ddr = (Boolean)config.get("disableDefaultResponse");
            }

            if (profile == null) {
               profile = pro;
            } else if (profile != pro) {
               GroovyValidator.error("zigbee report must define 'profile' the same for all reports included in a single message");
            }

            if (cluster == null) {
               cluster = cls;
            } else if (cluster != cls) {
               GroovyValidator.error("zigbee report must define 'cluster' the same for all reports included in a single message");
            }

            if (endpoint == null) {
               endpoint = end;
            } else if (endpoint != end) {
               GroovyValidator.error("zigbee report must define 'endpoint' the same for all reports included in a single message");
            }

            if (manufCode == null && manuf != null) {
               manufCode = manuf;
            } else if (manuf != null && manufCode != manuf) {
               GroovyValidator.error("zigbee report must define 'manufacturer' the same for all reports included in a single message");
            }

            if (clsSpec == null && clsspc != null) {
               clsSpec = clsspc;
            } else if (clsspc != null && clsSpec != clsspc) {
               GroovyValidator.error("zigbee report must define 'clusterSpecific' the same for all reports included in a single message");
            }

            if (fromServ == null && fromsrv != null) {
               fromServ = fromsrv;
            } else if (fromsrv != null && fromServ != fromsrv) {
               GroovyValidator.error("zigbee report must define 'fromServer' the same for all reports included in a single message");
            }

            if (disableRsp == null && ddr != null) {
               disableRsp = ddr;
            } else if (ddr != null && disableRsp != ddr) {
               GroovyValidator.error("zigbee report must define 'disableDefaultResponse' the same for all reports included in a single message");
            }

            if (rchange != null) {
               recs[i++] = com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord.builder()
                  .setDirection(0)
                  .setAttributeIdentifier(attr)
                  .setAttributeDataType(type)
                  .setReportableChange(rchange)
                  .setMinimumReportingInterval(min)
                  .setMaximumReportingInterval(max)
                  .create();
            } else {
               recs[i++] = com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord.builder()
                  .setDirection(0)
                  .setAttributeIdentifier(attr)
                  .setAttributeDataType(type)
                  .setMinimumReportingInterval(min)
                  .setMaximumReportingInterval(max)
                  .create();
            }
         }

         if (endpoint == null) {
            GroovyValidator.error("zigbee report must define 'endpoint'");
         }

         if (cluster == null) {
            GroovyValidator.error("zigbee report must define 'cluster'");
         }

         if (profile == null) {
            GroovyValidator.error("zigbee report must define 'profile'");
         }

         if (endpoint == null || cluster == null || profile == null) {
            return null;
         }

         ProtocMessage crep = com.iris.protocol.zigbee.zcl.General.ZclConfigureReporting.builder()
            .setAttributes(recs)
            .create();
         
         ZigbeeMessage.Zcl.Builder bld = ZigbeeMessage.Zcl.builder()
            .setProfileId(profile)
            .setEndpoint(endpoint)
            .setClusterId(cluster)
            .setPayload(ByteOrder.LITTLE_ENDIAN, crep)
            .setZclMessageId(crep.getMessageId());

         if (clsSpec != null || fromServ != null || disableRsp != null || manufCode != null) {
            int flags = 0;
            if (Boolean.TRUE.equals(clsSpec)) {
               flags |= ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
            }

            if (Boolean.TRUE.equals(fromServ)) {
               flags |= ZigbeeMessage.Zcl.FROM_SERVER;
            }

            if (Boolean.TRUE.equals(disableRsp)) {
               flags |= ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
            }

            if (manufCode != null) {
               flags |= ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
               bld.setManufacturerCode(manufCode);
            }

            bld.setFlags(flags);
         }

         return bld.create();
      } catch (Exception ex) {
         GroovyValidator.error("could not process zigbee configure reporting message", ex);
         return null;
      }
   }

   public ProtocMessage write(Map<String,Object> config) {
      return write(ImmutableList.of(config));
   }

   public ProtocMessage write(List<Map<String,Object>> configs) {
      try {
         Integer profile = null;
         Integer endpoint = null;
         Integer cluster = null;
         Integer manufCode = null;
         Boolean clsSpec = null;
         Boolean fromServ = null;
         Boolean disableRsp = null;

         if (configs.isEmpty()) {
            GroovyValidator.error("zigbee write should not be applied to empty lists");
         }

         int i = 0;
         com.iris.protocol.zigbee.zcl.General.ZclWriteAttributeRecord[] attrs = new com.iris.protocol.zigbee.zcl.General.ZclWriteAttributeRecord[configs.size()];
         for (Map<String,Object> config : configs) {
            int attr = -1;
            if (config.containsKey("attr")) {
               attr = ((Number)config.get("attr")).intValue();
            } else {
               GroovyValidator.error("zigbee write must define 'attr'");
            }

            ZclData data;
            if (config.containsKey("value")) {
               Object value = config.get("value");
               if (value instanceof ZclData) {
                  data = (ZclData)value;
               } else {
                  data = ZclData.builder().create();
                  GroovyValidator.error("zigbee write must define 'value' as ZclData");
               }
            } else {
               data = ZclData.builder().create();
               GroovyValidator.error("zigbee write must define 'attr'");
            }

            int pro = ZigbeeNaming.HA_PROFILE_ID;
            Integer cls = null;
            Integer end = null;
            Integer manuf = null;
            Boolean clsspc= null;
            Boolean fromsrv = null;
            Boolean ddr = null;

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
                  GroovyValidator.error("unknown data type for zigbee write 'cluster': " + ocls);
               }
            } else {
               GroovyValidator.error("zigbee write must define 'cluster'");
            }

            if (config.containsKey("manufacturer")) {
               manuf = ((Number)config.get("manufacturer")).intValue();
            }

            if (config.containsKey("clusterSpecific")) {
               clsspc = (Boolean)config.get("clusterSpecific");
            }

            if (config.containsKey("fromServer")) {
               fromsrv = (Boolean)config.get("fromServer");
            }

            if (config.containsKey("disableDefaultResponse")) {
               ddr = (Boolean)config.get("disableDefaultResponse");
            }

            if (profile == null) {
               profile = pro;
            } else if (profile != pro) {
               GroovyValidator.error("zigbee write must define 'profile' the same for all reports included in a single message");
            }

            if (cluster == null) {
               cluster = cls;
            } else if (cluster != cls) {
               GroovyValidator.error("zigbee write must define 'cluster' the same for all reports included in a single message");
            }

            if (endpoint == null) {
               endpoint = end;
            } else if (endpoint != end) {
               GroovyValidator.error("zigbee write must define 'endpoint' the same for all reports included in a single message");
            }

            if (manufCode == null && manuf != null) {
               manufCode = manuf;
            } else if (manuf != null && manufCode != manuf) {
               GroovyValidator.error("zigbee report must define 'manufacturer' the same for all reports included in a single message");
            }

            if (clsSpec == null && clsspc != null) {
               clsSpec = clsspc;
            } else if (clsspc != null && clsSpec != clsspc) {
               GroovyValidator.error("zigbee report must define 'clusterSpecific' the same for all reports included in a single message");
            }

            if (fromServ == null && fromsrv != null) {
               fromServ = fromsrv;
            } else if (fromsrv != null && fromServ != fromsrv) {
               GroovyValidator.error("zigbee report must define 'fromServer' the same for all reports included in a single message");
            }

            if (disableRsp == null && ddr != null) {
               disableRsp = ddr;
            } else if (ddr != null && disableRsp != ddr) {
               GroovyValidator.error("zigbee report must define 'disableDefaultResponse' the same for all reports included in a single message");
            }

            attrs[i++] = com.iris.protocol.zigbee.zcl.General.ZclWriteAttributeRecord.builder()
               .setAttributeIdentifier(attr)
               .setAttributeData(data)
               .create();
         }

         if (endpoint == null) {
            GroovyValidator.error("zigbee write must define 'endpoint'");
         }

         if (cluster == null) {
            GroovyValidator.error("zigbee write must define 'cluster'");
         }

         if (profile == null) {
            GroovyValidator.error("zigbee write must define 'profile'");
         }

         if (endpoint == null || cluster == null || profile == null) {
            return null;
         }

         ProtocMessage read = com.iris.protocol.zigbee.zcl.General.ZclWriteAttributes.builder()
            .setAttributes(attrs)
            .create();

         ZigbeeMessage.Zcl.Builder bld = ZigbeeMessage.Zcl.builder()
            .setProfileId(profile)
            .setEndpoint(endpoint)
            .setClusterId(cluster)
            .setPayload(ByteOrder.LITTLE_ENDIAN, read)
            .setZclMessageId(read.getMessageId());

         if (clsSpec != null || fromServ != null || disableRsp != null || manufCode != null) {
            int flags = 0;
            if (Boolean.TRUE.equals(clsSpec)) {
               flags |= ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
            }

            if (Boolean.TRUE.equals(fromServ)) {
               flags |= ZigbeeMessage.Zcl.FROM_SERVER;
            }

            if (Boolean.TRUE.equals(disableRsp)) {
               flags |= ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
            }

            if (manufCode != null) {
               flags |= ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
               bld.setManufacturerCode(manufCode);
            }

            bld.setFlags(flags);
         }

         return bld.create();
      } catch (Exception ex) {
         GroovyValidator.error("could not process zigbee write attributes message", ex);
         return null;
      }
   }

   public ProtocMessage read(Map<String,Object> config) {
      return read(ImmutableList.of(config));
   }

   public ProtocMessage read(List<Map<String,Object>> configs) {
      try {
         Integer profile = null;
         Integer endpoint = null;
         Integer cluster = null;
         Integer manufCode = null;
         Boolean clsSpec = null;
         Boolean fromServ = null;
         Boolean disableRsp = null;

         if (configs.isEmpty()) {
            GroovyValidator.error("zigbee read should not be applied to empty lists");
         }

         int i = 0;
         short[] attrs = new short[configs.size()];
         for (Map<String,Object> config : configs) {
            int attr = -1;
            if (config.containsKey("attr")) {
               attr = ((Number)config.get("attr")).intValue();
            } else {
               GroovyValidator.error("zigbee read must define 'attr'");
            }

            int pro = ZigbeeNaming.HA_PROFILE_ID;
            Integer cls = null;
            Integer end = null;
            Integer manuf = null;
            Boolean clsspc= null;
            Boolean fromsrv = null;
            Boolean ddr = null;

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
                  GroovyValidator.error("unknown data type for zigbee read 'cluster': " + ocls);
               }
            } else {
               GroovyValidator.error("zigbee read must define 'cluster'");
            }

            if (config.containsKey("manufacturer")) {
               manuf = ((Number)config.get("manufacturer")).intValue();
            }

            if (config.containsKey("clusterSpecific")) {
               clsspc = (Boolean)config.get("clusterSpecific");
            }

            if (config.containsKey("fromServer")) {
               fromsrv = (Boolean)config.get("fromServer");
            }

            if (config.containsKey("disableDefaultResponse")) {
               ddr = (Boolean)config.get("disableDefaultResponse");
            }

            if (profile == null) {
               profile = pro;
            } else if (profile != pro) {
               GroovyValidator.error("zigbee read must define 'profile' the same for all reports included in a single message");
            }

            if (cluster == null) {
               cluster = cls;
            } else if (cluster != cls) {
               GroovyValidator.error("zigbee read must define 'cluster' the same for all reports included in a single message");
            }

            if (endpoint == null) {
               endpoint = end;
            } else if (endpoint != end) {
               GroovyValidator.error("zigbee read must define 'endpoint' the same for all reports included in a single message");
            }

            if (manufCode == null && manuf != null) {
               manufCode = manuf;
            } else if (manuf != null && manufCode != manuf) {
               GroovyValidator.error("zigbee report must define 'manufacturer' the same for all reports included in a single message");
            }

            if (clsSpec == null && clsspc != null) {
               clsSpec = clsspc;
            } else if (clsspc != null && clsSpec != clsspc) {
               GroovyValidator.error("zigbee report must define 'clusterSpecific' the same for all reports included in a single message");
            }

            if (fromServ == null && fromsrv != null) {
               fromServ = fromsrv;
            } else if (fromsrv != null && fromServ != fromsrv) {
               GroovyValidator.error("zigbee report must define 'fromServer' the same for all reports included in a single message");
            }

            if (disableRsp == null && ddr != null) {
               disableRsp = ddr;
            } else if (ddr != null && disableRsp != ddr) {
               GroovyValidator.error("zigbee report must define 'disableDefaultResponse' the same for all reports included in a single message");
            }

            attrs[i++] = (short)attr;
         }

         if (endpoint == null) {
            GroovyValidator.error("zigbee read must define 'endpoint'");
         }

         if (cluster == null) {
            GroovyValidator.error("zigbee read must define 'cluster'");
         }

         if (profile == null) {
            GroovyValidator.error("zigbee read must define 'profile'");
         }

         if (endpoint == null || cluster == null || profile == null) {
            return null;
         }

         ProtocMessage read = com.iris.protocol.zigbee.zcl.General.ZclReadAttributes.builder()
            .setAttributes(attrs)
            .create();

         ZigbeeMessage.Zcl.Builder bld = ZigbeeMessage.Zcl.builder()
            .setProfileId(profile)
            .setEndpoint(endpoint)
            .setClusterId(cluster)
            .setPayload(ByteOrder.LITTLE_ENDIAN, read)
            .setZclMessageId(read.getMessageId());

         if (clsSpec != null || fromServ != null || disableRsp != null || manufCode != null) {
            int flags = 0;
            if (Boolean.TRUE.equals(clsSpec)) {
               flags |= ZigbeeMessage.Zcl.CLUSTER_SPECIFIC;
            }

            if (Boolean.TRUE.equals(fromServ)) {
               flags |= ZigbeeMessage.Zcl.FROM_SERVER;
            }

            if (Boolean.TRUE.equals(disableRsp)) {
               flags |= ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE;
            }

            if (manufCode != null) {
               flags |= ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
               bld.setManufacturerCode(manufCode);
            }

            bld.setFlags(flags);
         }

         return bld.create();
      } catch (Exception ex) {
         GroovyValidator.error("could not process zigbee read attributes message", ex);
         return null;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Zigbee Send Action
   /////////////////////////////////////////////////////////////////////////////

   public static abstract class ZigbeeSendProcessor implements ReflexMatchContext.ProtocolClosureProcessor {
      @Override
      public void process(Object cmd, Map<String,Object> values) {
         if (cmd instanceof ZigbeeConfigProtocol.ZigbeeFieldNaming) {
            processZigbeeCommand((ZigbeeConfigProtocol.ZigbeeFieldNaming)cmd, values);
         } else if (cmd instanceof ProtocMessage) {
            processMessage((ProtocMessage)cmd);
         } else {
            GroovyValidator.error("cannot process send: " + cmd);
         }
      }

      private void processZigbeeCommand(ZigbeeConfigProtocol.ZigbeeFieldNaming naming, Map<String,Object> values) {
         try {
            processMessage((ProtocMessage)extractZigbeeCommand(naming,values));
         } catch (Exception ex) {
            GroovyValidator.error("could not process zigbee send", ex);
         }
      }

      protected void processMessage(ProtocMessage message) {
         try {
            processSendCommand(
               ZigbeeMessage.Protocol.builder()
                  .setType(message.getMessageId())
                  .setPayload(ByteOrder.LITTLE_ENDIAN, message)
                  .create()
            );
         } catch (Exception ex) {
            GroovyValidator.error("could not process zigbee send", ex);
         }
      }

      protected abstract void processSendCommand(ProtocStruct command);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Zigbee Regex Match
   /////////////////////////////////////////////////////////////////////////////

   public static abstract class ZigbeeMatchProcessor implements ReflexMatchContext.ProtocolClosureProcessor {
      @Override
      public void process(Object cmd, Map<String,Object> values) {
         if (cmd instanceof ZigbeeConfigProtocol.ZigbeeFieldNaming) {
            processZigbeeCommand((ZigbeeConfigProtocol.ZigbeeFieldNaming)cmd, values);
         } else if (cmd instanceof ProtocMessage) {
            processMessage((ProtocMessage)cmd);
         } else if (cmd instanceof ProtocolMatch) {
            processMatch((ProtocolMatch)cmd);
         } else {
            GroovyValidator.error("cannot process match: " + cmd);
         }
      }

      private void processZigbeeCommand(ZigbeeConfigProtocol.ZigbeeFieldNaming naming, Map<String,Object> values) {
         try {
            processMatchString(naming.match(values));
         } catch (Exception ex) {
            GroovyValidator.error("could not process zigbee send", ex);
         }
      }

      protected void processMessage(ProtocMessage message) {
         try {
            // <TYPE> <LEN> <LEN> <LEN> <LEN> <PAYLOAD>
            StringBuilder rex = new StringBuilder();
            rex.append(ProtocUtil.toHexString((byte)message.getMessageId()))
               .append(" . . . .");

            byte[] payload = message.toBytes(ByteOrder.LITTLE_ENDIAN);
            for (byte val : payload) {
               rex.append(' ');
               rex.append(ProtocUtil.toHexString(val));
            }

            processMatchString(rex.toString());
         } catch (Exception ex) {
            GroovyValidator.error("could not process zigbee send", ex);
         }
      }

      protected void processMatch(ProtocolMatch match) {
         try {
            StringBuilder rex = match.regex;
            if (rex.length() != 0) {
               rex.append(" ");
            }

            rex.append(". . . .");

            for (String val : match.getPayload()) {
               if (rex.length() != 0) {
                  rex.append(' ');
               }

               if (val == null || val.isEmpty()) {
                  rex.append('.');
               } else {
                  rex.append(val);
               }
            }

            processMatchString(rex.toString());
         } catch (Exception ex) {
            GroovyValidator.error("could not process zigbee send", ex);
         }
      }

      /*
      private void processNodeInfo(Map<String,Object> values) {
         // [02,00,00,00,05,03,84,04,10,01]
         
         String status = getMatchValue(values, "status", null);
         String basic = getMatchValue(values, "basic", null);
         String generic = getMatchValue(values, "generic", null);
         String specific = getMatchValue(values, "specific", null);
         
         // <TYPE> <LEN> <LEN> <LEN> <LEN> <NODE> <STATUS> <BASIC> <GENERIC> <SPECIFIC>
         int dots = 1;
         StringBuilder regexMatch = new StringBuilder();
         appendValue(regexMatch, Protocol.NodeInfo.ID, 0);
         appendValue(regexMatch, 0, 0);
         appendValue(regexMatch, 0, 0);
         appendValue(regexMatch, 0, 0);
         appendValue(regexMatch, 5, 0);
         dots = appendValue(regexMatch, status, dots);
         dots = appendValue(regexMatch, basic, dots);
         dots = appendValue(regexMatch, generic, dots);
         dots = appendValue(regexMatch, specific, dots);

         Set<String> unmatched = new HashSet<>(values.keySet());
         unmatched.remove("status");
         unmatched.remove("basic");
         unmatched.remove("generic");
         unmatched.remove("specific");
         GroovyValidator.assertTrue(unmatched.isEmpty(), "zwave reflex match specifies unknown values: " + unmatched);

         processMatchString(regexMatch.toString());
      }

      private void processZigbeeCommand(ZigbeeCommand command, Map<String,Object> values) {
         ArrayList<String> names = command.getSendNames();
         if (names == null || names.isEmpty()) {
            names = command.getReceiveNames();
         }

         // <TYPE> <LEN> <LEN> <LEN> <LEN> <NODE> <CCID> <CMDID> <LEN> <LEN> <LEN> <LEN> <PAYLOAD>...
         StringBuilder regexMatch = new StringBuilder();
         appendValue(regexMatch, Protocol.Command.ID, 0);
         appendValue(regexMatch, command.commandClass & 0xFF, 5);
         appendValue(regexMatch, command.commandNumber & 0xFF, 0);

         int dots = 4;
         for (String name : names) {
            Object value = values.get(name);
            if (value == null) {
               dots++;
            } else if (value instanceof Number) {
               Number num = (Number)value;
               long lvalue = num.longValue();
               long ltst = Math.abs(lvalue);
               
               GroovyValidator.assertTrue(ltst < 256, "zwave reflex matches can only use 8-bit values");

               appendValue(regexMatch, (int)(lvalue & 0xFF), dots);
               dots = 0;
            } else {
               GroovyValidator.error("zwave reflex matches can only use 8-bit numeric values");
            }
         }

         Set<String> unmatched = new HashSet<>(values.keySet());
         unmatched.removeAll(names);
         GroovyValidator.assertTrue(unmatched.isEmpty(), "zwave reflex match specifies unknown values: " + unmatched);

         processMatchString(regexMatch.toString());
      }

      private String getMatchValue(Map<String,Object> values, String key, Object def) {
         Object res = def;
         if (values.containsKey(key)) {
            res = values.get(key);
         }

         if (res == null) {
            return null;
         }

         return String.valueOf(res);
      }

      private void appendValue(StringBuilder regexMatch, int val, int dotsBeforeValue) {
         if (val < 10) appendValue(regexMatch, "0" + Integer.toHexString(val), dotsBeforeValue);
         else appendValue(regexMatch, Integer.toHexString(val), dotsBeforeValue);
      }

      private int appendValue(StringBuilder regexMatch, String val, int dotsBeforeValue) {
         if (val == null) {
            return dotsBeforeValue + 1;
         }

         for (int i = 0; i < dotsBeforeValue; ++i) {
            space(regexMatch);
            regexMatch.append(".");
         }

         space(regexMatch);
         regexMatch.append(val);
         return 0;
      }

      private void space(StringBuilder regexMatch) {
         if (regexMatch.length() != 0) {
            regexMatch.append(" ");
         }
      }
   */

      protected abstract void processMatchString(String match);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Helpers
   /////////////////////////////////////////////////////////////////////////////

   private static ProtocStruct extractZigbeeCommand(ZigbeeConfigProtocol.ZigbeeFieldNaming naming, Map<String,Object> values) throws Exception {
      int endpoint = naming.getGroups().getDefaultEndpoint();
      if (values.containsKey("endpoint")) {
         endpoint = ((Number)values.get("endpoint")).intValue();
      }

      int profile = naming.getGroups().getDefaultProfile();
      if (values.containsKey("profile")) {
         profile = ((Number)values.get("profile")).intValue();
      }

      int manuf = -1;
      if (values.containsKey("manuf")) {
         manuf = ((Number)values.get("manuf")).intValue();
      }

      boolean cls = naming.getFields().isClusterSpecific();
      if (values.containsKey("clusterSpecific")) {
         cls = (Boolean)values.get("clusterSpecific");
      }

      boolean frs = naming.getFields().isFromServer();
      if (values.containsKey("fromServer")) {
         frs = (Boolean)values.get("fromServer");
      }

      boolean ddr = false;
      if (values.containsKey("disableDefaultResponse")) {
         ddr = (Boolean)values.get("disableDefaultResponse");
      }

      int cluster = naming.getMessages().getClusterId();
      if (values.containsKey("cluster")) {
         cluster = ((Number)values.get("cluster")).intValue();
      }

      Map<String,Object> context = new HashMap<>();
      Map<String,Class<?>> fields = naming.getFields().getFields();
      for (Map.Entry<String,Class<?>> field : fields.entrySet()) {
         String name = field.getKey();
         Object value = values.get(name);
         if (value == null) {
            GroovyValidator.error("zigbee send must define value for '" + name + "'");
            continue;
         }

         Class<?> srcType = value.getClass();
         Class<?> dstType = field.getValue();
         context.put(name, coerce(srcType, dstType, value));
      }

      Set<String> unmatched = new HashSet<>(values.keySet());
      unmatched.remove("endpoint");
      unmatched.remove("profile");
      unmatched.remove("manuf");
      unmatched.remove("clusterSpecific");
      unmatched.remove("fromServer");
      unmatched.remove("disableDefaultResponse");
      unmatched.remove("cluster");

      unmatched.removeAll(fields.keySet());
      GroovyValidator.assertTrue(unmatched.isEmpty(), "zigbee send specifies unknown values: " + unmatched);

      ProtocMessage msg = (ProtocMessage)naming.getFields().create(context);
      return naming.getGroups().wrap(msg, profile, endpoint, cluster, msg.getMessageId(), manuf, cls, frs, ddr);
   }

   public static Object coerce(Class<?> srcType, Class<?> dstType, Object value) {
      try {
         if (dstType == byte.class || dstType==Byte.class) {
            return ((Number)value).byteValue();
         } if (dstType == short.class || dstType == Short.class) {
            return ((Number)value).shortValue();
         } if (dstType == int.class || dstType == Integer.class) {
            return ((Number)value).intValue();
         } if (dstType == long.class || dstType == Long.class) {
            return ((Number)value).longValue();
         } if (dstType == float.class || dstType == Float.class) {
            return ((Number)value).floatValue();
         } if (dstType == double.class || dstType == Double.class) {
            return ((Number)value).doubleValue();
         }
      } catch (Exception ex) {
         // fall back to original value
      }

      return value;
   }

   public static final class ProtocolMatch {
      private final StringBuilder regex;
      private final List<String> payload;

      public ProtocolMatch(StringBuilder regex, List<String> payload) {
         this.regex = regex;
         this.payload = payload;
      }

      public StringBuilder getRegex() {
         return regex;
      }

      public List<String> getPayload() {
         return payload;
      }
   }
}

