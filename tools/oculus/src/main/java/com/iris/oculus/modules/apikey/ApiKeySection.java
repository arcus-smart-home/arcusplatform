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
package com.iris.oculus.modules.apikey;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import com.iris.oculus.Oculus;
import com.iris.oculus.OculusSection;

public class ApiKeySection implements OculusSection {
   private static final String[] COLUMNS = {
      "Label", "Key Prefix", "Permissions", "Created", "Last Used", "Revoked At"
   };
   private static final String[] ATTR_KEYS = {
      "label", "keyPrefix", "permissions", "created", "lastUsed", "revokedAt"
   };

   private final ApiKeyController controller;

   @Inject
   public ApiKeySection(ApiKeyController controller) {
      this.controller = controller;
   }

   @Override
   public String getName() {
      return "API Keys";
   }

   @Override
   public Component getComponent() {
      String CARD_TABLE = "table";
      String CARD_UNSUPPORTED = "unsupported";

      CardLayout cards = new CardLayout();
      JPanel cardPanel = new JPanel(cards);

      // Table card
      KeyTableModel model = new KeyTableModel();

      JTable table = new JTable(model);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      table.getTableHeader().setReorderingAllowed(false);

      JToolBar toolbar = new JToolBar();
      toolbar.setFloatable(false);

      JButton createBtn = new JButton("Create");
      createBtn.addActionListener((e) -> promptCreate());
      toolbar.add(createBtn);

      JButton revokeBtn = new JButton("Revoke");
      revokeBtn.addActionListener((e) -> {
         int row = table.getSelectedRow();
         if (row < 0) {
            JOptionPane.showMessageDialog(Oculus.getMainWindow(), "Select a key to revoke.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
         }
         List<Map<String, String>> keys = controller.getKeys();
         Map<String, String> key = keys.get(row);
         String revokedAt = key.get("revokedAt");
         if (revokedAt != null && !revokedAt.isEmpty()) {
            JOptionPane.showMessageDialog(Oculus.getMainWindow(), "This key is already revoked.", "Already Revoked", JOptionPane.INFORMATION_MESSAGE);
            return;
         }
         controller.revokeKey(key.get("id"), key.get("label"));
      });
      toolbar.add(revokeBtn);

      toolbar.addSeparator();

      JButton refreshBtn = new JButton(controller.actionReload());
      toolbar.add(refreshBtn);

      JPanel tablePanel = new JPanel(new BorderLayout());
      tablePanel.add(toolbar, BorderLayout.NORTH);
      tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
      cardPanel.add(tablePanel, CARD_TABLE);

      // Unsupported card
      JLabel unsupportedLabel = new JLabel("API Key management is not supported by the connected platform.");
      unsupportedLabel.setHorizontalAlignment(SwingConstants.CENTER);
      cardPanel.add(unsupportedLabel, CARD_UNSUPPORTED);

      // Switch cards on data change
      controller.addChangeListener(() -> {
         model.fireUpdate();
         cards.show(cardPanel, controller.isUnsupported() ? CARD_UNSUPPORTED : CARD_TABLE);
      });

      // Show correct card initially
      cards.show(cardPanel, controller.isUnsupported() ? CARD_UNSUPPORTED : CARD_TABLE);

      return cardPanel;
   }

   private void promptCreate() {
      JTextField labelField = new JTextField(20);
      JTextField permsField = new JTextField(30);

      JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 4, 4));
      form.add(new JLabel("Label:"));
      form.add(labelField);
      form.add(new JLabel("Permissions (comma-separated):"));
      form.add(permsField);

      int result = JOptionPane.showConfirmDialog(
         Oculus.getMainWindow(),
         form,
         "Create API Key",
         JOptionPane.OK_CANCEL_OPTION,
         JOptionPane.PLAIN_MESSAGE
      );
      if (result != JOptionPane.OK_OPTION) {
         return;
      }

      String label = labelField.getText().trim();
      if (label.isEmpty()) {
         JOptionPane.showMessageDialog(Oculus.getMainWindow(), "Label is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      String permsText = permsField.getText().trim();
      HashSet<String> perms = new HashSet<>();
      if (!permsText.isEmpty()) {
         for (String p : permsText.split(",")) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
               perms.add(trimmed);
            }
         }
      }

      controller.createKey(label, perms);
   }

   private class KeyTableModel extends AbstractTableModel {
      @Override
      public int getRowCount() {
         return controller.getKeys().size();
      }

      @Override
      public int getColumnCount() {
         return COLUMNS.length;
      }

      @Override
      public String getColumnName(int column) {
         return COLUMNS[column];
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         List<Map<String, String>> keys = controller.getKeys();
         if (rowIndex >= keys.size()) {
            return "";
         }
         Map<String, String> row = keys.get(rowIndex);
         String attrKey = ATTR_KEYS[columnIndex];
         String value = row.get(attrKey);
         return value != null ? value : "";
      }

      void fireUpdate() {
         fireTableDataChanged();
      }
   }
}
