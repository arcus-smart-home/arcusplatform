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
package com.iris.oculus.modules.capability;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.oculus.OculusSection;

public class PermissionBrowserSection implements OculusSection {
   private static final String[] COLUMNS = { "Namespace", "Name", "Type" };

   private final DefinitionRegistry registry;
   private List<Entry> entries;

   @Inject
   public PermissionBrowserSection(DefinitionRegistry registry) {
      this.registry = registry;
   }

   @Override
   public String getName() {
      return "Permissions";
   }

   @Override
   public Component getComponent() {
      entries = buildEntries();

      EntryTableModel model = new EntryTableModel();
      JTable table = new JTable(model);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      table.getTableHeader().setReorderingAllowed(false);

      JTextArea detail = new JTextArea();
      detail.setEditable(false);
      detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

      table.getSelectionModel().addListSelectionListener(e -> {
         if (e.getValueIsAdjusting()) return;
         int row = table.getSelectedRow();
         if (row < 0 || row >= entries.size()) {
            detail.setText("");
            return;
         }
         detail.setText(buildDetail(entries.get(row)));
         detail.setCaretPosition(0);
      });

      JSplitPane split = new JSplitPane(
         JSplitPane.HORIZONTAL_SPLIT,
         new JScrollPane(table),
         new JScrollPane(detail)
      );
      split.setDividerLocation(350);
      split.setResizeWeight(0.3);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(split, BorderLayout.CENTER);
      return panel;
   }

   private List<Entry> buildEntries() {
      List<Entry> list = new ArrayList<>();
      for (CapabilityDefinition cap : registry.getCapabilities()) {
         list.add(new Entry(cap.getNamespace(), cap.getName(), "Capability", cap, null));
      }
      for (ServiceDefinition svc : registry.getServices()) {
         list.add(new Entry(svc.getNamespace(), svc.getName(), "Service", null, svc));
      }
      list.sort(Comparator.comparing(e -> e.namespace));
      return list;
   }

   private String buildDetail(Entry entry) {
      StringBuilder sb = new StringBuilder();
      sb.append(entry.name).append(" (").append(entry.type).append(")\n");
      sb.append("Namespace: ").append(entry.namespace).append("\n\n");

      // Permission strings
      sb.append("--- Permission Strings ---\n");
      if (entry.capability != null) {
         boolean hasReadable = false;
         boolean hasWritable = false;
         for (AttributeDefinition attr : entry.capability.getAttributes()) {
            if (attr.isReadable()) hasReadable = true;
            if (attr.isWritable()) hasWritable = true;
         }
         if (hasReadable) sb.append("  ").append(entry.namespace).append(":r:*\n");
         if (hasWritable) sb.append("  ").append(entry.namespace).append(":w:*\n");
         if (!entry.capability.getMethods().isEmpty()) sb.append("  ").append(entry.namespace).append(":x:*\n");
      } else {
         // Service — no attributes, just methods
         if (!entry.service.getMethods().isEmpty()) sb.append("  ").append(entry.namespace).append(":x:*\n");
      }

      // Methods
      List<MethodDefinition> methods = entry.capability != null
         ? entry.capability.getMethods()
         : entry.service.getMethods();
      if (!methods.isEmpty()) {
         sb.append("\n--- Methods ---\n");
         for (MethodDefinition m : methods) {
            sb.append("  ").append(entry.namespace).append(":").append(m.getName());
            String desc = m.getDescription();
            if (desc != null && !desc.isEmpty()) {
               sb.append("\n    ").append(desc);
            }
            sb.append("\n");
         }
      }

      // Attributes (capabilities only)
      if (entry.capability != null && !entry.capability.getAttributes().isEmpty()) {
         sb.append("\n--- Attributes ---\n");
         for (AttributeDefinition attr : entry.capability.getAttributes()) {
            sb.append("  ").append(entry.namespace).append(":").append(attr.getName());
            sb.append("  [").append(attr.getType());
            if (attr.isReadable()) sb.append(", R");
            if (attr.isWritable()) sb.append(", W");
            if (attr.isOptional()) sb.append(", optional");
            sb.append("]\n");
         }
      }

      return sb.toString();
   }

   private static class Entry {
      final String namespace;
      final String name;
      final String type;
      final CapabilityDefinition capability;
      final ServiceDefinition service;

      Entry(String namespace, String name, String type,
            CapabilityDefinition capability, ServiceDefinition service) {
         this.namespace = namespace;
         this.name = name;
         this.type = type;
         this.capability = capability;
         this.service = service;
      }
   }

   private class EntryTableModel extends AbstractTableModel {
      @Override
      public int getRowCount() {
         return entries != null ? entries.size() : 0;
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
      public Object getValueAt(int row, int col) {
         if (entries == null || row >= entries.size()) return "";
         Entry e = entries.get(row);
         switch (col) {
            case 0: return e.namespace;
            case 1: return e.name;
            case 2: return e.type;
            default: return "";
         }
      }
   }
}
