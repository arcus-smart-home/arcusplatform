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
package com.iris.client.impl.netty;

public interface WebsocketStateHandler {
   
   /**
    * There is an attempt to connect to
    * the server.
    */
   void onConnecting();
   
   /**
    * The connection is fully established and
    * messages may be sent.
    */
   void onConnected();
 
   /**
    * The connection has been lost but a re-connect
    * will be attempted.
    */
   void onDisconnected();
   
   /**
    * The socket has been closed and no attempt
    * will be made to autonomously reconnect.
    * @param status
    */
   void onClosed(CloseCause cause);
   
   /**
    * An error occured on the socket, this will generally
    * be followed by onDisconnected() or onClosed(...)
    * @param cause
    */
   void onException(Throwable cause);
   
   public enum CloseCause {
      /**
       * The socket was closed because
       * {@link Client#disconnect()} was called.  
       */
      REQUESTED,
      /**
       * The socket was closed because the
       * session is no longer authenticated.
       * Won't retry until login is invoked again.
       */
      SESSION_EXPIRED,
      /**
       * The socket is closed because too many
       * retries failed.
       */
      RETRIES_EXCEEDED
   }
}

