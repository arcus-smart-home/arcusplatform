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
package com.iris.bridge.server.http.health;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class HealthCheckServerConfig {
   public static final String NAME_HEALTHCHECK_RESOURCES = "HealthCheckResources";
   
   @Inject(optional = false) @Named("healthcheck.http.port")
   private int port = 9080;
   
   @Inject(optional = true) @Named("healthcheck.http.maxRequestSizeBytes")
   private int maxRequestSizeBytes = 1024;

   /**
    * @return the maxRequestSizeBytes
    */
   public int getMaxRequestSizeBytes() {
      return maxRequestSizeBytes;
   }

   /**
    * @param maxRequestSizeBytes the maxRequestSizeBytes to set
    */
   public void setMaxRequestSizeBytes(int maxRequestSizeBytes) {
      this.maxRequestSizeBytes = maxRequestSizeBytes;
   }

   /**
    * @return the port
    */
   public int getPort() {
      return port;
   }

   /**
    * @param port the port to set
    */
   public void setPort(int port) {
      this.port = port;
   }
   
}

