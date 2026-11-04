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
package com.iris.client.server.passkey;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Passkeys are disabled unless {@code passkey.rp.id} is set.
 */
@Singleton
public class PasskeyConfig {

   /**
    * The relying party id: the domain passkeys are bound to, e.g. "example.com". The
    * web app and the client bridge must both be served from it or a subdomain of it.
    */
   @Inject(optional = true) @Named("passkey.rp.id")
   private String rpId = "";

   /** Shown by the browser and authenticator when creating a passkey. */
   @Inject(optional = true) @Named("passkey.rp.name")
   private String rpName = "Arcus";

   /**
    * Comma-separated origins allowed to use passkeys, e.g. "https://example.com".
    * Defaults to https://{rpId}.
    */
   @Inject(optional = true) @Named("passkey.origins")
   private String origins = "";

   /**
    * The longest a request can live: passkey_request's gc_grace_seconds. A used request
    * has to expire before the tombstone that deleted it can be dropped.
    */
   static final int MAX_REQUEST_TTL_SECS = 3600;

   /**
    * How long a browser has to answer a registration or login request, also sent to the
    * browser as its prompt timeout. Kept between 1 second and {@link #MAX_REQUEST_TTL_SECS}:
    * a TTL of 0 would mean requests never expire, and WebAuthn timeouts must be positive.
    */
   @Inject(optional = true) @Named("passkey.request.ttl.secs")
   private int requestTtlSecs = 300;

   /**
    * How many login requests a client address can make at once. 0 turns rate limiting
    * off.
    */
   @Inject(optional = true) @Named("passkey.ratelimit.capacity")
   private int rateLimitCapacity = 20;

   /** Login requests per second a client address earns back after using its burst. */
   @Inject(optional = true) @Named("passkey.ratelimit.rate")
   private double rateLimitRate = 0.2;

   public boolean isEnabled() {
      return StringUtils.isNotBlank(rpId);
   }

   public String getRpId() {
      return rpId;
   }

   public void setRpId(String rpId) {
      this.rpId = rpId;
   }

   public String getRpName() {
      return rpName;
   }

   public void setRpName(String rpName) {
      this.rpName = rpName;
   }

   public Set<String> getOrigins() {
      if (StringUtils.isBlank(origins)) {
         Set<String> defaults = new LinkedHashSet<>();
         defaults.add("https://" + rpId);
         return defaults;
      }
      return Arrays.stream(origins.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public void setOrigins(String origins) {
      this.origins = origins;
   }

   public int getRequestTtlSecs() {
      return Math.max(1, Math.min(requestTtlSecs, MAX_REQUEST_TTL_SECS));
   }

   public void setRequestTtlSecs(int requestTtlSecs) {
      this.requestTtlSecs = requestTtlSecs;
   }

   public int getRateLimitCapacity() {
      return rateLimitCapacity;
   }

   public void setRateLimitCapacity(int rateLimitCapacity) {
      this.rateLimitCapacity = rateLimitCapacity;
   }

   public double getRateLimitRate() {
      return rateLimitRate;
   }

   public void setRateLimitRate(double rateLimitRate) {
      this.rateLimitRate = rateLimitRate;
   }
}
