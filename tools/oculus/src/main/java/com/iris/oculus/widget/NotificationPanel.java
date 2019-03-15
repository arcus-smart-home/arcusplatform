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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.JXCollapsiblePane;

/**
 * @author tweidlin
 *
 */
public class NotificationPanel extends JXCollapsiblePane {
   public enum NotificationFlags {
      WRAP_TEXT,
      AUTO_COLLAPSE;
      
      public static Set<NotificationFlags> defaults() {
         return EnumSet.of(WRAP_TEXT, AUTO_COLLAPSE);
      }
   }
   
   private JTextComponent label;
   private JLabel actionLabel;
   private Action action;
   
   private Timer timer;
   
   public NotificationPanel() {
      this(NotificationFlags.defaults());
   }
   
   public NotificationPanel(Set<NotificationFlags> notificationFlags) {
      init(notificationFlags);
   }
   
   public void setDelay(int delayMs) {
      timer.setDelay(delayMs);
   }
   
   private void init(Set<NotificationFlags> flags) {
      setBorder(BorderFactory.createLoweredBevelBorder());
      JPanel contents = new JPanel();
      contents.setLayout(new BorderLayout());
      contents.setBackground(Color.decode("#EEE8AA"));
      
      JTextArea component = new JTextArea();
      component.setEditable(false);
      component.setBackground(contents.getBackground());
      if(flags.contains(NotificationFlags.WRAP_TEXT)) {
         component.setLineWrap(true);
         component.setWrapStyleWord(true);
      }
      contents.add(component, BorderLayout.CENTER);
      
      JLabel actionLabel = new JLabel();
      actionLabel.setForeground(Color.BLUE);
      actionLabel.setFont(contents.getFont().deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)));
      actionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      actionLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if(action != null) {
               action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "click"));
            }
            if(timer != null) {
               timer.stop();
            }
            collapse();
         }
      });
      contents.add(actionLabel, BorderLayout.SOUTH);
      
      this.actionLabel = actionLabel;
      this.label = component;
      
      this.add(contents);
      
      if(flags.contains(NotificationFlags.AUTO_COLLAPSE)) {
         this.setCollapsed(true);
         timer = new Timer(30000, (event) -> collapse());
      }
   }
   
   @Override
   public Dimension getMinimumSize() {
      Dimension parent = super.getMinimumSize();
      if(isCollapsed()) {
         return new Dimension(parent.width, 0);
      }
      return parent;
   }

   @Override
   public Dimension getPreferredSize() {
      Dimension parent = super.getPreferredSize();
      if(isCollapsed()) {
         return new Dimension(parent.width, 0);
      }
      return parent;
   }

   public void collapse() {
      setAnimated(true);
      setCollapsed(true);
   }
   
   public void info(String message) {
      show(message, null, UIManager.getIcon("OptionPane.informationIcon"));
   }
   
   public void info(String message, Action a) {
      show(message, a, UIManager.getIcon("OptionPane.informationIcon"));
   }
   
   public void warn(String message) {
      show(message, null, UIManager.getIcon("OptionPane.warningIcon"));
   }
   
   public void warn(String message, Action a) {
      show(message, a, UIManager.getIcon("OptionPane.warningIcon"));
   }
   
   public void error(String message) {
      show(message, null, UIManager.getIcon("OptionPane.errorIcon"));
   }
   
   public void error(String message, Action a) {
      show(message, a, UIManager.getIcon("OptionPane.errorIcon"));
   }
   
   public void show(String message) {
      this.show(message, null, null);
   }
   
   public void show(String message, Action action) {
      this.show(message, action, null);
   }
   
   public void show(String message, Action action, Icon icon) {
      // TODO need tiny icons
      
//      this.label.setIcon(icon);
      this.action = action;
      this.label.setText(message);
      if(action == null) {
         this.actionLabel.setText("");
      }
      else {
         this.actionLabel.setText((String) action.getValue(Action.NAME));
         this.actionLabel.setIcon((Icon) action.getValue(Action.SMALL_ICON));
      }

      this.setAnimated(false);
      this.setCollapsed(false);
      this.revalidate();
      if(timer != null) {
         timer.restart();
      }
   }
}

