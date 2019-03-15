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
package com.iris.driver.groovy.zwave;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition.CapabilityHandlerContext;
import com.iris.driver.groovy.context.SetAttributesHandlerDefinition;
import com.iris.protocol.zwave.model.ZWaveCommand;

public class ZWaveActionContext extends Closure<Object> {
   private final ZWaveContext parent;

   public ZWaveActionContext(Object owner, ZWaveContext parent) {
      super(owner);
      this.parent = parent;
   }

   @Override
   public Object getProperty(String propertyName) {
      Object commandClass = parent.getCommandClassProperty(propertyName);
      if (commandClass != null) {
         return commandClass;
      }

      return super.getProperty(propertyName);
   }

   public void send(Map<String,Object> args, ZWaveContext.CommandClosure cmd) {
      GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this).addAction(
         new ZWaveSendAction(cmd.getZWaveCommand(), args)
      );
   }

   public void send(ZWaveContext.CommandClosure cmd) {
      GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this).addAction(
         new ZWaveSendAction(cmd.getZWaveCommand(), ImmutableMap.<String,Object>of())
      );
   }

   public void ordered(Closure<?> closure) {
      ZWaveSendOrdered ctx = new ZWaveSendOrdered(GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this));
      closure.setResolveStrategy(Closure.DELEGATE_FIRST);
      closure.setDelegate(ctx);
      closure.call();
   }

   private static ZWaveCommand parseZWaveCommandDefinition(ZWaveCommand cmd, Map<String,Object> values) {
      boolean isSend = true;
      List<String> names = cmd.getSendNames();
      if (names == null || names.isEmpty()) {
         names = cmd.getReceiveNames();
         isSend = names.isEmpty();
      }

      ZWaveCommand copy = new ZWaveCommand(cmd);
      for (String name : names) {
         Object value = values.get(name);
         if (value == null) {
            GroovyValidator.error("zwave send action must define value for '" + name + "'");
            continue;
         }

         if (!(value instanceof Number)) {
            GroovyValidator.error("zwave send action values must be 8-bit numbers");
         }

         Number num = (Number)value;
         long lvalue = num.longValue();
         long ltst = Math.abs(lvalue);
         
         GroovyValidator.assertTrue(ltst < 256, "zwave send action values cannot be larger than 8-bits");
         if (isSend) {
            copy.setSend(name, (byte)lvalue);
         } else {
            copy.setRecv(name, (byte)lvalue);
         }
      }

      Set<String> unmatched = new HashSet<>(values.keySet());
      unmatched.removeAll(names);
      GroovyValidator.assertTrue(unmatched.isEmpty(), "zwave poll specifies unknown values: " + unmatched);

      return copy;
   }

   public static final class ZWaveSendOrdered extends GroovyObjectSupport {
      private final ZWaveOrderedSendAction ordered;

      public ZWaveSendOrdered(CapabilityHandlerContext ctx) {
         this.ordered = new ZWaveOrderedSendAction();
         ctx.addAction(this.ordered);
      }

      public void send(Map<String,Object> args, ZWaveContext.CommandClosure cmd) {
         ordered.add(parseZWaveCommandDefinition(cmd.getZWaveCommand(), args));
      }

      public void send(ZWaveContext.CommandClosure cmd) {
         ordered.add(parseZWaveCommandDefinition(cmd.getZWaveCommand(), ImmutableMap.<String,Object>of()));
      }
   }

   private static final class ZWaveOrderedSendAction implements SetAttributesHandlerDefinition.Action {
      private final List<ZWaveCommand> cmds = new ArrayList<>();

      void add(ZWaveCommand cmd) {
         cmds.add(cmd);
      }

      @Override
      public void run(DeviceDriverContext context, Object value) {
         ZWaveContext.doSendZWaveCommandsOrdered(context, cmds);
      }
   }

   private static final class ZWaveSendAction implements SetAttributesHandlerDefinition.Action {
      private final ZWaveCommand cmd;
      
      private ZWaveSendAction(ZWaveCommand cmd, Map<String,Object> values) {
         this.cmd = parseZWaveCommandDefinition(cmd, values); 
      }

      @Override
      public void run(DeviceDriverContext context, Object value) {
         ZWaveContext.doSendZWaveCommand(context, cmd);
      }
   }
}

