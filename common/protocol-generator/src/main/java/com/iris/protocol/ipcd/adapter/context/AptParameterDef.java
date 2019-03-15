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
package com.iris.protocol.ipcd.adapter.context;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class AptParameterDef {
   private String aosName;
   private String aosType;
   private String name;
   private String type;
   private List<String> values;
   private String attrib;
   private String unit;
   private Double floor;
   private Double ceiling;
   private String description;
   
   public String getFloorValue() {
      return floor != null ? String.valueOf(floor) : "null";
   }
   
   public String getCeilingValue() {
      return ceiling != null ? String.valueOf(ceiling) : "null";
   }
   
   public String getUnitValue() {
      return StringUtils.isEmpty(unit) ? "null" : "\"" + unit + "\"";
   }
   
   public String getDescriptionValue() {
      return StringUtils.isEmpty(description) ? "null" : "\"" + description + "\"";
   }
   
   public String getValuesBuilder() {
      if (values == null || values.isEmpty()) {
         return "null";
      }
      else {
         StringBuilder sb = new StringBuilder("Arrays.asList(");
         boolean notFirst = false;
         for (String s : values) {
            if (notFirst) {
               sb.append(",");
            }
            else {
               notFirst = true;
            }
            sb.append('"').append(s).append('"');
         }
         sb.append(")");
         return sb.toString();
      }
   }
   
   public String getAosName() {
      return aosName;
   }
   public void setAosName(String aosName) {
      this.aosName = aosName;
   }
   public String getAosType() {
      return aosType;
   }
   public void setAosType(String aosType) {
      this.aosType = aosType;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   public String getType() {
      return type;
   }
   public void setType(String type) {
      this.type = type;
   }
   public List<String> getValues() {
      return values;
   }
   public void setValues(List<String> values) {
      this.values = values;
   }
   public String getAttrib() {
      return attrib;
   }
   public void setAttrib(String attrib) {
      this.attrib = attrib;
   }
   public String getUnit() {
      return unit;
   }
   public void setUnit(String unit) {
      this.unit = unit;
   }
   public Double getFloor() {
      return floor;
   }
   public void setFloor(Double floor) {
      this.floor = floor;
   }
   public Double getCeiling() {
      return ceiling;
   }
   public void setCeiling(Double ceiling) {
      this.ceiling = ceiling;
   }
   public String getDescription() {
      return description;
   }
   public void setDescription(String description) {
      this.description = description;
   }
}

