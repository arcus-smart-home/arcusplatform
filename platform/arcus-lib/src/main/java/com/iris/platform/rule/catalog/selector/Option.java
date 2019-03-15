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
package com.iris.platform.rule.catalog.selector;

public class Option {

   private String label;
   private Object value;

   /**
    * An optional value that may be used to determine whether this option should be included.
    */
   private Object match;
   
   public Option() {
   }
   
   public Option(String label, Object value) {
      this.label = label;
      this.value = value;
   }

   public Option(String label, Object value, Object match) {
      this(label, value);
      this.match = match;
   }

   public String getLabel() {
      return label;
   }
   
   public void setLabel(String label) {
      this.label = label;
   }
   
   public Object getValue() {
      return value;
   }
   
   public void setValue(Object value) {
      this.value = value;
   }

   public Object getMatch()
   {
      return match;
   }

   public void setMatch(Object match)
   {
      this.match = match;
   }

   public String toString() {
	   return "Option["+this.label+","+this.value+","+this.match+"]";
   }
}

