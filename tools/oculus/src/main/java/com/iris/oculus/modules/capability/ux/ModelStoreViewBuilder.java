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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.bootstrap.ServiceLocator;
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
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;

/**
 * A replacement for CapabilityViewBuilder which uses a table on the front
 * view instead of a drop-down selector.
 */
public class ModelStoreViewBuilder<M extends Model> {
   
   public static <M extends Model> ModelStoreViewBuilder<M>  builder(Store<M> store) {
      return new ModelStoreViewBuilder<>(ViewModels.from(store));
   }
   
   public static <M extends Model> ModelStoreViewBuilder<M>  builder(ViewModel<M> view) {
      return new ModelStoreViewBuilder<>(view);
   }
   
   private String typeName = "Model";
   private ModelTableView view;
   private Component selector;
   private Component toolbar;
   private ViewModel<M> viewModel;
   
   private Component selectorView;
   
   private SelectionModel<M> selectionModel = null;
   private ListenerList<ComponentEvent> showListeners = new ListenerList<>();
   
   public ModelStoreViewBuilder(ViewModel<M> model) {
      this.viewModel = model;
      this.view = new ModelTableView();
   }
   
   public ModelStoreViewBuilder<M> sortedBy(Comparator<? super M> comparator) {
      this.viewModel = ViewModels.sort(viewModel, comparator);
      return this;
   }
   
   public ModelStoreViewBuilder<M> withTypeName(String typeName) {
      this.typeName = typeName;
      return this;
   }
   
   public ModelStoreViewBuilder<M> withListView(Component selectorView) {
      this.selectorView = selectorView;
      return this;
   }
   
   public ModelStoreViewBuilder<M> withListTableModel(TableModel<M> model, SelectionModel<M> selector) {
      return withListView(createSelectionTable(model, selector));
   }
   
	public ModelStoreViewBuilder<M> withListTableModel(
   		TableModel<M> model, 
   		SelectionModel<M> selector,
   		Component actions
	) {
   	JPanel contents = new JPanel(new BorderLayout());
   	contents.add(createSelectionTable(model, selector), BorderLayout.CENTER);
   	contents.add(actions, BorderLayout.SOUTH);
      return withListView(contents);
   }
   
   public ModelStoreViewBuilder<M> withModelSelector(
         String attributeName,
         SelectionModel<M> selectionModel,
         Action refreshAction
   ) {
      return withModelSelector(attributeName, selectionModel, refreshAction, null);
   }
   
   public ModelStoreViewBuilder<M> withModelSelector(
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
   
   public ModelStoreViewBuilder<M> withModelSelector(
         Function<M, String> labelRenderer,
         SelectionModel<M> selectionListener,
         Action refreshAction
   ) {
      return withModelSelector(labelRenderer, selectionListener, refreshAction, null);
   }
   
   public ModelStoreViewBuilder<M> withModelSelector(
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
      JButton back = new JButton("<");
      back.addActionListener((e) -> selectionModel.clearSelection());
      selectorBar.add(back, gbc.clone());
      
      JLabel label = new JLabel(this.typeName + "s: ");
      selectorBar.add(label, gbc.clone());
      
      gbc.insets = new Insets(0, 0, 0, 0);
      JComboBox<? extends Model> selector =
            ComboBoxBuilder
               .from(viewModel)
               .noteditable()
               .withRenderer((model) -> {
                  if(model != null) {
                     String labelText = labelRenderer.apply(model);
                     if(StringUtils.isEmpty(labelText)) {
                        labelText = model.getAddress();
                     }
                     return labelText;
                  }
                  
                  if(viewModel.size() == 0) {
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
   
   public ModelStoreViewBuilder<M> withSelectorComponent(Component topBar, SelectionModel<M> selectionModel) {
      this.selector = topBar;
      this.selectionModel = selectionModel;
      return this;
   }
   
   // TODO gener-ify this a bit as well
   public ModelStoreViewBuilder<M> withToolbarComponent(Component toolbar) {
      this.toolbar = toolbar;
      return this;
   }
   
   public ModelStoreViewBuilder<M> addShowListener(Listener<? super ComponentEvent> listener) {
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
   
   protected Component createSelectionTable(TableModel<M> model, SelectionModel<M> selector) {
      Table<M> table = new Table<>(model);
      table.setRowSorter(new TableRowSorter<TableModel<M>>(model));
      // TODO wrap this into a selection listener
      table.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent me) {
            if (me.getClickCount() == 2) {
               Point p = me.getPoint();
               int row = table.rowAtPoint(p);
               int offset = table.convertRowIndexToModel(row);
               M model = table.getModel().getValue(offset);
               selector.setSelection(model);
            }
        }
      });
      return new JScrollPane(table);
	}

   public Component build() {
      JPanel panel = new JPanel();
      CardLayout layout = new CardLayout();
      panel.setLayout(layout);
      panel.add(buildSelectorPanel(), "selector");
      panel.add(buildDetailPanel(), "details");
      if(showListeners.hasListeners()) {
         panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
               showListeners.fireEvent(e);
            }
            
         });
      }
      selectionModel.addSelectionListener((o) -> {
         if(o.isPresent()) {
            layout.show(panel, "details");
         }
         else {
            layout.show(panel, "selector");
         }
      });
      
      return panel;
   }
   
   private Component buildSelectorPanel() {
      Preconditions.checkState(selectorView != null, "Must specify a selectorView");
      return selectorView;
   }
   
   private Component buildDetailPanel() {
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
      return panel;
   }
   
}

