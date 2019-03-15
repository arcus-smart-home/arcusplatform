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

import com.iris.protocol.ipcd.adapter.reader.AdapterContextBuilder;

public class AptDeviceValue {
   private String value;
   private boolean parameter;
   
   public String getListBuilder() {
      StringBuffer sb = new StringBuffer("Arrays.asList(");
      boolean notFirst = false;
      String[] vals = value.split(AdapterContextBuilder.SEPARATOR);
      for (String s : vals) {
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
   
   public String getValue() {
      return value;
   }
   public void setValue(String value) {
      this.value = value;
   }
   public boolean isParameter() {
      return parameter;
   }
   public void setParameter(boolean parameter) {
      this.parameter = parameter;
   }
}

