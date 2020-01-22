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

import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.Definitions;
import com.iris.capability.definition.Definitions.CapabilityDefinitionBuilder;

public class CapabilityReader extends BaseDefinitionReader<CapabilityDefinition, CapabilityDefinitionBuilder> {
   public final static String SCHEMA_URI = "http://www.arcussmarthome.com/schema/capability/1.0.0";

   public CapabilityReader() {
      super(SCHEMA_URI);
   }

   @Override
   protected CapabilityDefinitionBuilder builder() {
      return Definitions.capabilityBuilder();
   }

   @Override
   protected void populateDefinitionSpecificData(CapabilityDefinitionBuilder builder, Element element) {
      builder.enhances(element.getAttribute("enhances"));
      builder.withAttributes(
            buildAttributes(element.getElementsByTagNameNS(schemaURI, "attribute"))
      )
      .withErrorEventExceptions(
    		buildErrorEventExceptions(element.getElementsByTagNameNS(schemaURI, "error"))
      );
   }
}


