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
package com.iris.client.event;

/**
 * Return value for things that can have listeners
 * added to them.
 */
public interface ListenerRegistration {

   /**
    * When {@code true} the listener is a candidate
    * to receive new events.
    * @return
    */
   public boolean isRegistered();
    
   /**
    * Removes the listener, it will not receive any new
    * events but may still receive pending events (events
    * which have been generated but haven't been fully
    * dispatched).
    * @return
    *    {@code true} if the listener was removed because of
    *    this call. {@code false} if the listener was not registered.
    */
   public boolean remove();
}

