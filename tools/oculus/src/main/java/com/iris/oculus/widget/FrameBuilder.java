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

import javax.swing.JFrame;

import com.iris.Utils;
import com.iris.oculus.util.ComponentWrapper;

/**
 * 
 */
public class FrameBuilder {
   private String title;
   private ComponentWrapper<? extends Component> contents;
   
   public FrameBuilder withTitle(String title) {
      this.title = title;
      return this;
   }
   
   public FrameBuilder withContents(Component contents) {
      return withContents(new ComponentWrapper<Component>() {
         @Override
         public Component getComponent() {
            return contents;
         }
      });
   }
   
   public FrameBuilder withContents(ComponentWrapper<? extends Component> contents) {
      this.contents = contents;
      return this;
   }
   
   public JFrame build() {
      Utils.assertNotNull(contents, "Must specify contents for the window");
      return new ComponentWrapperFrame(title, contents);
   }
   
   private static class ComponentWrapperFrame extends JFrame {
      private ComponentWrapper<? extends Component> contents;
      
      ComponentWrapperFrame(String title, ComponentWrapper<? extends Component> contents) {
         super(title);
         this.contents = contents;
         setLayout(new BorderLayout());
      }
      
      @Override
      public void pack() {
         add(contents.getComponent(), BorderLayout.CENTER);
         super.pack();
      }

      @Override
      public void dispose() {
         Component c = contents.getComponent();
         if(contents.dispose()) {
            remove(c);
         }
         super.dispose();
      }

   }
}

