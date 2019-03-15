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
package com.iris.oculus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.iris.bootstrap.ServiceLocator;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.util.ComponentWrapper;

/**
 * @author tweidlin
 *
 */
public class OculusViewBuilder {
   private List<ComponentWrapper<JMenu>> menus;
   private List<OculusSection> sections;
   
   @Inject
   public OculusViewBuilder(
         List<ComponentWrapper<JMenu>> menus,
         List<OculusSection> sections
   ) {
      this.menus = menus;
      this.sections = sections;
   }
   
   protected Component createContents() {
      if(sections.isEmpty()) {
         return new JLabel("Testing");
      }
      if(sections.size() == 1) {
         return sections.get(0).getComponent();
      }
      JTabbedPane pane = new JTabbedPane();
      for(OculusSection section: sections) {
         pane.add(section.getName(), section.getComponent());
      }
      return pane;
   }
   
   public JMenuBar createMenuBar() {
      JMenuBar menu = new JMenuBar();
      for(ComponentWrapper<JMenu> cw: menus) {
         menu.add(cw.getComponent());
      }
      return menu;
   }
   
   public JPanel create() {
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(createContents(), BorderLayout.CENTER);
      return panel;
   }
   
}

