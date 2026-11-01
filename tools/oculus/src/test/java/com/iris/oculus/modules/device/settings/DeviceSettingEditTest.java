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

import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.driverOnlyEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.enumEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.param;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.rangeEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.withValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.iris.oculus.modules.device.settings.DeviceSettingEdit.Status;

public class DeviceSettingEditTest {
   private final Map<String, Object> entry = rangeEntry("level", 0, 99, 10, 50);
   private final DeviceSettingEdit edit = new DeviceSettingEdit(param(entry));

   private void changeAttribute(double value) {
      edit.onParamChanged(withValue(edit.getParam(), entry, value));
   }

   private void assertStatus(Status status, String text) {
      assertEquals(status, edit.getStatus());
      assertEquals(text, edit.getStatusText());
   }

   @Test
   public void testStartsClean() {
      assertFalse(edit.hasUnsavedEdit());
      assertFalse(edit.canSave());
      assertTrue(edit.canEdit());
      assertTrue(edit.canReset());
      assertTrue(edit.canRead());
      assertEquals("50", edit.getEditorValue());
      assertStatus(Status.NONE, "");
   }

   @Test
   public void testEditBackToTheSavedValueIsNotAnEdit() {
      edit.onEdit("60");
      assertTrue(edit.canSave());

      edit.onEdit("50");
      assertFalse(edit.canSave());
      assertFalse(edit.hasUnsavedEdit());
   }

   @Test
   public void testInvalidEditCannotBeSaved() {
      edit.onEdit("60");
      edit.onInvalidEdit("Must be a whole number from 0 to 99");

      assertFalse(edit.canSave());
      assertTrue(edit.hasUnsavedEdit());
      assertStatus(Status.ERROR, "Must be a whole number from 0 to 99");

      edit.onEdit("61");
      assertTrue(edit.canSave());
      assertStatus(Status.NONE, "");
   }

   @Test
   public void testSaveLocksTheRowUntilAnswered() {
      edit.onEdit("60");

      assertEquals("60", edit.beginSave());

      assertFalse(edit.canEdit());
      assertFalse(edit.canSave());
      assertFalse(edit.canReset());
      assertFalse(edit.canRead());
      assertStatus(Status.NONE, "Sending...");
   }

   @Test
   public void testPendingResolvesWhenTheAttributeShowsTheValue() {
      edit.onEdit("60");
      edit.beginSave();

      edit.onSetResponse("PENDING");

      assertEquals(Status.PENDING, edit.getStatus());
      assertTrue(edit.getStatusText().startsWith("PENDING"));
      assertEquals("60", edit.getEditorValue());
      assertFalse(edit.hasUnsavedEdit());

      changeAttribute(60);

      assertStatus(Status.OK, "Attribute shows 60");
      assertEquals("60", edit.getEditorValue());
   }

   @Test
   public void testPendingSurvivesAChangeToAnotherParam() {
      edit.onEdit("60");
      edit.beginSave();
      edit.onSetResponse("PENDING");

      changeAttribute(50);

      assertEquals(Status.PENDING, edit.getStatus());
      assertEquals("60", edit.getEditorValue());
   }

   @Test
   public void testAttributeChangeBeforeTheResponse() {
      edit.onEdit("60");
      edit.beginSave();
      changeAttribute(60);

      edit.onSetResponse("PENDING");

      assertStatus(Status.OK, "Attribute shows 60");
      assertEquals("60", edit.getEditorValue());
   }

   @Test
   public void testDeviceReportsSomethingElseAfterTheDriverAcceptedIt() {
      edit.onEdit("60");
      edit.beginSave();
      edit.onSetResponse("PENDING");
      changeAttribute(60);

      changeAttribute(50);

      assertStatus(Status.WARNING, "Attribute changed to 50 after requesting 60");
      assertEquals("50", edit.getEditorValue());
   }

   @Test
   public void testAttributeGoesStraightToAnotherValue() {
      edit.onEdit("60");
      edit.beginSave();
      edit.onSetResponse("PENDING");

      changeAttribute(70);

      assertStatus(Status.WARNING, "Attribute changed to 70 after requesting 60");
      assertEquals("70", edit.getEditorValue());
   }

   @Test
   public void testOkStaysVisibleWhenTheAttributeCatchesUp() {
      DeviceSettingEdit driverOnly = new DeviceSettingEdit(param(driverOnlyEntry("volume", 0, 10, 5, 7)));
      driverOnly.onEdit("8");
      driverOnly.beginSave();

      driverOnly.onSetResponse("OK");

      assertEquals(Status.OK, driverOnly.getStatus());
      assertEquals("OK", driverOnly.getStatusText());
      assertEquals("8", driverOnly.getEditorValue());

      driverOnly.onParamChanged(param(driverOnlyEntry("volume", 0, 10, 5, 8)));

      assertEquals("OK", driverOnly.getStatusText());
      assertEquals("8", driverOnly.getEditorValue());
   }

   @Test
   public void testInvalidValueIsAnErrorAndKeepsTheEdit() {
      edit.onEdit("60");
      edit.beginSave();

      edit.onSetResponse("INVALID_VALUE");

      assertStatus(Status.ERROR, "INVALID_VALUE: the driver rejected 60");
      assertTrue(edit.hasUnsavedEdit());
      assertTrue(edit.canSave());
      assertTrue(edit.canEdit());
   }

