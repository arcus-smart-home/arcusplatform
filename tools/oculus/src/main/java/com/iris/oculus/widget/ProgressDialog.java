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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.iris.client.event.Futures;
import com.iris.client.event.Listener;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.util.Result;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 * 
 */
public class ProgressDialog {
   
   public static ProgressDialog.Builder builder() {
      return new Builder();
   }
   
   private static class Implementation extends BaseComponentWrapper<JDialog> implements Progress {
      private SettableClientFuture<Object> result = Futures.settableFuture();
      
      private String title;
      private boolean cancellable;
      private int maxValue;
      
      private JTextArea description = new JTextArea();
      private JProgressBar progress = new JProgressBar();
      
      Implementation(String title, boolean cancellable, int maxValue) {
         this.title = title;
         this.cancellable = cancellable;
         this.maxValue = maxValue;
      }
      
      @Override
      protected JDialog createComponent() {
         
         JDialog dialog = new JDialog() {
            public void dispose() {
               result.cancel(true);
               super.dispose();               
            }
         };

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.weightx = 1.0;
         
         description.setPreferredSize(new Dimension(400, (int)(1.3 * description.getFontMetrics(description.getFont()).getHeight())));
         description.setEditable(false);
         description.setLineWrap(true);
         description.setWrapStyleWord(true);
         description.setBackground(dialog.getBackground());
         
         JProgressBar progress = new JProgressBar();
         if(maxValue > 0) {
            progress.setMinimum(0);
            progress.setMaximum(maxValue);
         }
         else {
            progress.setIndeterminate(true);
         }
         
         dialog.setTitle(title);
         dialog.setLayout(new GridBagLayout());
         dialog.setResizable(false);
         dialog.add(description, gbc.clone());
         gbc.gridy++;
         dialog.add(progress, gbc.clone());
         gbc.gridy++;
         if(cancellable) {
            dialog.add(new JSeparator(), gbc.clone());
            gbc.gridy++;
            
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener((e) -> result.cancel(true));
            dialog.add(cancel, gbc.clone());
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
         }
         else {
            dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
         }
         dialog.pack();
         
         result.onCompletion((e) -> dialog.dispose());
         
         return dialog;
      }

      @Override
      public boolean isDeterminate() {
         return !progress.isIndeterminate();
      }

      @Override
      public int getCompletion() {
         return progress.getValue();
      }

      @Override
      public void setMessage(String message) {
         description.setText(message);
         if(isActive()) {
            getComponent().pack();
         }
      }

      @Override
      public void updateProgress(int progress) {
         this.progress.setValue(progress);
      }

      @Override
      public void complete() {
         result.setValue(null);
      }

      public Progress onSuccess(Listener<Object> listener) {
         result.onSuccess(listener);
         return this;
      }

      public Progress onFailure(Listener<Throwable> listener) {
         result.onFailure(listener);
         return this;
      }

      public Progress onCompletion(Listener<Result<Object>> listener) {
         result.onCompletion(listener);
         return this;
      }

   }
   
   public static class Builder {
      private String title;
      private String message;
      private boolean cancellable;
      private int maxValue;
      
      public Builder withTitle(String title) {
         this.title = title;
         return this;
      }
      
      public Builder withMessage(String message) {
         this.message = message;
         return this;
      }
      
      public Builder withCancelEnable(boolean cancellable) {
         this.cancellable = cancellable;
         return this;
      }
      
      public Builder withMaxValue(int maxValue) {
         this.maxValue = maxValue;
         return this;
      }
      
      public Builder indeterminate() {
         this.maxValue = -1;
         return this;
      }
      
      public Progress show() {
         Implementation i = new Implementation(title, cancellable, maxValue);
         i.setMessage(message);
         JDialog jd = i.getComponent();
         jd.pack();
         SwingUtilities.invokeLater(() -> jd.setVisible(true));
         return i;
      }
   }
   
   public static interface Progress {
      
      public boolean isDeterminate();
      
      public int getCompletion();
      
      public void setMessage(String message);
      
      public void updateProgress(int progress);
      
      public void complete();
      
      public Progress onSuccess(Listener<Object> listener);

      public Progress onFailure(Listener<Throwable> listener);

      public Progress onCompletion(Listener<Result<Object>> listener);
   }

   public static void main(String [] args) {
      ProgressDialog
         .builder()
         .withCancelEnable(true)
         .withMessage("Wait for ever...")
         .show()
         .onFailure((e) -> System.out.println("Cancelled..."))
         .onCompletion((e) -> System.out.println("Done!"));
   }
}

