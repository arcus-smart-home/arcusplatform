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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class SimpleTrustManager implements X509TrustManager {
   private final static Logger logger = LoggerFactory.getLogger(SimpleTrustManager.class);
   private final boolean mutualAuthRequired;
   private final X509TrustManager jdkTrustManager;
   
   SimpleTrustManager(X509TrustManager jdkTrustManager, boolean mutualAuthRequired) {
      this.jdkTrustManager = jdkTrustManager;
      this.mutualAuthRequired = mutualAuthRequired;
   }
   
   @Override
   public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      
      if (chain == null || chain.length == 0) {
         if(mutualAuthRequired) {
            logger.debug("Rejecting request with no client certificates");
            throw new CertificateException("No certificate chain exists");
         }
         else {
            logger.debug("Accepting request with no client certificate -- mutual auth not required");
            return;
         }
      }
      
      try {
         jdkTrustManager.checkClientTrusted(chain, authType);
         logger.debug("Client certificate accepted [{}], serial number [{}]", chain[0].getSubjectDN(), chain[0].getSerialNumber());
      } 
      catch (CertificateException ex) {
         if (chain.length > 0 && chain[0] != null) {
            logger.error("Client certificate rejected [{}]", chain[0].getSubjectDN());
         }
         throw ex;
      }
   }

   @Override
   public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      jdkTrustManager.checkServerTrusted(chain, authType);
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return jdkTrustManager.getAcceptedIssuers();
   }
}

