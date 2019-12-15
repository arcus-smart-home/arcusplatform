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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.iris.capability.definition.Definitions;
import com.iris.capability.definition.Definitions.ProtocolDefinitionBuilder;
import com.iris.capability.definition.ProtocolDefinition;

public class ProtocolReader  extends BaseDefinitionReader<ProtocolDefinition, ProtocolDefinitionBuilder> {
   private final static Logger logger = LoggerFactory.getLogger(ProtocolReader.class);
   public final static String SCHEMA_URI = "http://www.arcussmarthome.com/schema/protocol/1.0.0";
   
   public ProtocolReader() {
      super(SCHEMA_URI);
   }

   @Override
   protected ProtocolDefinitionBuilder builder() {
      return Definitions.protocolBuilder();
   }

   @Override
   protected ProtocolDefinition buildModel(Element root) {
      ProtocolDefinitionBuilder builder = builder();
      builder
         .withName(root.getAttribute("name"))
         .withNamespace(root.getAttribute("namespace"))
         .withVersion(root.getAttribute("version"));
      
      NodeList nodes = root.getElementsByTagNameNS(schemaURI, "description");
      if (nodes.getLength() > 0)
      {
         Element description = (Element)nodes.item(0);
         builder.withDescription(readDescription(description));
      }
      else {
         logger.warn("No description was given for the capability {}", root.getAttribute("name"));
      }
      
      builder.withAttributes(buildAttributes(root.getElementsByTagNameNS(schemaURI, "attribute")));
      return builder.build();
   }

   @Override
   protected void populateDefinitionSpecificData(ProtocolDefinitionBuilder builder, Element element) {
      // No specific data to handle. 
   }
}

