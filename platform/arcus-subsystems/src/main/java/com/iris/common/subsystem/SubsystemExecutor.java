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
package com.iris.common.subsystem;

import com.iris.common.subsystem.event.SubsystemResponseEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.context.PlaceContext;
import com.iris.messages.event.ScheduledEvent;

/**
 * Manages the subsystems associated with a place.
 */
public interface SubsystemExecutor {
   
   PlaceContext context();
   
   void start();
   
   void stop();
   
   void delete();
   
   void onPlatformMessage(PlatformMessage message);
   
   void onScheduledEvent(ScheduledEvent event);
   
   void onSubystemResponse(SubsystemResponseEvent event);
   
   void add(Subsystem<?> subsystem);
   
   Subsystem<?> get(Address address);
   
   SubsystemContext<?> getContext(Address address);
   
   void activate(Address address);
   
   void deactivate(Address address);
   
   void delete(Address address);

}

