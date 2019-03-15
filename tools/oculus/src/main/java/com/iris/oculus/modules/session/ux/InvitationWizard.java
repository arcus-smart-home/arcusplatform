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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.bean.Invitation;
import com.iris.client.capability.Person;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.service.InvitationService;
import com.iris.client.service.InvitationService.AcceptInvitationCreateLoginRequest;
import com.iris.client.service.InvitationService.GetInvitationRequest;
import com.iris.client.service.InvitationService.GetInvitationResponse;
import com.iris.client.service.PlaceService;
import com.iris.oculus.modules.session.LoginCredentials;
import com.iris.oculus.modules.session.LoginCredentials.Action;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;

/**
 * @author tweidlin
 *
 */
public class InvitationWizard {

   private static Pattern emailPattern = Pattern.compile(
         "^[_A-Z0-9-\\+]+(\\.[_A-Z0-9-]+)*@[A-Z0-9-]+(\\.[A-Z0-9]+)*(\\.[A-Z]{2,})$",
         Pattern.CASE_INSENSITIVE);

   public static ClientFuture<LoginCredentials> show() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return new InvitationWizardImpl().show();
   }

   private static class InvitationWizardImpl {

      private SettableClientFuture<LoginCredentials> result = Futures.settableFuture();

      public SettableClientFuture<LoginCredentials> show() {
         showGetInvitation();
         return result;
      }

      protected void showGetInvitation() {
        GetInvitationDialog
            .INSTANCE
            .prompt()
            .onSuccess((e) -> { this.doGetInvitation(e); })
            .onFailure((e) -> result.setError(e));
      }

      protected void showGetInvitation(Throwable error) {
         GetInvitationDialog
            .INSTANCE
            .promptError("Unable to do get invitation: " + error.getMessage())
            .onSuccess((e) -> this.doGetInvitation(e))
            .onFailure((e) -> result.setError(e));
      }

      protected void showInvitation(Map<String,Object> invitation) {
         InvitationDialog
            .INSTANCE
            .prompt(invitation)
            .onSuccess((e) -> doCreateLogin(e))
            .onFailure((error) -> result.setError(error));
      }

      protected void doCreateLogin(AcceptInvitationCreateLoginRequest request) {
         IrisClient client = IrisClientFactory.getClient();

         ClientFuture<ClientEvent> response = client.request(request);
         response
            .onSuccess((e) -> {
               Map<String,Object> pers = request.getPerson();
               LoginCredentials creds = new LoginCredentials(
                     Action.LOGIN,
                     client.getConnectionURL(),
                     (String) pers.get(Person.ATTR_EMAIL),
                     request.getPassword().toCharArray());
               result.setValue(creds);
            })
            .onFailure((e) -> { showGetInvitation(e); });
      }

      protected void doGetInvitation(InvitationInfo info) {
         IrisClient client = IrisClientFactory.getClient();
         GetInvitationRequest request = new GetInvitationRequest();
         request.setAddress("SERV:" + InvitationService.NAMESPACE + ":");
         request.setInviteeEmail(info.email);
         request.setCode(info.code);
         request.setRestfulRequest(true);

         ClientFuture<ClientEvent> response = client.request(request);
         response
            .onSuccess((e) -> { showInvitation(new GetInvitationResponse(e).getInvitation()); })
            .onFailure((e) -> { showGetInvitation(e); });
      }
   }

   private static class InvitationInfo {
      private final String code;
      private final String email;
      InvitationInfo(String code, String email) { this.code = code; this.email = email; }
   }

   @SuppressWarnings("serial")
   private static class GetInvitationDialog extends Dialog<InvitationInfo> {
      private static final GetInvitationDialog INSTANCE = new GetInvitationDialog();

      FieldWrapper<?, String> codeField = Fields.textFieldBuilder().labelled("Invitation Code").build();
      FieldWrapper<?, String> emailField = Fields.textFieldBuilder().labelled("Email").build();
      JButton submit = new JButton("OK");

      GetInvitationDialog() {
         super();
         setTitle("Get Invitation");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      public ClientFuture<InvitationInfo> prompt() {
         clearErrorMessage();
         return super.prompt();
      }

      public ClientFuture<InvitationInfo> promptError(String errorMessage) {
         setErrorMessage(errorMessage);
         return super.prompt();
      }

      protected InvitationInfo getValue() {
         validateForm();
         return new InvitationInfo(codeField.getValue(), emailField.getValue());
      }

      private void validateForm() {
         if(StringUtils.isBlank(codeField.getValue())) {
            throw new RuntimeException("code is required");
         }
         if(StringUtils.isBlank(emailField.getValue())) {
            throw new RuntimeException("email is required");
         }
         if (!emailPattern.matcher(emailField.getValue()).matches()) {
            throw new RuntimeException("email format is invalid.");
         }
      }

      protected JPanel createContents() {
         submit.addActionListener((e) -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(codeField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(codeField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(emailField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(emailField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.anchor = GridBagConstraints.NORTHEAST;
         gbc.weighty = 1.0;
         gbc.gridx = 1;
         gbc.fill = GridBagConstraints.NONE;

         panel.add(submit, gbc.clone());

         return panel;
      }
   }

   @SuppressWarnings("serial")
   private static class InvitationDialog extends Dialog<AcceptInvitationCreateLoginRequest> {

      private static final InvitationDialog INSTANCE = new InvitationDialog();

      FieldWrapper<?, String> codeField = Fields.textFieldBuilder().labelled("Invitation Code").build();
      FieldWrapper<?, String> emailField = Fields.textFieldBuilder().labelled("Invitee Email").build();
      FieldWrapper<?, String> firstNameField = Fields.textFieldBuilder().labelled("First Name").build();
      FieldWrapper<?, String> lastNameField = Fields.textFieldBuilder().labelled("Last Name").build();
      FieldWrapper<?, String> irisEmailField = Fields.textFieldBuilder().labelled("Iris Email").build();
      FieldWrapper<?, String> mobilePhoneField = Fields.formattedTextFieldBuilder("(###) ###-####").labelled("Mobile Number").build();
      JLabel passwordLabel = new JLabel("Password");
      JTextField password = new JPasswordField();
      JButton submit = new JButton("Create");

      InvitationDialog() {
         super();
         setTitle("Create Login");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      public ClientFuture<AcceptInvitationCreateLoginRequest> prompt(Map<String,Object> invitation) {
         clearErrorMessage();
         codeField.setValue((String) invitation.get(Invitation.ATTR_CODE));
         emailField.setValue((String) invitation.get(Invitation.ATTR_INVITEEEMAIL));
         firstNameField.setValue((String) invitation.get(Invitation.ATTR_INVITEEFIRSTNAME));
         lastNameField.setValue((String) invitation.get(Invitation.ATTR_INVITEELASTNAME));
         irisEmailField.setValue((String) invitation.get(Invitation.ATTR_INVITEEEMAIL));
         return super.prompt();
      }

      protected AcceptInvitationCreateLoginRequest getValue() {
         validateForm();
         Map<String,Object> person = new HashMap<>();
         person.put(Person.ATTR_FIRSTNAME, firstNameField.getValue());
         person.put(Person.ATTR_LASTNAME, lastNameField.getValue());
         String email = irisEmailField.getValue();
         if(StringUtils.isBlank(email)) {
            email = emailField.getValue();
         }
         person.put(Person.ATTR_EMAIL, email);
         if(!"()-".equals(StringUtils.deleteWhitespace(mobilePhoneField.getValue()))) {
            person.put(Person.ATTR_MOBILENUMBER, mobilePhoneField.getValue());
         }

         AcceptInvitationCreateLoginRequest request = new AcceptInvitationCreateLoginRequest();
         request.setAddress("SERV:" + PlaceService.NAMESPACE + ":");
         request.setCode(codeField.getValue());
         request.setInviteeEmail(emailField.getValue());
         request.setPassword(password.getText());
         request.setPerson(person);
         request.setRestfulRequest(true);
         return request;
      }

      private void validateForm() {
         if(StringUtils.isBlank(codeField.getValue())) {
            throw new RuntimeException("code is required");
         }
         if(StringUtils.isBlank(emailField.getValue())) {
            throw new RuntimeException("email is required");
         }
         if(!emailPattern.matcher(emailField.getValue()).matches()) {
            throw new RuntimeException("email format is invalid.");
         }
         if(StringUtils.isBlank(firstNameField.getValue())) {
            throw new RuntimeException("firstName is required");
         }
         if(StringUtils.isBlank(lastNameField.getValue())) {
            throw new RuntimeException("lastName is required");
         }
         if(!StringUtils.isBlank(irisEmailField.getValue()) && !emailPattern.matcher(irisEmailField.getValue()).matches()) {
            throw new RuntimeException("iris email format is invalid");
         }
         if(StringUtils.isBlank(password.getText())) {
            throw new RuntimeException("passowrd is required");
         }
      }

      protected JPanel createContents() {
         submit.addActionListener((e) -> this.submit());

         codeField.getComponent().setEnabled(false);
         emailField.getComponent().setEnabled(false);

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(codeField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(codeField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(emailField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(emailField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(firstNameField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(firstNameField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(lastNameField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(lastNameField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(mobilePhoneField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(mobilePhoneField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(irisEmailField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(irisEmailField.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(passwordLabel, gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(password, gbc.clone());

         gbc.gridy++;
         gbc.anchor = GridBagConstraints.NORTHEAST;
         gbc.weighty = 1.0;
         gbc.gridx = 1;
         gbc.fill = GridBagConstraints.NONE;

         panel.add(submit, gbc.clone());

         return panel;
      }

   }

}

