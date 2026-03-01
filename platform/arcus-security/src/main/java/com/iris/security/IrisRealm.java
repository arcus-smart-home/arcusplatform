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
import org.apache.shiro.codec.Base64;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;

/**
 * Realm that allows authentication and authorization via Cassandra calls.
 *
 * @since 0.2
 *
 * @deprecated This class has been replaced with the GuicedIrisRealm.
 */
@Deprecated
public class IrisRealm extends AuthorizingRealm {

   protected static final String DEFAULT_AUTHENTICATION_QUERY = "select password from login where domain = ? and user_0_3 = ? and user = ?";
   protected static final String DEFAULT_SALTED_AUTHENTICATION_QUERY = "select password, password_salt from login where domain = ? and user_0_3 = ? and user = ?";
   protected static final String DEFAULT_USER_ROLES_QUERY = "select role_names from user_roles where user = ?";
   protected static final String DEFAULT_PERMISSIONS_QUERY = "select permission_names from role_permissions where role_name = ?";

   private static final Logger log = LoggerFactory.getLogger(IrisRealm.class);

   public enum SaltStyle {
      NO_SALT, CRYPT, COLUMN, EXTERNAL
   }

   private String keyspaceName;
   protected CqlSession cassandraSession;

   protected String authenticationQuery = DEFAULT_AUTHENTICATION_QUERY;
   private PreparedStatement preparedAuthenticationQuery;

   protected String userRolesQuery = DEFAULT_USER_ROLES_QUERY;
   private PreparedStatement preparedUserRolesQuery;

   protected String permissionsQuery = DEFAULT_PERMISSIONS_QUERY;
   private PreparedStatement preparedPermissionsQuery;

   protected boolean permissionsLookupEnabled = false;

   protected SaltStyle saltStyle = SaltStyle.COLUMN;

   @Override
   protected void onInit() {
      super.onInit();
      preparedAuthenticationQuery = cassandraSession.prepare(authenticationQuery);
      preparedUserRolesQuery = cassandraSession.prepare(userRolesQuery);
      preparedPermissionsQuery = cassandraSession.prepare(permissionsQuery);
   }

   public String getKeyspaceName() {
      return keyspaceName;
   }

   public void setKeyspaceName(String keyspaceName) {
      this.keyspaceName = keyspaceName;
   }

   public void setSession(CqlSession cassandraSession) {
      this.cassandraSession = cassandraSession;

      // session changed, re-prepare queries
      preparedAuthenticationQuery = cassandraSession.prepare(authenticationQuery);
      preparedUserRolesQuery = cassandraSession.prepare(userRolesQuery);
      preparedPermissionsQuery = cassandraSession.prepare(permissionsQuery);
   }

   public void setAuthenticationQuery(String authenticationQuery) {
      this.authenticationQuery = authenticationQuery;
      if (cassandraSession != null) {
         preparedAuthenticationQuery = cassandraSession.prepare(authenticationQuery);
      }
   }

   public void setUserRolesQuery(String userRolesQuery) {
      this.userRolesQuery = userRolesQuery;
      if (cassandraSession != null) {
         preparedUserRolesQuery = cassandraSession.prepare(userRolesQuery);
      }
   }

   public void setPermissionsQuery(String permissionsQuery) {
      this.permissionsQuery = permissionsQuery;
      if (cassandraSession != null) {
         preparedPermissionsQuery = cassandraSession.prepare(permissionsQuery);
      }
   }

   public void setPermissionsLookupEnabled(boolean permissionsLookupEnabled) {
      this.permissionsLookupEnabled = permissionsLookupEnabled;
   }

   public void setSaltStyle(String saltStyleStr) {
      SaltStyle saltStyle = SaltStyle.valueOf(saltStyleStr);
      this.saltStyle = saltStyle;
      if (saltStyle == SaltStyle.COLUMN && authenticationQuery.equals(DEFAULT_AUTHENTICATION_QUERY)) {
         authenticationQuery = DEFAULT_SALTED_AUTHENTICATION_QUERY;
         if (cassandraSession != null) {
            preparedAuthenticationQuery = cassandraSession.prepare(authenticationQuery);
         }
      }
   }

