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

import java.util.Set;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.messages.address.AddressMatcher;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.Subscription;

/**
 *
 */
public interface MessageBus<M> {

   public ListenableFuture<Void> send(M message);

   public ListenableFuture<Void> send(PlatformPartition partition, M message);
   
   public Subscription addMessageListener(Set<AddressMatcher> matcher, MessageListener<M> listener);
   
   /**
    * Add listener that listens on broadcast messages based on the message's source field.
    * @param matcher
    * @param listener
    * @return
    */
   public Subscription addBroadcastMessageListener(Set<AddressMatcher> matcher, MessageListener<M> listener);
}

