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
package com.iris.driver.config;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.event.ScheduledDriverEvent;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutors;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;

public class DriverConfigurationStateMachine {
   private static final Logger log = LoggerFactory.getLogger(DriverConfigurationStateMachine.class);

   private static final long RETRY_BASE_MS = TimeUnit.SECONDS.toMillis(5);
   private static final long RETRY_MAX_MS = TimeUnit.MINUTES.toMillis(1);
   private static final int RETRY_MAX_ATTEMPTS = 10;

   private final Map<String,State> stateMachine;

   private DriverConfigurationStateMachine(Map<String,State> stateMachine) {
      this.stateMachine = stateMachine;
   }

   public void onDriverRestored(DeviceDriverContext context) {
      if (isStateMachineStarted(context)) {
         setCurrentState(context, null);
         setRetryNumber(context, 0);

         startNextConfigurationProcess(context);
      }
   }

   public ContextualEventHandler<DriverEvent> createEventHandler() {
      return new ContextualEventHandler<DriverEvent>() {
         @Override
         public boolean handleEvent(DeviceDriverContext context, DriverEvent event) throws Exception {
            if (event instanceof DeviceConnectedEvent) {
               clearAllStates(context);
               markStateMachineStarted(context, true);
               startNextConfigurationProcess(context);
            } else if (event instanceof ScheduledDriverEvent) {
               final State currentState = getCurrentState(context);
               if (currentState != null) {
                  ScheduledDriverEvent sch = (ScheduledDriverEvent)event;
                  if (sch.getData() == currentState) {
                     final int retryNumber = getRetryNumber(context);
                     if (retryNumber >= RETRY_MAX_ATTEMPTS) {
                        context.getLogger().info("configuration state {} failed: attempts={}", currentState.getName(), retryNumber);
                        markStateFinishedAndRunNext(context, currentState);
                        return false;
                     }

                     context.getLogger().info("retrying configuration state {}: attempt={}", currentState.getName(), retryNumber);
                     currentState.start(DriverConfigurationStateMachine.this, context);
                  }
               }
            }

            return false;
         }
      };
   }

   public ContextualEventHandler<ProtocolMessage> createProtocolMessageHandler() {
      return new ContextualEventHandler<ProtocolMessage>() {
         @Override
         public boolean handleEvent(DeviceDriverContext context, ProtocolMessage message) throws Exception {
            final State currentState = getCurrentState(context);
            if (currentState != null) {
               currentState.check(DriverConfigurationStateMachine.this, context, message);
            }

            return false;
         }
      };
   }

   private int getRetryNumber(DeviceDriverContext context) {
      Integer retry = (Integer)context.getVariable("drvconf-retrynum");
      return (retry == null) ? 0 : retry.intValue();
   }

   private void setRetryNumber(DeviceDriverContext context, int retryNumber) {
      context.setVariable("drvconf-retrynum", retryNumber);
   }

   private State getCurrentState(DeviceDriverContext context) {
      String stateName = (String)context.getVariable("drvconf-curstate");
      return StringUtils.isBlank(stateName) ? null : stateMachine.get(stateName);
   }

   private void setCurrentState(DeviceDriverContext context, State state) {
      context.setVariable("drvconf-curstate", (state == null) ? "" : state.getName());
   }

   private void startNextConfigurationProcess(DeviceDriverContext context) {
      final State currentState = getCurrentState(context);
      if (currentState != null) {
         return;
      }

      State next = getNextState(context);
      if (next != null) {
         setCurrentState(context, next);
         setRetryNumber(context, 0);

         context.getLogger().info("starting configuration state {}", next.getName());
         next.start(this, context);
      }
   }

   private State getNextState(DeviceDriverContext context) {
      for (Map.Entry<String,State> entry : stateMachine.entrySet()) {
         if (!isStateDone(context, entry.getKey())) {
            return entry.getValue();
         }
      }

      return null;
   }

   private final void clearAllStates(DeviceDriverContext context) {
      for (String state : stateMachine.keySet()) {
         markStateDone(context, state, false);
      }
   }

   private final void markStateMachineStarted(DeviceDriverContext context, boolean started) {
      context.setVariable("drvconf-started", started);
   }

