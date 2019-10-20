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
package com.iris.agent.zwave.client;

import com.iris.agent.zwave.ZWMsg;

/**
 * Client for sending messages to the ZWave gateway for a particular
 * node. If the client uses resources, it should go inactive and release
 * resources after there has been no traffic with the node for awhile.
 * 
 * @author Erik Larson
 */
public interface ZWClient {

   /**
    * Sends a message to this client's node. If the client is
    * inactive, then the client will be spun up.
    * 
    * @param msg Message to send.
    */
   void send(ZWMsg msg);
   
   /**
    * Starts the client. Generally, this also starts a new thread.
    */
   void start();
   
   /**
    * Shuts down the client and releases resources. Generally, this
    * should not be called since the client should handle going
    * inactive by itself.
    */
   void shutdown();
   
   /**
    * Indicates that the client is currently active.
    * 
    * @return true if the client is active.
    */
   boolean isRunning();
   
}
