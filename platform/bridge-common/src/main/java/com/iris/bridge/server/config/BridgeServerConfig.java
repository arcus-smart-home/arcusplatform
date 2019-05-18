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
package com.iris.bridge.server.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class BridgeServerConfig {
   public static final String TLS_PROVIDER_DEFAULT = "default";
   public static final String TLS_PROVIDER_JDK = "jdk";
   public static final String TLS_PROVIDER_OPENSSL = "openssl";

   public static final String EVENT_LOOP_PROVIDER_DEFAULT = "default";
   public static final String EVENT_LOOP_PROVIDER_NIO = "nio";
   public static final String EVENT_LOOP_PROVIDER_EPOLL = "epoll";

   @Inject(optional = true) @Named("server.name")
   private String serverName = "server";

   @Inject(optional = true) @Named("bridge.name")
   private String bridgeName = "bridge";

   @Inject(optional = true) @Named("bind.address")
   private String bindAddress = "0.0.0.0";

   @Inject(optional = true) @Named("port")
   private int tcpPort = 8083;

   @Inject(optional=true) @Named("application.version")
   private String applicationVersion = "[unknown]";

   @Inject(optional = true) @Named("auth.basic")
   private boolean authBasic = false;

   @Inject(optional = true) @Named("tls.server")
   private boolean tlsServer = false;

   @Inject(optional = true) @Named("tls.server.ciphers")
   private String tlsServerCiphers = "";

   @Inject(optional = true) @Named("tls.server.protocols")
   private String tlsServerProtocols = "";

   @Inject(optional = true) @Named("event.loop.provider")
   private String eventLoopProvider = EVENT_LOOP_PROVIDER_DEFAULT;

   @Inject(optional = true) @Named("tls.provider")
   private String tlsProvider = TLS_PROVIDER_DEFAULT;

   @Inject(optional = true) @Named("tls.server.keystore.filepath")
   private String tlsServerKeystoreFilepath = "keystore.jks";

   @Inject(optional = true) @Named("tls.server.keystore.password")
   private String tlsServerKeystorePassword = "password";

   @Inject(optional = true) @Named("tls.server.cert.filepath")
   private String tlsServerCertificateFilepath = "";

   @Inject(optional = true) @Named("tls.server.privatekey.filepath")
   private String tlsServerPrivateKeyFilepath = "";

   @Inject(optional = true) @Named("tls.server.key.password")
   private String tlsServerKeyPassword = "keypass";

   @Inject(optional = true) @Named("tls.need.client.auth")
   private boolean tlsNeedClientAuth = false;

   @Inject(optional = true) @Named("tls.request.client.auth")
   private boolean tlsRequestClientAuth = false;

   @Inject(optional = true) @Named("tls.handshake.timeout.sec")
   private int tlsHandshakeTimeoutSec = 90;
   
   @Inject(optional = true) @Named("tls.close.notify.timeout.sec")
   private int tlsCloseNotifyTimeoutSec = 10;
   
   // See: https://github.com/netty/netty/issues/832
   // See: https://github.com/AsyncHttpClient/async-http-client/issues/837
   // See https://github.com/netty/netty/issues/2384
   @Inject(optional = true) @Named("tls.session.cache.size")
   private int tlsSessionCacheSize = 1;
   
   // See: https://github.com/netty/netty/issues/832
   // See: https://github.com/AsyncHttpClient/async-http-client/issues/837
   // See https://github.com/netty/netty/issues/2384
   @Inject(optional = true) @Named("tls.session.timeout")
   private long tlsSessionTimeout = 60L;

   @Inject(optional = true) @Named("boss.thread.count")
   private int bossThreadCount = -1;

   @Inject(optional = true) @Named("boss.accept.ratelimit.capacity")
   private int bossAcceptCapacity = -1;

   @Inject(optional = true) @Named("boss.accept.ratelimit.rate")
   private double bossAcceptRate = -1.0;

   @Inject(optional = true) @Named("worker.thread.count")
   private int workerThreadCount = -1;

   @Inject(optional = true) @Named("so.keepalive")
   private boolean soKeepAlive = true;

   // See: http://veithen.github.io/2014/01/01/how-tcp-backlog-works-in-linux.html
   // See: http://tangentsoft.net/wskfaq/advanced.html#backlog
   @Inject(optional = true) @Named("so.backlog")
   private int soBacklog = 128;

   @Inject(optional = true) @Named("web.socket.path")
   private String webSocketPath = "";

   @Inject(optional = true) @Named("web.socket.ping.rate")
   // Seconds between ping frames being sent.
   private int webSocketPingRate = 30;

   @Inject(optional = true) @Named("web.socket.pong.timeout")
   // Seconds before lack of a pong response indicates the socket is dead.
   // TODO: set to non-zero value to enable
   private int webSocketPongTimeout = 0;

   @Inject(optional=true) @Named("use.ssl")
   private boolean useSsl = false;
   
   @Inject(optional=true) @Named("web.socket.maxFrameSizeBytes")
   private int maxFrameSize = 65535;

   @Inject(optional=true) @Named("web.socket.read.idle.close")
   private boolean closeOnReadIdle = false;
   
   @Inject(optional=true) @Named("allow.forwardedfor")
   private boolean allowForwardedFor = false;

   @Inject(optional=true) @Named("allow.forwardedssl")
   private boolean allowForwardedSsl = false;

   @Inject(optional=true) @Named("login.page.enabled")
   private boolean loginPageEnabled = false;

   public String getServerName() { return serverName; }

   public void setServerName(String serverName) { this.serverName = serverName; }

   public String getBridgeName() {
      return bridgeName;
   }

   public void setBridgeName(String bridgeName) {
      this.bridgeName = bridgeName;
   }

   public String getBindAddress() {
      return bindAddress;
   }

   public void setBindAddress(String bindAddress) {
      this.bindAddress = bindAddress;
   }

   public int getTcpPort() {
      return tcpPort;
   }

   public void setTcpPort(int tcpPort) {
      this.tcpPort = tcpPort;
   }

   public String getApplicationVersion() { return applicationVersion; }
   public void setApplicationVersion(String applicationVersion) { this.applicationVersion = applicationVersion; }

   public boolean isTlsServer() {
      return tlsServer;
   }

   public boolean isBasicAuth() {
      return authBasic;
   }

   public void setTlsServer(boolean tlsServer) {
      this.tlsServer = tlsServer;
   }

   public String getTlsServerCiphers() {
      return tlsServerCiphers;
   }

   public void setTlsServerCiphers(String tlsServerCiphers) {
      this.tlsServerCiphers = tlsServerCiphers;
   }

   public String getTlsServerProtocols() {
      return tlsServerProtocols;
   }

   public void setTlsServerProtocols(String tlsServerProtocols) {
      this.tlsServerProtocols = tlsServerProtocols;
   }

   public String getEventLoopProvider() {
      return eventLoopProvider;
   }

   public void setEventLoopProvider(String eventLoopProvider) {
      this.eventLoopProvider = eventLoopProvider;
   }

   public String getTlsProvider() {
      return tlsProvider;
   }

   public void setTlsProvider(String tlsProvider) {
      this.tlsProvider = tlsProvider;
   }

   public String getTlsServerKeystoreFilepath() {
      return tlsServerKeystoreFilepath;
   }

   public void setTlsServerKeystoreFilepath(String tlsServerKeystoreFilepath) {
      this.tlsServerKeystoreFilepath = tlsServerKeystoreFilepath;
   }

   public String getTlsServerKeystorePassword() {
      return tlsServerKeystorePassword;
   }

   public void setTlsServerKeystorePassword(String tlsServerKeystorePassword) {
      this.tlsServerKeystorePassword = tlsServerKeystorePassword;
   }

   public String getTlsServerKeyPassword() {
      return tlsServerKeyPassword;
   }

   public void setTlsServerKeyPassword(String tlsServerKeyPassword) {
      this.tlsServerKeyPassword = tlsServerKeyPassword;
   }

   public String getTlsServerCertificateFilepath() { return tlsServerCertificateFilepath;}

   public String getTlsServerPrivateKeyFilepath() { return tlsServerPrivateKeyFilepath;}

   public boolean isTlsNeedClientAuth() {
      return tlsNeedClientAuth;
   }

   public void setTlsNeedClientAuth(boolean tlsNeedClientAuth) {
      this.tlsNeedClientAuth = tlsNeedClientAuth;
   }

   public boolean isTlsRequestClientAuth() {
      return tlsRequestClientAuth;
   }

   public void setTlsRequestClientAuth(boolean tlsRequestClientAuth) {
      this.tlsRequestClientAuth = tlsRequestClientAuth;
   }

   /**
    * @return the tlsHandshakeTimeoutSec
    */
   public int getTlsHandshakeTimeoutSec() {
      return tlsHandshakeTimeoutSec;
   }

   /**
    * @param tlsHandshakeTimeoutSec the tlsHandshakeTimeoutSec to set
    */
   public void setTlsHandshakeTimeoutSec(int tlsHandshakeTimeoutSec) {
      this.tlsHandshakeTimeoutSec = tlsHandshakeTimeoutSec;
   }

   /**
    * @return the tlsCloseNotifyTimeoutSec
    */
   public int getTlsCloseNotifyTimeoutSec() {
      return tlsCloseNotifyTimeoutSec;
   }

   /**
    * @param tlsCloseNotifyTimeoutSec the tlsCloseNotifyTimeoutSec to set
    */
   public void setTlsCloseNotifyTimeoutSec(int tlsCloseNotifyTimeoutSec) {
      this.tlsCloseNotifyTimeoutSec = tlsCloseNotifyTimeoutSec;
   }

   public int getTlsSessionCacheSize() {
      return tlsSessionCacheSize;
   }

   public void setTlsSessionCacheSize(int tlsSessionCacheSize) {
      this.tlsSessionCacheSize = tlsSessionCacheSize;
   }

   public long getTlsSessionTimeout() {
      return tlsSessionTimeout;
   }

   public void setTlsSessionTimeout(long tlsSessionTimeout) {
      this.tlsSessionTimeout = tlsSessionTimeout;
   }

   public int getBossThreadCount() {
      if (bossThreadCount <= 0) {
         return Runtime.getRuntime().availableProcessors();
      }

      return bossThreadCount;
   }

   public void setBossThreadCount(int bossThreadCount) {
      this.bossThreadCount = bossThreadCount;
   }

   public int getBossAcceptCapacity() {
      return bossAcceptCapacity;
   }

   public void setBossAcceptCapacity(int bossAcceptCapacity) {
      this.bossAcceptCapacity = bossAcceptCapacity;
   }

   public double getBossAcceptRate() {
      return bossAcceptRate;
   }

   public void setBossAcceptRate(double bossAcceptRate) {
      this.bossAcceptRate = bossAcceptRate;
   }

   public int getWorkerThreadCount() {
      if (workerThreadCount <= 0) {
         return Runtime.getRuntime().availableProcessors();
      }

      return workerThreadCount;
   }

   public void setWorkerThreadCount(int workerThreadCount) {
      this.workerThreadCount = workerThreadCount;
   }

   public boolean isSoKeepAlive() {
      return soKeepAlive;
   }

   public void setSoKeepAlive(boolean soKeepAlive) {
      this.soKeepAlive = soKeepAlive;
   }

   public int getSoBacklog() {
      return soBacklog;
   }

   public void setSoBacklog(int soBacklog) {
      this.soBacklog = soBacklog;
   }

   public String getWebSocketPath() {
      return webSocketPath;
   }

   public void setWebSocketPath(String webSocketPath) {
      this.webSocketPath = webSocketPath;
   }

   public int getWebSocketPingRate() {
      return webSocketPingRate;
   }

   public void setWebSocketPingRate(int webSocketPingRate) {
      this.webSocketPingRate = webSocketPingRate;
   }

   public int getWebSocketPongTimeout() {
      return webSocketPongTimeout;
   }

   public void setWebSocketPongTimeout(int webSocketPongTimeout) {
      this.webSocketPongTimeout = webSocketPongTimeout;
   }

   public boolean isUseSsl() {
      return useSsl;
   }

   public void setUseSsl(boolean useSsl) {
      this.useSsl = useSsl;
   }

   public int getMaxFrameSize() {
      return maxFrameSize;
   }

   public void setMaxFrameSize(int maxFrameSize) {
      this.maxFrameSize = maxFrameSize;
   }

   public boolean isCloseOnReadIdle() {
      return closeOnReadIdle;
   }

   public void setCloseOnReadIdle(boolean closeOnReadIdle) {
      this.closeOnReadIdle = closeOnReadIdle;
   }

   public boolean isAllowForwardedFor() {
      return allowForwardedFor;
   }

   public void setAllowForwardedFor(boolean allowForwardedFor) {
      this.allowForwardedFor = allowForwardedFor;
   }

   public boolean isAllowForwardedSsl() {
      return allowForwardedSsl;
   }

   public void setAllowForwardedSsl(boolean allowForwardedSsl) {
      this.allowForwardedSsl = allowForwardedSsl;
   }

   public boolean getLoginPageEnabled() { return loginPageEnabled; }

   public void setLoginPageEnabled(boolean loginPageEnabled) { this.loginPageEnabled = loginPageEnabled; }
}

