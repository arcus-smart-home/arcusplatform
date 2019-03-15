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

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.security.authz.filter.DefaultMessageFilter;
import com.iris.security.authz.filter.ListDevicesResponseMessageFilter;
import com.iris.security.authz.filter.MessageFilter;
import com.iris.security.authz.filter.MessageFilterRegistry;
import com.iris.security.authz.permission.GetAttributesPermissionExtractor;
import com.iris.security.authz.permission.PermissionExtractor;
import com.iris.security.authz.permission.PermissionExtractorRegistry;
import com.iris.security.authz.permission.SetAttributesPermissionExtractor;

public class AuthzModule extends AbstractIrisModule {

   public static final String AUTHZ_ALGORITHM_PROP = "authz.algorithm";
   public static final String AUTHZ_ALGORITHM_NONE = "none";
   public static final String AUTHZ_ALGORITHM_PERMISSIONS = "permissions";
   public static final String AUTHZ_ALGORITHM_ROLE = "role";

   @Inject(optional = true)
   @Named(AUTHZ_ALGORITHM_PROP)
   private String algorithm = AUTHZ_ALGORITHM_PERMISSIONS;

   @Override
   protected void configure() {

      bindMapToInstancesOf(
            new TypeLiteral<Map<String, PermissionExtractor>>() {},
            (PermissionExtractor extractor) -> extractor.getSupportedMessageType());

      Multibinder<MessageFilter> filterBinder = bindSetOf(MessageFilter.class);
      filterBinder.addBinding().to(DefaultMessageFilter.class);
      filterBinder.addBinding().to(ListDevicesResponseMessageFilter.class);

      bind(PermissionExtractorRegistry.class);
      bind(MessageFilterRegistry.class);

      if(algorithm.equalsIgnoreCase(AUTHZ_ALGORITHM_NONE)) {
         bind(Authorizer.class).to(NoopAuthorizer.class);
      } else if(algorithm.equalsIgnoreCase(AUTHZ_ALGORITHM_ROLE)) {
         bind(Authorizer.class).to(RoleAuthorizer.class);
      } else {
         bind(Authorizer.class).to(PermissionsAuthorizer.class);
      }

   }

   @Provides
   public GetAttributesPermissionExtractor getAttributesExtractor() {
      return new GetAttributesPermissionExtractor();
   }

   @Provides
   public SetAttributesPermissionExtractor setAttributesExtractor() {
      return new SetAttributesPermissionExtractor();
   }
}

