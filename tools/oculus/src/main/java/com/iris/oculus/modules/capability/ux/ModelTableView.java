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
package com.iris.oculus.modules.capability.ux;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXCollapsiblePane;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.StaticDefinitionRegistry;
import com.iris.client.capability.Capability;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.Model;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.SelectionModel;

/**
 * @author tweidlin
 *
 */
public class ModelTableView extends BaseComponentWrapper<Component> {
   
   private DefinitionRegistry registry;
   private Model model;
   private Component empty;
   private ListenerRegistration modelListener = null;
   private ListenerRegistration selectionListener = null;
   
   private boolean allCollapsed = false;
   
   public ModelTableView() {
      this(StaticDefinitionRegistry.getInstance());
   }
   
   public ModelTableView(DefinitionRegistry registry) {
      this.registry = registry;
      this.empty = defaultEmptyMessage();
   }
   
   public DefinitionRegistry getDefinitionRegistry() {
      return registry;
   }
   
   public void setEmptyComponent(Component empty) {
      if(empty == null) {
         this.empty = defaultEmptyMessage();
      }
      else {
         this.empty = empty;
      }
   }
   
   public void bind(SelectionModel<? extends Model> selectionModel) {
      if(selectionListener != null) {
         selectionListener.remove();
      }
      selectionListener = selectionModel.addSelectionListener((selection) -> this.setSelection(selection));
      this.setSelection(selectionModel.getSelectedItem());
   }
   
   protected void setSelection(Optional<? extends Model> model) {
      if(model.isPresent()) {
         setModel(model.get());
      }
      else {
         clearModel();
      }
   }
   
   protected void setModel(Model model) {
      this.model = model;
      if(!isActive()) {
         return;
      }
      showModel((JPanel) getComponent(), model);
   }
   
   protected void clearModel() {
      setModel(null);
   }
   
   public void expandAll() {
      setCollapsed(false);
   }
   
   public void collapseAll() {
      setCollapsed(true);
   }
   
   private void setCollapsed(boolean collapsed) {
      if(!isActive()) {
         return;
      }
      for(Component c: ((JPanel) getComponent()).getComponents()) {
         if(!(c instanceof JPanel)) {
            continue;
         }
         JPanel jp = (JPanel) c;
         if(!(jp.getComponents().length == 2 && jp.getComponents()[1] instanceof JXCollapsiblePane)) {
            continue;
         }
         JXCollapsiblePane collapser = (JXCollapsiblePane) jp.getComponents()[1];
         collapser.setCollapsed(collapsed);
      }
   }
   
   @Override
   protected Component createComponent() {
      JPanel contents = new JPanel();
      contents.setLayout(new GridBagLayout());
      contents.setBackground(Color.WHITE);
      
      if(model == null) {
         showEmptyMessage(contents);
      }
      else {
         showModel(contents, model);
      }
      
      return contents;
   }
   
   protected void showEmptyMessage(JPanel contents) {
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.anchor = GridBagConstraints.NORTH;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      contents.add(empty, gbc);
   }
   
   protected void showModel(JPanel contents, Model model) {
      if(modelListener != null) {
         modelListener.remove();
      }
      contents.removeAll();
      if(model == null) {
         showEmptyMessage(contents);
         return;
      }
      modelListener = model.addListener((event) -> {
         if(Capability.ATTR_CAPS.equals(event.getPropertyName())) {
            showModel(contents, model);
         }
         else if(Capability.EVENT_DELETED.equals(event.getPropertyName())) {
            clearModel();
         }
      });

      List<CapabilityDefinition> definitions = getDefinitionsFromModel(model);
      Map<String, Collection<String>> instances = model.getInstances();
      if(instances == null) {
         instances = ImmutableMap.of();
      }
      if(definitions.isEmpty() && instances.isEmpty()) {
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.BOTH;
         gbc.anchor = GridBagConstraints.NORTH;
      
         JLabel label = new JLabel("The selected model does not support any capabilities", UIManager.getIcon("OptionPane.warningIcon"), JLabel.LEADING);
         contents.add(label, gbc);
      }
      else {
         MultiTableSelectionListener selectionHandler = new MultiTableSelectionListener();
         
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridx = 0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.anchor = GridBagConstraints.NORTH;
         gbc.weightx = 1.0;
         gbc.weighty = 0.0;
         
         // add a toolbar
         Component toolbar = createToolbar();
         contents.add(toolbar, gbc.clone());
         
         // always add base first
         Component table = CapabilityTableBuilder.createCapabilityTable(Capability.DEFINITION, model, selectionHandler);
         contents.add(table, gbc.clone());

         Collections.sort(definitions, (d1, d2) -> d1.getName().compareTo(d2.getName()));
         for(CapabilityDefinition definition: definitions) {
            if(Capability.NAMESPACE.equals(definition.getNamespace())) {
               // already added
               continue;
            }
            
            table = CapabilityTableBuilder.createCapabilityTable(definition, model, selectionHandler);
            
            contents.add(table, gbc.clone());
         }
         for(Map.Entry<String, Collection<String>> instance: instances.entrySet()) {
            
            for(String cap: instance.getValue()) {
               CapabilityDefinition definition = registry.getCapability(cap);
               if(definition == null) {
                  continue;
               }
               table = CapabilityTableBuilder.createInstanceTable(instance.getKey(), definition, model, selectionHandler);
               contents.add(table, gbc.clone());
            }
         }
         gbc.weighty = 1.0;
         contents.add(new JLabel(), gbc.clone());
      }
      
      contents.revalidate();
      contents.repaint();
   }
   
   private Component createToolbar() {
      JLabel toolbar = new JLabel();
      toolbar.setBackground(Color.DARK_GRAY);
      toolbar.setIcon(UIManager.getIcon("Tree.expandedIcon"));
      toolbar.setText("Collapse All");
      toolbar.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            // TODO check left key
            allCollapsed = !allCollapsed;
            setCollapsed(allCollapsed);
            if(allCollapsed) {
               toolbar.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
               toolbar.setText("Expand All");
            }
            else {
               toolbar.setIcon(UIManager.getIcon("Tree.expandedIcon"));
               toolbar.setText("Collapse All");
            }
         }
      });
      return toolbar;
   }

   private Component defaultEmptyMessage() {
      return new JLabel("Please select a model to view the details");
   }

   private List<CapabilityDefinition> getDefinitionsFromModel(Model model) {
      List<CapabilityDefinition> definitions = new ArrayList<CapabilityDefinition>();
      Collection<String> caps = model.getCaps();
      if(caps == null || caps.isEmpty()) {
         return Collections.emptyList();
         
      }
      for(String cap: caps) {
         CapabilityDefinition definition = registry.getCapability(cap);
         if(definition != null) {
            definitions.add(definition);
         }
      }
      return definitions;
   }

}

