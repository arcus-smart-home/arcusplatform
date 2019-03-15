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
package com.iris.capability.definition;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeDefinition extends Definition {
   private static final Logger logger = LoggerFactory.getLogger(AttributeDefinition.class);

   private final AttributeType type;
   private final Set<AttributeOption> flags;
   private final List<String> enumValues;
   private final String min;
   private final String max;
   private final String unit;
   
   AttributeDefinition(
         String name,
         String description,
         AttributeType type,
         Set<AttributeOption> flags,
         List<String> enumValues,
         String min,
         String max,
         String unit
   ) {
      super(name, description);
      this.type = type;
      this.flags = Collections.unmodifiableSet(flags);
      this.enumValues = Collections.unmodifiableList(enumValues);
      this.min = min;
      this.max = max;
      this.unit = unit;
   }
   
   public AttributeType getType() {
      return type;
   }
   
   public boolean isReadable() {
      return flags.contains(AttributeOption.READABLE);
   }
   
   public boolean isWritable() {
      return flags.contains(AttributeOption.WRITABLE);
   }
   
   public boolean isOptional() {
      return flags.contains(AttributeOption.OPTIONAL);
   }
   
   public String getMin() {
      return min;
   }
   
   public String getMax() {
      return max;
   }
   
   public String getUnit() {
      return unit;
   }
   
   public List<String> getEnumValues() {
      return enumValues;
   }
   
   public String getRange() {
      if (min != null && !min.isEmpty() && max !=null && !max.isEmpty()) {
         return min + ".." + max;
      }
      if (min != null && !min.isEmpty()) {
         return min + "..";
      }
      if (max != null && !max.isEmpty()) {
         return ".." + max;
      }
      if (enumValues.size() > 0) {
         StringBuilder sb = new StringBuilder();
         for(String value : enumValues) {
            if (sb.length() > 0) {
               sb.append(", ");
            }
            sb.append(value);
         }
         return sb.toString();
      }
      return "";
   }
   
   /**
    * Returns true if min or max is set.
    * @return
    */
   public boolean hasMinMax() {
      return StringUtils.isNotEmpty(min) || StringUtils.isNotEmpty(max);
   }
   
   public boolean isInRange(Object value) {
      if(value == null || !hasMinMax()) {
         return true;
      }

      switch(getType().getRawType()) {
      case INT: {
         int min = StringUtils.isEmpty(getMin()) ? Integer.MIN_VALUE : Integer.parseInt(getMin());
         int max = StringUtils.isEmpty(getMax()) ? Integer.MAX_VALUE : Integer.parseInt(getMax());
         int num = (Integer) getType().coerce(value);
         return num >= min && num <= max;
      }
      case LONG: {
         long min = StringUtils.isEmpty(getMin()) ? Long.MIN_VALUE : Long.parseLong(getMin());
         long max = StringUtils.isEmpty(getMax()) ? Long.MAX_VALUE : Long.parseLong(getMax());
         long num = (Long) getType().coerce(value);
         return num >= min && num <= max;
      }
      case DOUBLE: {
         double min = StringUtils.isEmpty(getMin()) ? Double.MIN_VALUE : Double.parseDouble(getMin());
         double max = StringUtils.isEmpty(getMax()) ? Double.MAX_VALUE : Double.parseDouble(getMax());
         double num = (Double) getType().coerce(value);
         return num >= min && num <= max;
      }
      default:
         logger.warn("Can't apply a range to attribute of type [{}] named [{}]", getType(), getName());
         return true;
      }
   }
   @Override
   public String toString() {
      return "Attribute [name=" + name + ", type=" + type + ", readable=" + isReadable()
            + ", writable=" + isWritable() + ", optional=" + isOptional() + ", enumValues=" + enumValues
            + ", min=" + min + ", max=" + max + ", description=" + description
            + ", unit=" + unit + "]";
   }
   
   public static enum AttributeOption {
      READABLE,
      WRITABLE,
      OPTIONAL;
   }
}

