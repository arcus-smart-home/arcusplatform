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
package com.iris.platform.rule.environment;

import java.util.concurrent.Callable;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.common.rule.event.RuleEvent;
import com.iris.messages.PlatformMessage;

/**
 *
 */
// TODO move this down to arcus-rules?
public interface PlaceEnvironmentExecutor {

   // TODO checks for active
   
   RuleModelStore getModelStore();

   void start();
   
   ListenableFuture<Void> submit(Runnable task);
   
   <V> ListenableFuture<V> submit(Callable<V> task);

   void handleRequest(PlatformMessage message);
   
   void onMessageReceived(PlatformMessage message);
   
   void fire(RuleEvent event);

   void stop();

   PlaceEnvironmentStatistics getStatistics();
}

