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
package com.iris.messages.event;

import com.iris.messages.address.Address;

/**
 * Fired after an attribute value is changed. The model
 * in the context will have the same value for the
 * attribute as {@link #getAttributeValue()}.  
 * 
 * Note that if a base:ValueChange event is received
 * with a value that is equal to the previous value, no
 * AttributeValueChangedEvent will be fired.
 */
public class ModelChangedEvent extends ModelEvent {
   private final Address address;
   private final String attributeName;
   private final Object attributeValue;
   private final Object oldValue;
   
   public static ModelChangedEvent create(
         Address address,
         String attributeName,
         Object attributeValue,
         Object oldValue
   ) {
      return new ModelChangedEvent(address, attributeName, attributeValue, oldValue);
   }
   
   private ModelChangedEvent(
         Address address,
         String attributeName,
         Object attributeValue,
         Object oldValue
   ) {
      this.address = address;
      this.attributeName = attributeName;
      this.attributeValue = attributeValue;
      this.oldValue = oldValue;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   public String getAttributeName() {
      return attributeName;
   }

   public Object getAttributeValue() {
      return attributeValue;
   }

   public Object getOldValue() {
      return oldValue;
   }

   @Override
   public String toString() {
      return "ValueChangeEvent [address=" + address + ", attributeName="
            + attributeName + ", attributeValue=" + attributeValue
            + ", oldValue=" + oldValue + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      result = prime * result
            + ((attributeName == null) ? 0 : attributeName.hashCode());
      result = prime * result
            + ((attributeValue == null) ? 0 : attributeValue.hashCode());
      result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ModelChangedEvent other = (ModelChangedEvent) obj;
      if (address == null) {
         if (other.address != null) return false;
      }
      else if (!address.equals(other.address)) return false;
      if (attributeName == null) {
         if (other.attributeName != null) return false;
      }
      else if (!attributeName.equals(other.attributeName)) return false;
      if (attributeValue == null) {
         if (other.attributeValue != null) return false;
      }
      else if (!attributeValue.equals(other.attributeValue)) return false;
      if (oldValue == null) {
         if (other.oldValue != null) return false;
      }
      else if (!oldValue.equals(other.oldValue)) return false;
      return true;
   }

}

