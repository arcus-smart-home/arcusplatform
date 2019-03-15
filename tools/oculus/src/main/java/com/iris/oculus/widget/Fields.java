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

import java.awt.Component;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Function;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;

import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;
import com.iris.capability.definition.AttributeType;
import com.iris.oculus.util.Attributes;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

/**
 * 
 */
public class Fields {
   
   public static <C extends Component, V> FieldBuilder<C, V> builder(C component) {
      return new FieldBuilder<C, V>(component);
   }

   public static TextFieldBuilder textFieldBuilder() {
      return new TextFieldBuilder();
   }
   
   public static LabelFieldBuilder labelBuilder() {
      return new LabelFieldBuilder();
   }

   public static <V> ComboBoxFieldBuilder<V> comboBoxBuilder() {
      return new ComboBoxFieldBuilder<V>();
   }
   
   public static <V> ListFieldBuilder<V> listBuilder() {
      return new ListFieldBuilder<V>();
   }
   
   public static CheckBoxBuilder checkBoxBuilder() {
      return new CheckBoxBuilder();
   }
   
   public static FieldBuilder<JTextField, Date> timestampBuilder() {
      return
            Fields
               .textFieldBuilder()
               .transform(Fields::stringToDate, Fields::dateToString)
               ;
   }

   public static FormattedTextFieldBuilder<String> formattedTextFieldBuilder(String mask) {
      try {
         return new FormattedTextFieldBuilder<String>(new MaskFormatter(mask));
      }
      catch(ParseException e) {
         throw new IllegalArgumentException("Invalid mask: " + mask, e);
      }
   }

   public static FormattedTextFieldBuilder<Object> formattedTextFieldBuilder(AbstractFormatter formatter) {
   	return new FormattedTextFieldBuilder<Object>(formatter);
   }
   
   public static NumberSpinnerBuilder<Byte> byteSpinnerBuilder() {
      return new NumberSpinnerBuilder((byte) 0, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 1);
   }

   public static NumberSpinnerBuilder<Short> shortSpinnerBuilder() {
      return new NumberSpinnerBuilder((short) 0, Short.MIN_VALUE, Short.MAX_VALUE, (short) 1);
   }

   public static NumberSpinnerBuilder<Integer> intSpinnerBuilder() {
      return new NumberSpinnerBuilder(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1000);
   }

   public static FieldBuilder<JFormattedTextField, Integer> intFieldBuilder() {
      NumberFormatter intFormatter = new NumberFormatter();
      intFormatter.setMaximum(Integer.MAX_VALUE);
      intFormatter.setMinimum(Integer.MIN_VALUE);
      return new FormattedTextFieldBuilder<Integer>(intFormatter);
   }

   public static FieldBuilder<JFormattedTextField, Long> longFieldBuilder() {
      NumberFormatter formatter = new NumberFormatter();
      formatter.setMaximum(Long.MAX_VALUE);
      formatter.setMinimum(Long.MIN_VALUE);
      return new FormattedTextFieldBuilder<Long>(formatter);
   }

   public static NumberSpinnerBuilder<Float> floatSpinnerBuilder() {
      return new NumberSpinnerBuilder((float) 0.0, -Float.MAX_VALUE, Float.MAX_VALUE, (float) 0.1);
   }

   public static FieldBuilder<JFormattedTextField, Float> floatFieldBuilder() {
      NumberFormatter formatter = new NumberFormatter();
      formatter.setMaximum(Float.MAX_VALUE);
      formatter.setMinimum(-Float.MAX_VALUE);
      return new FormattedTextFieldBuilder<Float>(formatter);
   }

   public static NumberSpinnerBuilder<Double> doubleSpinnerBuilder() {
      return new NumberSpinnerBuilder((double) 0.0, -Double.MAX_VALUE, Double.MAX_VALUE, (double) 0.1);
   }
   
   public static FieldBuilder<JFormattedTextField, Double> doubleFieldBuilder() {
      NumberFormatter formatter = new NumberFormatter();
      formatter.setMaximum(Double.MAX_VALUE);
      formatter.setMinimum(-Double.MAX_VALUE);
      return new FormattedTextFieldBuilder<Double>(formatter);
   }

