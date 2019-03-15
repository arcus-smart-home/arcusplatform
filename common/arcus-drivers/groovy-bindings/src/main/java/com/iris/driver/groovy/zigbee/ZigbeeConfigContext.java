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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.reflex.ReflexContext;
import com.iris.driver.groovy.reflex.ReflexForwardContext;
import com.iris.driver.groovy.reflex.ReflexMatchContext;
import com.iris.driver.groovy.reflex.ReflexUtil;
import com.iris.driver.reflex.ReflexActionAlertmeLifesign;
import com.iris.driver.reflex.ReflexActionBuiltin;
import com.iris.driver.reflex.ReflexActionSendProtocol;
import com.iris.driver.reflex.ReflexActionZigbeeIasZoneEnroll;
import com.iris.driver.reflex.ReflexMatchAlertmeLifesign;
import com.iris.driver.reflex.ReflexMatchLifecycle;
import com.iris.driver.reflex.ReflexMatchRegex;
import com.iris.driver.reflex.ReflexMatchZigbeeAttribute;
import com.iris.driver.reflex.ReflexMatchZigbeeIasZoneStatus;
import com.iris.driver.reflex.ReflexRunMode;
import com.iris.protoc.runtime.ProtocStruct;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.IasZone;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import groovy.lang.ReadOnlyPropertyException;

public class ZigbeeConfigContext extends GroovyObjectSupport {
   private static final Logger LOGGER = LoggerFactory.getLogger(ZigbeeConfigContext.class);
   private static final Data data = new Data();

   private long offlineTimeout = Long.MAX_VALUE;
   private List<ReflexContext> reflexes = new ArrayList<>(1);
   private ZigbeeConfigurationContext configuration;

   public ZigbeeConfigContext() {
   }

   public void processReflexes(DriverBinding binding) {
      for (ReflexContext context : reflexes) {
         binding.getBuilder().addReflexDefinition(context.getDefinition());
      }

      if (offlineTimeout != Long.MAX_VALUE) {
         binding.getBuilder().withOfflineTimeout(offlineTimeout);
      }
   }

