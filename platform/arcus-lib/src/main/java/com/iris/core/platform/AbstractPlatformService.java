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
package com.iris.core.platform;

import java.util.concurrent.Executor;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

/**
 *
 */
// TODO deprecate PlatformServiceDispatcher and just register here...
public abstract class AbstractPlatformService extends AbstractPlatformMessageListener implements PlatformService {
   private final Address address;

   protected AbstractPlatformService(PlatformMessageBus platformBus, String name) { 
      super(platformBus);
      this.address = Address.platformService(name);
   }

   protected AbstractPlatformService(PlatformMessageBus platformBus, Address address) { 
      super(platformBus);
      this.address = address;
   }

   protected AbstractPlatformService(PlatformMessageBus platformBus, String name, Executor executor) {
      super(platformBus, executor);
      this.address = Address.platformService(name); 
   }
   
   protected AbstractPlatformService(PlatformMessageBus platformBus, Address address, Executor executor) {
      super(platformBus, executor);
      this.address = address;
   }

   public AbstractPlatformService(PlatformMessageBus platformBus, String name, int maxThreads, long threadTimeoutMs) {
      this(platformBus, Address.platformService(name), maxThreads, threadTimeoutMs);
   }

   public AbstractPlatformService(PlatformMessageBus platformBus, Address address, int maxThreads, long threadTimeoutMs) {
      super(platformBus, address.getGroup() + "-service", maxThreads, threadTimeoutMs);
      this.address = address;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.PlatformService#getAddress()
    */
   @Override
   public Address getAddress() {
      return address;
   }
   
   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#onMessage(com.iris.messages.PlatformMessage)
    */
   @Override
   public void onMessage(PlatformMessage message) {
      executor().execute(() -> doHandleMessage(message));
   }

   // HACK PlatformService and AbstractPlatformMessageListener names collide here, force it onto the proper
   //      thread and don't allow anyone to override the wrong method and unintentionally break the threading model
   @Override
   public final void handleMessage(PlatformMessage message) {
      executor().execute(() -> doHandleMessage(message));
   }

   protected void doHandleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

}

