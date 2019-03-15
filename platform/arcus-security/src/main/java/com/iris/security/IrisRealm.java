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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;

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
 *
 * @deprecated This class has been replaced with the GuicedIrisRealm.
 */
@Deprecated
public class IrisRealm extends AuthorizingRealm {

   //TODO - complete JavaDoc

    /*--------------------------------------------
    |             C O N S T A N T S             |
    ============================================*/
   /**
    * The default query used to retrieve account data for the user.
    */
   protected static final String DEFAULT_AUTHENTICATION_QUERY = "select password from login where domain = ? and user_0_3 = ? and user = ?";

   /**
    * The default query used to retrieve account data for the user when {@link #saltStyle} is COLUMN.
    */
   protected static final String DEFAULT_SALTED_AUTHENTICATION_QUERY = "select password, password_salt from login where domain = ? and user_0_3 = ? and user = ?";

   /**
    * The default query used to retrieve the roles that apply to a user.
    */
   protected static final String DEFAULT_USER_ROLES_QUERY = "select role_names from user_roles where user = ?";

   /**
    * The default query used to retrieve permissions that apply to a particular role.
    */
   protected static final String DEFAULT_PERMISSIONS_QUERY = "select permission_names from role_permissions where role_name = ?";

   private static final Logger log = LoggerFactory.getLogger(IrisRealm.class);

   /**
    * Password hash salt configuration. <ul>
    * <li>NO_SALT - password hashes are not salted.</li>
    * <li>CRYPT - password hashes are stored in unix crypt format.</li>
    * <li>COLUMN - salt is in a separate column in the database.</li>
    * <li>EXTERNAL - salt is not stored in the database. {@link #getSaltForUser(String)} will be called
    * to get the salt</li></ul>
    */
   public enum SaltStyle {
      NO_SALT, CRYPT, COLUMN, EXTERNAL
   }

   /*--------------------------------------------
   |    I N S T A N C E   V A R I A B L E S    |
   ============================================*/
   private String keyspaceName;
   private Cluster cluster; //created during init
   protected com.datastax.driver.core.Session cassandraSession;

   protected String authenticationQuery = DEFAULT_AUTHENTICATION_QUERY;
   private PreparedStatement preparedAuthenticationQuery;

   protected String userRolesQuery = DEFAULT_USER_ROLES_QUERY;
   private PreparedStatement preparedUserRolesQuery;

   protected String permissionsQuery = DEFAULT_PERMISSIONS_QUERY;
   private PreparedStatement preparedPermissionsQuery;

   protected boolean permissionsLookupEnabled = false;

   protected SaltStyle saltStyle = SaltStyle.COLUMN;

   /*--------------------------------------------
   |         C O N S T R U C T O R S           |
   ============================================*/
   @Override
   protected void onInit() {
      super.onInit();
      cassandraSession = cluster.connect(keyspaceName);
      preparedAuthenticationQuery = cassandraSession.prepare(authenticationQuery);
      preparedUserRolesQuery = cassandraSession.prepare(userRolesQuery);
      preparedPermissionsQuery = cassandraSession.prepare(permissionsQuery);
   }

   /*--------------------------------------------
   |  A C C E S S O R S / M O D I F I E R S    |
   ============================================*/
   public Cluster getCluster() {
      return cluster;
   }

   public void setCluster(Cluster cluster) {
      this.cluster = cluster;
   }

   public String getKeyspaceName() {
      return keyspaceName;
   }

   public void setKeyspaceName(String keyspaceName) {
      this.keyspaceName = keyspaceName;
   }

   /**
    * Sets the cassandraSession that should be used to retrieve connections used by this realm.
    *
    * @param cassandraSession
    *       the CQL cassandraSession.
    */
   public void setSession(com.datastax.driver.core.Session cassandraSession) {
      this.cassandraSession = cassandraSession;

      // session changed, re-prepare queries
      preparedAuthenticationQuery = cassandraSession.prepare(authenticationQuery);
      preparedUserRolesQuery = cassandraSession.prepare(userRolesQuery);
      preparedPermissionsQuery = cassandraSession.prepare(permissionsQuery);
   }

   /**
    * Overrides the default query used to retrieve a user's password during authentication.  When using the default
    * implementation, this query must take the user's username as a single parameter and return a single result
    * with the user's password as the first column.  If you require a solution that does not match this query
    * structure, you can override {@link #doGetAuthenticationInfo(org.apache.shiro.authc.AuthenticationToken)} or
    * just {@link #getPasswordForUser(com.datastax.driver.core.Session, String)}
    *
    * @param authenticationQuery
    *       the query to use for authentication.
    * @see #DEFAULT_AUTHENTICATION_QUERY
    */
   public void setAuthenticationQuery(String authenticationQuery) {
      this.authenticationQuery = authenticationQuery;
      if (cassandraSession != null) {
         preparedAuthenticationQuery = cassandraSession.prepare(authenticationQuery);
      }
   }

