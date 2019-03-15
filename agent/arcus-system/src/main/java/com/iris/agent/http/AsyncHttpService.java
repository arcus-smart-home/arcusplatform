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
package com.iris.agent.http;

import java.net.URI;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.channel.NoopChannelPool;
import org.asynchttpclient.netty.ssl.DefaultSslEngineFactory;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.ssl.SslKeyStore;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContextBuilder;

public final class AsyncHttpService {
   private static final int DEFAULT_HANDSHAKE_TIMEOUT = (int)TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);
   private static final int DEFAULT_REQUEST_TIMEOUT = (int)TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);

   private static @Nullable AsyncHttpClient client;
   private static EventLoopGroup evlg;
   private static boolean useEpoll;
   private static boolean useOpenSsl;

   private AsyncHttpService() {
   }

   static void start() {
      final AtomicLong counter = new AtomicLong();
      ThreadFactory tf = new ThreadFactory() {
         @Override
         public Thread newThread(@Nullable Runnable runnable) {
            Thread thr = new Thread(runnable);
            thr.setName("ahc" + counter.getAndIncrement());
            thr.setDaemon(true);
            return thr;
         }
      };

      useEpoll = Epoll.isAvailable();
      useOpenSsl = false;

      evlg = useEpoll ? new EpollEventLoopGroup(2,tf) : new NioEventLoopGroup(2,tf);

      DefaultAsyncHttpClientConfig config = builder().build();
      client = new DefaultAsyncHttpClient(config);
   }

   static void shutdown() {
      AsyncHttpClient clnt = client;
      if (clnt != null) {
         try {
            clnt.close();
         } catch (Exception ex) {
         }
      }

      client = null;
   }

   private static AsyncHttpClient get() {
      AsyncHttpClient result = client;
      if (result == null) {
         throw new IllegalStateException("async http service not started");
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Custom Async Http Client Builder
   /////////////////////////////////////////////////////////////////////////////
   
   public static DefaultAsyncHttpClientConfig.Builder builder() {
      try {
         return new DefaultAsyncHttpClientConfig.Builder()
            .setUseOpenSsl(useOpenSsl)
            .setUseNativeTransport(useEpoll)
            .setEventLoopGroup(evlg)
            .setChannelPool(NoopChannelPool.INSTANCE)
            .setHandshakeTimeout(DEFAULT_HANDSHAKE_TIMEOUT)
            .setRequestTimeout(DEFAULT_REQUEST_TIMEOUT);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static DefaultAsyncHttpClientConfig.Builder builderWithClientCertificates() {
      try {
         return new DefaultAsyncHttpClientConfig.Builder()
            .setUseNativeTransport(useEpoll)
            .setEventLoopGroup(evlg)
            .setChannelPool(NoopChannelPool.INSTANCE)
            .setHandshakeTimeout(DEFAULT_HANDSHAKE_TIMEOUT)
            .setRequestTimeout(DEFAULT_REQUEST_TIMEOUT)
            .setSslEngineFactory(new DefaultSslEngineFactory() {
               @Override
               protected SslContextBuilder configureSslContextBuilder(@Nullable SslContextBuilder builder) {
                  if (builder == null) throw new NullPointerException();

                  SslContextBuilder updated = builder
                     .trustManager(SslKeyStore.getFingerPrintTrustManagerFactory())
                     .keyManager(SslKeyStore.getKeyManagerFactory());

                  return super.configureSslContextBuilder(updated);
               }
            });
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // HTTP request utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static BoundRequestBuilder get(String uri) {
      return get().prepareGet(uri);
   }

   public static BoundRequestBuilder get(URI uri) {
      return get(uri.toString());
   }
}

