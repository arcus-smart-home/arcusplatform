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
package com.iris.bridge.server.http.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Iterator;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpPageResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;

/**
 * 
 */
@Singleton
@HttpGet("/sessions")
public class ClientSessionsPage extends HttpPageResource {
	
   
   private static final String HEADER =
         "<html>" + NEWLINE +
            "<head>"  + NEWLINE +
            "<title>Client Sessions</title>"  + NEWLINE +
            "<style>" + NEWLINE +
               "th { border-bottom: 2px solid black; }" + NEWLINE +
               "td { border: 1px solid black; }" + NEWLINE +
            "</style>" + NEWLINE +
         "</head>" + NEWLINE +
         "<body>" + NEWLINE +
         "<h2>Client Sessions</h2>" + NEWLINE + 
         "<div>" + NEWLINE +
            "<table cellspacing='0' cellpadding='2'>" + NEWLINE +
               "<tr>" + NEWLINE +
                  "<th>Token</th>" + NEWLINE +
                  "<th>Client Start Time</th>" + NEWLINE +
                  "<th>Authenticated?</th>" + NEWLINE +
                  "<th>User Name</th>" + NEWLINE +
                  "<th>Session Start Time</th>" + NEWLINE +
                  "<th>Session Expiration Time</th>" + NEWLINE +
                  "<th>Logout</th>" + NEWLINE +
                  "<th>Disconnect</th>" + NEWLINE +
               "</tr>"  + NEWLINE
         ;
   private static final String ROW =
         "<tr>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
            "<td>%s</td>" + NEWLINE +
         "</tr>"  + NEWLINE
         ;
   private static final String EMPTY_ROW =
         "<tr>" + NEWLINE +
            "<td colspan='8' style='text-align: center'><i>No active clients are attached</i></td>" + NEWLINE +
         "</tr>"  + NEWLINE
         ;
   private static final String FOOTER =
            "</table>" + NEWLINE +
         "</div>" + NEWLINE +
         "</body>" + NEWLINE + 
         "</html>" + NEWLINE
         ;
   private static final String ERROR_ROW =
	         "<tr>" + NEWLINE +
	            "<td colspan='8' style='text-align: center'><i>Error retrieving the session:\n%s</i></td>" + NEWLINE +
	         "</tr>"  + NEWLINE
	         ;

   private final SessionRegistry registry;
   
   @Inject
   public ClientSessionsPage(SessionRegistry registry, AlwaysAllow alwaysAllow, BridgeMetrics metrics, ClientFactory factory) {
      super(alwaysAllow, new HttpSender(ClientSessionsPage.class, metrics), factory);
      this.registry = registry;
   }
   
   /* (non-Javadoc)
    * @see com.iris.bridge.server.index.WebResource#getContent(com.iris.bridge.server.client.Client, java.lang.String)
    */
   @Override
   public ByteBuf getContent(Client context, String uri) {
      // TODO enable chunked responses
      StringBuilder sb = new StringBuilder(HEADER);
      Iterator<Session> sessions = registry.getSessions().iterator();
      if(!sessions.hasNext()) {
         sb.append(EMPTY_ROW);
      }
      else {
    	 int count = 0;
         while(sessions.hasNext()) {
            Session session = sessions.next();
            try{
	            sb.append(String.format(
	                  ROW, 
	                  session.getClientToken().getRepresentation(), 
	                  session.getSessionStartTime(), 
	                  session.getClient().isAuthenticated(),
	                  session.getClient().getPrincipalName(),
	                  session.getClient().getLoginTime(),
	                  session.getClient().getExpirationTime(),
	                  createLinkForSession(count++, session, SessionActionHelper.Action.LOGOUT),
	                  createLinkForSession(count++, session, SessionActionHelper.Action.DISCONNECT)
	            ));
            }catch(Exception e) {
            	sb.append(String.format(ERROR_ROW, e.getMessage()));
            }
         }
      }
      sb.append(FOOTER);
      
      return Unpooled.copiedBuffer(sb.toString(), Charsets.US_ASCII);
   }

	private String createLinkForSession(int count, Session session, SessionActionHelper.Action action) {
		String tokenString = "";
		if(session.getClientToken() != null) {
			tokenString = session.getClientToken().getRepresentation();
		}
		return SessionActionHelper.createHtml(count, tokenString, action);   
	}

}

