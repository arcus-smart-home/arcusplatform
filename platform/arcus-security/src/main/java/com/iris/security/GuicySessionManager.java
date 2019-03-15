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

import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.SessionListener;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class GuicySessionManager extends DefaultSessionManager {
	
	private final long globalSessionTimeoutInSecs;
	
	@Inject(optional=true) @Named("auth.validationschedule.enabled")
	private boolean validationscheduleEnabled = false;
	
	public static final String PROP_SESSION_LISTENERS = "auth.session.listeners";
	
	@Inject
	public GuicySessionManager(SessionDAO sessionDAO, CacheManager cacheManager, @Named(PROP_SESSION_LISTENERS) Set<SessionListener> listeners, SessionConfig config) {
		super();
		this.globalSessionTimeoutInSecs = config.getDefaultSessionTimeoutInSecs();
		setSessionDAO(sessionDAO);
		setCacheManager(cacheManager);		
		setSessionListeners(listeners);
	}
	
	@PostConstruct
	public void init() {
		setGlobalSessionTimeout(globalSessionTimeoutInSecs * 1000);
		setSessionValidationSchedulerEnabled(validationscheduleEnabled);
	}
}

