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
package com.iris.oauth.handlers;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {

   @SerializedName("access_token")
   private final String accessToken;
   @SerializedName("refresh_token")
   private final String refreshToken;
   @SerializedName("token_type")
   private final String tokenType;
   @SerializedName("expires_in")
   private final int expiresIn;

   public TokenResponse(String accessToken, String refreshToken, String tokenType, int expiresIn) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.tokenType = tokenType;
      this.expiresIn = expiresIn;
   }

   public String getAccessToken() {
      return accessToken;
   }

   public String getRefreshToken() {
      return refreshToken;
   }

   public String getTokenType() {
      return tokenType;
   }

   public int getExpiresIn() {
      return expiresIn;
   }
}

