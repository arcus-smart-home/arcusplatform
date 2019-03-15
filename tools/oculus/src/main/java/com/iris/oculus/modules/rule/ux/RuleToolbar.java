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
package com.iris.oculus.modules.rule.ux;

import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.RuleModel;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.rule.RuleController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Toolbar;

/**
 * @author tweidlin
 *
 */
public class RuleToolbar extends BaseToolbar<RuleModel> {
   private Action create;
   private Action enable;
   private Action disable;
   private Action update;
   private Action delete;

   private JButton toggleButton = new JButton(enable);
   private ListenerRegistration listener;

   public RuleToolbar(RuleController controller) {
      this.create = Actions.build("New Rule", controller::createRuleFromTemplate);
      this.enable = Actions.build("Enable Rule", (Supplier<RuleModel>) this::model, controller::enable);
      this.disable = Actions.build("Disable Rule", (Supplier<RuleModel>) this::model, controller::disable);
      this.update = Actions.build("Edit Rule", (Supplier<RuleModel>) this::model, controller::update);
      this.delete = Actions.build("Delete Rule", (Supplier<RuleModel>) this::model, controller::delete);
   }

   
   @Override
   protected JPanel createComponent() {
      return
         Toolbar
            .builder()
            .left().addButton(create)
            .right().addComponent(toggleButton)
            .right().addButton(update)
            .right().addButton(delete)
            .build();
   }


   @Override
   protected void setModel(RuleModel model) {
      super.setModel(model);
      enable.setEnabled(true);
      disable.setEnabled(true);
      update.setEnabled(true);
      delete.setEnabled(true);
      syncToggleButtonState(model);
      if(listener != null) {
         listener.remove();
      }
      listener = model.addListener((p) -> syncToggleButtonState(model));
   }

   @Override
   protected void clearModel() {
      super.clearModel();
      enable.setEnabled(false);
      disable.setEnabled(false);
      update.setEnabled(false);
      delete.setEnabled(false);
      if(listener != null) {
         listener.remove();
         listener = null;
      }
   }

   private void syncToggleButtonState(RuleModel model) {
      if(RuleModel.STATE_ENABLED.equals(model.getState())) {
         toggleButton.setAction(disable);
      }
      else {
         toggleButton.setAction(enable);
      }
   }

}

