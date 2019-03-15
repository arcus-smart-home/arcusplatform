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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyStoreLoader {
   private final static Logger logger = LoggerFactory.getLogger(KeyStoreLoader.class);
   
   public static KeyStore loadKeyStore(String filePath, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(getKeyStoreInputStream(filePath), password.toCharArray());
      return ks;
   }
   
   private static InputStream getKeyStoreInputStream(String keystoreFilePath) throws IOException {
      // first try to load the keystore file relative to the main server class or jar
      InputStream keystoreInputStream = null;
      try {
         keystoreInputStream = KeyStoreLoader.class.getResourceAsStream("/" + keystoreFilePath);
         if (keystoreInputStream == null) throw new IllegalStateException("Keystore not Found");
         return keystoreInputStream;
      } catch (Exception e) {
         logger.debug("Did not find keystore file " + keystoreFilePath + " on classpath");
      }
      
      // if that failed, try as absolute path
      try {
         keystoreInputStream = new FileInputStream(keystoreFilePath);
         return keystoreInputStream;
      } catch (Exception e) {
         logger.debug("Did not find keystore file " + keystoreFilePath + " as fully-qualified path on filesystem");
      }
      
      throw new IOException("Could not find keystore file at " + keystoreFilePath);
      
   }
}

