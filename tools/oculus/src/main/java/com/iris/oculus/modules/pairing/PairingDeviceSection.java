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
package com.iris.oculus.modules.pairing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang3.StringUtils;

import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.client.capability.PairingDeviceMock;
import com.iris.client.model.PairingDeviceModel;
import com.iris.client.model.RuleModel;
import com.iris.oculus.modules.BaseSection;
import com.iris.oculus.modules.capability.ux.ModelStoreViewBuilder;
import com.iris.oculus.modules.pairing.ux.PairingToolbar;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.util.Models;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

/**
 * 
 */
public class PairingDeviceSection extends BaseSection<PairingDeviceModel> {
   private PairingDeviceController controller;

   @Inject
   public PairingDeviceSection(PairingDeviceController controller) {
      super(controller);
      this.controller = controller;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "Pairing";
   }

   @Override
   protected String renderLabel(PairingDeviceModel model) {
      String productName = !StringUtils.isEmpty(model.getProductAddress()) ? Models.nameOf(model.getProductAddress()) : "Discovering";
      return String.format("%s [%s]", productName, model.getPairingState());
   }

   @Override
   protected Component createComponent() {
      PairingToolbar toolbar = new PairingToolbar(controller, ServiceLocator.getInstance(SessionController.class));
      controller.getSelection().addSelectionListener((o) -> toolbar.onEvent(o.orNull()));
      return 
         ModelStoreViewBuilder
            .builder(controller.getStore())
            .withTypeName("Product")
            .withModelSelector(
                  this::renderLabel,
                  controller.getSelection(),
                  controller.actionReload()
            )
            .withListView(createSelectionTable())
            .withToolbarComponent(toolbar.getComponent())
            .addShowListener((e) -> controller.reload())
            .build();
   }

   private TableModel<PairingDeviceModel> createTableModel() {
      return 
         TableModelBuilder
            .builder(controller.getStore())
            .columnBuilder()
               .withName("Address")
               .withGetter(PairingDeviceModel::getAddress)
               .add()
            .columnBuilder()
               .withName("Product Type")
               .withGetter((model) -> !StringUtils.isEmpty(model.getProductAddress()) ? Models.nameOf(model.getProductAddress()) : null)
               .withRenderer((c, product, isSelected, hasFocus) -> {
                  JLabel label = (JLabel) c;
                  if(product == null) {
                     if(!isSelected) {
                        label.setForeground(UIManager.getColor("Label.disabledForeground"));
                     }
                     label.setFont(label.getFont().deriveFont(Font.ITALIC));
                     label.setText("Unknown");
                  }
                  else {
                     if(!isSelected) {
                        label.setForeground(UIManager.getColor("Label.foreground"));
                     }
                     label.setFont(label.getFont().deriveFont(Font.PLAIN));
                     label.setText((String) product);
                  }
               })
               .add()
            .columnBuilder()
               .withName("Pairing State")
               .withGetter(PairingDeviceModel::getPairingState)
               .add()
            .columnBuilder()
               .withName("Pairing Phase")
               .withGetter(PairingDeviceModel::getPairingPhase)
               .add()
            .columnBuilder()
               .withName("Device")
               // FIXME render a link here
               .withGetter((model) -> !StringUtils.isEmpty(model.getDeviceAddress()) ? Models.nameOf(model.getDeviceAddress()) : null)
               .withRenderer((c, name, isSelected, hasFocus) -> {
                  JLabel label = (JLabel) c;
                  if(name == null) {
                     if(!isSelected) {
                        label.setForeground(UIManager.getColor("Label.disabledForeground"));
                     }
                     label.setFont(label.getFont().deriveFont(Font.ITALIC));
                     label.setText("No Driver");
                  }
                  else {
                     if(!isSelected) {
                        label.setForeground(UIManager.getColor("Label.foreground"));
                     }
                     label.setFont(label.getFont().deriveFont(Font.PLAIN));
                     label.setText((String) name);
                  }
               })
               .add()
            .columnBuilder()
               .withName("Mock")
               .withGetter((model) -> model.getCaps().contains(PairingDeviceMock.NAMESPACE) ? "Yes" : "No")
               .add()
            .build();
   }

   protected Component createSelectionTable() {
      PairingToolbar toolbar = new PairingToolbar(controller, ServiceLocator.getInstance(SessionController.class));
      TableModel<PairingDeviceModel> model = createTableModel();
      Table<PairingDeviceModel> table = new Table<>(model);
      table.setRowSorter(new TableRowSorter<TableModel<PairingDeviceModel>>(model));
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      // toolbar selector
      table.getSelectionModel().addListSelectionListener((e) -> {
         if(e.getValueIsAdjusting()) {
            return;
         }
         int index = table.getSelectionModel().getMinSelectionIndex();
         // TODO this should probably go through the controller
         if(index == -1) {
            toolbar.onEvent(null);
         }
         else {
            toolbar.onEvent( table.getModel().getValue(index) );
         }
      });
      table.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent me) {
            if (me.getClickCount() == 2) {
               Point p = me.getPoint();
               int row = table.rowAtPoint(p);
               int offset = table.convertRowIndexToModel(row);
               PairingDeviceModel model = table.getModel().getValue(offset);
               controller.getSelection().setSelection(model);
            }
        }
      });

      JPanel contents = new JPanel(new BorderLayout());
      contents.add(new JScrollPane(table), BorderLayout.CENTER);
      contents.add(toolbar.getComponent(), BorderLayout.SOUTH);
      return contents;
   }

}

