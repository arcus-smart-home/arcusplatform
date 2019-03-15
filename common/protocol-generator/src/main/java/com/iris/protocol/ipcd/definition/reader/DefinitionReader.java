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
package com.iris.protocol.ipcd.definition.reader;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.iris.protocol.ipcd.xml.model.Definition;
import com.iris.protocol.ipcd.xml.model.ObjectFactory;
import com.iris.protocol.ipcd.definition.context.DefinitionContext;

public class DefinitionReader {
   private final JAXBContext jaxbContext;
   
   public DefinitionReader() {
      try {
         jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
      } catch (JAXBException e) {
         throw new RuntimeException("Unable to load JAXBContext", e);
      }
   }
   
   public DefinitionContext readBindings(File input) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder;
      Document document = null;
      try {
         builder = factory.newDocumentBuilder();
         document = builder.parse(input);
      }
      catch (Exception ex) {
         throw new RuntimeException("Cannot read ipcd binding input file", ex);
      }
      Definition ipcdDef = unmarshall(document);
      DefinitionContextBuilder contextBuilder = new DefinitionContextBuilder();
      return contextBuilder.build(ipcdDef);
   }
   
   private Definition unmarshall(Document doc) {
      try {
         Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
         return (Definition)jaxbUnmarshaller.unmarshal(doc);
      }
      catch (Exception ex) {
         throw new RuntimeException("Error unmarshalling JAXB", ex);
      }
      
   }
}

