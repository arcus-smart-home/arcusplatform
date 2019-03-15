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
package com.iris.io.xml;

import java.io.InputStream;

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
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.io.Closeables;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

public class XMLUtil {
   private static final Logger logger = LoggerFactory.getLogger(XMLUtil.class);

   public static Document parseDocumentDOM(String xmlResource) {
      InputStream is = null;
      try{
         Resource resource = Resources.getResource(xmlResource);
         is = resource.open();
         return parseDocumentDOM(is);
      }
      catch(Exception ioe){
         throw new RuntimeException(ioe);
      }
      finally{
         Closeables.closeQuietly(is);
      }
   }
   
   public static Document parseDocumentDOM(String xmlResource, String schemaResource) {
      Document doc = parseDocumentDOM(xmlResource);
      validate(doc,schemaResource);
      return doc;
   }
   
   public static Document parseDocumentDOM(InputStream is) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      try{
         DocumentBuilder builder = factory.newDocumentBuilder();
         Document document = builder.parse(is);
         document.getDocumentElement().normalize();
         return document;
      }catch (Exception ex){
         throw new RuntimeException("Failed to parse xml file", ex);
      }
   }

   public static void validate(Document document, String schemaResource) {
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      InputStream is=null;
      try{
         Resource resource = Resources.getResource(schemaResource);
         is=resource.open();
         Source schemaFile = new StreamSource(is);
         Schema schema = factory.newSchema(schemaFile);
         XMLUtil.SimpleXMLValidationErrorHandler errorHandler = new XMLUtil.SimpleXMLValidationErrorHandler();
         Validator validator = schema.newValidator();
         validator.setErrorHandler(errorHandler);
         try{
            validator.validate(new DOMSource(document));
            if (!errorHandler.isValid()){
               throw new RuntimeException("XML is invalid see log for details.");
            }
         }catch (Exception e){
            throw new RuntimeException("Error while validating XML", e);
         }
      }catch (SAXException e){
         throw new RuntimeException("Could not parse XML schema file for " + schemaResource, e);
      }
      catch (Exception ioe){
         throw new RuntimeException("Could not parse XML schema file for " + ioe, ioe);
      }
      finally{
         Closeables.closeQuietly(is);
      }
      
   }

   private static class SimpleXMLValidationErrorHandler implements ErrorHandler {
      private boolean isValid = true;

      @Override
      public void warning(SAXParseException exception) throws SAXException {
         logger.warn("Warning while validating XML: {}", exception.getMessage());
      }

      @Override
      public void error(SAXParseException exception) throws SAXException {
         logger.error("Error while validating XML: {}", exception.getMessage());
         isValid = false;

      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
         logger.error("Fatal error while validating XML: {}", exception.getMessage());
         throw exception;
      }

      public boolean isValid() {
         return isValid;
      }
   }
}

