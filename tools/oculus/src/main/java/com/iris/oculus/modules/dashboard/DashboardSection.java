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
package com.iris.oculus.modules.dashboard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.inject.Inject;
import com.iris.client.bean.HistoryLog;
import com.iris.oculus.OculusSection;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

public class DashboardSection extends BaseComponentWrapper<Component> implements OculusSection {
   private DashboardController controller;
   
   @Inject
   public DashboardSection(DashboardController controller) {
      this.controller = controller;
   }

   @Override
   public String getName() {
      return "Welcome";
   }

   protected Table<HistoryLog> createLogTable() {
      TableModel<HistoryLog> history =
            TableModelBuilder
               .builder(controller.getLogs())
               .columnBuilder()
                  .withName("Time")
                  .withGetter((entry) -> entry.getTimestamp())
                  .add()
               .columnBuilder()
                  .withName("Key")
                  .withGetter((entry) -> entry.getKey())
                  .add()
               .columnBuilder()
                  .withName("Address")
                  .withGetter((entry) -> entry.getSubjectAddress())
                  .add()
               .columnBuilder()
                  .withName("Name")
                  .withGetter((entry) -> entry.getSubjectName())
                  .add()
               .columnBuilder()
                  .withName("Short")
                  .withGetter((entry) -> entry.getShortMessage())
                  .add()
               .columnBuilder()
                  .withName("Long")
                  .withGetter((entry) -> entry.getLongMessage())
                  .add()
               .build()
               ;
      return new Table<>(history);
   }
   
   protected JPanel createToolbar() {
      JPanel toolbar = new JPanel();
      toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
      toolbar.add(new JButton(controller.refreshAction()));
      toolbar.add(Box.createHorizontalGlue());
      toolbar.add(new JButton(controller.previousAction()));
      toolbar.add(new JButton(controller.nextAction()));
      return toolbar;
   }
   
   @Override
   protected Component createComponent() {
      JPanel content = new JPanel(new BorderLayout());
      content.add(new JScrollPane(createLogTable()), BorderLayout.CENTER);
      content.add(createToolbar(), BorderLayout.SOUTH);
      content.addComponentListener(new ComponentAdapter() {

         /* (non-Javadoc)
          * @see java.awt.event.ComponentAdapter#componentShown(java.awt.event.ComponentEvent)
          */
         @Override
         public void componentShown(ComponentEvent e) {
            controller.refreshLogs();
         }
         
      });
      return content;
   }
}

