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
package com.iris.oculus.modules.account.ux;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.iris.Utils;
import com.iris.billing.client.model.request.BillingInfoRequest;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.FormattedTextField;
import com.iris.oculus.widget.FormattedTextField.FieldType;
import com.iris.oculus.widget.ValidatedControl;

public class BillingInformationPrompt {

   private static class InstanceRef {
      private static final BillingInformationDialog INSTANCE = new BillingInformationDialog();
   }

   public static ClientFuture<BillingInfoRequest> prompt(String buttonLabel) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt(buttonLabel);
   }

   @SuppressWarnings("serial")
   private static class BillingInformationDialog extends Dialog<BillingInfoRequest> {
      private JLabel ccLabel = new JLabel("CC Number*:");
      private JLabel monthLabel = new JLabel("Exp Month:");
      private JLabel yearLabel = new JLabel("Exp Year:");
      private JLabel ccvLabel = new JLabel("CCV:");
      private JLabel address1Label = new JLabel("Address1:");
      private JLabel address2Label = new JLabel("Address2:");
      private JLabel cityLabel = new JLabel("City:");
      private JLabel stateProvLabel = new JLabel("State:");
      private JLabel zipCodeLabel = new JLabel("ZipCode:");
      private JLabel countryLabel = new JLabel("Country:");
      private JLabel firstNameLabel = new JLabel("First Name:");
      private JLabel lastNameLabel = new JLabel("Last Name:");
      private JLabel note = new JLabel("*Required Value");

      private FormattedTextField cc = FormattedTextField.builder()
                           .setIsRequired(true)
                           .setValidationMessage("CC is required.  Test values may be found at https://docs.recurly.com/payment-gateways/test")
                           .setValue("4111-1111-1111-1111")
                           .build();
      private JComboBox<Integer> month = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
      private JComboBox<Integer> year = new JComboBox<>(new Integer[] { 2015, 2016, 2017, 2018, 2019, 2020 });

      private FormattedTextField ccv = FormattedTextField.builder()
            .setType(FieldType.INTEGER)
            .setUseGrouping(false)
            .setMinLength(3)
            .setMaxLength(3)
            .setValidationMessage("Invalid CCV")
            .build();

      private JTextField address1 = new JTextField();
      private JTextField address2 = new JTextField();
      private JTextField city = new JTextField();
      private JTextField stateProv = new JTextField();
      private FormattedTextField zipCode = FormattedTextField.builder()
                              .setType(FieldType.INTEGER)
                              .setUseGrouping(false)
                              .setMinLength(5)
                              .setMaxLength(5)
                              .setValidationMessage("Invalid Zip Code")
                              .build();
      private JTextField country = new JTextField();
      private JTextField firstName = new JTextField();
      private JTextField lastName = new JTextField();
      private JButton submit = new JButton();

      private ValidatedControl[] validatedControls = { cc, ccv, zipCode };

      public ClientFuture<BillingInfoRequest> prompt(String buttonLabel) {
         submit.setText(buttonLabel);
         return super.prompt();
      }

      @Override
      protected BillingInfoRequest getValue() {
         for (ValidatedControl validatedControl : validatedControls) {
            if (!validatedControl.validateContent()) {
               setErrorMessage(validatedControl.validationMessage());
               return null;
            }
         }

         BillingInfoRequest req = new BillingInfoRequest();
         req.setAddress1(address1.getText());
         req.setAddress2(address2.getText());
         req.setCardNumber(cc.getText());
         req.setCity(city.getText());
         req.setCountry(country.getText());
         req.setFirstName(firstName.getText());
         req.setLastName(lastName.getText());
         req.setMonth(((Number) month.getSelectedItem()).intValue());
         req.setPostalCode(zipCode.getText());
         req.setState(stateProv.getText());
         req.setVerificationValue(ccv.getText());
         req.setYear(((Number) year.getSelectedItem()).intValue());

         return req;
      }

      @Override
      protected Component createContents() {
         submit.addActionListener((e) -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         addLabelAndInput(panel, ccLabel, cc, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, monthLabel, month, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, yearLabel, year, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, ccvLabel, ccv, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, address1Label, address1, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, address2Label, address2, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, cityLabel, city, 280, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, stateProvLabel, stateProv, 280, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, zipCodeLabel, zipCode, 140, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, countryLabel, country, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, firstNameLabel, firstName, 280, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, lastNameLabel, lastName, 280, gbc);

         gbc.gridy++;
         addLabel(panel, note, gbc);

         gbc.gridy++;
         gbc.gridx = 1;
         gbc.anchor = GridBagConstraints.NORTHEAST;
         gbc.weighty = 1.0;
         gbc.fill = GridBagConstraints.NONE;
         panel.add(submit, gbc.clone());

         return panel;
      }

      private void addLabelAndInput(JPanel p, Component label, Component input, GridBagConstraints gbc) {
         addLabelAndInput(p, label, input, 0, gbc);
      }

      private void addLabelAndInput(JPanel p, Component label, Component input, int inputSize, GridBagConstraints gbc) {
         addLabel(p, label, gbc);
         addInput(p, input, inputSize, gbc);
      }

      private void addLabel(JPanel p, Component c, GridBagConstraints gbc) {
         gbc.gridx = 0;
         gbc.fill = GridBagConstraints.NONE;
         gbc.anchor = GridBagConstraints.EAST;
         gbc.weightx = 0;
         p.add(c, gbc.clone());
      }

      private void addInput(JPanel p, Component c, int inputSize, GridBagConstraints gbc) {
         gbc.gridx = 1;
         gbc.fill = inputSize > 0 ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
         gbc.anchor = GridBagConstraints.NORTHWEST;
         gbc.weightx = inputSize > 0 ? 0 : 1;
         if (inputSize > 0) {
            c.setPreferredSize(new Dimension(inputSize, c.getPreferredSize().height));
            c.setMinimumSize(c.getPreferredSize());
         }
         p.add(c, gbc.clone());
      }
   }
}