   @Test
   public void testInvalidParamIsAnError() {
      edit.onEdit("60");
      edit.beginSave();

      edit.onSetResponse("INVALID_PARAM");

      assertStatus(Status.ERROR, "INVALID_PARAM: the driver does not have this setting");
   }

   @Test
   public void testUnexpectedStatusIsAnError() {
      edit.onEdit("60");
      edit.beginSave();

      edit.onSetResponse(null);

      assertStatus(Status.ERROR, "Unexpected status: null");
      assertTrue(edit.canSave());
   }

   @Test
   public void testFailureKeepsTheEdit() {
      edit.onEdit("60");
      edit.beginSave();

      edit.onSetFailed("Request timed out");

      assertStatus(Status.ERROR, "Failed: Request timed out");
      assertTrue(edit.canSave());
   }

   @Test
   public void testResetSendsTheDefaultAndDropsTheEdit() {
      edit.onEdit("60");

      assertEquals("10", edit.beginReset());

      assertFalse(edit.hasUnsavedEdit());
      assertEquals("10", edit.getEditorValue());

      edit.onSetResponse("PENDING");
      changeAttribute(10);

      assertStatus(Status.OK, "Attribute shows 10");
   }

   @Test
   public void testRejectedResetLeavesTheCurrentValue() {
      edit.beginReset();

      edit.onSetResponse("INVALID_VALUE");

      assertEquals(Status.ERROR, edit.getStatus());
      assertEquals("50", edit.getEditorValue());
   }

   @Test
   public void testResetAtTheDefaultDoesNotWaitForever() {
      DeviceSettingEdit atDefault = new DeviceSettingEdit(param(rangeEntry("level", 0, 99, 10, 10)));
      atDefault.beginReset();

      atDefault.onSetResponse("PENDING");

      assertEquals(Status.OK, atDefault.getStatus());
   }

   @Test
   public void testResetUnavailableWithoutAValidDefault() {
      assertFalse(new DeviceSettingEdit(param(rangeEntry("level", 1, 99, 0, 5))).canReset());
      assertFalse(new DeviceSettingEdit(param(enumEntry("ledMode", 7, 1))).canReset());

      Map<String, Object> noDefault = rangeEntry("level", 0, 99, 0, 5);
      noDefault.remove("dflt");
      assertFalse(new DeviceSettingEdit(param(noDefault)).canReset());
   }

   @Test
   public void testEditIsDroppedWhenTheAttributeArrivesAtIt() {
      edit.onEdit("60");

      changeAttribute(60);

      assertFalse(edit.hasUnsavedEdit());
   }

   @Test
   public void testAttributeChangeKeepsAnUnrelatedEdit() {
      edit.onEdit("60");

      changeAttribute(70);

      assertTrue(edit.canSave());
      assertEquals("70", edit.getParam().getValue());
   }

   @Test
   public void testAttributeValueOutOfRangeIsAcceptedAndCanBeOverwritten() {
      Map<String, Object> signed = rangeEntry("offset", -3, 3, 0, -1);
      DeviceSettingEdit offset = new DeviceSettingEdit(param(signed));
      offset.onEdit("-2");
      offset.beginSave();
      offset.onSetResponse("PENDING");
      offset.onParamChanged(withValue(offset.getParam(), signed, -2));

      // one byte of -2 read back as unsigned
      offset.onParamChanged(withValue(offset.getParam(), signed, 254));

      assertEquals(Status.WARNING, offset.getStatus());
      assertEquals("254", offset.getEditorValue());

      offset.onEdit("1");
      assertTrue(offset.canSave());
   }

   @Test
   public void testReadPendingShowsTheLastKnownValue() {
      edit.beginRead();
      assertFalse(edit.canRead());

      edit.onReadResponse("PENDING", "50");

      assertStatus(Status.PENDING, "PENDING, last known 50");
      assertTrue(edit.canRead());
   }

   @Test
   public void testReadPendingWithoutAValue() {
      edit.beginRead();

      edit.onReadResponse("PENDING", null);

      assertStatus(Status.PENDING, "PENDING");
   }

   @Test
   public void testReadValueIsNormalized() {
      edit.beginRead();

      edit.onReadResponse("PENDING", "50.0");

      assertStatus(Status.PENDING, "PENDING, last known 50");
   }

   @Test
   public void testReadEndsWhenTheAttributeChanges() {
      edit.beginRead();
      edit.onReadResponse("PENDING", "50");

      changeAttribute(55);

      assertStatus(Status.OK, "Attribute changed to 55");
   }

   @Test
   public void testReadOfDriverOnlySetting() {
      edit.beginRead();

      edit.onReadResponse("OK", "50");

      assertStatus(Status.OK, "OK: 50");
   }

   @Test
   public void testReadInvalidParam() {
      edit.beginRead();

      edit.onReadResponse("INVALID_PARAM", null);

      assertEquals(Status.ERROR, edit.getStatus());
   }

   @Test
   public void testReadFailed() {
      edit.beginRead();

      edit.onReadFailed("Request timed out");

      assertStatus(Status.ERROR, "Failed: Request timed out");
      assertTrue(edit.canRead());
   }

   @Test
   public void testPendingSetStillResolvesAfterARead() {
      edit.onEdit("60");
      edit.beginSave();
      edit.onSetResponse("PENDING");
      edit.beginRead();
      edit.onReadResponse("PENDING", "60");

      changeAttribute(60);

      assertStatus(Status.OK, "Attribute shows 60");
   }
}
