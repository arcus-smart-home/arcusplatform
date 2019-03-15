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
package com.iris.platform.tag;

import static com.iris.messages.address.AddressMatchers.BROADCAST_MESSAGE_MATCHER;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.core.platform.PlatformDispatcherFactory;
import com.iris.core.platform.PlatformDispatcherFactory.DispatcherBuilder;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;

public class TagServiceListener extends AbstractPlatformMessageListener
{
   public static final String NAME_EXECUTOR_POOL = "threadpool.tag";
   public static final String PROP_HANDLERS = "service.tag.handlers";

   private MessageListener<PlatformMessage> dispatcher;

   @Inject
   public TagServiceListener(
      AnalyticsMessageBus analyticsBus,
      @Named(NAME_EXECUTOR_POOL) ExecutorService executor,
      PlatformDispatcherFactory dispatcherFactory,
      @Named(PROP_HANDLERS) Set<Object> handlers)
   {
      super(analyticsBus, executor);

      DispatcherBuilder dispatcherBuilder = dispatcherFactory.buildDispatcher();

      for (Object handler : handlers)
      {
         dispatcherBuilder.addAnnotatedHandler(handler);
      }

      this.dispatcher = dispatcherBuilder.build();
   }

   @Override
   protected void onStart()
   {
      super.onStart();

      addListeners(BROADCAST_MESSAGE_MATCHER);
   }

   @Override
   protected void handleMessage(PlatformMessage message)
   {
      dispatcher.onMessage(message);
   }

   @Override
   protected boolean isError(PlatformMessage message) {
      return message.isError();
   }

   @Override
   protected boolean isRequest(PlatformMessage message) {
      return message.isRequest();
   }

   @Override
   protected MessageBody getMessageBody(PlatformMessage message) {
      return message.getValue();
   }
}

