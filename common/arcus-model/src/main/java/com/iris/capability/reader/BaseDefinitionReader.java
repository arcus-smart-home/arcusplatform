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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.Definitions;
import com.iris.capability.definition.Definitions.AttributeDefinitionBuilder;
import com.iris.capability.definition.Definitions.ErrorCodeDefinitionBuilder;
import com.iris.capability.definition.Definitions.ObjectDefinitionBuilder;
import com.iris.capability.definition.ErrorCodeDefinition;
import com.iris.capability.definition.EventDefinition;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ParameterDefinition;

public abstract class BaseDefinitionReader<T extends Definition, B extends ObjectDefinitionBuilder<B, T>> {

   private final static Logger logger = LoggerFactory.getLogger(BaseDefinitionReader.class);
   protected final String schemaURI;

   public BaseDefinitionReader(String schemaURI) {
      this.schemaURI = schemaURI;
   }

   public List<T> readDefinitionsFromPath(String filePathName) throws IOException {
      File filePath = new File(filePathName);
      if (!filePath.isDirectory()) {
         throw new IOException("The specified path " + filePathName + " is not a valid directory.");
      }
      if (!filePath.canRead()) {
         throw new IOException("Insufficient permissions to read the directory at " + filePathName);
      }
      List<T> defs = new ArrayList<>();
      readDefinitionsFromDirectory(defs, filePath);
      return defs;
   }

   private void readDefinitionsFromDirectory(List<T> defs, File filePath) {
      File[] files = filePath.listFiles();
      for (File file : files) {
         if (file.isFile() && file.getName().endsWith(".xml")) {
            T def = readDefinition(file);
            if (def != null) {
               defs.add(def);
               logger.info("Adding definition for [{}]", def.getName());
            }
         }
         else if (file.isDirectory()) {
            readDefinitionsFromDirectory(defs, file);
         }
      }
   }

   public T readDefinition(File file) {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      try {
         documentBuilderFactory.setNamespaceAware(true);
         DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
         Document doc = documentBuilder.parse(file);
         Element root = doc.getDocumentElement();
         logger.trace("Read root element [{}]", root.getNodeName());
         return buildModel(root);
      } catch (ParserConfigurationException e) {
         logger.error("Could not create document builder", e);
         return null;
      } catch (SAXException e) {
         logger.error("Could not parse xml file {}", file, e);
         return null;
      } catch (IOException e) {
         logger.error("Could not open xml file {}", file, e);
         return null;
      }
   }

   public T readDefinition(InputStream is) throws ParserConfigurationException, SAXException, IOException {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document doc = documentBuilder.parse(is);
      Element root = doc.getDocumentElement();
      logger.trace("Read root element [{}]", root.getNodeName());
      return buildModel(root);
   }

   protected T buildModel(Element root) {
      B builder = builder();
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

      populateDefinitionSpecificData(builder, root);

      builder.withMethods(buildMethods(root.getElementsByTagNameNS(schemaURI, "method")));
      builder.withEvents(buildEvents(root.getElementsByTagNameNS(schemaURI, "event")));
      return builder.build();
   }

   protected abstract B builder();
   
   protected abstract void populateDefinitionSpecificData(B builder, Element element);
   
   protected List<AttributeDefinition> buildAttributes(NodeList nodes) {
      List<AttributeDefinition> attributes = new ArrayList<AttributeDefinition>(nodes.getLength() + 1);
      logger.trace("Reading {} attribute nodes", nodes.getLength());
      for (int i = 0; i < nodes.getLength(); i++) {
         Element element = (Element)nodes.item(i);
         AttributeDefinitionBuilder builder =
               Definitions
                  .attributeBuilder()
                  .withName(element.getAttribute("name"))
                  .withType(element.getAttribute("type"))
                  .withMin(element.getAttribute("min"))
                  .withMax(element.getAttribute("max"))
                  .withDescription(element.getAttribute("description"))
                  .withUnit(element.getAttribute("unit"));
         if(element.getAttribute("optional").equals("true")) {
            builder.optional();
         }
         if(element.getAttribute("readwrite").equals("rw")) {
            builder.writable();
         }
         String enumValues = element.getAttribute("values");
         if(enumValues != null && enumValues.length() > 0) {
            for(String enumValue: enumValues.split("\\,")) {
               builder.addEnumValue(enumValue.trim());
            }
         }
         AttributeDefinition attribute = builder.build();
         attributes.add(attribute);
         logger.trace("Added attribute [{}]", attribute.getName());
      }
      return attributes;
   }
   
