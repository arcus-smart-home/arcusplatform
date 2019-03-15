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
package com.iris.oculus.modules.account.ux;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.iris.oculus.modules.account.AccountController;
import com.iris.oculus.modules.capability.ux.ModelTableView;

/**
 *
 */
public class AccountViewBuilder {
   private AccountController controller;

   private ModelTableView view;

   public AccountViewBuilder(AccountController controller) {
      this.view = new ModelTableView();
      this.view.bind(controller.getAccountSelectionModel());
      this.controller = controller;
   }

   protected Component createCapabilityTable() {
      JScrollPane pane = new JScrollPane(view.getComponent());
      return pane;
   }

   protected Component createDeviceToolbar() {
      AccountToolbar toolbar = new AccountToolbar();
      toolbar.setController(controller);
      controller.addAccountListener((account) -> toolbar.setModel(account));
      return toolbar;
   }

   public Component build() {
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setPreferredSize(new Dimension(600, 450));
      panel.add(createCapabilityTable(), BorderLayout.CENTER);
      panel.add(createDeviceToolbar(), BorderLayout.SOUTH);
      return panel;
   }

//   public static void main(String [] args) throws Exception {
//      Main.setLookAndFeel();
//      Bootstrap bootstrap =
//            Bootstrap
//               .builder()
//               .withModuleClasses(OculusModule.class)
//               .build();
//      ServiceLocator.init(bootstrap.bootstrap());
//
//      DeviceController controller = ServiceLocator.getInstance(DeviceController.class);
//      StatusController statusController = ServiceLocator.getInstance(StatusController.class);
//      Main.launch(new DeviceViewBuilder(controller).build());
//
//      Device dev1 = new Device();
//      dev1.setId(UUID.randomUUID());
//      dev1.setCaps(new LinkedHashSet<>(Arrays.asList(
//            DeviceBase.NAMESPACE,
//            DeviceAdvanced.NAMESPACE,
//            DeviceConnection.NAMESPACE,
//            DevicePower.NAMESPACE,
//            Switch.NAMESPACE
//      )));
//      dev1.setImage(new URL("http://deviceimages.com/swit"));
//      dev1.setDevTypeHint("swit");
//      dev1.setName("Switch");
//      controller.getDeviceStore().add(new DeviceModel(dev1));
//
//      Device dev2 = new Device();
//      dev2.setId(UUID.randomUUID());
//      dev2.setCaps(new LinkedHashSet<>(Arrays.asList(
//            DeviceBase.NAMESPACE,
//            DeviceAdvanced.NAMESPACE,
//            DeviceConnection.NAMESPACE,
//            DevicePower.NAMESPACE,
//            Contact.NAMESPACE
//      )));
//      dev2.setImage(new URL("http://deviceimages.com/sensor"));
//      dev2.setDevTypeHint("sensor");
//      dev2.setName("Contact Sensor");
//      controller.getDeviceStore().add(new DeviceModel(dev2));
//
//      Device dev3 = new Device();
//      dev3.setId(UUID.randomUUID());
//      dev3.setDevTypeHint("invalid");
//      dev3.setName("No Capabilities");
//      controller.getDeviceStore().add(new DeviceModel(dev3));
//
//      Timer timer = new Timer(5000, new ActionListener() {
//         int count = 1;
//         @Override
//         public void actionPerformed(ActionEvent e) {
//            System.out.println("UPDATE: " + count);
//            ClientMessage message =
//                  ClientMessage
//                     .builder()
//                     .withSource("DRIV::" + dev2.getId())
//                     .withPayload(
//                           new DeviceEvent(
//                                 DeviceBase.EVENT_DEVICE_VALUE_CHANGE.getName(),
//                                 Collections.singletonMap(DeviceBase.ATTR_NAME.getName(), "Switch " + count)
//                           )
//                     )
//                     .create();
//            ((DeviceStore) controller.getDeviceStore()).onEvent(message);
//            count++;
//         }
//      });
//      timer.setRepeats(true);
//      timer.start();
//
//      Timer statusTimer = new Timer(10000, new ActionListener() {
//         int count = 1;
//         @Override
//         public void actionPerformed(ActionEvent e) {
//
//            switch(count % 4) {
//            case 0:
//               statusController.debug("DEBUG " + count++);
//               break;
//            case 1:
//               statusController.info("INFO " + count++);
//               break;
//            case 2:
//               statusController.warn("WARN " + count++);
//               break;
//            case 3:
//               statusController.error("ERROR " + count++);
//               break;
//            }
//         }
//      });
//      statusTimer.setInitialDelay(0);
//      statusTimer.setRepeats(true);
////      statusTimer.start();
//
//   }
}

