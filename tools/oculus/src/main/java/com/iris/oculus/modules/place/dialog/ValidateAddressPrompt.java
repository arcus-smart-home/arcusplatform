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
package com.iris.oculus.modules.place.dialog;

import com.iris.Utils;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.bean.StreetAddress;
import com.iris.client.capability.Place;
import com.iris.client.event.ClientFuture;
import com.iris.client.service.PlaceService;
import com.iris.oculus.widget.Dialog;

import javax.swing.*;
import java.awt.*;

public class ValidateAddressPrompt {

   private static class InstanceRef {
      private static final ValidateAddressDialog INSTANCE = new ValidateAddressDialog();
   }

   public static ClientFuture<Void> prompt(IrisClient client) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt(client);
   }

   private static class ValidateAddressDialog extends Dialog<Void> {

      private JLabel placeIdLabel = new JLabel("Optional Place ID:");
      private JTextField placeId = new JTextField();
      private JLabel line1Label = new JLabel("Line 1:");
      private JTextField line1 = new JTextField();
      private JLabel line2Label = new JLabel("Line 2:");
      private JTextField line2 = new JTextField();
      private JLabel cityLabel = new JLabel("City:");
      private JTextField city = new JTextField();
      private JLabel stateLabel = new JLabel("State:");
      private JTextField state = new JTextField();
      private JLabel zipLabel = new JLabel("Zip:");
      private JTextField zip = new JTextField();

      private JLabel resultsLabel = new JLabel("Validation Results");
      private JTextArea results = new JTextArea();

      private JButton submit = new JButton("Validate");

      private IrisClient client;


      public ClientFuture<Void> prompt(IrisClient irisClient) {
         this.client = irisClient;
         return prompt();
      }

      @Override
      protected void submit() {

         String id = placeId.getText();

         StreetAddress addr = new StreetAddress();
         addr.setLine1(line1.getText());
         addr.setLine2(line2.getText());
         addr.setCity(city.getText());
         addr.setState(state.getText());
         addr.setZip(zip.getText());

         System.out.println("!!!!! submitting");

         PlaceService.ValidateAddressRequest request = new PlaceService.ValidateAddressRequest();
         request.setPlaceId(id);
         request.setStreetAddress(addr.toMap());
         request.setAddress(Addresses.toServiceAddress(Place.NAMESPACE));
         request.setRestfulRequest(false);

         client.request(request)
               .onSuccess((e) -> {
                  results.setText(e.toString());
               })
               .onFailure((e) -> {
                  results.setText(e.toString());
               });
      }

      @Override
      protected Void getValue() {
         return null;
      }

      @Override
      protected Component createContents() {
         results.setEditable(false);
         results.setRows(10);
         results.setLineWrap(true);
         results.setWrapStyleWord(true);
         submit.addActionListener((e) -> submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         addLabelAndInput(panel, placeIdLabel, placeId, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, line1Label, line1, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, line2Label, line2, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, cityLabel, city, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, stateLabel, state, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, zipLabel, zip, gbc);

         gbc.gridy++;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.anchor = GridBagConstraints.EAST;
         gbc.weightx = 0;
         panel.add(resultsLabel, gbc.clone());

         gbc.gridy++;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.anchor = GridBagConstraints.EAST;
         gbc.weightx = 0;
         panel.add(results, gbc.clone());

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

