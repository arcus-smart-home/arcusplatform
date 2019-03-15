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
package com.iris.netty.server.netty;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cors.CorsConfig;

/**
 * Created by wesleystueve on 5/24/17.
 */
@Singleton
public class IrisNettyCorsConfig {

   private static final Logger logger = LoggerFactory.getLogger(IrisNettyCorsConfig.class);

   private static final String CORS_EXPOSE_HEADERS_PROP = "cors.expose.headers";
   private static final String CORS_ALLOW_REQUEST_HEADERS_PROP = "cors.allow.request.headers";
   private static final String CORS_ALLOW_REQUEST_METHODS_PROP = "cors.allow.request.methods";

   //wds - if you add or change the headers list, be sure to keep everything LOWERCASE.  The rfp for headers says the are case insensitive,
   //but ms edge seems to have an issue with it.  Edge will report the x-client-version is bad, but it's the whole list.  Other browsers seem
   //to be ok. Proper casing is X-Client-Version if they fix the bug: https://developer.microsoft.com/en-us/microsoft-edge/platform/issues/10584749/
   @Inject(optional = true)
   @Named(CORS_EXPOSE_HEADERS_PROP)
   private String exposeRequestHeaders = "accept, accept-encoding, accept-language, access-control-request-headers, cache-control, connection, content-type, dnt, expires, host, last-modified, origin, pragma, referer, set-cookie, user-agent, x-firephp-version, x-client-version";

   @Inject(optional = true)
   @Named(CORS_ALLOW_REQUEST_HEADERS_PROP)
   private String allowRequestHeaders = "accept, accept-encoding, accept-language, access-control-request-headers, cache-control, connection, content-type, dnt, expires, host, last-modified, origin, pragma, referer, set-cookie, user-agent, x-firephp-version, x-client-version";

   @Inject(optional = true)
   @Named(CORS_ALLOW_REQUEST_METHODS_PROP)
   private String allowRequestMethods = "GET, POST, OPTIONS";

   @Inject(optional = true)
   @Named("cors.origins")
   private String corsOrigins = "https://one.example.com, https://two.example.com" ;

   @Inject(optional = true)
   @Named("cors.allow.any")
   private boolean allowAny = false;

   private String[] exposeRequest;
   private String[] allowRequest;
   private HttpMethod[] allowMethods;
   private String[] origins;

   // post constructor to parse the strings into arrays just once instead of everytime build is called, which happens
   // on every http request
   @PostConstruct
   public void init() {
      exposeRequest = split(exposeRequestHeaders, false);
      allowRequest = split(allowRequestHeaders, false);
      String[] splitMethods = split(allowRequestMethods, false);
      allowMethods = new HttpMethod[splitMethods.length];
      for(int i = 0; i < splitMethods.length; i++) {
         allowMethods[i] = new HttpMethod(splitMethods[i]);
      }
      origins = split(corsOrigins ,true);

      logger.info("cors configured expose headers: {}", Arrays.toString(exposeRequest));
      logger.info("cors configured allow headers: {}", Arrays.toString(allowRequest));
      logger.info("cors configured allow methods: {}", Arrays.toString(allowMethods));
      logger.info("cors configured to allow origins: {}", Arrays.toString(origins));
   }

   private String[] split(String str, boolean trim) {
      String[] val = str.split(",");
      if(trim) {
         for(int i = 0; i < val.length; i++) {
            val[i] = val[i].trim();
         }
      }
      return val;
   }

   public CorsConfig build() {
      CorsConfig.Builder builder = allowAny ? CorsConfig.withAnyOrigin()  : CorsConfig.withOrigins(origins);

      return builder
         .allowCredentials()
         .exposeHeaders(exposeRequest)
         .allowedRequestHeaders(allowRequest)
         .allowedRequestMethods(allowMethods)
         .maxAge(1209600)
         .shortCurcuit()
         .build();
   }
}

