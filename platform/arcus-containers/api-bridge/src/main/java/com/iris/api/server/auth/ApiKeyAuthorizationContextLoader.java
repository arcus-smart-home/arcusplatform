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

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.netty.security.IrisNettyAuthorizationContextLoader;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.principal.Principal;

@Singleton
public class ApiKeyAuthorizationContextLoader implements IrisNettyAuthorizationContextLoader {

   @Inject
   public ApiKeyAuthorizationContextLoader() {
   }

   @Override
   public AuthorizationContext loadContext(Principal principal) {
      if (principal == null) {
         return new AuthorizationContext(null, null, Collections.emptyList());
      }

      if (!(principal instanceof ApiKeyPrincipal)) {
         return new AuthorizationContext(principal, null, Collections.emptyList());
      }

      ApiKeyPrincipal apiKeyPrincipal = (ApiKeyPrincipal) principal;

      // Build a synthetic AuthorizationGrant for the key's place with its permissions
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setEntityId(apiKeyPrincipal.getKeyId());
      grant.setPlaceId(apiKeyPrincipal.getPlaceId());
      grant.setAccountId(apiKeyPrincipal.getAccountId());
      grant.setAccountOwner(false);
      grant.addPermissions(apiKeyPrincipal.getPermissions());

      return new AuthorizationContext(principal, null, Collections.singletonList(grant));
   }
}
