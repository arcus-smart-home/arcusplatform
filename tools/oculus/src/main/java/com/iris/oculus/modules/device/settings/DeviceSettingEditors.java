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
package com.iris.oculus.modules.device.settings;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Builds the control for a device setting. Each control works in the strings
 * SetParam takes. Only input from the user reaches the listener, values pushed
 * in with {@link Editor#setValue(String)} do not.
 */
public final class DeviceSettingEditors {

   public interface Listener {
      void onEdit(String value);

      void onInvalidEdit(String message);
   }

   public interface Editor {
      Component getComponent();

      void setValue(String value);

      void setEnabled(boolean enabled);
   }

   public static Editor create(DeviceSettingParam param, Listener listener) {
      switch(param.getType()) {
      case ENUM:
         return new EnumEditor(param, listener);
      case RANGE:
         return new RangeEditor(param, listener);
      case BOOLEAN:
         return new BooleanEditor(listener);
      default:
         return new ReadOnlyEditor(param);
      }
   }

   private DeviceSettingEditors() {
   }

   private static class Option {
      private final String key;
      private final String label;

      Option(String key, String label) {
         this.key = key;
         this.label = label;
      }

      @Override
      public String toString() {
         return label;
      }
   }

   private static class EnumEditor implements Editor {
      private final DeviceSettingParam param;
      private final DefaultComboBoxModel<Option> options = new DefaultComboBoxModel<>();
      private final JComboBox<Option> combo = new JComboBox<>(options);
      private Option unlisted;
      private boolean updating;

      EnumEditor(DeviceSettingParam param, Listener listener) {
         this.param = param;
         param.getValues().forEach((key, label) -> options.addElement(new Option(key, label)));
         combo.addActionListener((event) -> {
            Option selected = (Option) combo.getSelectedItem();
            if(!updating && selected != null) {
               listener.onEdit(selected.key);
            }
         });
      }

      @Override
      public Component getComponent() {
         return combo;
      }

      /** A value the driver doesn't list gets its own entry rather than showing another option as selected. */
      @Override
      public void setValue(String value) {
         updating = true;
         try {
            if(unlisted != null) {
               options.removeElement(unlisted);
               unlisted = null;
            }
            Option selected = find(value);
            if(selected == null && value != null) {
               unlisted = new Option(value, param.describe(value));
               options.addElement(unlisted);
               selected = unlisted;
            }
            combo.setSelectedItem(selected);
         }
         finally {
            updating = false;
         }
      }

      private Option find(String key) {
         for(int i = 0; i < options.getSize(); i++) {
            if(options.getElementAt(i).key.equals(key)) {
               return options.getElementAt(i);
            }
         }
         return null;
      }

      @Override
      public void setEnabled(boolean enabled) {
         combo.setEnabled(enabled);
      }
   }

   /**
    * Validates the text on every change rather than the spinner's committed
    * value, which keeps the last good number while the text is out of range and
    * would otherwise be what gets saved.
    *
    * The check runs once the text settles. The field replaces its text on every
    * focus change by emptying it first, and clicking Save moves focus after the
    * button is armed, so reacting to the empty text would disable the button
    * under the mouse and lose the click.
    */
   private static class RangeEditor implements Editor {
      private final DeviceSettingParam param;
      private final Listener listener;
      private final JSpinner spinner;
      private final JFormattedTextField field;
      private String shown;
      private boolean updating;
      private boolean checkQueued;

      RangeEditor(DeviceSettingParam param, Listener listener) {
         this.param = param;
         this.listener = listener;
         this.spinner = new JSpinner(createModel(param.getMin(), param.getMax()));
         this.spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
         this.field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
         this.field.setColumns(8);
         this.field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
               queueCheck();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
               queueCheck();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
               queueCheck();
            }
         });
      }

      /** SpinnerNumberModel refuses a value outside its bounds or bounds that are out of order. */
      private static SpinnerNumberModel createModel(Long min, Long max) {
         boolean usable = min == null || max == null || min <= max;
         Long minimum = usable ? min : null;
         Long maximum = usable ? max : null;
         Long initial = Long.valueOf(0);
         if(minimum != null && initial < minimum) {
            initial = minimum;
         }
         if(maximum != null && initial > maximum) {
            initial = maximum;
         }
         return new SpinnerNumberModel(initial, minimum, maximum, Long.valueOf(1));
      }

      private void queueCheck() {
         if(updating || checkQueued) {
            return;
         }
         checkQueued = true;
         SwingUtilities.invokeLater(() -> {
            checkQueued = false;
            checkText();
         });
      }

      private void checkText() {
         String text = field.getText().trim();
         // a pushed value is reformatted like any other and may be out of range
         if(text.equals(shown)) {
            listener.onEdit(text);
            return;
         }

         String error = param.validate(text);
         if(error != null) {
            listener.onInvalidEdit(error);
         }
         else {
            listener.onEdit(Long.valueOf(text).toString());
         }
      }

      @Override
      public Component getComponent() {
         return spinner;
      }

      /** The current value is shown even if it is out of range, the device can report one. */
      @Override
      public void setValue(String value) {
         Long number = toNumber(value);
         if(number == null) {
            return;
         }

         updating = true;
         try {
            shown = value;
            spinner.setValue(number);
            // the spinner ignores its existing value but the text may have been edited
            field.setValue(number);
         }
         finally {
            updating = false;
         }
      }

      private static Long toNumber(String value) {
         try {
            return value == null ? null : Long.valueOf(value);
         }
         catch(NumberFormatException e) {
            return null;
         }
      }

      @Override
      public void setEnabled(boolean enabled) {
         spinner.setEnabled(enabled);
      }
   }

   private static class BooleanEditor implements Editor {
      private final JCheckBox checkBox = new JCheckBox();
      private boolean updating;

      BooleanEditor(Listener listener) {
         checkBox.addItemListener((event) -> {
            if(!updating) {
               listener.onEdit(checkBox.isSelected() ? "1" : "0");
            }
         });
      }

      @Override
      public Component getComponent() {
         return checkBox;
      }

      @Override
      public void setValue(String value) {
         updating = true;
         try {
            checkBox.setSelected(value != null && !"0".equals(value));
         }
         finally {
            updating = false;
         }
      }

      @Override
      public void setEnabled(boolean enabled) {
         checkBox.setEnabled(enabled);
      }
   }

   private static class ReadOnlyEditor implements Editor {
      private final DeviceSettingParam param;
      private final JLabel label = new JLabel();

      ReadOnlyEditor(DeviceSettingParam param) {
         this.param = param;
      }

      @Override
      public Component getComponent() {
         return label;
      }

      @Override
      public void setValue(String value) {
         label.setText(param.describe(value));
      }

      @Override
      public void setEnabled(boolean enabled) {
         label.setEnabled(enabled);
      }
   }
}
