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
package com.iris.protocol.ipcd.adapter;

import java.util.List;

import com.iris.protocol.ipcd.message.model.ParameterInfo;

public abstract class AdapterParameterInfo extends ParameterInfo {
   
   public AdapterParameterInfo(String type,
            List<String> enumValues,
            String attrib,
            String unit,
            Double floor,
            Double ceiling,
            String description) {
      super.setType(type);
      super.setEnumvalues(enumValues);
      super.setAttrib(attrib);
      super.setUnit(unit);
      super.setFloor(floor);
      super.setCeiling(ceiling);
      super.setDescription(description);
   }

   @Override
   public void setType(String type) { }

   @Override
   public void setEnumvalues(List<String> enumvalues) { }

   @Override
   public void setAttrib(String attrib) { }

   @Override
   public void setUnit(String unit) { }

   @Override
   public void setFloor(Double floor) { }

   @Override
   public void setCeiling(Double ceiling) { }

   @Override
   public void setDescription(String description) { }
   
}

