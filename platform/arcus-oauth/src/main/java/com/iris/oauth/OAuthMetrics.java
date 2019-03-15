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
package com.iris.oauth;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public class OAuthMetrics {

   private OAuthMetrics() {
   }


   private static final IrisMetricSet METRICS = IrisMetrics.metrics("oauth");

   private static final Counter authorizeRequests = METRICS.counter("request.authorize");
   private static final Counter authorizeSuccess = METRICS.counter("request.authorize.success");
   private static final Counter authorizeInvalidRedirect = METRICS.counter("request.authorize.failed.invalidredirect");
   private static final Counter authorizeUnsupportedType = METRICS.counter("request.authorize.failed.unsupportedtype");
   private static final Counter authorizeMissingExtraData = METRICS.counter("request.authorize.failed.missingextra");
   private static final Counter authorizeInvalidScope = METRICS.counter("request.authorize.failed.invalidscope");
   private static final Counter authorizeCannotCreateCode = METRICS.counter("request.authorize.failed.cannotcreatecode");

   private static final Counter listPlacesRequests = METRICS.counter("request.listplaces");

   private static final Counter tokenFailedInvalidGranttype = METRICS.counter("request.token.failed.invalidgranttype");
   private static final Counter tokenFailedMissingParam = METRICS.counter("request.token.failed.missingparam");

   private static final Counter accessTokenRequest = METRICS.counter("request.token.access");
   private static final Counter accessTokenSuccess = METRICS.counter("request.token.access.success");
   private static final Counter accessTokenInvalidRedirect = METRICS.counter("request.token.access.failed.invalidredirect");
   private static final Counter accessTokenExpiredCode = METRICS.counter("request.token.access.failed.codeexpired");
   private static final Counter accessTokenException = METRICS.counter("request.token.access.failed.exception");

   private static final Counter refreshTokenRequest = METRICS.counter("request.token.refresh");
   private static final Counter refreshTokenSuccess = METRICS.counter("request.token.refresh.success");
   private static final Counter refreshTokenExpired = METRICS.counter("request.token.refresh.failed.expired");
   private static final Counter refreshTokenException = METRICS.counter("request.token.refresh.failed.exception");

   public static void incAuthorizeRequests() { authorizeRequests.inc(); }
   public static void incAuthorizeSuccess() { authorizeSuccess.inc(); }
   public static void incAuthorizeMissingExtraData() { authorizeMissingExtraData.inc(); }
   public static void incAuthorizeInvalidRedirect() { authorizeInvalidRedirect.inc(); }
   public static void incAuthorizeUnsupportedType() { authorizeUnsupportedType.inc(); }
   public static void incAuthorizeInvalidScope()  { authorizeInvalidScope.inc(); }
   public static void incAuthorizeCannotCreateCode() { authorizeCannotCreateCode.inc(); }

   public static void incListPlacesRequests() { listPlacesRequests.inc(); }

   public static void incTokenFailedInvalidGranttype() { tokenFailedInvalidGranttype.inc(); }
   public static void incTokenFailedMissingParam() { tokenFailedMissingParam.inc(); }

   public static void incAccessTokenRequest() { accessTokenRequest.inc(); }
   public static void incAccessTokenSuccess() { accessTokenSuccess.inc(); }
   public static void incAccessTokenInvalidRedirect() { accessTokenInvalidRedirect.inc(); }
   public static void incAccessTokenExpiredCode() { accessTokenExpiredCode.inc(); }
   public static void incAccessTokenException() { accessTokenException.inc(); }

   public static void incRefreshTokenRequest() { refreshTokenRequest.inc(); }
   public static void incRefreshTokenSuccess() { refreshTokenSuccess.inc(); }
   public static void incRefreshTokenExpired() { refreshTokenExpired.inc(); }
   public static void incRefreshTokenException() { refreshTokenException.inc(); }

}

