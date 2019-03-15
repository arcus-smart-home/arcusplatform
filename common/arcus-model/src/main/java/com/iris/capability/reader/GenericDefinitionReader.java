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
package com.iris.capability.reader;

import org.w3c.dom.Element;

import com.iris.capability.definition.Definition;
import com.iris.capability.definition.ObjectDefinition;
import com.iris.capability.definition.Definitions.ObjectDefinitionBuilder;

/**
 * 
 */
public class GenericDefinitionReader extends BaseDefinitionReader<Definition, GenericDefinitionReader.Builder> {
   private CapabilityReader capabilityReader = new CapabilityReader();
   private ServiceReader serviceReader = new ServiceReader();
   private ProtocolReader protocolReader = new ProtocolReader();
   private TypeReader typeReader = new TypeReader();

   public GenericDefinitionReader() {
      super(null);
   }

   @Override
   protected Definition buildModel(Element root) {
      String name = root.getLocalName();
      if("capability".equals(name)) {
         return capabilityReader.buildModel(root);
      }
      else if("service".equals(name)) {
         return serviceReader.buildModel(root);
      }
      else if("protocol".equals(name)) {
         return protocolReader.buildModel(root);
      }
      else if("type".equals(name)) {
         return typeReader.buildModel(root);
      }
      throw new IllegalArgumentException("Invalid XML file, unrecognized root element: " + name);
   }

   @Override
   protected Builder builder() {
      // no-op
      return null;
   }

   @Override
   protected void populateDefinitionSpecificData(Builder def, Element element) {
      // TODO Auto-generated method stub
      
   }

   // need a concrete type to use in the signature
   public static class Builder extends ObjectDefinitionBuilder<Builder, Definition> {

      @Override
      public ObjectDefinition build() {
         // TODO Auto-generated method stub
         return null;
      }
      
   }
}

