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
package com.iris.platform.metrics;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.util.ThreadPoolBuilder;

@Singleton
public final class KairosDB {
   private static final Logger log = LoggerFactory.getLogger(KairosDB.class);
   private static final IrisMetricSet METRICS = IrisMetrics.metrics("kairos.post");

   private final Gson gson;
   private final MetricsServerConfig config;
   private final CloseableHttpClient client;
   private final URI uri;
   
   private final ExecutorService executor;
   
   @Inject
   private KairosDB(Gson gson, MetricsServerConfig config) {
      this.gson = gson;
      this.config = config;
      
      this.executor = new ThreadPoolBuilder()
              .withMaxPoolSize(config.getKairosPostThreadsMax())
              .withKeepAliveMs(1000)
              .withBlockingBacklog()
              .withNameFormat("kairos-producer-%d")
              .withMetrics("metrics-server.kairos-producer")
              .build();

      this.uri = URI.create(this.config.getUrl() + "/api/v1/datapoints");
      log.info("posting metrics to kairos at: {}", this.uri);

      try {
         SSLContext ctx = new SSLContextBuilder()
            .useProtocol("TLS")
            .build();

         Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", new PlainConnectionSocketFactory())
            .register("https", new SSLConnectionSocketFactory(ctx,SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER))
            .build();

         PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager(registry);
         this.client = HttpClientBuilder.create()
            .setConnectionManager(conman)
            .setMaxConnTotal(config.getKairosPostThreadsMax()) // connections == threads
            .setMaxConnPerRoute(config.getKairosPostThreadsMax()) // connections == threads
            .evictIdleConnections(60L, TimeUnit.SECONDS)
            .evictExpiredConnections()
            .build();
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }
   
   @PreDestroy
   public void shutdown() {
      this.executor.shutdownNow();
   }

   public void post(JsonArray metrics) {    
	   executor.submit(() -> {
		   long startTime = System.nanoTime();
		   HttpPost post = new HttpPost(uri);
		   StringEntity entity = new StringEntity(gson.toJson(metrics), StandardCharsets.UTF_8);
		   post.setEntity(entity);

		   try (CloseableHttpResponse rsp = this.client.execute(post)) {

			   int sc = rsp.getStatusLine().getStatusCode();		  
			   if (sc == 204) {
				   METRICS.counter("success").inc();
				   METRICS.timer("post." + sc + ".success").update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			   } else {
				   METRICS.counter("failure." + sc).inc();
				   METRICS.timer("post." + sc + ".fail").update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                   log.warn("could not post metrics to kairosdb: {}", gson.toJson(metrics));
			   }

               log.debug("posted metrics to kairosdb: response status code is {}", sc);

			   EntityUtils.consumeQuietly(rsp.getEntity());
		   } catch (IOException ex) {
			   METRICS.counter("failure").inc();
			   METRICS.timer("post.exception.fail").update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			   log.warn("could not post metrics to kairosdb: {}", ex.getMessage(), ex);
		   } 
	   });
   }
}

