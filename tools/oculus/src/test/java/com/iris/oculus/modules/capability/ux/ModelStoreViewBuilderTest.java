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
package com.iris.oculus.modules.capability.ux;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.table.JTableHeader;

import org.junit.Test;

import com.iris.client.model.DeviceModel;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.view.ViewModels;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

public class ModelStoreViewBuilderTest {

   private static DeviceModel device(String name, String type) {
      DeviceModel device = createNiceMock(DeviceModel.class);
      expect(device.getName()).andReturn(name).anyTimes();
      expect(device.getDevtypehint()).andReturn(type).anyTimes();
      replay(device);
      return device;
   }

   @SuppressWarnings("unchecked")
   private static Table<DeviceModel> table(DeviceModel... devices) {
      List<DeviceModel> models = Arrays.asList(devices);
      TableModel<DeviceModel> model =
            TableModelBuilder
               .builder(models)
               .columnBuilder()
                  .withName("Name")
                  .withGetter(DeviceModel::getName)
                  .add()
               .columnBuilder()
                  .withName("Type")
                  .withGetter(DeviceModel::getDevtypehint)
                  .add()
               .build();
      JScrollPane pane = (JScrollPane)
            new ModelStoreViewBuilder<>(ViewModels.from(models))
               .createSelectionTable(model, new DefaultSelectionModel<>());
      return (Table<DeviceModel>) pane.getViewport().getView();
   }

   private static List<Object> column(Table<?> table, int column) {
      List<Object> values = new ArrayList<>();
      for(int row = 0; row < table.getRowCount(); row++) {
         values.add(table.getValueAt(row, column));
      }
      return values;
   }

   private static void clickHeader(Table<?> table, int column) {
      JTableHeader header = table.getTableHeader();
      Rectangle bounds = header.getHeaderRect(column);
      header.dispatchEvent(new MouseEvent(
            header, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
            bounds.x + bounds.width / 2, 0, 1, false, MouseEvent.BUTTON1
      ));
   }

   @Test
   public void testStartsSortedByFirstColumnIgnoringCase() {
      Table<DeviceModel> table = table(
            device("Porch Light", "Light"),
            device("back door", "Contact"),
            device("Attic Fan", "Switch")
      );

      assertEquals(Arrays.asList("Attic Fan", "back door", "Porch Light"), column(table, 0));
   }

   @Test
   public void testClickingAHeaderSortsByThatColumn() {
      Table<DeviceModel> table = table(
            device("Porch Light", "Light"),
            device("Back Door", "Contact"),
            device("Attic Fan", "Switch")
      );

      clickHeader(table, 1);
      assertEquals(Arrays.asList("Contact", "Light", "Switch"), column(table, 1));

      clickHeader(table, 1);
      assertEquals(Arrays.asList("Switch", "Light", "Contact"), column(table, 1));
   }
}
