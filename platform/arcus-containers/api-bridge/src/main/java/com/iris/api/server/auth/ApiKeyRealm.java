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
package com.iris.api.server.auth;

import java.util.Date;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.security.apikey.ApiKey;
import com.iris.security.apikey.ApiKeyGenerator;
import com.iris.security.apikey.ApiKeyPrincipal;

@Singleton
public class ApiKeyRealm extends AuthenticatingRealm {

   private static final Logger logger = LoggerFactory.getLogger(ApiKeyRealm.class);

   private final ApiKeyDAO apiKeyDao;

   @Inject
   public ApiKeyRealm(ApiKeyDAO apiKeyDao) {
      this.apiKeyDao = apiKeyDao;
      setCredentialsMatcher(new AllowAllCredentialsMatcher());
   }

   @Override
   public boolean supports(AuthenticationToken token) {
      return token instanceof ApiKeyToken;
   }

   @Override
   protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      ApiKeyToken apiKeyToken = (ApiKeyToken) token;
      String rawKey = apiKeyToken.getRawKey();

      String keyHash = ApiKeyGenerator.hashKey(rawKey);
      ApiKey apiKey = apiKeyDao.findByKeyHash(keyHash);

      if (apiKey == null) {
         ApiKeyRealmMetrics.incAuthFailed();
         throw new IncorrectCredentialsException("Invalid API key");
      }

      ApiKeyRealmMetrics.incAuthSuccess();

      // Update lastUsed asynchronously (best-effort)
      try {
         apiKeyDao.updateLastUsed(keyHash, new Date());
      } catch (Exception e) {
         logger.debug("Failed to update lastUsed for API key {}", apiKey.getId(), e);
      }

      ApiKeyPrincipal principal = new ApiKeyPrincipal(
            apiKey.getId(),
            apiKey.getLabel(),
            apiKey.getPlaceId(),
            apiKey.getPersonId(),
            apiKey.getAccountId(),
            apiKey.getPermissions()
      );

      return new SimpleAuthenticationInfo(principal, token, getName());
   }

   private static class ApiKeyRealmMetrics {
      private ApiKeyRealmMetrics() {}

      private static final IrisMetricSet METRICS = IrisMetrics.metrics("apikey.auth");
      private static final Counter authSuccess = METRICS.counter("success");
      private static final Counter authFailed = METRICS.counter("failure");

      public static void incAuthSuccess() { authSuccess.inc(); }
      public static void incAuthFailed() { authFailed.inc(); }
   }
}
