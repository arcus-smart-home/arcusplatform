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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.gson.JsonObject;
import com.iris.agent.config.ConfigService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.util.RxIris;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.ProtocolMessage;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

public class GatewayConnection {
   private static final Logger LOG = LoggerFactory.getLogger(GatewayConnection.class.getName());
   private static final Logger PRIMARY_LOG = LoggerFactory.getLogger(GatewayConnection.class.getName() + ".pri");
   private static final Logger SECONDARY_LOG = LoggerFactory.getLogger(GatewayConnection.class.getName() + ".sec");

   private static final String[] ALLOWED_CIPHERS = new String[] {
      "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
   };

   public static final int WEBSOCKETS_MAX_FRAME_LENGTH = 1 * 1024 * 1024; // 1 MB
   private static final Supplier<Integer> CONNECT_TIMEOUT = ConfigService.supplier("iris.gateway.timeout.connect", Integer.class, 90000);
   private static final Supplier<Long> SSL_HANDSHAKE_TIMEOUT = ConfigService.supplier("iris.gateway.timeout.ssl.handshake", Long.class, 90000L);
   private static final Supplier<Long> SSL_CLOSE_NOTIFY_TIMEOUT = ConfigService.supplier("iris.gateway.timeout.ssl.closenotify", Long.class, 15000L);
   private static final Supplier<Long> FAILURES_BEFORE_FALLBACK = ConfigService.supplier("iris.gatway.fallback.fails", Long.class, 25L);
   private static final Supplier<String> configConnectUri = ConfigService.supplier("iris.gateway.uri", "wss://bh.irisbylowes.com/hub/1.0");

   private final GatewayHandler handler;
   private final boolean isPrimary;

   private GatewayConnection(GatewayHandler handler, boolean isPrimary) {
      this.handler = handler;
      this.isPrimary = isPrimary;
   }

   public boolean isPrimary() {
      return isPrimary;
   }

   boolean isConnected() {
      return handler.isConnected();
   }
   
   long timeSinceLastPlatformMessage(TimeUnit unit) {
      return handler.timeSinceLastPlatformMessage(unit);
   }

   public SocketAddress getOutboundInterface() {
      return handler.getOutboundInterface();
   }

