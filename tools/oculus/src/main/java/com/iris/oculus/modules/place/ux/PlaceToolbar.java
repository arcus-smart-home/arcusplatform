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
package com.iris.oculus.modules.place.ux;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.iris.client.ErrorEvent;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Capability.SetAttributesErrorEvent;
import com.iris.client.model.PlaceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.place.PlaceController;
import com.iris.oculus.util.Actions;

/**
 * @author tweidlin
 *
 */
public class PlaceToolbar extends JPanel {
   private Action refresh = Actions.build("Refresh (GetAttributes)", () -> onRefresh());
   private Action save = Actions.build("Save (SetAttributes)", () -> onSave());
   private PlaceController controller;
   private PlaceModel model;

   public PlaceToolbar(PlaceController controller) {
      this.controller = controller;
      init();
      clearModel();
   }

   private void init() {
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();

      gbc.anchor = GridBagConstraints.EAST;
      add(new JButton(controller.actionStartAddingDevices()), gbc.clone());
      add(new JButton(controller.actionStopAddingDevices()), gbc.clone());
      add(new JButton(controller.actionSetTimezone()), gbc.clone());
      add(new JButton(refresh), gbc.clone());
      gbc.weightx = 0.0;
      add(new JButton(save), gbc.clone());
      
      controller.getActivePlace().addSelectionListener((selection) -> {
         if(selection.isPresent()) {
            setModel(selection.get());
         }
         else {
            clearModel();
         }
      });
   }
   
   protected void onRefresh() {
      refresh.setEnabled(false);
      this
         .model
         .refresh()
         .onFailure((error) -> onRefreshFailed(error))
         .onCompletion((v) -> refresh.setEnabled(model != null))
         ;
   }

   protected void onSave() {
      save.setEnabled(false);
      this
         .model
         .commit()
         .onFailure((error) -> onSaveFailed(error))
         .onSuccess((event) -> { 
            if(SetAttributesErrorEvent.NAME.equals(event.getType())) {
               onSaveFailed(new SetAttributesErrorEvent(event));
            }
         })
         .onCompletion((v) -> save.setEnabled(model != null))
         ;
   }

   protected void onRefreshFailed(Throwable error) {
      Oculus.showError("Unable to Refresh", error);
   }

   protected void onSaveFailed(Throwable error) {
      Oculus.showError("Unable to Save", error);
   }

   protected void onSaveFailed(Capability.SetAttributesErrorEvent errors) {
      StringBuilder sb = new StringBuilder("<html>Unable to update some attributes:<ul>");
      for(ErrorEvent error: errors.getErrors()) {
         sb.append("<li>" + error.getCode() + ": " + error.getMessage() + "</li>");
      }
      sb.append("</li></html>");
      JOptionPane.showMessageDialog(this, sb.toString(), "Error", JOptionPane.WARNING_MESSAGE);
   }

   public void setModel(PlaceModel model) {
      clearModel();
      if(model != null) {
         this.model = model;
         refresh.setEnabled(true);
         save.setEnabled(true);
      }
   }

   public void clearModel() {
      refresh.setEnabled(false);
      save.setEnabled(false);
      this.model = null;
   }

}

