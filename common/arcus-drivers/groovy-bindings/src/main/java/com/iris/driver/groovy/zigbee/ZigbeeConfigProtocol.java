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

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.reflex.ReflexMatchContext;
import com.iris.driver.groovy.reflex.ReflexUtil;
import com.iris.driver.groovy.zigbee.cluster.alertme.ZigbeeAlertmeNaming;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclNaming;
import com.iris.driver.groovy.zigbee.cluster.zdp.ZigbeeZdpNaming;
import com.iris.protoc.runtime.ProtocNamingFields;
import com.iris.protoc.runtime.ProtocNamingGroups;
import com.iris.protoc.runtime.ProtocNamingMessages;
import com.iris.protoc.runtime.ProtocStruct;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;

public class ZigbeeConfigProtocol {
   private static final Logger LOGGER = LoggerFactory.getLogger(ZigbeeConfigProtocol.class);

   public static Object getProtocolProperty(String name) {
      switch (name) {
      case "alertme":
      case "Alertme":
      case "AlertMe":
      case "ALERTME":
         return AlertmeSupport.instance;
      case "zcl":
      case "Zcl":
      case "ZCL":
         return ZclSupport.instance;
      case "zdp":
      case "Zdp":
      case "ZDP":
         return ZdpSupport.instance;
      default:
         return null;
      }
   }

   private static final class AlertmeSupport extends GroovyObjectSupport {
      private static final AlertmeSupport instance = new AlertmeSupport();

      @Override
      public Object getProperty(String property) {
         ProtocNamingMessages nming = ZigbeeAlertmeNaming.INSTANCE.getGroups().get(property.toLowerCase());

         if (nming != null) {
            return new ZigbeeMessageNaming(ZigbeeAlertmeNaming.INSTANCE, nming, ZclFieldNamingSupport.INSTANCE);
         }

         return super.getProperty(property);
      }
   }

   private static final class ZclSupport extends GroovyObjectSupport {
      private static final ZclSupport instance = new ZclSupport();

      @Override
      public Object getProperty(String property) {
         ProtocNamingMessages nming = ZigbeeZclNaming.INSTANCE.getGroups().get(property.toLowerCase());
         if (nming != null) {
            return new ZigbeeMessageNaming(ZigbeeZclNaming.INSTANCE, nming, ZclFieldNamingSupport.INSTANCE);
         }

         return super.getProperty(property);
      }

      public void send(ReflexMatchContext.ProtocolClosureProcessor proc, Map<String,Object> payload) {
         if (payload == null) {
            throw new MissingMethodException("send", getClass(), new Object[] { proc, payload });
         }

         proc.process(extractMessage(payload), ImmutableMap.<String,Object>of());
      }

      public void match(ReflexMatchContext.ProtocolClosureProcessor proc, Map<String,Object> payload) {
         if (payload == null) {
            throw new MissingMethodException("match", getClass(), new Object[] { proc, payload });
         }

         StringBuilder rex = new StringBuilder();
         ZclFieldNamingSupport.INSTANCE.match(rex, payload, ImmutableMap.<String,Object>of());

         List<String> msg = ReflexUtil.extractAsMatchList(payload, "payload");
         proc.process(new ZigbeeReflex.ProtocolMatch(rex,msg), ImmutableMap.<String,Object>of());
      }

      public static ProtocStruct extractMessage(Map<String,Object> payload) {
         int id = -1;
         if (payload.containsKey("id") && !payload.containsKey("command")) {
            id = ((Number)payload.get("id")).intValue();
         } else if (!payload.containsKey("id") && payload.containsKey("command")) {
            id = ((Number)payload.get("command")).intValue();
         } else {
            GroovyValidator.error("zigbee zcl message must contain either 'id' or 'command' to define the command identifier");
         }

         int manuf = -1;
         if (payload.containsKey("manuf")) {
            manuf = ((Number)payload.get("manuf")).intValue();
         } 

         int endpoint = -1;
         if (payload.containsKey("endpoint")) {
            endpoint = ((Number)payload.get("endpoint")).intValue();
         } else {
            GroovyValidator.error("zigbee zcl message must define 'endpoint'");
         }

         int cluster = -1;
         if (payload.containsKey("cluster")) {
            cluster = ((Number)payload.get("cluster")).intValue();
         } else {
            GroovyValidator.error("zigbee zcl message must define 'cluster'");
         }

         int profile = ZigbeeNaming.HA_PROFILE_ID;
         if (payload.containsKey("profile")) {
            profile = ((Number)payload.get("profile")).intValue();
         } 

         boolean isClusterSpecific = true;
         if (payload.containsKey("clusterSpecific")) {
            isClusterSpecific = (boolean)payload.get("clusterSpecific");
         } 

         boolean isFromServer = false;
         if (payload.containsKey("fromServer")) {
            isFromServer = (boolean)payload.get("fromServer");
         } 

         boolean isDisableDefaultResponse = false;
         if (payload.containsKey("disableDefaultResponse")) {
            isDisableDefaultResponse = (boolean)payload.get("disableDefaultResponse");
         } 

         byte[] msg = ReflexUtil.extractAsByteArray(payload, "payload");
         return ZigbeeNaming.INSTANCE.wrapZclMessage(msg, profile, endpoint, cluster, id, manuf, isClusterSpecific, isFromServer, isDisableDefaultResponse);
      }
   }

