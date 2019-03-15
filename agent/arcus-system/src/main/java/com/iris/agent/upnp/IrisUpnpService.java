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

import java.io.StringReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Response;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.iris.agent.exec.ExecService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.http.AsyncHttpService;
import com.iris.agent.http.HttpServer;
import com.iris.agent.ssl.SslKeyStore;
import com.iris.util.IrisUUID;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

public final class IrisUpnpService {
   private static final Logger log = LoggerFactory.getLogger(IrisUpnpService.class);
   private static final Object LOCK = new Object();

   private static final long UPNP_DISCOVERY_INTERVAL = TimeUnit.SECONDS.toNanos(60);
   private static final long UPNP_RESTART_CHECK_NORMAL = TimeUnit.SECONDS.toNanos(150);
   private static final long UPNP_RESTART_CHECK_FAST = TimeUnit.SECONDS.toNanos(30);
   private static final long UPNP_SEARCH_INTERVAL = TimeUnit.MINUTES.toNanos(2);
   private static final long UPNP_SEARCH_STARTUP_DELAY = TimeUnit.SECONDS.toNanos(15);

   private static final XmlPullParserFactory xmlFactory;
   private static List<String> inets;
   static final String uuid;

   private static final CopyOnWriteArraySet<IrisUpnpListener> listeners = new CopyOnWriteArraySet<>();
   private static final ConcurrentMap<String,UpnpDeviceInformation> devices = new ConcurrentHashMap<>();
   // Whitelist camera devices - only a single type for now, but may change in the future
   public static final Set<String> upnpCameraDeviceTypeWhiteList = ImmutableSet.<String>builder()
      .add("Wireless Network Camera")
      .build();

   private static final Predicate<HttpHeaders> headerFilter = (@NonNull HttpHeaders h) -> h.contains("hue-bridgeid");

   private static EventLoopGroup eventLoopGroup;
   private static List<IrisUpnpServer> servers = new ArrayList<>();
   private static final HashedWheelTimer timer = new HashedWheelTimer();
   
