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
package com.iris.common.subsystem.alarm.generic;

import java.util.Map;

import org.junit.Test;

import com.iris.common.subsystem.alarm.PlatformAlarmSubsystemTestCase;
import com.iris.common.subsystem.alarm.co.CarbonMonoxideAlarm;
import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.water.WaterAlarm;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestMonitored extends PlatformAlarmSubsystemTestCase {

	protected Model addPlace(ServiceLevel serviceLevel) {
		Map<String, Object> attributes =
				ModelFixtures
					.buildServiceAttributes(context.getPlaceId(), PlaceCapability.NAMESPACE)
					.put(PlaceCapability.ATTR_SERVICELEVEL, serviceLevel)
					.create();
		return addModel(attributes);
	}
	
	@Test
	public void testDowngradeFromPromon() throws Exception {
		Model place = addPlace(ServiceLevel.PREMIUM_PROMON);
		init(subsystem);
		
		assertEquals(true, AlarmModel.getMonitored(CarbonMonoxideAlarm.NAME, context.model()));
		assertEquals(true, AlarmModel.getMonitored(PanicAlarm.NAME, context.model()));
		assertEquals(true, AlarmModel.getMonitored(SecurityAlarm.NAME, context.model()));
		assertEquals(true, AlarmModel.getMonitored(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(WaterAlarm.NAME, context.model()));
		
		place.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, ServiceLevel.PREMIUM);
		commit();
		
		assertEquals(false, AlarmModel.getMonitored(CarbonMonoxideAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(PanicAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(SecurityAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(WaterAlarm.NAME, context.model()));
	}
	
	@Test
	public void testUpgradeToPromon() throws Exception {
		Model place = addPlace(ServiceLevel.BASIC);
		init(subsystem);
		
		assertEquals(false, AlarmModel.getMonitored(CarbonMonoxideAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(PanicAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(SecurityAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(WaterAlarm.NAME, context.model()));
		
		place.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, ServiceLevel.PREMIUM_PROMON);
		commit();
		
		assertEquals(true, AlarmModel.getMonitored(CarbonMonoxideAlarm.NAME, context.model()));
		assertEquals(true, AlarmModel.getMonitored(PanicAlarm.NAME, context.model()));
		assertEquals(true, AlarmModel.getMonitored(SecurityAlarm.NAME, context.model()));
		assertEquals(true, AlarmModel.getMonitored(SmokeAlarm.NAME, context.model()));
		assertEquals(false, AlarmModel.getMonitored(WaterAlarm.NAME, context.model()));
	}
	
}

