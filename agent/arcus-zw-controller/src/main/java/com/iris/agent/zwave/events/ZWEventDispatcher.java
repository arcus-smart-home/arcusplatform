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
package com.iris.agent.zwave.events;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages listeners for ZW event listeners. These events are internal 
 * to the controller.
 * 
 * @author Erik Larson
 */
public class ZWEventDispatcher {

   public static final ZWEventDispatcher INSTANCE = new ZWEventDispatcher();
   
   private ZWEventDispatcher() {};
   
   private final Set<ZWEventListener> listeners = new CopyOnWriteArraySet<ZWEventListener>();
   
   /**
    * Dispatches an internal ZW controller event to all listeners.
    * 
    * @param event internal ZW controller event
    */
   //TODO: Queue events and process in separate thread.
   public void dispatch(final ZWEvent event) {      
      listeners.forEach(l -> l.onZWEvent(event));
   }
   
   /**
    * Registers a listener for ZW controller event
    * 
    * @param listener listener for ZW controller event to be added
    */
   public void register(final ZWEventListener listener) {
      listeners.add(listener);
   }
   
   /**
    * Removes a listener for ZW controller event
    * 
    * @param listener listener for ZW controller event to be removed
    */
   public void unregister(final ZWEventListener listener) {
      listeners.remove(listener);
   }
   
}
