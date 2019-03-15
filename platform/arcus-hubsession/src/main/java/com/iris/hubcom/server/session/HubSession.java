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
package com.iris.hubcom.server.session;

import io.netty.channel.Channel;

import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.session.DefaultSessionImpl;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.platform.partition.PlatformPartition;

/**
 *
 */
public class HubSession extends DefaultSessionImpl {

   public static enum State {
      CONNECTED, PENDING_REG_ACK, REGISTERED, AUTHORIZED;
   }

   public static enum UnauthorizedReason {
      BELOW_MIN_FW, UNREGISTERED, REGISTERING, ORPHANED, INVALID_ACCOUNT, HANDSHAKING, BANNED_CELL, UNAUTHENTICATED
   }

   private final String hubId;
   private volatile State state = State.CONNECTED;
   private volatile UnauthorizedReason unauthReason = UnauthorizedReason.HANDSHAKING;
   private volatile Date lastStateChange = new Date();
   private volatile PlatformPartition partition;
   private volatile String connType;
   private volatile String simId;
   private volatile String firmwareVersion;

   /**
    * @param channel
    * @param bridgeMetrics
    */
   public HubSession(SessionRegistry parent, Channel channel, BridgeMetrics bridgeMetrics, HubClientToken token) {
      super(parent, channel, bridgeMetrics);
      setClientType(TYPE_HUB);
      setClientToken(token);
      hubId = token.getRepresentation();
   }

   public String getHubId() {
      return hubId;
   }

   public State getState() {
      return state;
   }

   public void setState(State state) {
      this.state = state;
      this.lastStateChange = new Date();
   }

   public void setConnectionType(String connType) {
      this.connType = connType;
   }

   public String getConnectionType() {
      return connType;
   }

   public void setSimId(String simId) {
      this.simId = simId;
   }

   public String getSimId() {
      return simId;
   }

   public UnauthorizedReason getUnauthReason() {
      return unauthReason;
   }

   public void setUnauthReason(UnauthorizedReason unauthReason) {
      this.unauthReason = unauthReason;
   }

   /**
    * @return the firmwareVersion
    */
   public String getFirmwareVersion() {
      return firmwareVersion;
   }

   /**
    * @param firmwareVersion the firmwareVersion to set
    */
   public void setFirmwareVersion(String firmwareVersion) {
      this.firmwareVersion = firmwareVersion;
   }

   public Date getLastStateChange() {
      return this.lastStateChange;
   }

   @Nullable
   public PlatformPartition getPartition() {
      return partition;
   }

   public void setPartition(PlatformPartition partition) {
      this.partition = partition;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "HubSession [hub=" + hubId + ", state=" + state + ", place=" + getActivePlace() + "]";
   }

}

