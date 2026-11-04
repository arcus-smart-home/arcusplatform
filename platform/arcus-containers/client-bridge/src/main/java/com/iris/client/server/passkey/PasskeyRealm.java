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
package com.iris.client.server.passkey;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;

import com.google.inject.Inject;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.Person;
import com.iris.security.Login;
import com.iris.security.passkey.PasskeyToken;
import com.iris.security.principal.PrincipalResolver;

/**
 * Logs a person in with a passkey. The WebAuthn response in the token is verified
 * here, so a PasskeyToken only authenticates if it carries a valid signature.
 */
public class PasskeyRealm extends AuthenticatingRealm {
   private final PasskeyService passkeyService;
   private final PersonDAO personDao;
   private final PrincipalResolver principalResolver;

   @Inject
   public PasskeyRealm(PasskeyService passkeyService, PersonDAO personDao, PrincipalResolver principalResolver) {
      this.passkeyService = passkeyService;
      this.personDao = personDao;
      this.principalResolver = principalResolver;
      // the passkey is verified in doGetAuthenticationInfo
      setCredentialsMatcher(new AllowAllCredentialsMatcher());
   }

   @Override
   public boolean supports(AuthenticationToken token) {
      return token instanceof PasskeyToken;
   }

   @Override
   protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      PasskeyToken passkeyToken = (PasskeyToken) token;
      UUID personId;
      try {
         personId = passkeyService.finishLogin(passkeyToken.getRequestId(), passkeyToken.getCredentials());
      }
      catch (PasskeyService.PasskeyException e) {
         throw new IncorrectCredentialsException(e.getMessage());
      }

      Person person = personDao.findById(personId);
      if (person == null || !person.getHasLogin() || StringUtils.isBlank(person.getEmail())) {
         // the person was removed, or no longer has a login
         throw new IncorrectCredentialsException("No login for passkey owner");
      }
      Login login = new Login();
      login.setUserId(person.getId());
      login.setUsername(person.getEmail());
      return new SimpleAuthenticationInfo(principalResolver.resolvePrincipal(login), token, getName());
   }
}
