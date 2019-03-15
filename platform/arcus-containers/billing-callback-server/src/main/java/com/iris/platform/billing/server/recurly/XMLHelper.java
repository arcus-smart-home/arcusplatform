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
package com.iris.platform.billing.server.recurly;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XMLHelper {
   private static final Logger LOGGER = LoggerFactory.getLogger(XMLHelper.class);
   private static JAXBContext jaxbContext;
   
   static{
      try {
         //The context is expensive to create and is thread safe so will will just create it once.
         jaxbContext=JAXBContext.newInstance(com.iris.billing.webhooks.model.ObjectFactory.class);
      } catch (JAXBException e) {
         LOGGER.error("Error initializing JAXBContext for webhooks");
      };
   }
   static public Document parse(String recurlyXML){
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder;
      Document document=null;
      try {
         builder = factory.newDocumentBuilder();
         document=builder.parse(new InputSource(new StringReader(recurlyXML)));
      } catch (Exception e) {
         LOGGER.warn("error parsing xml from recurly request",e);
      }
      return document;
   }
   
   @SuppressWarnings("unchecked")
   static public <T>T unmarshall(Document fullDoc, Class<T> expectedType){
      try{
         Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
         return (T)jaxbUnmarshaller.unmarshal(fullDoc);
      }
      catch(JAXBException ex){
           throw new RuntimeException("error unmarshalling jaxb object",ex); 
      }
      
   }
}

