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
package com.iris.oculus.modules.device.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.iris.client.capability.DeviceMock;
import com.iris.client.model.DeviceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.device.mockaction.MockAction;
import com.iris.oculus.modules.device.mockaction.MockActionsNexus;
import com.iris.oculus.util.Actions;

@SuppressWarnings("serial")
public class MockActionsDialog extends JDialog {
   
   private JTabbedPane tabbedPane;
   private NavigableMap<String,Component> tabs = new TreeMap<>();
   private MockActionsNexus nexus;
   private DeviceModel model = null;
   
   private Action close = Actions.build("Close", () -> close());
   
   public MockActionsDialog(MockActionsNexus nexus) {
      super(Oculus.getMainWindow(), "Mock Actions");
      setModal(false);
      this.nexus = nexus;
      initContents(nexus.getAllMockActions());
      addWindowListener(new WindowAdapter() {         
         @Override
         public void windowClosing(WindowEvent e) {
            close();
         }
      });
   }
   
   public boolean show(DeviceModel model) {
      this.model = model;
      this.tabbedPane.removeAll();
      Collection<String> caps = model.getCaps();
      if (caps.contains(DeviceMock.NAMESPACE)) {
         for (String tab : tabs.navigableKeySet()) {
            if (caps.contains(tab)) {
               tabbedPane.add(tab, tabs.get(tab));
            }
         }
         pack();
         setVisible(true);
         return true;
      }
      else {
         return false;
      }
   }
   
  void close() {
      model = null;
      this.setVisible(false);
   }
   
   private void initContents(Map<String,List<MockAction>> actions) {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
      
      tabbedPane = new JTabbedPane();
      for (String cap : actions.keySet()) {
         Component component = createActions(actions.get(cap));
         tabbedPane.addTab(cap, component);
         tabs.put(cap, component);
      }
      
      JPanel buttonBar = new JPanel(new BorderLayout());
      JButton closeButton = new JButton(close);
      buttonBar.add(closeButton, BorderLayout.PAGE_END);
      
      panel.add(tabbedPane);
      panel.add(Box.createVerticalStrut(10));
      panel.add(buttonBar);
      
      getContentPane().add(panel);
      this.pack();
   }
   
   private Component createActions(List<MockAction> actions) {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
      for (MockAction mockAction : actions) {
         Action action = Actions.build(mockAction.getName(), new DoItDammit(mockAction));
         panel.add(new JButton(action));
      }
      return panel;
   }
   
   private class DoItDammit implements Consumer<ActionEvent> {
      private MockAction mockAction;
      
      private DoItDammit(MockAction mockAction) {
         this.mockAction = mockAction;
      }
      
      @Override
      public void accept(ActionEvent e) {
         nexus.performAction(mockAction, model, (err) -> onMockActionFailed(err));
      }
   }
   
   private void onMockActionFailed(Throwable error) {
      JOptionPane.showMessageDialog(this, "Unable to perform mock action: " + error.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
   }
   
   public static interface OnClosedCallback {
      void onClosed();
   }
}



