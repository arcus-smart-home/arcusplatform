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
package com.iris.agent.gateway;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.fourg.FourgService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.messages.capability.HubNetworkCapability;

class GatewayNetworkChecker {
   private static final Logger log = LoggerFactory.getLogger(GatewayNetworkChecker.class);

   public static final long SHORT_CHECK_INTERVAL = TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);
   public static final long LONG_CHECK_INTERVAL = TimeUnit.NANOSECONDS.convert(15, TimeUnit.MINUTES);
   
   private static final HubAttributesService.Attribute<String> gateway = HubAttributesService.persisted(String.class, HubNetworkCapability.ATTR_GATEWAY, "0.0.0.0");

   @SuppressWarnings("rawtypes")
   private static final HubAttributesService.Attribute<List> dns = HubAttributesService.ephemeral(List.class, HubNetworkCapability.ATTR_DNS, IrisHal.getDNS());

   @SuppressWarnings({ "rawtypes", "unused" })
   private static final HubAttributesService.Attribute<List> intf = HubAttributesService.computed(List.class, HubNetworkCapability.ATTR_INTERFACES, new Supplier<List>() {
      @Override
      public List get() {
         return IrisHal.getInterfaces();
      }
   });

   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Long> uptime = HubAttributesService.computed(Long.class, HubNetworkCapability.ATTR_UPTIME, new Supplier<Long>() {
      @Override
      public Long get() {
         long uptime = System.nanoTime() - lastInterfaceChangeTime;
         return TimeUnit.SECONDS.convert(uptime, TimeUnit.NANOSECONDS);
      }
   });

   private static long lastInterfaceChangeTime = System.nanoTime();

   private boolean primaryFirst = true;
   private boolean secondaryFirst = true;

   private @Nullable String primaryIp = null;
   private @Nullable String primaryNetmask = null;
   private @Nullable String primaryInterfaceType = null;

   private @Nullable String secondaryIp = null;
   private @Nullable String secondaryNetmask = null;
   private @Nullable String secondaryInterfaceType = null;

   private final AtomicReference<GatewayConnection> primaryConnection;
   private final AtomicReference<GatewayConnection> secondaryConnection;
   private final AtomicReference<GatewayConnection> currentConnection;

   GatewayNetworkChecker(AtomicReference<GatewayConnection> primaryConnection, AtomicReference<GatewayConnection> secondaryConnection, AtomicReference<GatewayConnection> currentConnection) {
      this.primaryConnection = primaryConnection;
      this.secondaryConnection = secondaryConnection;
      this.currentConnection = currentConnection;
   }

   void start() {
      ExecService.periodic().scheduleWithFixedDelay(new NetworkUpdater(), 0, SHORT_CHECK_INTERVAL, TimeUnit.NANOSECONDS);
   }

   static void markInterfaceChanged() {
      lastInterfaceChangeTime = System.nanoTime();
   }

   private static boolean compare(@Nullable Object o1, @Nullable Object o2) {
      if (o1 == null) return o2 == null;
      return o1.equals(o2);
   }

   private final class NetworkUpdater implements Runnable {
      private long lastCheckTime = Long.MIN_VALUE;

      @Override
      public void run() {
         try {
            GatewayConnection cur = currentConnection.get();

            IrisHal.NetworkInfo ni = IrisHal.getNetworkInfo();
            log.trace("primary interface: {} ({}), secondary interface: {} ({})", ni.primary, ni.primaryIp, ni.secondary, ni.secondaryIp);

            boolean primaryUpdated = false;
            primaryUpdated |= !compare(primaryIp, ni.primaryIp);
            primaryUpdated |= !compare(primaryNetmask, ni.primaryNetmask);
            primaryUpdated |= !compare(primaryInterfaceType, ni.primaryInterfaceType);

            boolean secondaryUpdated = false;
            secondaryUpdated |= !compare(secondaryIp, ni.secondaryIp);
            secondaryUpdated |= !compare(secondaryNetmask, ni.secondaryNetmask);
            secondaryUpdated |= !compare(secondaryInterfaceType, ni.secondaryInterfaceType);

            long curTime = System.nanoTime();
            if (primaryUpdated || lastCheckTime == Long.MIN_VALUE || (curTime - lastCheckTime) >= LONG_CHECK_INTERVAL) {
               lastCheckTime = curTime;

               primaryUpdated |= gateway.set(IrisHal.getGateway());
               dns.set(IrisHal.getDNS());
            }

            if (primaryUpdated) {
               log.trace("primary gateway interface changed: ip={}, netmask={}, type={}", ni.primaryIp, ni.primaryNetmask, ni.primaryInterfaceType);
               primaryIp = ni.primaryIp;
               primaryNetmask = ni.primaryNetmask;
               primaryInterfaceType = ni.primaryInterfaceType;

               if (!primaryFirst) {
                  GatewayConnection primaryEstablished = primaryConnection.get();
                  SocketAddress primarySocket = (primaryEstablished == null) ? null : primaryEstablished.getOutboundInterface();
                  InetAddress primaryInet = (primarySocket == null || !(primarySocket instanceof InetSocketAddress)) ? null : ((InetSocketAddress)primarySocket).getAddress();
                  String establishedIp = (primaryInet == null) ? null : primaryInet.getHostAddress();

                  if (cur != null && cur.isPrimary() && (establishedIp == null || !establishedIp.equals(primaryIp))) {
                     log.warn("forcing gateway reconnect because primary interface changed");
                     LifeCycleService.setState(LifeCycle.CONNECTING);
                  }
               }

               primaryFirst = false;
            }

            if (secondaryUpdated) {
               log.trace("secondary gateway interface changed: ip={}, netmask={}, type={}", ni.primaryIp, ni.primaryNetmask, ni.primaryInterfaceType);
               secondaryIp = ni.secondaryIp;
               secondaryNetmask = ni.secondaryNetmask;
               secondaryInterfaceType = ni.secondaryInterfaceType;

               if (!secondaryFirst) {
                  GatewayConnection secondaryEstablished = secondaryConnection.get();
                  SocketAddress secondarySocket = (secondaryEstablished == null) ? null : secondaryEstablished.getOutboundInterface();
                  InetAddress secondaryInet = (secondarySocket == null || !(secondarySocket instanceof InetSocketAddress)) ? null : ((InetSocketAddress)secondarySocket).getAddress();
                  String establishedIp = (secondaryInet == null) ? null : secondaryInet.getHostAddress();

                  if (cur != null && !cur.isPrimary() && (establishedIp == null || !establishedIp.equals(secondaryIp))) {
                     log.warn("forcing gateway reconnect because secondary interface changed");
                     LifeCycleService.setState(LifeCycle.CONNECTING);
                  }
               }

               if (ni.secondary != null && secondaryIp != null && !"0.0.0.0".equals(secondaryIp)) {
                  FourgService.setState(FourgService.State.CONNECTING);
               } else {
                  FourgService.setState(FourgService.State.UNAVAILABLE);
               }

               secondaryFirst = false;
            }
         } catch (Exception ex) {
            // ignore
         }
      }
   }
}