   private final boolean isStateMachineStarted(DeviceDriverContext context) {
      return Boolean.TRUE.equals(context.getVariable("drvconf-started"));
   }

   private final void markStateDone(DeviceDriverContext context, String state, boolean done) {
      context.setVariable("drvconf-done-" + state, done);
   }

   private final boolean isStateDone(DeviceDriverContext context, String state) {
      return Boolean.TRUE.equals(context.getVariable("drvconf-done-" + state));
   }

   private void markStateFinishedAndRunNext(DeviceDriverContext context, State state) {
      markStateDone(context, state.getName(), true);

      setCurrentState(context, null);
      startNextConfigurationProcess(context);
   }

   /////////////////////////////////////////////////////////////////////////////
   // API for Configuration States
   /////////////////////////////////////////////////////////////////////////////
   
   public void finishState(DeviceDriverContext context, State state) {
      context.getLogger().info("configuration state {} finished", state.getName());
      markStateFinishedAndRunNext(context, state);
   }

   public void scheduleRetry(DeviceDriverContext context, State state) {
	   DriverExecutor executor = DriverExecutors.get();

      final int retryNumber = getRetryNumber(context);
	   int delayMs = (int)Math.min(RETRY_BASE_MS*Math.pow(2,retryNumber), RETRY_MAX_MS);
      Date runAt = new Date(System.currentTimeMillis() + delayMs);

      setRetryNumber(context, retryNumber+1);

      ScheduledDriverEvent event = DriverEvent.createScheduledEvent("drvconf-retry", state, null, runAt);
      executor.defer(event, runAt);
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   
   public static interface State {
      String getName();

      void start(DriverConfigurationStateMachine csm, DeviceDriverContext context);
      void check(DriverConfigurationStateMachine csm, DeviceDriverContext context, ProtocolMessage message);
   }

   public static abstract class AbstractState implements State {
      private final String name;
      private DriverConfigurationStateMachine parent;
      private DeviceDriverContext context;

      public AbstractState(String name) {
         this.name = name;
      }

      @Override
      public String getName() {
         return name;
      }

      @Override
      public void start(DriverConfigurationStateMachine csm, DeviceDriverContext context) {
         try {
            this.parent = csm;
            this.context = context;
            start();
         } finally {
            this.context = null;
            this.parent = null;
         }
      }

      @Override
      public void check(DriverConfigurationStateMachine csm, DeviceDriverContext context, ProtocolMessage message) {
         try {
            this.parent = csm;
            this.context = context;
            check(message);
         } finally {
            this.context = null;
            this.parent = null;
         }
      }

      protected void setVariable(String name, Object value) {
         check("set variable");
         context.setVariable("drvconf-" + name, value);
      }

      protected Object getVariable(String name) {
         check("get variable");
         return context.getVariable("drvconf-" + name);
      }

      protected Object getVariable(String name, Object def) {
         Object val = getVariable(name);
         return (val == null) ? def : val;
      }

      protected void finish() {
         check("finish configuration state");
         parent.finishState(context, this);
      }

      protected <M> void sendToDevice(Protocol<M> protocol, M message) {
         sendToDevice(protocol, message, -1);
      }

      protected <M> void sendToDevice(Protocol<M> protocol, M message, int ttl) {
         check("send to device");
         context.sendToDevice(protocol, message, ttl);
      }

      protected <M> void scheduleRetry() {
         check("schedule configuration retry");
         parent.scheduleRetry(context, this);
      }

      private void check(String name) {
         if (parent == null || context == null) {
            throw new RuntimeException("cannot " + name + " outside of handler");
         }
      }

      protected abstract void start();
      protected abstract void check(ProtocolMessage message);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Builder Interfaces
   /////////////////////////////////////////////////////////////////////////////
   
   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {
      private final LinkedHashMap<String,State> steps = new LinkedHashMap<>();

      private Builder() {
      }

      public Builder step(State state) {
         steps.put(state.getName(), state);
         return this;
      }

      public DriverConfigurationStateMachine build() {
         return new DriverConfigurationStateMachine(
            Collections.unmodifiableMap(new LinkedHashMap<String,State>(steps))
         );
      }
   }
}

