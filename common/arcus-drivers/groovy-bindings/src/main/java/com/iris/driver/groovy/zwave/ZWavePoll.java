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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

public enum ZWavePoll {
   INSTANCE;

   private final Map<String,ZWaveCommandClassContextFactory> factories;

   private ZWavePoll() {
      this.factories = convertToGroovy(getAllZWaveCommandClasses());
   }

   public ZWaveCommandClassContext create(String name, PollProcessor processor) {
      ZWaveCommandClassContextFactory ccFactory = factories.get(name);
      if (ccFactory == null) {
         return null;
      }

      return ccFactory.create(processor);
   }

   private static List<ZWaveCommandClass> getAllZWaveCommandClasses() {
      ZWaveAllCommandClasses.init();
      
      List<ZWaveCommandClass> all = ZWaveUtil.getCommandClasses().commandClasses;
      if (all == null || all.isEmpty()) {
         throw new RuntimeException("could not load zwave command classes");
      }

      return all;
   }

   private static Map<String,ZWaveCommandClassContextFactory> convertToGroovy(List<ZWaveCommandClass> commandClasses) {
      ImmutableMap.Builder<String,ZWaveCommandClassContextFactory> builder = ImmutableMap.builder();
      for (ZWaveCommandClass commandClass : commandClasses) {
         builder.put(ZWaveCommandClass.scrub(commandClass.name), new ZWaveCommandClassContextFactory(commandClass));
      }

      return builder.build();
   }

   public static class ZWaveCommandClassContextFactory {
      private final ZWaveCommandClass commandClass;
      private final Map<String,ZWaveCommandContextFactory> factories;

      ZWaveCommandClassContextFactory(ZWaveCommandClass commandClass) {
         this.commandClass = commandClass;
         this.factories = convertToGroovy(commandClass.commandsByName.values());
      }

      private Map<String,ZWaveCommandContextFactory> convertToGroovy(Collection<ZWaveCommand> commands) {
         if(commands == null || commands.isEmpty()) {
            return ImmutableMap.of();
         }

         ImmutableMap.Builder<String,ZWaveCommandContextFactory> builder = ImmutableMap.builder();
         for (ZWaveCommand command : commands) {
            builder.put(command.commandName, new ZWaveCommandContextFactory(command));
         }

         return builder.build();
      }

      public ZWaveCommandClassContext create(PollProcessor processor) {
         return new ZWaveCommandClassContext(commandClass, factories, processor);
      }
   }

   public static class ZWaveCommandContextFactory {
      private final ZWaveCommand command;

      ZWaveCommandContextFactory(ZWaveCommand command) {
         this.command = command;
      }

      public ZWaveCommandContext create(ZWaveCommandClassContext parent, PollProcessor processor) {
         return new ZWaveCommandContext(parent, command, processor);
      }
   }

   public static class ZWaveCommandClassContext extends GroovyObjectSupport {
      private final Map<String,ZWaveCommandContextFactory> factories;
      private final PollProcessor processor;
      private final ZWaveCommandClass commandClass;

      ZWaveCommandClassContext(ZWaveCommandClass commandClass, Map<String,ZWaveCommandContextFactory> factories, PollProcessor processor) {
         this.commandClass = commandClass;
         this.factories = factories;
         this.processor = processor;
      }

      @Override
      public Object getProperty(String name) {
         ZWaveCommandContextFactory factory = factories.get(name);
         if(factory != null) {
            return factory.create(this,processor);
         }
         
         return super.getProperty(name);
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         if (args instanceof Object[]) {
            Object[] match = (Object[])args;
            if (match.length == 1 && match[0] instanceof Map) {
               ZWaveCommandContextFactory factory = factories.get(name);
               if (factory != null) {
                  return factory.create(this,processor);
               }
            }
         }

         return super.invokeMethod(name, args);
      }
   }

   public static class ZWaveCommandContext extends Closure<Object> {
      private static final long serialVersionUID = -3761204101658134242L;

      private final ZWaveCommandClass commandClass;
      private final ZWaveCommand command;
      private final PollProcessor processor;

      ZWaveCommandContext(ZWaveCommandClassContext parent, ZWaveCommand command, PollProcessor processor) {
         super(parent);
         this.commandClass = parent.commandClass;
         this.command = command;
         this.processor = processor;
      }

      ZWaveCommand getZWaveCommand() {
         return command;
      }

      protected void doCall() {
         addPoll(ImmutableMap.<String,Object>of());
      }

      protected void doCall(Map<String,Object> args) {
         addPoll(args);
      }

      private void addPoll(Map<String,Object> values) {
         boolean isSend = true;
         ArrayList<String> names = command.getSendNames();
         if (names == null || names.isEmpty()) {
            names = command.getReceiveNames();
            isSend = names.isEmpty();
         }

         ZWaveCommand pollCommand = new ZWaveCommand(command);
         for (String name : names) {
            Object value = values.get(name);
            if (value == null) {
               throw new RuntimeException("zwave poll must define value for '" + name + "'");
            }

            if (!(value instanceof Number)) {
               throw new RuntimeException("zwave poll values must be 8-bit numbers");
            }

            Number num = (Number)value;
            long lvalue = num.longValue();
            long ltst = Math.abs(lvalue);
            
            if (ltst >= 256) {
               throw new RuntimeException("zwave poll values cannot be larger than 8-bits");
            }

            if (isSend) {
               pollCommand.setSend(name, (byte)lvalue);
            } else {
               pollCommand.setRecv(name, (byte)lvalue);
            }
         }

         processor.addPollCommand(pollCommand);
      }
   }

   public static interface PollProcessor {
      void addPollCommand(ZWaveCommand command);
   }
}

