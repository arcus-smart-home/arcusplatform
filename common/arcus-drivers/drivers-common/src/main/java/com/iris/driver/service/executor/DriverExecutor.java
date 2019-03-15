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
package com.iris.driver.service.executor;

import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.messages.model.DriverId;

// TODO extract base interface for ContextualExecutor
public interface DriverExecutor {

   /**
    * Attempts to "fire" the event, if the current thread is the
    * executor thread, then this will execute in the current thread,
    * otherwise it will be queued for dispatch.
    * @param event
    * @return
    */
   ListenableFuture<Void> fire(Object event);
   
   /**
    * Adds the event to the execution queue, this will never run immediately.
    * It is not safe to block on the result in the execution thread.
    * @param event
    * @return
    */
   ListenableFuture<Void> defer(Object event);
   
   /**
    * Adds the event to the execution queue at roughly timestamp, 
    * this will never run immediately.
    * It is not safe to block on the result in the execution thread.
    * @param event
    * @return
    */
   ListenableFuture<Void> defer(Object event, Date timestamp);
   
   /**
    * Adds the event to the execution queue that can be cancelled by referencing 
    * the key.
    * @see #cancel(String)
    * @param key
    * @param event
    * @param timestamp
    */
   ListenableFuture<Void> defer(String key, Object event, Date timestamp);
   
   /**
    * Cancels a DeferredEvent by the key.
    * @param key
    * @return
    */
   boolean cancel(String key);
   
   /**
    * Gets a reference to the current {@link DeviceDriver}.
    * @return
    */
   DeviceDriver driver();
   
   /**
    * Gets a reference to the current {@link DeviceDriverContext}.
    * @return
    */
   DeviceDriverContext context();
   
   /**
    * Retrieves the thread the executor is running on.  Note that 
    * when a driver is not processing any events it is "idle" and
    * will not be associate with any thread.
    * @return
    */
   @Nullable Thread getExecutorThread();

   /**
    * Called when the driver is loaded
    */
   void start();
   
   /**
    * Called when a new driver is loaded
    * @param previous
    */
   void upgraded(DriverId previous);
   
   /**
    * Called before the driver is removed from memory
    */
   void stop();
}

