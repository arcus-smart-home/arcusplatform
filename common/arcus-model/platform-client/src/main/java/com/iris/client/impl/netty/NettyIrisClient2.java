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
package com.iris.client.impl.netty;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.ClientMessage;
import com.iris.client.ClientRequest;
import com.iris.client.ErrorEvent;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.connection.ConnectionEvent;
import com.iris.client.connection.ConnectionState;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.exception.ClientException;
import com.iris.client.exception.ConnectionException;
import com.iris.client.exception.ErrorResponseException;
import com.iris.client.exception.UnauthenticatedException;
import com.iris.client.exception.UnauthorizedException;
import com.iris.client.impl.ClientMessageSerializer;
import com.iris.client.impl.MessageConstants;
import com.iris.client.service.PersonService;
import com.iris.client.service.PersonService.ResetPasswordRequest;
import com.iris.client.service.SessionService;
import com.iris.client.service.SessionService.SetActivePlaceResponse;
import com.iris.client.session.Credentials;
import com.iris.client.session.HandoffTokenCredentials;
import com.iris.client.session.ResetPasswordCredentials;
import com.iris.client.session.SessionActivePlaceSetEvent;
import com.iris.client.session.SessionAuthenticatedEvent;
import com.iris.client.session.SessionEvent;
import com.iris.client.session.SessionExpiredEvent;
import com.iris.client.session.SessionInfo;
import com.iris.client.session.SessionPlaceClearedEvent;
import com.iris.client.session.SessionTokenCredentials;
import com.iris.client.session.UsernameAndPasswordCredentials;
import com.iris.client.util.CachedCallable;
import com.iris.client.util.Result;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringEncoder;

public class NettyIrisClient2 implements IrisClient {
   private static final Logger logger = LoggerFactory.getLogger(NettyIrisClient2.class);

   private static final int MAX_RECONNETION_ATTEMPTS = Integer.MAX_VALUE;
   private static final int SECONDS_BETWEEN_RECONNECTION_ATTEMPTS = 5;
   private static final int MAX_WEBSOCKET_FRAME_BYTES = 1024 * 1024;

   private static final String PATH_LOGIN        = "/login";
   private static final String PATH_WEBSOCKET    = "/websocket";
   private static final String PATH_LOGOUT       = "/logout";
   private static final String PATH_WEBLAUNCH    = "/web/launch";

   private static final String AUTH_COOKIE       = "irisAuthToken";
   private static final String ICST_AUTH_COOKIE  = "prodIrisAuthToken";
   private static final String ICST_AUTH_COOKIE2 = "devIrisAuthToken";

   private final AtomicReference<ConnectionState> connectionStateRef = new AtomicReference<>(ConnectionState.CLOSED);
   private final CachedCallable<Client> clientRef = new CachedCallable<Client>() {

      @Override
      protected Client load() throws Exception {
         return createClient();
      }

      @Override
      protected void afterCleared(Client result) {
         setConnectionState(ConnectionState.CLOSED);
         result.disconnect();
         result.shutdown();
      }

   };

   /**
    * Message & Session References.
    */
   private final ScheduledExecutorService timeouts;
   private final Map<String, PendingResponse> futures =  Collections.synchronizedMap(new HashMap<String, PendingResponse>());
   private final AtomicReference<LoginHandler> loginRef =  new AtomicReference<LoginHandler>();
   private final AtomicBoolean authenticated = new AtomicBoolean(false);
   private final AtomicReference<SessionInfo> sessionRef = new AtomicReference<>();
   private final AtomicReference<UUID> activePlaceRef = new AtomicReference<>();
   private final Map<String, String> userAgentParams =  Collections.synchronizedMap(new HashMap<String, String>());

   /**
    * If you start with secure prefix (wss or https) this will continue to use that.
    */
   private volatile String connectionURL;

   /**
    * Listeners
    */
   private final ListenerList<ConnectionEvent> connectionListeners   = new ListenerList<>();
   private final ListenerList<SessionEvent>    sessionEventListeners = new ListenerList<>();
   private final ListenerList<ClientRequest>   requestListeners      = new ListenerList<>();
   private final ListenerList<ClientMessage>   messageListeners      = new ListenerList<>();

   public NettyIrisClient2() {
      timeouts = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
               .setNameFormat("request-timeouts")
               .setDaemon(true)
               .build()
      );
      timeouts.scheduleWithFixedDelay(new Runnable() {
         @Override
         public void run() {
            timeout();
         }
      }, 1, 1, TimeUnit.SECONDS);

