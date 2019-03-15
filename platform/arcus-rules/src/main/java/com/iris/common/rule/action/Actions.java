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
package com.iris.common.rule.action;

import com.iris.messages.capability.Capability;

/**
 * 
 */
public class Actions {

   /**
    * Returns the {@code value} if it is non-null or
    * ${name} if value is {@code null}.  This allows
    * a string like the template syntax to be generated.
    * @param name
    * @param value
    * @return
    */
   public static String valueOrVar(String name, Object value) {
      if(value == null) {
         return "${" + name + "}";
      }
      return String.valueOf(value);
   }
   
   public static SendAction setValue(String attributeName, Object attributeValue) {
      return buildSetValue(attributeName, attributeValue).build();
   }
   
   public static SendAction.Builder buildSendAction(String messageType) {
      return new SendAction.Builder(messageType);
   }
   
   public static SendAction.Builder buildSetValue() {
      return new SendAction.Builder(Capability.CMD_SET_ATTRIBUTES);
   }

   public static SendAction.Builder buildSetValue(String attributeName, Object attributeValue) {
      return buildSetValue().withAttribute(attributeName, attributeValue);
   }
   
   public static ActionList.Builder buildActionList() {
      return new ActionList.Builder();
   }
   
   // TODO add NotificationAction.Builder
   
}

