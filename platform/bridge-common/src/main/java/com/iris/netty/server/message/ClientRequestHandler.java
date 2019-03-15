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
package com.iris.netty.server.message;

import com.iris.bridge.server.session.Session;
import com.iris.messages.ClientMessage;

public interface ClientRequestHandler
{
   String getRequestType();

   /**
    * Submits the request to be executed at some point in the future.
    * 
    * Similarly to Executor implementations, may choose to execute immediately or run in a background thread pool.
    */
   void submit(ClientMessage request, Session session);

   /**
    * Handles the request immediately.
    * 
    * Implementations are strongly encouraged to delegate to this method from {@code submit()}.
    */
   ClientMessage handle(ClientMessage request, Session session);
}

