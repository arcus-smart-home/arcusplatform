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
package com.iris.voice.alexa.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.serder.SerDer;
import com.iris.io.json.JSON;
import com.iris.util.TypeMarker;
import com.iris.voice.alexa.AlexaConfig;
import com.iris.voice.alexa.AlexaMetrics;
import com.iris.voice.proactive.ProactiveCreds;

@Singleton
public class AlexaHttpClient {

   private static final Logger logger = LoggerFactory.getLogger(AlexaHttpClient.class);

   private static final Header OAUTH_HEADER = new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");
   private static final Header REPORT_CONTENT_TYPE = new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
   private static final TypeMarker<Map<String, Object>> ERR_TYPE = new TypeMarker<Map<String, Object>>() {};

   private final AlexaConfig config;
   private final PoolingHttpClientConnectionManager pool;
   private final RequestConfig requestConfig;

   @Inject
   public AlexaHttpClient(AlexaConfig config) {
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
   }

   @PreDestroy
   public void destroy() {
      pool.shutdown();
   }

   public ProactiveCreds createCreds(UUID place, String code) {
      logger.debug("creating alexa proactive reporting creds for {}", place);
      try(Timer.Context ctxt = AlexaMetrics.startOAuthCreateTimer()) {
         return executeOAuth(
            ImmutableList.of(
               new BasicNameValuePair("grant_type", "authorization_code"),
               new BasicNameValuePair("code", code)
            )
         );
      }
   }

   public ProactiveCreds refreshCreds(UUID place, String refresh) {
      logger.debug("refreshing alexa proactive reporting creds for {}", place);
      try(Timer.Context ctxt = AlexaMetrics.startOAuthRefreshTimer()) {
         return executeOAuth(
            ImmutableList.of(
               new BasicNameValuePair("grant_type", "refresh_token"),
               new BasicNameValuePair("refresh_token", refresh)
            )
         );
      }
   }

   public void report(AlexaMessage msg) {
      try(Timer.Context ctxt = AlexaMetrics.startPostEventTimer()) {
         HttpPost post = createPost(config.getEventEndpoint(), REPORT_CONTENT_TYPE);
         post.setConfig(requestConfig);
         String json = SerDer.serialize(msg);
         logger.trace("proactively reporting {}", json);
         post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

         try(CloseableHttpClient client = httpClient()) {
            try(CloseableHttpResponse response = client.execute(post)) {
               if(response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
                  if(disabledSkill500(response)) {
                     throw new SkillDisabledException();
                  }
                  if(disabledSkill403(response)) {
                     throw new SkillDisabledException();
                  }
                  logger.warn("proactive reporting of {} failed with status {}", msg, response.getStatusLine());
               }
            }
         } catch(IOException e) {
            AlexaMetrics.incPostEventFailed();
            logger.warn("proactive reporting of {} failed", msg, e);
         }
      }
   }

   private boolean disabledSkill500(CloseableHttpResponse response) throws IOException {
      if(response.getStatusLine().getStatusCode() != HttpStatus.SC_INTERNAL_SERVER_ERROR) {
         return false;
      }
      HttpEntity entity = null;
      try {
         entity = response.getEntity();
         Map<String, Object> body = JSON.fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), ERR_TYPE);
         String msg = (String) body.get("message");
         boolean disabled = StringUtils.containsIgnoreCase(msg, "SkillIdentifier");
         if(disabled) {
            logger.warn("disabled skill due to 500 error with body {}", body);
         }
         return disabled;
      } finally {
         consumeQuietly(entity);
      }
   }

   private boolean disabledSkill403(CloseableHttpResponse response) throws IOException {
      if(response.getStatusLine().getStatusCode() != HttpStatus.SC_FORBIDDEN) {
         return false;
      }
      HttpEntity entity = null;
      try {
         entity = response.getEntity();
         Map<String, Object> body = JSON.fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), ERR_TYPE);
         boolean disabled = StringUtils.equals("skill_not_enabled", (String) body.get("error"));
         if(disabled) {
            logger.warn("disabled skill due to 403 error with body {}", body);
         }
         return disabled;
      } finally {
         consumeQuietly(entity);
      }
   }

   private ProactiveCreds executeOAuth(List<NameValuePair> args) {
      HttpPost post = createPost(config.getOauthEndpoint(), OAUTH_HEADER);
      List<NameValuePair> form = ImmutableList.<NameValuePair>builder()
         .addAll(args)
         .add(new BasicNameValuePair("client_id", config.getOauthClientId()))
         .add(new BasicNameValuePair("client_secret", config.getOauthClientSecret()))
         .build();
      post.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

      try(CloseableHttpClient client = httpClient()) {
         HttpEntity entity = null;
         try(CloseableHttpResponse response = client.execute(post)) {
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
               entity = response.getEntity();
               Map<String, Object> error = JSON.fromJson(EntityUtils.toString(entity, StandardCharsets.UTF_8), ERR_TYPE);
               logger.warn("oauth request failed with {}", error);
               if(StringUtils.equals("invalid_grant", (String) error.get("error"))) {
                  logger.warn("disabled skill because the grant is no longer valid");
                  throw new SkillDisabledException();
               }
               throw new RuntimeException("oauth http post failed: " + response.getStatusLine());
            }
            return createCreds(response);
         } finally {
            consumeQuietly(entity);
         }
      } catch(IOException e) {
         AlexaMetrics.incOauthFailure();
         throw new RuntimeException(e);
      }
   }

   private void consumeQuietly(HttpEntity entity) {
      if(entity != null) {
         EntityUtils.consumeQuietly(entity);
      }
   }

   private static ProactiveCreds createCreds(CloseableHttpResponse response) throws IOException {
      HttpEntity entity = response.getEntity();
      String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
      EntityUtils.consumeQuietly(entity);
      TokenInfo tokenInfo = JSON.fromJson(responseBody, TokenInfo.class);
      Date expiresIn = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tokenInfo.getExpires_in()));
      return new ProactiveCreds(tokenInfo.getAccess_token(), expiresIn, tokenInfo.getRefresh_token());
   }

   private static HttpPost createPost(String url, Header contentType) {
      HttpPost post = new HttpPost(url);
      post.setHeader(contentType);
      return post;
   }

   private static class TokenInfo {
      private String access_token;
      private int expires_in;
      private String refresh_token;

      public String getAccess_token() {
         return access_token;
      }

      public void setAccess_token(String access_token) {
         this.access_token = access_token;
      }

      public int getExpires_in() {
         return expires_in;
      }

      public void setExpires_in(int expires_in) {
         this.expires_in = expires_in;
      }

      public String getRefresh_token() {
         return refresh_token;
      }

      public void setRefresh_token(String refresh_token) {
         this.refresh_token = refresh_token;
      }
   }

   private CloseableHttpClient httpClient() {
      return HttpClients.custom()
         .setConnectionManager(pool)
         .setConnectionManagerShared(true)
         .build();
   }
}

