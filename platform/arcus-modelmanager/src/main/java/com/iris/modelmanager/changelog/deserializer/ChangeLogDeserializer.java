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
package com.iris.modelmanager.changelog.deserializer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.changelog.ChangeLog;
import com.iris.modelmanager.changelog.ChangeLogSet;
import com.iris.modelmanager.changelog.ChangeSet;
import com.iris.modelmanager.changelog.Command;
import com.iris.modelmanager.changelog.JavaCommand;
import com.iris.modelmanager.changelog.checksum.ChecksumUtil;
import com.iris.modelmanager.context.ManagerContext;

public class ChangeLogDeserializer {

   private static final String SCHEMA_LOCATION = "schema/changelog-1.0.0.xsd";

   private final ManagerContext context;
   private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
   private final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

   private static class ParsingContext {
      List<ChangeLog> changeLogs = new LinkedList<ChangeLog>();
      Set<String> identifiers = new HashSet<String>();
      Set<String> versionsSeen = new HashSet<String>();
      ChangeLog curChangeLog;
      ChangeSet curChangeSet;
      Command curCommand;
      String curChangeLogFileName = null;

      ParsingContext() {
      }

      void setCurChangeLog(ChangeLog changeLog) throws XMLStreamException {
         if(versionsSeen.contains(changeLog.getVersion())) {
            throw new XMLStreamException("Duplciate version " + changeLog.getVersion() + " on changelogs have been found.");
         }
         versionsSeen.add(changeLog.getVersion());
         curChangeLog = changeLog;
      }

      void setCurChangeSet(ChangeSet changeSet) throws XMLStreamException {
         if(identifiers.contains(changeSet.getUniqueIdentifier())) {
            throw new XMLStreamException("Duplicate change sets have been found with author: " + changeSet.getAuthor() + " and identifier: " + changeSet.getIdentifier());
         }
         identifiers.add(changeSet.getUniqueIdentifier());
         curChangeSet = changeSet;
      }
   }

   public ChangeLogDeserializer(ManagerContext context) {
      this.context = context;
   }

   public ChangeLogSet deserialize(String changelog) throws XMLStreamException {
      ParsingContext parsingContext = new ParsingContext();
      deserializeInternal(changelog, parsingContext);
      return new ChangeLogSet(parsingContext.changeLogs);
   }

   private void deserializeInternal(String changeLog, ParsingContext parsingContext) throws XMLStreamException {

      XMLStreamReader xmlStream = null;

      try {

         URL url = context.getResourceLocator().locate(ManagerContext.CHANGELOG_DIRECTORY, changeLog);

         if(url == null) {
            throw new XMLStreamException("No changelog " + changeLog + " could be located.");
         }

         parsingContext.curChangeLogFileName = changeLog;
         String contents = IOUtils.toString(url.openStream());

         xmlStream = inputFactory.createXMLStreamReader(new StringReader(contents));
         validate(xmlStream);

         // reset for actually parsing after the file has been validated
         xmlStream.close();
         xmlStream = inputFactory.createXMLStreamReader(new StringReader(contents));

         while(xmlStream.hasNext()) {
            int event = xmlStream.next();
            switch(event) {
            case XMLStreamConstants.START_ELEMENT:
               handleStartElement(xmlStream, parsingContext);
               break;
            case XMLStreamConstants.END_ELEMENT:
               handleEndElement(xmlStream, parsingContext);
               break;
            default:
               /* no op */
            }
         }
      } catch(IOException ioe) {
         throw new XMLStreamException(ioe);
      } finally {
         if(xmlStream != null) { xmlStream.close(); }
      }
   }

   private void validate(XMLStreamReader reader) throws XMLStreamException {
      try {
         Source source = new StreamSource(getClass().getClassLoader().getResourceAsStream(SCHEMA_LOCATION));
         Schema schema = schemaFactory.newSchema(source);
         Validator validator = schema.newValidator();
         validator.validate(new StAXSource(reader));
      } catch(Exception e) {
         throw new XMLStreamException(e);
      }
   }

