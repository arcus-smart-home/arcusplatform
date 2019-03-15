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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.ssl.SslKeyStore;
import com.iris.agent.util.AbstractProgress;
import com.iris.agent.util.Progress;
import com.iris.agent.util.ProgressMonitor;
import com.iris.agent.util.Progresses;

public final class HttpService {
   private static final Object START_LOCK = new Object();

   @Nullable
   private static CloseableHttpClient client;

   private HttpService() {
   }

   public static HttpClient client() {
      return get();
   }


   @SuppressWarnings("deprecation")
   public static void start() {
      synchronized (START_LOCK) {
         if (client != null) {
            throw new IllegalStateException("http service already started");
         }

         try {
            SSLContext ctx = new SSLContextBuilder()
               .loadKeyMaterial(SslKeyStore.getKeyStore(), SslKeyStore.getKeyStorePassword())
               .useProtocol("TLS")
               .build();

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
               .register("http", new PlainConnectionSocketFactory())
               .register("https", new SSLConnectionSocketFactory(ctx,SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER))
               .build();

            SocketConfig sockConfig = SocketConfig.copy(SocketConfig.DEFAULT)
               .setSoLinger(0)
               .build();

            RequestConfig reqConfig = RequestConfig.copy(RequestConfig.DEFAULT)
               .setConnectTimeout((int)TimeUnit.MILLISECONDS.convert(90,TimeUnit.SECONDS))
               .setSocketTimeout((int)TimeUnit.MILLISECONDS.convert(90,TimeUnit.SECONDS))
               .build();

            PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager(registry);
            client = HttpClientBuilder.create()
               .setDefaultRequestConfig(reqConfig)
               .setDefaultSocketConfig(sockConfig)
               .setConnectionManager(conman)
               .evictIdleConnections(60L, TimeUnit.SECONDS)
               .evictExpiredConnections()
               .build();
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }

         AsyncHttpService.start();
         HttpServer.start();
      }
   }

   public static void shutdown() {
      synchronized (START_LOCK) {
         HttpServer.shutdown();
         AsyncHttpService.shutdown();

         if (client != null) {
            IOUtils.closeQuietly(client);
         }

         client = null;
      }
   }

   private static CloseableHttpClient get() {
      CloseableHttpClient result = client;
      if (result == null) {
         throw new IllegalStateException("http service not started");
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // HTTP request utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static CloseableHttpResponse execute(HttpUriRequest req) throws IOException {
      return get().execute(req);
   }

   public static CloseableHttpResponse execute(HttpUriRequest req, @Nullable Credentials auth) throws IOException {
      if (auth != null) {
         URI uri = req.getURI();
         AuthScope scope = new AuthScope(uri.getHost(), uri.getPort());

         CredentialsProvider provider = new BasicCredentialsProvider();
         provider.setCredentials(scope, auth);

         HttpClientContext context = HttpClientContext.create();
         context.setCredentialsProvider(provider);

         return get().execute(req, context);
      }

      return execute(req);
   }

   public static CloseableHttpResponse get(String uri) throws IOException {
      return get(URI.create(uri));
   }

   public static CloseableHttpResponse get(URI uri) throws IOException {
      HttpGet get = new HttpGet(uri);
      get.setHeader("Connection", "close");
      return execute(get);
   }

   public static CloseableHttpResponse get(String uri, Credentials auth) throws IOException {
      return get(URI.create(uri), auth);
   }

   public static CloseableHttpResponse get(URI uri, Credentials auth) throws IOException {
      HttpGet get = new HttpGet(uri);
      get.setHeader("Connection", "close");
      return execute(get, auth);
   }

   public static Credentials basic(String username, String password) {
      return new UsernamePasswordCredentials(username, password);
   }

   /////////////////////////////////////////////////////////////////////////////
   // HTTP downloading utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static File download(String uri) throws IOException {
      return download(URI.create(uri));
   }

   public static File download(URI uri) throws IOException {
      return download(uri, NULL_MONITOR);
   }

   public static File download(String uri, String prefix, String suffix) throws IOException {
      return download(URI.create(uri), prefix, suffix);
   }

   public static File download(URI uri, String prefix, String suffix) throws IOException {
      return download(uri, prefix, suffix, NULL_MONITOR);
   }

   public static File download(String uri, File dst) throws IOException {
      return download(URI.create(uri), dst);
   }

