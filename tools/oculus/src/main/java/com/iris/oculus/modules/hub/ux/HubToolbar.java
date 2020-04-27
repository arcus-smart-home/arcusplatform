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
package com.iris.oculus.modules.hub.ux;

import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.iris.client.ClientRequest;
import com.iris.client.capability.Hub;
import com.iris.client.capability.HubZwave;
import com.iris.client.capability.HubZwave.NetworkInformationResponse;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.HubModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.hub.HubController;
import com.iris.oculus.util.Actions;

@SuppressWarnings("serial")
public class HubToolbar extends JPanel {

   private Action save     = Actions.build("Set", this, HubToolbar::onSet);
   private Action refresh  = Actions.build("Refresh", this, HubToolbar::onRefresh);
   private Action logs     = Actions.build("Logs", this, HubToolbar::onLogs);
   private Action config   = Actions.build("Config", this, HubToolbar::onConfig);
   private Action delete   = Actions.build("Delete", this, HubToolbar::onDelete);
   private Action unpair   = Actions.build("Unpair Orphan", this, HubToolbar::onUnpair);
   private Action graph    = Actions.build("Z-Wave Graph", this, HubToolbar::onGraph);


   private HubController  controller;
   private HubModel       model;

   public HubToolbar(HubController controller) {
      this.controller = controller;
      setLayout(new FlowLayout());
      add(new JSeparator());
      add(new JButton(save));
      add(new JButton(refresh));
      add(new JSeparator());
      add(new JButton(logs));
      add(new JButton(config));
      add(new JButton(graph));
      add(new JSeparator());
      add(new JButton(delete));
      add(new JSeparator());
      add(new JButton(unpair));
      disableButtons();
   }

   public void disableButtons() {
      save.setEnabled(false);
      refresh.setEnabled(false);
      logs.setEnabled(false);
      config.setEnabled(false);
      delete.setEnabled(false);
      unpair.setEnabled(false);
   }

   public void enableButtons() {
      save.setEnabled(true);
      refresh.setEnabled(true);
      logs.setEnabled(true);
      config.setEnabled(true);
      delete.setEnabled(true);
      unpair.setEnabled(true);
   }

   public void setController(HubController controller) {
      this.controller = controller;
   }

   public void setModel(HubModel model) {
      this.model = model;
      if(this.model == null) {
         disableButtons();
      } else {
         enableButtons();
      }
   }

   public void onLogs() {
      controller.getLogs(model);
   }

   public void onConfig() {
      controller.getConfig(model);
   }

   public  void onRefresh() {
      refresh.setEnabled(false);
      this
         .model
         .refresh()
         .onFailure((error) -> onRefreshFailed(error))
         .onCompletion((v) -> refresh.setEnabled(model != null))
         ;
   }

   public void onSet() {
      save.setEnabled(false);
      this
         .model
         .commit()
//         .onSuccess((event) -> { if(event != null && event instanceof Capability.SetAttributesErrorEvent) onSaveFailed((Capability.SetAttributesErrorEvent) event))
         .onFailure((error) -> onSaveFailed(error))
         .onCompletion((v) -> save.setEnabled(model != null))
         ;
   }

   public void onDelete() {
      delete.setEnabled(false);
      controller.delete(model).onCompletion((v) -> delete.setEnabled(model != null));
   }

   public void onUnpair() {
      unpair.setEnabled(false);
      controller.startUnpairing(model.getAddress()).onCompletion((v) -> unpair.setEnabled(model != null));
   }
   
   public void onGraph() {
      graph.setEnabled(false);

      ClientRequest request = new ClientRequest();
      request.setCommand(HubZwave.NetworkInformationRequest.NAME);
      request.setAddress(this.model.getAddress());

      ClientFuture<?> theFuture =
         this.model.request(request)
            // FIXME update i2common and refer to the constant
            .onSuccess((response) -> NetworkGraph.show((String) response.getAttribute("graph")))
            .onFailure((cause) -> Oculus.error("Unable to analyze network", Actions.build("Details", (e) -> Oculus.showError("Fail", cause))))
            .onCompletion((e) -> graph.setEnabled(true))
            ;
      Oculus.showProgress(theFuture, "Analyzing Z-Wave Network...");
   }

   public  void onRefreshFailed(Throwable error) {
      JOptionPane.showMessageDialog(this, "Unable to refresh object: " + error.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
   }

   public  void onSaveFailed(Throwable error) {
      JOptionPane.showMessageDialog(this, "Unable to save object: " + error.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
   }

}

