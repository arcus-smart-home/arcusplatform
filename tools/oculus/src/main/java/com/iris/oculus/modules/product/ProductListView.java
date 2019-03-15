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
package com.iris.oculus.modules.product;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.ProductModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.Documents;
import com.iris.oculus.util.OperationAdapter;
import com.iris.oculus.util.SelectionModel;
import com.iris.oculus.view.ViewModels;
import com.iris.oculus.widget.ComboBoxBuilder;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

public class ProductListView extends BaseComponentWrapper<JPanel> {
   private final ProductController controller;
   private final FilteredProductStore store;
   private final SelectionModel<ProductModel> selectionModel = new DefaultSelectionModel<>();
   private FieldWrapper<JTextField, String> search;
   private FieldWrapper<?, Optional<Map<String, Object>>> brand;
   private FieldWrapper<?, Optional<Map<String, Object>>> category;
   private FieldWrapper<?, Optional<Boolean>> browseable;
   private FieldWrapper<?, Optional<Boolean>> certification;
   private FieldWrapper<?, Optional<Boolean>> appRequired;
   private FieldWrapper<?, String> matches;
   private Action clear;
   
   public ProductListView(ProductController controller) {
      this.controller = controller;
      this.store = new FilteredProductStore(controller);
      init();
   }
   
   private void init() {
      this.search =
         Fields
            .textFieldBuilder()
            .labelled("Search")
            .build();
      this.brand =
         Fields
            .<JComboBox<Optional<Map<String, Object>>>, Optional<Map<String, Object>>>builder(
               ComboBoxBuilder
                  .from(ViewModels.optional(controller.getBrands()))
                  .noteditable()
                  .withRenderer((o) -> o != null && o.isPresent() ? String.valueOf(o.get().get("name")) : "[Any]")
                  .addListener((brand) -> {
                     // listing by brand only includes browseable
                     if(brand.isPresent()) {
                        browseable.setValue(Optional.of(Boolean.TRUE));
                     }
                     store.setBrand(brand);
                  })
                  .create()
            )
            .withGetter((j) -> (Optional<Map<String, Object>>) j.getSelectedItem())
            .withSetter((j, o) -> { j.setSelectedItem(o); j.repaint(); }) 
            .labelled("Brand:")
            .build();
     this.category =
           Fields
           .<JComboBox<Optional<Map<String, Object>>>, Optional<Map<String, Object>>>builder(
              ComboBoxBuilder
                 .from(ViewModels.optional(controller.getCategories()))
                 .noteditable()
                 .withRenderer((o) -> o != null && o.isPresent() ? String.valueOf(o.get().get("name")) : "[Any]")
                 .addListener((category) -> {
                    // listing by brand only includes browseable
                    if(category.isPresent()) {
                       browseable.setValue(Optional.of(Boolean.TRUE));
                    }
                    store.setCategory(category);
                 })
                 .create()
           )
           .withGetter((j) -> (Optional<Map<String, Object>>) j.getSelectedItem())
           .withSetter((j, o) -> { j.setSelectedItem(o); j.repaint(); }) 
           .labelled("Category:")
           .build();
      this.browseable =
         Fields
         .<JComboBox<Optional<Boolean>>, Optional<Boolean>>builder(
               ComboBoxBuilder
                  .from(ImmutableList.<Optional<Boolean>>of(Optional.empty(), Optional.of(Boolean.TRUE), Optional.of(Boolean.FALSE)))
                  .noteditable()
                  .withRenderer((o) -> o != null && o.isPresent() ? (o.get() ? "Yes" : "No") : "[Any]")
                  .addListener((browseable) -> {
                     if(!browseable.isPresent() || !browseable.get()) {
                        search.setValue("");
                        brand.setValue(Optional.empty());
                        category.setValue(Optional.empty());
                     }
                     store.setBrowsable(browseable);
                  })
                  .create()
            )
            .withGetter((j) -> (Optional<Boolean>) j.getSelectedItem())
            .withSetter((j, o) -> { j.setSelectedItem(o); j.repaint(); }) 
            .labelled("Browseable:")
            .build();
      this.certification =
            Fields
            .<JComboBox<Optional<Boolean>>, Optional<Boolean>>builder(
                  ComboBoxBuilder
                     .from(ImmutableList.<Optional<Boolean>>of(Optional.empty(), Optional.of(Boolean.TRUE), Optional.of(Boolean.FALSE)))
                     .noteditable()
                     .withRenderer((o) -> o != null && o.isPresent() ? (o.get() ? "Works With Iris" : "Uncertified") : "[Any]")
                     .addListener(store::setCertified)
                     .create()
               )
               .withGetter((j) -> (Optional<Boolean>) j.getSelectedItem())
               .withSetter((j, o) -> { j.setSelectedItem(o); j.repaint(); }) 
               .labelled("Certification:")
               .build();
      this.appRequired =
              Fields
              .<JComboBox<Optional<Boolean>>, Optional<Boolean>>builder(
                     ComboBoxBuilder
                             .from(ImmutableList.<Optional<Boolean>>of(Optional.empty(), Optional.of(Boolean.TRUE), Optional.of(Boolean.FALSE)))
                             .noteditable()
                             .withRenderer((o) -> o != null && o.isPresent() ? (o.get() ? "True" : "False") : "[Any]")
                             .addListener(store::setAppRequired)
                             .create()
             )
             .withGetter((j) -> (Optional<Boolean>) j.getSelectedItem())
             .withSetter((j, o) -> { j.setSelectedItem(o); j.repaint(); })
             .labelled("App Required:")
             .build();
      this.matches =
         Fields
            .labelBuilder()
            .labelled("Matches: ")
            .build();
      this.clear = Actions.build("Reset", this::reset);
      Documents
         .addDocumentChangeListener(search.getComponent().getDocument(), this::search)
         .debounce(500);
      this.store.addOperationListener(new OperationAdapter<Integer>() {

         @Override
         protected void onLoading() {
            matches.getComponent().setFont(matches.getComponent().getFont().deriveFont(Font.ITALIC));
            matches.setValue("Loading");
         }

         @Override
         protected void onLoaded(Integer value) {
            matches.getComponent().setFont(matches.getComponent().getFont().deriveFont(Font.PLAIN));
            matches.setValue(Integer.toString(value));
         }

         @Override
         protected void onError(Throwable cause) {
            matches.getComponent().setFont(matches.getComponent().getFont().deriveFont(Font.ITALIC));
            matches.setValue("Error filtering products");
            Oculus.error("Error loading products", cause);
         }
      
      });
   }

