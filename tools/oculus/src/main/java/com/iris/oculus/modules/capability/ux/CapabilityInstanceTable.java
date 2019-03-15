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
package com.iris.oculus.modules.capability.ux;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.CapabilityDefinition;

/**
 * 
 */
public class CapabilityInstanceTable extends CapabilityTable {
   private String instanceId;
   
   /**
    * 
    */
   public CapabilityInstanceTable() {
   }

   /**
    * @param definition
    */
   public CapabilityInstanceTable(CapabilityDefinition definition, String instanceId) {
      super(definition);
      this.instanceId = instanceId;
   }

   /**
    * @return the instanceId
    */
   public String getInstanceId() {
      return instanceId;
   }

   /**
    * @param instanceId the instanceId to set
    */
   public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.capability.ux.CapabilityTable#getAttributeName(com.iris.capability.definition.AttributeDefinition)
    */
   @Override
   protected String getAttributeName(AttributeDefinition definition) {
      return definition.getName() + ":" + instanceId;
   }

}

