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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.service.PersonService;
import com.iris.client.service.PersonService.SendPasswordResetResponse;
import com.iris.client.session.ResetPasswordCredentials;
import com.iris.client.session.SessionInfo;
import com.iris.oculus.Oculus;
import com.iris.oculus.Oculus.ProgressOptions;
import com.iris.oculus.OculusModule;
import com.iris.oculus.modules.session.ResetCodeInfo;
import com.iris.oculus.modules.session.SendResetTokenInfo;
import com.iris.oculus.modules.session.SendResetTokenInfo.NotificationMethod;
import com.iris.oculus.widget.Dialog;




/**
 * @author tweidlin
 *
 */
public class ResetPasswordWizard {
   private static final Logger logger = LoggerFactory.getLogger(ResetPasswordWizard.class);

   private static class InstanceRef {
      private static final ResetPasswordDialog INSTANCE = new ResetPasswordDialog();
   }

   public static ClientFuture<SessionInfo> show(String username) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return new ForgotPasswordImpl().show(username);
   }

   protected static void hide() {
      InstanceRef.INSTANCE.setVisible(false);
   }
   
   private static class ForgotPasswordImpl {
      
      private SettableClientFuture<SessionInfo> result = Futures.settableFuture();
      
      public SettableClientFuture<SessionInfo> show(String username) {
         showReset(username);
         return result;
      }
      
      protected void showReset(String username) {
         ResetPasswordDialog
            .INSTANCE
            .prompt(username)
            .onSuccess((e) -> this.doSendToken(e))
            .onFailure((e) -> result.setError(e));
      }
      
      protected void showReset(Throwable error) {
         ResetPasswordDialog
            .INSTANCE
            .promptError("Unable to do reset: " + error.getMessage())
            .onSuccess((e) -> this.doSendToken(e))
            .onFailure((e) -> result.setError(e));
      }
      
      protected void showEnterCode(String username) {
         EnterCodeDialog
            .INSTANCE
            .prompt(username)
            .onSuccess((e) -> doChange(e))
            .onFailure((error) -> result.setError(error))
            ;
      }
      
      protected void showEnterCode(Throwable error) {
         EnterCodeDialog
            .INSTANCE
            .promptError("Unable to do reset: " + error.getMessage())
            .onSuccess((e) -> doChange(e))
            .onFailure((e) -> result.setError(e));
      }
      
      protected void doChange(ResetCodeInfo info) {
         if(info.isBack()) {
            showReset(info.getUsername());
         }
         else {
            IrisClient client = IrisClientFactory.getClient();
            ResetPasswordCredentials credentials = new ResetPasswordCredentials();
            credentials.setConnectionURL(client.getConnectionURL());
            credentials.setUsername(info.getUsername());
            credentials.setToken(info.getCode());
            credentials.setPassword(info.getPassword());
            
            ClientFuture<SessionInfo> response = client.login(credentials);
            Oculus.showProgress(response, "Updating Password...", ProgressOptions.UNCANCELLABLE, ProgressOptions.NO_ERROR_DIALOG);
            response
               .onSuccess((i) -> {
                  result.setValue(i);
                  Oculus.info("Password successfully reset");
               })
               .onFailure((e) -> showEnterCode(e))
               ;
         }
      }
      
      protected void doSendToken(SendResetTokenInfo info) {
         ClientFuture<SendPasswordResetResponse> response =
            IrisClientFactory
               .getService(PersonService.class)
               .sendPasswordReset(
                     info.getUsername(), 
                     info.getMethod() == NotificationMethod.EMAIL ? 
                           PersonService.SendPasswordResetRequest.METHOD_EMAIL :
                           PersonService.SendPasswordResetRequest.METHOD_IVR
               );
         Oculus.showProgress(response, "Generating Reset Token...", ProgressOptions.NO_ERROR_DIALOG);
         response
            .onSuccess((e) -> showEnterCode(info.getUsername()))
            .onFailure((e) -> showReset(e))
            ;
      }

   }

   private static class ResetPasswordDialog extends Dialog<SendResetTokenInfo> {
      private static final ResetPasswordDialog INSTANCE = new ResetPasswordDialog();
      
      JTextField username = new JTextField();
      JRadioButton email = new JRadioButton("by email", true);
      JRadioButton phone = new JRadioButton("by a phone call", false);
      JButton submit = new JButton("Send Token");

      ResetPasswordDialog() {
         super();
         setTitle("Reset Password");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      public ClientFuture<SendResetTokenInfo> prompt(String username) {
         clearErrorMessage();
         this.username.setText(username);
         return super.prompt();
      }

      public ClientFuture<SendResetTokenInfo> promptError(String errorMessage) {
         setErrorMessage(errorMessage);
         return super.prompt();
      }

      protected SendResetTokenInfo getValue() {
         String username = this.username.getText();
         NotificationMethod method;
         if(email.isSelected()) {
            method = NotificationMethod.EMAIL;
         }
         else {
            method = NotificationMethod.IVR;
         }
         return new SendResetTokenInfo(username, method);
      }

      protected void onShow() {
         email.setSelected(true);
         if(username.getText().isEmpty()) {
            username.requestFocus();
         }
         else {
            submit.requestFocus();
         }
      }

      protected JPanel createContents() {
         ButtonGroup group = new ButtonGroup();
         group.add(email);
         group.add(phone);
         submit.addActionListener((e) -> this.submit());

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
         
         panel.add(new JLabel("Username:"), labels.clone());
         panel.add(username, fields.clone());
         labels.gridy++;
         fields.gridy++;

         GridBagConstraints buttons = new GridBagConstraints();
         buttons.gridy = labels.gridy + 1;
         buttons.weightx = 1.0;
         buttons.fill = GridBagConstraints.HORIZONTAL;
         buttons.anchor = GridBagConstraints.NORTHEAST;
         buttons.gridwidth = 2;

         JPanel selectorPanel = new JPanel();
         selectorPanel.setLayout(new VerticalLayout());
         selectorPanel.setBorder(BorderFactory.createTitledBorder("Notify me by:"));
         selectorPanel.add(email);
         selectorPanel.add(phone);
         panel.add(selectorPanel, buttons.clone());
         buttons.gridy++;


         buttons.weighty = 1.0;
         buttons.fill = GridBagConstraints.NONE;
         
         JPanel buttonPanel = new JPanel();
         buttonPanel.setLayout(new HorizontalLayout());

         buttonPanel.add(submit);
         panel.add(buttonPanel, buttons.clone());

         return panel;
      }

   }

   private static class EnterCodeDialog extends Dialog<ResetCodeInfo> {
      private static final EnterCodeDialog INSTANCE = new EnterCodeDialog();
      
      boolean goBack = false;
      String username;
      JTextField code = new JTextField();
      JTextField password = new JPasswordField();
      JButton back = new JButton("Request Another Code");
      JButton submit = new JButton("Reset");

      EnterCodeDialog() {
         super();
         setTitle("Enter Reset Code");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      public ClientFuture<ResetCodeInfo> prompt(String username) {
         clearErrorMessage();
         this.username = username;
         this.goBack = false;
         return super.prompt();
      }

      public ClientFuture<ResetCodeInfo> promptError(String errorMessage) {
         setErrorMessage(errorMessage);
         this.goBack = false;
         return super.prompt();
      }
      
      protected void back() {
         goBack = true;
         this.submit();
      }

      protected ResetCodeInfo getValue() {
         if(goBack) {
            return ResetCodeInfo.back(username);
         }
         else {
            String code = this.code.getText();
            String password = this.password.getText();
            return ResetCodeInfo.submit(username, code, password);
         }
      }

      protected void onShow() {
         code.requestFocus();
      }

      protected JPanel createContents() {
         Oculus.invokeOnEnter(code, () -> this.submit());
         Oculus.invokeOnEnter(password, () -> this.submit());
         submit.addActionListener((e) -> this.submit());
         back.addActionListener((e) -> this.back());

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
         
         panel.add(new JLabel("Code:"), labels.clone());
         panel.add(code, fields.clone());
         labels.gridy++;
         fields.gridy++;

         panel.add(new JLabel("New Password:"), labels.clone());
         panel.add(password, fields.clone());
         labels.gridy++;
         fields.gridy++;

         GridBagConstraints buttons = new GridBagConstraints();
         buttons.gridy = labels.gridy + 1;
         buttons.weightx = 1.0;
         buttons.weighty = 1.0;
         buttons.fill = GridBagConstraints.NONE;
         buttons.anchor = GridBagConstraints.NORTHEAST;
         buttons.gridwidth = 2;

         JPanel buttonPanel = new JPanel();
         buttonPanel.setLayout(new HorizontalLayout());

         buttonPanel.add(back);
         buttonPanel.add(submit);
         panel.add(buttonPanel, buttons.clone());

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
      SwingUtilities.invokeAndWait(() -> {
         ResetPasswordWizard.show("").onCompletion((e) -> {
            System.out.println("Done " + e);
            System.exit(e.isError() ? -1 : 0);
         });
      });
   }
}

