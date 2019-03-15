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
package com.iris.bridge.server.http.handlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.RequestHandlerImpl;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.http.impl.responder.RedirectResponder;

@Singleton
@HttpGet("/")
@HttpGet("/index*")
public class RootRemoteRedirect extends RequestHandlerImpl {

   @Inject
   public RootRemoteRedirect(AlwaysAllow alwaysAllow, BridgeMetrics metrics, Config config) {
      super(alwaysAllow, new RedirectResponder(config.redirectUrl, new HttpSender(RootRemoteRedirect.class, metrics)));
   }

   public static class Config {
      @Inject(optional = true) @Named("root.redirectUrl")
      private String redirectUrl = "";

      /**
       * @return the redirectUrl
       */
      public String getRedirectUrl() {
         return redirectUrl;
      }

      /**
       * @param redirectUrl the redirectUrl to set
       */
      public void setRedirectUrl(String redirectUrl) {
         this.redirectUrl = redirectUrl;
      }

   }
}

