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
package com.iris.driver.groovy.reflex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.EventDefinition;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.context.GroovyAttributeDefinition;
import com.iris.driver.groovy.context.GroovyCommandDefinition;
import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexActionDebug;
import com.iris.driver.reflex.ReflexActionDelay;
import com.iris.driver.reflex.ReflexActionLog;
import com.iris.driver.reflex.ReflexActionOrdered;
import com.iris.driver.reflex.ReflexActionSendPlatform;
import com.iris.driver.reflex.ReflexActionSetAttribute;
import com.iris.driver.reflex.ReflexMatch;
import com.iris.driver.reflex.ReflexMatchAttribute;
import com.iris.driver.reflex.ReflexMatchMessage;
import com.iris.driver.reflex.ReflexMatchRegex;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;

@SuppressWarnings("deprecation")
public class ReflexMatchContext extends ReflexLogContext {
   private List<ReflexActionModifier> modifiers;

   public ReflexMatchContext() {
      this (new ArrayList<ReflexMatch>(), new ArrayList<ReflexAction>());
   }

   private ReflexMatchContext(List<ReflexMatch> matches, List<ReflexAction> actions) {
      super(matches,actions);
      this.modifiers = new ArrayList<>(1);
      this.modifiers.add(ReflexActionDefaultModifier.INSTANCE);
   }

