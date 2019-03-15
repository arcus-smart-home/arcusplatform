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
package com.iris.hubcom.server.session;

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.hubcom.server.session.listener.HubConnectionSessionListener;
import com.iris.hubcom.server.session.listener.HubSessionListener;

public class HubSessionModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(SessionFactory.class).to(HubSessionFactory.class);
      bind(SessionRegistry.class).to(HubSessionRegistry.class);
      bind(HubSessionListener.class).to(HubConnectionSessionListener.class);
      bind(HubSessionMetrics.class).asEagerSingleton();
   }

}

