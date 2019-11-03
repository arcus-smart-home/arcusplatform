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

import java.security.Security;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.iris.agent.config.ConfigService;
import com.iris.agent.ssl.SslKeyStore;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContextBuilder;

class GatewayNetty {
   private static final Logger log = LoggerFactory.getLogger(GatewayNetty.class);

   private GatewayNetty() {
   }

   public static Provider create() {
      Supplier<String> nettyProvider = ConfigService.supplier("iris.gateway.provider", String.class, "");
      switch (nettyProvider.get()) {
      case "epoll":
         if (Epoll.isAvailable()) {
            log.debug("using netty epoll provider for gateway connection");
            return epoll();
         } else {
            if (!"".equals(nettyProvider.get())) {
               log.warn("netty epoll provider requested but not available, using nio for gateway connection:", Epoll.unavailabilityCause());
            } else {
               log.debug("using netty nio provider for gateway connection");
            }
            return nio();
         }

      case "":
      case "nio":
         log.debug("using netty nio provider for gateway connection");
         return nio();

      default:
         log.warn("unknown netty provider, using nio by default");
         return nio();
      }
   }

   public static SslProvider createSslProvider() {
      Supplier<String> sslProvider = ConfigService.supplier("iris.gateway.ssl.provider", String.class, "");

      Security.addProvider(new BouncyCastleProvider());

      switch (sslProvider.get()) {
      case "":
      case "openssl":
         if (OpenSsl.isAvailable()) {
            log.debug("using openssl for gateway ssl provider");
            return openssl();
         } else {
            if (!"".equals(sslProvider.get())) {
               log.warn("openssl ssl provider requested but not available, using jdk ssl for gateway connection:", OpenSsl.unavailabilityCause());
            } else {
               log.debug("using jdk for gateway ssl provider: ", OpenSsl.unavailabilityCause());
            }
            return jdk();
         }

      case "jdk":
         log.debug("using jdk for gateway ssl provider");
         return jdk();

      default:
         log.warn("unknown ssl provider, using jdk by default");
         return jdk();
      }
   }

   public static Provider nio() {
      return NioProvider.INSTANCE;
   }

   public static Provider epoll() {
      return EpollProvider.INSTANCE;
   }

   public static SslProvider openssl() {
      return OpenSslProvider.INSTANCE;
   }

   public static SslProvider jdk() {
      return JdkSslProvider.INSTANCE;
   }

   public interface Provider {
      EventLoopGroup createEventLoopGroup();
      Class<? extends SocketChannel> getSocketChannelClass();
   }

   public interface SslProvider {
      SslContextBuilder get();
      void setupClientCertificates(SslContextBuilder builder);
   }

   public enum NioProvider implements Provider {
      INSTANCE;

      @Override
      public Class<? extends SocketChannel> getSocketChannelClass() {
         return NioSocketChannel.class;
      }

      @Override
      public EventLoopGroup createEventLoopGroup() {
         return new NioEventLoopGroup(2,new GatewayThreadFactory());
      }
   }

   public enum EpollProvider implements Provider {
      INSTANCE;

      @Override
      public Class<? extends SocketChannel> getSocketChannelClass() {
         return EpollSocketChannel.class;
      }

      @Override
      public EventLoopGroup createEventLoopGroup() {
         return new EpollEventLoopGroup(2,new GatewayThreadFactory());
      }
   }

   public enum OpenSslProvider implements SslProvider {
      INSTANCE;

      @Override
      public SslContextBuilder get() {
         return SslContextBuilder.forClient().sslProvider(io.netty.handler.ssl.SslProvider.OPENSSL);
      }

      @Override
      public void setupClientCertificates(SslContextBuilder builder) {
         builder.trustManager(SslKeyStore.getTrustedBridgeCertificates())
                .keyManager(SslKeyStore.readHubPrivateKey(), SslKeyStore.readHubCertificateAsArray());
      }
   }

   public enum JdkSslProvider implements SslProvider {
      INSTANCE;

      @Override
      public SslContextBuilder get() {
         return SslContextBuilder.forClient().sslProvider(io.netty.handler.ssl.SslProvider.JDK);
      }

      @Override
      public void setupClientCertificates(SslContextBuilder builder) {
         builder.trustManager(SslKeyStore.getTrustManagerFactory())
                .keyManager(SslKeyStore.getKeyManagerFactory());
      }
   }

   private static final class GatewayThreadFactory implements ThreadFactory {
      private final AtomicLong num = new AtomicLong();

      @Override
      public Thread newThread(@Nullable Runnable r) {
         if (r == null) throw new NullPointerException("runnable");

         Thread thr = new Thread(r);
         thr.setName("gtwy" + num.getAndIncrement());
         return thr;
      }
   }
}

