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
package com.iris.oculus.widget;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.base.Preconditions;
import com.iris.oculus.util.Documents;

@SuppressWarnings("serial")
public class FormattedTextField extends JFormattedTextField implements ValidatedControl {
   
   private static final Color ERROR_BACKGROUND = new Color(255,215,215);
   private Color backgroundColor;
   private List<Validator> validators = new ArrayList<>();
   private String validationMessage = null;
   
   public FormattedTextField() {
      init();
   }
   
   public FormattedTextField(Format format) {
      super(new ParseAllFormat(format));
      init();
   }
   
   private void init() {
      setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
      updateBackgroundOnEachUpdate();
      addFocusListener(new MousePositionCorrectorListener());
   }
   
   @Override
   public void updateUI() {
      super.updateUI();
      backgroundColor = getBackground();
   }

   @Override
   public void setValue(Object value) {
      try {
         AbstractFormatter formatter = getFormatter();
         if (formatter != null) {
            formatter.valueToString(value);
         }
         int old_caret_position = getCaretPosition();
         super.setValue(value);
         setCaretPosition(Math.min(old_caret_position, getText().length()));
      }
      catch (ParseException ex) {
         // Do nothing since we need to update background regardless
      }
      updateBackground();
   }

   @Override
   protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
      if (validateContent()) {
         return super.processKeyBinding(ks, e, condition, pressed)
               && ks != KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
      }
      else {
         return super.processKeyBinding(ks, e, condition, pressed);
      }
   }
   
   public void addValidator(Validator validator) {
      validators.add(validator);
      updateBackground();
   }

   private void updateBackgroundOnEachUpdate() {
      Documents.addDocumentChangeListener(getDocument(), this::updateBackground);
   }
   
   private void updateBackground() {
      setBackground(validateContent() ? backgroundColor : ERROR_BACKGROUND);
   }
   
   @Override
   public boolean validateContent() {
      AbstractFormatter formatter = getFormatter();
      String text = getText();
      if (formatter != null && text != null && !text.isEmpty()) {
         try {
            formatter.stringToValue(text);
         }
         catch (ParseException ex){
            return false;
         }
      }
      return checkValidators(text);
   }
   
   @Override
   public String validationMessage() {
      return validationMessage;
   }

   public void setValidationMessage(String message) {
      validationMessage = message;
   }
   
   private boolean checkValidators(String value) {
      for (Validator validator : validators) {
         if (!validator.validate(value)) {
            return false;
         }
      }
      return true;
   }
   
   public static interface Validator {
      boolean validate(String value);
   }
   
   private static class MousePositionCorrectorListener extends FocusAdapter {
      @Override
      public void focusGained(FocusEvent e) {
         final JTextField field = (JTextField) e.getSource();
         final int dot = field.getCaret().getDot();
         final int mark = field.getCaret().getMark();
         if (field.isEnabled() && field.isEditable()) {
            SwingUtilities.invokeLater(() -> {
               if (dot == mark ) {
                  field.getCaret().setDot(dot);
               }
            });
         }
      }
   }
   
   private static class ParseAllFormat extends Format {
      private final Format delegate;
      
      ParseAllFormat(Format delegate) {
         this.delegate = delegate;
      }

      @Override
      public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
         return delegate.format(obj, toAppendTo, pos);
      }

      @Override
      public Object parseObject(String source, ParsePosition pos) {
         int initialIndex = pos.getIndex();
         Object result = delegate.parseObject(source, pos);
         if (result != null && pos.getIndex() < source.length()) {
            int errorIndex = pos.getIndex();
            pos.setIndex(initialIndex);
            pos.setErrorIndex(errorIndex);
            return null;
         }
         return result;
      }

      @Override
      public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
         return delegate.formatToCharacterIterator(obj);
      }
   }
   
   public static Builder builder() {
      return new Builder();
   }
   
   public enum FieldType {
      INTEGER, TEXT, DOUBLE
   }
   
   public static class Builder {
      private FieldType type = FieldType.TEXT;
      private boolean useGrouping = true;
      private Integer minLength = null;
      private Integer maxLength = null;
      private boolean isRequired = false;
      private Object value = null;
      private String validationMessage = null;
      
      private Builder() {}
      
      public Builder setType(FieldType type) {
         this.type = type;
         return this;
      }
      
      public Builder setUseGrouping(boolean useGrouping) {
         this.useGrouping = useGrouping;
         return this;
      }
      
      public Builder setMinLength(Integer minLength) {
         Preconditions.checkArgument(minLength == null || minLength >= 0);
         this.minLength = minLength;
         return this;
      }
      
      public Builder setMaxLength(Integer maxLength) {
         Preconditions.checkArgument(maxLength == null || maxLength > 0);
         this.maxLength = maxLength;
         return this;
      }
      
      public Builder setIsRequired(boolean isRequired) {
         this.isRequired = isRequired;
         return this;
      }
      
      public Builder setValue(Object value) {
         this.value = value;
         return this;
      }
      
      public Builder setValidationMessage(String message) {
         this.validationMessage = message;
         return this;
      }
      
      public FormattedTextField build() {
         Format format = getFormat();
         FormattedTextField field = format != null ? new FormattedTextField(format) : new FormattedTextField();
         if (minLength != null) {
            field.addValidator((v) -> {
               return v == null || v.isEmpty() || v.length() >= minLength;
            });
         }
         if (maxLength != null) {
            field.addValidator((v) -> {
               return v == null || v.isEmpty() || v.length() <= maxLength;
            });
         }
         if (isRequired) {
            field.addValidator((v) -> {
               return v != null && !v.trim().isEmpty();
            });
         }
         field.setValidationMessage(validationMessage);
         if (value != null) {
            field.setValue(value);
         }
         return field;
      }
      
      private Format getFormat() {
         switch(type) {
         case INTEGER:
         {
            NumberFormat format = NumberFormat.getIntegerInstance();
            format.setGroupingUsed(useGrouping);
            format.setMinimumIntegerDigits(minLength);
            format.setMaximumIntegerDigits(maxLength);
            return format;
         }
         case DOUBLE:
         {
            NumberFormat format = NumberFormat.getNumberInstance();
            format.setGroupingUsed(useGrouping);
            return format;
         }
         default:
            return null;
         }
      }
   }
}

