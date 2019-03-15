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
package com.iris.common.subsystem.alarm;

import com.iris.common.subsystem.alarm.panic.PanicAlarm;
import com.iris.common.subsystem.alarm.smoke.SmokeAlarm;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AlarmModel;
import com.iris.messages.model.test.ModelFixtures;

public class TestAlarmSubsystem_Sirens extends PlatformAlarmSubsystemTestCase {

	private Model smoke;
	private Model siren;
	
	@Before
	public void createSiren() {
		// enable the alarms
		addContactDevice();
		smoke = addSmokeDevice(SmokeCapability.SMOKE_SAFE);
		siren = addModel( ModelFixtures.createAlertAttributes() );
	}
	
	protected void assertAlertSent() {
		assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(AlertCapability.ATTR_STATE, AlertCapability.STATE_ALERTING));
	}
	
	protected void assertQuietSent() {
		assertContainsRequestMessageWithAttrs(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.<String, Object>of(AlertCapability.ATTR_STATE, AlertCapability.STATE_QUIET));
	}
	
	@Test
	public void testAlertWithSounds() throws Exception {
		expectAddAlert(PanicAlarm.NAME);
		expectUpdateIncident();
		start();
		
		panic();
		
		assertAlertSent();
	}
	
	@Test
	public void testAlertSilent() throws Exception {
		expectAddAlert(PanicAlarm.NAME);
		start();
		
		AlarmModel.setSilent(PanicAlarm.NAME, context.model(), true);
		panic();
		
		assertNoRequests();
	}
	
	@Test
	public void testSilentAlertThenAlertWithSounds() throws Exception {
		expectAddAlert(PanicAlarm.NAME);
		expectUpdateIncident();
		expectAddAlert(SmokeAlarm.NAME);
		expectUpdateIncident();
		start();
		
		AlarmModel.setSilent(PanicAlarm.NAME, context.model(), true);
		
		{
			panic();
			assertFalse(requests.hasCaptured());
		}

		{
			trigger(smoke);
			assertAlertSent();
		}
	}
	
	@Test
	public void testMultipleSilentAlerts() throws Exception {
		expectAddAlert(PanicAlarm.NAME);
		expectAddAlert(SmokeAlarm.NAME);
		start();
		
		AlarmModel.setSilent(PanicAlarm.NAME, context.model(), true);
		AlarmModel.setSilent(SmokeAlarm.NAME, context.model(), true);
		
		{
			panic();
			assertFalse(requests.hasCaptured());
		}

		{
			trigger(smoke);
			assertFalse(requests.hasCaptured());
		}
	}
	
	@Test
	public void testCancel() throws Exception {
		stageAlerting(PanicAlarm.NAME);
		expectCancelIncidentAndReturn(SettableFuture.<Void>create(), stageAlarmIncident(PanicAlarm.NAME));
		start();
		
		cancel();
		assertQuietSent();
	}

}