   @Override
   public Object getProperty(String name) {
      switch (name) {
      case "debug":
         addAction(new ReflexActionDebug());
         return null;

      default:
         return super.getProperty(name);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Log Actions Support
   /////////////////////////////////////////////////////////////////////////////
   
   @Override
   protected void addLogAction(ReflexActionLog log) {
      addAction(log);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Combinator Actions
   /////////////////////////////////////////////////////////////////////////////

   public void delay(Closure<?> config) {
      ReflexDelayContext ctx = new ReflexDelayContext(this);
      try (ReflexActionPop pop = new ReflexActionPop(ctx)) {
         config.setDelegate(ctx);
         config.call();
      } finally {
         addAction(ctx.delayed);
      }
   }

   public void ordered(Closure<?> config) {
      ReflexOrderedContext ctx = new ReflexOrderedContext(this);
      try (ReflexActionPop pop = new ReflexActionPop(ctx)) {
         config.setDelegate(ctx);
         config.call();
      } finally {
         addAction(ctx.ordered);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Attribute Actions
   /////////////////////////////////////////////////////////////////////////////

   public void set(GroovyAttributeDefinition attr, Object value) {
      addAction(new ReflexActionSetAttribute(attr.getAttribute().getName(),value));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Protocol Message Matches and Actions
   /////////////////////////////////////////////////////////////////////////////

   public void on(Closure<?> action) {
      action.call(new Object[] { getProtocolMatchProcessor() } );
   }

   public void on(Map<String,Object> vals, Closure<?> action) {
      action.call(new Object[] { getProtocolMatchProcessor(), vals });
   }

   public void on(Map<String,Object> vals, GroovyObject action) {
      if (action instanceof Closure) {
         on(vals, (Closure<?>)action);
      } else {
         action.invokeMethod("match", new Object[] { getProtocolMatchProcessor(), vals });
      }
   }

   public void on(String regex) {
      addMatch(new ReflexMatchRegex(regex));
   }

   public void on(GroovyAttributeDefinition attr, Object value) {
      addMatch(new ReflexMatchAttribute(attr.getName(),attr.getAttributeType().coerce(value)));
   }

   public void on(GroovyCommandDefinition cmd) {
      boolean hasAll = true;
      for (Map.Entry<String,AttributeDefinition> entry : cmd.getInputArguments().entrySet()) {
         String name = entry.getKey();
         AttributeDefinition attr = entry.getValue();

         if (!attr.isOptional()) {
            hasAll = false;
            GroovyValidator.error("reflex command match must specify value for '" + name + "'");
         }
      }

      if (hasAll) {
         MessageBody msg = MessageBody.buildMessage(cmd.getName(), ImmutableMap.<String,Object>of());
         addMatch(new ReflexMatchMessage(msg));
      }
   }

   public void on(Map<String,Object> attrs, GroovyCommandDefinition cmd) {
      boolean hasAll = true;
      Map<String,Object> values = new HashMap<>();
      for (Map.Entry<String,AttributeDefinition> entry : cmd.getInputArguments().entrySet()) {
         String name = entry.getKey();
         AttributeDefinition attr = entry.getValue();
         Object value = attrs.get(name);

         if (!attr.isOptional() && value == null) {
            hasAll = false;
            GroovyValidator.error("reflex command match must specify value for '" + name + "'");
         } else {
            values.put(name, attr.getAttributeType().coerce(value));
         }
      }

      if (hasAll) {
         MessageBody msg = MessageBody.buildMessage(cmd.getName(), values);
         addMatch(new ReflexMatchMessage(msg));
      }
   }

   public void send(Closure<?> action) {
      action.call(new Object[] { getProtocolSendProcessor() } );
   }

   public void send(Map<String,Object> vals, Closure<?> action) {
      action.call(new Object[] { getProtocolSendProcessor(), vals });
   }

   public void send(GroovyObject action) {
      if (action instanceof Closure) {
         send((Closure<?>)action);
      } else {
         action.invokeMethod("send", new Object[] { getProtocolSendProcessor() } );
      }
   }

   public void send(Map<String,Object> vals, GroovyObject action) {
      if (action instanceof Closure) {
         send(vals, (Closure<?>)action);
      } else {
         action.invokeMethod("send", new Object[] { getProtocolSendProcessor(), vals });
      }
   }

   public void respond(String rsp) {
      respond(rsp, ImmutableMap.<String,Object>of());
   }

   public void respond(String rsp, Map<String,Object> attrs) {
      boolean hasPlatformMatch = false;
      for (ReflexMatch match : getMatches()) {
         if (match instanceof ReflexMatchMessage) {
            hasPlatformMatch = true;
            break;
         }
      }

      boolean hasPlatformResponse = false;
      for (ReflexAction action : getActions()) {
         if (action instanceof ReflexActionSendPlatform) {
            ReflexActionSendPlatform rasp = (ReflexActionSendPlatform)action;
            if (rasp.isResponse()) {
               hasPlatformResponse = true;
               break;
            }
         }
      }

      if (!hasPlatformMatch) {
         GroovyValidator.error("responses can only be used when matching a platform message");
         return;
      }

      if (hasPlatformResponse) {
         GroovyValidator.error("only one response is allowed");
         return;
      }

      addAction(new ReflexActionSendPlatform(rsp, ImmutableMap.copyOf(attrs), true));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Platform Message Actions
   /////////////////////////////////////////////////////////////////////////////

   public void emit(EventDefinition event) {
      emit(ImmutableMap.<String,Object>of(), event);
   }

   public void emit(Map<String,Object> args, EventDefinition event) {
      verifyMessageArguments(args, event.getAttributes(), "emit");
      addAction(new ReflexActionSendPlatform(event.getName(), args, false));
   }

   public void emit(GroovyAttributeDefinition attr, Object value) {
      addAction(new ReflexActionSendPlatform(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(attr.getName(), value), false));
   }

   private void verifyMessageArguments(Map<String,Object> args, Map<String,AttributeDefinition> attrs, String type) {
      for (Map.Entry<String,AttributeDefinition> entry : attrs.entrySet()) {
         AttributeDefinition attr = entry.getValue();
         if (!attr.isOptional() && !args.containsKey(entry.getKey())) {
            GroovyValidator.error(type + " must contain values for all required parameters: missing '" + attr.getName() + "'");
         }
      }

      for (String arg : args.keySet()) {
         if (!attrs.containsKey(arg)) {
            GroovyValidator.error("cannot " + type + " message with unknown parameter: '" + arg + "'");
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   
   public void addAction(ReflexAction action) {
      if (action == null) {
         return;
      }

      ReflexActionModifier modifier = modifiers.get(modifiers.size() - 1);
      ReflexAction updated = modifier.apply(action);
      if (updated != null) {
         addFinalAction(updated);
      }
   }

   protected void addFinalAction(ReflexAction action) {
      actions.add(action);
   }

   public void addMatch(ReflexMatch match) {
      matches.add(match);
   }

   public ProtocolClosureProcessor getProtocolMatchProcessor() {
      return ErrorProtocolClosureProcessor.INSTANCE;
   }

   public ProtocolClosureProcessor getProtocolSendProcessor() {
      return ErrorProtocolClosureProcessor.INSTANCE;
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   
   public static interface ProtocolClosureProcessor {
      void process(Object protocolMessage, Map<String,Object> args);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   
   public static enum ErrorProtocolClosureProcessor implements ProtocolClosureProcessor {
      INSTANCE;

      @Override
      public void process(Object protocolMessage, Map<String,Object> args) {
         GroovyValidator.error("cannot match or send protocol messages in this reflex configuration closure");
      }
   }
   
   public final class ReflexActionPop implements AutoCloseable {
      public ReflexActionPop(ReflexActionModifier modifier) {
         modifiers.add(modifier);
      }

      @Override
      public void close() {
         modifiers.remove(modifiers.size() - 1);
      }
   }

   public static class ReflexDelegatingContext extends GroovyObjectSupport {
      protected final ReflexContext delegate;

      private ReflexDelegatingContext(ReflexContext delegate) {
         this.delegate = delegate;
      }

      @Override
      public Object getProperty(String name) {
         return delegate.getProperty(name);
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         return delegate.invokeMethod(name, args);
      }
   }

   public static final class ReflexDelayContext extends ReflexDelegatingContext implements ReflexActionModifier {
      private ReflexActionDelay delayed;

      private ReflexDelayContext(ReflexContext delegate) {
         super(delegate);
      }

      public void after(long time) {
         after(time, TimeUnit.SECONDS);
      }

      public void after(long time, TimeUnit unit) {
         GroovyValidator.assertNull(delayed, "delay time should only be specified once in a delay action");
         this.delayed = new ReflexActionDelay(time, unit);
      }
   
      @Override
      public ReflexAction apply(ReflexAction action) {
         GroovyValidator.assertNotNull(delayed, "delay must be specified using 'by <TIME>, <UNIT>' before declaring actions inside a delay block");
         delayed.addAction(action);
         return null;
      }
   }

   public static final class ReflexOrderedContext extends ReflexDelegatingContext implements ReflexActionModifier {
      private ReflexActionOrdered ordered;

      private ReflexOrderedContext(ReflexContext delegate) {
         super(delegate);
      }

      @Override
      public ReflexAction apply(ReflexAction action) {
         if (ordered == null) {
            ordered = new ReflexActionOrdered();
         }

         ordered.addAction(action);
         return null;
      }
   }

   public static interface ReflexActionModifier {
      ReflexAction apply(ReflexAction action);
   }

   public static enum ReflexActionDefaultModifier implements ReflexActionModifier {
      INSTANCE;

      @Override
      public ReflexAction apply(ReflexAction action) {
         return action;
      }
   }
}

