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
package com.iris.oculus.modules.device.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.iris.oculus.Oculus;

public class ListDriversDialog {

   private static final String[] COLUMNS = { "Name", "Version", "Description", "Capabilities", "Matchers" };

   public static void show(List<Map<String, Object>> drivers) {
      SwingUtilities.invokeLater(() -> {
         JDialog dialog = new JDialog(Oculus.getMainWindow(), "Supported Drivers (" + drivers.size() + ")");

         AbstractTableModel tableModel = new AbstractTableModel() {
            @Override
            public int getRowCount() { return drivers.size(); }

            @Override
            public int getColumnCount() { return COLUMNS.length; }

            @Override
            public String getColumnName(int col) { return COLUMNS[col]; }

            @Override
            public Object getValueAt(int row, int col) {
               Map<String, Object> driver = drivers.get(row);
               switch (col) {
                  case 0: return driver.get("name");
                  case 1: return driver.get("version");
                  case 2: return driver.get("description");
                  case 3: return String.valueOf(driver.get("capabilities"));
                  case 4: return String.valueOf(driver.get("matchers"));
                  default: return "";
               }
            }
         };

         JTable table = new JTable(tableModel);
         table.setAutoCreateRowSorter(true);
         table.getColumnModel().getColumn(0).setPreferredWidth(250);
         table.getColumnModel().getColumn(1).setPreferredWidth(50);
         table.getColumnModel().getColumn(2).setPreferredWidth(300);
         table.getColumnModel().getColumn(3).setPreferredWidth(200);
         table.getColumnModel().getColumn(4).setPreferredWidth(300);

         JScrollPane scrollPane = new JScrollPane(table);
         dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
         dialog.setPreferredSize(new Dimension(1200, 600));
         dialog.pack();
         dialog.setLocationRelativeTo(Oculus.getMainWindow());
         dialog.setVisible(true);
      });
   }
}
