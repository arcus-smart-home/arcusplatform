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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.reflex.ReflexContext;
import com.iris.driver.groovy.reflex.ReflexMatchContext;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

public enum ZWaveReflex {
   INSTANCE;

   private final Map<String,ZWaveCommandClassContextFactory> factories;

   private ZWaveReflex() {
      this.factories = convertToGroovy(getAllZWaveCommandClasses());
   }

   /////////////////////////////////////////////////////////////////////////////
   // ZWave Send Action
   /////////////////////////////////////////////////////////////////////////////

   public static abstract class ZWaveSendProcessor implements ReflexMatchContext.ProtocolClosureProcessor {
      @Override
      public void process(Object cmd, Map<String,Object> values) {
         if (cmd instanceof ZWaveCommand) {
            processZWaveCommand((ZWaveCommand)cmd, values);
         } else if (cmd == ZWaveNodeInfo.INSTANCE) {
            processNodeInfo(values);
         } else {
            GroovyValidator.error("cannot process send: " + cmd);
         }
      }

      private void processNodeInfo(Map<String,Object> values) {
         GroovyValidator.error("zwave poll should not send node info messages");
      }

      private void processZWaveCommand(ZWaveCommand command, Map<String,Object> values) {
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
               GroovyValidator.error("zwave poll must define value for '" + name + "'");
               continue;
            }

            if (!(value instanceof Number)) {
               GroovyValidator.error("zwave poll values must be 8-bit numbers");
            }

            Number num = (Number)value;
            long lvalue = num.longValue();
            long ltst = Math.abs(lvalue);
            
            GroovyValidator.assertTrue(ltst < 256, "zwave poll values cannot be larger than 8-bits");
            if (isSend) {
               pollCommand.setSend(name, (byte)lvalue);
            } else {
               pollCommand.setRecv(name, (byte)lvalue);
            }
         }

         Set<String> unmatched = new HashSet<>(values.keySet());
         unmatched.removeAll(names);
         GroovyValidator.assertTrue(unmatched.isEmpty(), "zwave poll specifies unknown values: " + unmatched);

