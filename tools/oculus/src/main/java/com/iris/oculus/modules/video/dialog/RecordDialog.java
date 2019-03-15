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
package com.iris.oculus.modules.video.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.HorizontalLayout;

import com.iris.Utils;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.OculusModule;
import com.iris.oculus.widget.Dialog;

public class RecordDialog {

   public static class RecordOperation {
      private String cameraAddress;
      private boolean stream;

      public String getCameraAddress() {
         return cameraAddress;
      }

      public void setCameraAddress(String cameraAddress) {
         this.cameraAddress = cameraAddress;
      }

      public boolean isStream() {
         return stream;
      }

      public void setStream(boolean stream) {
         this.stream = stream;
      }
   }

   private static class InstanceRef {
      private static final RecordDialogImpl INSTANCE = new RecordDialogImpl();
   }

   public static ClientFuture<RecordOperation> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   public static ClientFuture<RecordOperation> prompt(String errorMessage) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt(errorMessage);
   }

   protected static void hide() {
      InstanceRef.INSTANCE.setVisible(false);
   }

   @SuppressWarnings("serial")
   private static class RecordDialogImpl extends Dialog<RecordOperation> {
      JTextField cameraAddress = new JTextField();
      JCheckBox stream = new JCheckBox();

      RecordDialogImpl() {
         super();
         setTitle("Start Recording/Streaming");
         setResizable(false);
         setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

         cameraAddress.setColumns(50);
      }

      @Override
      public ClientFuture<RecordOperation> prompt() {
         clearErrorMessage();
         return super.prompt();
      }

      public ClientFuture<RecordOperation> prompt(String errorMessage) {
         setErrorMessage(errorMessage);
         return super.prompt();
      }

      @Override
      protected RecordOperation getValue() {
         RecordOperation op = new RecordOperation();
         op.setCameraAddress(cameraAddress.getText());
         op.setStream(stream.isSelected());
         return op;
      }

      @Override
      protected JPanel createContents() {
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

         panel.add(new JLabel("Camera Address"), labels.clone());
         panel.add(cameraAddress, fields.clone());
         labels.gridy++;
         fields.gridy++;

         panel.add(new JLabel("Stream"), labels.clone());
         panel.add(stream, fields.clone());
         labels.gridy++;
         fields.gridy++;

         JPanel buttonPanel = new JPanel();
         buttonPanel.setLayout(new HorizontalLayout());
         GridBagConstraints buttons = new GridBagConstraints();
         buttons.gridy = labels.gridy + 1;
         buttons.anchor = GridBagConstraints.NORTHEAST;
         buttons.gridwidth = 2;
         buttons.weighty = 1.0;

         buttonPanel.add(new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
               RecordDialogImpl.this.dispose();
            }
         }));
         buttonPanel.add(new JButton(submitAction("Start")));
         panel.add(buttonPanel, buttons);

         return panel;
      }

   }

   @SuppressWarnings("unchecked")
   public static void main(String [] args) throws Exception {
      Bootstrap bootstrap =
            Bootstrap
            .builder()
            .withModuleClasses(OculusModule.class)
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
      SwingUtilities.invokeAndWait(RecordDialog::prompt);
   }
}

