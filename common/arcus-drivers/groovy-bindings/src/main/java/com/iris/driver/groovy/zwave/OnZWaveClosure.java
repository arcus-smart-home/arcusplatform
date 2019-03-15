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
/**
 * 
 */
package com.iris.driver.groovy.zwave;

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyTypeChecking;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 *
 */
public class OnZWaveClosure extends Closure<Object> {
   private final ZWaveAllCommandClasses commandClasses;
   private final EnvironmentBinding binding;
   
   private final Map<String, Object> properties;
   
   public OnZWaveClosure(
         ZWaveAllCommandClasses commandClasses,
         EnvironmentBinding binding
   ) {
      super(binding);
      this.setResolveStrategy(TO_SELF);
      this.binding = binding;
      this.commandClasses = commandClasses;
      
      Map<String, Object> properties = new HashMap<>();

      // process command classes
      for(ZWaveCommandClass commandClass: commandClasses.commandClasses) {
         properties.put(ZWaveCommandClass.scrub(commandClass.name), new OnZWaveCommandClassClosure(commandClass, binding));
      }
      
      this.properties = Collections.unmodifiableMap(properties);
   }
   
   protected void doCall(String commandClassName, String commandName, Closure<?> closure) {
      ZWaveCommandClass commandClass = ZWaveUtil.getCommandClassByName(commandClassName);
      ZWaveCommand command = ZWaveUtil.getCommandByName(commandClass, commandName);
      addHandler(commandClass.number, command.commandNumber, closure);
   }

   protected void doCall(String commandClassName, Closure<?> closure) {
      ZWaveCommandClass commandClass = ZWaveUtil.getCommandClassByName(commandClassName);
      addHandler(commandClass.number, null, closure);
   }

   protected void doCall(Byte commandClassId, Byte commandId, Closure<?> closure) {
      addHandler(commandClassId, commandId, closure);
   }

   protected void doCall(Byte commandClassId, Closure<?> closure) {
      addHandler(commandClassId, null, closure);
   }

   protected void doCall(Closure<?> closure) {
      addHandler(null, null, closure);
   }

   protected void addHandler(Byte commandClass, Byte commandId, Closure<?> closure) {
      ZWaveProtocolEventMatcher matcher = new ZWaveProtocolEventMatcher();
      matcher.setProtocolName(ZWaveProtocol.NAMESPACE);
      
      // Okay, so this is why this makes sense. If the command class is null, then this closure is
      // 'onZWaveMessage()' which is supposed to catch ALL ZWaveMessages so it shouldn't be limited
      // to ZWaveCommandMessages. If, however, there is a command class specified, then it only
      // applies to ZWaveCommandMessages since they are the only messages with command classes.
      matcher.setMessageType(commandClass != null ? ZWaveCommandMessage.TYPE : null);
      
      matcher.setCommandClass(commandClass);
      matcher.setCommandId(commandId);
      matcher.setHandler(DriverBinding.wrapAsHandler(closure));
      binding.getBuilder().addEventMatcher(matcher);

      // GroovyTypeChecking.invokeProtocolMessageHandler(closure);
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
   public Object invokeMethod(String name, Object arguments) {
      Object o = properties.get(name);
      if(o != null && o instanceof Closure<?>) {
         return ((Closure<?>) o).call((Object[]) arguments);
      }
      return super.invokeMethod(name, arguments);
   }

   @Override
   public String toString() {
      return "onZWaveMessage([commandClass], [commandId]) { <function> }";
   }

   private class OnZWaveCommandClassClosure extends Closure<Object> {
      private final EnvironmentBinding binding;
      private final ZWaveCommandClass commandClass;
      
      public OnZWaveCommandClassClosure(
            ZWaveCommandClass commandClass,
            EnvironmentBinding binding
      ) {
         super(binding);
         this.setResolveStrategy(TO_SELF);
         this.binding = binding;
         this.commandClass = commandClass;
      }

      protected void doCall(Closure<?> closure) {
         addHandler(commandClass.number, null, closure);
      }
      
      protected void doCall(String commandName, Closure<?> closure) {
         ZWaveCommand command = ZWaveUtil.getCommandByName(commandClass, commandName);
         addHandler(commandClass.number, command.commandNumber, closure);
      }

      protected void doCall(Byte commandId, Closure<?> closure) {
         addHandler(commandClass.number, commandId, closure);
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         ZWaveCommand command = ZWaveUtil.getCommandByName(commandClass, name);
         Object [] arguments = (Object[]) args;
         if(arguments.length == 1 && arguments[0] instanceof Closure) {
            addHandler(commandClass.number, command.commandNumber, (Closure<?>) arguments[0]);
            return null;
         }
         else {
            throw new MissingMethodException(name, getClass(), arguments);
         }
      }

   }

}

