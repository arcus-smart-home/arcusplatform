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
package com.iris.capability.reader;

import org.w3c.dom.Element;

import com.iris.capability.definition.Definitions;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.definition.Definitions.ServiceDefinitionBuilder;

public class ServiceReader extends BaseDefinitionReader<ServiceDefinition, ServiceDefinitionBuilder> {
   public final static String SCHEMA_URI = "http://www.arcussmarthome.com/schema/service/1.0.0";

   public ServiceReader() {
      super(SCHEMA_URI);
   }

   @Override
   protected ServiceDefinitionBuilder builder() {
      return Definitions.serviceBuilder();
   }

   @Override
   protected void populateDefinitionSpecificData(ServiceDefinitionBuilder builder, Element element) {
      // no op
   }
}

