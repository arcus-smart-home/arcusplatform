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
package com.iris.platform.subsystem.pairing;

/**
 * What the pairing subsystem remembers about a hub waiting for an S2 PIN. This
 * is saved with the subsystem, so it must never hold the PIN or the DSK.
 */
public class S2PinRequest {
   private final String requestId;
   private final int node;
   private final String hub;
   private final long expires;
   private final boolean answered;

   public S2PinRequest(String requestId, int node, String hub, long expires, boolean answered) {
      this.requestId = requestId;
      this.node = node;
      this.hub = hub;
      this.expires = expires;
      this.answered = answered;
   }

   public String getRequestId() {
      return requestId;
   }

   public int getNode() {
      return node;
   }

   /** The address of the hub that is waiting. */
   public String getHub() {
      return hub;
   }

   /** When answers stop being accepted, in milliseconds since the epoch. */
   public long getExpires() {
      return expires;
   }

   /** True once a PIN has gone to the hub, which uses up the request. */
   public boolean isAnswered() {
      return answered;
   }

   public S2PinRequest toAnswered() {
      return new S2PinRequest(requestId, node, hub, expires, true);
   }

   @Override
   public String toString() {
      return "S2PinRequest [requestId=" + requestId + ", node=" + node + ", hub=" + hub
            + ", expires=" + expires + ", answered=" + answered + "]";
   }
}