         processPollCommand(pollCommand);
      }

      protected abstract void processPollCommand(ZWaveCommand command);
   }

   /////////////////////////////////////////////////////////////////////////////
   // ZWave Regex Match
   /////////////////////////////////////////////////////////////////////////////

   public static abstract class ZWaveMatchProcessor implements ReflexMatchContext.ProtocolClosureProcessor {
      @Override
      public void process(Object cmd, Map<String,Object> values) {
         if (cmd instanceof ZWaveCommand) {
            processZWaveCommand((ZWaveCommand)cmd, values);
         } else if (cmd == ZWaveNodeInfo.INSTANCE) {
            processNodeInfo(values);
         } else if (cmd instanceof ProtocolMatch) {
            processMatch((ProtocolMatch)cmd);
         } else {
            GroovyValidator.error("cannot process match: " + cmd);
         }
      }

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

      public void processMatch(ProtocolMatch match) {
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
      }

      private void processZWaveCommand(ZWaveCommand command, Map<String,Object> values) {
         ArrayList<String> names = command.getSendNames();
         if (names == null || names.isEmpty()) {
            names = command.getReceiveNames();
         }

         // <TYPE> <LEN> <LEN> <LEN> <LEN> <NODE> <CCID> <CMDID> <LEN> <LEN> <LEN> <LEN> <PAYLOAD>...
         List<StringBuilder> regexMatches = new ArrayList<>();
         regexMatches.add(new StringBuilder());
         appendToRegexes(regexMatches, Protocol.Command.ID, 0);
         appendToRegexes(regexMatches, command.commandClass & 0xFF, 5);
         appendToRegexes(regexMatches, command.commandNumber & 0xFF, 0);

         int dots = 4;
         for (String name : names) {
            Object value = values.get(name);
            if (value == null) {
               dots++;
            } else if (value instanceof Number) {
               appendToRegexes(regexMatches, (Number)value, dots);
               dots = 0;
            } else if (value instanceof List) {
               appendToRegexes(regexMatches, (List<?>)value, dots);
               dots = 0;
            } else {
               GroovyValidator.error("zwave reflex matches can only use 8-bit numeric values or arrays of 8-bit numeric values: " + (value.getClass()));
            }
         }

         Set<String> unmatched = new HashSet<>(values.keySet());
         unmatched.removeAll(names);
         GroovyValidator.assertTrue(unmatched.isEmpty(), "zwave reflex match specifies unknown values: " + unmatched);

         for (StringBuilder regexMatch : regexMatches) {
            String match = regexMatch.toString();
            processMatchString(match);
         }
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

      private static void appendToRegexes(List<StringBuilder> regexes, int val, int dots) {
         for (StringBuilder regex : regexes) {
            appendValue(regex, val, dots);
         }
      }

      private static void appendToRegexes(List<StringBuilder> regexes, Number num, int dots) {
         for (StringBuilder regex : regexes) {
            appendToRegex(regex, num, dots);
         }
      }

      private static void appendToRegex(StringBuilder regex, Number num, int dots) {
         long lvalue = num.longValue();
         long ltst = Math.abs(lvalue);
         
         GroovyValidator.assertTrue(ltst < 256, "zwave reflex matches can only use 8-bit values");

         appendValue(regex, (int)(lvalue & 0xFF), dots);
      }

      private static void appendToRegexes(List<StringBuilder> regexes, List<?> values, int dots) {
         List<?> expanded = expandValueList(values);
         List<StringBuilder> updated = new ArrayList<>(regexes.size() * values.size());
         for (StringBuilder regex : regexes) {
            for (Object value : expanded) {
               StringBuilder updatedRegex = new StringBuilder(regex);

               if (!(value instanceof Number)) {
                  GroovyValidator.error("zwave reflex matches only support arrays of 8-bit numeric values: " + (value.getClass()));
                  continue;
               }

               appendToRegex(updatedRegex, (Number)value, dots);
               updated.add(updatedRegex);
            }
         }

         regexes.clear();
         regexes.addAll(updated);
      }

      private static void appendValue(StringBuilder regexMatch, int val, int dotsBeforeValue) {
         if (val < 16) appendValue(regexMatch, "0" + Integer.toHexString(val), dotsBeforeValue);
         else appendValue(regexMatch, Integer.toHexString(val), dotsBeforeValue);
      }

      private static int appendValue(StringBuilder regexMatch, String val, int dotsBeforeValue) {
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

      private static void space(StringBuilder regexMatch) {
         if (regexMatch.length() != 0) {
            regexMatch.append(" ");
         }
      }

      private static List<Object> expandValueList(Collection<?> values) {
         List<Object> result = new ArrayList<>();
         expandValueList(result, values);
         return result;
      }

      private static void expandValueList(List<Object> result, Collection<?> values) {
         for (Object next : values) {
            if (next instanceof Number) {
               result.add(next);
            } else if (next instanceof Collection) {
               expandValueList(result, (Collection<?>)next);
            } else {
               GroovyValidator.error("zwave reflex matches can only use 8-bit numeric values or arrays of 8-bit numeric values: " + (next.getClass()));
            }
         }
      }

      protected abstract void processMatchString(String match);
   }

   /////////////////////////////////////////////////////////////////////////////
   // DSL For ZWave Commands
   /////////////////////////////////////////////////////////////////////////////

   public ZWaveCommandClassContext create(String name) {
      ZWaveCommandClassContextFactory ccFactory = factories.get(name);
      if (ccFactory == null) {
         return null;
      }

      return ccFactory.create();
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
      private final Map<String,ZWaveCommandContextFactory> factories;

      ZWaveCommandClassContextFactory(ZWaveCommandClass commandClass) {
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

      public ZWaveCommandClassContext create() {
         return new ZWaveCommandClassContext(factories);
      }
   }

   public static class ZWaveCommandContextFactory {
      private final ZWaveCommand command;

      ZWaveCommandContextFactory(ZWaveCommand command) {
         this.command = command;
      }

      public ZWaveCommandContext create(ZWaveCommandClassContext parent) {
         return new ZWaveCommandContext(parent, command);
      }
   }

   public static class ZWaveCommandClassContext extends GroovyObjectSupport {
      private final Map<String,ZWaveCommandContextFactory> factories;

      ZWaveCommandClassContext(Map<String,ZWaveCommandContextFactory> factories) {
         this.factories = factories;
      }

      @Override
      public Object getProperty(String name) {
         ZWaveCommandContextFactory factory = factories.get(name);
         if(factory != null) {
            return factory.create(this);
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
                  return factory.create(this);
               }
            }
         }

         return super.invokeMethod(name, args);
      }
   }

   public static class ZWaveCommandContext extends Closure<Object> {
      private static final long serialVersionUID = -3761204101658134242L;
      private final ZWaveCommand command;

      ZWaveCommandContext(ZWaveCommandClassContext parent, ZWaveCommand command) {
         super(parent);
         this.command = command;
      }

      ZWaveCommand getZWaveCommand() {
         return command;
      }

      protected void doCall(ReflexMatchContext.ProtocolClosureProcessor processor) {
         processor.process(command, ImmutableMap.<String,Object>of());
      }

      protected void doCall(ReflexMatchContext.ProtocolClosureProcessor processor, Map<String,Object> args) {
         processor.process(command, args);
      }
   }

   public static class ZWaveNodeInfoContext extends Closure<Object> {
      private static final long serialVersionUID = -4501880181848524589L;

      ZWaveNodeInfoContext(GroovyObjectSupport parent) {
         super(parent);
      }

      protected void doCall(ReflexMatchContext.ProtocolClosureProcessor processor) {
         processor.process(ZWaveNodeInfo.INSTANCE, ImmutableMap.<String,Object>of());
      }

      protected void doCall(ReflexMatchContext.ProtocolClosureProcessor processor, Map<String,Object> args) {
         processor.process(ZWaveNodeInfo.INSTANCE, args);
      }
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

   public static enum ZWaveNodeInfo {
      INSTANCE;
   }
}

