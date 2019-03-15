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
package com.iris.bridge.server.http.health;

import com.google.inject.PrivateModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import com.iris.bridge.server.http.handlers.AdHocSessionAction;
import com.iris.bridge.server.http.handlers.CheckPage;
import com.iris.bridge.server.http.handlers.ClientSessionsPage;
import com.iris.bridge.server.http.handlers.StatusPage;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.platform.cluster.ClusterService;

// this has to be private to prevent the normal request handler from
// auto-discovering the status pages
public class HttpHealthCheckModule extends PrivateModule {

   @Override
   protected void configure() {
      bind(HttpHealthCheckServer.class)
         .asEagerSingleton();
      expose(HttpHealthCheckServer.class);
      
      OptionalBinder.newOptionalBinder(binder(), ClusterService.class);
      
      Multibinder<HttpResource> resourceBinder =
            Multibinder
               .newSetBinder(binder(), HttpResource.class, Names.named(HealthCheckServerConfig.NAME_HEALTHCHECK_RESOURCES));
      resourceBinder.addBinding().to(CheckPage.class);
      resourceBinder.addBinding().to(StatusPage.class);
   }

}

