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
package com.iris.oculus.modules.scene.ux;

import java.util.List;

import com.iris.client.bean.ActionTemplate;
import com.iris.client.capability.Scene;
import com.iris.client.model.SceneModel;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.FormView;

/**
 * 
 */
public class SceneEditorForm extends FormView {
   
   public static SceneEditorForm create(List<ActionTemplate> templates) {
      return new SceneEditorForm(templates);
   }
   
   public static SceneEditorForm edit(List<ActionTemplate> templates, SceneModel scene) {
      SceneEditorForm form = create(templates);
      form.setValues(scene.toMap());
      return form;
   }
   
   private ActionsField actions;
 
   protected SceneEditorForm(List<ActionTemplate> templates) {
      this.actions = new ActionsField(Scene.ATTR_ACTIONS, templates);
      addField(
            Fields
               .textFieldBuilder()
               .labelled("Name")
               .named(Scene.ATTR_NAME)
               .build()
      );
      addField(
            Fields
               .checkBoxBuilder()
               .labelled("Send Notifications")
               .named(Scene.ATTR_NOTIFICATION)
               .build()
      );
      addSeparator();
      addField(actions, LabelLocation.NONE);
   }
   
   // TODO verify whether its already added?
   public void addAction(ActionTemplate template) {
      actions.addAction(template);
   }

}

