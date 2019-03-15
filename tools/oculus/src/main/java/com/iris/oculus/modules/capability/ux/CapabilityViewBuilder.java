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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang3.StringUtils;

import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.client.capability.Device;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.model.Model;
import com.iris.client.model.Store;
import com.iris.oculus.modules.models.ModelController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.SelectionModel;
import com.iris.oculus.view.ViewModel;
import com.iris.oculus.view.ViewModels;
import com.iris.oculus.widget.ComboBoxBuilder;

/**
 *
 */
public class CapabilityViewBuilder<M extends Model> {
   
   public static <M extends Model> CapabilityViewBuilder<M>  builder(Store<M> store) {
      return new CapabilityViewBuilder<>(ViewModels.from(store));
   }
   
   public static <M extends Model> CapabilityViewBuilder<M>  builder(ViewModel<M> view) {
      return new CapabilityViewBuilder<>(view);
   }
   
   private String typeName = "Model";
   private ModelTableView view;
   private Component selector;
   private Component toolbar;
   private ViewModel<M> selectorView;
   
   private SelectionModel<M> selectionModel = null;
   private ListenerList<ComponentEvent> showListeners = new ListenerList<>();
   
   public CapabilityViewBuilder(ViewModel<M> model) {
      this.selectorView = model;
      this.view = new ModelTableView();
   }
   
   public CapabilityViewBuilder<M> sortedBy(Comparator<? super M> comparator) {
      this.selectorView = ViewModels.sort(selectorView, comparator);
      return this;
   }
   
   public CapabilityViewBuilder<M> withTypeName(String typeName) {
      this.typeName = typeName;
      return this;
   }
   
   public CapabilityViewBuilder<M> withEmptyView(Component emptyView) {
      this.view.setEmptyComponent(emptyView);
      return this;
   }
   
   public CapabilityViewBuilder<M> withCapabilitesAsEmptyView(
         String header,
         String baseType
   ) {
      StringBuilder sb = new StringBuilder("<html><b>" + header + "</b>");
      for(CapabilityDefinition definition: view.getDefinitionRegistry().getCapabilities()) {
         if(Device.NAME.equals(definition.getEnhances()) || Device.NAME.equals(definition.getName())) {
            sb.append("<br/>&nbsp;&nbsp;")
               .append(definition.getName())
               .append(" (").append(definition.getNamespace()).append(")")
               ;
         }
      }
      sb.append("</html>");
      return withEmptyView(new JLabel(sb.toString()));
   }
   
   public CapabilityViewBuilder<M> withModelSelector(
         String attributeName,
         SelectionModel<M> selectionModel,
         Action refreshAction
   ) {
      return withModelSelector(attributeName, selectionModel, refreshAction, null);
   }
   
   public CapabilityViewBuilder<M> withModelSelector(
         String attributeName,
         SelectionModel<M> selectionModel,
         Action refreshAction,
         Component info
   ) {
      return withModelSelector(
            (model) -> Objects.toString(model.get(attributeName), null), 
            selectionModel, 
            refreshAction,
            info
      );
   }
   
   public CapabilityViewBuilder<M> withModelSelector(
         Function<M, String> labelRenderer,
         SelectionModel<M> selectionListener,
         Action refreshAction
   ) {
      return withModelSelector(labelRenderer, selectionListener, refreshAction, null);
   }
   
