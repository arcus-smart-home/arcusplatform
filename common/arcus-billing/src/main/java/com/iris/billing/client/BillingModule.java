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
package com.iris.billing.client;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BillingModule extends AbstractIrisModule {
   private static final Logger logger = LoggerFactory.getLogger(BillingModule.class);

   @Inject(optional = true)
   @Named(value = "billing.api.key")
   private String recurlyAPIKey;

   @Inject(optional = true)
   @Named(value = "billing.url")
   private String recurlyURL;

   @Inject(optional = true)
   @Named(value = "billing.client")
   private String billingClient = "default";

   @Override
   protected void configure() {
   }

   @Provides
   public BillingClient billingClient() {
      switch (billingClient) {
         default:
            logger.warn("unknown billing client {}: using default instead");
            // fall through
         case "default":
         case "recurly":
            logger.info("using recurly billing client");
            return new RecurlyClient(recurlyURL, recurlyAPIKey);

         case "noop":
            logger.warn("using noop billing client");
            return new NoopBillingClient();
      }
   }
}
