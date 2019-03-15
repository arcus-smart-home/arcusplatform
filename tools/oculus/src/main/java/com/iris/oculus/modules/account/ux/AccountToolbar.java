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
package com.iris.oculus.modules.account.ux;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.iris.client.model.AccountModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.account.AccountController;

/**
 * @author tweidlin
 *
 */
public class AccountToolbar extends JPanel {
   private Action refresh = new AbstractAction("Refresh (GetAttributes)") {
      @Override
      public void actionPerformed(ActionEvent e) {
         onRefresh();
      }
   };
   private Action save = new AbstractAction("Save (SetAttributes)") {
      @Override
      public void actionPerformed(ActionEvent e) {
         onSave();
      }
   };
   private Action createBillingAccount = new AbstractAction("Create Billing") {
      @Override
      public void actionPerformed(ActionEvent e) {
         controller.promptBillingInformation();
      }
   };
   private Action updateBillingAccount = new AbstractAction("Update Billing") {
      @Override
      public void actionPerformed(ActionEvent e) {
         controller.promptUpdateBillingInformation();
      }
   };
   private Action delete = new AbstractAction("Delete") {
      @Override
      public void actionPerformed(ActionEvent e) {
         controller.promptDelete();
      }
   };

   private AccountModel model;
   private AccountController controller;

   public AccountToolbar() {
      init();
      clearModel();
   }

   public void setController(AccountController controller) {
      this.controller = controller;
   }

   private void init() {
      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();

      gbc.anchor = GridBagConstraints.WEST;
      gbc.weightx = 0.0;
      add(new JButton(createBillingAccount), gbc.clone());
      gbc.weightx = 1.0;
      add(new JButton(updateBillingAccount), gbc.clone());
      gbc.weightx = 1.0;
      add(new JButton(delete), gbc.clone());

      gbc.anchor = GridBagConstraints.EAST;
      add(new JButton(refresh), gbc.clone());
      gbc.weightx = 0.0;
      add(new JButton(save), gbc.clone());
   }

   protected void onRefresh() {
      refresh.setEnabled(false);
      this
         .model
         .refresh()
         .onFailure((error) -> Oculus.showError("Error Refreshing Account", error))
         .onCompletion((v) -> refresh.setEnabled(model != null))
         ;
   }

   protected void onSave() {
      save.setEnabled(false);
      this
         .model
         .commit()
//         .onSuccess((event) -> { if(event != null && event instanceof Capability.SetAttributesErrorEvent) onSaveFailed((Capability.SetAttributesErrorEvent) event))
         .onFailure((error) -> Oculus.showError("Error Saving Account", error))
         .onCompletion((v) -> save.setEnabled(model != null))
         ;
   }

   protected void onRefreshFailed(Throwable error) {
      JOptionPane.showMessageDialog(this, "Unable to refresh object: " + error.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
   }

   protected void onSaveFailed(Throwable error) {
      JOptionPane.showMessageDialog(this, "Unable to save object: " + error.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
   }

   protected void onDeleteFailed(Throwable error) {
      JOptionPane.showMessageDialog(this, "Unable to delete object: " + error.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
   }

//   protected void onSaveFailed(Capability.SetAttributesErrorEvent errors) {
//      StringBuilder sb = new StringBuilder("<html>Unable to update some attributes:<ul>");
//      for(ErrorEvent error: errors.getErrors()) {
//         sb.append("<li>" + error.getCode() + ": " + error.getMessage() + "</li>")
//      }
//      sb.append("</li></html>");
//      JOptionPane.showMessageDialog(this, sb.toString(), "Error", JOptionPane.WARNING_MESSAGE);
//   }

   public void setModel(AccountModel model) {
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

