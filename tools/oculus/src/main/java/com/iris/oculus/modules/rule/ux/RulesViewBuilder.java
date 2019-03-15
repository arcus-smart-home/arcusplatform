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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.iris.client.model.RuleModel;
import com.iris.client.model.Store;
import com.iris.oculus.modules.rule.RuleController;
import com.iris.oculus.widget.table.Table;

/**
 *
 */
public class RulesViewBuilder {
   private Store<RuleModel> rules;
   
   private RuleToolbar toolbar;
   
   public RulesViewBuilder(RuleController controller) {
      this.rules = controller.getStore();
      this.toolbar = new RuleToolbar(controller);
   }
   
   protected Component createContents() {
      Table<RuleModel> ruleTable = null; //new RuleTableBuilder(rules).build();
      ruleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      ruleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) {
               return;
            }
            int index = ruleTable.getSelectionModel().getMinSelectionIndex();
            // TODO this should probably go through the controller
            if(index == -1) {
               toolbar.clearModel();
            }
            else {
               RuleModel model = ruleTable.getModel().getValue(index);
               toolbar.setModel(model);
            }
         }
      });
      return new JScrollPane(ruleTable);
   }
   
   protected Component createToolbar() {
      return toolbar.getComponent();
   }
   
   public Component build() {
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setPreferredSize(new Dimension(600, 450));
//      panel.add(createModelSelector(), BorderLayout.NORTH);
      panel.add(createContents(), BorderLayout.CENTER);
      panel.add(createToolbar(), BorderLayout.SOUTH);
      return panel;
   }
   
}