   protected Set<ErrorCodeDefinition> buildErrorEventExceptions(NodeList nodes) {
      Set<ErrorCodeDefinition> errorEventExceptions = new HashSet<ErrorCodeDefinition>(nodes.getLength() + 1);
      logger.trace("Reading {} error code nodes", nodes.getLength());
      for (int i = 0; i < nodes.getLength(); i++) {
	         Element element = (Element)nodes.item(i);
	         ErrorCodeDefinition errorCode = readErrorCode(element);
	         errorEventExceptions.add(errorCode);
	         logger.trace("Added errorCode [{}]", errorCode.getName());
	  }
      
      return errorEventExceptions;
   }  

   private List<MethodDefinition> buildMethods(NodeList nodes) {
      List<MethodDefinition> methods = new ArrayList<MethodDefinition>(nodes.getLength() + 1);
      logger.trace("Reading {} method nodes", nodes.getLength());
      for (int i = 0; i < nodes.getLength(); i++) {
         Element element = (Element)nodes.item(i);
         MethodDefinition method = 
               Definitions
                  .methodBuilder()
                  .withName(element.getAttribute("name"))
                  .withDescription(element.getAttribute("description"))
                  .isRestful(Boolean.parseBoolean(element.getAttribute("isRESTful")))
                  .withParameters(
                        buildParameters(element.getElementsByTagNameNS(schemaURI, "parameter"))
                  )
                  .withReturnValues(
                        buildParameters(element.getElementsByTagNameNS(schemaURI, "return"))
                  )
                  .withErrorCodes(
                		  this.buildErrorCodes(element.getElementsByTagNameNS(schemaURI, "error"))
                  )
                  .build();
         methods.add(method);
         logger.trace("Added method [{}]", method.getName());
      }
      return methods;
   }

   private List<EventDefinition> buildEvents(NodeList nodes) {
      List<EventDefinition> events = new ArrayList<EventDefinition>(nodes.getLength() + 1);
      logger.trace("Reading {} event nodes", nodes.getLength());
      for (int i = 0; i < nodes.getLength(); i++) {
         Element element = (Element)nodes.item(i);
         EventDefinition event = 
               Definitions
                  .eventBuilder()
                  .withName(element.getAttribute("name"))
                  .withDescription(element.getAttribute("description"))
                  .withParameters(
                        buildParameters(element.getElementsByTagNameNS(schemaURI, "parameter"))
                  )
                  .build();
         events.add(event);
         logger.trace("Added event [{}]", event.getName());
      }
      return events;
   }

   private List<ErrorCodeDefinition> buildErrorCodes(NodeList nodes) {
	      List<ErrorCodeDefinition> errorCodes = new ArrayList<ErrorCodeDefinition>(nodes.getLength() + 1);
	      for (int i = 0; i < nodes.getLength(); i++) {
	         Element element = (Element)nodes.item(i);
	         ErrorCodeDefinition errorCode = readErrorCode(element);
	         errorCodes.add(errorCode);
	         logger.trace("Added errorCode [{}]", errorCode.getName());
	      }
	      return errorCodes;
	   }

   private ErrorCodeDefinition readErrorCode(Element element) {
      return 
         Definitions
            .errorCodeBuilder()
            .withCode(element.getAttribute("code"))
            .withDescription(element.getAttribute("description"))
            .build();
   }
	   
   private List<ParameterDefinition> buildParameters(NodeList nodes) {
      List<ParameterDefinition> parameters = new ArrayList<ParameterDefinition>(nodes.getLength() + 1);
      for (int i = 0; i < nodes.getLength(); i++) {
         Element element = (Element)nodes.item(i);
         ParameterDefinition parameter = readParameter(element);
         parameters.add(parameter);
         logger.trace("Added parameter [{}]", parameter.getName());
      }
      return parameters;
   }

   private ParameterDefinition readParameter(Element element) {
      return 
         Definitions
            .parameterBuilder()
            .withName(element.getAttribute("name"))
            .withType(element.getAttribute("type"))
            .isOptional(Boolean.parseBoolean(element.getAttribute("optional")))
            .withEnumValues(element.getAttribute("values"))
            .withDescription(element.getAttribute("description"))
            .build()
            ;
   }
   
   protected String readDescription(Element element) {
      String value = extractText(element);
      if (value == null || value.isEmpty()) {
         value = element.getNodeValue();
      }
      if (value != null) {
         value = value.replaceAll("[\\n\\r]", "");
      }
      return value;
   }

   private String extractText(Element element) {
      NodeList children = element.getChildNodes();
      if (children != null && children.getLength() > 0) {
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Text) {
               sb.append(((Text)child).getNodeValue());
            }
         }
         return sb.toString().trim();
      }
      return null;
   }
}

