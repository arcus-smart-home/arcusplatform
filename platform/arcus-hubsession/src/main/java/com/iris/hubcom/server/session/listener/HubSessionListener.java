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
package com.iris.hubcom.server.session.listener;

import com.iris.hubcom.server.session.HubSession;

// TODO combine this with SessionListener?
public interface HubSessionListener {

   /**
    * Invoked when the socket has first been established.
    * @param session
    */
   void onConnected(HubSession session);
   
   /**
    * Invoked when the platform has attempted to register the hub.
    * @param session
    */
   void onRegisterRequested(HubSession session);
   
   /**
    * Invoked when the hub acknowledges the platform registration.
    * @param session
    */
   void onRegistered(HubSession session);
   
   /**
    * Invoked when the hub is authorized to start sending messages.
    * @param session
    */
   void onAuthorized(HubSession session);

   /**
    * Invoked when the hub disconnects from the system.
    * @param session
    */
   void onDisconnected(HubSession session);

}

