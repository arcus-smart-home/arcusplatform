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
package com.iris.oculus.modules.scene.ux;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.iris.client.model.SceneModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.scene.SceneController;
import com.iris.oculus.util.Actions;

public class SceneToolbar extends JPanel {
	private static final long serialVersionUID = 1L;

	private SceneController controller;
	private SceneModel model;

	private Action add      = Actions.build("New Scene", this, SceneToolbar::onAdd);
   private Action remove   = Actions.build("Delete Scene", this, SceneToolbar::onRemove);
   private Action edit     = Actions.build("Edit Scene", this, SceneToolbar::onEdit);
   private Action execute  = Actions.build("Run Scene", this, SceneToolbar::onExecute);

   public SceneToolbar(SceneController controller) {
      this.controller = controller;
      this.controller.getSceneSelector().addSelectionListener((o) -> setModel(o.orNull()));
      
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      add(new JButton(add));
      add(Box.createHorizontalGlue());
      add(new JButton(execute));
      add(new JButton(edit));
      add(new JButton(remove));

      remove.setEnabled(false);
      edit.setEnabled(false);
      execute.setEnabled(false);
   }

   public void onAdd() {
   	controller.addScene();
   }

   public void onRemove() {
		Oculus.showProgress(model.delete(), "Deleting scene...");
   }
   
   public void onEdit() {
      controller.edit(model);
   }

   public void onExecute() {
      Oculus.showProgress(model.fire(), "Executing scene...");
   }

	public void setModel(SceneModel model) {
	   this.model = model;
	   if(model == null) {
	      remove.setEnabled(false);
	      edit.setEnabled(false);
	      execute.setEnabled(false);
	   }
	   else {
         remove.setEnabled(true);
         edit.setEnabled(true);
         execute.setEnabled(true);
	   }
	}
}

