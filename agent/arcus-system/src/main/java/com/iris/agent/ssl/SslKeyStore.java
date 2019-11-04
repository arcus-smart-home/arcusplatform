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
package com.iris.agent.ssl;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.IrisHal;

import io.netty.handler.ssl.util.FingerprintTrustManagerFactory;

public final class SslKeyStore {
   private static final Logger log = LoggerFactory.getLogger(SslKeyStore.class);
   private static final String PKCS12_HEADER = "-----BEGIN RSA PRIVATE KEY-----";

   private static final KeyStore keystore;
   private static final KeyManagerFactory keyfactory;

   private static final KeyStore truststore;
   private static final TrustManagerFactory trustfactory;
   private static final TrustManagerFactory fingerPrintTrustFactory;

   private static X509Certificate[] trustedCertificates;

   static {
      KeyStore ks = null;
      KeyStore ts = null;
      KeyManagerFactory kmf = null;
      TrustManagerFactory tmf = null;

      String algorithm = Security.getProperty("ssl.TrustManagerFactory.algorithm");
      if (algorithm == null) {
         algorithm = "SunX509";
      }

      try {
         ks = KeyStore.getInstance("JKS");
         ks.load(null, null);

         PrivateKey key = readHubPrivateKey();
         Collection<? extends Certificate> certs = readHubCertificate();
         if (key != null && certs != null) {
            ks.setKeyEntry("key", readHubPrivateKey(), new char[0], certs.toArray(new Certificate[certs.size()]));
         }

         kmf = KeyManagerFactory.getInstance(algorithm);
         kmf.init(ks, new char[0]);
      } catch (Exception ex) {
         log.error("error settings up hub keystore: {}", ex.getMessage(), ex);
         throw new RuntimeException(ex);
      }

      try (InputStream is = new BufferedInputStream(ClassLoader.getSystemResourceAsStream("truststore.jks"))) {
         char[] pass = new char[] {
            '8','E','F','J','h','x','m','7',
            'a','R','s','2','h','m','m','K',
            'w','V','u','M','9','R','P','S',
            'w','h','N','C','t','M','p','C'
         };

         ts = KeyStore.getInstance("JKS");
         ts.load(is, pass);

         Arrays.fill(pass, 0, pass.length, '0');

         tmf = TrustManagerFactory.getInstance(algorithm);
         tmf.init(ts);

         ArrayList<X509Certificate> trustedCerts = new ArrayList<>();
         Enumeration<String> aliases = ts.aliases();
         while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            try {
               trustedCerts.add((X509Certificate)ts.getCertificate(alias));
            } catch (Throwable th) {
               log.warn("could not load certificate {}:", alias, th);
            }
         }

         trustedCertificates = trustedCerts.toArray(new X509Certificate[trustedCerts.size()]);
      } catch (Exception ex) {
         log.error("error settings up hub trust store: {}", ex.getMessage(), ex);
         throw new RuntimeException(ex);
      }

      keystore = ks;
      keyfactory = kmf;

      truststore = ts;
      trustfactory = tmf;

      List<byte[]> fps = new ArrayList<>();
      for (X509Certificate cert : trustedCertificates) {
         fps.add(getFingerPrint(cert));
      }

      fingerPrintTrustFactory = new FingerprintTrustManagerFactory(fps.toArray(new byte[0][]));
   }

   private SslKeyStore() {
   }

   public static KeyStore getKeyStore() {
      return keystore;
   }

   public static char[] getKeyStorePassword() {
      return new char[0];
   }

   public static KeyManagerFactory getKeyManagerFactory() {
      return keyfactory;
   }

   public static KeyStore getTrustStore() {
      return truststore;
   }

   public static TrustManagerFactory getTrustManagerFactory() {
      return trustfactory;
   }

   public static TrustManagerFactory getFingerPrintTrustManagerFactory() {
      return fingerPrintTrustFactory;
   }

   public static X509Certificate[] getTrustedBridgeCertificates() {
      return trustedCertificates;
   }

   public static byte[] getFingerPrint(X509Certificate cert) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA1");
         md.update(cert.getEncoded());
         
         return md.digest();
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   @Nullable
   public static X509Certificate[] readHubCertificateAsArray() {
      Collection<? extends X509Certificate> cert = readHubCertificate();
      if (cert == null) return null;

      return cert.toArray(new X509Certificate[cert.size()]);
   }

   @Nullable
   private static Collection<? extends X509Certificate> readHubCertificate() {
      try {
         PemContent cert = readPemContent(IrisHal.getHubCertificateFile());
         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         return (Collection<? extends X509Certificate>)cf.generateCertificates(new ByteArrayInputStream(cert.content));
      } catch (Exception ex) {
         log.debug("could not read hub certificate: {}", ex.getMessage(), ex);
         return null;
      }
   }

   @Nullable
   public static PrivateKey readHubPrivateKey() {
      try {
         PemContent pem = readPemContent(IrisHal.getHubKeyFile());
         switch (pem.format) {
         case PKCS1:
            return fromPkcs1(pem.content);

         case PKCS8:
         default:
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pem.content));
         }
      } catch (Exception ex) {
         log.debug("could not read hub private key: {}", ex.getMessage(), ex);
         return null;
      }
   }


   private static PemContent readPemContent(File file) throws IOException {
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
         List<String> lines = IOUtils.readLines(is, StandardCharsets.US_ASCII);
         boolean pkcs1 = !lines.isEmpty() && PKCS12_HEADER.equals(lines.get(0));

         List<String> contentLines = lines.subList(1, lines.size() - 1);
         String content = StringUtils.join(contentLines, "");
         return new PemContent(pkcs1 ? PemContent.Format.PKCS1 : PemContent.Format.PKCS8, content);
      }
   }

   private static PrivateKey fromPkcs1(byte[] content) throws Exception {
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
      converter.setProvider("BC");
      PEMParser pemParser = new PEMParser(new StringReader(new String(content)));

      return converter.getKeyPair((PEMKeyPair) pemParser.readObject()).getPrivate();
   }

   private static final class PemContent {
      static enum Format { PKCS1, PKCS8 }

      Format format;
      byte[] content;

      PemContent(Format format, String content) {
         this.format = format;
         this.content = Base64.decodeBase64(content);
      }
   }
}

