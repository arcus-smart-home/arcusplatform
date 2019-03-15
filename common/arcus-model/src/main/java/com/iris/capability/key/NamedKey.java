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



public class NamedKey extends NamespacedKey {
   private final String name;
   
   NamedKey(String representation, String namespace, String name) {
      super(representation, namespace);
      this.name = name;
   }

   /* (non-Javadoc)
    * @see com.iris.capability.util.Namespace#getName()
    */
   @Override
   public String getName() {
      return name;
   }

   /* (non-Javadoc)
    * @see com.iris.capability.util.Key#isName()
    */
   @Override
   public boolean isNamed() {
      return true;
   }

   public String getNamedRepresentation() {
      return getRepresentation();
   }
   
   public InstancedKey toInstance(String instance) {
      return instanced(getNamespace(), getName(), instance);
   }

}

