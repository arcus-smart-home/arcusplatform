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
package com.iris.bridge.server.client;

import java.util.Date;
import java.util.UUID;

import org.apache.shiro.session.InvalidSessionException;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.security.authz.AuthorizationContext;
import com.iris.security.principal.Principal;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * 
 */
// TODO should this move down to arcus-security (minus the channel stuff)?
public interface Client {
   public static final AttributeKey<Client> ATTR_CLIENT =
         AttributeKey.<Client>valueOf(Client.class.getName());

   /**
    * Returns {@code true} if the current session is authenticated.
    * For client bridges this means associated with a logged in user.
    * When {@code false} it indicates this is an anonymous session.
    * @return
    */
   public boolean isAuthenticated();
   
   public Principal getPrincipal();
   
   /**
    * The name of the authenticated principal.  If isAuthenticated() == false,
    * this will return [anonymous] or a similar string.
    * @return
    */
   public default String getPrincipalName() {
      Principal p = getPrincipal();
      if(p == null) {
         return "[anonymous]";
      }
      return p.getUsername();
   }
   
   /**
    * Gets the UUID of the subject.  For user logins this should be
    * a Person ID.  For hubs this will be the hub id.
    * If there is no subject (anonymous sessions) this will return
    * {@code null}.
    * @return
    */
   public default UUID getPrincipalId() {
      Principal p = getPrincipal();
      if(p == null) {
         return null;
      }
      return p.getUserId();
   }
   
   public String getSessionId();
   
   public Date getLoginTime();

   public default boolean isExpired() {
   	Date expirationTime = getExpirationTime();
   	return expirationTime == null ? false : expirationTime.before(new Date());
   }
   
   public Date getExpirationTime();
   
   public void resetExpirationTime();
   
	/**
    * Sets the time in <b>milliseconds</b> that the session may remain idle before expiring.
    * <ul>
    * <li>A negative value means the session will never expire.</li>
    * <li>A non-negative value (0 or greater) means the session expiration will occur if idle for that
    * length of time.</li>
    * </ul>
    * By default this will throw an {@link UnsupportedOperationException}.  Clients that support session
    * expiration must over-ride this method.
    * 
    * @param timeoutMs the time in milliseconds that the session may remain idle before expiring.
    * @throws InvalidSessionException if the session has been stopped or expired prior to calling this method.
    */   
   public default void setSessionExpirationTimeout(long timeoutMs) { 
      throw new UnsupportedOperationException(); 
   }
   
   /**
    * Invoked whenever a request is sent by this client.  Some clients
    * may use this to keep the session alive.
    */
   public default void requestReceived() { } 
   
   public AuthorizationContext getAuthorizationContext();
   
   public default AuthorizationContext getAuthorizationContext(boolean reload) { return getAuthorizationContext(); }
   
   public void login(Object authenticationToken);
   
   public void logout();
   
   public static void bind(Channel channel, Client client) {
      channel.attr(ATTR_CLIENT).set(client);
   }
   
   @Nullable
   public static Client get(Channel channel) {
   	return channel.attr(ATTR_CLIENT).get();
   }
   
   public static void clear(Channel channel) {
      channel.attr(ATTR_CLIENT).set(null);
   }
}

