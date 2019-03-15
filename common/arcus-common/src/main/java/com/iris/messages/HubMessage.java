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
package com.iris.messages;

public class HubMessage {
   public enum Type { PLATFORM, PROTOCOL, LOG, METRICS };

   private Type type;
   private byte[] buffer;

   public static HubMessage create(Type type, byte[] payload) {
      return new HubMessage(type, payload);
   }

   public static HubMessage createPlatform(byte[] payload) {
      return new HubMessage(Type.PLATFORM, payload);
   }

   public static HubMessage createProtocol(byte[] payload) {
      return new HubMessage(Type.PROTOCOL, payload);
   }

   public static HubMessage createLog(byte[] payload) {
      return new HubMessage(Type.LOG, payload);
   }

   public static HubMessage createMetrics(byte[] payload) {
      return new HubMessage(Type.METRICS, payload);
   }

   HubMessage(Type type, byte[] buffer) {
      this.type = type;
      this.buffer = buffer;
   }

   public Type getType() {
      return type;
   }

   public byte[] getPayload() {
      return buffer;
   }
}

