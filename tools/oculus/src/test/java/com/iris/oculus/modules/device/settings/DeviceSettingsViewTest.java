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

import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.attribute;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.booleanEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.driverOnlyEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.enumEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.rangeEntry;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;

import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.capability.Capability;
import com.iris.client.capability.DeviceSettings;
import com.iris.client.event.Futures;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.model.BaseModel;
import com.iris.client.model.DeviceModel;
import com.iris.client.model.Model;
import com.iris.client.model.proxy.ProxyModelFactory;

public class DeviceSettingsViewTest {
   private static final String ADDRESS = "DRIV:dev:00000000-0000-0000-0000-000000000000";
   private static final String OTHER_ADDRESS = "DRIV:dev:00000000-0000-0000-0000-000000000001";

   private final Capture<ClientRequest> requests = Capture.newInstance(CaptureType.ALL);
   private final Deque<SettableClientFuture<ClientEvent>> responses = new ArrayDeque<>();
   private IrisClient client;
   private DeviceSettingsView view;

   @Before
   public void setUp() {
      client = createMock(IrisClient.class);
      expect(client.request(capture(requests))).andAnswer(() -> {
         SettableClientFuture<ClientEvent> response = Futures.settableFuture();
         responses.add(response);
         return response;
      }).anyTimes();
      replay(client);
      view = new DeviceSettingsView();
   }

