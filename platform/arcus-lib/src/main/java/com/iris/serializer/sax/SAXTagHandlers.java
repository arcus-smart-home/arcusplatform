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
package com.iris.serializer.sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.iris.platform.rule.catalog.serializer.DispatchProcessor;
import com.iris.validators.ValidationException;

/**
 * 
 */
public class SAXTagHandlers {

   public static void parse(String xml, TagProcessor handler) throws ValidationException {
      try {
         doParse(new ByteArrayInputStream(xml.getBytes()), handler);
      }
      catch (IOException e) {
         // this shouldn't happen on a ByteArrayInputStream
         throw new RuntimeException(e);
      }
   }

   public static void parse(String xml, String tag, TagProcessor handler) throws ValidationException {
      try {
         parse(new ByteArrayInputStream(xml.getBytes()), new DispatchProcessor(tag, handler));
      }
      catch(IOException e) {
         // this shouldn't happen on a ByteArrayInputStream
         throw new RuntimeException(e);
      }
   }

   public static void parse(InputStream xml, TagProcessor handler) throws IOException, ValidationException {
      doParse(xml, handler);
   }

   public static void parse(InputStream xml, String tag, TagProcessor handler) throws IOException, ValidationException {
      doParse(xml, new DispatchProcessor(tag, handler));
   }

   private static void doParse(InputStream input, TagProcessor handler) throws IOException, ValidationException {
      try {
         SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

         SAXTagHandler saxHandler = new SAXTagHandler(handler);
         parser.parse(input, saxHandler);
      }
      catch(IOException | RuntimeException e) {
         throw e;
      }
      catch(Exception e) {
         throw new IOException("Error parsing xml", e);
      }
      handler.getValidator().throwIfErrors();
   }
}

