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
package com.iris.driver.groovy.context;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.driver.groovy.binding.EnvironmentBinding;

/**
 * 
 */
public class GroovyInstancedAttributeDefinition extends GroovyAttributeDefinition {
   private final String instanceId;
   private final AttributeKey<?> key;
   
   /**
    * @param delegate
    * @param binding
    */
   public GroovyInstancedAttributeDefinition(
         AttributeDefinition delegate,
         String instanceId,
         EnvironmentBinding binding
   ) {
      super(delegate, binding);
      this.instanceId = instanceId;
      this.key = AttributeKey.createType(delegate.getName() + ":" + instanceId, delegate.getType());
   }

   /* (non-Javadoc)
    * @see com.iris.driver.groovy.context.GroovyAttributeDefinition#getKey()
    */
   @Override
   public AttributeKey<?> getKey() {
      return key;
   }

   /* (non-Javadoc)
    * @see com.iris.driver.groovy.context.GroovyAttributeDefinition#getName()
    */
   @Override
   public String getName() {
      return key.getName();
   }

   @Override
   public boolean isCase(Object o) {
      if(o == null) return false;

      String name;
      if(o instanceof String) {
         name = (String) o;
      }
      else if(o instanceof AttributeKey) {
         name = ((AttributeKey<?>) o).getName();
      }
      else {
         return false;
      }
      
      return key.getName().equals(name);
   }

}

