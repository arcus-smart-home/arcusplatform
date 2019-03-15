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
/**
 * 
 */
package com.iris.bridge.server.shiro;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.client.Client;
import com.iris.netty.security.IrisNettyAuthorizationContextLoader;
import com.iris.platform.util.LazyReference;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.principal.Principal;

/**
 * 
 */
public class ShiroClient implements Client {
	private static final long SHORT_SESSION_MS = TimeUnit.HOURS.toMillis(1);
			
	private final static Logger logger = LoggerFactory.getLogger(ShiroClient.class);

   private final Subject subject;
   private final LazyReference<AuthorizationContext> context;
   private final String principalName;
   private volatile boolean shortSession = false;
   
   ShiroClient(IrisNettyAuthorizationContextLoader contextLoader) {
      this.context = LazyReference.fromCallable(() -> contextLoader.loadContext(getPrincipal()));
      this.subject = new Subject.Builder().buildSubject();

      Principal pr = (Principal)subject.getPrincipal();
      this.principalName = (pr != null) ? pr.getUsername() : "[anonymous]"; 
   }

   ShiroClient(IrisNettyAuthorizationContextLoader contextLoader, String clientId) {
      this.context = LazyReference.fromCallable(() -> contextLoader.loadContext(getPrincipal()));
      this.subject = new Subject.Builder()
         .sessionId(clientId)
         .buildSubject();
      
      if (subject != null && subject.isAuthenticated()) {
      	resetExpirationTime();
      	Session session = subject.getSession(false);
      	if(session != null && session.getTimeout() <= SHORT_SESSION_MS) {
      		shortSession = true;
      	}
      }

      Principal pr = (Principal)subject.getPrincipal();
      this.principalName = (pr != null) ? pr.getUsername() : "[anonymous]"; 
   }

   @Override
   public boolean isAuthenticated() {
      return subject.isAuthenticated() && !isExpired() && startedAfterLastPasswordChange();
   }
   
   @Override
   public String getPrincipalName() {
      return principalName;
   }
   
   @Override
   public Principal getPrincipal() {
      return (Principal) subject.getPrincipal();
   }
   
   @Override
   public String getSessionId() {
      Session session = this.subject.getSession(false);
      if(session == null) {
         return null;
      }
      return session.getId().toString();
   }

   @Override
   public Date getLoginTime() {
      return subject.getSession().getStartTimestamp();
   }

   private boolean startedAfterLastPasswordChange() {
      AuthorizationContext context = getAuthorizationContext();
      Date lastPasswordChange = context.getLastPasswordChange();
      if(lastPasswordChange == null) {
         return true;
      }
      return subject.getSession().getStartTimestamp().after(lastPasswordChange);
   }

   @Override
	public boolean isExpired() {
		try {
			return Client.super.isExpired();
		}
		catch(UnknownSessionException e) {
			return true;
		}
	}

	@Override
   public Date getExpirationTime() {
      long timeout = subject.getSession().getTimeout();
      if(timeout <= 0) {
         return null;
      }
      return new Date(subject.getSession().getLastAccessTime().getTime() + timeout);
   }

   @Override
   public void resetExpirationTime() {
      subject.getSession().touch();
   }

   @Override
   public void requestReceived() {
      // reset the expiration time for every request on short sessions
      // don't worry about it for longer sessions to reduce db writes
      if(shortSession) {
         resetExpirationTime();
      }
   }

   @Override
   public void setSessionExpirationTimeout(long timeoutMs) {
      shortSession = timeoutMs <= SHORT_SESSION_MS;
      subject.getSession().setTimeout(timeoutMs); 
      
      /*
       * Update the lastAccessTime of this session to the current time when
       * this method is invoked.
       */
      subject.getSession().touch();
   }

   @Override
   public AuthorizationContext getAuthorizationContext() {
      return context.get();
   }

   @Override
   public AuthorizationContext getAuthorizationContext(boolean reload) {
      if(reload) {
         context.reset();
      }
      return getAuthorizationContext();
   }

   @Override
   public void login(Object credentials) throws AuthenticationException {
      if(!(credentials instanceof AuthenticationToken)) {
         throw new UnsupportedTokenException("Invalid authentication token");
      }
      
      subject.login((AuthenticationToken) credentials);
   }

   @Override
   public void logout() {
   	logger.debug("Logout of Shiro Client for session Id: {}", getSessionId());
      subject.logout();
   }

}

