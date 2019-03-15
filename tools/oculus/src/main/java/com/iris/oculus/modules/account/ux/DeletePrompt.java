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
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.iris.Utils;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.widget.Dialog;

public class DeletePrompt {

   private static class InstanceRef {
      private static final DeleteDialog INSTANCE = new DeleteDialog();
   }

   public static ClientFuture<Boolean> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   @SuppressWarnings("serial")
   private static class DeleteDialog extends Dialog<Boolean> {
      private JLabel deleteLoginLabel = new JLabel("Include Owner Login:");
      private JCheckBox deleteLoginCheck = new JCheckBox();
      private JButton submit = new JButton("Submit");

      @Override
      protected Boolean getValue() {
         return deleteLoginCheck.isSelected();
      }

      @Override
      protected Component createContents() {
         deleteLoginCheck.setSelected(true);
         submit.addActionListener((e) -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         addLabelAndInput(panel, deleteLoginLabel, deleteLoginCheck, gbc);

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

