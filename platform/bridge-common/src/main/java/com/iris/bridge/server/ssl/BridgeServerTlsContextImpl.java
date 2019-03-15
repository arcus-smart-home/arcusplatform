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
package com.iris.bridge.server.ssl;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.config.BridgeServerConfig;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

@Singleton
public class BridgeServerTlsContextImpl implements BridgeServerTlsContext {
   private static final Logger logger = LoggerFactory.getLogger(BridgeServerTlsContextImpl.class);

   private final boolean useTls;
   protected final SslContext context;

   @Inject
   public BridgeServerTlsContextImpl(BridgeServerConfig serverConfig, BridgeServerTrustManagerFactory trustManagerFactory) {
      this.useTls = serverConfig.isTlsServer();
      if (!this.useTls) {
         logger.info("BridgeServerTlsContext is disabled.");
         this.context = null;
         return;
      }

      try {
         KeyManagerFactory kmf = createKeyManagerFactory(serverConfig);
         SslContextBuilder serverContext = SslContextBuilder.forServer(kmf)
            .sslProvider(createSslProvider(serverConfig));

         if (serverConfig.getTlsSessionCacheSize() > 0) {
            serverContext.sessionCacheSize(serverConfig.getTlsSessionCacheSize());
         }

         if (serverConfig.getTlsSessionTimeout() > 0) {
            serverContext.sessionTimeout(serverConfig.getTlsSessionTimeout());
         }

         if (serverConfig.isTlsNeedClientAuth()) {
            serverContext.clientAuth(ClientAuth.REQUIRE);
         } else if (serverConfig.isTlsRequestClientAuth()) {
            serverContext.clientAuth(ClientAuth.OPTIONAL);
         } else {
            serverContext.clientAuth(ClientAuth.NONE);
         }

         if (serverConfig.isTlsNeedClientAuth() || serverConfig.isTlsRequestClientAuth()) {
            TrustManagerFactory tmf = trustManagerFactory.getTrustManagerFactory();
            if (tmf != null) {
               serverContext.trustManager(tmf);
            }
         }

         context = serverContext.build();
      } catch (Exception ex) {
         logger.error("Failed to initialize the server-size SSLContext", ex);
         throw new IllegalStateException("Failed to initialize the server-side SSLContext", ex);
      }
   }

   @Override
   public SslContext getContext() {
      return context;
   }

   @Override
   public boolean useTls() {
      return useTls;
   }

   private static SslProvider createSslProvider(BridgeServerConfig serverConfig) {
      switch (serverConfig.getTlsProvider()) {
      case BridgeServerConfig.TLS_PROVIDER_JDK:
      case BridgeServerConfig.TLS_PROVIDER_DEFAULT:
         logger.info("using jdk ssl provider");
         return SslProvider.JDK;

      case BridgeServerConfig.TLS_PROVIDER_OPENSSL:
         if (!OpenSsl.isAvailable()) {
            throw new RuntimeException("could not initialize openssl ssl provider", OpenSsl.unavailabilityCause());
         }

         logger.info("using openssl ssl provider");
         return SslProvider.OPENSSL_REFCNT;

      default:
         throw new RuntimeException("unknown ssl provider: " + serverConfig.getTlsProvider());
      }
   }

   private static KeyManagerFactory createKeyManagerFactory(BridgeServerConfig serverConfig)
      throws IOException, KeyStoreException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException {
      String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
      if (algorithm == null) {
         algorithm = "SunX509";
      }

      KeyStore ks = KeyStoreLoader.loadKeyStore(
         serverConfig.getTlsServerKeystoreFilepath(),
         serverConfig.getTlsServerKeystorePassword()
      );

      KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
      kmf.init(ks, serverConfig.getTlsServerKeyPassword().toCharArray());

      return kmf;
   }
}

