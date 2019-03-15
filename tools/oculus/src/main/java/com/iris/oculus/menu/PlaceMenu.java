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
package com.iris.oculus.menu;

import java.awt.event.ActionEvent;

import javax.inject.Inject;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;

import com.iris.oculus.modules.place.PlaceController;
import com.iris.oculus.util.BaseComponentWrapper;

public class PlaceMenu extends BaseComponentWrapper<JMenu> {
	private String label = "Places";

   private PlaceController placeController;
   private Action addPlace;
   private Action deletePlace;
   private Action selectPlace;
   private Action validateAddress;

   @Inject
   public PlaceMenu(PlaceController placeController) {
      this.placeController = placeController;
   }

   @SuppressWarnings("serial")
	@Override
   protected JMenu createComponent() {
      JMenu menu = new JMenu(label);
      addPlace = placeController.actionAddPlace();
      deletePlace = placeController.actionDeletePlace();
      selectPlace = new AbstractAction("Select Place") {
			@Override
			public void actionPerformed(ActionEvent e) {
				PlaceMenu.this.placeController.promptForSelectPlace().
				   onSuccess((p) -> { PlaceMenu.this.placeController.selectPlace(p); });
			}
		};
      validateAddress = placeController.actionValidateAddress();

      menu.add(addPlace);
      menu.addSeparator();
      menu.add(deletePlace);
      menu.addSeparator();
      menu.add(selectPlace);
      menu.addSeparator();
      menu.add(validateAddress);

      return menu;
   }
}

