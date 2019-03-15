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
package com.iris.kafka.util;

import java.util.Map;

/**
 * 
 */
public class ImmutableMapEntry<K, V> implements Map.Entry<K, V> {
   private final K key;
   private final V value;
   
   /**
    * 
    */
   public ImmutableMapEntry(K key, V value) {
      this.key = key;
      this.value = value;
   }

   /**
    * @return the key
    */
   @Override
   public K getKey() {
      return key;
   }

   /**
    * @return the value
    */
   @Override
   public V getValue() {
      return value;
   }

   @Override
   public V setValue(V value) {
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ImmutableMapEntry [key=" + key + ", value=" + value + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
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
      ImmutableMapEntry other = (ImmutableMapEntry) obj;
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

