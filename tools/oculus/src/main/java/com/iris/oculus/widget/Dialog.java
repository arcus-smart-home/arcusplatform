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
package com.iris.oculus.widget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.EnumSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.SettableClientFuture;
import com.iris.oculus.Oculus;
import com.iris.oculus.widget.NotificationPanel.NotificationFlags;

/**
 * 
 */
@SuppressWarnings("serial")
public abstract class Dialog<V> extends JDialog {
   private boolean reentrantFuture;
   private NotificationPanel notification;
   private SettableClientFuture<V> result;
   private boolean initd = false;
   
   protected Dialog() {
      this(false);
   }
   
   protected Dialog(boolean reentrantFuture) {
      this.reentrantFuture = reentrantFuture;
      this.setMinimumSize(new Dimension(350, 0));
      this.setMaximumSize(Toolkit.getDefaultToolkit().getScreenSize());
      if (Oculus.isMainWindowSet()) {
         this.setLocationRelativeTo(Oculus.getMainWindow());
      }
      this.notification = new NotificationPanel(EnumSet.noneOf(NotificationFlags.class));
   }
   
   public ClientFuture<V> prompt() {
      SettableClientFuture<V> result = reset();
      SwingUtilities.invokeLater(() -> { pack(); setVisible(true); });
      return result;
   }
   
   private SettableClientFuture<V> reset() {
      if(result != null && !result.isDone()) {
         if(reentrantFuture) {
            return result;
         }
         
         result.cancel(true);
      }
      result = Futures.settableFuture();
      return result;
   }
   
   public void setErrorMessage(String message) {
      notification.error(message);
      notification.invalidate();
      pack();
   }
   
   public void setErrorMessage(String message, Action action) {
      notification.error(message, action);
      pack();
   }
   
   public void clearErrorMessage() {
      notification.collapse();
      pack();
   }
   
   @Override
   public void setVisible(boolean b) {
      if(b == true) {
         init();
      }
      super.setVisible(b);
   }
   
   @Override
   public void dispose() {
      super.dispose();
      onHide();
   }

   protected abstract V getValue();
   
   protected abstract Component createContents();
   
   protected Action cancelAction() {
      return new AbstractAction("Cancel") {
         @Override
         public void actionPerformed(ActionEvent e) {
            dispose();
         }
      };
   }
   
   protected Action submitAction() {
      return submitAction("Ok");
   }
   
   protected Action submitAction(String label) {
      return new AbstractAction(label) {
         @Override
         public void actionPerformed(ActionEvent e) {
            submit();
         }
      };
   }
   
   protected void submit() {
      try {
         V value = getValue();
         if(value == null) {
            return;
         }
         setVisible(false);
         if(result != null) {
            result.setValue(value);
            result = null;
         }
      }
      catch(Exception e) {
         setErrorMessage(e.getMessage());
      }
   }
   
   protected void onShow() {
      pack();
   }
   
   protected void onHide() {
      if(result != null) {
         result.cancel(true);
         result = null;
      }
   }
   
   private void init() {
      if(initd == true) {
         return;
      }
      initd = true;
      
      notification.setFont(notification.getFont().deriveFont(Font.BOLD));
      
      addComponentListener(new ComponentAdapter() {
         @Override
         public void componentShown(ComponentEvent e) {
            Dialog.this.onShow();
         }

         @Override
         public void componentHidden(ComponentEvent e) {
            Dialog.this.onHide();
         }
         
      });
      
      setLayout(new BorderLayout(10, 10));
      add(notification, BorderLayout.NORTH);
      Component c = createContents();
      if(c instanceof JComponent) {
         ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
      }
      add(c, BorderLayout.CENTER);
      pack();
   }

}

