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
package com.iris.common.subsystem.care.behavior;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.io.Resources;

//TODO: Sloppy I know, I'll fix this.

@XmlRootElement
public class BehaviorCatalog {

   @XmlElement
   private List<BehaviorCatalogTemplate> behavior;

   private Map<String, BehaviorCatalogTemplate> behaviorMap;

   public List<BehaviorCatalogTemplate> getBehavior() {
      if (behavior == null) {
         init();
      }
      return behavior;
   }

   @Override
   public String toString() {
      return "BehaviorCatalog [behavior=" + behavior + "]";
   }

   public Set<String> getBehaviorTypes() {
      return behaviorMap.keySet();
   }

   public BehaviorCatalogTemplate getBehaviorCatalogTemplate(String id) {
      return behaviorMap.get(id);
   }

   public synchronized void init() {
      try{
         URL url = Resources.getResource("com/iris/common/subsystem/care/behavior/behavior_catalog.xml");
         JAXBContext context = JAXBContext.newInstance(BehaviorCatalog.class);
         Marshaller m = context.createMarshaller();
         BehaviorCatalog catalog = (BehaviorCatalog) context.createUnmarshaller().unmarshal(url);
         behavior = catalog.getBehavior();
         behaviorMap = new HashMap<String, BehaviorCatalog.BehaviorCatalogTemplate>(behavior.size());
         for (BehaviorCatalogTemplate catTemplate : behavior){
            behaviorMap.put(catTemplate.id, catTemplate);
         }

      }catch (Exception e){
         throw new RuntimeException(e);
      }
   }

   @XmlRootElement(name = "behavior")
   public static class BehaviorCatalogTemplate {
      @Override
      public String toString() {
         return "BehaviorCatalogTemplate [id=" + id + ", name=" + name + ", description=" + description + ", type=" + type + ", deviceSelectorQuery=" + deviceSelectorQuery + ", timeWindowSupport=" + timeWindowSupport + "]";
      }

      private String id;
      private String name;
      private String description;
      private String type;
      private String deviceSelectorQuery;
      private String timeWindowSupport;
      
      private List<BehaviorOption> option;

      public List<BehaviorOption> getOption() {
         return option;
      }

      public void setOption(List<BehaviorOption> option) {
         this.option = option;
      }

      public String getTimeWindowSupport() {
         return timeWindowSupport;
      }

      public void setTimeWindowSupport(String timeWindowSupport) {
         this.timeWindowSupport = timeWindowSupport;
      }

      public String getId() {
         return id;
      }

      public void setId(String id) {
         this.id = id;
      }

      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public String getDescription() {
         return description;
      }

      public void setDescription(String description) {
         this.description = description;
      }

      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      public String getDeviceSelectorQuery() {
         return deviceSelectorQuery;
      }

      public void setDeviceSelectorQuery(String deviceSelectorQuery) {
         this.deviceSelectorQuery = deviceSelectorQuery;
      }

   }

   @XmlRootElement(name = "option")
   @XmlAccessorType(XmlAccessType.FIELD)
   public static class BehaviorOption {
     
      @XmlAttribute
      private String name;
      @XmlAttribute
      private String label;
      @XmlValue
      private String description;
      @XmlAttribute
      private String unit;
      @XmlAttribute
      private String values;

      @Override
      public String toString() {
         return "BehaviorOptions [name=" + name + ", label=" + label + ", description=" + description + ", unit=" + unit + ", values=" + values + "]";
      }
      
      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public String getLabel() {
         return label;
      }

      public void setLabel(String label) {
         this.label = label;
      }

      public String getDescription() {
         return description;
      }

      public void setDescription(String description) {
         this.description = description;
      }

      public String getUnit() {
         return unit;
      }

      public void setUnit(String unit) {
         this.unit = unit;
      }

      public String getValues() {
         return values;
      }
      

      public void setValues(String values) {
         this.values = values;
      }
      

   }
}