   public static File download(URI uri, File dst) throws IOException {
      return download(uri, dst, NULL_MONITOR);
   }

   public static File download(String uri, ProgressMonitor<Long> monitor) throws IOException {
      return download(URI.create(uri), monitor);
   }

   public static File download(URI uri, ProgressMonitor<Long> monitor) throws IOException {
      return download(uri, "download", ".tmp", monitor);
   }

   public static File download(String uri, String prefix, String suffix, ProgressMonitor<Long> monitor) throws IOException {
      return download(URI.create(uri), prefix, suffix, monitor);
   }

   public static File download(URI uri, String prefix, String suffix, ProgressMonitor<Long> monitor) throws IOException {
      File dst = File.createTempFile(prefix, suffix);
      return download(uri, dst, monitor);
   }

   public static File download(URI uri, String prefix, String suffix, File dir, ProgressMonitor<Long> monitor) throws IOException {
      File dst = File.createTempFile(prefix, suffix, dir);
      try {
         return download(uri, dst, monitor);
      } catch (Exception ex) {
         dst.delete();
         throw ex;
      }
   }

   public static File download(String uri, File dst, ProgressMonitor<Long> monitor) throws IOException {
      return download(URI.create(uri), dst, monitor);
   }

   public static File download(URI uri, File dst, ProgressMonitor<Long> monitor) throws IOException {
      CloseableHttpResponse rsp = get(uri);
      try {
         if (rsp.getStatusLine().getStatusCode() != 200) {
            throw new IOException("http request failed: " + rsp.getStatusLine().getStatusCode());
         }

         HttpEntity entity = rsp.getEntity();
         long length = entity.getContentLength();

         try (InputStream is = entity.getContent();
              OutputStream os = new BufferedOutputStream(new FileOutputStream(dst))) {
            copy(is, os, length, monitor);
         }
         EntityUtils.consume(entity);
      } finally {
         rsp.close();
      }
      return dst;
   }

   public static File download(String uri, Credentials auth) throws IOException {
      return download(uri, auth, NULL_MONITOR);
   }

   public static File download(URI uri, Credentials auth) throws IOException {
      return download(uri, auth, NULL_MONITOR);
   }

   public static File download(String uri, Credentials auth, String prefix, String suffix) throws IOException {
      return download(uri, auth, prefix, suffix, NULL_MONITOR);
   }

   public static File download(URI uri, Credentials auth, String prefix, String suffix) throws IOException {
      return download(uri, auth, prefix, suffix, NULL_MONITOR);
   }

   public static File download(String uri, Credentials auth, File dst) throws IOException {
      return download(uri, auth, dst, NULL_MONITOR);
   }

   public static File download(URI uri, Credentials auth, File dst) throws IOException {
      return download(uri, auth, dst, NULL_MONITOR);
   }

   public static File download(String uri, Credentials auth, ProgressMonitor<Long> monitor) throws IOException {
      return download(URI.create(uri), auth, monitor);
   }

   public static File download(URI uri, Credentials auth, ProgressMonitor<Long> monitor) throws IOException {
      return download(uri, auth, "download", ".tmp", monitor);
   }

   public static File download(String uri, Credentials auth, String prefix, String suffix, ProgressMonitor<Long> monitor) throws IOException {
      return download(URI.create(uri), auth, prefix, suffix, monitor);
   }

   public static File download(URI uri, Credentials auth, String prefix, String suffix, ProgressMonitor<Long> monitor) throws IOException {
      File dst = File.createTempFile(prefix, suffix);
      return download(uri, auth, dst, monitor);
   }

   public static File download(String uri, Credentials auth, File dst, ProgressMonitor<Long> monitor) throws IOException {
      return download(URI.create(uri), dst, monitor);
   }

   public static File download(URI uri, Credentials auth, File dst, ProgressMonitor<Long> monitor) throws IOException {
      CloseableHttpResponse rsp = get(uri, auth);
      try {
         if (rsp.getStatusLine().getStatusCode() != 200) {
            throw new IOException("http request failed: " + rsp.getStatusLine().getStatusCode());
         }

         HttpEntity entity = rsp.getEntity();
         long length = entity.getContentLength();

         try (InputStream is = entity.getContent();
              OutputStream os = new BufferedOutputStream(new FileOutputStream(dst))) {
            copy(is, os, length, monitor);
         }
         EntityUtils.consume(entity);
      } finally {
         rsp.close();
      }
      return dst;
   }

