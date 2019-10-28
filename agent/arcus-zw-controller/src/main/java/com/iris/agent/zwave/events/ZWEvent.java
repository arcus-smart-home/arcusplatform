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
package com.iris.agent.zwave.events;

/**
 * Interface for events internal to the ZWave subsystem
 * 
 * @author Erik Larson
 */
public interface ZWEvent {
   
   public enum ZWEventType {
      BOOTSTRAPPED,
      START_PAIRING,
      STOP_PAIRING,
      START_UNPAIRING,
      STOP_UNPAIRING,
      NODE_ADDED,
      NODE_REMOVED,
      NODE_DISCOVERED,
      NODE_ID,
      NODE_MAPPED,
      NODE_COMMAND,
      HOME_ID_CHANGED,
      PROTOCOL_VERSION,
      OFFLINE_TIMEOUT,
      HEARD_FROM,
      GONE_OFFLINE,
      GONE_ONLINE
   }
   
   ZWEventType getType();
}
