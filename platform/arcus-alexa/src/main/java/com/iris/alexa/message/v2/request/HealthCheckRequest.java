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
package com.iris.alexa.message.v2.request;


public class HealthCheckRequest implements RequestPayload {

   private String initiationTimestamp;

   public String getInitiationTimestamp() {
      return initiationTimestamp;
   }

   public void setInitiationTimestamp(String initiationTimestamp) {
      this.initiationTimestamp = initiationTimestamp;
   }

   @Override
   public String getNamespace() {
      return "Alexa.ConnectedHome.System";
   }

   @Override
   public String toString() {
      return "HealthCheckRequest [initiationTimestamp=" + initiationTimestamp
            + ']';
   }

}