   public CapabilityViewBuilder<M> withModelSelector(
         Function<M, String> labelRenderer,
         SelectionModel<M> selectionModel,
         Action refreshAction,
         Component info
   ) {
      JPanel selectorBar = new JPanel();
      selectorBar.setLayout(new GridBagLayout());
      
      GridBagConstraints gbc = new GridBagConstraints();
      
      gbc.weightx = 0.0;
      gbc.insets = new Insets(0, 4, 0, 4);
      JLabel label = new JLabel(this.typeName + "s: ");
      selectorBar.add(label, gbc.clone());
      
      gbc.insets = new Insets(0, 0, 0, 0);
      JComboBox<? extends Model> selector =
            ComboBoxBuilder
               .from(selectorView)
               .noteditable()
               .withRenderer((model) -> {
                  if(model != null) {
                     String labelText = labelRenderer.apply(model);
                     if(StringUtils.isEmpty(labelText)) {
                        labelText = model.getAddress();
                     }
                     return labelText;
                  }
                  
                  if(selectorView.size() == 0) {
                     return "<No " + typeName + "s>";
                  }
                  else {
                     return "<Select a " + typeName + ">";
                  }
               })
               .create();

      label.setLabelFor(selector);
      selector.setMinimumSize(new Dimension(250, selector.getMinimumSize().height));
      
      selector.addItemListener(new ItemListener() {
         @Override
         public void itemStateChanged(ItemEvent e) {
            if(e.getStateChange() == ItemEvent.SELECTED) {
               M model = (M) e.getItem();
               if(model != null) {
                  selectionModel.setSelection(model);
               }
            }
         }
      });
      selectionModel.addSelectionListener((reference) -> {
         if(reference.isPresent()) {
            M selected = reference.get();
            if(!Objects.equals(selector.getSelectedItem(), selected)) {
               selector.setSelectedItem(selected);
            }
         }
         else {
            selector.setSelectedItem(null);
         }
      });

      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      selectorBar.add(selector, gbc.clone());
      
      gbc.weightx = 0.0;
      JButton refresh = new JButton(refreshAction);
      if(refreshAction != null) {
         selectorBar.add(refresh, gbc.clone());
      }
      
      Action action = 
            Actions
               .builder()
               .withAction((e) -> ServiceLocator.getInstance(ModelController.class).show(selectionModel.getSelectedItem().get()))
               .withSmallIcon("classpath:/icons/expand.png")
               .build()
               ;
      action.setEnabled(false);
      selectionModel.addSelectionListener((o) -> action.setEnabled(o.isPresent()));
      JButton expand = new JButton(action);
      expand.setToolTipText("Pop Into New Window");
      gbc.fill = GridBagConstraints.BOTH;
      selectorBar.add(new JButton(action), gbc.clone());
      
      if(info != null) {
         gbc.insets = new Insets(0, 4, 4, 4);
         gbc.gridy = 1;
         gbc.weightx = 1.0;
         gbc.gridwidth = selectorBar.getComponentCount();
         selectorBar.add(info, gbc.clone());
      }
      
      return withSelectorComponent(selectorBar, selectionModel);
   }
   
   public CapabilityViewBuilder<M> withSelectorComponent(Component topBar, SelectionModel<M> selectionModel) {
      this.selector = topBar;
      this.selectionModel = selectionModel;
      return this;
   }
   
   // TODO gener-ify this a bit as well
   public CapabilityViewBuilder<M> withToolbarComponent(Component toolbar) {
      this.toolbar = toolbar;
      return this;
   }
   
   public CapabilityViewBuilder<M> addShowListener(Listener<? super ComponentEvent> listener) {
      this.showListeners.addListener(listener);
      return this;
   }
   
   protected Component createCapabilityTable() {
      if(selectionModel == null) {
         throw new IllegalStateException("No selection model has been configured for this view");
      }
      view.bind(selectionModel);
      JScrollPane pane = new JScrollPane(view.getComponent());
      return pane;
   }
   
   public Component build() {
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setPreferredSize(new Dimension(600, 450));
      if(selector != null) {
         panel.add(selector, BorderLayout.NORTH);
      }
      panel.add(createCapabilityTable(), BorderLayout.CENTER);
      if(toolbar != null) {
         panel.add(toolbar, BorderLayout.SOUTH);
      }
      if(showListeners.hasListeners()) {
         panel.addComponentListener(new ComponentAdapter() {
   
            /* (non-Javadoc)
             * @see java.awt.event.ComponentAdapter#componentShown(java.awt.event.ComponentEvent)
             */
            @Override
            public void componentShown(ComponentEvent e) {
               showListeners.fireEvent(e);
            }
            
         });
      }
      
      
      return panel;
   }
   
}

