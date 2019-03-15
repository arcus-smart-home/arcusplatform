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
package com.iris.oculus.modules.video;

import java.awt.Component;
import java.util.Comparator;

import javax.inject.Inject;

import com.iris.capability.util.Addresses;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Device;
import com.iris.client.capability.Recording;
import com.iris.client.model.DeviceModel;
import com.iris.client.model.RecordingModel;
import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.capability.ux.ModelStoreViewBuilder;
import com.iris.oculus.modules.video.ux.PagingFilterToolbar;
import com.iris.oculus.modules.video.ux.VideoToolbar;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.Models;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

public class VideoSection extends BaseComponentWrapper<Component> implements OculusSection {
   private VideoController controller;

   @Inject
   public VideoSection(VideoController controller) {
      this.controller = controller;
   }

   @Override
   public String getName() {
      return "Recordings";
   }

   protected TableModel<RecordingModel> createSummaryTableModel() {
      return
            TableModelBuilder
               .builder(controller.getStore())
               .columnBuilder()
                  .withName("Camera")
                  .withGetter(this::formatCameraName)
                  .add()
               .columnBuilder()
                  .withName("Time")
                  .withGetter(RecordingModel::getTimestamp)
                  .add()
               .columnBuilder()
                  .withName("Type")
                  .withGetter(RecordingModel::getType)
                  .add()
               .columnBuilder()
                  .withName("Pending Deletion?")
                  .withGetter(RecordingModel::getDeleted)
                  .add()
               .build();
   }

   protected String formatCameraName(RecordingModel model) {
      String address = Addresses.toObjectAddress(Device.NAMESPACE, model.getCameraid());
      return Models.nameOf(address);
   }
   
   protected String formatType(RecordingModel model) {
      return (String) model.get(Recording.ATTR_TYPE);
   }
   
   @Override
   protected Component createComponent() {
      VideoToolbar toolbar = new VideoToolbar();
      controller.setToolbar(toolbar);
      controller.addSelectedListener(toolbar);
      PagingFilterToolbar pagingToolbar = new PagingFilterToolbar(controller);
      return
            ModelStoreViewBuilder
               .builder(controller.getStore())
               .sortedBy(Comparator.comparing(RecordingModel::getTimestamp).reversed())
               .withTypeName("Recording")
               .withListTableModel(createSummaryTableModel(), controller.getSelection(), pagingToolbar)
               .withModelSelector(
                     Recording.ATTR_NAME,
                     controller.getSelection(),
                     controller.actionReload()
               )
               .withToolbarComponent(toolbar.getComponent())
               .addShowListener((e) -> pagingToolbar.reloadCurrentPage())
               .build();
   }

}