   private static final class ZdpSupport extends GroovyObjectSupport {
      private static final ZdpSupport instance = new ZdpSupport();

      @Override
      public Object getProperty(String property) {
         ProtocNamingMessages nming = ZigbeeZdpNaming.INSTANCE.getGroups().get(property.toLowerCase());
         if (nming != null) {
            return new ZigbeeMessageNaming(ZigbeeZdpNaming.INSTANCE, nming, ZdpFieldNamingSupport.INSTANCE);
         }

         return super.getProperty(property);
      }

      public void send(ReflexMatchContext.ProtocolClosureProcessor proc, Map<String,Object> payload) {
         if (payload == null) {
            throw new MissingMethodException("send", getClass(), new Object[] { proc, payload });
         }

         proc.process(extractMessage(payload), ImmutableMap.<String,Object>of());
      }

      public void match(ReflexMatchContext.ProtocolClosureProcessor proc, Map<String,Object> payload) {
         if (payload == null) {
            throw new MissingMethodException("match", getClass(), new Object[] { proc, payload });
         }

         StringBuilder rex = new StringBuilder();
         ZdpFieldNamingSupport.INSTANCE.match(rex, payload, ImmutableMap.<String,Object>of());

         List<String> msg = ReflexUtil.extractAsMatchList(payload, "payload");
         proc.process(new ZigbeeReflex.ProtocolMatch(rex,msg), ImmutableMap.<String,Object>of());
      }

      public static ProtocStruct extractMessage(Map<String,Object> payload) {
         int id = -1;
         if (payload.containsKey("id") && !payload.containsKey("command")) {
            id = ((Number)payload.get("id")).intValue();
         } else if (!payload.containsKey("id") && payload.containsKey("command")) {
            id = ((Number)payload.get("command")).intValue();
         } else {
            GroovyValidator.error("zigbee zcl message must contain either 'id' or 'command' to define the command identifier");
         }

         byte[] msg = ReflexUtil.extractAsByteArray(payload, "payload");
         return ZigbeeNaming.INSTANCE.wrapZdpMessage(msg, id);
      }
   }

   private static enum ZclFieldNamingSupport implements ZigbeeFieldNamingSupport {
      INSTANCE;

      @Override
      public void match(StringBuilder rex, Map<String,Object> values, Map<String,Object> defaults) {
         rex.append(ProtocUtil.toHexString((byte)ZigbeeMessage.Zcl.ID))
            .append(" . . . .");

         HashMap<String,Object> payload = new HashMap<>();
         payload.putAll(defaults);
         payload.putAll(values);

         rex.append(" ");
         if (payload.containsKey("id") && !payload.containsKey("command")) {
            rex.append(ProtocUtil.toHexString(((Number)payload.get("id")).byteValue()));
         } else if (!payload.containsKey("id") && payload.containsKey("command")) {
            rex.append(ProtocUtil.toHexString(((Number)payload.get("command")).byteValue()));
         } else {
            rex.append(".");
         }

         rex.append(" ");
         if (payload.containsKey("flags")) {
            rex.append(ProtocUtil.toHexString(((Number)payload.get("flags")).byteValue()));
         } else {
            rex.append(".");
         }

         rex.append(" ");
         if (payload.containsKey("profile")) {
            rex.append(ProtocUtil.toHexString(Short.reverseBytes(((Number)payload.get("profile")).shortValue()), " "));
         } else {
            rex.append(". .");
         }

         rex.append(" ");
         if (payload.containsKey("cluster")) {
            rex.append(ProtocUtil.toHexString(Short.reverseBytes(((Number)payload.get("cluster")).shortValue()), " "));
         } else {
            rex.append(". .");
         }

         if (payload.containsKey("manuf")) {
            Object manuf = payload.get("manuf");
            if (manuf instanceof Boolean) {
               if ((Boolean)manuf) {
                  rex.append(" . .");
               }
            } else if (manuf instanceof Number) {
               rex.append(" ");
               rex.append(ProtocUtil.toHexString(Short.reverseBytes(((Number)payload.get("manuf")).shortValue()), " "));
            } else {
               GroovyValidator.error("manuf field must be either a number of boolean value");
            }
         } else {
            rex.append(" (. .)?");
         }

         rex.append(" ");
         if (payload.containsKey("endpoint")) {
            rex.append(ProtocUtil.toHexString(((Number)payload.get("endpoint")).byteValue()));
         } else {
            rex.append(".");
         }
      }
   }

   private static enum ZdpFieldNamingSupport implements ZigbeeFieldNamingSupport {
      INSTANCE;

