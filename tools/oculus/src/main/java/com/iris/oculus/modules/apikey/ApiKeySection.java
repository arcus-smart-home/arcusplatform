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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.oculus.Oculus;
import com.iris.oculus.OculusSection;

public class ApiKeySection implements OculusSection {
   private static final String[] COLUMNS = {
      "Label", "Key Prefix", "Permissions", "Created", "Last Used", "Expires At"
   };
   private static final String[] ATTR_KEYS = {
      "label", "keyPrefix", "permissions", "created", "lastUsed", "expiresAt"
   };
   private static final SimpleDateFormat TIMESTAMP_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   private final ApiKeyController controller;
   private final DefinitionRegistry registry;

   @Inject
   public ApiKeySection(ApiKeyController controller, DefinitionRegistry registry) {
      this.controller = controller;
      this.registry = registry;
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

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      JButton createBtn = new JButton("Create");
      createBtn.addActionListener((e) -> promptCreate());
      buttonPanel.add(createBtn);

      JButton revokeBtn = new JButton("Revoke");
      revokeBtn.addActionListener((e) -> {
         int row = table.getSelectedRow();
         if (row < 0) {
            JOptionPane.showMessageDialog(Oculus.getMainWindow(), "Select a key to revoke.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
         }
         List<Map<String, String>> keys = controller.getKeys();
         Map<String, String> key = keys.get(row);
         Object expiresAt = key.get("expiresAt");
         if (expiresAt != null && !String.valueOf(expiresAt).isEmpty()) {
            JOptionPane.showMessageDialog(Oculus.getMainWindow(), "This key is already revoked.", "Already Revoked", JOptionPane.INFORMATION_MESSAGE);
            return;
         }
         controller.revokeKey(key.get("id"), key.get("label"));
      });
      buttonPanel.add(revokeBtn);

      JButton refreshBtn = new JButton(controller.actionReload());
      buttonPanel.add(refreshBtn);

      JPanel tablePanel = new JPanel(new BorderLayout());
      tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
      tablePanel.add(buttonPanel, BorderLayout.SOUTH);
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

   // ---- Namespace entry for the combo box ----

   private static class NamespaceEntry {
      final String namespace;
      final String displayName;
      final boolean hasReadable;
      final boolean hasWritable;
      final boolean hasExecutable;

      NamespaceEntry(String namespace, String displayName, boolean hasReadable, boolean hasWritable, boolean hasExecutable) {
         this.namespace = namespace;
         this.displayName = displayName;
         this.hasReadable = hasReadable;
         this.hasWritable = hasWritable;
         this.hasExecutable = hasExecutable;
      }

      NamespaceEntry merge(NamespaceEntry other) {
         return new NamespaceEntry(
               namespace,
               displayName,
               hasReadable || other.hasReadable,
               hasWritable || other.hasWritable,
               hasExecutable || other.hasExecutable
         );
      }

      @Override
      public String toString() {
         return namespace + " \u2014 " + displayName;
      }
   }

   private List<NamespaceEntry> buildNamespaceEntries() {
      Map<String, NamespaceEntry> byNamespace = new LinkedHashMap<>();

      for (CapabilityDefinition cap : registry.getCapabilities()) {
         boolean readable = false;
         boolean writable = false;
         for (AttributeDefinition attr : cap.getAttributes()) {
            if (attr.isReadable()) readable = true;
            if (attr.isWritable()) writable = true;
         }
         boolean executable = !cap.getMethods().isEmpty();
         byNamespace.merge(cap.getNamespace(),
               new NamespaceEntry(cap.getNamespace(), cap.getName(), readable, writable, executable),
               NamespaceEntry::merge);
      }

      for (ServiceDefinition svc : registry.getServices()) {
         boolean executable = !svc.getMethods().isEmpty();
         byNamespace.merge(svc.getNamespace(),
               new NamespaceEntry(svc.getNamespace(), svc.getName(), false, false, executable),
               NamespaceEntry::merge);
      }

      List<NamespaceEntry> entries = new ArrayList<>(byNamespace.values());
      entries.sort(Comparator.comparing(e -> e.namespace.toLowerCase()));
      return entries;
   }

   // ---- Create dialog ----

   private void promptCreate() {
      JDialog dialog = new JDialog(Oculus.getMainWindow(), "Create API Key", JDialog.DEFAULT_MODALITY_TYPE);
      dialog.setLayout(new BorderLayout(8, 8));

      // -- Top: label field --
      JPanel labelPanel = new JPanel(new BorderLayout(4, 0));
      labelPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
      labelPanel.add(new JLabel("Label:"), BorderLayout.WEST);
      JTextField labelField = new JTextField(20);
      labelPanel.add(labelField, BorderLayout.CENTER);
      dialog.add(labelPanel, BorderLayout.NORTH);

      // -- Middle: permission calculator --
      JPanel calcPanel = new JPanel(new BorderLayout(4, 4));
      calcPanel.setBorder(BorderFactory.createTitledBorder("Permissions"));

      // Namespace combo + checkboxes + add button
      List<NamespaceEntry> nsEntries = buildNamespaceEntries();
      JComboBox<NamespaceEntry> nsCombo = new JComboBox<>(nsEntries.toArray(new NamespaceEntry[0]));

      JCheckBox readCb = new JCheckBox("Read (r)");
      JCheckBox writeCb = new JCheckBox("Write (w)");
      JCheckBox execCb = new JCheckBox("Execute (x)");

      // Update checkbox enabled state when selection changes
      Runnable updateCheckboxes = () -> {
         NamespaceEntry sel = (NamespaceEntry) nsCombo.getSelectedItem();
         if (sel != null) {
            readCb.setEnabled(sel.hasReadable);
            writeCb.setEnabled(sel.hasWritable);
            execCb.setEnabled(sel.hasExecutable);
            if (!sel.hasReadable) readCb.setSelected(false);
            if (!sel.hasWritable) writeCb.setSelected(false);
            if (!sel.hasExecutable) execCb.setSelected(false);
         }
      };
      nsCombo.addActionListener((e) -> updateCheckboxes.run());
      updateCheckboxes.run();

      // Permission list model
      DefaultListModel<String> permListModel = new DefaultListModel<>();
      JList<String> permList = new JList<>(permListModel);
      permList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

      JButton addBtn = new JButton("Add");
      addBtn.addActionListener((e) -> {
         NamespaceEntry sel = (NamespaceEntry) nsCombo.getSelectedItem();
         if (sel == null) return;
         boolean any = false;
         if (readCb.isSelected() && readCb.isEnabled()) {
            String perm = sel.namespace + ":r:*";
            if (!permListModel.contains(perm)) {
               permListModel.addElement(perm);
            }
            any = true;
         }
         if (writeCb.isSelected() && writeCb.isEnabled()) {
            String perm = sel.namespace + ":w:*";
            if (!permListModel.contains(perm)) {
               permListModel.addElement(perm);
            }
            any = true;
         }
         if (execCb.isSelected() && execCb.isEnabled()) {
            String perm = sel.namespace + ":x:*";
            if (!permListModel.contains(perm)) {
               permListModel.addElement(perm);
            }
            any = true;
         }
         if (!any) {
            JOptionPane.showMessageDialog(dialog, "Select at least one permission code (r/w/x).", "Nothing Selected", JOptionPane.WARNING_MESSAGE);
         }
      });

      // Top row: combo + checkboxes + add
      JPanel selectorRow = new JPanel(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(2, 2, 2, 2);
      gbc.gridy = 0;

      gbc.gridx = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
      selectorRow.add(nsCombo, gbc);

      gbc.gridx = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
      selectorRow.add(readCb, gbc);
      gbc.gridx = 2;
      selectorRow.add(writeCb, gbc);
      gbc.gridx = 3;
      selectorRow.add(execCb, gbc);
      gbc.gridx = 4;
      selectorRow.add(addBtn, gbc);

      calcPanel.add(selectorRow, BorderLayout.NORTH);

      // Center: permission list
      JScrollPane listScroll = new JScrollPane(permList);
      listScroll.setPreferredSize(new Dimension(0, 120));
      calcPanel.add(listScroll, BorderLayout.CENTER);

      // Bottom row: remove button + custom entry
      JPanel bottomRow = new JPanel();
      bottomRow.setLayout(new BoxLayout(bottomRow, BoxLayout.X_AXIS));

      JButton removeBtn = new JButton("Remove");
      removeBtn.addActionListener((e) -> {
         int[] indices = permList.getSelectedIndices();
         for (int i = indices.length - 1; i >= 0; i--) {
            permListModel.remove(indices[i]);
         }
      });
      bottomRow.add(removeBtn);

      bottomRow.add(Box.createHorizontalStrut(12));
      JTextField customField = new JTextField(15);
      bottomRow.add(customField);
      JButton addCustomBtn = new JButton("Add Custom");
      addCustomBtn.addActionListener((e) -> {
         String text = customField.getText().trim();
         if (!text.isEmpty() && !permListModel.contains(text)) {
            permListModel.addElement(text);
            customField.setText("");
         }
      });
      bottomRow.add(addCustomBtn);

      calcPanel.add(bottomRow, BorderLayout.SOUTH);

      dialog.add(calcPanel, BorderLayout.CENTER);

      // -- Bottom: OK / Cancel --
      boolean[] accepted = { false };
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      JButton okBtn = new JButton("OK");
      JButton cancelBtn = new JButton("Cancel");

      okBtn.addActionListener((e) -> {
         accepted[0] = true;
         dialog.dispose();
      });
      cancelBtn.addActionListener((e) -> dialog.dispose());

      buttonPanel.add(okBtn);
      buttonPanel.add(cancelBtn);
      dialog.getRootPane().setDefaultButton(okBtn);
      dialog.add(buttonPanel, BorderLayout.SOUTH);

      dialog.pack();
      dialog.setMinimumSize(new Dimension(600, 400));
      dialog.setLocationRelativeTo(Oculus.getMainWindow());
      dialog.setVisible(true);

      if (!accepted[0]) {
         return;
      }

      // Validate
      String label = labelField.getText().trim();
      if (label.isEmpty()) {
         JOptionPane.showMessageDialog(Oculus.getMainWindow(), "Label is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      if (permListModel.isEmpty()) {
         JOptionPane.showMessageDialog(Oculus.getMainWindow(), "At least one permission is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
         return;
      }

      HashSet<String> perms = new HashSet<>();
      for (int i = 0; i < permListModel.size(); i++) {
         perms.add(permListModel.get(i));
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
         Object value = row.get(attrKey);
         if (value == null) {
            return "";
         }
         // Format timestamp columns (created, lastUsed, expiresAt) from millis
         if ("created".equals(attrKey) || "lastUsed".equals(attrKey) || "expiresAt".equals(attrKey)) {
            try {
               long millis;
               if (value instanceof Number) {
                  millis = ((Number) value).longValue();
               } else {
                  millis = Long.parseLong(String.valueOf(value));
               }
               return TIMESTAMP_FMT.format(new Date(millis));
            } catch (NumberFormatException e) {
               // fall through to default
            }
         }
         return String.valueOf(value);
      }

      void fireUpdate() {
         fireTableDataChanged();
      }
   }
}
