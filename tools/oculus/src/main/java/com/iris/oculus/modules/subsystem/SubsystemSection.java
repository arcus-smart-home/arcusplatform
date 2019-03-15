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
package com.iris.oculus.modules.subsystem;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Comparator;

import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.iris.client.model.SubsystemModel;
import com.iris.oculus.modules.BaseSection;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.capability.ux.ModelStoreViewBuilder;
import com.iris.oculus.util.Models;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

/**
 * 
 */
public class SubsystemSection extends BaseSection<SubsystemModel> {
   private SubsystemController controller;
   
   @Inject
   public SubsystemSection(SubsystemController controller) {
      super(controller);
      this.controller = controller;
   }

   protected TableModel<SubsystemModel> createTableModel() {
      return
            TableModelBuilder
               .builder(controller.getStore())
               .columnBuilder()
                  .withName("Name")
                  .withGetter(SubsystemModel::getName)
                  .add()
               .columnBuilder()
                  .withName("Available?")
                  .withGetter(SubsystemModel::getAvailable)
                  .add()
               .columnBuilder()
                  .withName("State")
                  .withGetter(SubsystemController::getState)
                  .add()
               .build();
   }
   
   protected JPanel createSummaryToolbar() {
      JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER));
      toolbar.add(new JButton(controller.actionReload()));
      JButton flush = new JButton(controller.actionFlushSubsystems());
      flush.setToolTipText("EXPERT -- This flushes the subsystem from server side cache, simulating a server restart or low-activity place");
      toolbar.add(flush);
      return toolbar;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      // TODO Auto-generated method stub
      return "Subsystems";
   }

   @Override
   protected Component createComponent() {
      BaseToolbar<SubsystemModel> toolbar = createToolbar();
      controller.addSelectedListener(toolbar);
      return 
            ModelStoreViewBuilder
               .builder(controller.getStore())
               .sortedBy(Comparator.comparing(SubsystemModel::getName))
               .withTypeName("Subsystem")
               .withListTableModel(
                     createTableModel(), 
                     controller.getSelection(),
                     createSummaryToolbar()
               )
               .withModelSelector(
                     Models::nameOf,
                     controller.getSelection(),
                     controller.actionReload()
               )
               .withToolbarComponent(toolbar.getComponent())
               .addShowListener((e) -> controller.reload())
               .build();
   }

}

