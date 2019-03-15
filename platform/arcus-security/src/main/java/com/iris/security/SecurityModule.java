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
package com.iris.security;

import java.util.Collection;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.guice.ShiroModule;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;

import com.google.inject.Inject;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.iris.security.credentials.CredentialsHashingStrategy;
import com.iris.security.credentials.Sha256CredentialsHashingStrategy;
import com.iris.security.dao.AuthenticationDAO;
import com.iris.security.dao.AuthorizationDAO;
import com.iris.security.dao.NoopAuthorizationDAOImpl;
import com.iris.security.principal.DefaultPrincipalResolver;
import com.iris.security.principal.PrincipalResolver;

public abstract class SecurityModule extends ShiroModule {
	
	private final long globalSessionTimeoutInSecs;

	@Inject
   public SecurityModule(SessionConfig config) {
      this.globalSessionTimeoutInSecs = config.getDefaultSessionTimeoutInSecs();
   }

   @Override
   protected void configureShiro() {
      bindSessionDAO(bind(SessionDAO.class));
      bindCacheManager(bind(CacheManager.class));
      bindAuthenticationDAO(bind(AuthenticationDAO.class));
      bindAuthorizationDAO(bind(AuthorizationDAO.class));
      bindPrincipalResolver(bind(PrincipalResolver.class));
      bindCredentialsHashingStrategy(bind(CredentialsHashingStrategy.class));
      expose(CredentialsHashingStrategy.class);
      bindRealm().to(GuicedIrisRealm.class);
   }

   @Override
   protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
	  bind.to(DefaultSessionManager.class).asEagerSingleton();
	  bind(DefaultSessionManager.class);
      bindConstant().annotatedWith(Names.named("shiro.globalSessionTimeout")).to(globalSessionTimeoutInSecs * 1000L);
      bindConstant().annotatedWith(Names.named("shiro.sessionValidationSchedulerEnabled")).to(false);
   }
   
   @Override
   protected void bindSecurityManager(AnnotatedBindingBuilder<? super SecurityManager> bind) {
   	try {
   		bind.toConstructor(DefaultSecurityManager.class.getConstructor(Collection.class)).asEagerSingleton();
     } catch (NoSuchMethodException e) {
         throw new ConfigurationException("This really shouldn't happen.  Either something has changed in Shiro, or there's a bug in " + ShiroModule.class.getSimpleName(), e);
     }
   }

	protected abstract void bindAuthenticationDAO(AnnotatedBindingBuilder<AuthenticationDAO> bind);

   protected void bindSessionDAO(AnnotatedBindingBuilder<SessionDAO> bind) {
      bind.to(GuicedCassandraSessionDAO.class);
   }

   protected void bindCacheManager(AnnotatedBindingBuilder<CacheManager> bind) {
      bind.to(MemoryConstrainedCacheManager.class);
   }

   protected void bindAuthorizationDAO(AnnotatedBindingBuilder<AuthorizationDAO> bind) {
      bind.to(NoopAuthorizationDAOImpl.class);
   }

   protected void bindPrincipalResolver(AnnotatedBindingBuilder<PrincipalResolver> bind) {
      bind.to(DefaultPrincipalResolver.class);
   }

   protected void bindCredentialsHashingStrategy(AnnotatedBindingBuilder<CredentialsHashingStrategy> bind) {
      bind.to(Sha256CredentialsHashingStrategy.class);
   }
}

