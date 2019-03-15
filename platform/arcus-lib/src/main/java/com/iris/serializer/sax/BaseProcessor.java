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
package com.iris.serializer.sax;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;

import com.google.common.collect.ImmutableSet;
import com.iris.util.IrisCollections;
import com.iris.validators.Validator;

public class BaseProcessor implements TagProcessor {

   protected Validator validator;
   protected String tag = "[unknown]";

   protected BaseProcessor(Validator validator) {
      if(validator != null) {
         this.validator = validator;
      }
      else {
         this.validator = new Validator();
      }
   }

   @Override
   public Validator getValidator() {
      return validator;
   }

   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      // TODO shouldn't this add a validator error?
      return new SkipTagProcessor(getValidator());
   }

   @Override
   public void enterTag(String qName, Attributes attributes) {
      this.tag = qName;
   }

   @Override
   public void exitTag(String qName) {
   }

   @Override
   public void enterChildTag(String qName, TagProcessor handler) {
      
   }

   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      
   }

   @Override
   public void onText(char[] text, int start, int length) {
      // TODO Auto-generated method stub
      
   }

   /**
    * Gets a required attribute, creating a validation error if it does
    * not exist.
    * @param name
    * @param attributes
    * @return
    */
   protected String getValue(String name, Attributes attributes) {
      String value = getValue(name, null, attributes);
      if(value == null) {
         getValidator().error("Missing required attribute [" + name + "] for tag <" + tag + ">");
      }
      return value;
   }

   /**
    * Gets an optional value, returning {@code dflt} if it is not set.
    * @param name
    * @param dflt
    * @param attributes
    * @return
    */
   protected String getValue(String name, String dflt, Attributes attributes) {
      String value = attributes.getValue(name);
      if(StringUtils.isEmpty(value)) {
         return dflt;
      }
      return value;
   }
   
   protected boolean parseBoolean(String value) {
      return Boolean.parseBoolean(value);
   }

   protected Date parseDate(String value) {
      DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      dateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
      if(StringUtils.isEmpty(value)) {
         return null;
      }
      try {
         return dateFmt.parse(value);
      }
      catch(ParseException e) {
         getValidator().error("Error parsing date: " + e.getMessage());
         return null;
      }
   
   }

   protected Set<String> parseCommaDelimited(String value) {
      if(StringUtils.isEmpty(value)) {
         return ImmutableSet.of();
      }
      String [] parts = StringUtils.split(value, ", ");
      return IrisCollections.setOf(parts);
   }

   protected <T extends Enum<T>> T getEnumValue(
         String name, Class<T> type, T defaultValue, Attributes attributes) {
            String value = attributes.getValue(name);
            if(StringUtils.isEmpty(value)) {
               return defaultValue;
            }
            for(T t: EnumSet.allOf(type)) {
               if(t.name().equalsIgnoreCase(value)) {
                  return t;
               }
            }
            return defaultValue;
         }

}

