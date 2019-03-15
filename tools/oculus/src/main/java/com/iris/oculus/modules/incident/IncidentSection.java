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
package com.iris.oculus.modules.incident;

import java.util.Date;

import javax.inject.Inject;

import com.iris.client.model.AlarmIncidentModel;
import com.iris.oculus.modules.BaseSection;

/**
 * 
 */
public class IncidentSection extends BaseSection<AlarmIncidentModel> {
   
   @Inject
   public IncidentSection(IncidentController controller) {
      super(controller);
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "Incident";
   }

   @Override
   protected String renderLabel(AlarmIncidentModel model) {
      Date started = model.getStartTime();
      return String.format("%s [%s - %s]", started, model.getAlert(), model.getAlertState());
   }

}

