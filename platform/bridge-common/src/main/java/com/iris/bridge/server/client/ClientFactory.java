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
/**
 * 
 */
package com.iris.bridge.server.client;

import io.netty.channel.Channel;

/**
 * 
 */
public interface ClientFactory {

   /**
    * Creates a new, unauthenticated client.
    * 
    * @return
    */
   Client create();
   
   /**
    * Loads the given client.
    * @param clientId
    * @return
    */
   Client load(String clientId);
   
   default public Client get(Channel channel) {
      Client client = channel.attr(Client.ATTR_CLIENT).get();
      if(client == null) {
         client = create();
         Client other = channel.attr(Client.ATTR_CLIENT).setIfAbsent(client);
         if (other != null) {
            client = other;
         }
      }
      return client;
   }
   
}

