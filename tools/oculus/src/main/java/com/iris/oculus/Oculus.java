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

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;

import com.iris.bootstrap.ServiceLocator;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.modules.status.StatusController;
import com.iris.oculus.util.Documents;
import com.iris.oculus.widget.ProgressDialog;
import com.iris.oculus.widget.ProgressDialog.Progress;

/**
 * @author tweidlin
 *
 */
public class Oculus {
   private final static Preferences PREFERENCES = Preferences.userNodeForPackage(Oculus.class);
   private volatile static Window MAIN;

   /**
    * Used to set the parent of dialogs.
    * @return
    */
   public static Window getMainWindow() {
      if(MAIN == null) {
         throw new IllegalArgumentException("No main window set!");
      }
      return MAIN;
   }
   
   public static boolean isMainWindowSet() {
      return MAIN != null;
   }

   public static void setMainWindow(Window main) {
      MAIN = main;
   }

   public static void debug(String message) {
      try {
         ServiceLocator.getInstance(StatusController.class).debug(message);
      }
      catch(Exception e) {
         // ignore
      }
   }

   public static void debug(String message, Throwable cause) {
      try {
         ServiceLocator.getInstance(StatusController.class).debug(message, cause);
      }
      catch(Exception e2) {
         // ignore
      }
   }

   public static void info(String message) {
      try {
         ServiceLocator.getInstance(StatusController.class).info(message);
      }
      catch(Exception e) {
         // ignore
      }
   }

   public static void info(String message, Throwable cause) {
      try {
         ServiceLocator.getInstance(StatusController.class).info(message, cause);
      }
      catch(Exception e2) {
         // ignore
      }
   }

   public static void error(String message, Action a) {
      try {
         ServiceLocator.getInstance(StatusController.class).info(message, a);
      }
      catch(Exception e2) {
         // ignore
      }
   }

   public static void warn(String message) {
      try {
      ServiceLocator.getInstance(StatusController.class).warn(message);
      }
      catch(Exception e) {
         // ignore
      }
   }

   public static void warn(String message, Throwable cause) {
      try {
         ServiceLocator.getInstance(StatusController.class).warn(message, cause);
      }
      catch(Exception e2) {
         // ignore
      }
   }

   public static void warn(String message, Action a) {
      try {
         ServiceLocator.getInstance(StatusController.class).warn(message, a);
      }
      catch(Exception e2) {
         // ignore
      }
   }

   public static void error(String message) {
      try {
      ServiceLocator.getInstance(StatusController.class).error(message);
      }
      catch(Exception e) {
         // ignore
      }
   }

   public static void error(String message, Throwable cause) {
      try {
         ServiceLocator.getInstance(StatusController.class).error(message, cause);
      }
      catch(Exception e2) {
         // ignore
      }
   }

   public static void info(String message, Action a) {
      try {
         ServiceLocator.getInstance(StatusController.class).error(message, a);
      }
      catch(Exception e2) {
         // ignore
      }
   }

   private static String getStackTrace(Throwable cause) {
      StringWriter writer = new StringWriter();
      PrintWriter pw = new PrintWriter(writer);
      cause.printStackTrace(pw);
      pw.flush();
      return writer.toString();
   }

   public static <V> ClientFuture<V> showProgress(ClientFuture<V> future, String message) {
      return showProgress(future, message, ProgressOptions.defaults());
   }

   public static <V> ClientFuture<V> showProgress(ClientFuture<V> future, String message, ProgressOptions o1, ProgressOptions... oN) {
      return showProgress(future, message, EnumSet.of(o1, oN));
   }

   public static <V> ClientFuture<V> showProgress(ClientFuture<V> future, String message, Set<ProgressOptions> options) {
      boolean cancellable = !options.contains(ProgressOptions.UNCANCELLABLE);
      boolean showErrorDialog = !options.contains(ProgressOptions.NO_ERROR_DIALOG);
      Progress p =
            ProgressDialog
               .builder()
               .withMessage(message)
               .withCancelEnable(cancellable)
               .show();
      AtomicBoolean cancelled = new AtomicBoolean(false);
      future.onCompletion((e) -> p.complete());
      if(showErrorDialog) {
         future.onFailure((e) -> { if(!cancelled.get()) Oculus.showError("Error processing request", e); } );
      }
      if(cancellable) {
         p.onFailure((e) -> {
            cancelled.set(true);
            future.cancel(true);
         });
      }
      return future;
   }

