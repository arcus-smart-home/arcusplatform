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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import com.iris.client.bean.DriverMatchCriterion;
import com.iris.client.bean.DriverMatcher;
import com.iris.client.bean.DriverSummary;
import com.iris.model.Version;
import com.iris.oculus.Oculus;

public class ListDriversDialog {

   private static final String[] COLUMNS = { "Name", "Version", "Populations", "Description", "Capabilities", "Matchers" };
   private static final int COLUMN_VERSION = 1;

   public static void show(List<Map<String, Object>> response) {
      List<DriverSummary> drivers = new ArrayList<>(response.size());
      for(Map<String, Object> attributes : response) {
         drivers.add(new DriverSummary(attributes));
      }

      SwingUtilities.invokeLater(() -> {
         JDialog dialog = new JDialog(Oculus.getMainWindow(), "Drivers (" + drivers.size() + ")");

         AbstractTableModel tableModel = new AbstractTableModel() {
            @Override
            public int getRowCount() { return drivers.size(); }

            @Override
            public int getColumnCount() { return COLUMNS.length; }

            @Override
            public String getColumnName(int col) { return COLUMNS[col]; }

            @Override
            public Object getValueAt(int row, int col) {
               DriverSummary driver = drivers.get(row);
               switch (col) {
                  case 0: return driver.getName();
                  case 1: return driver.getVersion();
                  case 2: return join(driver.getPopulations());
                  case 3: return driver.getDescription();
                  case 4: return join(driver.getCapabilities());
                  case 5: return describeMatchers(driver.getMatchers());
                  default: return "";
               }
            }
         };

         JTable table = new JTable(tableModel);
         TableRowSorter<AbstractTableModel> sorter = new TableRowSorter<>(tableModel);
         // 2.10 is newer than 2.9; Version sorts newest first, so reverse it for ascending order
         sorter.setComparator(COLUMN_VERSION, (a, b) ->
            Version.fromRepresentation((String) b).compareTo(Version.fromRepresentation((String) a))
         );
         table.setRowSorter(sorter);
         table.getColumnModel().getColumn(0).setPreferredWidth(250);
         table.getColumnModel().getColumn(1).setPreferredWidth(50);
         table.getColumnModel().getColumn(2).setPreferredWidth(100);
         table.getColumnModel().getColumn(3).setPreferredWidth(250);
         table.getColumnModel().getColumn(4).setPreferredWidth(200);
         table.getColumnModel().getColumn(5).setPreferredWidth(400);

         JScrollPane scrollPane = new JScrollPane(table);
         dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
         dialog.setPreferredSize(new Dimension(1400, 600));
         dialog.pack();
         dialog.setLocationRelativeTo(Oculus.getMainWindow());
         dialog.setVisible(true);
      });
   }

   private static String join(List<String> values) {
      return values == null ? "" : String.join(", ", values);
   }

   // one matcher line per "|", e.g. ZWAV:Manufacturer=99, ZWAV:ProductId=12338 | ZIGB:model~/^PGC410.*/
   private static String describeMatchers(List<Map<String, Object>> matchers) {
      if(matchers == null) {
         return "";
      }
      StringJoiner lines = new StringJoiner(" | ");
      for(Map<String, Object> matcher : matchers) {
         StringJoiner criteria = new StringJoiner(", ");
         List<Map<String, Object>> line = new DriverMatcher(matcher).getCriteria();
         if(line != null) {
            for(Map<String, Object> attributes : line) {
               criteria.add(describe(new DriverMatchCriterion(attributes)));
            }
         }
         lines.add(criteria.toString());
      }
      return lines.toString();
   }

   private static String describe(DriverMatchCriterion criterion) {
      String attribute = criterion.getAttribute();
      String value = criterion.getValue();
      if(DriverMatchCriterion.COMPARISON_REGEX.equals(criterion.getComparison())) {
         return attribute + "~/" + value + "/";
      }
      if(DriverMatchCriterion.COMPARISON_IN.equals(criterion.getComparison())) {
         return attribute + " in " + value;
      }
      if(DriverMatchCriterion.COMPARISON_CONTAINS.equals(criterion.getComparison())) {
         return attribute + " contains " + value;
      }
      return attribute + "=" + value;
   }
}