   /////////////////////////////////////////////////////////////////////////////
   // HTTP uploading utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static void upload(String uri, File src, String description) throws IOException {
      upload(URI.create(uri), src, description);
   }

   public static void upload(String uri, File src) throws IOException {
      upload(URI.create(uri), src, "upload file");
   }

   public static void upload(URI uri, File dst) throws IOException {
      upload(uri, dst, "upload file");
   }

   public static void upload(URI uri, File src, String description) throws IOException {
      HttpPost httppost = new HttpPost(uri);
      FileBody data = new FileBody(src);
      StringBody text = new StringBody(description, ContentType.TEXT_PLAIN);

      // Build up multi-part form
      HttpEntity reqEntity = MultipartEntityBuilder.create()
              .addPart("upload", data)
              .addPart("comment", text)
              .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
              .build();
      httppost.setEntity(reqEntity);

      // Execute post and get response
      CloseableHttpClient client = get();
      CloseableHttpResponse response = client.execute(httppost);
      try {
          HttpEntity resEntity = response.getEntity();
          EntityUtils.consume(resEntity);
      } finally {
          response.close();
      }
   }

   public static void upload(String uri, Credentials auth, File src, String description) throws IOException {
      upload(URI.create(uri), auth, src, description);
   }

   public static void upload(String uri, Credentials auth, File src) throws IOException {
      upload(URI.create(uri), auth, src, "upload file");
   }

   public static void upload(URI uri, Credentials auth, File dst) throws IOException {
      upload(uri, auth, dst, "upload file");
   }

   public static void upload(URI uri, Credentials auth, File src, String description) throws IOException {
      String cred = auth.getUserPrincipal().getName()+":"+auth.getPassword();
      String encoding = Base64.encodeBase64String(cred.getBytes());
      HttpPost httppost = new HttpPost(uri);
      // Add in authentication
      httppost.setHeader("Authorization", "Basic " + encoding);
      FileBody data = new FileBody(src);
      StringBody text = new StringBody(description, ContentType.TEXT_PLAIN);

      // Build up multi-part form
      HttpEntity reqEntity = MultipartEntityBuilder.create()
              .addPart("upload", data)
              .addPart("comment", text)
              .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
              .build();
      httppost.setEntity(reqEntity);

      // Execute post and get response
      CloseableHttpClient client = get();
      CloseableHttpResponse response = client.execute(httppost);
      try {
          HttpEntity resEntity = response.getEntity();
          EntityUtils.consume(resEntity);
      } finally {
          response.close();
      }
   }


   public static byte[] content(String uri) throws IOException {
      return content(uri, NULL_MONITOR);
   }

   public static byte[] content(URI uri) throws IOException {
      return content(uri, NULL_MONITOR);
   }

   public static byte[] content(String uri, ProgressMonitor<Long> monitor) throws IOException {
      return content(URI.create(uri), monitor);
   }

   public static byte[] content(URI uri, ProgressMonitor<Long> monitor) throws IOException {
      try (CloseableHttpResponse rsp = get(uri)) {
         if (rsp.getStatusLine().getStatusCode() != 200) {
            throw new IOException("http request failed: " + rsp.getStatusLine().getStatusCode());
         }

         HttpEntity entity = rsp.getEntity();

         long length = entity.getContentLength();
         int clength = (int)length;
         if (clength <= 0 || length >= Integer.MAX_VALUE) clength = 256;

         try (InputStream is = entity.getContent();
              ByteArrayOutputStream os = new ByteArrayOutputStream(clength)) {
            copy(is, os, length, monitor);
            EntityUtils.consume(entity);
            return os.toByteArray();
         }
      }
   }

   public static byte[] content(String uri, Credentials auth) throws IOException {
      return content(uri, auth, NULL_MONITOR);
   }

   public static byte[] content(URI uri, Credentials auth) throws IOException {
      return content(uri, auth, NULL_MONITOR);
   }

   public static byte[] content(String uri, Credentials auth, ProgressMonitor<Long> monitor) throws IOException {
      return content(URI.create(uri), auth, monitor);
   }

   public static byte[] content(URI uri, Credentials auth, ProgressMonitor<Long> monitor) throws IOException {
      CloseableHttpResponse rsp = get(uri, auth);
      try {
         if (rsp.getStatusLine().getStatusCode() != 200) {
            throw new IOException("http request failed: " + rsp.getStatusLine().getStatusCode());
         }

         HttpEntity entity = rsp.getEntity();

         long length = entity.getContentLength();
         int clength = (int)length;
         if (clength <= 0 || length >= Integer.MAX_VALUE) clength = 256;

         try (InputStream is = entity.getContent();
              ByteArrayOutputStream os = new ByteArrayOutputStream(clength)) {
            copy(is, os, length, monitor);
            EntityUtils.consume(entity);
            return os.toByteArray();
         }
      } finally {
         rsp.close();
      }
   }

   public static String contentAsString(String uri) throws IOException {
      return contentAsString(uri, NULL_MONITOR);
   }

   public static String contentAsString(URI uri) throws IOException {
      return contentAsString(uri, NULL_MONITOR);
   }

   public static String contentAsString(String uri, ProgressMonitor<Long> monitor) throws IOException {
      return contentAsString(URI.create(uri), monitor);
   }

   public static String contentAsString(URI uri, ProgressMonitor<Long> monitor) throws IOException {
      CloseableHttpResponse rsp = get(uri);
      try {
         if (rsp.getStatusLine().getStatusCode() != 200) {
            throw new IOException("http request failed: " + rsp.getStatusLine().getStatusCode());
         }

         HttpEntity entity = rsp.getEntity();
         ContentType ct = ContentType.get(entity);

         Charset cs = ct.getCharset();
         if (cs == null) cs = StandardCharsets.UTF_8;

         long length = entity.getContentLength();
         int clength = (int)length;
         if (clength <= 0 || length >= Integer.MAX_VALUE) clength = 256;

         try (InputStream is = entity.getContent();
              ByteArrayOutputStream os = new ByteArrayOutputStream(clength)) {
            copy(is, os, length, monitor);
            EntityUtils.consume(entity);
            return new String(os.toByteArray(), cs);
         }
      } finally {
         rsp.close();
      }
   }

   public static String contentAsString(String uri, Credentials auth) throws IOException {
      return contentAsString(uri, auth, NULL_MONITOR);
   }

   public static String contentAsString(URI uri, Credentials auth) throws IOException {
      return contentAsString(uri, auth, NULL_MONITOR);
   }

   public static String contentAsString(String uri, Credentials auth, ProgressMonitor<Long> monitor) throws IOException {
      return contentAsString(URI.create(uri), auth, monitor);
   }

   public static String contentAsString(URI uri, Credentials auth, ProgressMonitor<Long> monitor) throws IOException {
      CloseableHttpResponse rsp = get(uri,auth);
      try {
         if (rsp.getStatusLine().getStatusCode() != 200) {
            throw new IOException("http request failed: " + rsp.getStatusLine().getStatusCode());
         }

         HttpEntity entity = rsp.getEntity();
         ContentType ct = ContentType.get(entity);

         Charset cs = ct.getCharset();
         if (cs == null) cs = StandardCharsets.UTF_8;

         long length = entity.getContentLength();
         int clength = (int)length;
         if (clength <= 0 || length >= Integer.MAX_VALUE) clength = 256;

         try (InputStream is = entity.getContent();
              ByteArrayOutputStream os = new ByteArrayOutputStream(clength)) {
            copy(is, os, length, monitor);
            EntityUtils.consume(entity);
            return new String(os.toByteArray(), cs);
         }
      } finally {
         rsp.close();
      }
   }

   public static void copy(InputStream is, OutputStream os, long length, ProgressMonitor<Long> monitor) throws IOException {
      AbstractProgress<Long> progress = Progresses.create(length < 0 ? 1L : length);
      progress.addProgressMonitor(monitor);

      byte[] buffer = new byte[8192];
      while (true) {
         int read = is.read(buffer);
         if (read < 0) break;

         if (read > 0) {
            os.write(buffer, 0, read);
            if (length >= 0) {
               progress.fireProgressUpdated((long)read);
            }
         }
      }

      progress.fireProgressComplete();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility classes
   /////////////////////////////////////////////////////////////////////////////

   private static final ProgressMonitor<Long> NULL_MONITOR = new ProgressMonitor<Long>() {
      @Override
      public void onProgressChange(Progress<? extends Long> progress, Long update) {
      }

      @Override
      public void onProgressComplete(Progress<? extends Long> progress) {
      }

      @Override
      public void onProgressFailed(Progress<? extends Long> progress) {
      }
   };
}

