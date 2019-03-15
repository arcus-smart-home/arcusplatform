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
package com.iris.device.attributes;


/**
 *
 */
public class AttributeValue<T> {
   private final AttributeKey<T> key;
   private final T value;
   
   public AttributeValue(
         AttributeKey<T> key,
         T value
   ) {
      this.key = key;
      this.value = value;
   }
   
	public AttributeKey<T> getKey() { 
	   return key; 
	}
	
	public T getValue() { 
	   return value;
	}

   @Override
   public String toString() {
      return "AttributeValue [key=" + key + ", value=" + value + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AttributeValue other = (AttributeValue) obj;
      if (key == null) {
         if (other.key != null) return false;
      }
      else if (!key.equals(other.key)) return false;
      if (value == null) {
         if (other.value != null) return false;
      }
      else if (!value.equals(other.value)) return false;
      return true;
   }
	
}

