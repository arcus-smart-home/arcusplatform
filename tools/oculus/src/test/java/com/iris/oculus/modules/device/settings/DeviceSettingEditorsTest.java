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

import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.booleanEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.enumEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.param;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.rangeEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import org.junit.Test;

import com.iris.oculus.modules.device.settings.DeviceSettingEditors.Editor;

public class DeviceSettingEditorsTest {
   private final List<String> edits = new ArrayList<>();
   private String lastError;

   private final DeviceSettingEditors.Listener listener = new DeviceSettingEditors.Listener() {
      @Override
      public void onEdit(String value) {
         lastError = null;
         edits.add(value);
      }

      @Override
      public void onInvalidEdit(String message) {
         lastError = message;
      }
   };

   private Editor create(Map<String, Object> entry) {
      DeviceSettingParam param = param(entry);
      Editor editor = DeviceSettingEditors.create(param, listener);
      editor.setValue(param.getValue());
      return editor;
   }

   private String lastEdit() {
      return edits.isEmpty() ? null : edits.get(edits.size() - 1);
   }

   /** Runs on the event thread, as user input does, and lets the range editor's check that follows finish. */
   private static void onEventThread(Runnable input) throws Exception {
      SwingUtilities.invokeAndWait(input);
      SwingUtilities.invokeAndWait(() -> { });
   }

   private static void type(JFormattedTextField field, String text) throws Exception {
      onEventThread(() -> field.setText(text));
   }

   private static JFormattedTextField textOf(Editor editor) {
      return ((JSpinner.DefaultEditor) ((JSpinner) editor.getComponent()).getEditor()).getTextField();
   }

   @SuppressWarnings("unchecked")
   private static JComboBox<Object> comboOf(Editor editor) {
      return (JComboBox<Object>) editor.getComponent();
   }

   private static List<String> labelsOf(JComboBox<Object> combo) {
      List<String> labels = new ArrayList<>();
      for(int i = 0; i < combo.getItemCount(); i++) {
         labels.add(combo.getItemAt(i).toString());
      }
      return labels;
   }

   @Test
   public void testEnumShowsLabelsAndSendsTheKey() {
      Editor editor = create(enumEntry("ledMode", 0, 1));
      JComboBox<Object> combo = comboOf(editor);

      assertEquals(Arrays.asList("Always Off", "Always On", "Follows Switch"), labelsOf(combo));
      assertEquals("Always On", combo.getSelectedItem().toString());
      assertTrue(edits.isEmpty());

      combo.setSelectedIndex(2);

      assertEquals("2", lastEdit());
   }

   @Test
   public void testEnumShowsAnUnlistedValueWithoutPickingAnother() {
      Editor editor = create(enumEntry("ledMode", 0, 9));
      JComboBox<Object> combo = comboOf(editor);

      assertEquals("Unlisted (9)", combo.getSelectedItem().toString());
      assertTrue(edits.isEmpty());

      editor.setValue("1");

      assertEquals("Always On", combo.getSelectedItem().toString());
      assertEquals(3, combo.getItemCount());
      assertTrue(edits.isEmpty());
   }

   @Test
   public void testRangeSendsAWholeNumberString() throws Exception {
      Editor editor = create(rangeEntry("level", 0, 99, 0, 5));

      type(textOf(editor), "42");

      assertEquals("42", lastEdit());
      assertNull(lastError);
   }

   @Test
   public void testRangeRefusesTextOutOfBoundsInsteadOfKeepingTheLastGoodNumber() throws Exception {
      Editor editor = create(rangeEntry("level", 0, 255, 0, 5));
      JFormattedTextField field = textOf(editor);

      type(field, "99");
      assertEquals("99", lastEdit());

      type(field, "999");

      assertEquals("Must be a whole number from 0 to 255", lastError);
   }

   @Test
   public void testRangeRefusesTextTheDriverCannotParse() throws Exception {
      JFormattedTextField field = textOf(create(rangeEntry("level", 0, 99, 0, 5)));

      type(field, "1.5");
      assertEquals("Must be a whole number from 0 to 99", lastError);

      type(field, "");
      assertEquals("Must be a whole number from 0 to 99", lastError);

      type(field, "abc");
      assertEquals("Must be a whole number from 0 to 99", lastError);
   }

