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
package com.iris.hubcom.server.ssl;

import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.ssl.BridgeServerTrustManagerFactory;
import com.iris.bridge.server.ssl.KeyStoreLoader;
import com.iris.core.dao.HubBlacklistDAO;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

@Singleton
public class HubTrustManagerFactoryImpl implements BridgeServerTrustManagerFactory {
   private final static Logger logger = LoggerFactory.getLogger(HubTrustManagerFactoryImpl.class);
   private final TrustManagerFactory trustManagerFactory;
   private final HubBlacklistDAO hubBlacklistDAO;
   private final boolean mutualAuthRequired;

   @Inject
   public HubTrustManagerFactoryImpl(BridgeServerConfig serverConfig, TrustConfig trustConfig, HubBlacklistDAO hubBlacklistDAO) throws Exception {
      String fp = trustConfig.getTlsServerTruststoreFilepath();
      String ps = trustConfig.getTlsServerTruststorePassword();
      if (serverConfig.isTlsServer() && fp != null && ps != null) {

         KeyStore truststore = KeyStoreLoader.loadKeyStore(fp, ps);
         String algorithm = Security.getProperty("ssl.TrustManagerFactory.algorithm");
         if (algorithm == null) {
            algorithm = "SunX509";
         }
         trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
         trustManagerFactory.init(truststore);
      }
      else {
         logger.info("skipping trust manager factory initializaion, not configured");
         trustManagerFactory = null;
      }

      this.hubBlacklistDAO = hubBlacklistDAO;
      this.mutualAuthRequired = serverConfig.isTlsNeedClientAuth();
   }

   @Override
   public TrustManagerFactory getTrustManagerFactory() {
      X509TrustManager x509 = null;
      for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
         if (trustManager instanceof X509TrustManager) {
            x509 = (X509TrustManager)trustManager;
            break;
         }
      }

      if (x509 == null) {
         throw new RuntimeException("No x509 trust managers were returned by the JDK.");
      }

      return new InternalTrustManagerFactory(x509, hubBlacklistDAO, mutualAuthRequired);
   }

   private static final class InternalTrustManagerFactory extends SimpleTrustManagerFactory {
      private final X509TrustManager x509;
      private final HubBlacklistDAO hubBlacklistDAO;
      private final boolean mutualAuthRequired;

      public InternalTrustManagerFactory(X509TrustManager x509, HubBlacklistDAO hubBlacklistDAO, boolean mutualAuthRequired) {
         this.x509 = x509;
         this.hubBlacklistDAO = hubBlacklistDAO;
         this.mutualAuthRequired = mutualAuthRequired;
      }

      @Override
      protected TrustManager[] engineGetTrustManagers() {
         return new TrustManager[] { new BlackListTrustManager(x509, hubBlacklistDAO, mutualAuthRequired) };
      }

      @Override
      protected void engineInit(KeyStore keyStore) throws Exception {
      }

      @Override
      protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
      }
   }
}