   /**
    * Overrides the default query used to retrieve a user's roles during authorization.  When using the default
    * implementation, this query must take the user's username as a single parameter and return a row
    * per role with a single column containing the role name.  If you require a solution that does not match this query
    * structure, you can override {@link #doGetAuthorizationInfo(PrincipalCollection)} or just
    * {@link #getRoleNamesForUser(com.datastax.driver.core.Session, String)}
    *
    * @param userRolesQuery
    *       the query to use for retrieving a user's roles.
    * @see #DEFAULT_USER_ROLES_QUERY
    */
   public void setUserRolesQuery(String userRolesQuery) {
      this.userRolesQuery = userRolesQuery;
      if (cassandraSession != null) {
         preparedUserRolesQuery = cassandraSession.prepare(userRolesQuery);
      }
   }

   /**
    * Overrides the default query used to retrieve a user's permissions during authorization.  When using the default
    * implementation, this query must take a role name as the single parameter and return a row
    * per permission with three columns containing the fully qualified name of the permission class, the permission
    * name, and the permission actions (in that order).  If you require a solution that does not match this query
    * structure, you can override {@link #doGetAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)} or just
    * {@link #getPermissions(com.datastax.driver.core.Session, java.util.Collection)}</p>
    * <p>
    * <b>Permissions are only retrieved if you set {@link #permissionsLookupEnabled} to true.  Otherwise,
    * this query is ignored.</b>
    *
    * @param permissionsQuery
    *       the query to use for retrieving permissions for a role.
    * @see #DEFAULT_PERMISSIONS_QUERY
    * @see #setPermissionsLookupEnabled(boolean)
    */
   public void setPermissionsQuery(String permissionsQuery) {
      this.permissionsQuery = permissionsQuery;
      if (cassandraSession != null) {
         preparedPermissionsQuery = cassandraSession.prepare(permissionsQuery);
      }
   }

   /**
    * Enables lookup of permissions during authorization.  The default is "false" - meaning that only roles
    * are associated with a user.  Set this to true in order to lookup roles <b>and</b> permissions.
    *
    * @param permissionsLookupEnabled
    *       true if permissions should be looked up during authorization, or false if only
    *       roles should be looked up.
    */
   public void setPermissionsLookupEnabled(boolean permissionsLookupEnabled) {
      this.permissionsLookupEnabled = permissionsLookupEnabled;
   }

   /**
    * Sets the salt style.  See {@link #saltStyle}.
    *
    * @param saltStyleStr
    *       new SaltStyle to set.
    */
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

    /*--------------------------------------------
    |               M E T H O D S               |
    ============================================*/

   @Override
   protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      UsernamePasswordToken upToken = (UsernamePasswordToken) token;
      String username = upToken.getUsername();

      // Null username is invalid
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
               // TODO: separate password and hash from getPasswordForUser[0]
               throw new ConfigurationException("Not implemented yet");
               //break;
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

         // Rethrow any SQL errors as an authentication exception
         throw new AuthenticationException(message, e);
      }

      return info;
   }

   private String[] getPasswordForUser(com.datastax.driver.core.Session cassandraSession, String username) throws SQLException {
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
      BoundStatement boundStatement = new BoundStatement(preparedAuthenticationQuery);
      Row row = cassandraSession.execute(boundStatement.bind(parsedEmail.getDomain(), parsedEmail.getUser_0_3(), parsedEmail.getUser())).one();

      if (row == null) {
         return result;
      }

      result[0] = row.getString("password");
      if (returningSeparatedSalt) {
         result[1] = row.getString("password_salt");
      }

      return result;
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

      String username = (String) getAvailablePrincipal(principals);

      Set<String> roleNames;
      Set<String> permissions = null;
      try {
         // Retrieve roles and permissions from database
         roleNames = getRoleNamesForUser(cassandraSession, username);
         if (permissionsLookupEnabled) {
            permissions = getPermissions(cassandraSession, roleNames);
         }
      } catch (SQLException e) {
         final String message = "There was a SQL error while authorizing user [" + username + "]";
         if (log.isErrorEnabled()) {
            log.error(message, e);
         }

         // Rethrow any SQL errors as an authorization exception
         throw new AuthorizationException(message, e);
      }

      SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
      info.setStringPermissions(permissions);
      return info;

   }

   protected Set<String> getRoleNamesForUser(com.datastax.driver.core.Session cassandraSession, String username) throws SQLException {
      ParsedEmail parsedEmail = ParsedEmail.parse(username);
      BoundStatement boundStatement = new BoundStatement(preparedUserRolesQuery);
      Row row = cassandraSession.execute(boundStatement.bind(parsedEmail.getDomain(), parsedEmail.getUser_0_3(), parsedEmail.getUser())).one();
      Set<String> roleNames = row.getSet("role_names", String.class);
      return roleNames;
   }

   protected Set<String> getPermissions(com.datastax.driver.core.Session cassandraSession, Collection<String> roleNames) throws SQLException {
      Set<String> permissions = new LinkedHashSet<String>();
      for (String roleName : roleNames) {
         BoundStatement boundStatement = new BoundStatement(preparedPermissionsQuery);
         Row row = cassandraSession.execute(boundStatement.bind(roleName)).one();
         Set<String> thesePermissions = row.getSet("permission_names", String.class);
         // Add the permission to the set of permissions
         permissions.addAll(thesePermissions);
      }

      return permissions;
   }

   protected String getSaltForUser(String username) {
      return username;
   }
}