   @Test
   public void testRangeNormalizesTheText() throws Exception {
      JFormattedTextField field = textOf(create(rangeEntry("level", 0, 99, 0, 5)));

      type(field, " 007 ");

      assertEquals("7", lastEdit());
   }

   @Test
   public void testRangeHandlesNegativeBounds() throws Exception {
      Editor editor = create(rangeEntry("offset", -3, 3, 0, -2));
      JFormattedTextField field = textOf(editor);

      assertEquals("-2", field.getText());

      type(field, "-3");

      assertEquals("-3", lastEdit());
   }

   @Test
   public void testRangeStepsAreBounded() throws Exception {
      Editor editor = create(rangeEntry("level", 0, 2, 0, 1));
      JSpinner spinner = (JSpinner) editor.getComponent();

      onEventThread(() -> spinner.setValue(spinner.getNextValue()));

      assertEquals("2", lastEdit());
      assertNull(spinner.getNextValue());
   }

   @Test
   public void testRangeShowsACurrentValueOutOfBounds() {
      Editor editor = create(rangeEntry("offset", -3, 3, 0, 255));

      assertEquals("255", textOf(editor).getText());
      assertTrue(edits.isEmpty());
      assertNull(lastError);
   }

   @Test
   public void testRangeReformatOfAnOutOfBoundsValueIsNotAnError() throws Exception {
      Editor editor = create(rangeEntry("offset", -3, 3, 0, 255));

      // what the field does on every focus change
      onEventThread(() -> textOf(editor).setValue(textOf(editor).getValue()));

      assertNull(lastError);
   }

   @Test
   public void testRangeValuesAboveIntegerMax() throws Exception {
      Editor editor = create(rangeEntry("timeout", 0, 4294967295.0, 0, 4294967295.0));
      JFormattedTextField field = textOf(editor);

      assertEquals("4294967295", field.getText());

      type(field, "4294967294");

      assertEquals("4294967294", lastEdit());
   }

   @Test
   public void testRangeBoundsOutOfOrderDoNotFail() {
      Editor editor = create(rangeEntry("broken", 10, 1, 0, 5));

      assertEquals("5", textOf(editor).getText());
   }

   @Test
   public void testRangePushOverTypedTextOfTheSameSpinnerValue() throws Exception {
      Editor editor = create(rangeEntry("level", 0, 99, 0, 5));
      type(textOf(editor), "42");
      int before = edits.size();

      onEventThread(() -> editor.setValue("5"));

      assertEquals("5", textOf(editor).getText());
      assertEquals(before, edits.size());
   }

   @Test
   public void testBooleanSendsOneAndZero() {
      Editor editor = create(booleanEntry("enabled", 0, 0));
      JCheckBox checkBox = (JCheckBox) editor.getComponent();

      assertFalse(checkBox.isSelected());
      assertTrue(edits.isEmpty());

      checkBox.setSelected(true);
      assertEquals("1", lastEdit());

      checkBox.setSelected(false);
      assertEquals("0", lastEdit());
   }

   @Test
   public void testBooleanPushDoesNotReportAnEdit() {
      Editor editor = create(booleanEntry("enabled", 0, 0));

      editor.setValue("1");

      assertTrue(((JCheckBox) editor.getComponent()).isSelected());
      assertTrue(edits.isEmpty());
   }

   @Test
   public void testUnsupportedTypeIsReadOnly() {
      Map<String, Object> entry = rangeEntry("future", 0, 9, 0, 5);
      entry.put("type", "bitmask");

      Editor editor = create(entry);

      assertEquals("5", ((JLabel) editor.getComponent()).getText());
   }

   @Test
   public void testDisableStopsTheControl() {
      Editor editor = create(rangeEntry("level", 0, 99, 0, 5));

      editor.setEnabled(false);

      assertFalse(editor.getComponent().isEnabled());
   }

   @Test
   public void testAnArrowMovesAnOutOfBoundsValueBackIntoRange() {
      Editor editor = create(rangeEntry("level", 0, 99, 10, 50));
      JSpinner spinner = (JSpinner) editor.getComponent();

      editor.setValue("255");
      assertEquals(Long.valueOf(99), spinner.getPreviousValue());
      assertEquals(null, spinner.getNextValue());

      editor.setValue("-5");
      assertEquals(Long.valueOf(0), spinner.getNextValue());
      assertEquals(null, spinner.getPreviousValue());
   }
}
