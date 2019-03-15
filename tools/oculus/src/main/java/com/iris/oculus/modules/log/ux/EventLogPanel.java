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
package com.iris.oculus.modules.log.ux;
import com.iris.oculus.modules.log.model.EventLogModel;
import com.iris.oculus.util.JsonPrettyPrinter;
import com.iris.oculus.view.ViewModel;
import com.iris.oculus.widget.InfoPanel;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

/**
 * 
 */
public class EventLogPanel extends InfoPanel<EventLogModel> {
   
   public EventLogPanel(ViewModel<EventLogModel> store) {
      super(store);
   }
   
   @Override
   protected TableModel<EventLogModel> createStoreTableModel() {
      return TableModelBuilder
               .<EventLogModel>builder()
                  .columnBuilder()
                  .withName("Type")
                  .withGetter(EventLogModel::getType)
                  .add()
               .columnBuilder()
                  .withName("Address")
                  .withGetter(EventLogModel::getAddress)
                  .add()
               .columnBuilder()
                  .withName("Timestamp")
                  .withGetter(EventLogModel::getTimestamp) // TODO format this?
                  .add()
               .columnBuilder()
                  .withName("Content")
                  .withGetter(EventLogModel::getContent)
                  .add()
               .build();
   }

   @Override
   protected String getMessageValue(EventLogModel value) {
      return JsonPrettyPrinter.prettyPrint(value.getContent());
   }


}