   private void handleStartElement(XMLStreamReader reader, ParsingContext parsingContext) throws XMLStreamException {
      String localName = reader.getLocalName();
      switch(localName) {
      case Constants.Elements.CHANGELOG:
         ChangeLog changeLog = new ChangeLog();
         changeLog.setSource(parsingContext.curChangeLogFileName);
         changeLog.setVersion(readRequiredAttribute(reader, Constants.Attributes.VERSION));
         parsingContext.setCurChangeLog(changeLog);
         break;
      case Constants.Elements.CHANGESET:
         ChangeSet changeSet = new ChangeSet();
         changeSet.setAuthor(readRequiredAttribute(reader, Constants.Attributes.AUTHOR));
         changeSet.setIdentifier(readRequiredAttribute(reader, Constants.Attributes.IDENTIFIER));
         parsingContext.setCurChangeSet(changeSet);
         break;
      case Constants.Elements.CQL: parsingContext.curCommand = new CQLCommand(); break;
      case Constants.Elements.UPDATE: ((CQLCommand) parsingContext.curCommand).setUpdateCql(readRequiredElementText(reader, localName)); break;
      case Constants.Elements.ROLLBACK: ((CQLCommand) parsingContext.curCommand).setRollbackCql(readRequiredElementText(reader, localName)); break;
      case Constants.Elements.DESCRIPTION: parsingContext.curChangeSet.setDescription(readOptionalElementText(reader)); break;
      case Constants.Elements.IMPORT:
         handleImport(readRequiredAttribute(reader, Constants.Attributes.FILE), parsingContext);
         break;
      case Constants.Elements.TRACKING: parsingContext.curChangeSet.setTracking(readOptionalElementText(reader)); break;
      case Constants.Elements.JAVA: parsingContext.curCommand = new JavaCommand(readRequiredAttribute(reader, Constants.Attributes.CLASS)); break;
      default:
         /* ignore */
      }
   }

   private void handleEndElement(XMLStreamReader reader, ParsingContext parsingContext) {
      String localName = reader.getLocalName();
      switch(localName) {
      case Constants.Elements.CHANGELOG:
         if(parsingContext.curChangeLog != null) {
            parsingContext.changeLogs.add(parsingContext.curChangeLog);
            parsingContext.curChangeLog = null;
         }
         break;
      case Constants.Elements.CHANGESET:
         ChecksumUtil.updateChecksum(parsingContext.curChangeSet);
         parsingContext.curChangeLog.addChangeSet(parsingContext.curChangeSet);
         parsingContext.curChangeSet = null;
         break;
      case Constants.Elements.CQL:
      case Constants.Elements.JAVA:
         parsingContext.curChangeSet.addCommand(parsingContext.curCommand);
         parsingContext.curCommand = null;
         break;
      default:
         /* ignore */
      }
   }

   private void handleImport(String file, ParsingContext parsingContext) throws XMLStreamException {
      parsingContext.curChangeLog = null;
      parsingContext.curChangeLogFileName = null;
      deserializeInternal(file, parsingContext);
   }

   private String readOptionalElementText(XMLStreamReader reader) throws XMLStreamException {
      String value = reader.getElementText();
      return StringUtils.isBlank(value) ? null : StringUtils.trim(value);
   }

   private String readRequiredElementText(XMLStreamReader reader, String elementName) throws XMLStreamException {
      String value = reader.getElementText();
      if(StringUtils.isBlank(value)) {
         throw new XMLStreamException("Required text content missing from " + elementName);
      }
      return value;
   }

   private String readRequiredAttribute(XMLStreamReader reader, String localName) throws XMLStreamException {
      String value = null;
      for(int i = 0; i < reader.getAttributeCount(); i++) {
         String attributeName = reader.getAttributeLocalName(i);
         if(attributeName.equals(localName)) {
            value = reader.getAttributeValue(i);
            break;
         }
      }

      if(StringUtils.isBlank(value)) {
         throw new XMLStreamException("Required attribute " + localName + " is missing");
      }

      return value;
   }
}

