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
package com.iris.common.subsystem.event;

import java.util.UUID;

import com.iris.messages.address.Address;
import com.iris.messages.event.AddressableEvent;

/**
 * 
 */
public abstract class SubsystemLifecycleEvent extends AddressableEvent {
   
   public static SubsystemAddedEvent added(Address address) {
      return new SubsystemAddedEvent(address);
   }
   
   public static SubsystemActivatedEvent activated(Address address) {
      return new SubsystemActivatedEvent(address);
   }
   
   public static SubsystemStartedEvent started(Address address) {
      return new SubsystemStartedEvent(address);
   }
   
   public static SubsystemStoppedEvent stopped(Address address) {
      return new SubsystemStoppedEvent(address);
   }
   
   public static SubsystemDeactivatedEvent deactivated(Address address) {
      return new SubsystemDeactivatedEvent(address);
   }
   
   public static SubsystemRemovedEvent removed(Address address) {
      return new SubsystemRemovedEvent(address);
   }
   
   private final Address address;
   
   /**
    * 
    */
   SubsystemLifecycleEvent(Address address) {
      this.address = address;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.event.AddressableEvent#getAddress()
    */
   @Override
   public Address getAddress() {
      return address;
   }

   public UUID getPlaceId() {
      return (UUID) address.getId();
   }
   
   public String getSubsystemType() {
      return (String) address.getGroup();
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return getClass().getSimpleName() + " [address=" + address + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SubsystemLifecycleEvent other = (SubsystemLifecycleEvent) obj;
      if (address == null) {
         if (other.address != null) return false;
      }
      else if (!address.equals(other.address)) return false;
      return true;
   }

}

