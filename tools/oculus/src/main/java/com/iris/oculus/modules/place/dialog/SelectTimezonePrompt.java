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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.iris.client.bean.TimeZone;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.Source;
import com.iris.oculus.widget.ComboBoxBuilder;
import com.iris.oculus.widget.Dialog;

public class SelectTimezonePrompt {
   private static final SelectTimezonePromptImpl instance = new SelectTimezonePromptImpl();
   
   public static ClientFuture<TimeZone> prompt(Source<TimeZone> timezones) {
      ClientFuture<TimeZone> result = instance.prompt();
      timezones
         .getData()
         .onSuccess((results) -> {
            instance.timezones.removeAllElements();
            for(TimeZone tz: results) {
               instance.timezones.addElement(tz);
            }
         })
         .onFailure((e) -> {
            result.cancel(true);
            Oculus.showError("Unable to load timezones", e);
         });
      return result;
   }
   
   private static class SelectTimezonePromptImpl extends Dialog<TimeZone> {
      DefaultComboBoxModel<TimeZone> timezones = new DefaultComboBoxModel<>();
      JComboBox<TimeZone> selectedZone;
      JLabel header;
      
      @Override
      protected TimeZone getValue() {
         return (TimeZone) selectedZone.getSelectedItem();
      }

      @Override
      protected Component createContents() {
         selectedZone =
               new ComboBoxBuilder<TimeZone>()
                  .withModel(timezones)
                  .withRenderer((tz) -> {
                     if(tz == null) {
                        return "<No Timezones Loaded>";
                     }
                     else {
                        return tz.getName();
                     }
                  })
                  .noteditable()
                  .create();
         
         header = new JLabel();
         header.setText("Select a timezone for your place");

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.anchor = GridBagConstraints.WEST;
         panel.add(header, gbc.clone());

         gbc.gridy++;
         gbc.weightx = 1.0;
         gbc.anchor = GridBagConstraints.NORTHWEST;
         panel.add(selectedZone, gbc.clone());
         
         gbc.gridy++;
         gbc.fill = GridBagConstraints.NONE;
         gbc.anchor = GridBagConstraints.SOUTHEAST;
         panel.add(new JButton(submitAction("Set Timezone")), gbc.clone());

         return panel;
      }
      
   }
}

