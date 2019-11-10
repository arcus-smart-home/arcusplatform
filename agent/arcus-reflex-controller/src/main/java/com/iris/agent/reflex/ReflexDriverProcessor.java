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
package com.iris.agent.reflex;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.util.Backoff;
import com.iris.agent.util.Backoffs;
import com.iris.agent.util.RxIris;
import com.iris.driver.reflex.ReflexDriver;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.model.Version;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;

public class ReflexDriverProcessor extends AbstractReflexProcessor {
   private static final Logger log = LoggerFactory.getLogger(ReflexController.class);

   final ReflexDriver driver;
   final ReflexDriverHubContext ctx;

   public ReflexDriverProcessor(
      ReflexController parent,
      Address addr,
      ReflexDriver driver) {
      this.ctx = new ReflexDriverHubContext(parent, addr);
      this.driver = driver;
   }

   @Override
   public boolean isOffline() {
      DeviceProtocolAddress daddr = (DeviceProtocolAddress)getAddress();
      switch (daddr.getProtocolName()) {
      case ZigbeeProtocol.NAMESPACE:
         return ctx.parent.zigbee().isOffline(daddr);
      case ZWaveProtocol.NAMESPACE:
         return ctx.parent.zwave().isOffline(daddr);
      default:
         return false;
      }

   }

   @Override
   public Set<String> getCapabilities() {
      return driver.getCapabilities();
   }

   @Override
   public @Nullable Object getAttribute(String name) {
      Map<String,Object> attrs = ctx.getState();
      return (attrs != null) ? attrs.get(name) : null;
   }

   @Override
   public Address getAddress() {
      return ctx.getProtocolAddress();
   }

   @Override
   public Map<String,Object> getState() {
      return ctx.getState();
   }

   @Override
   public Map<String,Object> getSyncState() {
      return ctx.getState();
   }

   @Override
   public String getDriver() {
      return driver.getDriver();
   }

   @Override
   public Version getVersion() {
      return driver.getVersion();
   }

   @Override
   public String getHash() {
      return driver.getHash();
   }

   @Override
   public boolean isDegraded() {
      return driver.isDegraded();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Lifecycle Reflexes
   /////////////////////////////////////////////////////////////////////////////
   
   public void start(Map<String,String> state, State curState) {
      ctx.initializeAttributes(state);
      super.start(curState);
   }
   
   @Override
   protected void onAdded() {
      if (ReflexController.DISABLE_LOCAL_PROCESSING) {
         return;
      }

      log.info("running on added: {}", ctx.getProtocolAddress());
      driver.fireOnAdded(ctx, ReflexDriver.V0);

      setCurrentState(State.ADDED);
      log.info("completed on added: {}", ctx.getProtocolAddress());
   }

   @Override
   protected void onConnected() {
      if (ReflexController.DISABLE_LOCAL_PROCESSING) {
         return;
      }

      log.info("running on connected: {}", ctx.getProtocolAddress());
      driver.fireOnConnected(ctx, ReflexDriver.V0);

      setCurrentState(State.CONNECTED);
      log.info("completed on connected: {}", ctx.getProtocolAddress());
   }

   @Override
   protected void onDisconnected() {
      if (ReflexController.DISABLE_LOCAL_PROCESSING) {
         return;
      }

      log.info("running on disconnected: {}", ctx.getProtocolAddress());
      driver.fireOnDisconnected(ctx, ReflexDriver.V0);

      setCurrentState(State.DISCONNECTED);
      log.info("completed on disconnected: {}", ctx.getProtocolAddress());
   }

   @Override
   protected void onRemoved() {
      if (ReflexController.DISABLE_LOCAL_PROCESSING) {
         return;
      }

      log.info("running on removed: {}", ctx.getProtocolAddress());
      driver.fireOnRemoved(ctx, ReflexDriver.V0);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Message Reflexes
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public boolean handle(PlatformMessage msg) {
      boolean result = driver.handle(ctx, msg);
      ctx.getAndResetSetAttributesConsumed();
      ctx.getAndResetResponse();
      return result;
   }

   @Override
   public boolean handle(ProtocolMessage msg) {
      return driver.handle(ctx, msg, ReflexDriver.V0);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Reflex Processor Factory
   /////////////////////////////////////////////////////////////////////////////

   public static ReflexDriverProcessor create(ReflexController parent, Address addr, ReflexDriver driver) {
      return new ReflexDriverProcessor(parent, addr, driver);
   }
}

