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
package com.iris.oculus.modules.account;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.account.ux.AccountViewBuilder;

/**
 * @author tweidlin
 *
 */
@Singleton
public class AccountSection implements OculusSection {
   private AccountController controller;
   
   @Inject
   public AccountSection(AccountController controller) {
      this.controller = controller;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "My Account";
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getComponent()
    */
   @Override
   public Component getComponent() {
      Component c = new AccountViewBuilder(controller).build();
      c.addComponentListener(new ComponentAdapter() {

         /* (non-Javadoc)
          * @see java.awt.event.ComponentAdapter#componentShown(java.awt.event.ComponentEvent)
          */
         @Override
         public void componentShown(ComponentEvent e) {
            controller.refreshAccount();
         }
      });
      return c;
   }

}