   public void processConfiguration(DriverBinding binding) {
      if (configuration != null) {
         binding.getBuilder().addConfigurationStateMachine(configuration.getConfigurationStateMachine());
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Zigbee Configuration State Machine
   /////////////////////////////////////////////////////////////////////////////
   
   public void configure(Closure<?> closure) {
      if (configuration != null) {
         GroovyValidator.error("a driver can only have one zigbee configuration block");
         return;
      }

      this.configuration = new ZigbeeConfigurationContext();
      closure.setDelegate(this.configuration);
      closure.call();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Configuration of Zigbee offline timeouts
   /////////////////////////////////////////////////////////////////////////////
   
   public void offlineTimeout(long seconds) {
      this.offlineTimeout = (seconds > 0) ? seconds : Long.MAX_VALUE;
   }
   
   public void offlineTimeout(long timeout, TimeUnit unit) {
      long secs = unit.toSeconds(timeout);
      if (timeout != 0 && secs == 0) secs = 1;
      offlineTimeout(secs);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Configuration of hub local reflexes
   /////////////////////////////////////////////////////////////////////////////
   
   public void builtin(ReflexNoClosure reflex) {
      reflexes.add(new BuiltinReflexContext());
   }

   public static final class BuiltinReflexContext extends ReflexContext {
      public BuiltinReflexContext() {
         actions.add(new ReflexActionBuiltin());
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Common Zigbee actions
   /////////////////////////////////////////////////////////////////////////////

   public void bind(ReflexMatchContext ctx, Map<String,Object> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.bind(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void bind(ReflexMatchContext ctx, List<Map<String,Object>> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.bind(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void report(ReflexMatchContext ctx, Map<String,Object> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.report(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void report(ReflexMatchContext ctx, List<Map<String,Object>> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.report(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void write(ReflexMatchContext ctx, Map<String,Object> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.write(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void write(ReflexMatchContext ctx, List<Map<String,Object>> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.write(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void read(ReflexMatchContext ctx, Map<String,Object> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.read(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void read(ReflexMatchContext ctx, List<Map<String,Object>> config) {
      ctx.getProtocolSendProcessor().process(
         ZigbeeReflex.INSTANCE.read(config),
         ImmutableMap.<String,Object>of()
      );
   }

   public void iaszone(ReflexMatchContext ctx, Map<String,Object> config, IasZoneTypes type) {
      int ep = 1;
      int pr = ZigbeeNaming.HA_PROFILE_ID;
      int cl = IasZone.CLUSTER_ID & 0xFFFF;

      if (config.containsKey("endpoint")) {
         ep = ((Number)config.get("endpoint")).intValue();
      }

      if (config.containsKey("profile")) {
         pr = ((Number)config.get("profile")).intValue();
      }

      if (config.containsKey("cluster")) {
         cl = ((Number)config.get("cluster")).intValue();
      }

      switch (type) {
      case ENROLL:
         ctx.addAction(new ReflexActionZigbeeIasZoneEnroll(ep,pr,cl));
         break;

      default:
         throw new MissingMethodException("iaszone", getClass(), new Object[] { type });
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Configuration of Zigbee polling
   /////////////////////////////////////////////////////////////////////////////
   
   public void poll(ReflexAndClosure reflex) {
      ReflexContext ctx = new PollReflexContext();
      reflexes.add(ctx);

      reflex.config.setDelegate(ctx);
      reflex.config.call();
   }

   public final class PollReflexContext extends ReflexMatchContext {
      public void on(ReflexMatchLifecycle.Type when) {
         addMatch(new ReflexMatchLifecycle(when));
      }

      public void bind(Map<String,Object> config) {
         ZigbeeConfigContext.this.bind(this,config);
      }

      public void bind(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.bind(this,config);
      }

      public void report(Map<String,Object> config) {
         ZigbeeConfigContext.this.report(this,config);
      }

      public void report(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.report(this,config);
      }

      public void write(Map<String,Object> config) {
         ZigbeeConfigContext.this.write(this,config);
      }

      public void write(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.write(this,config);
      }

      public void read(Map<String,Object> config) {
         ZigbeeConfigContext.this.read(this,config);
      }

      public void read(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.read(this,config);
      }

      public void iaszone(IasZoneTypes type) {
         ZigbeeConfigContext.this.iaszone(this,ImmutableMap.<String,Object>of(), type);
      }

      public void iaszone(Map<String,Object> config, IasZoneTypes type) {
         ZigbeeConfigContext.this.iaszone(this,config,type);
      }

      @Override
      public Object getProperty(String name) {
         if (name == null) {
            return super.getProperty(name);
         }

         switch (name) {
         case "bind":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  bind(config);
               }
            };
         case "read":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  read(config);
               }
            };
         case "write":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  write(config);
               }
            };
         case "report":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  report(config);
               }
            };
         default:
            // fall through
            break;
         }

         Object prot = ZigbeeConfigProtocol.getProtocolProperty(name);
         if (prot != null) {
            return prot;
         }

         try {
            return IasZoneTypes.valueOf(name.toUpperCase());
         } catch (IllegalArgumentException ex) {
            // ignore
         }

         try {
            return ReflexMatchLifecycle.Type.valueOf(name.toUpperCase());
         } catch (IllegalArgumentException ex) {
            // ignore
         }

         return super.getProperty(name);
      }

      @Override
      public ProtocolClosureProcessor getProtocolSendProcessor() {
         return new ZigbeeReflex.ZigbeeSendProcessor() {
            @Override
            public void processSendCommand(ProtocStruct command) {
               try {
                  addAction(new ReflexActionSendProtocol(ReflexActionSendProtocol.Type.ZIGBEE, command.toBytes(ByteOrder.LITTLE_ENDIAN)));
               } catch (Exception ex) {
                  GroovyValidator.error("could not process zigbee send", ex);
               }
            }
         };
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Configuration of Zigbee polling
   /////////////////////////////////////////////////////////////////////////////
   
   public void match(ReflexAndClosure reflex) {
      ReflexContext ctx = new MatchReflexContext();
      reflexes.add(ctx);

      reflex.config.setDelegate(ctx);
      reflex.config.call();
   }

   private static final Map<String,Integer> IASZONE_STATUS_BITS = ImmutableMap.<String,Integer>builder()
      .put("alarm1", 0)
      .put("alarm2", 1)
      .put("tamper", 2)
      .put("battery", 3)
      .put("supervisionReports", 4)
      .put("supervision", 4) // short name for supervisionReports
      .put("restoreReports", 5)
      .put("restore", 5) // short name for restoreReports
      .put("trouble", 6)
      .put("failure", 6) // alternative name for trouble
      .put("ac", 7)
      .put("mains", 7) // alternative name for ac
      .put("test", 8)
      .put("batteryDefect", 9)
      .build();

   private static final Map<String,Integer> ALERTME_SWITCH_BITS = ImmutableMap.<String,Integer>builder()
      .put("main", 0)
      .put("sensor", 0) // alternative name for main
      .put("tamper", 1)
      .build();

   public final class MatchReflexContext extends ReflexForwardContext {
      private int getAsBitset(String entry, Object value, Map<String,Integer> names) {
         if (value instanceof Number) {
            return (((Number)value).intValue());
         } else if (value instanceof Collection) {
            int val = 0;
            Collection<String> cvalue = (Collection<String>)value;
            for (String name : cvalue) {
               if (name == null) {
                  GroovyValidator.error("null bit position for zigbee match " + entry);
                  continue;
               }

               Integer pos = null;
               if (name.startsWith("bit")) {
                  try {
                     pos = Integer.valueOf(name.substring("bit".length()));
                  } catch (NumberFormatException ex) {
                     // ignore
                  }
               }
               
               if (pos == null) {
                  pos = names.get(name);
               }

               if (pos == null) {
                  GroovyValidator.error("unknown bit position '" + name + "' for zigbee match " + entry);
                  continue;
               }

               val |= (1 << pos);
            }

            return val;
         } else {
            GroovyValidator.error("unknown data type for zigbee match " + entry);
            return 0;
         }
      }

      public void on(ReflexMatchZigbeeIasZoneStatus.Type iaszone) {
         on(ImmutableMap.<String,Object>of(), iaszone);
      }

      public void on(Map<String,Object> config, ReflexMatchZigbeeIasZoneStatus.Type iaszone) {
         // Default options are standard HA IAS Zone on endpoint 1
         // matching any report with any change delay.
         int pro = ZigbeeNaming.HA_PROFILE_ID;
         int cls = IasZone.CLUSTER_ID & 0xFFFF;
         int setMask = -1;
         int clrMask = -1;
         int maxDelay = -1;
         Integer end = null;
         Integer manuf = null;
         Integer flags = null;

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
               GroovyValidator.error("unknown data type for zigbee match iaszone 'cluster': " + ocls);
            }
         }

         if (config.containsKey("set")) {
            setMask = getAsBitset("ias zone 'set'", config.get("set"), IASZONE_STATUS_BITS);
         }

         if (config.containsKey("clear")) {
            clrMask = getAsBitset("ias zone 'clear'", config.get("clear"), IASZONE_STATUS_BITS);
         }

         if (config.containsKey("maxDelay")) {
            Object md = config.get("maxDelay");
            if (md instanceof Number) {
               maxDelay = (((Number)md).intValue());
               if (maxDelay < 0) {
                  maxDelay = -1;
               }
            } else {
               GroovyValidator.error("unknown data type for zigbee match ias zone 'maxDelay'");
            }
         }

         if (end == null) {
            end = 1;
         }

         if (config.containsKey("manufacturer")) {
            manuf = ((Number)config.get("manufacturer")).intValue();
            flags = ((flags == null) ? 0 : flags) |ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
         }

         if (config.containsKey("clusterSpecific")) {
            Boolean clssp = ((Boolean)config.get("clusterSpecific"));
            flags = ((flags == null) ? 0 : flags) | (Boolean.TRUE.equals(clssp) ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0);
         }

         if (config.containsKey("fromServer")) {
            Boolean fsv = ((Boolean)config.get("fromServer"));
            flags = ((flags == null) ? 0 : flags) | (Boolean.TRUE.equals(fsv) ? ZigbeeMessage.Zcl.FROM_SERVER : 0);
         }

         if (config.containsKey("disableDefaultResponse")) {
            Boolean ddr = ((Boolean)config.get("disableDefaultResponse"));
            flags = ((flags == null) ? 0 : flags) | (Boolean.TRUE.equals(ddr) ? ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE : 0);
         }

         addMatch(new ReflexMatchZigbeeIasZoneStatus(iaszone, pro, end, cls, setMask, clrMask, maxDelay, manuf, flags));
      }

      public void on(AlertmeTypes ame) {
         on(ImmutableMap.<String,Object>of(), ame);
      }

      public void on(Map<String,Object> config, AlertmeTypes ame) {
         switch (ame) {
         case LIFESIGN:
            onAlertmeLifesign(config);
            break;

         default:
            throw new MissingMethodException("on", getClass(), new Object[] {config, ame});
         }
      }

      private void onAlertmeLifesign(Map<String,Object> config) {
         // Default options are AlertMe Profile, General Cluster on Endpoint 2
         // matching any report.
         int pro = ZigbeeNaming.AME_PROFILE_ID;
         int cls = com.iris.protocol.zigbee.alertme.AMGeneral.CLUSTER_ID & 0xFFFF;
         int setMask = -1;
         int clrMask = -1;
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
               GroovyValidator.error("unknown data type for zigbee match alertme lifesign 'cluster': " + ocls);
            }
         }

         if (config.containsKey("set")) {
            setMask = getAsBitset("alertme lifesign 'set'", config.get("set"), ALERTME_SWITCH_BITS);
         }

         if (config.containsKey("clear")) {
            clrMask = getAsBitset("alertme lifesign 'clear'", config.get("clear"), ALERTME_SWITCH_BITS);
         }

         if (end == null) {
            end = 2;
         }

         addMatch(new ReflexMatchAlertmeLifesign(pro, end, cls, setMask, clrMask));
      }

      public void on(Map<String,Object> config, ReflexMatchZigbeeAttribute.Type report) {
         int pro = ZigbeeNaming.HA_PROFILE_ID;
         Integer cls = null;
         Integer end = null;
         Integer attr = null;
         ZclData value = null;
         Integer manuf = null;
         Integer flags = null;

         if (config.containsKey("attr")) {
            attr = ((Number)config.get("attr")).intValue();
         } else {
            GroovyValidator.error("zigbee match attribute must define 'attr'");
         }

         if (config.containsKey("value")) {
            value = ((ZclData)config.get("value"));
         }

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
               GroovyValidator.error("unknown data type for zigbee match attribute 'cluster': " + ocls);
            }
         } else {
            GroovyValidator.error("zigbee match attribute must define 'cluster'");
         }

         if (end == null) {
            GroovyValidator.error("zigbee match attribute must define 'endpoint'");
         }

         if (config.containsKey("manufacturer")) {
            manuf = ((Number)config.get("manufacturer")).intValue();
            flags = ((flags == null) ? 0 : flags) |ZigbeeMessage.Zcl.MANUFACTURER_SPECIFIC;
         }

         if (config.containsKey("clusterSpecific")) {
            Boolean clssp = ((Boolean)config.get("clusterSpecific"));
            flags = ((flags == null) ? 0 : flags) | (Boolean.TRUE.equals(clssp) ? ZigbeeMessage.Zcl.CLUSTER_SPECIFIC : 0);
         }

         if (config.containsKey("fromServer")) {
            Boolean fsv = ((Boolean)config.get("fromServer"));
            flags = ((flags == null) ? 0 : flags) | (Boolean.TRUE.equals(fsv) ? ZigbeeMessage.Zcl.FROM_SERVER : 0);
         }

         if (config.containsKey("disableDefaultResponse")) {
            Boolean ddr = ((Boolean)config.get("disableDefaultResponse"));
            flags = ((flags == null) ? 0 : flags) | (Boolean.TRUE.equals(ddr) ? ZigbeeMessage.Zcl.DISABLE_DEFAULT_RESPONSE : 0);
         }

         if (cls != null && end != null && attr != null) {
            addMatch(new ReflexMatchZigbeeAttribute(report, pro, end, cls, attr, value, manuf, flags));
         }
      }

      public void bind(Map<String,Object> config) {
         ZigbeeConfigContext.this.bind(this,config);
      }

      public void bind(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.bind(this,config);
      }

      public void report(Map<String,Object> config) {
         ZigbeeConfigContext.this.report(this,config);
      }

      public void report(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.report(this,config);
      }

      public void write(Map<String,Object> config) {
         ZigbeeConfigContext.this.write(this,config);
      }

      public void write(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.write(this,config);
      }

      public void read(Map<String,Object> config) {
         ZigbeeConfigContext.this.read(this,config);
      }

      public void read(List<Map<String,Object>> config) {
         ZigbeeConfigContext.this.read(this,config);
      }

      public void iaszone(IasZoneTypes type) {
         ZigbeeConfigContext.this.iaszone(this,ImmutableMap.<String,Object>of(), type);
      }

      public void iaszone(Map<String,Object> config, IasZoneTypes type) {
         ZigbeeConfigContext.this.iaszone(this,config,type);
      }

      public void amlifesign(ReflexNames name) {
         amlifesign(ImmutableMap.<String,Object>of(), name);
      }

      public void amlifesign(Map<String,Object> config, ReflexNames name) {
         switch (name) {
         case BATTERY:
            double min = 0.0;
            double nom = 0.0;
            if (config.containsKey("minimumVolts")) {
               min = ((Number)config.get("minimumVolts")).doubleValue();
            } else {
               GroovyValidator.error("alertme lifesign action must define 'minimumVolts'");
            }

            if (config.containsKey("nominalVolts")) {
               nom = ((Number)config.get("nominalVolts")).doubleValue();
            } else {
               GroovyValidator.error("alertme lifesign action must define 'nominalVolts'");
            }

            Set<String> unknown = new HashSet<>(config.keySet());
            unknown.remove("minimumVolts");
            unknown.remove("nominalVolts");

            if (!unknown.isEmpty()) {
               GroovyValidator.error("unrecognized alertme lifesign action configuration: " + unknown);
            } else {
               this.addAction(new ReflexActionAlertmeLifesign(ReflexActionAlertmeLifesign.Type.BATTERY, min, nom));
            }
            break;

         case SIGNAL:
            if (!config.isEmpty()) {
               GroovyValidator.error("unrecognized alertme lifesign action configuration: " + config.keySet());
            } else {
               this.addAction(new ReflexActionAlertmeLifesign(ReflexActionAlertmeLifesign.Type.SIGNAL));
            }
            break;

         case TEMPERATURE:
            if (!config.isEmpty()) {
               GroovyValidator.error("unrecognized alertme lifesign action configuration: " + config.keySet());
            } else {
               this.addAction(new ReflexActionAlertmeLifesign(ReflexActionAlertmeLifesign.Type.TEMPERATURE));
            }
            break;

         default:
            throw new MissingMethodException("amlifesign", getClass(), new Object[] { config, name });
         }
      }

      public void amlifesign(GroovyCapabilityDefinition cap) {
         amlifesign(ImmutableMap.<String,Object>of(), cap);
      }

      public void amlifesign(Map<String,Object> config, GroovyCapabilityDefinition cap) {
         switch (cap.getNamespace()) {
         case "temp":
            amlifesign(config, ReflexNames.TEMPERATURE);
            break;

         default:
            throw new MissingMethodException("amlifesign", getClass(), new Object[] { config, cap });
         }
      }

      @Override
      public Object getProperty(String name) {
         if (name == null) {
            return super.getProperty(name);
         }

         switch (name) {
         case "_":
            return ReflexUtil.WILDCARD;
         case "attr":
            return ReflexMatchZigbeeAttribute.Type.BOTH;
         case "readattr":
            return ReflexMatchZigbeeAttribute.Type.READ;
         case "reportattr":
            return ReflexMatchZigbeeAttribute.Type.REPORT;
         case "iaszoneattr":
            return ReflexMatchZigbeeIasZoneStatus.Type.ATTR;
         case "iaszonenotif":
            return ReflexMatchZigbeeIasZoneStatus.Type.NOTIFICATION;
         case "iaszone":
            return ReflexMatchZigbeeIasZoneStatus.Type.BOTH;
         case "amlifesign":
            return AlertmeTypes.LIFESIGN;
         case "battery":
            return ReflexNames.BATTERY;
         case "signal":
            return ReflexNames.SIGNAL;
         case "bind":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  bind(config);
               }
            };
         case "read":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  read(config);

               }
            };
         case "write":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  write(config);
               }
            };
         case "report":
            return new Closure<Object>(this) {
               protected void getAt(List<Map<String,Object>> config) {
                  report(config);
               }
            };
         default:
            // fall through
            break;
         }

         Object prot = ZigbeeConfigProtocol.getProtocolProperty(name);
         if (prot != null) {
            return prot;
         }

         try {
            return IasZoneTypes.valueOf(name.toUpperCase());
         } catch (IllegalArgumentException ex) {
            // ignore
         }

         return super.getProperty(name);
      }

      @Override
      public ProtocolClosureProcessor getProtocolMatchProcessor() {
         return new ZigbeeReflex.ZigbeeMatchProcessor() {
            @Override
            public void processMatchString(String match) {
               addMatch(new ReflexMatchRegex(match));
            }
         };
      }

      @Override
      public ProtocolClosureProcessor getProtocolSendProcessor() {
         return new ZigbeeReflex.ZigbeeSendProcessor() {
            @Override
            public void processSendCommand(ProtocStruct command) {
               try {
                  addAction(new ReflexActionSendProtocol(ReflexActionSendProtocol.Type.ZIGBEE, command.toBytes(ByteOrder.LITTLE_ENDIAN)));
               } catch (Exception ex) {
                  GroovyValidator.error("could not process zigbee send", ex);
               }
            }
         };
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Configuration of hub local reflexes
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public Object getProperty(String property) {
      switch (property) {
      case "reflex":
         return ReflexNoClosure.INSTANCE;
      case "Data":
         return data;
      default:
         return super.getProperty(property);
      }
   }

   @Override
   public void setProperty(String property, Object newValue) {
      switch (property) {
      case "reflex":
         throw new ReadOnlyPropertyException("reflex", getClass());
      case "Data":
         throw new ReadOnlyPropertyException("reflex", getClass());

      default:
         super.setProperty(property, newValue);
         break;
      }
   }
   
   public Object reflex(Closure<?> closure) {
      return new ReflexAndClosure(closure);
   }

   public static enum AlertmeTypes {
      LIFESIGN
   }

   public static enum IasZoneTypes {
      ENROLL
   }

   public static enum ReflexNoClosure {
      INSTANCE;
   }

   public static enum ReflexNames {
      BATTERY,
      SIGNAL,
      TEMPERATURE;
   }

   public static final class ReflexAndClosure {
      private final Closure<?> config;

      public ReflexAndClosure(Closure<?> config) {
         this.config = config;
      }
   }
}

