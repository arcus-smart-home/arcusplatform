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

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.iris.client.capability.Capability.AddedEvent;
import com.iris.oculus.widget.Dialog;

/**
 * 
 */
public class HubRegistrationDialog extends Dialog<AddedEvent> {
   private AddedEvent event;
   
   public HubRegistrationDialog() {
      init();
   }
   
   private void init() {
      setTitle("Registering Hub...");
      setModal(true);
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
   }
   
   @Override
   protected AddedEvent getValue() {
      return event;
   }

   @Override
   protected Component createContents() {
      JLabel label = new JLabel("Waiting for the hub to be registered, this will wait indefinitely, hit 'Cancel' to exit.");
      JButton cancel = new JButton("Cancel");
      cancel.addActionListener((e) -> this.dispose());
      
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(label, BorderLayout.CENTER);
      panel.add(cancel, BorderLayout.SOUTH);
      return panel;
   }

   public void submit(AddedEvent event) {
      this.event = event;
      super.submit();
   }

}

