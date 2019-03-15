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
package com.iris.driver;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DeviceDisconnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.service.TestDriverModule;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 *
 */
@Mocks({ DeviceDAO.class, PersonDAO.class, HubDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PopulationDAO.class, PlacePopulationCacheManager.class })
@Modules({ InMemoryMessageModule.class, TestDriverModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class })
public class TestPlatformDeviceDriverContextEvents extends IrisMockTestCase {
	DeviceDriver nonpresDriver;
	DeviceDriver presDriver;
	ContextualEventHandler<DriverEvent> connectedEventHandler;
	ContextualEventHandler<DriverEvent> disconnectedEventHandler;
	PlatformDeviceDriverContext context;
	
	@Inject
	CapabilityRegistry registry;
	
	@Inject
	InMemoryPlatformMessageBus messages;
	
	@Inject
	DeviceDAO mockDeviceDao;	

	@Inject
	HubDAO mockHubDao;
	
	@Inject
	PlacePopulationCacheManager mockPopulationCacheMgr;
	
	AttributeKey<String> ATTR_DEVICE_PRESENCE =
	         AttributeKey.create(PresenceCapability.ATTR_PRESENCE, String.class);	
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		connectedEventHandler = EasyMock.createMock(ContextualEventHandler.class);
		disconnectedEventHandler = EasyMock.createMock(ContextualEventHandler.class);

		nonpresDriver = Drivers.builder().withName("Test").withMatcher(Predicates.alwaysTrue())
				.withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL))
				.addDriverEventHandler(DeviceConnectedEvent.class, connectedEventHandler)
				.addDriverEventHandler(DeviceDisconnectedEvent.class, disconnectedEventHandler)
				.addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
				.create(true);

		presDriver = Drivers.builder().withName("Test").withMatcher(Predicates.alwaysTrue())
				.withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL))
				.addDriverEventHandler(DeviceConnectedEvent.class, connectedEventHandler)
				.addDriverEventHandler(DeviceDisconnectedEvent.class, disconnectedEventHandler)
				.addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(DeviceCapability.NAMESPACE))
				.addCapabilityDefinition(registry.getCapabilityDefinitionByNamespace(PresenceCapability.NAMESPACE))
				.create(true);
	}

	@Override
	public void replay() {
		super.replay();
		EasyMock.replay(connectedEventHandler, disconnectedEventHandler);
	}

	@Override
	public void verify() {
		EasyMock.verify(connectedEventHandler, disconnectedEventHandler);
		super.verify();
	}

	@Test
	public void testNonPresenceDeviceOnlineEvent() throws Exception {
		// create online non presense device
		Device device = Fixtures.createDevice();
		context = new PlatformDeviceDriverContext(device, nonpresDriver, mockPopulationCacheMgr);
		
		// state change
		context.setAttributeValue(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));
		DriverEvent connected = DriverEvent.createConnected(0);
		
		// setup expected states
		EasyMock.expect(connectedEventHandler.handleEvent(context, connected)).andReturn(true).once();
		expectOnlineDriverState(device);
		
		replay();
		
		nonpresDriver.handleDriverEvent(connected, context);
		verify();
	}
	
	@Test
	public void testNonPresenceDeviceOfflineEvent() throws Exception {
		Device device = Fixtures.createDevice();
		context = new PlatformDeviceDriverContext(device, nonpresDriver, mockPopulationCacheMgr);
		context.setAttributeValue(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_OFFLINE));
		DriverEvent disconnected = DriverEvent.createDisconnected(0);
		EasyMock.expect(disconnectedEventHandler.handleEvent(context, disconnected)).andReturn(true).once();
		expectOfflineDriverState(device);
		replay();
		nonpresDriver.handleDriverEvent(disconnected, context);
		verify();
	}	

	@Test
	public void testPresenceDeviceOnlineAndPresentEvent() throws Exception {
		// create online and PRESENT presence device
		Device device = Fixtures.createDevice(DeviceCapability.NAMESPACE, PresenceCapability.NAMESPACE);
        
      // PRESENCE DEVICES ARE NEVER OFFLINE
      // Hence I am testing a state change from ABSENT to PRESENT
      AttributeMap onl = AttributeMap.mapOf(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));
		onl.set(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
		DeviceDriverStateHolder presenceDeviceOnlineState = new DeviceDriverStateHolder(onl);
		
      // create a context while online
      context = new PlatformDeviceDriverContext(device, presDriver.getDefinition(), presenceDeviceOnlineState, mockPopulationCacheMgr);
        
        // state change
		context.setAttributeValue(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
		
		
		DriverEvent connected = DriverEvent.createConnected(0);
		EasyMock.expect(connectedEventHandler.handleEvent(context, connected)).andReturn(true).once();
		expectOnlineAndPresentDriverState(device);
		replay();
		
		// fire connected event and test driver updates with dirty changes
		presDriver.handleDriverEvent(connected, context);
		verify();
	}
	
	@Test
	public void testPresenceDeviceOnlineAndAbsentEvent() throws Exception {
		// create online and ABSENT presence device
		Device device = Fixtures.createDevice(DeviceCapability.NAMESPACE, PresenceCapability.NAMESPACE);
        
		// Online and Present should reflect an online status
		AttributeMap onl = AttributeMap.mapOf(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));
		onl.set(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
		DeviceDriverStateHolder presenceDeviceOnlineState = new DeviceDriverStateHolder(onl);
		
      // create a context while online
      context = new PlatformDeviceDriverContext(device, presDriver.getDefinition(), presenceDeviceOnlineState, mockPopulationCacheMgr);
        
        // switch the context to offline, should set dirty state causing DAO Update when run
		context.setAttributeValue(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
		
		DriverEvent disconnected = DriverEvent.createDisconnected(0);
		
		// setup expectations
		EasyMock.expect(disconnectedEventHandler.handleEvent(context, disconnected)).andReturn(true).once();
		
		// ONLINE should still be true
        // even after disconnected event, hence no change on DAO regarding devconn:state
		expectOnlineAndAbsentDriverState(device);
		
		replay();
		
		// fire disconnected event
		presDriver.handleDriverEvent(disconnected, context);
		verify();
	}	
	
	private void expectOfflineDriverState(Device device) {
		AttributeMap attributes = AttributeMap.mapOf(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_OFFLINE));
		DeviceDriverStateHolder state = new DeviceDriverStateHolder(attributes);
		mockDeviceDao.updateDriverState(device, state);
		EasyMock.expectLastCall();
	}	

	private void expectOnlineDriverState(Device device) {
		AttributeMap attributes = AttributeMap.mapOf(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE));
		DeviceDriverStateHolder state = new DeviceDriverStateHolder(attributes);
		mockDeviceDao.updateDriverState(device, state);
		EasyMock.expectLastCall();
	}
	
	private void expectOnlineAndAbsentDriverState(Device device) {
		// Online and Absent should reflect an online status
		// hence ABSENT changes but not ONLINE (thus I don't test for that update at the dao)
		AttributeMap absent = AttributeMap.newMap();
		absent.set(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_ABSENT);
		DeviceDriverStateHolder state  = new DeviceDriverStateHolder(absent);
		mockDeviceDao.updateDriverState(device, state);
		EasyMock.expectLastCall();
	}	
	
	private void expectOnlineAndPresentDriverState(Device device) {
		AttributeMap present = AttributeMap.newMap();
		present.set(ATTR_DEVICE_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
		DeviceDriverStateHolder state = new DeviceDriverStateHolder(present);
		mockDeviceDao.updateDriverState(device, state);
		EasyMock.expectLastCall();
	}		
}

