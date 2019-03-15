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
package com.iris.capability.builder;

import com.iris.device.model.CapabilityDefinition;

/**
 *
 */
public class Builders {

   public static CommandDefinitionBuilder newCommandBuilder(String namespace, String command) {
      return new CommandDefinitionBuilder(namespace, command);
   }
   
   public static CapabilityBuilder newCapabilityBuilder(CapabilityDefinition definition) {
      return new CapabilityBuilder(definition);
   }

   public static ReflectiveCapabilityBuilder newReflectiveCapabilityBuilder(CapabilityDefinition definition) {
      return new ReflectiveCapabilityBuilder(definition);
   }

}

