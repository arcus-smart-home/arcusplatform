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
package com.iris.client.server.healthcheck;

import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.iris.bridge.server.http.handlers.AdHocSessionAction;
import com.iris.bridge.server.http.handlers.ClientSessionsPage;
import com.iris.bridge.server.http.health.HealthCheckServerConfig;
import com.iris.bridge.server.http.health.HttpHealthCheckModule;
import com.iris.bridge.server.http.impl.HttpResource;

public class ClientBridgeHealthCheckModule extends HttpHealthCheckModule {

   /* (non-Javadoc)
    * @see com.iris.bridge.server.http.health.HttpHealthCheckModule#configure()
    */
   @Override
   protected void configure() {
      super.configure();
      
      Multibinder<HttpResource> resourceBinder =
            Multibinder
               .newSetBinder(binder(), HttpResource.class, Names.named(HealthCheckServerConfig.NAME_HEALTHCHECK_RESOURCES));
      resourceBinder.addBinding().to(ClientSessionsPage.class);
      resourceBinder.addBinding().to(AdHocSessionAction.class);
   }

}

