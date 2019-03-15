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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.iris.agent.addressing.HubAddr;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.fourg.FourgService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleListener;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.lifecycle.LifeCycleService.Reset;
import com.iris.agent.logging.IrisAgentAppender;
import com.iris.agent.logging.IrisAgentLogging;
import com.iris.agent.metrics.MetricsService;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.router.Router;
import com.iris.agent.router.SnoopingPortHandler;
import com.iris.agent.util.Backoff;
import com.iris.agent.util.Backoffs;
import com.iris.agent.util.RxIris;
import com.iris.agent.watchdog.WatchdogCheck;
import com.iris.agent.watchdog.WatchdogService;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.protocol.ProtocolMessage;
import com.netflix.governator.annotations.WarmUp;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;

public class Gateway implements SnoopingPortHandler, LifeCycleListener, IrisAgentAppender.Listener, MetricsService.Listener {
   private static final Logger log = LoggerFactory.getLogger(Gateway.class);

   // NOTE: This is the amount of time that we will wait during initial startup before we attempt a
   //       connection on the secondary interface. This gives the primary interface some time to
   //       connect before the secondary is allowed to connect.
   //
   // IMPORTANT: This must be some non-trivial margin less than the hub's watchdog check, otherwise
   //            the watchdog may reboot the hub before a secondary connection can be established.
   public static final long SECONDARY_DELAY_BEFORE_INITIAL_CONNECT = TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS);
   
   // NOTE: This is the amount of time to allow the primary connection to be re-established before
   //       attempting a connection on the secondary interface. This prevents flapping between
   //       the primary and secondary interfaces too quickly during a reconnect event.
   //
   // IMPORTANT: This must be some non-trivial margin less than the hub's watchdog check, otherwise
   //            the watchdog may reboot the hub before a secondary connection can be established.
   public static final long SECONDARY_DELAY_BEFORE_CONNECT = TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);

   public static final HubAddr ADDRESS = HubAddressUtils.service("gateway");
   public static final long UNCONN_FORCE_REBOOT_TIME = (System.getenv("IRIS_AGENT_UNCONN_REBOOT_TIME") != null)
      ? TimeUnit.NANOSECONDS.convert(Long.parseLong(System.getenv("IRIS_AGENT_UNCONN_REBOOT_TIME")), TimeUnit.MINUTES) 
      : TimeUnit.NANOSECONDS.convert(30, TimeUnit.MINUTES);
   public static final long LOG_SEND_FREQ = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
   public static final long CONN_REPORT_INTERVAL = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);
   public static final long UNAUTH_FORCE_RECONNECT_TIME = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

   private static final List<String> STACK_TRACE_OPTIONS = Arrays.asList("full");

   @SuppressWarnings({ "unused", "null" })
   private static final HubAttributesService.Attribute<String> extip = HubAttributesService.persisted(String.class, HubNetworkCapability.ATTR_EXTERNALIP, null);
   private static final HubAttributesService.Attribute<String> ip = HubAttributesService.persisted(String.class, HubNetworkCapability.ATTR_IP, "0.0.0.0");
   private static final HubAttributesService.Attribute<String> netmask = HubAttributesService.persisted(String.class, HubNetworkCapability.ATTR_NETMASK, "0.0.0.0");
   private static final HubAttributesService.Attribute<String> type = HubAttributesService.persisted(String.class, HubNetworkCapability.ATTR_TYPE, "eth");

   private final Router router;
   private Port port;

   private final PingSender primaryPingSender;
   private final PingSender secondaryPingSender;
   private final LogSender logSender;
   private final GatewayOutboundQueue queue;
   private final BlockingQueue<JsonObject> bufferedLogMessages = new ArrayBlockingQueue<>(1024);
   private final Backoff primaryBackoff;
   private final Backoff secondaryBackoff;

   private final GatewayNetworkChecker networkChecker;
   private final ThrowableProxyConverter converter;
   private EventLoopGroup eventLoopGroup;

   private AtomicReference<GatewayConnection> primaryConnection = new AtomicReference<>();
   private AtomicReference<GatewayConnection> secondaryConnection = new AtomicReference<>();
   private AtomicReference<GatewayConnection> currentConnection = new AtomicReference<>();
   private AtomicBoolean allowSecondaryConnection = new AtomicBoolean(true);

   @Inject
   @SuppressWarnings("null")
   public Gateway(Router router) {
      WatchdogService.addWatchdogCheck(new ConnectionWatchdogCheck());

      this.router = router;
      this.networkChecker = new GatewayNetworkChecker(primaryConnection, secondaryConnection, currentConnection);

      this.queue = new GatewayOutboundQueue();

      this.primaryPingSender = new PingSender(true);
      this.secondaryPingSender = new PingSender(false);
      this.logSender = new LogSender();

      this.converter = new ThrowableProxyConverter();
      this.converter.setOptionList(STACK_TRACE_OPTIONS);
      this.converter.start();

      this.primaryBackoff = Backoffs.exponential()
         .initial(0, TimeUnit.SECONDS)
         .delay(1, TimeUnit.SECONDS)
         .factor(2.0)
         .random(0.67)
         .max(90, TimeUnit.SECONDS)
         .build();

      this.secondaryBackoff = Backoffs.exponential()
         .initial(9, TimeUnit.SECONDS)
         .delay(1, TimeUnit.SECONDS)
         .factor(2.0)
         .random(0.67)
         .max(90, TimeUnit.SECONDS)
         .build();
   }

   @PostConstruct
   public void initialize() {
      this.port = router.gateway("gtwy", this, ADDRESS, new PortHandler() {
         @Override
         public void recv(Port port, ProtocolMessage message) {
            Gateway.this.recv(port, message);
         }

         @Override
         @Nullable
         public Object recv(Port port, PlatformMessage message) throws Exception {
            throw new UnsupportedOperationException("should not be delivered to addressable endpoint");
         }

         @Override
         public void recv(Port port, Object message) {
            throw new UnsupportedOperationException("should not be delievered to addressable endpoint");
         }
      });

      MetricsService.addListener(this);
      IrisAgentLogging.getInMemoryAppender().addListener(this);
      LifeCycleService.addListener(this);
   }

   @WarmUp
   public void start() {
      log.info("attempting to start hub gateway...");
      networkChecker.start();

      final GatewayNetty.Provider nettyProvider = GatewayNetty.create();
      final GatewayNetty.SslProvider sslProvider = GatewayNetty.createSslProvider();
      final GatewayPokeHandler pokeHandler = new GatewayPokeHandler();
      this.eventLoopGroup = nettyProvider.createEventLoopGroup(); 

      LifeCycleService.setState(LifeCycle.CONNECTING);

      GatewayConnection.primaryConnection(nettyProvider, sslProvider, pokeHandler, eventLoopGroup)
         .retryWhen(RxIris.retry(primaryBackoff))
         .subscribe(new RxIris.Subscriber<GatewayConnection>() {
         @Override
         public void processNext(GatewayConnection conn) {
            conn.registered().subscribe(new HandleRegisteredEvent(conn));
            conn.authorized().subscribe(new HandleAuthorizedEvent(conn));
            conn.platform().subscribe(new HandlePlatformMessageEvent(port));
            conn.protocol().subscribe(new HandleProtocolMessageEvent(port));

            conn.onChannelClosed(new HandleDisconnectedEvent(conn));
            conn.onHandshakeCompleted(new HandleConnectedEvent(primaryBackoff, conn));
         }

         @Override
         public void processError(Throwable e) {
            log.warn("primary gateway connection process failed:",e);
         }

         @Override
         public void processCompleted() {
            log.warn("primary gateway connection process complete");
         }
      });

      // NOTE: The secondary interface is brought up after a delay to give the primary connection
      //       enought time to attempt to connect. This startup delay is controlled by the 
      //       SECONDARY_DELAY_BEFORE_INITIAL_CONNECT constant.
      checkAllowSecondary(SECONDARY_DELAY_BEFORE_INITIAL_CONNECT, TimeUnit.NANOSECONDS);
      GatewayConnection.secondaryConnection(nettyProvider, sslProvider, pokeHandler, eventLoopGroup, allowSecondaryConnection)
         .doOnError(new RxIris.Action1<Throwable>() {
            @Override
            public void run(Throwable error) {
               // If the secondary gateway connection is currently not allowed then
               // reset the backoff algorithm because that isn't really a failure and
               // we want to attempt a connection on the secondary connection quickly
               // when we decide to fail over.
               if (error instanceof GatewayConnectionNotAllowedException) {
                  secondaryBackoff.onSuccess();
               }
            }
         }).retryWhen(RxIris.retry(secondaryBackoff))
         .subscribe(new RxIris.Subscriber<GatewayConnection>() {
         @Override
         public void processNext(GatewayConnection conn) {
            conn.registered().subscribe(new HandleRegisteredEvent(conn));
            conn.authorized().subscribe(new HandleAuthorizedEvent(conn));
            conn.platform().subscribe(new HandlePlatformMessageEvent(port));
            conn.protocol().subscribe(new HandleProtocolMessageEvent(port));

            conn.onChannelClosed(new HandleDisconnectedEvent(conn));
            conn.onHandshakeCompleted(new HandleConnectedEvent(secondaryBackoff, conn));
         }

         @Override
         public void processError(Throwable e) {
            log.warn("secondary gateway connection process failed:",e);
         }

         @Override
         public void processCompleted() {
            log.warn("secondary gateway connection process complete");
         }
      });

      // NOTE: We divide by two here because the pong response will necessarily come in after the
      //       last ping request. If we used the exact ping frequency requested then that would
      //       mean that we skip every other ping because of a check that happens that makes sure
      //       we don't ping if we have gotten a platform message during the ping interval.
      eventLoopGroup.scheduleWithFixedDelay(primaryPingSender, GatewayHandler.PING_FREQ, GatewayHandler.PING_FREQ, TimeUnit.NANOSECONDS);
      eventLoopGroup.scheduleWithFixedDelay(secondaryPingSender, GatewayHandler.PING_FREQ, GatewayHandler.PING_FREQ, TimeUnit.NANOSECONDS);
      eventLoopGroup.scheduleWithFixedDelay(logSender, LOG_SEND_FREQ, LOG_SEND_FREQ, TimeUnit.NANOSECONDS);
   }

   @PreDestroy
   @SuppressWarnings("null")
   public void shutdown() {
      log.info("attempting to stop hub gateway...");
      if (this.eventLoopGroup != null) {
         try {
            GatewayConnection conn =currentConnection.get();
            if (conn != null) {
               conn.disconnect();
            }

            Future<?> shutdown = eventLoopGroup.shutdownGracefully(1, 10, TimeUnit.SECONDS);
            shutdown.await(10, TimeUnit.SECONDS);
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
         }
      }
   }

   @Override
   public void onMetricsCollected(JsonObject metrics) {
      GatewayConnection conn = currentConnection.get();
      if (conn != null) {
         conn.sendMetrics(metrics);
      }
   }

   @Override
   public void appendLogEntry(ILoggingEvent event) {
      JsonObject object = new JsonObject();
      for(Map.Entry<String, String> e: event.getMDCPropertyMap().entrySet()) {
         object.addProperty(e.getKey(), e.getValue());
      }

      object.addProperty("ts", event.getTimeStamp());
      object.addProperty("lvl", event.getLevel().toString());
      object.addProperty("thd", event.getThreadName());
      object.addProperty("log", event.getLoggerName());
      object.addProperty("msg", event.getFormattedMessage());

      IThrowableProxy thrw = event.getThrowableProxy();
      if (thrw != null) {
         String stackTrace = converter.convert(event);
         object.addProperty("exc", stackTrace);
      } else {
         object.remove("exc");
      }

      if (!bufferedLogMessages.offer(object)) {
         log.trace("log message dropped because queue overflowed");
      }
   }

   @Override
   public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {
      GatewayConnection current = currentConnection.get();
      if (LifeCycleService.isConnectedState(oldState) && LifeCycle.CONNECTING == newState && current != null) {
         if (current.isPrimary()) {
            log.warn("gateway attempting to reconnect due to internal request");
            current.disconnect();
         }
      }
   }

   @Override
   public void hubAccountIdUpdated(@Nullable UUID oldAcc, @Nullable UUID newAcc) {
      // ignore
   }

   @Override
   public void hubReset(Reset type) {
      // ignore
   }

   @Override
   public void hubDeregistered() {
      // ignore
   }

   @Override
   public boolean isInterestedIn(PlatformMessage message) {
      Address address = message.getDestination();
      if (address != null && address.isHubAddress() && !address.isBroadcast()) {
         return false;
      }
   
      return true;
   }

   @Override
   public boolean isInterestedIn(ProtocolMessage message) {
      // The gateway only wants to receive protocol messages after
      // they have been processed by the reflex controller so we
      // don't snoop them and instead rely on them being forwarded
      // from the reflex controller when appropriate.
      return false;
   }

   @Override
   @Nullable
   public Object recv(Port port, PlatformMessage message) throws Exception {
      GatewayConnection conn = currentConnection.get();
      if (conn == null || !conn.send(message)) {
         if (!queue.queueIfNeeded(message)) {
            log.trace("no gateway connection, dropping message: {}", message);
         }
      }

      return null;
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
      GatewayConnection conn = currentConnection.get();
      if (conn == null || !conn.send(message)) {
         if (!queue.queueIfNeeded(message)) {
            log.trace("no gateway connection, dropping message: {}", message);
         }
      }
   }

   @Override
   public void recv(Port port, Object message) {
      throw new UnsupportedOperationException();
   }

   private void checkAllowSecondary(long time, TimeUnit unit) {
      eventLoopGroup.schedule(new Runnable() {
         @Override
         public void run() {
            GatewayConnection checkCur = currentConnection.get();
            if (checkCur == null || !checkCur.isPrimary()) {
               log.warn("there are no active gateway connections, allowing secondary connections");
               allowSecondaryConnection.set(true);
            }
         }
      }, time, unit);
   }

   private boolean updateCurrentConnection() {
      GatewayConnection cur = currentConnection.get();
      GatewayConnection pri = primaryConnection.get();
      GatewayConnection sec = secondaryConnection.get();

      if (pri == null && sec == null) {
         log.info("there are no active gateway connections");
         checkAllowSecondary(SECONDARY_DELAY_BEFORE_CONNECT, TimeUnit.NANOSECONDS);

         GatewayConnection old = currentConnection.getAndSet(null);
         if (old != null) {
            old.disconnect();
         }

         LifeCycleService.setState(LifeCycle.CONNECTING);
         networkChecker.markInterfaceChanged();
         return true;
      }

      if (pri != null && cur != pri) {
         log.info("switching to primary gateway connection");
         allowSecondaryConnection.set(false);
         if (FourgService.isAuthorized()) {
            FourgService.setState(FourgService.State.CONNECTED);
         }

         GatewayConnection old = currentConnection.getAndSet(pri);
         if (old != null) {
            old.disconnect();
         }

         networkChecker.markInterfaceChanged();
         return true;
      }

      if (sec != null && pri == null) {
         log.info("switching to secondary gateway connection");
         allowSecondaryConnection.set(true);
         FourgService.setState(FourgService.State.AUTHORIZED);

         GatewayConnection old = currentConnection.getAndSet(sec);
         if (old != null) {
            old.disconnect();
         }

         networkChecker.markInterfaceChanged();
         return true;
      }

      return false;
   }
  
   private final class HandleConnectedEvent implements rx.Observer<Object> {
      private final Backoff backoff;
      private final GatewayConnection conn;

      HandleConnectedEvent(Backoff backoff, GatewayConnection conn) {
         this.backoff = backoff;
         this.conn = conn;
      }

      @Override
      public void onCompleted() {
         GatewayConnection cur = currentConnection.get();
         String nname = conn.isPrimary() ? "primary" : "secondary";
         String aname = (cur == null) ? "unknown" : (cur.isPrimary() ? "primary" : "secondary");

         AtomicReference<GatewayConnection> ref = conn.isPrimary() ? primaryConnection : secondaryConnection;
         GatewayConnection old = ref.getAndSet(conn);
         if (old != null) {
            log.warn("{} gateway connection marked active while old one still alive, disconnnecting old connection", nname);
            old.disconnect();
         }

         if (!updateCurrentConnection()) {
            log.warn("{} gateway connection active but {} still connected, closing down {} connection", nname, aname, nname);
            conn.disconnect();
            return;
         }

         log.info("sending connected event on {} interface", conn.isPrimary() ? "primary" : "secondary");

         IrisHal.NetworkInfo ni = IrisHal.getNetworkInfo();
         ip.set(conn.isPrimary() ? ni.primaryIp : ni.secondaryIp);
         netmask.set(conn.isPrimary() ? ni.primaryNetmask : ni.secondaryNetmask);
         type.set(conn.isPrimary() ? ni.primaryInterfaceType : ni.secondaryInterfaceType);

         PingSender ping = conn.isPrimary() ? primaryPingSender : secondaryPingSender;
         ping.markConnected();

         backoff.onSuccess();
         
         Address addr = Address.hubService(HubAttributesService.getHubId(), "hub");

         MessageBody msg = MessageBody.buildMessage(MessageConstants.MSG_HUB_CONNECTED_EVENT, HubAttributesService.asAttributeMap(false,true,false));
         conn.send(PlatformMessage.buildEvent(msg, addr).create(), false);

         LifeCycleService.setState(LifeCycle.CONNECTED);
      }

      @Override
      public void onError(@Nullable Throwable error) {
         conn.disconnect();
      }

      @Override public void onNext(@Nullable Object _unused) { }
   }
  
   private final class HandleDisconnectedEvent implements rx.Observer<Object> {
      private final GatewayConnection conn;

      HandleDisconnectedEvent(GatewayConnection conn) {
         this.conn = conn;
      }

      @Override
      public void onCompleted() {
         log.info("{} gateway connection was disconnected", conn.isPrimary() ? "primary" : "secondary");
         PingSender ping = conn.isPrimary() ? primaryPingSender : secondaryPingSender;
         ping.markDisconnected();

         AtomicReference<GatewayConnection> ref = conn.isPrimary() ? primaryConnection : secondaryConnection;
         if (ref.compareAndSet(conn, null)) {
            updateCurrentConnection();
         }
      }

      @Override public void onError(@Nullable Throwable error) { }
      @Override public void onNext(@Nullable Object _unused) { }
   }
   
   private static final class HandleRegisteredEvent implements rx.Observer<PlatformMessage> {
      private final GatewayConnection conn;

      HandleRegisteredEvent(GatewayConnection conn) {
         this.conn = conn;
      }

      @Override
      public void onNext(@Nullable PlatformMessage registered) {
         if (registered == null) {
            log.warn("invalid register request: null");
            return;
         }

         UUID oldAcc = HubAttributesService.getAccountId();
         if (oldAcc != null) {
            throw new RuntimeException("hub already registered to account: " + HubAttributesService.getAccountId());
         }

         boolean updated = HubAttributesService.updateAttributes(registered.getValue().getAttributes());
         if (updated) {
            log.info("updated hub attributes: acccount={}, place={}", HubAttributesService.getAccountId(), HubAttributesService.getPlaceId());
            LifeCycleService.fireHubRegistered(oldAcc, HubAttributesService.getAccountId());
         } else {
            throw new RuntimeException("invalid register request");
         }
      
         Address addr = Address.hubService(HubAttributesService.getHubId(), "hub");
         PlatformMessage pmsg = null;
         try {
            MessageBody rsp = MessageBody.buildMessage(MessageConstants.MSG_HUB_REGISTERED_RESPONSE, Collections.<String,Object>emptyMap());
            pmsg = PlatformMessage.buildResponse(registered, rsp, addr).create();
         } catch (Exception ex) {
            ErrorEvent error = ErrorEvent.fromException(ex);
            pmsg = PlatformMessage.buildResponse(registered, error, addr).create();
         } finally {
            if (pmsg != null) {
               conn.send(pmsg, false);
            }
         }
      }

      @Override public void onError(@Nullable Throwable error) { }
      @Override public void onCompleted() { }
   }
   
   private final class HandleAuthorizedEvent implements rx.Observer<PlatformMessage> {
      private final GatewayConnection conn;

      HandleAuthorizedEvent(GatewayConnection conn) {
         this.conn = conn;
      }

      @Override
      public void onNext(@Nullable PlatformMessage authorized) {
         PingSender ping = conn.isPrimary() ? primaryPingSender : secondaryPingSender;
         ping.markAuthorized();

         long time = System.nanoTime();
         while (true) {
            Object message = queue.take(time);
            if (message == null) {
               break;
            }
      
            if (message instanceof PlatformMessage) {
               conn.send((PlatformMessage)message);
            } else if (message instanceof ProtocolMessage) {
               conn.send((ProtocolMessage)message);
            }
         }

         LifeCycleService.setState(LifeCycle.AUTHORIZED);
      }

      @Override public void onError(@Nullable Throwable error) { }
      @Override public void onCompleted() { }
   }
   
   private static final class HandlePlatformMessageEvent implements rx.Observer<PlatformMessage> {
      private final Port port;

      HandlePlatformMessageEvent(Port port) {
         this.port = port;
      }

      @Override
      public void onNext(@Nullable PlatformMessage message) {
         if (message != null) {
            port.queue(message);
         }
      }

      @Override public void onError(@Nullable Throwable error) { }
      @Override public void onCompleted() { }
   }
   
   private static final class HandleProtocolMessageEvent implements rx.Observer<ProtocolMessage> {
      private final Port port;

      HandleProtocolMessageEvent(Port port) {
         this.port = port;
      }

      @Override
      public void onNext(@Nullable ProtocolMessage message) {
         if (message != null) {
            port.queue(message);
         }
      }

      @Override public void onError(@Nullable Throwable error) { }
      @Override public void onCompleted() { }
   }
   
   private final class PingSender implements Runnable {
      private final boolean primary;
      private long lastConnReport;
      private long lastAuthTime;
      private long lastConnTime;
      private long lastDisconnTime;

      PingSender(boolean primary) {
         this.primary = primary;
         this.lastConnReport = Long.MIN_VALUE;
         this.lastAuthTime = Long.MIN_VALUE;
         this.lastConnTime = Long.MIN_VALUE;
         this.lastDisconnTime = System.currentTimeMillis();
      }

      void markConnected() {
         lastConnTime = System.currentTimeMillis();
      }

      void markAuthorized() {
         lastAuthTime = System.currentTimeMillis();
      }

      void markDisconnected() {
         if (lastDisconnTime > lastConnTime) {
            return;
         }

         lastDisconnTime = System.currentTimeMillis();
      }

      @Override
      public void run() {
         try {
            String type = primary ? "primary" : "secondary";
            long time = System.currentTimeMillis();
            long timeSinceLastReport = time - lastConnReport;

            GatewayConnection conn = primary ? primaryConnection.get() : secondaryConnection.get();
            if (conn == null) {
               if (lastConnReport == Long.MIN_VALUE || timeSinceLastReport >= CONN_REPORT_INTERVAL) {
                  lastConnReport = time;

                  long disconnTime = (lastDisconnTime == Long.MIN_VALUE) ? time : lastDisconnTime;
                  long timeSinceLastDisconn = time - disconnTime;
                  if (primary) {
                     log.info("{} gateway connection has been disconnected for {}m", type, TimeUnit.MINUTES.convert(timeSinceLastDisconn, TimeUnit.MILLISECONDS));
                  }
               }

               return;
            }

            long connTime = (lastConnTime == Long.MIN_VALUE) ? time : lastConnTime;
            long authTime = (lastAuthTime == Long.MIN_VALUE) ? time : lastAuthTime;

            long timeSinceLastConn = time - connTime;
            long timeSinceLastAuth = time - authTime;
            long timeConnButNotAuth = timeSinceLastConn - timeSinceLastAuth;

            if (lastConnReport == Long.MIN_VALUE || timeSinceLastReport >= CONN_REPORT_INTERVAL) {
               lastConnReport = time;
               log.info("{} gateway connection times: conn={}m, auth={}m", type, TimeUnit.MINUTES.convert(timeSinceLastConn, TimeUnit.MILLISECONDS), TimeUnit.MINUTES.convert(timeSinceLastAuth, TimeUnit.MILLISECONDS));
            }

            if (HubAttributesService.getAccountId() != null && timeConnButNotAuth > UNAUTH_FORCE_RECONNECT_TIME) {
               log.warn("hub has an account id and {} gateway connection is connected but has not been authorized for {}m, forcing reconnect...", type, TimeUnit.MINUTES.convert(timeConnButNotAuth, TimeUnit.MILLISECONDS));
               conn.disconnect();
               return;
            }

            conn.sendPing();
         } catch (Exception ex) {
            log.debug("failed to run ping sender", ex);
         }
      }
   }

   private final class LogSender implements Runnable {
      @Override
      public void run() {
         try {
            GatewayConnection conn = currentConnection.get();
            if (conn != null && conn.isPrimary()) {
               conn.sendLogs(bufferedLogMessages);
            }
         } catch (Exception ex) {
            // ignore
         }
      }
   }

   private final class ConnectionWatchdogCheck implements WatchdogCheck {
      long lastCheckConnectedInNs = System.nanoTime();

      @Override
      public String name() {
         return "gateway connection";
      }

      @Override
      public boolean check(long nowInNs) throws Exception {
         GatewayConnection conn = currentConnection.get();
         boolean connected = conn != null && conn.isConnected() &&
            conn.timeSinceLastPlatformMessage(TimeUnit.MILLISECONDS) < GatewayHandler.IDLE_TIMEOUT;
         if (connected) {
            lastCheckConnectedInNs = nowInNs;
         }

         long elapsed = nowInNs - lastCheckConnectedInNs;
         return elapsed < UNCONN_FORCE_REBOOT_TIME;
      }
   }
}

