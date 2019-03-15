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
package com.iris.billing.serializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.xml.XmlEscapers;
import com.iris.billing.client.model.BaseRecurlyModel;

public class RecurlyObjectSerializer {
   private int depth = 0;
   private int depthLevel = 2;
   private boolean formattingEnabled = false;
   private List<BaseRecurlyModel> nestedModels;

   private BaseRecurlyModel rootObject;
   private StringBuilder xmlReturnString;

   public RecurlyObjectSerializer() {
      xmlReturnString = new StringBuilder();
      nestedModels = new ArrayList<>();
   }

   public int getDepthLevel() {
      return depthLevel;
   }

   public void setDepthLevel(int level) {
      if (level < 0) {
         depthLevel = 2;
      } else {
         depthLevel = level;
      }
   }

   public boolean formattingEnabled() {
      return formattingEnabled;
   }

   public void enableFormatting(boolean formatText) {
      formattingEnabled = formatText;
   }

   public <T extends BaseRecurlyModel> void setRoot(T wrapper) {
      rootObject = wrapper;
   }

   public synchronized String serialize() {
      if (rootObject == null) {
         throw new IllegalArgumentException("Must initialize root object. Cannot be null.");
      }

      if (formattingEnabled) {
         return String.format(generateXML());
      }

      String data = generateXML();

      this.rootObject = null;
      this.nestedModels.clear();
      xmlReturnString = new StringBuilder();

      return data;
   }

   public <T extends BaseRecurlyModel> void addNestedModel(T model) {
      nestedModels.add(model);
   }

   @SuppressWarnings("unchecked")
   private String generateXML() {
      if (rootObject == null) {
         return "";
      }

      Map<String, Object> xmlMappings = rootObject.getXMLMappings();
      if (xmlMappings == null || xmlMappings.isEmpty()) {
         return "";
      }

      xmlReturnString.append("<").append(rootObject.getTagName()).append(">");
      addNewLine();

      for (BaseRecurlyModel model : nestedModels) {
         depth += depthLevel;
         generateInnerXML(model);
      }

      for (Map.Entry<String, Object> item : xmlMappings.entrySet()) {
         if (item.getValue() instanceof List) {
            depth += depthLevel;
            xmlReturnString.append("<").append(item.getKey()).append(">");
            generateInnerXMLFromList((List<BaseRecurlyModel>) item.getValue());
            xmlReturnString.append("</").append(item.getKey()).append(">");
         } else if (item.getValue() instanceof BaseRecurlyModel) {
            depth += depthLevel;
            generateInnerXML((BaseRecurlyModel) item.getValue());
         } else {
            addSecondLevelSpaces();
            xmlReturnString.append("<").append(item.getKey()).append(">");
            xmlReturnString.append(getString(item.getValue()));
            xmlReturnString.append("</").append(item.getKey()).append(">");
            addNewLine();
         }
      }

      xmlReturnString.append("</").append(rootObject.getTagName()).append(">");
      addNewLine();
      addNewLine();

      return xmlReturnString.toString();
   }

   @SuppressWarnings("unchecked")
   private void generateInnerXML(BaseRecurlyModel billingModel) {
      Map<String, Object> xmlMappings = billingModel.getXMLMappings();

      if (xmlMappings == null || xmlMappings.isEmpty()) {
         return;
      }

      addFirstLevelSpaces();
      xmlReturnString.append("<").append(billingModel.getTagName()).append(">");
      addNewLine();

      for (Map.Entry<String, Object> item : xmlMappings.entrySet()) {
         if (item.getKey() == null || item.getValue() == null) {
            continue;
         }

         if (item.getValue() instanceof List) {
            depth += depthLevel;
            generateInnerXMLFromList((List<BaseRecurlyModel>) item.getValue());
         } else if (item.getValue() instanceof BaseRecurlyModel) {
            depth += depthLevel;
            generateInnerXML((BaseRecurlyModel) item.getValue());
         } else {
            addSecondLevelSpaces();
            xmlReturnString.append("<").append(item.getKey()).append(">");
            xmlReturnString.append(getString(item.getValue()));
            xmlReturnString.append("</").append(item.getKey()).append(">");
            addNewLine();
         }
      }

      addFirstLevelSpaces();
      xmlReturnString.append("</").append(billingModel.getTagName()).append(">");
      addNewLine();
      addNewLine();

      depth -= depthLevel;
   }

   private void generateInnerXMLFromList(List<BaseRecurlyModel> billingModel) {
      for (Object modelObject : billingModel) {
         if (!(modelObject instanceof BaseRecurlyModel)) {
            continue;
         }

         BaseRecurlyModel model = (BaseRecurlyModel) modelObject;
         Map<String, Object> xmlMappings = model.getXMLMappings();
   
         if (xmlMappings == null || xmlMappings.isEmpty()) {
            return;
         }

         addFirstLevelSpaces();
         xmlReturnString.append("<").append(model.getTagName()).append(">");
         addNewLine();
   
         for (Map.Entry<String, Object> item : xmlMappings.entrySet()) {
            if (item.getValue() instanceof BaseRecurlyModel) {
               depth += depthLevel;
               generateInnerXML((BaseRecurlyModel) item.getValue());
            } else {
               addSecondLevelSpaces();
               xmlReturnString.append("<").append(item.getKey()).append(">");
               xmlReturnString.append(getString(item.getValue()));
               xmlReturnString.append("</").append(item.getKey()).append(">");
               addNewLine();
            }
         }
         
         addFirstLevelSpaces();
         xmlReturnString.append("</").append(model.getTagName()).append(">");
         addNewLine();
         addNewLine();
      }

      depth -= depthLevel;
   }

   /**
    * 
    * Calls toString() for all objects passed in except for char arrays for which
    * this converts to a new string and then clears the char array buffer.
    * 
    * @param object
    * @return String
    */
   private String getString(Object object) {
      if (object == null) { return ""; }

      return XmlEscapers.xmlContentEscaper().escape(object.toString());
   }

   private void addFirstLevelSpaces() {
      if (formattingEnabled) {
         xmlReturnString.append(Strings.repeat(" ", depth));
      }
   }

   private void addSecondLevelSpaces() {
      if (formattingEnabled) {
         xmlReturnString.append(Strings.repeat(" ", depth + depthLevel));
      }
   }

   public void addNewLine() {
      if (formattingEnabled) {
         xmlReturnString.append("%n");
      }
   }
}

