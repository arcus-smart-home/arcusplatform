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
package com.iris.oculus.modules.person.ux;

import java.util.concurrent.CancellationException;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.iris.client.model.PersonModel;
import com.iris.client.model.PlaceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.person.PersonController;
import com.iris.oculus.modules.person.dialog.CreatePersonDialog;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Toolbar;

public class PersonToolbar extends BaseToolbar<PersonModel> {

	private PersonController personController;

	private Action add      = Actions.build("Add", this::onAdd);
   private Action remove   = Actions.build("Remove", this::onRemove);

   public PersonToolbar(PersonController controller) {
      personController = controller;
      controller.addSelectedListener(this);
   }

   @Override
	protected JPanel createComponent() {
		return
			Toolbar
				.builder()
				.left().addButton(add)
				.right().addButton(remove)
				.right().addButton(refresh())
				.right().addButton(save())
				.build();
	}

	public void onAdd() {
   	add.setEnabled(false);

		CreatePersonDialog.prompt()
			.onCompletion((result) -> {
				add.setEnabled(true);

				if (result.isError() && !(result.getError() instanceof CancellationException)) {
					// Shouldn't really happen, but just in case something funky is going on.
					Oculus.showError("Error adding person", result.getError());
				}
				else {
					PlaceModel currentPlace = personController.getCurrentPlace();
					if (currentPlace != null) {

						currentPlace.addPerson(result.getValue(), (String) result.getValue().get("password"))
							.onSuccess((response) -> {
									Oculus.info("Person created. " + result.getValue());
								})
							.onFailure((error) -> {
									Oculus.showError("Error creating person", error);
								});
					} else {
						Oculus.showDialog(
								"Error",
								"Current place not found in store.  Cannot add person to a null place.",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			});
   }

   public void onRemove() {
		remove.setEnabled(false);
   	model()
   		.delete()
   		.onFailure((error) -> Oculus.error("Unable to remove person. ", error))
   		.onCompletion((r) -> remove.setEnabled(model() != null))
   		;
   }

	@Override
	public void setModel(PersonModel model) {
		super.setModel(model);
		remove.setEnabled(true);
	}
	
	@Override
	public void clearModel() {
		super.clearModel();
		remove.setEnabled(false);
	}
}

