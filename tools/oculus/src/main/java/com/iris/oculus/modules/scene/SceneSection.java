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
package com.iris.oculus.modules.scene;

import java.awt.Component;

import javax.inject.Inject;

import com.iris.client.model.SceneModel;
import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.capability.ux.CapabilityViewBuilder;
import com.iris.oculus.modules.scene.ux.SceneToolbar;

/**
 * 
 */
public class SceneSection implements OculusSection {
   private SceneController controller;
   
   @Inject
   public SceneSection(SceneController controller) {
      this.controller = controller;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "Scenes";
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getComponent()
    */
   @Override
   public Component getComponent() {
      SceneToolbar toolbar = new SceneToolbar(controller);
      return 
            CapabilityViewBuilder
               .builder(controller.getScenes())
               .withTypeName("Scene")
               .withModelSelector(
                     SceneModel::getName,
                     controller.getSceneSelector(),
                     controller.actionReloadRules()
               )
               .withToolbarComponent(toolbar)
               .build();
   }

}