      @Override
      public void match(StringBuilder rex, Map<String,Object> values, Map<String,Object> defaults) {
         rex.append(ProtocUtil.toHexString((byte)ZigbeeMessage.Zdp.ID))
            .append(" . . . . ");

         HashMap<String,Object> payload = new HashMap<>();
         payload.putAll(defaults);
         payload.putAll(values);

         if (payload.containsKey("id") && !payload.containsKey("command")) {
            rex.append(ProtocUtil.toHexString(Short.reverseBytes(((Number)payload.get("id")).shortValue()), " "));
         } else if (!payload.containsKey("id") && payload.containsKey("command")) {
            rex.append(ProtocUtil.toHexString(Short.reverseBytes(((Number)payload.get("command")).shortValue()), " "));
         } else {
            rex.append(". .");
         }
      }
   }

   private static final class ZigbeeMessageNaming extends GroovyObjectSupport {
      private final ProtocNamingGroups groups;
      private final ProtocNamingMessages messages;
      private final ZigbeeFieldNamingSupport support;

      public ZigbeeMessageNaming(ProtocNamingGroups groups, ProtocNamingMessages messages, ZigbeeFieldNamingSupport support) {
         this.groups = groups;
         this.messages = messages;
         this.support = support;
      }

      @Override
      public Object getProperty(String property) {
         Object constant = messages.getConstants().get(property);
         if (constant != null) {
            return constant;
         }

         ProtocNamingFields nming = messages.getMessages().get(property);
         if (nming != null) {
            return new ZigbeeFieldNaming(this,groups,messages,nming,support);
         }

         return super.getProperty(property);
      }
   }

   public static final class ZigbeeFieldNaming extends Closure<Object> {
      private final ProtocNamingGroups groups;
      private final ProtocNamingMessages messages;
      private final ProtocNamingFields fields;
      private final ZigbeeFieldNamingSupport support;

      public ZigbeeFieldNaming(ZigbeeMessageNaming msg, ProtocNamingGroups groups, ProtocNamingMessages messages, ProtocNamingFields fields, ZigbeeFieldNamingSupport support) {
         super(msg);
         this.groups = groups;
         this.messages = messages;
         this.fields = fields;
         this.support = support;
      }

      protected void doCall(ReflexMatchContext.ProtocolClosureProcessor processor) {
         processor.process(this, ImmutableMap.<String,Object>of());
      }

      protected void doCall(ReflexMatchContext.ProtocolClosureProcessor processor, Map<String,Object> args) {
         processor.process(this, args);
      }

      public ProtocNamingGroups getGroups() {
         return groups;
      }

      public ProtocNamingMessages getMessages() {
         return messages;
      }

      public ProtocNamingFields getFields() {
         return fields;
      }

      public String match(Map<String,Object> values) {
         int flags = fields.isClusterSpecific() ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0 ;
         flags |= fields.isFromServer() ? ZigbeeMessage.Zcl.FROM_SERVER : 0 ;

         Map<String,Object> defaults = ImmutableMap.<String,Object>builder()
            .put("command", fields.getMessageId())
            .put("endpoint", groups.getDefaultEndpoint())
            .put("profile", groups.getDefaultProfile())
            .put("cluster", messages.getClusterId())
            .put("flags", flags)
            .put("manuf", Boolean.FALSE)
            .build();

         StringBuilder rex = new StringBuilder();
         support.match(rex, values, defaults);

         rex.append(" . . . .");

         Set<String> unknown = new HashSet<>();
         unknown.addAll(values.keySet());
         unknown.remove("id");
         unknown.remove("command");
         unknown.remove("flags");
         unknown.remove("profile");
         unknown.remove("cluster");
         unknown.remove("manuf");
         unknown.remove("endpoint");
         unknown.removeAll(fields.getFields().keySet());
         if (!unknown.isEmpty()) {
            GroovyValidator.error("zigbee message match defines unknown fields: " + unknown);
         }

         Map<String,Object> context = new HashMap<>();
         for (Map.Entry<String,Class<?>> fld : fields.getFields().entrySet()) {
            Object value = values.get(fld.getKey());
            if (value == null) {
               continue;
            }

            Class<?> srcType = value.getClass();
            Class<?> dstType = fld.getValue();
            Object newValue = ZigbeeReflex.coerce(srcType, dstType, value);

            context.put(fld.getKey(), newValue);
         }

         Map<Integer,String> offsets = new TreeMap<>();
         for (Map.Entry<String,Object> fld : context.entrySet()) {
            int offset = fields.getOffset(fld.getKey(), context);
            int size = fields.getSize(fld.getKey(), context);
            long value = ((Number)fld.getValue()).longValue();

            Object old = offsets.put(offset,ProtocUtil.toHexString(value, size, " "));
            if (old != null) {
               GroovyValidator.error("multiple entries in protocol message at offset " + offset);
            }
         }

         int lastOffset = 0;
         for (Map.Entry<Integer,String> offset : offsets.entrySet()) {
            int nextOffset = offset.getKey();
            while (lastOffset < nextOffset) {
               rex.append(" .");
               lastOffset++;
            }

            int size = ((offset.getValue().length()-2)/3) + 1;
            lastOffset += size;

            rex.append(" ").append(offset.getValue());
         }

         return rex.toString();
      }
   }

   private static interface ZigbeeFieldNamingSupport {
      void match(StringBuilder rex, Map<String,Object> payload, Map<String,Object> defaults);
   }
}

