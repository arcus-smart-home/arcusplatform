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
package com.iris.client.server.rest;

import java.util.List;
import java.util.UUID;

import com.iris.bridge.server.client.Client;
import com.iris.security.authz.AuthorizationGrant;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class RESTHandlers
{
   /**
    * @return {@code true} iff there is a {@link Client} on the {@link Channel} and the logged-in person is the owner of
    * the specified account
    */
   public static boolean isAccountOwnerLoggedIn(ChannelHandlerContext ctx, UUID accountId)
   {
      Client client = Client.get(ctx.channel());
      if (client == null)
      {
         return false;
      }

      UUID loggedInPerson = client.getPrincipalId();
      if (loggedInPerson == null)
      {
         return false;
      }

      List<AuthorizationGrant> grants = client.getAuthorizationContext().getGrants();

      for (AuthorizationGrant grant : grants)
      {
         if (grant.getAccountId().equals(accountId)
            && grant.getEntityId().equals(loggedInPerson)
            && grant.isAccountOwner())
         {
            return true;
         }
      }

      return false;
   }

   private RESTHandlers() { }
}