   protected void reset() {
      search.setValue("");
      brand.setValue(Optional.empty());
      category.setValue(Optional.empty());
      browseable.setValue(Optional.empty());
      certification.setValue(Optional.empty());
      appRequired.setValue(Optional.empty());
   }
   
   protected void search() {
      if(!StringUtils.isEmpty(search.getValue())) {
         browseable.setValue(Optional.of(Boolean.TRUE));
      }
      store.search(search.getValue());
   }
   
   protected TableModel<ProductModel> createTableModel() {
      return
            TableModelBuilder
               .builder(store.model())
               .columnBuilder()
               .withName("Vendor")
                  .withGetter(ProductModel::getVendor)
                  .add()
               .columnBuilder()
                  .withName("Name")
                  .withGetter(ProductModel::getName)
                  .add()
               .columnBuilder()
                  .withName("Certification")
                  .withGetter((m) -> "WORKS".equals(m.getCert()) ? "Works With Iris" : "Uncertified")
                  .add()
               .columnBuilder()
                  .withName("Browseable")
                  .withGetter((m) -> Boolean.TRUE.equals(m.getCanBrowse()) ? "Yes" : "No")
                  .add()
               .columnBuilder()
                  .withName("Manufacturer")
                  .withGetter(ProductModel::getManufacturer)
                  .add()
               .build();
   }
   
   protected Component createSearchBar() {
      JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));
      filter.add(brand.getLabel());
      filter.add(brand.getComponent());
      filter.add(category.getLabel());
      filter.add(category.getComponent());
      filter.add(browseable.getLabel());
      filter.add(browseable.getComponent());
      filter.add(certification.getLabel());
      filter.add(certification.getComponent());
      filter.add(appRequired.getLabel());
      filter.add(appRequired.getComponent());
      
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(search.getLabel(), BorderLayout.WEST);
      panel.add(search.getComponent(), BorderLayout.CENTER);
      panel.add(new JButton(clear), BorderLayout.EAST);
      panel.add(filter, BorderLayout.SOUTH);
      
      return panel;
   }
   
   protected Component createStatusBar() {
      JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT));
      status.add(matches.getLabel());
      status.add(matches.getComponent());
      return status;
   }
   
   protected Component createListSelector() {
      TableModel<ProductModel> tableModel = createTableModel();
      TableRowSorter<TableModel<ProductModel>> sorter = new TableRowSorter<>(tableModel);
      sorter.setSortKeys(ImmutableList.<SortKey>of(
            new RowSorter.SortKey(0, SortOrder.UNSORTED),
            new RowSorter.SortKey(1, SortOrder.UNSORTED)
      ));
      Table<ProductModel> table = new Table<>(tableModel);
      table.setRowSorter(sorter);
      table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) {
               return;
            }
            int selected = table.getSelectedRow();
            if(selected == -1) {
               selectionModel.clearSelection();
            }
            else {
               selected = table.getRowSorter().convertRowIndexToModel(selected);
               selectionModel.setSelection(table.getModel().getValue(selected));
            }
         }
      });
      // TODO wrap this into a selection listener
      table.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent me) {
            if (me.getClickCount() == 2) {
               Point p = me.getPoint();
               int row = table.rowAtPoint(p);
               int offset = table.convertRowIndexToModel(row);
               ProductModel model = table.getModel().getValue(offset);
               controller.getProductSelection().setSelection(model);
            }
        }
      });
      return new JScrollPane(table);
   }
   
   @Override
   protected JPanel createComponent() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(createSearchBar(), BorderLayout.NORTH);
      panel.add(createListSelector(), BorderLayout.CENTER);
      panel.add(createStatusBar(), BorderLayout.SOUTH);
      return panel;
   }

   public boolean hasSelection() {
      return selectionModel.hasSelection();
   }

   public com.google.common.base.Optional<ProductModel> getSelectedItem() {
      return selectionModel.getSelectedItem();
   }

   public ListenerRegistration addSelectionListener(Listener<com.google.common.base.Optional<ProductModel>> listener) {
      return selectionModel.addSelectionListener(listener);
   }

}

