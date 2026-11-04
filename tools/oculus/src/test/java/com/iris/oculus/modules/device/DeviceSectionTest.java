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
package com.iris.oculus.modules.device;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;

import org.junit.Test;

import com.iris.client.capability.DeviceAdvanced;
import com.iris.client.impl.InMemoryStore;
import com.iris.client.model.DeviceModel;
import com.iris.oculus.widget.table.Table;

public class DeviceSectionTest {
   private static final int NAME = 0;
   private static final int ADDED = 5;

   private interface AdvancedDeviceModel extends DeviceModel, DeviceAdvanced {
   }

   private static int nextId = 0;

   private static DeviceModel device(String name, String added) {
      AdvancedDeviceModel device = createNiceMock(AdvancedDeviceModel.class);
      expect(device.getId()).andReturn("device-" + nextId++).anyTimes();
      expect(device.getName()).andReturn(name).anyTimes();
      expect(device.getAdded()).andReturn(added == null ? null : Date.from(Instant.parse(added))).anyTimes();
      replay(device);
      return device;
   }

   private static Table<DeviceModel> table(DeviceModel... devices) {
      InMemoryStore<DeviceModel> store = new InMemoryStore<>();
      store.update(Arrays.asList(devices));
      DeviceController controller = createNiceMock(DeviceController.class);
      expect(controller.getStore()).andReturn(store).anyTimes();
      replay(controller);
      return new Table<>(new DeviceSection(controller).createTableModel());
   }

   private static List<Object> names(Table<DeviceModel> table) {
      List<Object> names = new ArrayList<>();
      for(int row = 0; row < table.getRowCount(); row++) {
         names.add(table.getValueAt(row, NAME));
      }
      return names;
   }

   private static JLabel rendered(Table<DeviceModel> table, int row, int column) {
      return (JLabel) table.prepareRenderer(table.getCellRenderer(row, column), row, column);
   }

   @Test
   public void testAddedSortsByDateNotByItsText() {
      // as text these sort Fri, Mon, Wed
      Table<DeviceModel> table = table(
            device("Wednesday 2026", "2026-07-01T12:00:00Z"),
            device("Friday 2025", "2025-01-03T12:00:00Z"),
            device("Monday 2024", "2024-03-04T12:00:00Z")
      );

      table.enableSorting(ADDED);

      assertEquals(Arrays.asList("Monday 2024", "Friday 2025", "Wednesday 2026"), names(table));
   }

   @Test
   public void testMissingAddedDateSortsFirstAndShowsUnknown() {
      Table<DeviceModel> table = table(
            device("Known", "2025-01-03T12:00:00Z"),
            device("Missing", null)
      );

      table.enableSorting(ADDED);

      assertEquals(Arrays.asList("Missing", "Known"), names(table));
      assertEquals("Unknown", rendered(table, 0, ADDED).getText());
   }

   @Test
   public void testSelectedRowKeepsSelectionColour() {
      Table<DeviceModel> table = table(
            device("Known", "2025-01-03T12:00:00Z"),
            device("Missing", null)
      );
      table.enableSorting(NAME);

      table.setRowSelectionInterval(0, 1);

      assertEquals(table.getSelectionForeground(), rendered(table, 0, ADDED).getForeground());
      assertEquals(table.getSelectionForeground(), rendered(table, 1, ADDED).getForeground());
   }

   @Test
   public void testNamesSortIgnoringCaseWithMissingNamesFirst() {
      Table<DeviceModel> table = table(
            device("porch", null),
            device(null, null),
            device("Attic", null),
            device("back door", null)
      );
      table.enableSorting(NAME);

      assertEquals(Arrays.asList(null, "Attic", "back door", "porch"), names(table));
   }
}
