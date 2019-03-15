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
package com.iris.oculus.modules.place;

import java.awt.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JTextPane;

import com.iris.client.IrisClientFactory;
import com.iris.client.model.PlaceModel;
import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.capability.ux.CapabilityViewBuilder;
import com.iris.oculus.modules.place.ux.PlaceToolbar;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 * @author tweidlin
 *
 */
@Singleton
public class PlaceSection extends BaseComponentWrapper<Component> implements OculusSection {
   private PlaceController controller;
   private PlaceAddDevicesDialog dialog;
   
   @Inject
   public PlaceSection(PlaceController controller,PlaceAddDevicesController adController) {
      this.controller = controller;
      this.dialog = new PlaceAddDevicesDialog(adController);
   }

   protected Component createInfoBar() {
      JTextPane info = new JTextPane();
      info.setEditable(false);
      info.setText("Loading place...");
      controller.getActivePlace().addSelectionListener((place) -> {
         if(!place.isPresent()) {
            info.setText("No active place");
            return;
         }
         
         info.setText(place.get().getName() + " [" + place.get().getServiceLevel() + "]");
      });
      return info;
   }
   
   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "Current Place";
   }

   @Override
   protected Component createComponent() {
      return
            CapabilityViewBuilder
               .builder(IrisClientFactory.getStore(PlaceModel.class))
               .withTypeName("Place")
               .withSelectorComponent(createInfoBar(), controller.getActivePlace())
               .withToolbarComponent(new PlaceToolbar(controller))
               .addShowListener((event) -> controller.refresh())
               .build();
   }

}

