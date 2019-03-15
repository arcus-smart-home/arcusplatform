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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.*;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.iris.Utils;
import com.iris.bootstrap.ServiceLocator;
import com.iris.client.IrisClientFactory;
import com.iris.client.bean.PlaceAccessDescriptor;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.service.SessionService;
import com.iris.client.service.SessionService.ListAvailablePlacesResponse;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.widget.Dialog;

public class SelectNewPlacePrompt {

   public static class InstanceRef {
      private static final SelectNewPlaceDialog INSTANCE = new SelectNewPlaceDialog();
   }

   public static ClientFuture<PlaceAccessDescriptor> prompt(String prompt) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt(prompt);
   }

   private static class SelectNewPlaceDialog extends Dialog<PlaceAccessDescriptor> {

      private JLabel header = new JLabel("Place or the person's access to it has been removed.  Please select a new place:");
      private JComboBox<PlaceAccessDescriptor> placeSelector = new JComboBox<>();
      private JButton select = new JButton("Select");
      private JButton logout = new JButton(ServiceLocator.getInstance(SessionController.class).getLogoutAndLoginAction());

      SelectNewPlaceDialog() {
         placeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
               PlaceAccessDescriptor place = (PlaceAccessDescriptor) value;
               Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
               String text = place == null ? "[No Places Available]" : place.getName();
               ((JLabel) c).setText(text);
               return c;
            }
         });
      }

      @Override
      protected PlaceAccessDescriptor getValue() {
         return (PlaceAccessDescriptor) placeSelector.getSelectedItem();
      }

      @Override
      protected Component createContents() {
         select.addActionListener((e) -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.fill = GridBagConstraints.NONE;
         gbc.anchor = GridBagConstraints.EAST;
         panel.add(header, gbc.clone());

         gbc.gridy++;
         gbc.anchor = GridBagConstraints.NORTHWEST;
         placeSelector.setPreferredSize(new Dimension(240, placeSelector.getPreferredSize().height));
         placeSelector.setMinimumSize(placeSelector.getPreferredSize());
         panel.add(placeSelector, gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.anchor = GridBagConstraints.NORTHEAST;
         gbc.weighty = 1;
         gbc.weightx = 0;
         panel.add(select, gbc.clone());

         gbc.gridy++;
         gbc.gridx = 0;
         gbc.anchor = GridBagConstraints.NORTHEAST;
         gbc.weighty = 1;
         gbc.weightx = 0;
         panel.add(select, gbc.clone());

         return panel;
      }

      public ClientFuture<PlaceAccessDescriptor> prompt(String prompt) {
         header.setText("Loading places...");
         placeSelector.removeAllItems();
         SessionService svc = IrisClientFactory.getService(SessionService.class);
         svc.listAvailablePlaces().onSuccess(new Listener<SessionService.ListAvailablePlacesResponse>() {
            @Override
            public void onEvent(ListAvailablePlacesResponse arg0) {
               header.setText(prompt);
               List<Map<String, Object>> alphPlaces = arg0.getPlaces();
               Collections.sort(alphPlaces,  new PlaceNameComparator());
               alphPlaces.forEach((p) -> { placeSelector.addItem(new PlaceAccessDescriptor(p)); });
            }
         });
         return prompt();
      }
   }
   private static class PlaceNameComparator implements Comparator <Map<String,Object>> {
      public int compare(Map<String,Object> a, Map<String, Object> b){
         String firstName = (String)a.get("name");
         String secondName = (String)b.get("name");
         return (firstName.toLowerCase()).compareTo(secondName.toLowerCase());
      }

   }

}

