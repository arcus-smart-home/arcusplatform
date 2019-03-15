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
package com.iris.client;

import java.io.Closeable;
import java.util.Map;
import java.util.UUID;

import com.iris.client.connection.ConnectionEvent;
import com.iris.client.connection.ConnectionState;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.session.Credentials;
import com.iris.client.session.SessionEvent;
import com.iris.client.session.SessionInfo;

/**
 * @author tweidlin
 *
 */
public interface IrisClient extends Closeable {
	public static final String CLIENT_DEVICE = "User-Agent";
	public static final String CLIENT_VERSION = "X-Client-Version";

	ClientFuture<SessionInfo> login(Credentials credentials);

	ClientFuture<?> logout();

	ClientFuture<UUID> setActivePlace(String placeId);

	UUID getActivePlace();
	
	/**
	 * Create an authenticated link to the
	 * web.
	 * @return
	 */
	ClientFuture<String> linkToWeb();
	
	/**
	 * Create an authenticated link to the
	 * web with the given deep link destination.
	 * @param destination
	 * @return
	 */
	ClientFuture<String> linkToWeb(String destination);
	
	/**
	 * Create an authenticated link to the
	 * web with the given deep link destination and
	 * query parameters.
	 * @param destination
	 * @param queryParams
	 * @return
	 */
	ClientFuture<String> linkToWeb(String destination, Map<String, String> queryParams);

	// TODO should this return a ClientFuture indicating the message made it to the platform?
	void submit(ClientRequest request);

	ClientFuture<ClientEvent> request(ClientRequest request);

	SessionInfo getSessionInfo();
	
	String getConnectionURL();

	void setClientAgent(String agent);

	void setClientVersion(String version);

	/**
	 * {@code true} when the websocket is actively connected.
	 * @deprecated Use {@link #getConnectionState()} instead.
	 * @return
	 */
   boolean isConnected();
   
   ConnectionState getConnectionState();

   /**
    * Sets the base URI for the service, can't be changed while connected.
    * @param connectionURL
    * @throws IllegalStateException
    */
   void setConnectionURL(String connectionURL) throws IllegalStateException;
   
   /**
    * Receives events about the change in the socket state, generally only useful
    * for debugging / informative purposes.
    * @param l
    * @return
    */
   ListenerRegistration addConnectionListener(Listener<? super ConnectionEvent> l);
   
	/**
	 * Receives events about changes in the session.
	 * @param l
	 * @return
	 */
	ListenerRegistration addSessionListener(Listener<? super SessionEvent> l);

	/**
	 * Receives client requests before they are sent to the platform service. Generally
	 * used for debugging.
	 * @param l
	 * @return
	 */
	ListenerRegistration addRequestListener(Listener<? super ClientRequest> l);

	/**
	 * Receives client messages from the server.
	 * @param l
	 * @return
	 */
	ListenerRegistration addMessageListener(Listener<? super ClientMessage> l);

}

