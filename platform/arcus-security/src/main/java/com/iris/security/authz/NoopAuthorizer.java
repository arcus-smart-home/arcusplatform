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
package com.iris.security.authz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;

@Singleton
public class NoopAuthorizer implements Authorizer {

   private static Logger logger = LoggerFactory.getLogger(NoopAuthorizer.class);

   @Override
   public boolean isAuthorized(AuthorizationContext context, String placeId, PlatformMessage message) {
      logger.debug("Authorized [{}] for message [{}]", context.getSubjectString(), message);
      return true;
   }

   @Override
   public PlatformMessage filter(AuthorizationContext context, String placeId, PlatformMessage message) {
      logger.debug("Skipping filtering [{}] for [{}]", message, context.getSubjectString());
      return message;
   }
}

