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
package com.iris.voice.google.homegraph;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.google.Predicates;
import com.iris.io.json.JSON;
import com.iris.messages.model.Model;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.VoiceUtil;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.google.GoogleConfig;
import com.iris.voice.google.GoogleMetrics;
import com.iris.voice.google.GoogleWhitelist;
import com.iris.voice.google.homegraph.ReportStateBuilder.ReportStateRequest;

import io.netty.util.HashedWheelTimer;

@Singleton
public class HomeGraphAPI {

   private static final Logger logger = LoggerFactory.getLogger(HomeGraphAPI.class);

   private static final String REQUEST_SYNC = ":requestSync";
   private static final String PROP_USERAGENT = "agent_user_id";
   public static final String EXECUTOR_NAME = "VoiceService#googleReportStateExecutor";


   private final GoogleConfig config;
   private final PoolingHttpClientConnectionManager pool;
   private final RequestConfig requestConfig;
   private final GoogleRpcContext gRpcContext;
   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private final HashedWheelTimer executor;


   @Inject
   public HomeGraphAPI(GoogleConfig config,
         GoogleRpcContext rpcContext,
         ProductCatalogManager prodCat,
         GoogleWhitelist whitelist,
         @Named(EXECUTOR_NAME) HashedWheelTimer executor
   ) {
      this.config = config;
      requestConfig = RequestConfig.custom()
         .setConnectionRequestTimeout(config.getConnectionRequestTimeoutMs())
         .setConnectTimeout(config.getConnectionTimeoutMs())
         .setSocketTimeout(config.getSocketTimeoutMs())
         .build();

      pool = new PoolingHttpClientConnectionManager(config.getTimeToLiveMs(), TimeUnit.MILLISECONDS);
      pool.setDefaultMaxPerRoute(config.getRouteMaxConnections());
      pool.setMaxTotal(config.getMaxConnections());
      pool.setValidateAfterInactivity(config.getValidateAfterInactivityMs());
      this.gRpcContext = rpcContext;
      this.prodCat = prodCat;
      this.whitelist = whitelist;
      this.executor = executor;
   }

   @PreDestroy
   public void destroy() {
      pool.shutdown();
   }

   public void requestSync(String accessToken) {
      if(accessToken == null) {
         return;
      }

      try(Timer.Context ctxt = GoogleMetrics.startRequestSyncTimer()) {
         Map<String, String> body = ImmutableMap.of(PROP_USERAGENT, accessToken);
         String bodyStr = JSON.toJson(body);
         HttpPost post = createPost(createUrl(REQUEST_SYNC), ContentType.APPLICATION_JSON, new StringEntity(bodyStr, StandardCharsets.UTF_8));
         try(CloseableHttpClient client = httpClient()) {
            HttpEntity entity = null;
            try(CloseableHttpResponse response = client.execute(post)) {
               if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                  logger.warn("failed to issue requestSync for {}: {}", accessToken, response.getStatusLine().getStatusCode());
                  entity = response.getEntity();
                  GoogleMetrics.incRequestSyncFailures();
               }
            } finally {
               consumeQuietly(entity);
            }
         } catch(Exception e) {
            logger.warn("failed to issue requestSync for {}", accessToken, e);
            GoogleMetrics.incRequestSyncFailures();
         }
      }
   }

   /**
    * The Report State request must follow a SYNC response.  However, the Report State request cannot go out until
    * the SYNC response has been received by google.  The Report State will be delayed by google.homegraph.resport.state.delay.sec
    */
   public void sendDelayedReportState(VoiceContext context) {
      if (!this.config.isReportStateEnabled()) {
         logger.debug("No delayed Report State scheduled to Google for {}. Reporting State is not enabled", context.getPlaceId());
         return;
      }
      
      this.executor.newTimeout(timeout -> {
         sendReportState(context);
      }, this.config.getReportStateDelaySec(), TimeUnit.SECONDS);
   }

   /**
    * Post a Report State to Google without delay.  This is used when there is an attribute change that doesn't require a SYNC.
    */
   public void sendReportState(VoiceContext context) {
      if (!this.config.isReportStateEnabled()) {
         logger.debug("Ignoring Report State to Google for {}. Reporting State is not enabled", context.getPlaceId());
         return;
      }
      
      boolean whitelisted = this.whitelist.isWhitelisted(context.getPlaceId());
      List<Model> devices = context.streamSupportedModels(model -> Predicates.isSupportedModel(model, whitelisted, VoiceUtil.getProduct(prodCat, model))).collect(Collectors.toList());

      sendReportState(context.getPlaceId(), devices, context.isHubOffline());
   }

   protected void sendReportState(UUID placeId, List<Model> devices, boolean hubOffline) {
      ReportStateRequest request = null;
      try (Timer.Context ctxt = GoogleMetrics.startReportStateTimer()) {
         // @formatter:off
         request = this.gRpcContext.getRequestBuilder()
               .withPlaceId(placeId)
               .withHubOffline(hubOffline)
               .withPayloadDevices(devices)
               .build();
         // @formatter:on

         request.send(); // throws on failure to post

         // Prod is set to debug level
         logger.trace("Successfully posted ReportState for {}: Request: {}", placeId, request);
         GoogleMetrics.incReportStateSuccesses();

      }
      catch (Exception e) {
         // sometimes we send google more information than they need.  It's difficult to know which device didn't get communicated with a SYNC call.
         if (e.getMessage().contains("Requested entity was not found")) {
            logger.trace("Sent data to Google for an unknown device in place [{}]: Request: {}", placeId, request, e);
         }
         else {
            logger.warn("Failed to post ReportState for {}: Request: {}", placeId, request, e);
         }
         
         GoogleMetrics.incReportStateFailures();
      }
   }

   private String createUrl(String method) {
      try {
         return new URIBuilder(config.getHomeGraphApiUrl() + method)
            .addParameter("key", config.getHomeGraphApiKey())
            .build()
            .toString();
      } catch(URISyntaxException use) {
         throw new RuntimeException(use);
      }
   }

   private static void consumeQuietly(HttpEntity entity) {
      if(entity != null) {
         EntityUtils.consumeQuietly(entity);
      }
   }

   private HttpPost createPost(String url, ContentType contentType, HttpEntity body) {
      HttpPost post = new HttpPost(url);
      post.setConfig(requestConfig);
      post.setHeader(HttpHeaders.CONTENT_TYPE, contentType.getMimeType());
      post.setEntity(body);
      return post;
   }

   private CloseableHttpClient httpClient() {
      return HttpClients.custom()
         .setConnectionManager(pool)
         .setConnectionManagerShared(true)
         .build();
   }



}

