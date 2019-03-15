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
package com.iris.oculus.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.html.HtmlEscapers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.iris.io.json.JSON;

/**
 * 
 */
public class JsonPrettyPrinter {

   public enum Format { 
      HTML, 
      PLAIN_TEXT;
      
      public Formatter newFormatter() {
         switch(this) {
         case PLAIN_TEXT:
            return new PlainTextFormatter();
         case HTML:
         default:
            return new HtmlFormatter();
         }
      }
   };
   
   public static String prettyPrint(String json) {
      return prettyPrint(json, null);
   }
   
   public static String prettyPrint(Object element) {
      return prettyPrint(element, null);
   }

   public static String prettyPrint(String json, Format format) {
      if(format == null) {
         format = Format.HTML;
      }
      return prettyPrint(JSON.fromJson(json, JsonElement.class), format.newFormatter());
   }
   
   public static String prettyPrint(Object element, Format format) {
      return prettyPrint(JSON.toJson(element), format);
   }
   
   protected static String prettyPrint(JsonElement element, Formatter formatter) {
      formatter.start();
      printElement(element, 0, formatter);
      return formatter.complete();
   }

   private static void printElement(JsonElement element, int indent, Formatter formatter) {
      if(element.isJsonNull()) {
         formatter.printNull();
      }
      else if(element.isJsonPrimitive()) {
         formatter.printPrimitive(element.getAsJsonPrimitive());
      }
      else if(element.isJsonArray()) {
         printArray(element.getAsJsonArray(), indent, formatter);
      }
      else if(element.isJsonObject()) {
         printObject(element.getAsJsonObject(), indent, formatter);
      }
      else {
         formatter.printChars(element.toString());
      }
   }

   private static void printArray(JsonArray array, int indent, Formatter formatter) {
      int size = array.size();
      if(size == 0) {
         formatter.printChars("[ ]");
      }
      else if(size == 1) {
         formatter.printChars("[ ");
         printElement(array.get(0), indent + 1, formatter);
         formatter.printChars(" ]");
      }
      else {
         formatter.printChars("[ ");
         for(int i=0; i<size; i++) {
            JsonElement je = array.get(i);
            if(je.isJsonObject()) {
               printElement(je, indent + 1, formatter);
            }
            else {
               formatter.printNewline(indent + 1);
               printElement(je, indent + 1, formatter);
            }
            if(i < (size - 1)) {
               formatter.printChars(", ");
            }
         }
         formatter.printNewline(indent);
         formatter.printChars(" ]");
      }
   }
   
   private static void printObject(JsonObject object, int indent, Formatter formatter) {
      List<String> keys = 
            object
               .entrySet()
               .stream()
               .map(Map.Entry::getKey)
               .sorted()
               .collect(Collectors.toList())
               ;
      int size = keys.size();
      if(size == 0) {
         formatter.printChars("{ }");
      }
      else {
         formatter.printChars("{");
         int count = 0;
         for(String key: keys) {
            count++;
            formatter.printNewline(indent + 1);
            formatter.printObjectKey(key);
            formatter.printChars(": ");
            printElement(object.get(key), indent + 1, formatter);
            if(count < size) {
               formatter.printChars(", ");
            }
         }
         formatter.printNewline(indent);
         formatter.printChars("}");
      }
   }

   private interface Formatter {
      
      void start();
      
      void printNewline(int indent);
      
      void printChars(String chars);
      
      void printObjectKey(String key);
      
      void printNull();
      
      void printPrimitive(JsonPrimitive value);
      
      String complete();
   }
   
   private static class HtmlFormatter implements Formatter {
      private StringBuilder out = new StringBuilder();

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#start()
       */
      @Override
      public void start() {
         out.append("<html><code>");
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printNewline(int)
       */
      @Override
      public void printNewline(int indent) {
         out.append("<br/>\n");
         out.append(StringUtils.repeat("&nbsp;&nbsp;&nbsp;", indent));
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printChars(java.lang.String)
       */
      @Override
      public void printChars(String chars) {
         out.append(chars);
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printObjectKey(java.lang.String)
       */
      @Override
      public void printObjectKey(String key) {
         printString(key);
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printNull()
       */
      @Override
      public void printNull() {
         out.append("<i>null</i>");
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printPrimitive(com.google.gson.JsonPrimitive)
       */
      @Override
      public void printPrimitive(JsonPrimitive primitive) {
         if(primitive.isString()) {
            printString(primitive.getAsString());
         }
         // TODO style by type?
         else {
            out.append(primitive.toString());
         }
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#complete()
       */
      @Override
      public String complete() {
         try {
            out.append("</code></html>");
            return out.toString();
         }
         finally {
            out.setLength(0);
         }
      }
      
      public void printString(String value) {
         out.append("\"").append(HtmlEscapers.htmlEscaper().escape(value)).append("\"");
      }
   }

   private static class PlainTextFormatter implements Formatter {
      private StringBuilder out = new StringBuilder();

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#start()
       */
      @Override
      public void start() {
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printNewline(int)
       */
      @Override
      public void printNewline(int indent) {
         out.append("\n");
         out.append(StringUtils.repeat("   ", indent));
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printChars(java.lang.String)
       */
      @Override
      public void printChars(String chars) {
         out.append(chars);
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printObjectKey(java.lang.String)
       */
      @Override
      public void printObjectKey(String key) {
         printString(key);
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printNull()
       */
      @Override
      public void printNull() {
         out.append("null");
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#printPrimitive(com.google.gson.JsonPrimitive)
       */
      @Override
      public void printPrimitive(JsonPrimitive primitive) {
         if(primitive.isString()) {
            printString(primitive.getAsString());
         }
         // TODO style by type?
         else {
            out.append(primitive.toString());
         }
      }

      /* (non-Javadoc)
       * @see com.iris.oculus.util.JsonPrettyPrinter.Formatter#complete()
       */
      @Override
      public String complete() {
         try {
            return out.toString();
         }
         finally {
            out.setLength(0);
         }
      }
      
      public void printString(String value) {
         out.append("\"").append(value.replaceAll("\"", "\\\"")).append("\"");
      }
   }
}

