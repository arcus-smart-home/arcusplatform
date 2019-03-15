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
package com.iris.oculus.modules.capability.ux;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXCollapsiblePane.Direction;

import com.google.common.base.Preconditions;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.client.model.Model;

public class CapabilityTableBuilder {
   
   public static CapabilityTableBuilder builder() {
      return new CapabilityTableBuilder();
   }
   
   public static Component createCapabilityTable(CapabilityDefinition definition, Model model, MultiTableSelectionListener selectionHandler) {
      return
            builder()
               .withDefinition(definition)
               .withModel(model)
               .withSelectionHandler(selectionHandler)
               .build();
   }
   
   public static Component createInstanceTable(String instanceId, CapabilityDefinition definition, Model model, MultiTableSelectionListener selectionHandler) {
      return
            builder()
               .withDefinition(definition)
               .withModel(model)
               .withSelectionHandler(selectionHandler)
               .withInstance(instanceId)
               .build();
   }
   
   private CapabilityDefinition definition;
   private Model model;
   private MultiTableSelectionListener selectionHandler;
   private String instanceId;
   
   public CapabilityTableBuilder withDefinition(CapabilityDefinition definition) {
      this.definition = definition;
      return this;
   }
   
   public CapabilityTableBuilder withModel(Model model) {
      this.model = model;
      return this;
   }
   
   public CapabilityTableBuilder withSelectionHandler(MultiTableSelectionListener selectionHandler) {
      this.selectionHandler = selectionHandler;
      return this;
   }
   
   public boolean isInstance() {
      return instanceId != null;
   }
   
   public CapabilityTableBuilder withInstance(String instanceId) {
      this.instanceId = instanceId;
      return this;
   }
   
   public Component build() {
      Preconditions.checkNotNull(definition);
      Preconditions.checkNotNull(model);
      Preconditions.checkNotNull(selectionHandler);
      
      CapabilityTable table = 
            isInstance() ?
                  new CapabilityInstanceTable(definition, instanceId) :
                  new CapabilityTable(definition);
      table.setAttributes(model);
      selectionHandler.add(table.getSelectionModel());
      
      JXCollapsiblePane pane = new JXCollapsiblePane(Direction.UP);
      pane.setAnimated(true);
      pane.add(table);
      
      JLabel title = new JLabel(createTitle());
      title.setIcon(UIManager.getIcon("Tree.expandedIcon"));
      title.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            // TODO check left key
            boolean collapse = !pane.isCollapsed();
            pane.setCollapsed(collapse);
            String icon = collapse ? "Tree.collapsedIcon" : "Tree.expandedIcon";
            title.setIcon(UIManager.getIcon(icon));
         }
      });
      pane.addPropertyChangeListener(
            JXCollapsiblePane.ANIMATION_STATE_KEY, 
            (e) -> {
               if("expanded".equals(e.getNewValue())) {
                  title.setIcon(UIManager.getIcon("Tree.expandedIcon"));
               }
               else if("collapsed".equals(e.getNewValue())) {
                  title.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
               }
            }
      );
      
      JPanel header = new JPanel();
      header.setLayout(new GridBagLayout());
      
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridy = 0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      gbc.anchor = GridBagConstraints.WEST;
      header.add(title, gbc.clone());
      
      Icon icon = UIManager.getIcon("Menu.arrowIcon");
      JButton button = new JButton(icon);
      gbc.anchor = GridBagConstraints.EAST;
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      header.add(button, gbc.clone());
      
      CapabilityMenuBinder menu = CapabilityMenuBinder.get(definition, instanceId);
      button.addActionListener((e) -> menu.bindAndShow(model, button, button.getWidth(), 0));
      
      
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(header, BorderLayout.NORTH);
      panel.add(pane, BorderLayout.CENTER);

      return panel;
   }

   private String createTitle() {
      if(isInstance()) {
         return definition.getName() + "(" + definition.getNamespace() + "): " + instanceId;
      }
      else {
         return definition.getName() + "(" + definition.getNamespace() + ")";
      }
   }
}

