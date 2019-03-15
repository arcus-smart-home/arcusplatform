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
package com.iris.client.security;

import java.util.Collections;
import java.util.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.netty.security.IrisNettyAuthorizationContextLoader;
import com.iris.security.Login;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.dao.AuthenticationDAO;
import com.iris.security.principal.Principal;

@Singleton
public class SubscriberAuthorizationContextLoader implements IrisNettyAuthorizationContextLoader {

   private final PersonDAO personDao;
   private final AuthorizationGrantDAO grantDao;

   @Inject
   public SubscriberAuthorizationContextLoader(PersonDAO personDao, AuthorizationGrantDAO grantDao) {
      this.personDao = personDao;
      this.grantDao = grantDao;
   }

   @Override
   public AuthorizationContext loadContext(Principal principal) {
      if(principal == null) {
         // subscriber some how successfully logged in, but they have no principal, so return
         // a context that allows them to do nothing.
         return new AuthorizationContext(principal, null, Collections.<AuthorizationGrant>emptyList());
      }

      Login login = personDao.findLogin(principal.getUsername());
      Date lastPasswordChange = login == null ? null : login.getLastPasswordChange();

      return new AuthorizationContext(principal, lastPasswordChange, grantDao.findForEntity(principal.getUserId()));
   }
}

