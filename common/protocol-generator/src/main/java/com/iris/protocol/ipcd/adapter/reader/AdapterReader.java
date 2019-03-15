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
package com.iris.protocol.ipcd.adapter.reader;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.iris.protocol.ipcd.adapter.context.AdapterContext;
import com.iris.protocol.ipcd.aos.xml.model.Adapter;
import com.iris.protocol.ipcd.aos.xml.model.ObjectFactory;

public class AdapterReader {
   private final JAXBContext jaxbContext;
   
   public AdapterReader() {
      try {
         jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
      } catch (JAXBException e) {
         throw new RuntimeException("Unable to load JAXBContext", e);
      }
   }
   
   public AdapterContext readBindings(File input) {
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
      Adapter adapter = unmarshall(document);
      AdapterContextBuilder contextBuilder = new AdapterContextBuilder();
      return contextBuilder.build(adapter);
   }
   
   private Adapter unmarshall(Document doc) {
      try {
         Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
         return (Adapter)jaxbUnmarshaller.unmarshal(doc);
      }
      catch (Exception ex) {
         throw new RuntimeException("Error unmarshalling JAXB", ex);
      }
      
   }

}

