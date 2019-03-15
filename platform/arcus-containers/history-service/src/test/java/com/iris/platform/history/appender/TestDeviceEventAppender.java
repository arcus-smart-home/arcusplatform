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
package com.iris.platform.history.appender;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Fixtures;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;

public class TestDeviceEventAppender extends EventAppenderTestCase {
	private DeviceEventAppender appender;
	
	@Before
	public void setup() {
		appender = new DeviceEventAppender(mockAppenderDao, mockNameCache);
	}

	@Test
   public void testMatchAdded() {
      PlatformMessage message = Fixtures.createAddedMessage(
            Fixtures.createDeviceAddress(),
            ImmutableMap.of(DeviceCapability.ATTR_NAME, DEVICE_NAME)
      );
      
      assertTrue(appender.matches(message).isMatch());
	}
	
	@Test
   public void testMatchRemoved() {
      PlatformMessage message = Fixtures.createDeletedMessage(
            Fixtures.createDeviceAddress(),
            ImmutableMap.of(DeviceCapability.ATTR_NAME, DEVICE_NAME)
      );
      
      assertTrue(appender.matches(message).isMatch());
   }
	
	@Test
   public void testWrongAddress() {
      PlatformMessage message = Fixtures.createAddedMessage(
            Fixtures.createObjectAddress(PlaceCapability.NAMESPACE),
            ImmutableMap.of(DeviceCapability.ATTR_NAME, DEVICE_NAME)
      );
      
      assertFalse(appender.matches(message).isMatch());
   }

   @Test
   public void testWrongMessageType() {
      PlatformMessage message = Fixtures.createValueChangeMessage(
            Fixtures.createDeviceAddress(),
            ImmutableMap.of(DeviceCapability.ATTR_NAME, DEVICE_NAME)
      );
      
      assertFalse(appender.matches(message).isMatch());
   }
   
   @Test
   public void testRemovedAppend() {
   	PlatformMessage message = Fixtures.newDeletedMessageBuilder(
   			PLACE_ID,
   			Address.platformDriverAddress(DEVICE_ID),
            ImmutableMap.of(DeviceCapability.ATTR_NAME, DEVICE_NAME)
      )
      .create();
   	expectFindPlaceName();
   	expectFindDeviceName();
   	Capture<HistoryLogEntry> event1 = expectAndCaptureAppend();
   	Capture<HistoryLogEntry> event2 = expectAndCaptureAppend();
   	Capture<HistoryLogEntry> event3 = expectAndCaptureAppend();
   	EasyMock.replay(mockAppenderDao, mockNameCache);
   	
   	Assert.assertTrue(appender.append(message));
   	{
   		HistoryLogEntry event = event1.getValue();
   		assertEquals(HistoryLogEntryType.CRITICAL_PLACE_LOG, event.getType());
         assertEquals(PLACE_ID, event.getId());
         assertEquals("device.removed", event.getMessageKey());
         assertEquals(DEVICE_NAME, event.getValues().get(0));
         assertEquals(PLACE_NAME, event.getValues().get(1));
   	}
		{
		    HistoryLogEntry event = event2.getValue();
		    assertEquals(HistoryLogEntryType.DETAILED_PLACE_LOG, event.getType());
		    assertEquals(PLACE_ID, event.getId());
		    assertEquals("device.removed", event.getMessageKey());
		    assertEquals(DEVICE_NAME, event.getValues().get(0));
		    assertEquals(PLACE_NAME, event.getValues().get(1));
		 }
		 {
		    HistoryLogEntry event = event3.getValue();
		    assertEquals(HistoryLogEntryType.DETAILED_DEVICE_LOG, event.getType());
		    assertEquals(deviceAddress.getId(), event.getId());
		    assertEquals("device.removed", event.getMessageKey());
		    assertEquals(DEVICE_NAME, event.getValues().get(0));
		    assertEquals(PLACE_NAME, event.getValues().get(1));
		 }
   }
   
   @Test
   public void testAddedAppend() {
   	PlatformMessage message = Fixtures.newAddedMessageBuilder(
   			PLACE_ID,
   			Address.platformDriverAddress(DEVICE_ID),
            ImmutableMap.of(DeviceCapability.ATTR_NAME, DEVICE_NAME)
      )
      .create();
   	expectFindPlaceName();
   	expectFindDeviceName();
   	Capture<HistoryLogEntry> event1 = expectAndCaptureAppend();
   	Capture<HistoryLogEntry> event2 = expectAndCaptureAppend();
   	Capture<HistoryLogEntry> event3 = expectAndCaptureAppend();
   	EasyMock.replay(mockAppenderDao, mockNameCache);
   	
   	Assert.assertTrue(appender.append(message));
   	{
   		HistoryLogEntry event = event1.getValue();
   		assertEquals(HistoryLogEntryType.CRITICAL_PLACE_LOG, event.getType());
         assertEquals(PLACE_ID, event.getId());
         assertEquals("device.added", event.getMessageKey());
         assertEquals(DEVICE_NAME, event.getValues().get(0));
         assertEquals(PLACE_NAME, event.getValues().get(1));
   	}
		{
		    HistoryLogEntry event = event2.getValue();
		    assertEquals(HistoryLogEntryType.DETAILED_PLACE_LOG, event.getType());
		    assertEquals(PLACE_ID, event.getId());
		    assertEquals("device.added", event.getMessageKey());
		    assertEquals(DEVICE_NAME, event.getValues().get(0));
		    assertEquals(PLACE_NAME, event.getValues().get(1));
		 }
		 {
		    HistoryLogEntry event = event3.getValue();
		    assertEquals(HistoryLogEntryType.DETAILED_DEVICE_LOG, event.getType());
		    assertEquals(deviceAddress.getId(), event.getId());
		    assertEquals("device.added", event.getMessageKey());
		    assertEquals(DEVICE_NAME, event.getValues().get(0));
		    assertEquals(PLACE_NAME, event.getValues().get(1));
		 }
   }
}

