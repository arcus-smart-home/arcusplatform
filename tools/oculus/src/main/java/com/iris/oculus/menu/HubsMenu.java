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
package com.iris.oculus.menu;

import javax.inject.Inject;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.iris.client.IrisClientFactory;
import com.iris.client.model.HubModel;
import com.iris.client.model.Store;
import com.iris.oculus.modules.hub.HubController;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 *
 */
public class HubsMenu extends BaseComponentWrapper<JMenu> {
   private String label = "Hubs";

   private HubController hubController;
   private JMenuItem registerHub;
   private JMenuItem registerHubLegacy;
   private JMenuItem reloadHub;
   private JMenuItem decodeHubInfo;

   @Inject
   public HubsMenu(HubController hubController) {
      this.hubController = hubController;
   }

   @Override
   protected JMenu createComponent() {
      JMenu menu = new JMenu(label);
      registerHub = new JMenuItem(hubController.actionRegisterHub());
      registerHubLegacy = new JMenuItem(hubController.actionRegisterHubLegacy());
      reloadHub = new JMenuItem(hubController.actionReloadHubs());
      decodeHubInfo = new JMenuItem(hubController.actionDecodeHubInfo());

      Store<HubModel> hubs = IrisClientFactory.getStore(HubModel.class);
      rebuild(hubs, menu);
      hubs.addListener((event) -> rebuild(hubs, menu));

      return menu;
   }

   private void rebuild(Store<HubModel> hubs, JMenu menu) {
      menu.removeAll();
      menu.add(registerHub);
      menu.add(registerHubLegacy);
      menu.add(reloadHub);
      menu.addSeparator();
      menu.add(decodeHubInfo);
      menu.addSeparator();

      if(hubs.size() == 0) {
         JMenuItem emptyMenu = new JMenuItem("Account has no hubs");
         emptyMenu.setEnabled(false);
         menu.add(emptyMenu);
      }
      else {
         for(HubModel hub: hubs.values()) {
            menu.add(new HubMenu(hub));
         }
      }
   }

   private class HubMenu extends JMenu {
      private HubModel model;

      private HubMenu(HubModel model) {
         super(model.getId());
         this.model = model;

         for(Action action: hubController.createHubActions(model)) {
            add(new JMenuItem(action));
         }
      }


   }

}

