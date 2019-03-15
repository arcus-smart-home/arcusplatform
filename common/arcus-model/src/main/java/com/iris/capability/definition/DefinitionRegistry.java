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
package com.iris.capability.definition;

import java.util.Collection;

/**
 * 
 */
public interface DefinitionRegistry {

   /**
    * Looks up a CapabilityDefinition by name or
    * namespace.
    * @param name
    * @return
    */
   CapabilityDefinition getCapability(String nameOrNamespace);

   /**
    * An unmodifiable view of all the capabilities in the system.
    * @return
    */
   Collection<CapabilityDefinition> getCapabilities();
   
   /**
    * Looks up a ServiceDefinition by name or
    * namespace.
    * @param name
    * @return
    */
   ServiceDefinition getService(String nameOrNamespace);
   
   Collection<ServiceDefinition> getServices();
   
   TypeDefinition getStruct(String name);
   
   Collection<TypeDefinition> getStructs();
   
   EventDefinition getEvent(String name);
   
   AttributeDefinition getAttribute(String name);
}

