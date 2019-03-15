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
package com.iris.oculus.modules.log.ux;

import java.awt.Dialog.ModalityType;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JFrame;

import com.iris.oculus.modules.log.model.EventLogModel;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.view.ViewModel;

/**
 * 
 */
public class EventLogPopup extends BaseComponentWrapper<Window> {

   private EventLogPanel log;
   
   public EventLogPopup(ViewModel<EventLogModel> events) {
      this.log = new EventLogPanel(events);
   }

   public void show() {
      this.getComponent().setVisible(true);
   }
   
   public void hide() {
      if(this.isActive()) {
         this.getComponent().setVisible(false);
         this.dispose();
      }
   }
   
   @Override
   protected Window createComponent() {
      JDialog window = new JDialog(null, "Event Log", ModalityType.MODELESS);
      window.setAlwaysOnTop(false);
      window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      // TODO remember dimensions
      window.setSize(800, 600);
      window.add(this.log.getComponent());
      return window;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.util.BaseComponentWrapper#disposeComponent(java.awt.Component)
    */
   @Override
   protected boolean disposeComponent(Window component) {
      component.setVisible(false);
      component.dispose();
      this.log.dispose();
      return true;
   }

}

