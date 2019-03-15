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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;
import com.iris.client.capability.Place;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.modules.place.PlaceInfo;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.FormattedTextField;
import com.iris.oculus.widget.FormattedTextField.FieldType;
import com.iris.oculus.widget.ValidatedControl;

public class AddPlacePrompt {

   private static class InstanceRef {
      private static final AddPlaceDialog INSTANCE = new AddPlaceDialog();
   }

   public static ClientFuture<PlaceInfo> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   @SuppressWarnings("serial")
   private static class AddPlaceDialog extends Dialog<PlaceInfo> {
      private JLabel nameLabel = new JLabel("Name*:");
      private JLabel serviceLevelLabel = new JLabel("Service Level:");
      private JLabel address1Label = new JLabel("Address1:");
      private JLabel address2Label = new JLabel("Address2:");
      private JLabel cityLabel = new JLabel("City:");
      private JLabel stateProvLabel = new JLabel("State:");
      private JLabel zipCodeLabel = new JLabel("ZipCode:");
      private JLabel zipPlus4Label = new JLabel("Zip Plus4:");
      private JLabel zipTypeLabel = new JLabel("Zip Type:");
      private JLabel serviceAddonsLabel = new JLabel("Service addons:");
      private JLabel countryLabel = new JLabel("Country:");
      private JLabel countyLabel = new JLabel("County:");
      private JLabel countyFipsLabel = new JLabel("County FIPS:");
      private JLabel tzNameLabel = new JLabel("Timezone:");
      private JLabel tzOffsetLabel = new JLabel("Timezone Offset:");
      private JLabel tzUsesDSTLabel = new JLabel("Daylight Savings:");
      private JLabel addrValidatedLabel = new JLabel("Address Validated:");
      private JLabel addrTypeLabel = new JLabel("Address Type:");
      private JLabel latitudeLabel = new JLabel("Latitude:");
      private JLabel longitudeLabel = new JLabel("Longitude:");
      private JLabel geoPrecisionLabel = new JLabel("GeoPrecision:");
      private JLabel addrRdiLabel = new JLabel("Address RDI:");
      private JLabel note = new JLabel("*Required Value");

      private FormattedTextField name = FormattedTextField.builder()
                           .setIsRequired(true)
                           .setValidationMessage("Name is required")
                           .build();
      private JComboBox<String> serviceLevel = new JComboBox<>(SERVICE_LEVELS);
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
      private JTextField serviceAddons = new JTextField();
      private FormattedTextField zipPlus4 = FormattedTextField.builder()
                              .setType(FieldType.INTEGER)
                              .setUseGrouping(false)
                              .setMinLength(4)
                              .setMaxLength(4)
                              .setValidationMessage("Invalid Zip Plus 4")
                              .build();
      private JTextField country = new JTextField();
      private JTextField county = new JTextField();
      private JTextField countyFips = new JTextField();
      private JComboBox<String> tzName = new JComboBox<>(TIMEZONES);
      private JComboBox<Integer> tzOffset = new JComboBox<>(TIMEZONE_OFFSET);
      private JCheckBox tzUsesDST = new JCheckBox();
      private JCheckBox addrValidated = new JCheckBox();
      private JComboBox<AddressType> addrType = new JComboBox<>(ADDRESS_TYPE);
      private JComboBox<String> zipType = new JComboBox<>(ZIP_TYPE);
      private FormattedTextField latitude = FormattedTextField.builder()
                                       .setType(FieldType.DOUBLE)
                                       .setValidationMessage("Invalid Latitude")
                                       .build();
      private FormattedTextField longitude = FormattedTextField.builder()
                                       .setType(FieldType.DOUBLE)
                                       .setValidationMessage("Invalid Longitude")
                                       .build();
      private JComboBox<String> geoPrecision = new JComboBox<>(GEO_PRECISION);
      private JComboBox<String> addrRdi = new JComboBox<>(RDI);
      private JButton submit = new JButton("Add Place");

      private ValidatedControl[] validatedControls = {
            name,
            zipCode,
            zipPlus4,
            latitude,
            longitude
      };

      

      

