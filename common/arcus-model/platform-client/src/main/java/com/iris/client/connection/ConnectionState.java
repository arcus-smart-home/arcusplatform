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
package com.iris.client.connection;

public enum ConnectionState {
   /**
    * The connection is closed and no attempt will
    * be made to re-connect.
    * This state may be entered because close() was
    * requested or because the session has expired and
    * new authentication is required before the connection
    * may be re-established.
    */
   CLOSED,
   /**
    * The connection is currently closed, but after
    * some period of time a re-connect will be attempted.
    */
   DISCONNECTED,
   /**
    * Attempting to connect but the websocket handshake
    * has not yet completed.
    */
   CONNECTING,
   /**
    * The socket is fully connected.
    */
   CONNECTED
}

