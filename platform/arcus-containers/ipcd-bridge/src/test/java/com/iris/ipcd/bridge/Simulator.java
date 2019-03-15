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
package com.iris.ipcd.bridge;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Simulator {
   public static void main(String[] args) {
      BlockingQueue<String> commands = new ArrayBlockingQueue<String>(20);
      MainFrame mainFrame = new MainFrame(commands);
      mainFrame.setVisible(true);
      
      FakeAOSmithWaterHeater faker = new FakeAOSmithWaterHeater(commands);
      Thread fakerThread = new Thread(faker);
      fakerThread.start();
   }
   
   @SuppressWarnings("serial")
   private static class MainFrame extends JFrame {
      private final BlockingQueue<String> commands;
      private JTextField input;
      private JButton submit;
      private JButton pollnow;
      
      private MainFrame(BlockingQueue<String> commandQueue) {
         super("A.O. Smith Water Heater Emulator");
         this.commands = commandQueue;
         this.input = new JTextField(48);
         this.submit = new JButton("Submit");
         this.pollnow = new JButton("Poll Now Dammit!");
         
         this.submit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               String cmd = input.getText();
               if (cmd != null && cmd.length() > 0) {
                  try {
                     commands.put(cmd);
                  } catch (InterruptedException e1) {
                     e1.printStackTrace();
                  }
               }
            }
            
         });
         
         this.pollnow.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               try {
                  commands.put("poll");
               } catch (InterruptedException e1) {
                  e1.printStackTrace();
               }
            }
            
         });
         
         initLayout();
         setDefaultCloseOperation(EXIT_ON_CLOSE);
      }
      
      private void initLayout() {
         setSize(800, 140);
         JPanel p = new JPanel();
         p.setLayout(new FlowLayout());
         p.add(input);
         p.add(submit);
         p.add(pollnow);
         add(p);
      }
   }
}