      addMessageListener(new Listener<ClientMessage>() {
         @Override
         public void onEvent(ClientMessage message) {
            if (MessageConstants.MSG_SESSION_CREATED.equals(message.getType())) {
               updateSessionInfo(message.getEvent());
            }
            else if (SessionService.SetActivePlaceResponse.NAME.equals(message.getType())) {
               updateActivePlace(message.getEvent());
            }
            else if (SessionService.ActivePlaceClearedEvent.NAME.equals(message.getType())) {
               UUID placeId = activePlaceRef.get();
               ClientEvent event = message.getEvent();
               if(placeId != null && StringUtils.equals(placeId.toString(), (String) event.getAttribute("placeId"))) {
                  clearActivePlace();
               }
            }
         }
      });
   }

   @Override
   public boolean isConnected() {
      return connectionStateRef.get() == ConnectionState.CONNECTED;
   }

   public boolean isClosed() {
      return connectionStateRef.get() == ConnectionState.CLOSED;
   }

   public boolean isAuthenticated() {
      return authenticated.get();
   }

   @Override
   public ConnectionState getConnectionState() {
      return connectionStateRef.get();
   }

   @Override
   public SessionInfo getSessionInfo() {
      return sessionRef.get();
   }

   @Override
   public String getConnectionURL() {
      return connectionURL;
   }

   @Override
   public void setConnectionURL(String connectionURL) throws IllegalStateException {
      if(!isClosed()) {
         throw new IllegalStateException("Can't change the connection URL while connected");
      }
      this.connectionURL = toHttpUrl(connectionURL);
   }

   @Override
	public void setClientAgent(String agent) {
   	if (agent == null || "".equals(agent)) {
   		userAgentParams.remove(IrisClient.CLIENT_DEVICE);
   	}
   	else {
   		userAgentParams.put(IrisClient.CLIENT_DEVICE, agent);
   	}
	}

	@Override
	public void setClientVersion(String version) {
		if (version == null || "".equals(version)) {
   		userAgentParams.remove(IrisClient.CLIENT_VERSION);
   	}
   	else {
   		userAgentParams.put(IrisClient.CLIENT_VERSION, version);
   	}
	}

   @Override
   public UUID getActivePlace() {
      return activePlaceRef.get();
   }

   @Override
   public ClientFuture<SessionInfo> login(Credentials credentials) {
      Preconditions.checkNotNull(credentials, "Must supply credentials for login authentication.");
      Preconditions.checkNotNull(credentials.getConnectionURL(), "Connection URL cannot be null.");

      clientRef.clear();
      if(sessionRef.getAndSet(null) != null) {
         logger.warn("Already logged in, destroying previous session");
      }

      this.connectionURL = toHttpUrl(credentials.getConnectionURL());

      LoginHandler handler = new LoginHandler();
      LoginHandler old = loginRef.getAndSet(handler);
      if(old != null) {
         old.cancel(true);
      }
      handler.login(credentials);
      return handler;
   }

   @Override
   public ClientFuture<?> logout() {
      String sessionId = getSessionId();
      if(sessionId == null) {
         // already logged out...
         logger.debug("No session info available, is the user already logged out?");
         return Futures.succeededFuture(true);
      }

      NettyHttpRequest.Builder request = logoutRequestBuilder();
      LogoutHandler handler = new LogoutHandler();
      sendHttpRequest(request).onCompletion(handler);
      return handler;
   }

   @Override
   public ClientFuture<UUID> setActivePlace(String placeId) {
      if (isClosed()) {
         return Futures.failedFuture(new ConnectionException("Cannot set active place while not connected."));
      }

      SessionService.SetActivePlaceRequest request =
            new SessionService.SetActivePlaceRequest();
      request.setAddress(Addresses.toServiceAddress(SessionService.NAMESPACE));
      request.setPlaceId(placeId);
      request.setTimeoutMs(600000);

      return
            Futures
               .transform(
                     websocketRequest(request),
                     new Function<ClientEvent, UUID>() {
                        @Override
                        public UUID apply(ClientEvent input) {
                           SetActivePlaceResponse response = new SetActivePlaceResponse(input);
                           return UUID.fromString(response.getPlaceId());
                        }
                     }
               )
               .onSuccess(new Listener<UUID>() {
                  @Override
                  public void onEvent(UUID placeId) {
                     activePlaceRef.set(placeId);
                     sessionEventListeners.fireEvent(new SessionActivePlaceSetEvent(placeId));
                  }
               });
   }

   @Override
   public ClientFuture<String> linkToWeb() {
      return linkToWeb("", Collections.<String,String>emptyMap());
   }

   @Override
   public ClientFuture<String> linkToWeb(String destination) {
      return linkToWeb(destination, Collections.<String,String>emptyMap());
   }

   @Override
   public ClientFuture<String> linkToWeb(String destination, Map<String, String> queryParams) {
      NettyHttpRequest.Builder builder = webLinkBuilder(destination, queryParams);
      return 
         sendHttpRequest(builder)
            .chain(new Function<NettyHttpResponse, ClientFuture<String>>() {
               @Override
               public ClientFuture<String> apply(NettyHttpResponse response) {
                  if(response.getStatusCode() >= 300 && response.getStatusCode() < 400) {
                     String location = response.getLocation();
                     if(StringUtils.isEmpty(location)) {
                        return Futures.failedFuture(new ClientException("Got a redirect but no location was specified") {});
                     }
                     else {
                        return Futures.succeededFuture(location);
                     }
                  }
                  else if(response.getStatusCode() == HttpResponseStatus.UNAUTHORIZED.code()) {
                     return Futures.failedFuture(new UnauthorizedException());
                  }
                  else if(response.getStatusCode() == HttpResponseStatus.FORBIDDEN.code()) {
                     return Futures.failedFuture(new UnauthenticatedException());
                  }
                  else {
                     return Futures.failedFuture(new ClientException("Unexpected status code: " + response.getStatusCode()) { });
                  }
               }
            });
   }

   @Override
   public void submit(ClientRequest clientRequest) {
      if (!isAuthenticated()) {
         throw new IllegalStateException("Client is not authenticated. Please login() first");
      }

      ClientMessage message =
            ClientMessage
               .builder()
               .isRequest(false)
               .withDestination(clientRequest.getAddress())
               .withType(clientRequest.getCommand())
               .withAttributes(clientRequest.getAttributes())
               .create();

      try {
         websocketSendMessage(clientRequest, message);
      } catch (IllegalStateException ex) {
         close();
         logger.error("Unable to send message.", ex);
      }
   }

   @Override
   public ClientFuture<ClientEvent> request(ClientRequest clientRequest) {
      if (clientRequest.isRestfulRequest()) {
         return restfulRequest(clientRequest);
      } else {
         return websocketRequest(clientRequest);
      }
   }

   @Override
   public void close() {
      if(!setConnectionState(ConnectionState.CLOSED)) {
         logger.trace("Ignoring request to close, the platform messages service is not running");
         return;
      }

      for(ClientFuture<?> future : futures.values()) {
         future.cancel(true);
      }
      timeouts.shutdown();
      futures.clear();
      clientRef.clear();
   }

   @Override
   public ListenerRegistration addConnectionListener(Listener<? super ConnectionEvent> l) {
      return connectionListeners.addListener(l);
   }

   @Override
   public ListenerRegistration addSessionListener(Listener<? super SessionEvent> l) {
      return sessionEventListeners.addListener(l);
   }

   @Override
   public ListenerRegistration addRequestListener(Listener<? super ClientRequest> l) {
      return requestListeners.addListener(l);
   }

   @Override
   public ListenerRegistration addMessageListener(Listener<? super ClientMessage> l) {
      return messageListeners.addListener(l);
   }

   protected ClientFuture<NettyHttpResponse> sendHttpRequest(NettyHttpRequest.Builder requestBuilder) {
      NettyHttpResponseHandler handler = new NettyHttpResponseHandler();
      try {
         String sessionId = getSessionId();
         if(sessionId != null) {
            requestBuilder.addCookie(AUTH_COOKIE, sessionId);
         }
         NettyHttpRequest request =
               requestBuilder
                  .setHandler(handler)
                  .build();
         getClient().executeAsyncHttpRequest(request);
      }
      catch(Exception e) {
         handler.setError(e);
      }
      return handler;
   }

   protected Client openWebSocket() {
      final String sessionId = getSessionId();
      if(sessionId == null) {
//         throw new ConnectionException("Not authenticated, please login first");
         throw new IllegalStateException("Not authenticated, please login first");
      }

      Client client = getClient();
      if(connectionStateRef.compareAndSet(ConnectionState.CLOSED, ConnectionState.CONNECTING)) {
         logger.debug("Requesting WebSocket Upgrade using Cookie [{}]", sessionId);
         NettyWebsocket.Builder websocketBuilder = NettyWebsocket.builder()
         		.uri(connectionURL + PATH_WEBSOCKET)
               .setTextHandler(new MessageHandler())
               .addHeader("Authorization", sessionId);

         try {
            websocketBuilder.addHeader(HttpHeaders.Names.HOST, new URL(connectionURL).getHost());
         } catch (MalformedURLException e) {
            logger.error("Connection URL is malformed. Unable to set host header, SNI may fail.", e);
         }

         for (Map.Entry<String, String> item : userAgentParams.entrySet()) {
         	logger.debug("Requesting WebSocket Upgrade Adding Header [{}]:[{}]", item.getKey(), item.getValue());
         	websocketBuilder.addHeader(item.getKey(), item.getValue());
         }

         websocketBuilder.retryAttempts(MAX_RECONNETION_ATTEMPTS)
               .retryDelayInSeconds(SECONDS_BETWEEN_RECONNECTION_ATTEMPTS)
               .maxFrameSize(MAX_WEBSOCKET_FRAME_BYTES);

         client.openWebSocket(websocketBuilder.build());
      }
      return client;
   }

   protected NettyHttpRequest.Builder loginRequestBuilder(UsernameAndPasswordCredentials credentials) {
      return
            NettyHttpRequest
               .builder()
               .post()
               .uri(connectionURL + PATH_LOGIN)
               .post()
               .addFormParam("user", credentials.getUsername())
               .addFormParam("password", credentials.getPassword())
               ;
   }

   protected NettyHttpRequest.Builder loginRequestBuilder(HandoffTokenCredentials credentials) {
      return
            NettyHttpRequest
               .builder()
               .post()
               .uri(connectionURL + PATH_LOGIN)
               .post()
               .addFormParam("token", credentials.getToken())
               ;
   }

   protected NettyHttpRequest.Builder webLinkBuilder(String deeplink, Map<String, String> queryParams) {
      SessionInfo info = getSessionInfo();
      QueryStringEncoder codec;
      if(info != null) {
         codec = new QueryStringEncoder(info.getWebLaunchUrl() + deeplink);
      }
      else {
         codec = new QueryStringEncoder(connectionURL + PATH_WEBLAUNCH + deeplink);
      }
      if(queryParams != null) {
         for(Map.Entry<String, String> e: queryParams.entrySet()) {
            codec.addParam(e.getKey(), e.getValue());
         }
      }
      return
            NettyHttpRequest
               .builder()
               .get()
               .uri(codec.toString())
               ;
   }

   protected NettyHttpRequest.Builder resetPasswordRequestBuilder(ResetPasswordCredentials credentials) {
      ResetPasswordRequest request = new ResetPasswordRequest();
      request.setRestfulRequest(true);
      request.setTimeoutMs(30000);
      request.setAddress("SERV:" + PersonService.NAMESPACE);
      request.setEmail(credentials.getUsername());
      request.setToken(credentials.getToken());
      request.setPassword(credentials.getPassword());
      return restfulRequestBuilder(request);
   }

   protected NettyHttpRequest.Builder logoutRequestBuilder() {
      return
            NettyHttpRequest
               .builder()
               .post()
               .uri(connectionURL + PATH_LOGOUT)
               .post()
               ;
   }

   protected NettyHttpRequest.Builder restfulRequestBuilder(ClientRequest clientRequest) {
      String url = toHttpUrl(clientRequest.getConnectionURL());
      if(url == null) {
         url = connectionURL;
      }
      if(StringUtils.isEmpty(url)) {
         throw new IllegalStateException("No connection url is specified");
      }

      url = url + "/" + clientRequest.getCommand().replace(":", "/");
      logger.debug("Making client request to: [{}], With command: [{}]", url, clientRequest.getCommand());

      String sessionId = getSessionId();
      NettyHttpRequest.Builder requestBuilder = NettyHttpRequest.builder().uri(url);

      for (Map.Entry<String, String> item : userAgentParams.entrySet()) {
      	logger.debug("Http Request Adding Header [{}]:[{}]", item.getKey(), item.getValue());
      	requestBuilder.addHeader(item.getKey(), item.getValue());
      }

      if(sessionId != null) {
         requestBuilder.addCookie(AUTH_COOKIE, sessionId);
      }
      if (clientRequest.getAddress().startsWith("GET")) {
         requestBuilder.get();
         if (!clientRequest.getAttributes().isEmpty()) {
            for (Map.Entry<String,Object> attrib : clientRequest.getAttributes().entrySet()) {
               requestBuilder.addFormParam(attrib.getKey(), (String) attrib.getValue());
            }
         }
      }
      else {
         ClientMessage message =
               ClientMessage
                  .builder()
                  .isRequest(true)
                  .withDestination(clientRequest.getAddress())
                  .withType(clientRequest.getCommand())
                  .withAttributes(clientRequest.getAttributes())
                  .create();
         requestBuilder.post();
         requestBuilder.setJson(ClientMessageSerializer.serialize(message));
      }
      return requestBuilder;
   }

   // Session State

   private String getSessionId() {
      SessionInfo info = getSessionInfo();
      if(info == null) {
         return null;
      }
      else {
         return info.getSessionToken();
      }
   }

   protected void updateSessionInfo(ClientEvent event) {
      LoginHandler handler = loginRef.getAndSet(null);
      if(handler == null) {
         // this means we re-connected
         UUID placeId = activePlaceRef.get();
         if(placeId != null) {
            logger.info("Socket re-connected, re-establishing place");
            setActivePlace(placeId.toString())
               .onFailure(new Listener<Throwable>() {
                  @Override
                  public void onEvent(Throwable event) {
                     logger.warn("Unable to set place", event);
                     clearActivePlace();
                  }
               });
         }
      }
      else {
         SessionInfo info = new SessionInfo(handler.getUsername(), handler.getSessionId(), event);
         sessionRef.set(info);
         authenticated.set(true);
         handler.setValue(info);
         sessionEventListeners.fireEvent(new SessionAuthenticatedEvent(info));
      }
   }

   protected void updateActivePlace(ClientEvent event) {
   }

   protected boolean clearSessionInfo() {
      sessionRef.getAndSet(null);
      if(authenticated.getAndSet(false)) {
         sessionEventListeners.fireEvent(new SessionExpiredEvent());
         return true;
      }
      return false;
   }

   // Connection State

   private boolean setConnectionState(ConnectionState state) {
      if(connectionStateRef.getAndSet(state) != state) {
         connectionListeners.fireEvent(new ConnectionEvent(state));
         return true;
      }
      return false;
   }

   private void clearActivePlace() {
      activePlaceRef.set(null);
      sessionEventListeners.fireEvent(new SessionPlaceClearedEvent());
   }

   private void timeout() {
      long ts = System.currentTimeMillis();
      synchronized(futures) {
         Iterator<PendingResponse> it = futures.values().iterator();
         while(it.hasNext()) {
            PendingResponse response = it.next();
            if(response.getExpirationTimestamp() < ts) {
               response.setError(new CancellationException("Request timed out"));
               it.remove();
            }
         }
      }
   }

   private String toHttpUrl(String url) {
      if(StringUtils.isEmpty(url)) {
         return null;
      }
      return url.startsWith("ws") ? url.replaceFirst("ws", "http") : url;
   }

   private Client getClient() {
      try {
         return clientRef.call();
      }
      catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   private ClientFuture<ClientEvent> restfulRequest(ClientRequest clientRequest) {
      NettyHttpRequest.Builder builder = restfulRequestBuilder(clientRequest);
      HttpResponseHandler handler = new HttpResponseHandler();
      sendHttpRequest(builder).onCompletion(handler);
      return handler;
   }

   private ClientFuture<ClientEvent> websocketRequest(ClientRequest clientRequest) {
      if (!isAuthenticated()) {
         return Futures.failedFuture(new IllegalStateException("Client is not connected. Please login() first"));
      }
      else {
         int timeoutMs = clientRequest.getTimeoutMs();
         PendingResponse response =
               new PendingResponse(timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : System.currentTimeMillis() + 30000);

         String corID = UUID.randomUUID().toString();
         futures.put(corID, response);
         ClientMessage message =
               ClientMessage
                  .builder()
                  .isRequest(true)
                  .withCorrelationId(corID)
                  .withDestination(clientRequest.getAddress())
                  .withType(clientRequest.getCommand())
                  .withAttributes(clientRequest.getAttributes())
                  .create();

         try {
            websocketSendMessage(clientRequest, message);
            return response;
         } catch (IllegalStateException ex) {
            futures.remove(corID);
            close();
            return Futures.failedFuture(ex);
         }
      }
   }

   private void websocketSendMessage(ClientRequest request, ClientMessage message) {
      // TODO
      requestListeners.fireEvent(request);
      String payload = ClientMessageSerializer.serialize(message);
      getClient().fire(payload);
      logger.debug("Request Sent To Platform: {}", payload);
   }

   private Client createClient() throws Exception {
      logger.debug("Using {} as the default SSL trust algorithm.", TrustManagerFactory.getDefaultAlgorithm());
      TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      KeyStore ks = null;
      factory.init(ks);

      return new Client(factory, new ClientWebsocketStateListener(),  IrisClientFactory.getClientConfig().getMaxReconnectionAttempts(), IrisClientFactory.getClientConfig().getSecondsBetweenReconnectionAttempts(), IrisClientFactory.getClientConfig().getMaxResponseSize());
   }

   private static class NettyHttpResponseHandler extends SettableClientFuture<NettyHttpResponse> implements ResponseHandler {

      private NettyHttpResponseHandler() {
         // have to process the response in the
         super(MoreExecutors.directExecutor());
      }

      @Override
      public void onThrowable(Throwable throwable) {
         setError(throwable);
      }

      @Override
      public void onCompleted(NettyHttpResponse response) {
         setValue(response);
      }

   }

   private class LoginHandler extends SettableClientFuture<SessionInfo> implements Listener<Result<NettyHttpResponse>> {
      private String username = "Unknown";
      private String sessionId;

      /**
       * @return the username
       */
      public String getUsername() {
         return username;
      }

      /**
       * @return the sessionId
       */
      public String getSessionId() {
         return sessionId;
      }

      private void setSessionId(String sessionId) {
         this.sessionId = sessionId;
      }

      public void login(Credentials credentials) {
         if(credentials instanceof UsernameAndPasswordCredentials) {
            this.username = ((UsernameAndPasswordCredentials) credentials).getUsername();
            NettyHttpRequest.Builder request = loginRequestBuilder((UsernameAndPasswordCredentials) credentials);
            sendHttpRequest(request).onCompletion(this);
         }
         else if(credentials instanceof HandoffTokenCredentials) {
            NettyHttpRequest.Builder request = loginRequestBuilder((HandoffTokenCredentials) credentials);
            sendHttpRequest(request).onCompletion(this);
         }
         else if(credentials instanceof ResetPasswordCredentials) {
            this.username = ((ResetPasswordCredentials) credentials).getUsername();
            NettyHttpRequest.Builder request = resetPasswordRequestBuilder((ResetPasswordCredentials) credentials);
            logger.info("HTTP reset request to: {}", connectionURL);
            sendHttpRequest(request).onCompletion(this);
         }
         else if(credentials instanceof SessionTokenCredentials) {
            updateSessionAndConnect(((SessionTokenCredentials) credentials).getToken());
         }
         else {
            setError(new ConnectionException("Unsupported credentials: " + credentials.getClass()));
         }
      }

      @Override
      public void onEvent(Result<NettyHttpResponse> result) {
         if(result.isError()) {
            setError(result.getError());
         }
         else {
            try {
               onLoginResponse(result.getValue());
            }
            catch(Exception e) {
               setError(e);
            }
         }
      }

      private void onLoginResponse(NettyHttpResponse response) {
         logger.trace("Login response [{}]", response.getBodyAsText());

         if (response.getStatusCode() != 200) {
            setError(new UnauthorizedException("Invalid Username/Password? Server Returned " + response.getStatusCode()));
         }
         else {
            String authToken = response.getCookieValue(AUTH_COOKIE);
            if(authToken == null) {
               authToken = response.getCookieValue(ICST_AUTH_COOKIE);
            }
            if(authToken == null) {
               authToken = response.getCookieValue(ICST_AUTH_COOKIE2);
            }
            if (authToken == null) {
               setError(new ConnectionException("Unable to locate session credentials."));
            }
            else {
               updateSessionAndConnect(authToken);
            }
         }

      }

      private void updateSessionAndConnect(String sessionId) {
         setSessionId(sessionId);
         sessionRef.set(new SessionInfo(sessionId, username, null, null, null, null));
         try {
            openWebSocket();
         }
         catch(Exception e) {
            setError(e);
         }
      }

   }

   private class LogoutHandler extends SettableClientFuture<Void> implements Listener<Result<NettyHttpResponse>> {

      @Override
      public void onEvent(Result<NettyHttpResponse> event) {
         if(event.isError()) {
            setError(event.getError());
            return;
         }

         // TODO should this always just be marked as a success
         NettyHttpResponse response = event.getValue();
         if(response == null) {
            setError(new ConnectionException("Empty response"));
         }
         else if(response.getStatusCode() == 200 || response.getStatusCode() == 401) {
            close();
            clearSessionInfo();
            setValue(null);
         }
         else {
            setError(new ConnectionException("Unexpected response code: " + response.getStatusCode()));
         }
      }

   }

   private class MessageHandler implements TextMessageHandler {

      @Override
      public void handleMessage(String json) {
         logger.debug("Got incoming message: {}", json);

         try {
            ClientMessage msg = ClientMessageSerializer.deserialize(json, ClientMessage.class);
            messageListeners.fireEvent(msg);
            SettableClientFuture<ClientEvent> result = futures.remove(msg.getCorrelationId());
            if (result != null) {
               ClientEvent event = msg.getEvent();
               if(event instanceof ErrorEvent) {
                  result.setError(new ErrorResponseException(((ErrorEvent) event).getCode(), ((ErrorEvent) event).getMessage()));
               }
               else {
                  result.setValue(event);
               }
            }
         }
         catch (Exception ex) {
            logger.error("Failed to handle incoming message " + json + ":", ex);
         }

      }
   }

   private class ClientWebsocketStateListener implements WebsocketStateHandler {

      @Override
      public void onConnecting() {
         setConnectionState(ConnectionState.CONNECTING);
      }

      @Override
      public void onConnected() {
         setConnectionState(ConnectionState.CONNECTED);
      }

      @Override
      public void onDisconnected() {
         setConnectionState(ConnectionState.DISCONNECTED);
      }

      @Override
      public void onException(Throwable cause) {
         // TODO there Client should always present the final state
         logger.warn("Error on websocket", cause);
         LoginHandler handler = loginRef.getAndSet(null);
         if(handler != null) {
            handler.setError(cause);
            // don't let it retry in the background
            getClient().disconnect();
         }
      }

      @Override
      public void onClosed(CloseCause cause) {
         // TODO better info about closed
         logger.debug("Websocket closed because [{}]", cause);
         setConnectionState(ConnectionState.CLOSED);
         if(cause == CloseCause.SESSION_EXPIRED) {
            clearSessionInfo();
         }
      }

   }

   private static class PendingResponse extends SettableClientFuture<ClientEvent> {
      private long expirationTimestamp;

      PendingResponse(long expirationTimestamp) {
         this.expirationTimestamp = expirationTimestamp;
      }

      public long getExpirationTimestamp() {
         return expirationTimestamp;
      }
   }

   private class HttpResponseHandler extends SettableClientFuture<ClientEvent> implements Listener<Result<NettyHttpResponse>> {

      @Override
      public void onEvent(Result<NettyHttpResponse> event) {
         if(event.isError()) {
            setError(event.getError());
         }
         else {
            onResponse(event.getValue());
         }
      }

      private void onResponse(NettyHttpResponse response) {
         // TODO follow redirects
         String json = response.getBodyAsText();
         logger.debug("Received Http Message From Server: {}", json);
         ClientMessage message = null;
         try {
            message = ClientMessageSerializer.deserialize(json, ClientMessage.class);
         }
         catch(Exception e) {
            logger.debug("Non json content [{}: {}]", response.getStatusCode(), json);
            setError(new ErrorResponseException("http.error." + response.getStatusCode(), json));
            return;
         }

         try {
         	if(message != null) {
               messageListeners.fireEvent(message);
               if(ErrorEvent.NAME.equals(message.getType())) {
                  ClientEvent event = message.getEvent();
                  setError(new ErrorResponseException(
                        (String) event.getAttribute(ErrorEvent.ATTR_CODE),
                        (String) event.getAttribute(ErrorEvent.ATTR_MESSAGE)
                  ));
               }
               else {
                  setValue(message.getEvent());
               }
         	}
         }
         catch(Exception e) {
            setError(e);
         }
      }

   }

}

