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
package com.iris.oculus.modules.hub;

import java.awt.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Hub;
import com.iris.client.capability.HubAdvanced;
import com.iris.client.model.HubModel;
import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.capability.ux.CapabilityViewBuilder;
import com.iris.oculus.modules.hub.ux.HubToolbar;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 * @author tweidlin
 *
 */
@Singleton
public class HubSection extends BaseComponentWrapper<Component> implements OculusSection {
   private HubController controller;
   // this is used by side-affect when a hub is being paired

   @Inject
   public HubSection(
         HubController controller
   ) {
      this.controller = controller;
   }

   protected Component createInfoBar() {
      JTextArea version = new JTextArea("No hub associated with this place");
      version.setBackground(UIManager.getColor("OptionPane.background"));
      version.setEditable(false);
      controller.addHubSelectedListener((model) -> {
         if(model == null) {
            version.setText("No device selected");
            return;
         }
         
         HubAdvanced hub = (HubAdvanced) model;
         String text = String.format(
               "%s v%s [%s]", 
               model.getId(),
               hub.getOsver(),
               hub.getState()
         );
         version.setText(text);
         version.invalidate();
      });
      return version;
   }
   
   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "Hubs";
   }

   @Override
   protected Component createComponent() {
      HubToolbar toolbar = new HubToolbar(controller);
      controller.addHubSelectedListener((model) -> toolbar.setModel(model));
      return
            CapabilityViewBuilder
               .builder(IrisClientFactory.getStore(HubModel.class))
               .withCapabilitesAsEmptyView("Hub Capabilities", Hub.NAMESPACE)
               .withTypeName("Hub")
               .withSelectorComponent(createInfoBar(), controller.getHubSelection())
               .withToolbarComponent(toolbar)
               .addShowListener((event) -> controller.reloadHub())
               .build();
   }

}

