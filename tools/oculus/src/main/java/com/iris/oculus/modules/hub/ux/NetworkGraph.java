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
package com.iris.oculus.modules.hub.ux;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.iris.oculus.Main;
import com.iris.oculus.util.Actions;

/**
 * 
 */
public class NetworkGraph {

   public static JFrame show(String image) {
      byte [] input = Base64.getDecoder().decode(image);
      try(GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(input))) {
         byte [] bytes = IOUtils.toByteArray(is);
         String contents = new String(bytes, Charsets.UTF_8);
         JTextArea result = new JTextArea(contents);
         result.setEditable(false);
         
         JPanel buttons = new JPanel();
         buttons.add(new JButton(
               Actions
                  .build("Copy", (e) -> 
                     Toolkit
                        .getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(contents), null)
                  )
         ));
         
         JPanel content = new JPanel(new BorderLayout());
         content.add(new JScrollPane(result, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
         content.add(buttons, BorderLayout.SOUTH);
         
         JFrame graph = new JFrame();
         graph.setTitle("Z-Wave Network - " + new Date());
         graph.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
         graph.setContentPane(content);
         graph.pack();
         graph.setVisible(true);
         return graph;
      }
      catch(IOException e) {
         throw new RuntimeException(e);
      }
   }
 
   public static void main(String [] args) throws Exception {
      Main.setLookAndFeel();
      SwingUtilities.invokeAndWait(() -> {
         show("H4sIAAAAAAAAALWSTQuCMBzG732K4T3Q3jzEAgOPQh20IjpMG2qKk2WpRd+9FzRoDjeFdt7z4/+8HEOfojQAtxxdcZJH4D4Ar6cYdrF1SrWwTlGpO85qY5hG9aAC9jFycQxVVQOUkAxm9ILBOUAphi4pDvOKobIMi8cYA4/EhEJCUeLXGC+kXoxr0tJkSWseaSJBaviyeaQpXxyx4h1PPOOLG2l4PLHOimX6GC6Eaec49IMMaj8RfZsS0gW51fQOwPY4KuBIbksf/+2LZS9sN/QXoGA+PYBSGWpy8+3RsgSw44Wt3/uUIgR2tSwGSrX8HvbjCQv10IF9BQAA")
            .setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      });

   }
}

