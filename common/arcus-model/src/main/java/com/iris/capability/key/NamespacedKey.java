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
package com.iris.capability.key;

import org.eclipse.jdt.annotation.Nullable;


public class NamespacedKey {
   public static final String SEPARATOR = ":";

   public static NamespacedKey parse(String key) {
      if(key == null || key.isEmpty()) {
         throw new IllegalArgumentException("key may not be empty");
      }
      String [] parts = key.split("\\:", 3);
      switch(parts.length) {
      case 1:
         return namespaced(parts[0]);
      case 2:
         return named(parts[0], parts[1]);
      case 3:
         return instanced(parts[0], parts[1], parts[2]);
      default:
         throw new IllegalArgumentException("Invalid attribute name [" + key + "]");
      }
   }
   
   public static NamespacedKey namespaced(String namespace) {
      return new NamespacedKey(namespace, namespace);
   }
   
   public static NamedKey named(String namespace, String name) {
      return new NamedKey(representation(namespace, name), namespace, name);
   }
   
   public static InstancedKey instanced(String namespace, String name, String instance) {
      return new InstancedKey(representation(namespace, name, instance), namespace, name, instance);
   }
   
   /**
    * Provided for consistency, same as {@link #namespaced(String)}.
    * @param namespace
    * @return
    */
   public static NamespacedKey of(String namespace) {
      return namespaced(namespace);
   }
   
   /**
    * Allows a {@link NamespacedKey} to be built when some
    * of the parts may be {@code null}. The appropriate type will
    * be returned based on which parts are non-null.
    * @param namespace
    * @param name
    * @return
    */
   public static NamespacedKey of(
         String namespace, 
         @Nullable String name
   ) {
      return of(namespace, name, null);
   }
   
   /**
    * Allows a {@link NamespacedKey} to be built when some
    * of the parts may be {@code null}. The appropriate type will
    * be returned based on which parts are non-null.
    * @param namespace
    * @param name
    * @param instance
    * @return
    */
   public static NamespacedKey of(
         String namespace, 
         @Nullable String name, 
         @Nullable String instance
   ) {
      if(instance != null) {
         return instanced(namespace, name, instance);
      }
      if(name != null) {
         return named(namespace, name);
      }
      return namespaced(namespace);
   }
   
   // TODO verify no ':' in values
   public static String representation(String namespace, String name) {
      return namespace + SEPARATOR + name;
   }
   
   public static String representation(String namespace, String name, String instance) {
      return namespace + SEPARATOR + name + SEPARATOR + instance;
   }
   
   private final String representation;
   private final String namespace;
   
   NamespacedKey(String representation, String namespace) {
      if(representation == null) {
         throw new IllegalArgumentException("namespace may not be null");
      }
      if(namespace == null) {
         throw new IllegalArgumentException("namespace may not be null");
      }
      this.representation = representation;
      this.namespace = namespace;
   }

   public String getNamespace() {
      return namespace;
   }
   
   public String getNamedRepresentation() {
      throw new IllegalArgumentException("Must be a NamedKey or InstanceKey, '" + representation + "' is a namespace.");
   }

   public NamespacedKey toNamespace() {
      return this;
   }
   
   public NamedKey toName(String name) {
      return named(namespace, name);
   }

   public InstancedKey toInstance(String name, String instance) {
      return instanced(namespace, name, instance);
   }

   public boolean isNamed()      { return false; }
   
   public boolean isInstanced()  { return false; }
   
   public String getRepresentation() { return representation; }
   
   public @Nullable String getName()     { return null; }

   public @Nullable String getInstance() { return null; }
   
   @Override
   public String toString() {
      return getClass().getSimpleName() + " ["  + representation + "]";
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((representation == null) ? 0 : representation.hashCode());
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
      NamespacedKey other = (NamespacedKey) obj;
      if (representation == null) {
         if (other.representation != null) return false;
      }
      else if (!representation.equals(other.representation)) return false;
      return true;
   }

}