   private Model model(String address, Object params) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(Capability.ATTR_ADDRESS, address);
      if(params != null) {
         attributes.put(DeviceSettings.ATTR_PARAMS, params);
      }
      return new BaseModel(attributes, client) { };
   }

   private Model bind(Object params) {
      Model model = model(ADDRESS, params);
      view.setModel(model);
      return model;
   }

   private static void changeAttribute(Model model, Object params) {
      model.updateAttributes(Collections.singletonMap(DeviceSettings.ATTR_PARAMS, params));
   }

   private ClientRequest lastRequest() {
      return requests.getValues().get(requests.getValues().size() - 1);
   }

   private void respond(String type, String status) {
      respond(type, Collections.<String, Object>singletonMap("status", status));
   }

   private void respond(String type, Map<String, Object> attributes) {
      responses.remove().setValue(new ClientEvent(type, ADDRESS, attributes));
   }

   private <C extends Component> List<C> find(Class<C> type) {
      List<C> found = new ArrayList<>();
      collect(view.getComponent(), type, found);
      return found;
   }

   private static <C extends Component> void collect(Container parent, Class<C> type, List<C> found) {
      for(Component child: parent.getComponents()) {
         if(type.isInstance(child)) {
            found.add(type.cast(child));
         }
         if(child instanceof Container) {
            collect((Container) child, type, found);
         }
      }
   }

   private JButton button(String text, int row) {
      int matched = 0;
      for(JButton button: find(JButton.class)) {
         if(text.equals(button.getText()) && matched++ == row) {
            return button;
         }
      }
      throw new AssertionError("No button " + text + " in row " + row);
   }

   private boolean showsText(String text) {
      return find(JLabel.class).stream().anyMatch((label) -> text.equals(label.getText().replaceAll("<[^>]+>", "")));
   }

   private JFormattedTextField spinnerText(int index) {
      return ((JSpinner.DefaultEditor) find(JSpinner.class).get(index).getEditor()).getTextField();
   }

   private static void type(JFormattedTextField field, String text) throws Exception {
      onEventThread(() -> field.setText(text));
   }

   /** Runs on the event thread, as user input does, and lets the range editor's check that follows finish. */
   private static void onEventThread(Runnable input) throws Exception {
      SwingUtilities.invokeAndWait(input);
      SwingUtilities.invokeAndWait(() -> { });
   }

   private void assertSent(String command, String paramId, String value) {
      ClientRequest request = lastRequest();
      assertEquals(command, request.getCommand());
      assertEquals(ADDRESS, request.getAddress());
      assertEquals(paramId, request.getAttributes().get("paramId"));
      assertEquals(value, request.getAttributes().get("value"));
   }

   @Test
   public void testListsNameDescriptionCurrentAndDefault() {
      bind(attribute(enumEntry("ledMode", 0, 1)));

      assertTrue(showsText("Led Mode"));
      assertTrue(showsText("Description of ledMode"));
      assertTrue(showsText("Always On (1)"));
      assertTrue(showsText("Always Off (0)"));
      assertTrue(showsText("ledMode, Z-Wave parameter 2, size 1"));
   }

   @Test
   public void testEmptyListShowsAMessageWithoutARequest() {
      bind(Collections.emptyList());

      assertTrue(showsText("This device has not reported any settings."));
      assertTrue(requests.getValues().isEmpty());
      assertTrue(view.actionRefresh().isEnabled());
   }

   @Test
   public void testMissingAttributeIsFetchedOnce() {
      bind(null);

      assertTrue(showsText("Loading settings..."));
      assertEquals(1, requests.getValues().size());
      assertEquals(Capability.CMD_GET_ATTRIBUTES, lastRequest().getCommand());
   }

   @Test
   public void testMissingAttributeArrivesFromGetAttributes() {
      bind(null);

      respond(Capability.CMD_GET_ATTRIBUTES + "Response", Collections.<String, Object>singletonMap(
            DeviceSettings.ATTR_PARAMS, attribute(enumEntry("ledMode", 0, 1))
      ));

      assertTrue(showsText("Led Mode"));
      assertTrue(view.actionRefresh().isEnabled());
   }

   @Test
   public void testAttributeStillMissingAfterGetAttributes() {
      bind(null);

      respond(Capability.CMD_GET_ATTRIBUTES + "Response", Collections.<String, Object>emptyMap());

      assertTrue(showsText("This device has not reported any settings."));
   }

   @Test
   public void testRefreshFailureIsShown() {
      bind(attribute(enumEntry("ledMode", 0, 1)));

      view.actionRefresh().actionPerformed(null);
      assertFalse(view.actionRefresh().isEnabled());

      responses.remove().setError(new IllegalStateException("Client is not connected"));

      assertTrue(showsText("Unable to refresh: Client is not connected"));
      assertTrue(view.actionRefresh().isEnabled());
   }

   @Test
   public void testSaveSendsTheEnumKey() {
      bind(attribute(enumEntry("ledMode", 0, 1)));

      assertFalse(button("Save", 0).isEnabled());
      find(JComboBox.class).get(0).setSelectedIndex(2);
      assertTrue(button("Save", 0).isEnabled());

      button("Save", 0).doClick();

      assertSent(DeviceSettings.CMD_SETPARAM, "ledMode", "2");
      assertFalse(button("Save", 0).isEnabled());
   }

   @Test
   public void testPendingThenAttributeChange() {
      Model model = bind(attribute(enumEntry("ledMode", 0, 1)));
      find(JComboBox.class).get(0).setSelectedIndex(2);
      button("Save", 0).doClick();

      respond(DeviceSettings.SetParamResponse.NAME, "PENDING");

      assertTrue(showsText("PENDING, waiting for the attribute to change"));
      assertEquals("Follows Switch", find(JComboBox.class).get(0).getSelectedItem().toString());
      assertTrue(showsText("Always On (1)"));

      changeAttribute(model, attribute(enumEntry("ledMode", 0, 2)));

      assertTrue(showsText("Sent Follows Switch (2), not confirmed by the device"));
      assertTrue(showsText("Follows Switch (2)"));
   }

   @Test
   public void testInvalidValueIsShownAsAnError() throws Exception {
      bind(attribute(rangeEntry("level", 0, 99, 0, 5)));
      type(spinnerText(0), "42");
      button("Save", 0).doClick();

      respond(DeviceSettings.SetParamResponse.NAME, "INVALID_VALUE");

      assertTrue(showsText("INVALID_VALUE: the driver rejected 42"));
      assertEquals("42", spinnerText(0).getText());
      assertTrue(button("Save", 0).isEnabled());
   }

   @Test
   public void testReformattingTheTextLeavesSaveAlone() throws Exception {
      bind(attribute(rangeEntry("level", 0, 99, 0, 5)));
      type(spinnerText(0), "42");
      List<Object> changes = new ArrayList<>();
      button("Save", 0).addPropertyChangeListener("enabled", (event) -> changes.add(event.getNewValue()));

      // what the field does on every focus change, a disabled button drops the click that moved focus
      JFormattedTextField field = spinnerText(0);
      onEventThread(() -> field.setValue(Long.valueOf(42)));

      assertTrue(changes.isEmpty());
      assertTrue(button("Save", 0).isEnabled());
   }

   @Test
   public void testRequestFailureIsShown() throws Exception {
      bind(attribute(rangeEntry("level", 0, 99, 0, 5)));
      type(spinnerText(0), "42");
      button("Save", 0).doClick();

      responses.remove().setError(new IllegalStateException("Request timed out"));

      assertTrue(showsText("Failed: Request timed out"));
      assertTrue(button("Save", 0).isEnabled());
   }

   @Test
   public void testDriverOnlySetting() throws Exception {
      Model model = bind(attribute(driverOnlyEntry("volume", 0, 10, 5, 7)));
      assertTrue(showsText("volume, driver setting, 0 to 10"));
      type(spinnerText(0), "8");
      button("Save", 0).doClick();

      respond(DeviceSettings.SetParamResponse.NAME, "OK");

      assertTrue(showsText("OK"));

      changeAttribute(model, attribute(driverOnlyEntry("volume", 0, 10, 5, 8)));

      assertTrue(showsText("OK"));
      assertEquals("8", spinnerText(0).getText());
   }

   @Test
   public void testResetSendsTheDefaultAsAWholeNumber() {
      bind(attribute(rangeEntry("level", 0, 99, 10, 50)));

      button("Reset", 0).doClick();

      assertSent(DeviceSettings.CMD_SETPARAM, "level", "10");
      assertEquals("10", spinnerText(0).getText());
   }

   @Test
   public void testResetDisabledWithoutAValidDefault() {
      bind(attribute(rangeEntry("level", 1, 99, 0, 50)));

      assertFalse(button("Reset", 0).isEnabled());
   }

   @Test
   public void testReadSendsGetParam() {
      bind(attribute(rangeEntry("level", 0, 99, 10, 50)));

      button("Read", 0).doClick();

      assertSent(DeviceSettings.CMD_GETPARAM, "level", null);
      assertFalse(button("Read", 0).isEnabled());

      Map<String, Object> response = new HashMap<>();
      response.put("status", "PENDING");
      response.put("value", "50");
      respond(DeviceSettings.GetParamResponse.NAME, response);

      assertTrue(showsText("PENDING, last known 50"));
      assertTrue(button("Read", 0).isEnabled());
   }

   @Test
   public void testBooleanIsSentAsOneOrZero() {
      bind(attribute(booleanEntry("enabled", 0, 0)));

      find(JCheckBox.class).get(0).setSelected(true);
      button("Save", 0).doClick();

      assertSent(DeviceSettings.CMD_SETPARAM, "enabled", "1");
   }

   @Test
   public void testAttributeChangeKeepsAnEditInAnotherRow() throws Exception {
      Model model = bind(attribute(rangeEntry("first", 0, 99, 0, 5), rangeEntry("second", 0, 99, 0, 6)));
      Component firstRowEditor = find(JSpinner.class).get(0);
      type(spinnerText(0), "42");

      changeAttribute(model, attribute(rangeEntry("first", 0, 99, 0, 5), rangeEntry("second", 0, 99, 0, 7)));

      assertTrue("rows must be updated in place", firstRowEditor == find(JSpinner.class).get(0));
      assertEquals("42", spinnerText(0).getText());
      assertEquals("7", spinnerText(1).getText());
      assertTrue(button("Save", 0).isEnabled());
      assertFalse(button("Save", 1).isEnabled());
   }

   @Test
   public void testAttributeChangeKeepsInvalidTextBeingTyped() throws Exception {
      Model model = bind(attribute(rangeEntry("first", 0, 99, 0, 5), rangeEntry("second", 0, 99, 0, 6)));
      type(spinnerText(0), "999");

      changeAttribute(model, attribute(rangeEntry("first", 0, 99, 0, 5), rangeEntry("second", 0, 99, 0, 7)));

      assertEquals("999", spinnerText(0).getText());
      assertTrue(showsText("Must be a whole number from 0 to 99"));
      assertFalse(button("Save", 0).isEnabled());
   }

   @Test
   public void testAttributeChangeOnAnUntouchedRowUpdatesItsEditor() {
      Model model = bind(attribute(enumEntry("ledMode", 0, 1)));

      changeAttribute(model, attribute(enumEntry("ledMode", 0, 2)));

      assertEquals("Follows Switch", find(JComboBox.class).get(0).getSelectedItem().toString());
      assertFalse(button("Save", 0).isEnabled());
   }

   @Test
   public void testAttributeClearedByTheDriverEmptiesTheList() {
      Model model = bind(attribute(enumEntry("ledMode", 0, 1)));

      changeAttribute(model, Collections.emptyList());

      assertTrue(find(JComboBox.class).isEmpty());
      assertTrue(showsText("This device has not reported any settings."));
   }

   @Test
   public void testChangedDefinitionRebuildsTheRows() {
      Model model = bind(attribute(enumEntry("ledMode", 0, 1)));

      changeAttribute(model, attribute(enumEntry("ledMode", 0, 1), rangeEntry("level", 0, 99, 0, 5)));

      assertEquals(1, find(JSpinner.class).size());
      assertTrue(showsText("Level"));
   }

   @Test
   public void testCurrentValueOutOfRangeRenders() {
      bind(attribute(rangeEntry("offset", -3, 3, 0, 255)));

      assertTrue(showsText("255"));
      assertEquals("255", spinnerText(0).getText());
      assertFalse(button("Save", 0).isEnabled());
   }

   @Test
   public void testUnchangedModelKeepsEdits() throws Exception {
      Model model = bind(attribute(rangeEntry("level", 0, 99, 0, 5)));
      type(spinnerText(0), "42");

      view.setModel(model);

      assertEquals("42", spinnerText(0).getText());
   }

   @Test
   public void testSwitchingModelsStopsFollowingTheOldOne() {
      Model first = bind(attribute(enumEntry("first", 0, 1)));
      Model second = model(OTHER_ADDRESS, attribute(enumEntry("second", 0, 1)));

      view.setModel(second);
      changeAttribute(first, attribute(enumEntry("first", 0, 2)));

      assertTrue(showsText("Second"));
      assertFalse(showsText("First"));
   }

   @Test
   public void testEventAlreadyQueuedFromTheOldModelIsIgnored() {
      List<Listener<PropertyChangeEvent>> listeners = new ArrayList<>();
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(Capability.ATTR_ADDRESS, ADDRESS);
      attributes.put(DeviceSettings.ATTR_PARAMS, attribute(enumEntry("first", 0, 1)));
      Model first = new BaseModel(attributes, client) {
         @Override
         public ListenerRegistration addListener(Listener<PropertyChangeEvent> listener) {
            listeners.add(listener);
            return super.addListener(listener);
         }
      };
      view.setModel(first);
      view.setModel(model(OTHER_ADDRESS, attribute(enumEntry("second", 0, 1))));

      // events are copied to the event thread with their listeners, so removal can't stop one in the queue
      listeners.get(0).onEvent(new PropertyChangeEvent(
            first, DeviceSettings.ATTR_PARAMS, null, attribute(enumEntry("first", 0, 2))
      ));

      assertTrue(showsText("Second"));
      assertFalse(showsText("First"));
   }

   @Test
   public void testWorksWithTheClientModelProxy() {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(Capability.ATTR_ADDRESS, ADDRESS);
      attributes.put(DeviceSettings.ATTR_PARAMS, attribute(enumEntry("ledMode", 0, 1)));
      Model model = new ProxyModelFactory(client).create(
            attributes, DeviceModel.class, Collections.<Class<? extends Capability>>singletonList(DeviceSettings.class)
      );
      view.setModel(model);

      find(JComboBox.class).get(0).setSelectedIndex(2);
      button("Save", 0).doClick();
      assertSent(DeviceSettings.CMD_SETPARAM, "ledMode", "2");
      respond(DeviceSettings.SetParamResponse.NAME, "PENDING");

      changeAttribute(model, attribute(enumEntry("ledMode", 0, 2)));

      assertTrue(showsText("Sent Follows Switch (2), not confirmed by the device"));

      view.actionRefresh().actionPerformed(null);
      assertEquals(Capability.CMD_GET_ATTRIBUTES, lastRequest().getCommand());
   }

   @Test
   public void testAnsweredRefreshOfTheOldModelIsIgnored() {
      bind(null);
      Model second = model(OTHER_ADDRESS, attribute(enumEntry("second", 0, 1)));
      view.setModel(second);

      respond(Capability.CMD_GET_ATTRIBUTES + "Response", Collections.<String, Object>singletonMap(
            DeviceSettings.ATTR_PARAMS, attribute(enumEntry("first", 0, 1))
      ));

      assertTrue(showsText("Second"));
      assertFalse(showsText("First"));
   }

   @Test
   public void testClearModelStopsFollowing() {
      Model model = bind(attribute(enumEntry("ledMode", 0, 1)));

      view.clearModel();
      changeAttribute(model, attribute(enumEntry("ledMode", 0, 2)));

      assertTrue(find(JComboBox.class).isEmpty());
      assertFalse(view.actionRefresh().isEnabled());
   }

   @Test
   public void testResponseAfterCloseDoesNotFail() throws Exception {
      bind(attribute(rangeEntry("level", 0, 99, 0, 5)));
      type(spinnerText(0), "42");
      button("Save", 0).doClick();

      view.clearModel();

      respond(DeviceSettings.SetParamResponse.NAME, "PENDING");
      assertTrue(find(JSpinner.class).isEmpty());
   }
}
