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
package com.iris.security.handoff;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.security.Login;
import com.iris.security.dao.AppHandoffDao;
import com.iris.security.dao.AppHandoffDao.SessionHandoff;
import com.iris.security.principal.PrincipalResolver;

@Singleton
public class AppHandoffRealm extends AuthenticatingRealm {
	private static final Logger logger = LoggerFactory.getLogger(AppHandoffRealm.class);
	private AppHandoffDao handoffDao;
	private PrincipalResolver principalResolver;
	
	@Inject(optional = true)
   @Named("bounce.handoff.sameip")
	private boolean checkSameIp = false;

	@Inject
	public AppHandoffRealm(AppHandoffDao handoffDao, PrincipalResolver principalResolver) {
		this.handoffDao = handoffDao;
		this.principalResolver = principalResolver;
		// if it gets to this point its authorized
		setCredentialsMatcher(new AllowAllCredentialsMatcher());
	}
	
	@Override
	public boolean supports(AuthenticationToken token) {
		return token instanceof AppHandoffToken;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {		
		SessionHandoff handoff = null;
		try {
			handoff = handoffDao.validate(((AppHandoffToken) token).getToken()).orElseThrow(() -> new IncorrectCredentialsException());
			AppHandoffMetrics.incValidateTokenSuccess();
		}catch(IncorrectCredentialsException e) {
			AppHandoffMetrics.incValidateTokenFailed();
			throw e;
		}
		if(checkSameIp) {
			String tokenHost = ((AppHandoffToken) token).getHost();
			if(StringUtils.isBlank(tokenHost) || StringUtils.isBlank(handoff.getIp()) || !tokenHost.equalsIgnoreCase(handoff.getIp())) {
				if(StringUtils.isBlank(handoff.getIp()) && StringUtils.isBlank(tokenHost)) {
					logger.warn("Both IP in token and app_handoff_token DB is null for person [{}].  Should not happen!", handoff.getPersonId());
				}
				AppHandoffMetrics.incSameIPFailed();
				throw new IncorrectCredentialsException();
			}
			AppHandoffMetrics.incSameIPSuccess();
		}
		Login login = new Login();
		login.setUserId(handoff.getPersonId());
		login.setUsername(handoff.getUsername());
		return new SimpleAuthenticationInfo(principalResolver.resolvePrincipal(login), token, getName()); 
	}
	
	
	private static class AppHandoffMetrics {

	   private AppHandoffMetrics() {
	   }

	   private static final IrisMetricSet METRICS = IrisMetrics.metrics("bounce.handoff");
	   private static final Counter ipMatchSuccess = METRICS.counter("sameip.success");
	   private static final Counter ipMatchFailure = METRICS.counter("sameip.failure");
	   private static final Counter validateTokenFailure = METRICS.counter("validatetoken.failure");
	   private static final Counter validateTokenSuccess = METRICS.counter("validatetoken.success");
	   
	   public static void incSameIPSuccess () { ipMatchSuccess.inc(); }
	   public static void incSameIPFailed() { ipMatchFailure.inc(); }
	   public static void incValidateTokenFailed() { validateTokenFailure.inc(); }
	   public static void incValidateTokenSuccess() { validateTokenSuccess.inc(); }
	}

}