   public static FieldBuilder<? extends Component, ?> attributeTypeBuilder(AttributeType type) {
      return attributeTypeBuilder(type, true);
   }
   
   public static FieldBuilder<? extends Component, ?> attributeTypeBuilder(AttributeType type, boolean allowCheckbox) {
      switch(type.getRawType()) {
      case ENUM:
         return
               Fields
                  .<String>comboBoxBuilder()
                  .noteditable()
                  .withValues(type.asEnum().getValues())
                  ;
      case BOOLEAN:
         if(allowCheckbox) {
            return Fields.checkBoxBuilder();
         }
         else {
            return 
                  Fields
                     .<Boolean>comboBoxBuilder()
                     .withValues(Arrays.asList(true, false))
                     .withRenderer((v) -> v == null ? null : (v ? "True" : "False"))
                     .noteditable()
                     ;
         }
      case BYTE:
         return Fields.byteSpinnerBuilder();
      case INT:
      	return Fields.intFieldBuilder();
      case LONG:
         return Fields.longFieldBuilder();
      case DOUBLE:
         return Fields.doubleFieldBuilder();
      case TIMESTAMP:
         return Fields.timestampBuilder();
      default:
         return 
               Fields
                  .textFieldBuilder()
                  .transform(
                        Attributes.parser(type),
                        Attributes.renderer(type)
                  )
                  ;
      }
      
   }

   public static Date stringToDate(String value) {
      if(StringUtils.isEmpty(value)) {
         return null;
      }
      
      SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
      try {
         return sdf.parse(value);
      }
      catch (ParseException e) {
      	// Pull out the anything goes parser.
      	Parser parser = new Parser(TimeZone.getDefault());
      	List<DateGroup> groups = parser.parse(value);
      	if (groups != null && groups.size() == 1) {
      		DateGroup group = groups.get(0);
      		List<Date> dates = group.getDates();
      		if (dates != null && dates.size() == 1) {
      			return dates.get(0);
      		}
      	}
         throw new IllegalArgumentException("Invalid date: " +e.getMessage(), e);
      }
   }

   public static String dateToString(Date value) {
      if(value == null) {
         return null;
      }
      return value.toString();
   }

   public static class FieldBuilder<C extends Component, V> {
      private C field;
      private boolean enabled = true;
      private String name;
      private String label;
      private String tooltip;
      private Function<C, V> getter;
      private Setter<C, V> setter;
      
      private FieldBuilder() {
         
      }
      
      private FieldBuilder(C field) {
         this.field = field;
      }
      
      private FieldBuilder<C, V> field(C field) {
         this.field = field;
         return this;
      }
      
      public FieldBuilder<C, V> named(String name) {
         this.name = name;
         return this;
      }
      
      public FieldBuilder<C, V> labelled(String label) {
         this.label = label;
         return this;
      }
      
      public FieldBuilder<C, V> enabled() {
         this.enabled = true;
         return this;
      }
      
      public FieldBuilder<C, V> disabled() {
         this.enabled = false;
         return this;
      }
      
      public FieldBuilder<C, V> withToolTip(String tooltip) {
         this.tooltip = tooltip;
         return this;
      }
      
      public FieldBuilder<C, V> withGetter(Function<C, V> getter) {
         this.getter = getter;
         return this;
      }
      
      public FieldBuilder<C, V> withSetter(Setter<C, V> setter) {
         this.setter = setter;
         return this;
      }
      
      public <T> FieldBuilder<C, T> transform(Function<V, T> getTransform, Function<T, V> setTransform) {
         FieldBuilder<C, V> delegate = this;
         C field = delegate.getField();
         if(field == null) {
            throw new UnsupportedOperationException("This builder does not support transforms");
         }
         return
               new FieldBuilder<C, T>(getField())
                  .named(delegate.name)
                  .labelled(delegate.label)
                  .withToolTip(tooltip)
                  .withGetter((f) -> {
                     return getTransform.apply(delegate.getter.apply(f));
                  })
                  .withSetter((f, value) -> {
                     delegate.setter.set(f, setTransform.apply(value));
                  })
                  ;
         
      }
      