      @Override
      protected PlaceInfo getValue() {
         PlaceInfo placeInfo = new PlaceInfo();

         for (ValidatedControl validatedControl : validatedControls) {
            if (!validatedControl.validateContent()) {
               setErrorMessage(validatedControl.validationMessage());
               return null;
            }
         }

         placeInfo.setName(name.getText());
         placeInfo.setServiceLevel(serviceLevel.getSelectedItem().toString());
         placeInfo.setAddress1(address1.getText());
         placeInfo.setAddress2(address2.getText());
         placeInfo.setCity(city.getText());
         placeInfo.setStateProv(stateProv.getText());
         placeInfo.setZipCode(zipCode.getText());
         placeInfo.setZipPlus4(zipPlus4.getText());
         placeInfo.setZipType(zipType.getSelectedItem().toString());
         placeInfo.setCountry(country.getText());
         placeInfo.setCounty(county.getText());
         placeInfo.setCountyFips(countyFips.getText());
         placeInfo.setTzName(tzName.getSelectedItem().toString());
         placeInfo.setTzOffset((Integer)tzOffset.getSelectedItem());
         placeInfo.setTzUsesDST(tzUsesDST.isSelected());
         placeInfo.setAddrValidated(addrValidated.isSelected());
         placeInfo.setAddrType(((AddressType)addrType.getSelectedItem()).getValue());

         if(!StringUtils.isBlank(latitude.getText())) {
            placeInfo.setLatitude(Double.parseDouble(latitude.getText()));
         }

         if(!StringUtils.isBlank(longitude.getText())) {
            placeInfo.setLongitude(Double.parseDouble(longitude.getText()));
         }

         placeInfo.setGeoPrecision(geoPrecision.getSelectedItem().toString());
         placeInfo.setRdi(addrRdi.getSelectedItem().toString());

         String addonsText = serviceAddons.getText();
         String[] addons = StringUtils.isEmpty(addonsText) ? null : StringUtils.split(addonsText, ',');
         if (addons != null && addons.length > 0) {
            Set<String> addonSet = new HashSet<>();
            for (String addon : addons) {
               addonSet.add(addon.trim());
            }
            placeInfo.setServiceAddons(addonSet);
         }
         else {
            placeInfo.setServiceAddons(Collections.emptySet());
         }

         return placeInfo;
      }

      @Override
      protected Component createContents() {
         submit.addActionListener((e) -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         addLabelAndInput(panel, nameLabel, name, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, serviceLevelLabel, serviceLevel, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, serviceAddonsLabel, serviceAddons, gbc);

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
         addLabelAndInput(panel, zipPlus4Label, zipPlus4, 120, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, zipTypeLabel, zipType, 120, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, countryLabel, country, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, countyLabel, county, 280, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, countyFipsLabel, countyFips, 160, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, addrRdiLabel, addrRdi, 120, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, addrValidatedLabel, addrValidated, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, addrTypeLabel, addrType, 180, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, tzNameLabel, tzName, 280, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, tzOffsetLabel, tzOffset, 120, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, tzUsesDSTLabel, tzUsesDST, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, latitudeLabel, latitude, 160, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, longitudeLabel, longitude, 160, gbc);

         gbc.gridy++;
         addLabelAndInput(panel, geoPrecisionLabel, geoPrecision, 120, gbc);

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

      private static class AddressType {
         private final String value;
         private final String tag;

         AddressType(String value, String tag) {
            this.value = value;
            this.tag = tag;
         }

         String getValue() {
            return value;
         }

         @Override
         public String toString() {
            return tag;
         }
      }

      private static final String[] SERVICE_LEVELS = {
         Place.SERVICELEVEL_PREMIUM,
         Place.SERVICELEVEL_BASIC
      };

      private static final String[] TIMEZONES = {
        "Alaska",
        "Atlantic",
        "Central",
        "Eastern",
        "Hawaii",
        "Mountain",
        "None",
        "Pacific",
        "Samoa",
        "UTC+10",
        "UTC+11",
        "UTC+12",
        "UTC+9"
      };

      private static final Integer[] TIMEZONE_OFFSET = {
         -9,
         -4,
         -6,
         -5,
         -10,
         -7,
         0,
         -8,
         -11,
         10,
         11,
         12,
         9
      };

      private static final AddressType[] ADDRESS_TYPE = {
         new AddressType("F", "F - firm"),
         new AddressType("G", "G - general"),
         new AddressType("H", "H - high-rise"),
         new AddressType("P", "P - PO box"),
         new AddressType("R", "R - rural route"),
         new AddressType("S", "S - street"),
         new AddressType("", "(invalid)")
      };

      private static final String[] ZIP_TYPE = {
         "Unique",
         "Military",
         "POBox",
         "Standard"
      };

      private static final String[] GEO_PRECISION = {
         "Unknown",
         "None",
         "Zip5",
         "Zip6",
         "Zip7",
         "Zip8",
         "Zip9"
      };

      private static final String[] RDI = {
         "Residential",
         "Commercial",
         "Unknown"
      };
   }

   /*
   @SuppressWarnings("unchecked")
   public static void main(String [] args) throws Exception {
      Bootstrap bootstrap =
         Bootstrap
            .builder()
            .withModuleClasses(OculusModule.class)
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
      SwingUtilities.invokeAndWait(() -> { AddPlacePrompt.prompt(null);});
   }
   */
}

