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
package com.iris.oculus.modules.video.ux;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.iris.client.ClientRequest;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Recording;
import com.iris.client.capability.Recording.DownloadResponse;
import com.iris.client.model.RecordingModel;
import com.iris.client.service.VideoService;
import com.iris.client.session.SessionInfo.PlaceDescriptor;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.capability.ux.CapabilityResponsePopUp;
import com.iris.oculus.modules.video.dialog.RecordDialog;
import com.iris.oculus.modules.video.dialog.RecordDialog.RecordOperation;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.Browser;
import com.iris.oculus.widget.Toolbar;


public class VideoToolbar extends BaseToolbar<RecordingModel> {
   private Action undelete = Actions.build("Undelete", this::onUndelete);
   private Action delete = Actions.build("Delete", this::onDelete);
   private Action record = Actions.build("Record", this::onRecord);
   private Action view = Actions.build("View", this::onView);
   private Action download = Actions.build("Download", this::onDownload);

   private final DecimalFormat usedFormat = new DecimalFormat("###0.000");
   private final DecimalFormat totalFormat = new DecimalFormat("###0.###");
   private JLabel quota = new JLabel("quota: unknown/unknown");

   public VideoToolbar() {
   }

   @Override
   protected JPanel createComponent() {
      // sync state
      onEvent(model());
      return
         Toolbar
            .builder()
            // left is place level actions
            .left().addButton(record)
            .left().addComponent(quota)
            // right is model level actions
            .right().addButton(view)
            .right().addButton(download)
            .right().addButton(delete)
            .right().addButton(undelete)
            .right().addButton(refresh())
            .right().addButton(save())
            .build();
   }

   protected void onRecord() {
      RecordDialog.prompt()
      .onCompletion((result) -> {
         UUID place = IrisClientFactory.getClient().getActivePlace();
         if(place == null) {
            Oculus.showDialog(
                  "Error",
                  "Current place not set.  Cannot start a recording.",
                  JOptionPane.ERROR_MESSAGE);
            return;
         }
         List<PlaceDescriptor> places = IrisClientFactory.getClient().getSessionInfo().getPlaces().stream()
               .filter((pd) -> { return pd.getPlaceId().equals(place.toString()); })
               .collect(Collectors.toList());

         if(places.isEmpty()) {
            Oculus.showDialog(
                  "Error",
                  "No account found for selected place.  Cannot start a recording.",
                  JOptionPane.ERROR_MESSAGE);
            return;
         }

         String accountId = places.get(0).getAccountId();

         try {
            RecordOperation op = result.get();
            ClientRequest req = new ClientRequest();
            req.setAddress("SERV:" + VideoService.NAMESPACE + ":");
            req.setCommand(VideoService.StartRecordingRequest.NAME);
            req.setAttribute("accountId", accountId);
            req.setAttribute("cameraAddress", op.getCameraAddress());
            req.setAttribute(VideoService.StartRecordingRequest.ATTR_PLACEID, place.toString());
            req.setAttribute(VideoService.StartRecordingRequest.ATTR_STREAM, op.isStream());
            IrisClientFactory.getClient().request(req)
               .onSuccess((r) -> { Oculus.info("Recording started. " + r.getAttributes()); })
               .onFailure((e) -> { Oculus.showError("Error starting recording", e); });


         } catch(ExecutionException ee) {
         }
      });
   }

   protected void onView() {
      view.setEnabled(false);
      CapabilityResponsePopUp response = new CapabilityResponsePopUp();
      response.setCommand(Recording.CMD_VIEW);
      this
         .model()
         .view()
         .onSuccess(response::onResponse)
         .onFailure(response::onError)
         .onCompletion((v) -> view.setEnabled(model() != null))
         ;
   }

   protected void onDownload() {
      download.setEnabled(false);
      this
         .model()
         .download()
         .onSuccess(this::onDownloadResponse)
         .onFailure((error) -> onDownloadFailed(error))
         .onCompletion((v) -> download.setEnabled(model() != null))
         ;
   }
   
   protected void onDownloadResponse(DownloadResponse response) {
      Browser.launch(response.getMp4());
   }

   protected void onDownloadFailed(Throwable error) {
      Oculus.showError("Unable to Download", error);
   }

   protected void onDelete() {
      delete.setEnabled(false);
      this
         .model()
         .delete()
         .onFailure((error) -> onDeleteFailed(error))
         .onCompletion((v) -> delete.setEnabled(model() != null))
         ;
   }

   protected void onUndelete() {
      undelete.setEnabled(false);
      this
         .model()
         .resurrect()
         .onFailure((error) -> onUndeleteFailed(error))
         .onCompletion((v) -> undelete.setEnabled(model() != null))
         ;
   }

   protected void onDeleteFailed(Throwable error) {
      Oculus.showError("Unable to Delete", error);
   }

   protected void onUndeleteFailed(Throwable error) {
      Oculus.showError("Unable to Undelete", error);
   }

   public void updateQuota(double used, double total) {
      double usedMb = used / (1024.0*1024.0);
      double totalMb = total / (1024.0*1024.0);
      quota.setText("quota: " + usedFormat.format(usedMb) + " MB/" + totalFormat.format(totalMb) + " MB");
   }

   @Override
   protected void setModel(RecordingModel model) {
      super.setModel(model);
      delete.setEnabled(true);
      view.setEnabled(true);
      download.setEnabled(true);
   }

   @Override
   protected void clearModel() {
      super.clearModel();
      delete.setEnabled(false);
      view.setEnabled(false);
      download.setEnabled(false);
   }
}

