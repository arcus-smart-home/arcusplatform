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
package com.iris.oculus.modules.device.ux;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.iris.bootstrap.ServiceLocator;
import com.iris.client.capability.DeviceAdvanced;
import com.iris.client.capability.DeviceMock;
import com.iris.client.capability.DeviceSettings;
import com.iris.client.model.DeviceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.device.dialog.DeviceSettingsDialog;
import com.iris.oculus.modules.device.dialog.MockActionsDialog;
import com.iris.oculus.modules.device.mockaction.MockActionsNexus;
import com.iris.oculus.modules.scheduler.SchedulerController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Toolbar;

/**
 * @author tweidlin
 *
 */
public class DeviceToolbar extends BaseToolbar<DeviceModel> {
   // TODO should these actions from from a per-device controller?
   private Action delete = Actions.build("Delete", () -> onDelete(false));
   private Action forceDelete = Actions.build("Force Delete", () -> onDelete(true));
   private Action schedule = Actions.build("Schedule", this::onSchedule);
   private Action actions = Actions.build("Mock Actions", this::onActions);
   private Action upgradeDriver = Actions.build("Upgrade Driver", this::onUpgradeDriver);
   private Action settings = Actions.build("Device Settings", this::onSettings);

   private MockActionsDialog mockActionsDialog = null;
   private DeviceSettingsDialog settingsDialog = null;

   public DeviceToolbar() {
      settings.setEnabled(false);
   }

   @Override
   protected JPanel createComponent() {
      return
         Toolbar
            .builder()
            .left().addButton(actions)
            .left().addButton(upgradeDriver)
            .left().addButton(settings)
            .right().addButton(schedule)
            .right().addButton(delete)
            .right().addButton(forceDelete)
            .right().addButton(refresh())
            .right().addButton(save())
            .build();
   }

   protected void onActions() {
      getMockActionsDialog(true).show(model());
   }

   protected void onSettings() {
      if (settingsDialog == null) {
         settingsDialog = new DeviceSettingsDialog();
      }
      settingsDialog.show(model());
   }

   protected void onUpgradeDriver() {
      upgradeDriver.setEnabled(false);
      DeviceModel model = model();
      DeviceAdvanced device = (DeviceAdvanced) model;
      device
         .upgradeDriver(null, null)
         .onSuccess((response) -> {
            DeviceAdvanced.UpgradeDriverResponse resp = new DeviceAdvanced.UpgradeDriverResponse(response);
            if (Boolean.TRUE.equals(resp.getUpgraded())) {
               Oculus.info("Driver upgraded to " + resp.getDriverName() + " v" + resp.getDriverVersion());
            } else {
               Oculus.info("Driver is already current: " + resp.getDriverName() + " v" + resp.getDriverVersion());
            }
         })
         .onFailure((error) -> Oculus.error("Unable to upgrade driver: " + error.getMessage(), error))
         .onCompletion((v) -> upgradeDriver.setEnabled(model() != null))
         ;
   }

   protected void onDelete(boolean force) {
      String message = force
         ? "Are you sure you want to force-delete this device?"
         : "Are you sure you want to delete this device?";
      int result = JOptionPane.showConfirmDialog(
         this.getComponent(), message, "Confirm Delete",
         JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
      );
      if(result != JOptionPane.YES_OPTION) {
         return;
      }

      delete.setEnabled(false);
      forceDelete.setEnabled(false);

      if (!force) {
         this
            .model()
            .remove(300*1000L)
            .onFailure((error) -> onDeleteFailed(error))
            .onCompletion((v) -> { delete.setEnabled(model() != null); forceDelete.setEnabled(model() != null); })
            ;
      } else {
         this
            .model()
            .forceRemove()
            .onFailure((error) -> onDeleteFailed(error))
            .onCompletion((v) -> { delete.setEnabled(model() != null); forceDelete.setEnabled(model() != null); })
            ;
      }
   }

   protected void onSchedule() {
      ServiceLocator
         .getInstance(SchedulerController.class)
         .addScheduledEvent(model().getAddress());
   }
   
   protected MockActionsDialog getMockActionsDialog(boolean create) {
      if (mockActionsDialog == null && create) {
         mockActionsDialog = new MockActionsDialog(ServiceLocator.getInstance(MockActionsNexus.class));
      }
      return mockActionsDialog;
   }
   
   protected void onDeleteFailed(Throwable error) {
      Oculus.error("Unable to delete object: " + error.getMessage(), error);
   }

   @Override
   protected void setModel(DeviceModel model) {
      super.setModel(model);
      delete.setEnabled(true);
      forceDelete.setEnabled(true);
      schedule.setEnabled(true);
      upgradeDriver.setEnabled(true);
      if (model.getCaps().contains(DeviceMock.NAMESPACE)) {
         if(mockActionsDialog != null && mockActionsDialog.isVisible()) {
            mockActionsDialog.show(model);
         }
         actions.setEnabled(true);
      }
      else {
         actions.setEnabled(false);
      }
      boolean hasSettings = model.getCaps().contains(DeviceSettings.NAMESPACE);
      settings.setEnabled(hasSettings);
      if (settingsDialog != null && settingsDialog.isVisible()) {
         if (hasSettings) {
            settingsDialog.follow(model);
         }
         else {
            settingsDialog.followWithoutSettings(model);
         }
      }
   }

   @Override
   protected void clearModel() {
      super.clearModel();
      MockActionsDialog actionsDialog = getMockActionsDialog(false);
      if (actionsDialog != null) {
         mockActionsDialog.setVisible(false);
      }
      if (settingsDialog != null) {
         settingsDialog.clear();
      }
      settings.setEnabled(false);
      actions.setEnabled(false);
      upgradeDriver.setEnabled(false);
      delete.setEnabled(false);
      forceDelete.setEnabled(false);
      schedule.setEnabled(false);
   }
}

