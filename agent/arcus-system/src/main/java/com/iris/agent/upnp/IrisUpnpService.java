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
package com.iris.agent.upnp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.iris.agent.exec.ExecService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.http.HttpServer;
import com.iris.agent.ssl.SslKeyStore;
import com.iris.util.IrisUUID;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public final class IrisUpnpService {
   private static final Logger log = LoggerFactory.getLogger(IrisUpnpService.class);
   private static final Object LOCK = new Object();

   private static final long UPNP_RESTART_CHECK_NORMAL = TimeUnit.SECONDS.toNanos(150);
   private static final long UPNP_RESTART_CHECK_FAST = TimeUnit.SECONDS.toNanos(30);
   private static final long UPNP_ALIVE_INTERVAL = TimeUnit.MINUTES.toNanos(2);
   private static final long UPNP_ALIVE_STARTUP_DELAY = TimeUnit.SECONDS.toNanos(15);

   private static List<String> inets;
   static final String uuid;

   private static EventLoopGroup eventLoopGroup;
   private static List<IrisUpnpServer> servers = new ArrayList<>();
   
   static {
      String upnpUuid;
      try {
         X509Certificate[] certs = SslKeyStore.readHubCertificateAsArray();
         byte[] fingerprint = SslKeyStore.getFingerPrint(certs[0]);
         long hash = (((long)(fingerprint[0] & 0xFF)) <<  0) | 
                     (((long)(fingerprint[1] & 0xFF)) <<  8) | 
                     (((long)(fingerprint[2] & 0xFF)) << 16) | 
                     (((long)(fingerprint[3] & 0xFF)) << 24) |
                     (((long)(fingerprint[4] & 0xFF)) << 32) |
                     (((long)(fingerprint[5] & 0xFF)) << 40) |
                     (((long)(fingerprint[6] & 0xFF)) << 48) |
                     (((long)(fingerprint[7] & 0xFF)) << 56);

         upnpUuid = IrisUUID.timeUUID(certs[0].getNotBefore(), hash).toString();
         log.info("upnp usn uuid: {}", upnpUuid);
      } catch (Exception ex) {
         log.error("could not create upnp usn uuid, using random one");
         upnpUuid = IrisUUID.randomUUID().toString();
      }

      inets = getInets();
      uuid = upnpUuid;

		HttpServer.Context ctx = HttpServer.registerContext("/upnp");
		IrisUpnpServlet servlet = new IrisUpnpServlet();

		ctx.addServlet("/*", servlet);
   }

   private IrisUpnpService() {
   }

   private static List<String> getInets() {
      List<String> confInets = IrisHal.getLocalMulticastInterfaces();
      String inetsOverride = System.getenv("IRIS_AGENT_UPNP_IFACES");
      if (inetsOverride != null && !inetsOverride.trim().isEmpty()) {
         confInets = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(inetsOverride);
      }
      return confInets;
   }

   /////////////////////////////////////////////////////////////////////////////
   // UPnP Service API 
   /////////////////////////////////////////////////////////////////////////////

   public static void alive() {
      List<IrisUpnpServer> alive;
      synchronized (LOCK) {
         alive = servers;
      }

      log.trace("performing upnp alive notification..");
      for (IrisUpnpServer server : alive) {
         server.alive(uuid);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // UPnP Service Life Cycle
   /////////////////////////////////////////////////////////////////////////////

   public static void start() {
      List<IrisUpnpServer> startup = new ArrayList<>(inets.size());

      synchronized (LOCK) {
         if (eventLoopGroup != null) {
            eventLoopGroup.shutdownNow();
         }

         final AtomicLong counter = new AtomicLong();
         eventLoopGroup = new NioEventLoopGroup(inets.size(), new ThreadFactory() {
            @Override
            public Thread newThread(@Nullable Runnable runnable) {
               Thread thr = new Thread(runnable);
               thr.setName("upnp" + counter.getAndIncrement());
               thr.setDaemon(true);
               return thr;
            }
         });

         log.info("starting upnp service: interfaces={}", inets);
         long restartCheckDelay = UPNP_RESTART_CHECK_NORMAL;
         for (String inet : inets) {
            try {
               startup.add(new IrisUpnpServer(inet));
            } catch (Exception ex) {
               log.info("failed to start upnp on interface {}", inet);
               // Try again shortly
               restartCheckDelay = UPNP_RESTART_CHECK_FAST;
            }
         }

         // Try again shortly if the list of networks is empty
         if (inets.isEmpty()) {
            restartCheckDelay = UPNP_RESTART_CHECK_FAST;
         }
         servers = startup;
         ExecService.periodic().scheduleAtFixedRate(new UpnpRestartCheck(),
            restartCheckDelay, UPNP_RESTART_CHECK_NORMAL, TimeUnit.NANOSECONDS);
         ExecService.periodic().scheduleAtFixedRate(new UpnpAlive(),
            UPNP_ALIVE_STARTUP_DELAY, UPNP_ALIVE_INTERVAL, TimeUnit.NANOSECONDS);
      }

      for (IrisUpnpServer server : startup) {
         server.start(eventLoopGroup);
      }
   }

   public static void shutdown() {
      List<IrisUpnpServer> shutdown;
      synchronized (LOCK) {
         shutdown = servers;
         servers = new ArrayList<>();
      }

      for (IrisUpnpServer server : shutdown) {
         server.shutdown();
      }
   }

   public static void restart() {
      List<IrisUpnpServer> startup = new ArrayList<>(inets.size());
      List<IrisUpnpServer> shutdown;

      synchronized (LOCK) {
         shutdown = servers;

         log.info("restarting upnp service: interfaces={}", inets);
         for (String inet : inets) {
            try {
               startup.add(new IrisUpnpServer(inet));
            } catch (Exception ex) {
               log.info("failed to start upnp on interface {}", inet);
            }
         }

         servers = startup;
      }

      for (IrisUpnpServer server : shutdown) {
         server.shutdown();
      }

      for (IrisUpnpServer server : startup) {
         server.start(eventLoopGroup);
      }
   }

   public static void restartAfterDelay(long time, TimeUnit unit) {
      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            restart();
         }
      }, time, unit);
   }

   private static final class UpnpRestartCheck implements Runnable {
      private final Map<String,NetworkInterface> netInterfaces = new LinkedHashMap<String,NetworkInterface>();
      private final Map<String,InetAddress> ipAddresses = new LinkedHashMap<String,InetAddress>();

      UpnpRestartCheck() {
         for (String inet : inets) {
            NetworkInterface nif = findNetworkInterface(inet);
            netInterfaces.put(inet, nif);
            ipAddresses.put(inet, findBestInet(nif));
         }
      }

      @Override
      public void run() {
         try {
            log.trace("checking if upnp needs restart...");

            // Update interface list
            List<String> lastInets = inets;
            inets = getInets();
            boolean changed = false;
            if (!inets.equals(lastInets)) {
               changed = true;
            }
            // Run through list of inets even if list is different to update existing data
            for (String inet : inets) {
               NetworkInterface nif = findNetworkInterface(inet);
               InetAddress addr = findBestInet(nif);

               NetworkInterface existingNif = netInterfaces.put(inet, nif);
               InetAddress existingAddr = ipAddresses.put(inet, addr);

               if ((existingNif == null && nif != null) ||
                   (existingAddr == null && addr != null) ||
                   (existingNif != null && !existingNif.equals(nif)) ||
                   (existingAddr != null && !existingAddr.equals(addr))) {
                  changed = true;
               }
            }

            if (changed) {
               restart();
            }
         } catch (Exception th) {
            log.warn("failed to run upnp restart check", th);
         }
      }

      @Nullable
      private NetworkInterface findNetworkInterface(String inet) {
         try {
            return NetworkInterface.getByName(inet);
         } catch (SocketException ex) {
            return null;
         }
      }

      @Nullable
      private InetAddress findBestInet(@Nullable NetworkInterface nif) {
         if (nif == null) {
            return null;
         }

         InetAddress best = null;
         Enumeration<InetAddress> addrs = nif.getInetAddresses();
         while (addrs.hasMoreElements()) {
            InetAddress addr = addrs.nextElement();
            if (best == null) {
               best = addr;
            } else if (addr instanceof Inet4Address) {
               best = addr;
            }
         }

         return best;
      }
   }

   private static final class UpnpAlive implements Runnable {
      @Override
      public void run() {
         try {
            alive();
         } catch (Exception th) {
            log.warn("failed to send upnp alive notification", th);
         }
      }
   }
}