   @Override
   protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      UsernamePasswordToken upToken = (UsernamePasswordToken) token;
      String username = upToken.getUsername();

      if (username == null) {
         throw new AccountException("Null usernames are not allowed by this realm.");
      }

      SimpleAuthenticationInfo info = null;
      try {
         String password = null;
         String salt = null;
         switch (saltStyle) {
            case NO_SALT:
               password = getPasswordForUser(cassandraSession, username)[0];
               break;
            case CRYPT:
               throw new ConfigurationException("Not implemented yet");
            case COLUMN:
               String[] queryResults = getPasswordForUser(cassandraSession, username);
               password = queryResults[0];
               salt = queryResults[1];
               break;
            case EXTERNAL:
               password = getPasswordForUser(cassandraSession, username)[0];
               salt = getSaltForUser(username);
         }

         if (password == null) {
            throw new UnknownAccountException("No account found for user [" + username + "]");
         }

         info = new SimpleAuthenticationInfo(username, password.toCharArray(), getName());

         if (salt != null) {
            info.setCredentialsSalt(ByteSource.Util.bytes(Base64.decode(salt)));
         }

      } catch (SQLException e) {
         final String message = "There was a SQL error while authenticating user [" + username + "]";
         if (log.isErrorEnabled()) {
            log.error(message, e);
         }

         throw new AuthenticationException(message, e);
      }

      return info;
   }

   private String[] getPasswordForUser(CqlSession cassandraSession, String username) throws SQLException {
      String[] result;
      boolean returningSeparatedSalt = false;
      switch (saltStyle) {
         case NO_SALT:
         case CRYPT:
         case EXTERNAL:
            result = new String[1];
            break;
         default:
            result = new String[2];
            returningSeparatedSalt = true;
      }

      ParsedEmail parsedEmail = ParsedEmail.parse(username);
      BoundStatement boundStatement = preparedAuthenticationQuery.bind(parsedEmail.getDomain(), parsedEmail.getUser_0_3(), parsedEmail.getUser());
      Row row = cassandraSession.execute(boundStatement).one();

      if (row == null) {
         return result;
      }

      result[0] = row.getString("password");
      if (returningSeparatedSalt) {
         result[1] = row.getString("password_salt");
      }

      return result;
   }

   @Override
   protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
      if (principals == null) {
         throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
      }

      String username = (String) getAvailablePrincipal(principals);

      Set<String> roleNames;
      Set<String> permissions = null;
      try {
         roleNames = getRoleNamesForUser(cassandraSession, username);
         if (permissionsLookupEnabled) {
            permissions = getPermissions(cassandraSession, roleNames);
         }
      } catch (SQLException e) {
         final String message = "There was a SQL error while authorizing user [" + username + "]";
         if (log.isErrorEnabled()) {
            log.error(message, e);
         }

         throw new AuthorizationException(message, e);
      }

      SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
      info.setStringPermissions(permissions);
      return info;

   }

   protected Set<String> getRoleNamesForUser(CqlSession cassandraSession, String username) throws SQLException {
      ParsedEmail parsedEmail = ParsedEmail.parse(username);
      BoundStatement boundStatement = preparedUserRolesQuery.bind(parsedEmail.getDomain(), parsedEmail.getUser_0_3(), parsedEmail.getUser());
      Row row = cassandraSession.execute(boundStatement).one();
      Set<String> roleNames = row.getSet("role_names", String.class);
      return roleNames;
   }

   protected Set<String> getPermissions(CqlSession cassandraSession, Collection<String> roleNames) throws SQLException {
      Set<String> permissions = new LinkedHashSet<String>();
      for (String roleName : roleNames) {
         BoundStatement boundStatement = preparedPermissionsQuery.bind(roleName);
         Row row = cassandraSession.execute(boundStatement).one();
         Set<String> thesePermissions = row.getSet("permission_names", String.class);
         permissions.addAll(thesePermissions);
      }

      return permissions;
   }

   protected String getSaltForUser(String username) {
      return username;
   }
}
