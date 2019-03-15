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
package com.iris.oculus.modules.status;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

public class ShowExceptionAction extends AbstractAction {
   private final String stackTrace;
   
   public ShowExceptionAction(Throwable cause) {
      super("Show Stack Trace");
      StringWriter writer = new StringWriter();
      PrintWriter pw = new PrintWriter(writer);
      cause.printStackTrace(pw);
      pw.flush();
      stackTrace = writer.toString();
   }
   
   @Override
   public void actionPerformed(ActionEvent e) {
      JTextArea area = new JTextArea(stackTrace);
      area.setBackground(UIManager.getColor("OptionPane.background"));
      area.setEditable(false);
      JOptionPane.showMessageDialog(null, area, "Stack Trace", JOptionPane.ERROR_MESSAGE);
   }
   
}

