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
package com.iris.common.rule.event;

import com.iris.messages.address.Address;

/**
 * Fired *after* a new model has been added to the
 * context.
 */
public class ModelAddedEvent extends RuleEvent {
   private final Address address;
   
   public static ModelAddedEvent create(Address address) {
      return new ModelAddedEvent(address);
   }
   
   private ModelAddedEvent(Address address) {
      this.address = address;
   }

   @Override
   public RuleEventType getType() {
      return RuleEventType.MODEL_ADDED;
   }

   /**
    * The address of the model which was added.
    * @return
    */
   public Address getAddress() {
      return address;
   }

   @Override
   public String toString() {
      return "ModelAddedEvent [address=" + address + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ModelAddedEvent other = (ModelAddedEvent) obj;
      if (address == null) {
         if (other.address != null) return false;
      }
      else if (!address.equals(other.address)) return false;
      return true;
   }

}

