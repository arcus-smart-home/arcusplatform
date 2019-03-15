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
package com.iris.agent.reflex;

import java.util.Map;

import com.iris.messages.PlatformMessage;
import com.iris.model.Version;
import com.iris.protocol.ProtocolMessage;

public interface ReflexProcessor extends ReflexDevice {
   public static enum State { INITIAL, ADDED, CONNECTED, DISCONNECTED, REMOVED }

   String getDriver();
   Version getVersion();
   String getHash();

   State getCurrentState();
   void setCurrentState(State newState);
   boolean isDegraded();

   boolean handle(PlatformMessage msg);
   boolean handle(ProtocolMessage msg);

   Map<String,Object> getState();
   Map<String,Object> getSyncState();
}

