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
package com.iris.core.messaging;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.iris.Utils;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

/**
 *
 */
public class MessageListeners {

   public static <M> MessageListener<M> filter(Predicate<M> p, MessageListener<M> listener) {
      Utils.assertNotNull(p);
      Utils.assertNotNull(listener);
      return (message) -> {
         if(p.test(message)) {
            listener.onMessage(message);
         }
      };
   }

   public static MessageListener<PlatformMessage> filterByDestinationNamespace(String namespace, MessageListener<PlatformMessage> listener) {
      Utils.assertNotNull(namespace, "namespace may not be null");
      return filter((m) -> namespace.equals(m.getDestination().getNamespace()), listener);
   }

   public static MessageListener<PlatformMessage> filterByDestinationAddress(Address address, MessageListener<PlatformMessage> listener) {
      Utils.assertNotNull(address, "address may not be null");
      return filter((m) -> address.equals(m.getDestination()), listener);
   }

   public static MessageListener<PlatformMessage> filterByDestinationAddresses(Collection<Address> addresses, MessageListener<PlatformMessage> listener) {
      Utils.assertNotNull(addresses, "addresses may not be null");
      final Set<Address> add = new HashSet<>(addresses);
      return filter((m) -> add.contains(m.getDestination()), listener);
   }

}

