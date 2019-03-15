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
package com.iris.bridge.server.netty;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

/**
 * 
 */
public class InstrumentedSSLEngine extends SSLEngine {
   
   private final SSLEngine engine;
   private final SslMetrics metrics;
   
   InstrumentedSSLEngine(SSLEngine engine, SslMetrics metrics) {
      this.engine = engine;
      this.metrics = metrics;
   }

   @Override
   public SSLEngineResult wrap(
         ByteBuffer[] srcs, int offset, int length, ByteBuffer dst)
         throws SSLException {
      long startTimeNs = metrics.startTime();
      try {
         return engine.wrap(srcs, offset, length, dst);
      }
      finally {
         metrics.onEncodeComplete(startTimeNs);
      }
   }

   @Override
   public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length)  throws SSLException {
      long startTimeNs = metrics.startTime();
      try {
         return engine.unwrap(src, dsts, offset, length);
      }
      finally {
         metrics.onDecodeComplete(startTimeNs);
      }
   }

   @Override
   public void setWantClientAuth(boolean want) {
      engine.setWantClientAuth(want);
   }
   
   @Override
   public void setUseClientMode(boolean mode) {
      engine.setUseClientMode(mode);
   }
   
   @Override
   public void setNeedClientAuth(boolean need) {
      engine.setNeedClientAuth(need);
   }
   
   @Override
   public void setEnabledProtocols(String[] protocols) {
      engine.setEnabledProtocols(protocols);
   }
   
   @Override
   public void setEnabledCipherSuites(String[] suites) {
      engine.setEnabledCipherSuites(suites);
   }
   
   @Override
   public void setEnableSessionCreation(boolean flag) {
      engine.setEnableSessionCreation(flag);
   }
   
   @Override
   public boolean isOutboundDone() {
      return engine.isOutboundDone();
   }
   
   @Override
   public boolean isInboundDone() {
      return engine.isInboundDone();
   }
   
   @Override
   public boolean getWantClientAuth() {
      return engine.getWantClientAuth();
   }
   
   @Override
   public boolean getUseClientMode() {
      return engine.getUseClientMode();
   }
   
   @Override
   public String[] getSupportedProtocols() {
      return engine.getSupportedProtocols();
   }
   
   @Override
   public String[] getSupportedCipherSuites() {
      return engine.getSupportedCipherSuites();
   }
   
   @Override
   public SSLSession getSession() {
      return engine.getSession();
   }
   
   @Override
   public boolean getNeedClientAuth() {
      return engine.getNeedClientAuth();
   }
   
   @Override
   public HandshakeStatus getHandshakeStatus() {
      return engine.getHandshakeStatus();
   }
   
   @Override
   public String[] getEnabledProtocols() {
      return engine.getEnabledProtocols();
   }
   
   @Override
   public String[] getEnabledCipherSuites() {
      return engine.getEnabledCipherSuites();
   }
   
   @Override
   public boolean getEnableSessionCreation() {
      return engine.getEnableSessionCreation();
   }
   
   @Override
   public Runnable getDelegatedTask() {
      return engine.getDelegatedTask();
   }
   
   @Override
   public void closeOutbound() {
      engine.closeOutbound();
   }
   
   @Override
   public void closeInbound() throws SSLException {
      engine.closeInbound();
   }
   
   @Override
   public void beginHandshake() throws SSLException {
      engine.beginHandshake();
   }

   @Override
   public SSLSession getHandshakeSession() {
      return engine.getHandshakeSession();
   }

   @Override
   public String getPeerHost() {
      return engine.getPeerHost();
   }

   @Override
   public int getPeerPort() {
      return engine.getPeerPort();
   }

   @Override
   public SSLParameters getSSLParameters() {
      return engine.getSSLParameters();
   }

   @Override
   public void setSSLParameters(SSLParameters params) {
      engine.setSSLParameters(params);
   }
}

