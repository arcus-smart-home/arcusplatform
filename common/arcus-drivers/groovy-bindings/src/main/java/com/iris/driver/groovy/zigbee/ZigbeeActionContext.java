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
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.context.CapabilityHandlerDefinition;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition.CapabilityHandlerContext;
import com.iris.driver.groovy.context.SetAttributesHandlerDefinition;
import com.iris.protoc.runtime.ProtocMessage;
import com.iris.protoc.runtime.ProtocStruct;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.IasZone;

public class ZigbeeActionContext extends Closure<Object> {
   private final ZigbeeContext parent;
   private final Data data = new Data();

   public ZigbeeActionContext(Object owner, ZigbeeContext parent) {
      super(owner);
      this.parent = parent;
   }

   @Override
   public Object getProperty(String name) {
      if (name == null) {
         return super.getProperty(name);
      }

      switch (name) {
      case "Data":
         return data;
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
         return ZigbeeConfigContext.IasZoneTypes.valueOf(name.toUpperCase());
      } catch (IllegalArgumentException ex) {
         // ignore
      }

      return super.getProperty(name);
   }

   public void bind(Map<String,Object> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.bind(config)));
   }

   public void bind(List<Map<String,Object>> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.bind(config)));
   }

   public void report(List<Map<String,Object>> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.report(config)));
   }

   public void report(Map<String,Object> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.report(config)));
   }

   public void write(List<Map<String,Object>> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.write(config)));
   }

   public void write(Map<String,Object> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.write(config)));
   }

   public void read(List<Map<String,Object>> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.read(config)));
   }

   public void read(Map<String,Object> config) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new ZigbeeSendAction(ZigbeeReflex.INSTANCE.read(config)));
   }

   public void iaszone(ZigbeeConfigContext.IasZoneTypes type) {
      iaszone(ImmutableMap.<String,Object>of(), type);
   }

   public void iaszone(Map<String,Object> config, ZigbeeConfigContext.IasZoneTypes type) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      switch (type) {
      case ENROLL:
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

         ctx.addAction(new ZigbeeSendAction(ZigbeeMessage.IasZoneEnroll.builder()
            .setEndpoint(ep)
            .setProfile(pr)
            .setCluster(cl)
            .create()
         ));
         break;

      default:
         throw new MissingMethodException("iaszone", getClass(), new Object[] { type });
      } 
   }

   public void send(Map<String,Object> args, ZigbeeConfigProtocol.ZigbeeFieldNaming msg) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ZigbeeReflex.ZigbeeSendProcessor proc = new ZigbeeReflex.ZigbeeSendProcessor() {
         @Override
         public void processSendCommand(ProtocStruct msg) {
            ctx.addAction(new ZigbeeSendAction(msg));
         }
      };

      msg.call(proc, args);
   }

   public void send(ZigbeeConfigProtocol.ZigbeeFieldNaming msg) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ZigbeeReflex.ZigbeeSendProcessor proc = new ZigbeeReflex.ZigbeeSendProcessor() {
         @Override
         public void processSendCommand(ProtocStruct msg) {
            ctx.addAction(new ZigbeeSendAction(msg));
         }
      };

      msg.call(proc);
   }

   public void send(Map<String,Object> args, GroovyObject obj) {
      final CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ZigbeeReflex.ZigbeeSendProcessor proc = new ZigbeeReflex.ZigbeeSendProcessor() {
         @Override
         public void processSendCommand(ProtocStruct msg) {
            ctx.addAction(new ZigbeeSendAction(msg));
         }
      };

      if (obj instanceof Closure<?>) {
         ((Closure<?>)obj).call(proc,args);
      } else {
         obj.invokeMethod("send", new Object[] {proc,args});
      }
   }

   public void ordered(Closure<?> closure) {
      ZigbeeSendOrdered ctx = new ZigbeeSendOrdered(GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this));
      closure.setResolveStrategy(Closure.DELEGATE_FIRST);
      closure.setDelegate(ctx);
      closure.call();
   }

   public static final class ZigbeeSendOrdered extends GroovyObjectSupport {
      private final ZigbeeOrderedSendAction ordered;

      public ZigbeeSendOrdered(CapabilityHandlerContext ctx) {
         this.ordered = new ZigbeeOrderedSendAction();
         ctx.addAction(this.ordered);
      }

      @Override
      public Object getProperty(String name) {
         if (name == null) {
            return super.getProperty(name);
         }

         switch (name) {
         case "bind":
            throw new MissingPropertyException(name, getClass());
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

         return super.getProperty(name);
      }

      public void bind(Map<String,Object> config) {
         throw new MissingMethodException("bind", getClass(), new Object[] { config });
      }

      public void bind(List<Map<String,Object>> config) {
         throw new MissingMethodException("bind", getClass(), new Object[] { config });
      }

      public void report(List<Map<String,Object>> config) {
         ordered.add(ZigbeeReflex.INSTANCE.report(config));
      }

      public void report(Map<String,Object> config) {
         ordered.add(ZigbeeReflex.INSTANCE.report(config));
      }

      public void write(List<Map<String,Object>> config) {
         ordered.add(ZigbeeReflex.INSTANCE.write(config));
      }

      public void write(Map<String,Object> config) {
         ordered.add(ZigbeeReflex.INSTANCE.write(config));
      }

      public void read(List<Map<String,Object>> config) {
         ordered.add(ZigbeeReflex.INSTANCE.read(config));
      }

      public void read(Map<String,Object> config) {
         ordered.add(ZigbeeReflex.INSTANCE.read(config));
      }

      public void iaszone(ZigbeeConfigContext.IasZoneTypes type) {
         throw new MissingMethodException("iaszone", getClass(), new Object[] { type });
      }

      public void send(Map<String,Object> args, ZigbeeConfigProtocol.ZigbeeFieldNaming msg) {
         ZigbeeReflex.ZigbeeSendProcessor proc = new ZigbeeReflex.ZigbeeSendProcessor() {
            @Override
            public void processSendCommand(ProtocStruct msg) {
               ordered.add(msg);
            }
         };

         msg.call(proc, args);
      }

      public void send(ZigbeeConfigProtocol.ZigbeeFieldNaming msg) {
         ZigbeeReflex.ZigbeeSendProcessor proc = new ZigbeeReflex.ZigbeeSendProcessor() {
            @Override
            public void processSendCommand(ProtocStruct msg) {
               ordered.add(msg);
            }
         };

         msg.call(proc);
      }

      public void send(Map<String,Object> args, GroovyObject obj) {
         ZigbeeReflex.ZigbeeSendProcessor proc = new ZigbeeReflex.ZigbeeSendProcessor() {
            @Override
            public void processSendCommand(ProtocStruct msg) {
               ordered.add(msg);
            }
         };

         if (obj instanceof Closure<?>) {
            ((Closure<?>)obj).call(proc,args);
         } else {
            obj.invokeMethod("send", new Object[] {proc,args});
         }
      }
   }

   private static final class ZigbeeOrderedSendAction implements CapabilityHandlerDefinition.Action {
      private final List<ZigbeeMessage.Protocol> msgs = new ArrayList<>();

      void add(ProtocStruct msg) {
         if (msg instanceof ZigbeeMessage.Protocol) {
            add((ZigbeeMessage.Protocol)msg);
         } else if (msg instanceof ProtocMessage) {
            add((ProtocMessage)msg);
         } else {
            GroovyValidator.error("zigbee message cannot be ordered: " + msg);
         }
      }

      void add(ProtocMessage msg) {
         try {
            add(
               ZigbeeMessage.Protocol.builder()
                  .setType(msg.getMessageId())
                  .setPayload(ByteOrder.LITTLE_ENDIAN, msg)
                  .create()
            );
         } catch (Exception ex) {
            GroovyValidator.error("zigbee message cannot be ordered", ex);
         }
      }

      void add(ZigbeeMessage.Protocol msg) {
         msgs.add(msg);
      }

      @Override
      public void run(DeviceDriverContext context, Object value) {
         try {
            int i = 0;
            ZigbeeMessage.Protocol[] msgs = new ZigbeeMessage.Protocol[this.msgs.size()];
            for (ZigbeeMessage.Protocol msg : this.msgs) {
               msgs[i++] = msg;
            }

            ZigbeeMessage.Ordered msg = ZigbeeMessage.Ordered.builder()
               .setPayload(msgs)
               .create();

            ZigbeeMessageUtil.doSendMessage(context, msg);
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }
   }

   private static final class ZigbeeSendAction implements CapabilityHandlerDefinition.Action {
      private final ProtocMessage msg;
      
      private ZigbeeSendAction(ProtocStruct msg) {
         this((ProtocMessage)msg);
      }

      private ZigbeeSendAction(ProtocMessage msg) {
         this.msg = msg;
      }

      @Override
      public void run(DeviceDriverContext context, Object value) {
         ZigbeeMessageUtil.doSendMessage(context, msg);
      }
   }
}

