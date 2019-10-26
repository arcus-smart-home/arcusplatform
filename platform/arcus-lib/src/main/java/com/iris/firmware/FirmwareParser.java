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
package com.iris.firmware;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.iris.resource.manager.ResourceParser;

public class FirmwareParser implements ResourceParser<List<FirmwareUpdate>> {
   private static final Logger logger = LoggerFactory.getLogger(FirmwareParser.class);
   
   private static final String FIRMWARE_SCHEMA = "firmware.xsd";
   private static final String ANY_NS = "*";
   private static final String ELEMENT_FIRMWARE = "firmware";
   private static final String ATTR_MIN = "min";
   private static final String ATTR_MAX = "max";
   private static final String ATTR_MODEL = "model";
   private static final String ATTR_TARGET = "target";
   private static final String ATTR_POPULATION = "population";
   

   @Override
   public List<FirmwareUpdate> parse(InputStream is) {
      List<FirmwareUpdate> firmwareUpdates = new ArrayList<>();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      
      try {
         DocumentBuilder builder = factory.newDocumentBuilder();
         Document document = builder.parse(is);
         validate(document);
         document.getDocumentElement().normalize();
         Element root = document.getDocumentElement();
         
         NodeList firmwareElements = root.getElementsByTagNameNS(ANY_NS, ELEMENT_FIRMWARE);
         for (int index = 0; index < firmwareElements.getLength(); index++) {
            FirmwareUpdate firmwareUpdate = buildFirmwareUpdate(firmwareElements.item(index));
            firmwareUpdates.add(firmwareUpdate);
         }
      }
      catch(Exception ex) {
         throw new RuntimeException("Failed to parse firmware updates.", ex);
      }
      FirmwareUpdateVerifier.verifyFirmwareUpdates(firmwareUpdates);
      return Collections.unmodifiableList(firmwareUpdates);
   }
   
   private FirmwareUpdate buildFirmwareUpdate(Node firmwareNode) {
      Element firmware = (Element)firmwareNode;
      String model = firmware.getAttribute(ATTR_MODEL);
      String min = firmware.getAttribute(ATTR_MIN);
      String max = firmware.getAttribute(ATTR_MAX);
      String target = firmware.getAttribute(ATTR_TARGET);
      Set<String> populations = getPopulations(firmware.getAttribute(ATTR_POPULATION));
      if(populations.isEmpty()) {
      	throw new RuntimeException("Firmware XML is invalid because population attribute is missing.");
      }
      
      return new FirmwareUpdate(model, min, max, target, populations);
   }
   
   private Set<String> getPopulations(String populationStr) {
      Set<String> populations = new HashSet<>();
      if (populationStr != null) {
         String[] populationNames = populationStr.split(",");
         if (populationNames != null) {
            for (String populationName : populationNames) {
               if (!populationName.isEmpty()) {
                  populations.add(populationName.trim().toLowerCase());
               }
            }
         }
      }
      return populations;
   }
   
   private void validate(Document document) {
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Source schemaFile = new StreamSource(FirmwareParser.class.getClassLoader().getResourceAsStream(FIRMWARE_SCHEMA));
      try {
         Schema schema = factory.newSchema(schemaFile);
         FirmwareErrorHandler errorHandler = new FirmwareErrorHandler();
         Validator validator = schema.newValidator();
         validator.setErrorHandler(errorHandler);
         try {
            validator.validate(new DOMSource(document));
            if (!errorHandler.isValid()) {
               throw new RuntimeException("Firmware XML is invalid see log for details.");
            }
         }
         catch (Exception e) {
            throw new RuntimeException("Error while validating firmware XML", e);
         } 
      } catch (SAXException e) {
         throw new RuntimeException("Could not parse XML schema file for firmware " + FIRMWARE_SCHEMA, e);
      }
   }
   
   private static class FirmwareErrorHandler implements ErrorHandler {
      private boolean isValid = true;

      @Override
      public void warning(SAXParseException exception) throws SAXException {
         logger.warn("Warning while validating Firmware XML: {}", exception.getMessage());
      }

      @Override
      public void error(SAXParseException exception) throws SAXException {
         logger.error("Error while validating Firmware XML: {}", exception.getMessage());
         isValid = false;
         
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
         logger.error("Fatal error while validating Firmware XML: {}", exception.getMessage());
         throw exception;
      }
      
      public boolean isValid() {
         return isValid;
      }    
   }
}

