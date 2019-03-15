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
package com.iris.log;

import java.util.Map;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 
 */
public class IrisMdcConverter extends ClassicConverter {
   String key;
   String prefix;
   String defaultValue;

   /**
    * 
    */
   public IrisMdcConverter() {
      // TODO Auto-generated constructor stub
   }

   /* (non-Javadoc)
    * @see ch.qos.logback.core.pattern.DynamicConverter#start()
    */
   @Override
   public void start() {
      String option = getFirstOption();
      if(option == null || option.length() == 0) {
         key = "";
      }
      else {
         String [] parts = option.split("\\:", 3);
         if(parts.length == 1) {
            key = option;
            prefix = "<";
            defaultValue = "";
         }
         else if(parts.length == 2) {
            prefix = "<" + parts[0] + ":";
            key = parts[1];
            defaultValue = "";
         }
         else if(parts.length == 3) {
            prefix = parts[0].length() > 0 ? "<" + parts[0] + ":"  : "<";
            key = parts[1];
            defaultValue = prefix + parts[2] + ">";
         }
      }
      super.start();
   }

   /* (non-Javadoc)
    * @see ch.qos.logback.core.pattern.Converter#convert(java.lang.Object)
    */
   @Override
   public String convert(ILoggingEvent event) {
      Map<String, String> properties = event.getMDCPropertyMap();
      if(properties == null) {
         return "";
      }
      String value = properties.get(key);
      if(value == null) {
         return defaultValue;
      }
      
      StringBuilder sb = 
            new StringBuilder(30)
               .append(prefix)
               .append(value)
               .append(">");
      
      return sb.toString();
   }

}

