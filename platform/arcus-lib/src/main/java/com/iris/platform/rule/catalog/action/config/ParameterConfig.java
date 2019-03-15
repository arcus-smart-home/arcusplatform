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
package com.iris.platform.rule.catalog.action.config;

import java.util.Map;

import com.iris.platform.rule.catalog.template.TemplatedValue;

public class ParameterConfig {

   public enum ParameterType {
      DATETIME, CONSTANT, ATTRIBUTEVALUE;
   }
   
   public enum DateType {
      DATE, TIME, DATETIME;
   }

   private ParameterType type;
   private DateType dateType;
   private String name;
   private String value;
   private String attributeName;
   private String address;

   public DateType getDateType() {
      return dateType;
   }

   public void setDateType(DateType dateType) {
      this.dateType = dateType;
   }
   
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public ParameterType getType() {
      return type;
   }

   public void setType(ParameterType type) {
      this.type = type;
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getAttributeName() {
      return attributeName;
   }

   public void setAttributeName(String attributeName) {
      this.attributeName = attributeName;
   }

   public String getAddress() {
      return address;
   }

   public void setAddress(String address) {
      this.address = address;
   }
   
   public ParameterConfig interpolate(Map<String,Object>variables){
      ParameterConfig newConfig = new ParameterConfig();
      newConfig.setName(this.name);
      newConfig.setType(this.type);
      if(type.equals(ParameterType.ATTRIBUTEVALUE)){
         newConfig.setAddress(TemplatedValue.text(this.address).apply(variables));
         newConfig.setAttributeName(this.attributeName);
      }
      else if(type.equals(ParameterType.CONSTANT)){
         newConfig.setValue(TemplatedValue.text(this.value).apply(variables));
      }
      else if(type.equals(ParameterType.DATETIME)){
         //what to do with times;
      }
      return newConfig;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
      result = prime * result + ((dateType == null) ? 0 : dateType.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ParameterConfig other = (ParameterConfig) obj;
      if (address == null) {
         if (other.address != null)
            return false;
      }else if (!address.equals(other.address))
         return false;
      if (attributeName == null) {
         if (other.attributeName != null)
            return false;
      }else if (!attributeName.equals(other.attributeName))
         return false;
      if (dateType != other.dateType)
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      }else if (!name.equals(other.name))
         return false;
      if (type != other.type)
         return false;
      if (value == null) {
         if (other.value != null)
            return false;
      }else if (!value.equals(other.value))
         return false;
      return true;
   }
   
}