      protected C getField() {
         return field;
      }
      
      public FieldWrapper<C,V> build() {
         Utils.assertNotEmpty(label, "Label may not be empty");

         JLabel label = new JLabel();
         C field = this.getField();
         label.setText(this.label);
         label.setLabelFor(field);
         if(!StringUtils.isEmpty(name)) {
            field.setName(name);
         }
         if(!StringUtils.isEmpty(tooltip)) {
            label.setToolTipText(tooltip);
         }
         field.setEnabled(enabled);
         return new FieldImpl<C,V>(field, label, getter, setter);
      }

   }

   public static class TextFieldBuilder extends FieldBuilder<JTextField, String> {
   	
      private TextFieldBuilder() {
         super(new JTextField());
         getField().setColumns(25);

         withGetter((field) -> field.getText());
         withSetter((field, value) -> field.setText(value));
      }
      
      public TextFieldBuilder editable() {
      	getField().setEditable(true);
      	return this;
      }
      
      public TextFieldBuilder notEditable() {
      	getField().setEditable(false);
      	return this;
      }
      
      public TextFieldBuilder withColumns(int col) {
         getField().setColumns(col);
         return this;
      }
   }

   public static class LabelFieldBuilder extends FieldBuilder<JLabel, String> {
   
      private LabelFieldBuilder() {
         super(new JLabel());
         withGetter((field) -> field.getText());
         withSetter((field, value) -> {
            field.setText(value);
            field.repaint();
         });
      }
   
   }

   public static class FormattedTextFieldBuilder<V> extends FieldBuilder<JFormattedTextField, V> {

   	public FormattedTextFieldBuilder(AbstractFormatter passedFormat) {
   		super(new JFormattedTextField(passedFormat));

   		withGetter((field) -> (V) field.getValue());
         withSetter((field, value) -> field.setValue((V) value));
   	}
   }

   public static class NumberSpinnerBuilder<V extends Number> extends FieldBuilder<JSpinner, V> {
      SpinnerNumberModel model;
      
      private NumberSpinnerBuilder(V number, V min, V max, V stepSize) {
         super(new JSpinner());
         model = new SpinnerNumberModel(number, (Comparable) min, (Comparable) max, stepSize);
         getField().setModel(model);
         withGetter((field) -> (V) field.getValue());
         withSetter((field, value) -> { if (value != null) {field.setValue((V) value);}});
      }
      
      public NumberSpinnerBuilder<V> withRange(V min, V max) {
         model.setMinimum((Comparable) min);
         model.setMaximum((Comparable) max);
         return this;
      }

      public NumberSpinnerBuilder<V> withStepSize(V stepSize) {
         model.setStepSize(stepSize);
         return this;
      }

      public NumberSpinnerBuilder<V> withValue(V value) {
         model.setValue(value);
         return this;
      }
   }
   
   public static class CheckBoxBuilder extends FieldBuilder<JCheckBox, Boolean> {
      private CheckBoxBuilder() {
         super(new JCheckBox());
         withGetter((field) -> field.isSelected());
         withSetter((field, value) -> field.setSelected(Boolean.TRUE.equals(value)));
      }
   }
   
   public static class ComboBoxFieldBuilder<V> extends FieldBuilder<JComboBox<V>, V> {
      private ComboBoxBuilder<V> comboBuilder;
      private ComboBoxFieldBuilder() {
         comboBuilder = new ComboBoxBuilder<>();
         withGetter((field) -> (V) field.getSelectedItem());
         withSetter((field, value) -> field.setSelectedItem(value));
      }
      
      public ComboBoxFieldBuilder<V> withModel(ComboBoxModel<V> model) {
         comboBuilder.withModel(model);
         return this;
      }
      
      public ComboBoxFieldBuilder<V> withValues(Collection<V> values) {
         comboBuilder.withValues(values);
         return this;
      }

