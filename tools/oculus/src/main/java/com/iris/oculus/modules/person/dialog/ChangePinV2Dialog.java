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
package com.iris.oculus.modules.person.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.iris.Utils;
import com.iris.client.capability.Person.ChangePinV2Request;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.Oculus;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;


/**
 * @author tweidlin
 *
 */
public class ChangePinV2Dialog {

   private static class InstanceRef {
      private static final ChangePinV2DialogImpl INSTANCE = new ChangePinV2DialogImpl();
   }

   public static ClientFuture<ChangePinV2Request> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   public static ClientFuture<ChangePinV2Request> prompt(String errorMessage) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt(errorMessage);
   }

   @SuppressWarnings("serial")
   private static class ChangePinV2DialogImpl extends Dialog<ChangePinV2Request> {
      FieldWrapper<?, String> placeField = Fields.textFieldBuilder().labelled("Place").build();
      FieldWrapper<?, String> newPin =
            Fields
               .formattedTextFieldBuilder("####")
               .labelled("New Pin")
               .build();
      JButton submit = new JButton("Change Pin");

      ChangePinV2DialogImpl() {
         init();
      }

      private void init() {
         setTitle("Enter Pin");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      @Override
      public ClientFuture<ChangePinV2Request> prompt() {
         clearErrorMessage();
         return super.prompt();
      }

      public ClientFuture<ChangePinV2Request> prompt(String message) {
         setErrorMessage(message);
         return super.prompt();
      }

      @Override
      protected ChangePinV2Request getValue() {
         ChangePinV2Request request = new ChangePinV2Request();
         request.setPin(newPin.getValue());
         request.setPlace(placeField.getValue());
         return request;
      }

      @Override
      protected void onShow() {
         newPin.getComponent().requestFocus();
      }

      @Override
      protected JPanel createContents() {
         submit.addActionListener((e) -> this.submit());
         Oculus.invokeOnEnter(newPin.getComponent(), () -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(newPin.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(newPin.getComponent(), gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.weightx = 0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(placeField.getLabel(), gbc.clone());

         gbc.gridx = 1;
         gbc.weightx = 1.0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         panel.add(placeField.getComponent(), gbc.clone());

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

