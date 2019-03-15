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
package com.iris.oculus.modules.session.ux;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.HorizontalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.Oculus;
import com.iris.oculus.OculusModule;
import com.iris.oculus.modules.session.ChangePasswordInfo;
import com.iris.oculus.widget.Dialog;




/**
 * @author tweidlin
 *
 */
public class ChangePasswordDialog {
   private static final Logger logger = LoggerFactory.getLogger(ChangePasswordDialog.class);

   private static class InstanceRef {
      private static final DialogImpl INSTANCE = new DialogImpl();
   }

   public static ClientFuture<ChangePasswordInfo> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   public static ClientFuture<ChangePasswordInfo> prompt(String errorMessage) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt(errorMessage);
   }

   protected static void hide() {
      InstanceRef.INSTANCE.setVisible(false);
   }

   private static class DialogImpl extends Dialog<ChangePasswordInfo> {
      JTextField oldPassword = new JPasswordField();
      JTextField newPassword = new JPasswordField();
      JButton login = new JButton("Change Password");

      DialogImpl() {
         super();
         setTitle("Change Password");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      public ClientFuture<ChangePasswordInfo> prompt() {
         clearErrorMessage();
         return super.prompt();
      }

      public ClientFuture<ChangePasswordInfo> prompt(String errorMessage) {
         setErrorMessage(errorMessage);
         return super.prompt();
      }

      protected ChangePasswordInfo getValue() {
         String oldPwd = oldPassword.getText();
         String newPwd = newPassword.getText();
         return new ChangePasswordInfo(oldPwd, newPwd);
      }

      protected void onShow() {
         oldPassword.setText("");
         newPassword.setText("");
         oldPassword.requestFocusInWindow();
      }

      protected JPanel createContents() {
         login.addActionListener((e) -> this.submit());
         Oculus.invokeOnEnter(oldPassword, () -> this.submit());
         Oculus.invokeOnEnter(newPassword, () -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints labels = new GridBagConstraints();
         labels.gridy = 0;
         labels.fill = GridBagConstraints.NONE;
         labels.anchor = GridBagConstraints.EAST;

         GridBagConstraints fields = new GridBagConstraints();
         fields.gridy = 0;
         fields.fill = GridBagConstraints.HORIZONTAL;
         fields.weightx = 1;

         panel.add(new JLabel("Old Password"), labels.clone());
         panel.add(oldPassword, fields.clone());
         labels.gridy++;
         fields.gridy++;

         panel.add(new JLabel("New Password"), labels.clone());
         panel.add(newPassword, fields.clone());
         labels.gridy++;
         fields.gridy++;

         JPanel buttonPanel = new JPanel();
         buttonPanel.setLayout(new HorizontalLayout());
         GridBagConstraints buttons = new GridBagConstraints();
         buttons.gridy = labels.gridy + 1;
         buttons.anchor = GridBagConstraints.NORTHEAST;
         buttons.gridwidth = 2;
         buttons.weighty = 1.0;

         buttonPanel.add(login);
         panel.add(buttonPanel, buttons);

         return panel;
      }

   }

   public static void main(String [] args) throws Exception {
      Bootstrap bootstrap =
         Bootstrap
            .builder()
            .withModuleClasses(OculusModule.class)
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
      SwingUtilities.invokeAndWait(ChangePasswordDialog::prompt);
   }
}