      public ComboBoxFieldBuilder<V> withRenderer(ListCellRenderer<? super V> renderer) {
         comboBuilder.withCellRenderer(renderer);
         return this;
      }
      
      public ComboBoxFieldBuilder<V> withRenderer(Function<V, String> renderer) {
         comboBuilder.withRenderer(renderer);
         return this;
      }
      
      public ComboBoxFieldBuilder<V> withRenderer(Function<V, String> renderer, String emptyLabel) {
         comboBuilder.withRenderer(renderer, emptyLabel);
         return this;
      }
      
      public ComboBoxFieldBuilder<V> editable() {
         comboBuilder.editable();
         return this;
      }
      
      public ComboBoxFieldBuilder<V> noteditable() {
         comboBuilder.noteditable();
         return this;
      }

      @Override
      public FieldWrapper<JComboBox<V>, V> build() {
         super.field(comboBuilder.create());
         return super.build();
      }

   }
   
   public static class ListFieldBuilder<V> extends FieldBuilder<JList<V>, V> {
      private ListBoxBuilder<V> listBuilder;
      private ListFieldBuilder() {
         listBuilder = new ListBoxBuilder<>();
         withGetter((field) -> (V) field.getSelectedValue());
         withSetter((field, value) -> field.setSelectedValue(value, true));
      }
      
      public ListFieldBuilder<V> withModel(ListModel<V> model) {
         listBuilder.withModel(model);
         return this;
      }
      
      public ListFieldBuilder<V> withValues(Collection<V> values) {
         listBuilder.withValues(values);
         return this;
      }

      public ListFieldBuilder<V> withRenderer(ListCellRenderer<? super V> renderer) {
         listBuilder.withRenderer(renderer);
         return this;
      }
      
      public ListFieldBuilder<V> withRenderer(Function<V, String> renderer) {
         listBuilder.withRenderer(renderer);
         return this;
      }
      
      public ListFieldBuilder<V> withRenderer(Function<V, String> renderer, String emptyLabel) {
         listBuilder.withRenderer(renderer, emptyLabel);
         return this;
      }
      
      /**
       * @return
       * @see com.iris.oculus.widget.ListBoxBuilder#singleSelectionMode()
       */
      public ListFieldBuilder<V> singleSelectionMode() {
         listBuilder.singleSelectionMode();
         return this;
      }

      /**
       * @return
       * @see com.iris.oculus.widget.ListBoxBuilder#singleIntervalSelectionMode()
       */
      public ListFieldBuilder<V> singleIntervalSelectionMode() {
         listBuilder.singleIntervalSelectionMode();
         return this;
      }

      /**
       * @return
       * @see com.iris.oculus.widget.ListBoxBuilder#multipleIntervalSelectionMode()
       */
      public ListFieldBuilder<V> multipleIntervalSelectionMode() {
         listBuilder.multipleIntervalSelectionMode();
         return this;
      }

      @Override
      public FieldWrapper<JList<V>, V> build() {
         super.field(listBuilder.create());
         return super.build();
      }

   }
   
   private static class FieldImpl<C extends Component, V> implements FieldWrapper<C, V> {
      private C field;
      private JLabel label;
      private Function<C, V> getter;
      private Setter<C, V> setter;
      
      FieldImpl(C field, JLabel label, Function<C, V> getter, Setter<C, V> setter) {
         this.field = field;
         this.label = label;
         this.getter = getter;
         this.setter = setter;
      }
      
      @Override
      public C getComponent() {
         return field;
      }

      @Override
      public JLabel getLabel() {
         return label;
      }

      @Override
      public V getValue() {
         if(getter == null) {
            throw new UnsupportedOperationException("No getter defined for " + field.getName());
         }
         return getter.apply(field);
      }

      @Override
      public void setValue(V value) {
         if(setter == null) {
            throw new UnsupportedOperationException("No setter defined for " + field.getName());
         }
         setter.set(field, value);
      }
      
   }
   
   @FunctionalInterface
   public interface Setter<D, V> {
      public void set(D destination, V value);
   }
}