   public static void showDialog(String title, String message, int style) {
      JTextArea area = new JTextArea(message);
      area.setBackground(UIManager.getColor("OptionPane.background"));
      area.setEditable(false);
      JComponent content = area;

      // If the message is too long then wrap it in
      // a scrollable text area and make the font
      // fixed width.
      if (message.length() >= 128) {
         area.setFont(new Font("monospaced", Font.PLAIN, 12));

         JScrollPane jp = new JScrollPane(area);
         jp.getViewport().add(area);

         //area.setColumns(80);
         area.setLineWrap(true);

         jp.setPreferredSize(new Dimension(800,600));
         content = jp;
      }

      // Make the dialog resizable:
      // https://blogs.oracle.com/scblog/entry/tip_making_joptionpane_dialog_resizable
      final JComponent pane = content;
      pane.addHierarchyListener(new HierarchyListener() {
         public void hierarchyChanged(HierarchyEvent e) {
            Window window = SwingUtilities.getWindowAncestor(pane);
            if (window instanceof Dialog) {
               Dialog dialog = (Dialog)window;
               if (!dialog.isResizable()) {
                  dialog.setResizable(true);
               }
            }
         }
      });

      JOptionPane.showMessageDialog(MAIN, pane, title, style);
   }

   public static int showOKCancelDialog(String title, String message) {
      JTextArea area = new JTextArea(message);
      area.setBackground(UIManager.getColor("OptionPane.background"));
      area.setEditable(false);
      JComponent content = area;

      // If the message is too long then wrap it in
      // a scrollable text area and make the font
      // fixed width.
      if (message.length() >= 128) {
         area.setFont(new Font("monospaced", Font.PLAIN, 12));

         JScrollPane jp = new JScrollPane(area);
         jp.getViewport().add(area);

         //area.setColumns(80);
         area.setLineWrap(true);

         jp.setPreferredSize(new Dimension(800,600));
         content = jp;
      }

      // Make the dialog resizable:
      // https://blogs.oracle.com/scblog/entry/tip_making_joptionpane_dialog_resizable
      final JComponent pane = content;
      pane.addHierarchyListener(new HierarchyListener() {
         public void hierarchyChanged(HierarchyEvent e) {
            Window window = SwingUtilities.getWindowAncestor(pane);
            if (window instanceof Dialog) {
               Dialog dialog = (Dialog)window;
               if (!dialog.isResizable()) {
                  dialog.setResizable(true);
               }
            }
         }
      });
      Object[] options = {"Cancel", "OK"};
      return JOptionPane.showOptionDialog(MAIN, pane, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
   }


   public static String showInputDialog(String title, String message, int style) {
      return JOptionPane.showInputDialog(MAIN, message, title, style);
   }

   /**
    * Shows an error pop-up rather than just updating the notification
    * bar, this may be more appropriate for blocking operations.
    * @param string
    * @param error
    * @return
    */
   public static void showError(String title, Throwable cause) {
      String stackTrace = getStackTrace(cause);
      showDialog(title, stackTrace, JOptionPane.ERROR_MESSAGE);
   }
   
   public static void showSuccess(String title, String message) {
      showDialog(title, message, JOptionPane.INFORMATION_MESSAGE);
   }   

   public static String getPreference(String propertyName, String defaultValue) {
      return PREFERENCES.get(propertyName, defaultValue);
   }

   public static void setPreference(String propertyName, String value) {
      PREFERENCES.put(propertyName, value);
   }

   public static void updateAndRemember(String propertyName, JTextComponent component, String defaultValue) {
      String value = getPreference(propertyName, defaultValue);
      if(value != null) {
         component.setText(value);
      }
      Documents
         .addDocumentChangeListener(component.getDocument(), (d) -> {
            String text = component.getText();
            if(!StringUtils.isEmpty(text)) {
               setPreference(propertyName, text);
            }
         })
         .debounce(500);
   }

   public static void invokeOnEnter(Component c, Runnable action) {
      if (c instanceof JTextField) {
         ((JTextField)c).addActionListener((evt) -> action.run());
      } else if (c instanceof AbstractButton) {
         ((AbstractButton)c).addActionListener((evt) -> action.run());
      } else {
         c.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
               if(e.getModifiers() == 0 && e.getKeyCode() == KeyEvent.VK_ENTER) {
                  action.run();
               }
            }
         });
      }
   }

   public static enum ProgressOptions {
      UNCANCELLABLE,
      NO_ERROR_DIALOG;

      private static final Set<ProgressOptions> DEFAULTS = EnumSet.noneOf(ProgressOptions.class);

      public static Set<ProgressOptions> defaults() {
         return DEFAULTS;
      }
   }

}