   static {

      try {
         xmlFactory = XmlPullParserFactory.newInstance();
      } catch (Exception ex) {
         log.error("could not create xml pull parser:", ex);
         throw new RuntimeException(ex);
      }

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

   public static void scan() {
      List<IrisUpnpServer> search;
      synchronized (LOCK) {
         search = servers;
      }

      log.trace("performing upnp search..");
      for (IrisUpnpServer server : search) {
         server.search();
      }
   }

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

   public static void remove(String uuid) {
      UpnpDeviceInformation info = devices.get(uuid);
      if (info != null) {
         remove(info);
      }
   }

   public static void addListener(IrisUpnpListener listener) {
      listeners.add(listener);
      for (UpnpDeviceInformation device : devices.values()) {
         if (device.discovered) {
            listener.deviceAdded(device.type, device.manuf, device.model, device.address.getHostAddress(), device.uuid);
         }
      }
   }

   public static void removeListener(IrisUpnpListener listener) {
      listeners.remove(listener);
   }

   /////////////////////////////////////////////////////////////////////////////
   // UPnP Device Management
   /////////////////////////////////////////////////////////////////////////////
   
   static void poke(
      InetSocketAddress sender,
      IrisUpnpServer.UniqueServiceName usn,
      long maxAgeInNs,
      @Nullable String url,
      @NonNull HttpHeaders headers
   ) {
      final String uuid = usn.deviceUuid;
      final String type = usn.type;
      if (uuid == null || type == null || !(upnpCameraDeviceTypeWhiteList.contains(type) || headerFilter.apply(headers))) {
         return;
      }

      if (log.isTraceEnabled()) {
         log.trace("upnp device poked: cache={}s, sender={}, uuid={}, class={}, namespace={}, type={}, version={}",
            TimeUnit.SECONDS.convert(maxAgeInNs,TimeUnit.NANOSECONDS),
            sender,
            usn.deviceUuid,
            usn.clazz,
            usn.namespace,
            usn.type,
            usn.version);
      }

      boolean wasAdded = false;
      boolean wasUpdated = false;
      long now = System.nanoTime();
      UpnpDeviceInformation device = devices.get(uuid);
      if (device == null) {
         UpnpDeviceInformation added = new UpnpDeviceInformation(uuid, type, sender.getAddress(), now);;
         device = devices.putIfAbsent(uuid, added);
         if (device == null) {
            wasAdded = true;
            device = added;
         }
      }

      if (device.timeout != null) {
         device.timeout.cancel();
      }

      device.timeout = timer.newTimeout(new TimerTask() {
         @Override
         public void run(@Nullable Timeout to) {
            if (to != null && to.isExpired()) {
               log.info("upnp device expired: {}", uuid);
               remove(uuid);
            }
         }
      }, maxAgeInNs, TimeUnit.NANOSECONDS);


      InetAddress addr = sender.getAddress();
      InetAddress eaddr = device.address;
      if (!addr.equals(eaddr)) {
         device.address = addr;
         wasUpdated = true;
      }

      boolean discovered = device.discovered;
      long rediscoveryTime = device.discovery.get();
      if (wasAdded || (!discovered && rediscoveryTime <= now)) {
         discover(device, url, now);
      } else if (discovered && wasUpdated) {
         updated(device);
      }
   }

   static void added(final UpnpDeviceInformation device) {
      ExecService.io().submit(new Runnable() {
         @Override
         public void run() {
            log.info("upnp device added: uuid={}, type={}, manuf={}, model={}, host={}", device.uuid, device.type, device.manuf, device.model, device.address.getHostAddress());
            for (IrisUpnpListener listener : listeners) {
               if (device.discovered) {
                  listener.deviceAdded(device.type, device.manuf, device.model, device.address.getHostAddress(), device.uuid);
               }
            }
         }
      });
   }

   static void updated(final UpnpDeviceInformation device) {
      ExecService.io().submit(new Runnable() {
         @Override
         public void run() {
            log.info("upnp device updated: uuid={}, type={}, manuf={}, model={}, host={}", device.uuid, device.type, device.manuf, device.model, device.address.getHostAddress());
            for (IrisUpnpListener listener : listeners) {
               if (device.discovered) {
                  listener.deviceUpdated(device.type, device.manuf, device.model, device.address.getHostAddress(), device.uuid);
               }
            }
         }
      });
   }

   static void remove(final UpnpDeviceInformation device) {
      device.timeout.cancel();
      devices.remove(device.uuid);
      ExecService.io().submit(new Runnable() {
         @Override
         public void run() {
            log.info("upnp device removed: uuid={}, type={}, manuf={}, model={}, host={}", device.uuid, device.type, device.manuf, device.model, device.address.getHostAddress());
            for (IrisUpnpListener listener : listeners) {
               if (device.discovered) {
                  listener.deviceRemoved(device.type, device.manuf, device.model, device.address.getHostAddress(), device.uuid);
               }
            }
         }
      });
   }

   static void discover(final UpnpDeviceInformation device, @Nullable String location, long now) {
      device.discovery.set(now + UPNP_DISCOVERY_INTERVAL);
      if (location == null) {
         return;
      }

      AsyncHttpService.get(location).execute(new AsyncCompletionHandler<Response>() {
         @Override
         public Response onCompleted(@Nullable Response response) throws Exception {
            if (response == null || response.getStatusCode() != 200) {
               return response;
            }

            try {
               XmlPullParser parser = xmlFactory.newPullParser();
               parser.setInput(new StringReader(response.getResponseBody(StandardCharsets.UTF_8)));

               String manuf = device.manuf;
               String model = device.model;

               do {
                  switch (parser.getEventType()) {
                  case XmlPullParser.START_TAG:
                     switch (parser.getName().toLowerCase()) {
                     case "manufacturer":
                        String nmanuf = parser.nextText();
                        if (!nmanuf.trim().isEmpty() && manuf == null) {
                           manuf = nmanuf;
                        }
                        break;
                     case "modelnumber":
                        String nmodel = parser.nextText();
                        if (!nmodel.trim().isEmpty() && model == null) {
                           model = nmodel;
                        }
                        break;
                     default:
                        // ignore
                        break;
                     }
                     break;
                  default:
                     // ignore
                     break;
                  }

                  //log.info("xml: {} -> {}", parser.getName(), parser.nextText());
               } while (parser.next() != XmlPullParser.END_DOCUMENT);

               device.manuf = manuf;
               device.model = model;
               if ((manuf != null) && (model != null)) {
                  device.discovered = true;
                  added(device);
               }
            } catch (Exception ex) {
               log.debug("failed to parse upnp descriptor:", ex);
            }

            return response;
         }
      });
   }

   /////////////////////////////////////////////////////////////////////////////
   // UPnP Service Life Cycle
   /////////////////////////////////////////////////////////////////////////////

   public static void start() {
      List<IrisUpnpServer> startup = new ArrayList<>(inets.size());

      synchronized (LOCK) {
         listeners.clear();
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
         ExecService.periodic().scheduleAtFixedRate(new UpnpSearch(),
            UPNP_SEARCH_STARTUP_DELAY, UPNP_SEARCH_INTERVAL, TimeUnit.NANOSECONDS);
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

   private static final class UpnpSearch implements Runnable {
      @Override
      public void run() {
         try {
            scan();
            alive();
         } catch (Exception th) {
            log.warn("failed to run upnp search", th);
         }
      }
   }

   private static final class UpnpDeviceInformation {
      final String uuid;
      final String type;
      final AtomicLong discovery;

      @Nullable String manuf;
      @Nullable String model;

      @Nullable Timeout timeout;
      InetAddress address;
      boolean discovered;

      UpnpDeviceInformation(String uuid, String type, InetAddress address, long now) {
         this.uuid = uuid;
         this.address = address;
         this.type = type;
         this.discovered = false;
         this.discovery = new AtomicLong(now + UPNP_DISCOVERY_INTERVAL);
      }
   }
}

