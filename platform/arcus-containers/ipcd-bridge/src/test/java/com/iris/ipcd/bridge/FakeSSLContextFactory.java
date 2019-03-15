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
package com.iris.ipcd.bridge;

import javax.net.ssl.SSLContext;

public class FakeSSLContextFactory {
   private static final String PROTOCOL = "TLS";
   private static final SSLContext CONTEXT;

   static {
       SSLContext clientContext;
       try {
          clientContext = SSLContext.getInstance(PROTOCOL);
          clientContext.init(null, FakeSSLTrustManagerFactory.getTrustManagers(), null);
          
          /*
          String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
          if (algorithm == null) {
             algorithm = "SunX509";
          }
          
          KeyStore ks = KeyStoreLoader.loadKeyStore("dskeystore.jks", "tempest");
          KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
          kmf.init(ks, "tempest".toCharArray());
          clientContext = SSLContext.getInstance(PROTOCOL);
          clientContext.init(kmf.getKeyManagers(), IpcdClientSslTrustManagerFactory.getTrustManagers(), null);
          */
          
       } catch (Exception e) {
           throw new Error(
                   "Failed to initialize the client-side SSLContext", e);
       }

       CONTEXT = clientContext;
   }

   public static SSLContext getContext() {
       return CONTEXT;
   }

   private FakeSSLContextFactory() {
       // Unused
   }
}

