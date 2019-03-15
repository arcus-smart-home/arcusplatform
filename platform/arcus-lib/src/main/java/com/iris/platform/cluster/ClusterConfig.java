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
package com.iris.platform.cluster;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class ClusterConfig {
   
   @Inject(optional = true) @Named("cluster.heartbeatIntervalSec")
   private long heartbeatIntervalSec = 5;
   @Inject(optional = true) @Named("cluster.registrationRetryDelaySec")
   private long registrationRetryDelaySec = 1;
   @Inject(optional = true) @Named("cluster.registrationRetries")
   private int registrationRetries = -1;
   @Inject(optional = true) @Named("cluster.exitOnDeregistered")
   private boolean exitOnDeregistered = true;
   @Inject(optional = true) @Named("cluster.heartbeatTimeoutSec")
   private long timeoutMs = TimeUnit.SECONDS.toMillis(20);
   
   public ClusterConfig() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the heartbeatIntervalSec
    */
   public long getHeartbeatIntervalSec() {
      return heartbeatIntervalSec;
   }
   
   public long getHeartbeatInterval(TimeUnit unit) {
      return unit.convert(heartbeatIntervalSec, TimeUnit.SECONDS);
   }

   /**
    * @param heartbeatIntervalSec the heartbeatIntervalSec to set
    */
   public void setHeartbeatIntervalSec(long heartbeatIntervalSec) {
      this.heartbeatIntervalSec = heartbeatIntervalSec;
   }

   /**
    * @return the registrationRetryDelaySec
    */
   public long getRegistrationRetryDelaySec() {
      return registrationRetryDelaySec;
   }

   public long getRegistrationRetryDelay(TimeUnit unit) {
      return unit.convert(registrationRetryDelaySec, TimeUnit.SECONDS);
   }
   
   public long getTimeoutMs() {
      return timeoutMs;
   }

   /**
    * @param timeoutMs the timeoutMs to set as used in CassandraCluserService
    */
   public void setTimeoutMs(long timeoutMs) {
      this.timeoutMs = timeoutMs;
   }

   /**
    * @param registrationRetryDelaySec the registrationRetryDelaySec to set
    */
   public void setRegistrationRetryDelaySec(long registrationRetryDelaySec) {
      this.registrationRetryDelaySec = registrationRetryDelaySec;
   }

   /**
    * @return the retries
    */
   public int getRegistrationRetries() {
      return registrationRetries;
   }

   /**
    * @param retries the retries to set
    */
   public void setRegistrationRetries(int retries) {
      this.registrationRetries = retries;
   }

   /**
    * @return the exitOnDeregistered
    */
   public boolean isExitOnDeregistered() {
      return exitOnDeregistered;
   }

   /**
    * @param exitOnDeregistered the exitOnDeregistered to set
    */
   public void setExitOnDeregistered(boolean exitOnDeregistered) {
      this.exitOnDeregistered = exitOnDeregistered;
   }

}

