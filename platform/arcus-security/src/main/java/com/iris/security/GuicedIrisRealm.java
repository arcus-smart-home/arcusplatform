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
/*
 * Adapted from Shiro's JdbcRealm class for IRIS
 *
 * Needs to be refactored
 */
package com.iris.security;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.security.credentials.CredentialsHashingStrategy;
import com.iris.security.dao.AuthenticationDAO;
import com.iris.security.dao.AuthorizationDAO;
import com.iris.security.principal.Principal;
import com.iris.security.principal.PrincipalResolver;

/**
 * Realm that allows authentication and authorization via Cassandra calls.  The default queries suggest a potential schema
 * for retrieving the user's password for authentication, and querying for a user's roles and permissions.  The
 * default queries can be overridden by setting the query properties of the realm.
 * <p>
 * If the default implementation
 * of authentication and authorization cannot handle your schema, this class can be subclassed and the
 * appropriate methods overridden. (usually {@link #doGetAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken)},
 * {@link #getRoleNamesForUser(com.datastax.driver.core.Session, String)}, and/or {@link #getPermissions(com.datastax.driver.core.Session, java.util.Collection)}
 * <p>
 * This realm supports caching by extending from {@link org.apache.shiro.realm.AuthorizingRealm}.
 *
 * @since 0.2
 */
@Singleton
public class GuicedIrisRealm extends AuthorizingRealm {

   //TODO - complete JavaDoc

   private static final Logger log = LoggerFactory.getLogger(GuicedIrisRealm.class);

   /*--------------------------------------------
   |    I N S T A N C E   V A R I A B L E S    |
   ============================================*/

   @Inject(optional = true)
   @Named("irisrealm.permissionsLookupEnabled")
   protected boolean permissionsLookupEnabled = false;

   private final AuthenticationDAO authenticationDao;
   private final AuthorizationDAO authorizationDao;
   private final PrincipalResolver principalResolver;
   private final CredentialsHashingStrategy credentialHashStrategy;

   @Inject
   public GuicedIrisRealm(AuthenticationDAO authenticationDao, AuthorizationDAO authorizationDao,
         PrincipalResolver principalResolver, CredentialsHashingStrategy credentialsHashingStrategy) {
      this.authenticationDao = authenticationDao;
      this.authorizationDao = authorizationDao;
      this.principalResolver = principalResolver;
      this.credentialHashStrategy = credentialsHashingStrategy;
      setCredentialsMatcher(this.credentialHashStrategy.getCredentialsMatcher());
   }

    /*--------------------------------------------
    |               M E T H O D S               |
    ============================================*/

   @Override
   protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      UsernamePasswordToken upToken = (UsernamePasswordToken) token;
      String username = upToken.getUsername();

      // Null username is invalid
      if (username == null) {
      	log.warn("Null usernames are not allowed");
         throw new AccountException("Null usernames are not allowed by this realm.");
      }

      SimpleAuthenticationInfo info = null;
      try {
         Login login = authenticationDao.findLogin(username);

         if (login == null) {
         	log.warn("No account found for user [" + username + "]");
            throw new UnknownAccountException("No account found for user [" + username + "]");
         }

         info = new SimpleAuthenticationInfo(principalResolver.resolvePrincipal(login), login.getPassword(), getName());

         if(credentialHashStrategy.isSalted()) {
            info.setCredentialsSalt(credentialHashStrategy.saltAsBytes(login.getPasswordSalt()));
         }

      } catch (Exception e) {
         final String message = "There was an error while authenticating user [" + username + "]";
         log.error(message, e);
         // Rethrow any SQL errors as an authentication exception
         throw new AuthenticationException(message, e);
      }

      log.debug("Got AuthenticationInfo from Realm {}", info);
      return info;
   }

   /**
    * This implementation of the interface expects the principals collection to return a String username keyed off of
    * this realm's {@link #getName() name}
    *
    * @see #getAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
    */
   @Override
   protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
      //null usernames are invalid
      if (principals == null) {
         throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
      }

      Principal principal = (Principal) getAvailablePrincipal(principals);

      Set<String> roleNames;
      Set<String> permissions = null;
      try {
         // Retrieve roles and permissions from database
         roleNames = getRoleNamesForUser(principal.getUsername());
         if (permissionsLookupEnabled) {
            permissions = getPermissions(roleNames);
         }
      } catch (Exception e) {
         final String message = "There was an error while authorizing user [" + principal.getUsername() + "]";
         log.error(message, e);
         // Rethrow any SQL errors as an authorization exception
         throw new AuthorizationException(message, e);
      }

      SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
      info.setStringPermissions(permissions);
      return info;

   }

   protected Set<String> getRoleNamesForUser(String username) throws SQLException {
      return new HashSet<String>(authorizationDao.findRolesForUser(username));
   }

   protected Set<String> getPermissions(Collection<String> roleNames) throws SQLException {
      Set<String> permissions = new LinkedHashSet<String>();
      for (String roleName : roleNames) {
         permissions.addAll(authorizationDao.findPermissionsForRole(roleName));
      }
      return permissions;
   }

   protected String getSaltForUser(String username) {
      return username;
   }
}

