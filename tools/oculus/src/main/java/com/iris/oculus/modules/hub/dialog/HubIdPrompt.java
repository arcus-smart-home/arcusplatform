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
package com.iris.oculus.modules.hub.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.Oculus;
import com.iris.oculus.OculusModule;
import com.iris.oculus.widget.Dialog;




/**
 * @author tweidlin
 *
 */
public class HubIdPrompt {
   private static final Logger logger = LoggerFactory.getLogger(HubIdPrompt.class);
   private static final Pattern hubIdPattern = Pattern.compile("\\w{3}\\-?\\w{4}");

   private static class InstanceRef {
      private static final HubIdDialog INSTANCE = new HubIdDialog();
   }

   public static ClientFuture<String> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   private static class HubIdDialog extends Dialog<String> {
      JLabel instructions = new JLabel("Enter your hub id in the format xxx-yyyy");
      JTextField hubId = new JTextField();
      JButton submit = new JButton("Register");

      HubIdDialog() {
         init();
      }

      private void init() {
         setTitle("Enter Hub Id");
         setModal(true);
         setResizable(false);
         setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
      }

      protected String getValue() {
         String value = this.hubId.getText();
         if(value != null) {
            value = value.trim();
         }
         if(StringUtils.isEmpty(value)) {
            setErrorMessage("Must specify a hub id");
            pack();
            return null;
         }
         if(!hubIdPattern.matcher(value).matches()) {
            setErrorMessage("Invalid hub id [" + value + "] must be of the form [xxx-yyyy]");
            return null;
         }
         if(value.length() == 7) {
            value = value.substring(0, 3) + "-" + value.substring(3, 7);
         }
         return value.toUpperCase();
      }

      protected void onShow() {
         hubId.requestFocusInWindow();
      }

      protected JPanel createContents() {
         submit.addActionListener((e) -> this.submit());
         Oculus.invokeOnEnter(hubId, () -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.weightx = 1;

         panel.add(instructions, gbc.clone());
         gbc.gridy++;

         panel.add(hubId, gbc.clone());
         gbc.gridy++;
         gbc.anchor = GridBagConstraints.NORTHEAST;
         gbc.weighty = 1.0;
         gbc.fill = GridBagConstraints.NONE;

         panel.add(submit, gbc.clone());

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
      SwingUtilities.invokeAndWait(HubIdPrompt::prompt);
   }
}

