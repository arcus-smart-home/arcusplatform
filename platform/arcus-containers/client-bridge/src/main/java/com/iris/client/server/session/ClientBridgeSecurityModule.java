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
package com.iris.client.server.session;

import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.SessionManager;

import com.google.inject.Inject;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.iris.bridge.server.shiro.ShiroSessionRegistryExpirer;
import com.iris.core.dao.cassandra.CassandraAppHandoffDaoModule;
import com.iris.core.dao.cassandra.PersonDAOSecurityModule;
import com.iris.security.GuicySessionManager;
import com.iris.security.SessionConfig;
import com.iris.security.handoff.AppHandoffRealm;
import com.netflix.governator.annotations.Modules;

@Modules(include = CassandraAppHandoffDaoModule.class)
public class ClientBridgeSecurityModule extends PersonDAOSecurityModule {
   
   @Inject
   public ClientBridgeSecurityModule(SessionConfig config) {
      super(config);
   }

   private Multibinder<SessionListener> shiroSessionListeners;
   
   @Override
   protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {     
     shiroSessionListeners = Multibinder.newSetBinder(binder(), SessionListener.class, Names.named(GuicySessionManager.PROP_SESSION_LISTENERS));
     shiroSessionListeners.addBinding().to(ShiroSessionRegistryExpirer.class).asEagerSingleton();

     bind.to(GuicySessionManager.class).asEagerSingleton();
     bind(GuicySessionManager.class);
     bindRealm().to(AppHandoffRealm.class);
   }

}

