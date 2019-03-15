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

import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.model.Fixtures;
import com.iris.platform.history.ActivityEvent;
import com.iris.platform.history.HistoryActivityDAO;
import com.iris.platform.history.appender.subsys.CareSubsystemActivityAppender;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks(HistoryActivityDAO.class)
public class TestCareSubsystemActivityAppender extends IrisMockTestCase {
	@Inject HistoryActivityDAO mockHistoryDao;
	@Inject CareSubsystemActivityAppender appender;
	
	private PlatformMessage triggeredDevicesMsg;
	private UUID placeId = UUID.randomUUID();
	private Set<String> triggeredDevices = ImmutableSet.of( Fixtures.createDeviceAddress().getRepresentation() );
	
	@Override
   public void setUp() throws Exception {
	   super.setUp();
	   triggeredDevicesMsg = 
	   		Fixtures
	   			.createValueChangeMessage(
	   					placeId,
	   					Address.platformService(placeId, CareSubsystemCapability.NAMESPACE), 
	   					ImmutableMap.of(CareSubsystemCapability.ATTR_TRIGGEREDDEVICES, triggeredDevices)
	   			);
   }

	@Test
	public void testAppend() {
		ActivityEvent event = new ActivityEvent();
		event.setPlaceId(placeId);
		event.setTimestamp(triggeredDevicesMsg.getTimestamp());
		event.setActiveDevices(triggeredDevices);
		event.setInactiveDevices(null);
		
		mockHistoryDao.append(event);
		replay();

		assertTrue(appender.append(triggeredDevicesMsg));
		verify();
	}

}