   public void disconnect() {
      handler.close();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gateway connection events
   /////////////////////////////////////////////////////////////////////////////

   public void onChannelClosed(Observer<?> obs) {
      handler.onChannelClosed(obs);
   }

   public void onHandshakeCompleted(Observer<?> obs) {
      handler.onHandshakeComplete(obs);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gateway connection inbound/outbound messages
   /////////////////////////////////////////////////////////////////////////////
   
   public boolean send(PlatformMessage msg) {
      return handler.send(msg);
   }
   
   public boolean send(PlatformMessage msg, boolean checkAuth) {
      return handler.send(msg, checkAuth);
   }

   public boolean send(ProtocolMessage msg) {
      return handler.send(msg);
   }

   public void sendPing() {
      handler.sendPing();
   }

   public void sendLogs(BlockingQueue<JsonObject> queue) {
      handler.sendLogs(queue);
   }

   public void sendMetrics(JsonObject metrics) {
      handler.sendMetrics(metrics);
   }

   public Observable<PlatformMessage> registered() {
      return handler.registered();
   }

   public Observable<PlatformMessage> authorized() {
      return handler.authorized();
   }

   public Observable<PlatformMessage> platform() {
      return handler.platform();
   }

   public Observable<ProtocolMessage> protocol() {
      return handler.protocol();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Gateway connection creation
   /////////////////////////////////////////////////////////////////////////////

   public static Observable<GatewayConnection> primaryConnection(
      GatewayNetty.Provider nettyProvider, GatewayNetty.SslProvider sslProvider,
      GatewayPokeHandler pokeHandler, EventLoopGroup eventLoopGroup) {
      return getConnection(true, primaryOutboundInterface(), nettyProvider, sslProvider, pokeHandler, eventLoopGroup, new AtomicBoolean(true));
   }

   public static Observable<GatewayConnection> secondaryConnection(
      GatewayNetty.Provider nettyProvider, GatewayNetty.SslProvider sslProvider,
      GatewayPokeHandler pokeHandler, EventLoopGroup eventLoopGroup,
      AtomicBoolean allowSecondaryConnection) {
      return getConnection(false, secondaryOutboundInterface(), nettyProvider, sslProvider, pokeHandler, eventLoopGroup, allowSecondaryConnection);
   }

   public static Observable<GatewayConnection> getConnection(
      final boolean isPrimary, Observable<InetAddress> outboundInterface,
      final GatewayNetty.Provider nettyProvider, final GatewayNetty.SslProvider sslProvider,
      final GatewayPokeHandler pokeHandler, final EventLoopGroup eventLoopGroup,
      final AtomicBoolean allowConnection) {

      return outboundInterface.lift(new RxIris.Operator<GatewayConnection,InetAddress>() {
         @Override
         public Subscriber<? super InetAddress> run(final Subscriber<? super GatewayConnection> s) {
            return new RxIris.Subscriber<InetAddress>() {
               @Override
               public void processNext(InetAddress outboundInterface) {
                  if (!allowConnection.get()) {
                     if (isPrimary) {
                        log(isPrimary).warn("disallowing primary gateway connection");
                     } else {
                        log(isPrimary).trace("disallowing secondary gateway connection");
                     }

                     s.onError(GatewayConnectionNotAllowedException.STATIC);
                     return;
                  }

                  try {
                     final URI connectUri = URI.create(configConnectUri.get());
                     String scheme = connectUri.getScheme();
                     if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                        log(isPrimary).warn("hub gateway uri has invalid scheme: {}", connectUri);
                        throw new IllegalArgumentException("Unsupported protocol: " + scheme);
                     }

                     final boolean isSSL = "wss".equalsIgnoreCase(scheme);
                     final String host = connectUri.getHost();
                     final int port = (connectUri.getPort() < 0) ? (isSSL ? 443 : 80) : connectUri.getPort();

                     SslContext ctx = null;
                     if (isSSL) {
                           SslContextBuilder builder = sslProvider.get();
                           sslProvider.setupClientCertificates(builder);
                           ctx = builder.build();
                     }

                     Observable<Bootstrap> bootstrap = getBootstrap(connectUri, host, port, nettyProvider, ctx, pokeHandler, eventLoopGroup);
                     connect(connectUri, host, port, bootstrap, outboundInterface).map(new RxIris.Func1<GatewayHandler,GatewayConnection>() {
                        @Override
                        public GatewayConnection run(GatewayHandler handler) {
                           return new GatewayConnection(handler, isPrimary);
                        }
                     }).subscribe(new RxIris.Subscriber<GatewayConnection>() {
                        @Override
                        public void processNext(GatewayConnection conn) {
                           if (!s.isUnsubscribed()) {
                              conn.onChannelClosed(new RxIris.Observer<Object>() {
                                 @Override public void processNext(Object t) { }

                                 @Override
                                 public void processError(Throwable e) {
                                    if (!LifeCycleService.isShutdown()) {
                                       log(isPrimary).info("error on channel closed, attempting to restart: ", e);
                                       s.onError(new Exception("channel closed"));
                                    }
                                 }

                                 @Override
                                 public void processCompleted() {
                                    if (!LifeCycleService.isShutdown()) {
                                       log(isPrimary).info("channel closed, attempting to restart");
                                       s.onError(new Exception("channel closed"));
                                    }
                                 }
                              });

                              s.onNext(conn);
                           }
                        }

                        @Override
                        public void processError(Throwable e) {
                           if (!s.isUnsubscribed()) {
                              s.onError(e);
                           }
                        }

                        @Override
                        public void processCompleted() {
                           // ignore
                        }
                     });
                  } catch (Exception ex) {
                     if (!s.isUnsubscribed()) {
                        s.onError(ex);
                     }
                  }
               }

               @Override
               public void processError(Throwable e) {
                  if (!s.isUnsubscribed()) {
                     s.onError(e);
                  }
               }

               @Override
               public void processCompleted() {
                  // ignore
               }
            };
         }
      });
   }

   private static Observable<GatewayHandler> connect(final URI connectUri, final String host, final int port, 
      Observable<Bootstrap> bootstrap, final InetAddress outboundInterface) {
      return bootstrap.lift(new RxIris.Operator<GatewayHandler,Bootstrap>() {
         @Override
         public Subscriber<? super Bootstrap> run(final Subscriber<? super GatewayHandler> s) {
            return new RxIris.Subscriber<Bootstrap>() {
               @Override
               public void processNext(Bootstrap bs) {
                  try {
                     InetAddress addr = GatewayDns.resolv(host);

                     InetSocketAddress remote = new InetSocketAddress(addr, port);
                     InetSocketAddress local = new InetSocketAddress(outboundInterface, 0);

                     LOG.debug("gateway attempting to connect: remote={}, local={}", addr, outboundInterface);

                     final ChannelFuture chFuture = bs.connect(remote, local);
                     chFuture.addListener(new GenericFutureListener<Future<Object>>() {
                        @Override
                        public void operationComplete(@Nullable Future<Object> future) {
                           try {
                              if (future == null) {
                                 throw new NullPointerException("future");
                              }

                              future.get();
                              GatewayHandler handler = chFuture.channel().pipeline().get(GatewayHandler.class);

                              s.onNext(handler);
                              s.onCompleted();
                           } catch (Exception ex) {
                              if (!s.isUnsubscribed()) {
                                 s.onError(ex);
                              }
                           }
                        }
                     });
                  } catch (Exception ex) {
                     if (!s.isUnsubscribed()) {
                        s.onError(ex);
                     }
                  }
               }

               @Override
               public void processError(Throwable e) {
                  if (!s.isUnsubscribed()) {
                     s.onError(e);
                  }
               }

               @Override
               public void processCompleted() {
                  // ignore
               }
            };
         }
      });
   }

   private static Observable<Bootstrap> getBootstrap(final URI connectUri, final String host, final int port, 
         final GatewayNetty.Provider nettyProvider, final @Nullable SslContext ctx,
         final GatewayPokeHandler pokeHandler, final EventLoopGroup eventLoopGroup) {
      return Observable.create(new RxIris.OnSubscribe<Bootstrap>() {
         @Override
         public void run(Subscriber<? super Bootstrap> sub) {
            try {
               if (LifeCycleService.getState() == LifeCycle.SHUTTING_DOWN) {
                  throw new Exception("agent shutting down, terminating gateway connections");
               }
         
               LOG.trace("setting up gateway connection: connection timeout={}ms, ssl handshake timeout={}ms, ssl close notify timeout={}ms", CONNECT_TIMEOUT.get(), SSL_HANDSHAKE_TIMEOUT.get(), SSL_CLOSE_NOTIFY_TIMEOUT.get());

               ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
                  @Override
                  public void initChannel(final @Nullable SocketChannel ch) throws Exception {
                     if (ch == null) {
                        throw new NullPointerException("ch");
                     }

                     ch.pipeline().addLast(pokeHandler);
                     if (ctx != null) {
                        final SSLEngine sslEngine = ctx.newEngine(ch.alloc(), host, port);
                        final SslHandler sslHandler = new SslHandler(sslEngine);

                        // To protect against SSL downgrade attacks, we are going to set
                        // the enabled set of ciphers to something deemed to be secure.
                        List<String> protocols = Arrays.asList(sslEngine.getSupportedProtocols());
                        List<String> ciphers = Arrays.asList(sslEngine.getSupportedCipherSuites());

                        // If the hub supports TLSv1.2 then use that, otherwise use TLSv1.1. If
                        // neither are supported then it is a fatal error. We don't allow negotiation
                        // with the hub gateway here to mitigate SSL downgrade attacks (the hub
                        // doesn't really need to be backwards compatible with older standards
                        // because we control the stack end-to-end).
                        if (protocols.contains("TLSv1.2")) {
                           sslEngine.setEnabledProtocols(new String[] {"TLSv1.2"});
                        } else {
                           LOG.error("!!!! SSL PROTOCOL COULD NOT BE SELECTED, ALLOWING ALL DEFAULTS");
                        }

                        ArrayList<String> enabled = new ArrayList<>();
                        for (String allowed : ALLOWED_CIPHERS) {
                           if (ciphers.contains(allowed)) {
                              enabled.add(allowed);
                           }
                        }

                        if (!enabled.isEmpty()) {
                           String[] suites = enabled.toArray(new String[enabled.size()]);
                           sslEngine.setEnabledCipherSuites(suites);
                        } else {
                           LOG.error("!!!! SSL CIPHER SUITE COULD NOT BE SELECTED, ALLOWING ALL DEFAULTS");
                        }

                        if (LOG.isTraceEnabled()) {
                           LOG.trace("enabled protocols: {}", Arrays.toString(sslHandler.engine().getEnabledProtocols()));
                           LOG.trace("enabled ciphers: {}", Arrays.toString(sslHandler.engine().getEnabledCipherSuites()));
                        }

                        sslHandler.setHandshakeTimeout(SSL_HANDSHAKE_TIMEOUT.get(), TimeUnit.MILLISECONDS);
                        sslHandler.setCloseNotifyTimeout(SSL_CLOSE_NOTIFY_TIMEOUT.get(), TimeUnit.MILLISECONDS);

                        final long connectAttemptTime = System.nanoTime();
                        sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<Object>>() {
                           @Override
                           public void operationComplete(@Nullable Future<Object> future) throws Exception {
                              long elapsed = System.nanoTime() - connectAttemptTime;
                              if (future != null && future.isSuccess()) {
                                 LOG.info("ssl handshake completed successfully in {} ms", TimeUnit.NANOSECONDS.toMillis(elapsed));
                                 LOG.trace("ssl session: protocol={}, cipher={}", sslHandler.engine().getSession().getProtocol(), sslHandler.engine().getSession().getCipherSuite());
                              } else {
                                 LOG.warn("ssl handshake failed after {} ms: {}", elapsed, (future == null) ? "unknown" : future.cause());
                                 ch.close();
                              }
                           }
                        });
                        ch.pipeline().addLast("ssl", sslHandler);
                     }

                     WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(connectUri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), WEBSOCKETS_MAX_FRAME_LENGTH);
                     GatewayHandler handler = new GatewayHandler(handshaker);

                     ch.pipeline()
                     .addLast("http-codec", new HttpClientCodec())
                     .addLast("aggregator", new HttpObjectAggregator(WEBSOCKETS_MAX_FRAME_LENGTH))
                     .addLast("iris-handler", handler);
                  }
               };

               Bootstrap bootstrap = new Bootstrap();
               bootstrap.group(eventLoopGroup)
                        .channel(nettyProvider.getSocketChannelClass())
                        .handler(initializer)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .option(ChannelOption.ALLOW_HALF_CLOSURE, false)
                        .option(ChannelOption.AUTO_CLOSE, true)
                        .option(ChannelOption.AUTO_READ, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT.get())
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.SO_LINGER, 0)
                        .option(ChannelOption.TCP_NODELAY, true)
                        ;

               sub.onNext(bootstrap);
               sub.onCompleted();
            } catch (Exception ex) {
               if (!sub.isUnsubscribed()) {
                  sub.onError(ex);
               }
            }
         }
      });
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility methods for outbound interfaces
   /////////////////////////////////////////////////////////////////////////////

   private static Observable<InetAddress> primaryOutboundInterface() {
      return getOutboundInterface(true);
   }

   private static Observable<InetAddress> secondaryOutboundInterface() {
      return getOutboundInterface(false);
   }

   private static Observable<InetAddress> getOutboundInterface(final boolean primary) {
      return Observable.create(new RxIris.OnSubscribe<InetAddress>() {
         @Override
         @SuppressWarnings("null")
         public void run(Subscriber<? super InetAddress> sub) {
            try {
               if (!sub.isUnsubscribed()) {
                  IrisHal.NetworkInfo ni = IrisHal.getNetworkInfo();
                  NetworkInterface intf = (ni == null) ? null : (primary ? ni.primary : ni.secondary);
                  if (intf == null) {
                     throw new Exception("cannot determine " + (primary ? "primary" : "secondary") + " network interface");
                  }

                  sub.onNext(getInetAddress(primary, primary ? ni.primary : ni.secondary));
                  sub.onCompleted();
               }
            } catch (Exception ex) {
               if (!sub.isUnsubscribed()) {
                  sub.onError(ex);
               }
            }
         }
      });
   }


   private static InetAddress getInetAddress(boolean primary, @Nullable NetworkInterface ni) throws Exception {
      if (ni == null) {
         throw new Exception("cannot determine " + (primary ? "primary" : "secondary") + " network interface");
      }

      InetAddress best = null;
      Enumeration<InetAddress> addrs = ni.getInetAddresses();
      while (addrs.hasMoreElements()) {
         InetAddress next = addrs.nextElement();
         if (best == null || next instanceof Inet4Address) {
           best = next;
         }
      }

      if (best == null) {
         throw new Exception("cannot determine network address for interface: " + ni);
      }

      return best;
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   
   private static Logger log(boolean isPrimary) {
      return isPrimary ? PRIMARY_LOG : SECONDARY_LOG;
   }
}

