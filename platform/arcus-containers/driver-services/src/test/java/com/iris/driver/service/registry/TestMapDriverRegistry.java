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
package com.iris.driver.service.registry;

import java.util.Map;

import javax.inject.Named;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.annotation.Version;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.bootstrap.guice.Injectors;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.dao.file.FileDAOModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.reflex.ReflexActionForward;
import com.iris.driver.reflex.ReflexDefinition;
import com.iris.driver.reflex.ReflexMatchLifecycle;
import com.iris.driver.service.TestDriverModule;
import com.iris.driver.service.matcher.DiscoveryAlgorithm;
import com.iris.driver.service.matcher.SortedDiscoveryAlgorithmFactory;
import com.iris.messages.ErrorEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.DriverId;
import com.iris.messages.type.Population;
import com.iris.protocol.ProtocolMessage;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 *
 */
@Mocks({ DeviceDAO.class, HubDAO.class, PlaceDAO.class, PopulationDAO.class })
@Modules({ TestDriverModule.class, TestMapDriverRegistry.TestModule.class, FileDAOModule.class })
public class TestMapDriverRegistry extends IrisMockTestCase {
	@Inject MapDriverRegistry registry;

	@Test
	public void testDriverMatchesFallback() {
		AttributeMap attributes = AttributeMap.emptyMap();
		DeviceDriver driver = registry.findDriverFor("general", attributes, 0);
		assertEquals(driver.getDriverId().getName(), "_FallbackDriver");
	}

	@Test
	public void testDriverMatchesOne() {
		AttributeMap attributes = AttributeMap.mapOf(
				MATCH_DRIVER2.valueOf(Boolean.TRUE)
		);
		assertEquals(new DriverId("Driver2", com.iris.model.Version.UNVERSIONED), registry.findDriverFor("general", attributes, 0).getDriverId());
	}

	@Test
	public void testDriverMatchesAll() {
		AttributeMap attributes = AttributeMap.mapOf(
				MATCH_DRIVER1V1.valueOf(Boolean.TRUE),
				MATCH_DRIVER1V2.valueOf(Boolean.TRUE),
				MATCH_DRIVER1V3.valueOf(Boolean.TRUE),
				MATCH_DRIVER1V4.valueOf(Boolean.TRUE),
				MATCH_DRIVER1V5.valueOf(Boolean.TRUE),
				MATCH_DRIVER2.valueOf(Boolean.TRUE)
		);
		assertEquals(new DriverId("Driver1", new com.iris.model.Version(2)), registry.findDriverFor("general", attributes, 0).getDriverId());
		assertEquals(new DriverId("Driver1", new com.iris.model.Version(4)), registry.findDriverFor("beta", attributes, 0).getDriverId());
		assertEquals(new DriverId("Driver1", new com.iris.model.Version(5)), registry.findDriverFor("beta", attributes, 1).getDriverId());

		assertEquals(DriverRegistry.FALLBACK_DRIVERID, registry.findDriverFor("alpha", attributes, 0).getDriverId());
		assertEquals(DriverRegistry.FALLBACK_DRIVERID, registry.findDriverFor("alpha", attributes, 1).getDriverId());
	}

	@Test
	public void testLoadDriverByName() {
		assertEquals(Driver1V2.class, registry.loadDriverByName("general", "Driver1", 0).getClass());
		assertEquals(Driver1V3.class, registry.loadDriverByName("general", "Driver1", 1).getClass());
		assertEquals(Driver1V4.class, registry.loadDriverByName("beta", "Driver1", 0).getClass());
		assertEquals(Driver1V5.class, registry.loadDriverByName("beta", "Driver1", 1).getClass());
		assertEquals(Driver2.class, registry.loadDriverByName("general", "Driver2", 0).getClass());
		assertNull(registry.loadDriverByName("general", "Driver0", 0));
		assertNull(registry.loadDriverByName("general", "Driver3", 0));
		assertNull(registry.loadDriverByName("alpha", "Driver1", 0));
	}

	@Test
	public void testLoadDriverById() {
		assertEquals(Driver1.class, registry.loadDriverById("Driver1", com.iris.model.Version.UNVERSIONED).getClass());
		assertEquals(Driver1V1.class, registry.loadDriverById("Driver1", new com.iris.model.Version(1)).getClass());
		assertEquals(Driver1V2.class, registry.loadDriverById("Driver1", new com.iris.model.Version(2)).getClass());
		assertEquals(Driver1V3.class, registry.loadDriverById("Driver1", new com.iris.model.Version(3)).getClass());
		assertEquals(Driver1V4.class, registry.loadDriverById("Driver1", new com.iris.model.Version(4)).getClass());
		assertEquals(Driver1V5.class, registry.loadDriverById("Driver1", new com.iris.model.Version(5)).getClass());
		assertEquals(Driver2.class,	registry.loadDriverById("Driver2", com.iris.model.Version.UNVERSIONED).getClass());

		assertNull(registry.loadDriverById("Driver1", new com.iris.model.Version(6)));
	}

	public static class TestModule extends AbstractIrisModule {

		@Override
		protected void configure() {
			bind(FallbackDriver.class);
			bind(Driver1.class);
			bind(Driver1V1.class);
			bind(Driver1V2.class);
			bind(Driver1V3.class);
			bind(Driver1V4.class);
			bind(Driver1V5.class);
			bind(Driver2.class);
			bind(PlatformMessageBus.class).to(InMemoryPlatformMessageBus.class);
			bind(ProtocolMessageBus.class).to(InMemoryProtocolMessageBus.class);
		}

		@Provides
		public DiscoveryAlgorithm discoveryAlgorithm(MapDriverRegistry registry) {
			return new SortedDiscoveryAlgorithmFactory().create(registry.listDrivers());
		}
	}

	public static abstract class DriverStub extends AbstractDriverStub {
		@Override
		protected DeviceDriverDefinition createDriverDefinition() {
			return DeviceDriverDefinition
				.builder()
				.withName(Injectors.getServiceName(this))
				.withVersion(Injectors.getServiceVersion(this, com.iris.model.Version.UNVERSIONED))
				.withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_QA, Population.NAME_BETA))
				.create();
		}
	}

	public static abstract class AbstractDriverStub implements DeviceDriver {
		private final DeviceDriverDefinition definition;

		protected AbstractDriverStub() {
			this.definition = createDriverDefinition();
		}

		protected abstract DeviceDriverDefinition createDriverDefinition();

		@Override
		public boolean supports(AttributeMap attributes) {
			return false;
		}

		@Override
		public void onRestored(DeviceDriverContext context) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onUpgraded(DriverEvent event, DriverId previous, DeviceDriverContext context) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSuspended(DeviceDriverContext context) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>,Object> attrs, Integer reflexVersion, boolean isDeviceMessage) {
		}

		@Override
		public void handleDriverEvent(DriverEvent event, DeviceDriverContext context) {

		}

		@Override
		public DeviceDriverDefinition getDefinition() {
			return definition;
		}

		@Override
		public void handleProtocolMessage(ProtocolMessage message,
				DeviceDriverContext context) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handlePlatformMessage(PlatformMessage message,
				DeviceDriverContext context) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleError(ErrorEvent error, DeviceDriverContext context) {
			// TODO Auto-generated method stub

		}

		@Override
		public AttributeMap getBaseAttributes() {
			return AttributeMap.emptyMap();
		}

	}

	@Named("Driver1")
	public static class Driver1 extends DriverStub {

		@Override
		public boolean supports(AttributeMap attributes) {
			return attributes.containsKey(MATCH_DRIVER1V1);
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId("Driver1", com.iris.model.Version.UNVERSIONED);
		}
	}

	@Named("Driver1")
	@Version(1)
	public static class Driver1V1 extends DriverStub {

		@Override
		public boolean supports(AttributeMap attributes) {
			return attributes.containsKey(MATCH_DRIVER1V1);
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId("Driver1", new com.iris.model.Version(1));
		}
	}

	@Named("Driver1")
	@Version(2)
	public static class Driver1V2 extends DriverStub {

		@Override
		public boolean supports(AttributeMap attributes) {
			return attributes.containsKey(MATCH_DRIVER1V2);
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId("Driver1", new com.iris.model.Version(2));
		}
	}

	@Named("Driver1")
	@Version(3)
	public static class Driver1V3 extends AbstractDriverStub {
		@Override
		protected DeviceDriverDefinition createDriverDefinition() {
			ReflexDefinition reflex = new ReflexDefinition(
				ImmutableList.of(new ReflexMatchLifecycle(ReflexMatchLifecycle.Type.CONNECTED)),
				ImmutableList.of(new ReflexActionForward())
			);

			return DeviceDriverDefinition
				.builder()
				.withName(Injectors.getServiceName(this))
				.withVersion(Injectors.getServiceVersion(this, com.iris.model.Version.UNVERSIONED))
				.withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL, Population.NAME_QA, Population.NAME_BETA))
				.addReflex(reflex)
				.create();
		}

		@Override
		public boolean supports(AttributeMap attributes) {
			return attributes.containsKey(MATCH_DRIVER1V3);
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId("Driver1", new com.iris.model.Version(3));
		}
	}

	@Named("Driver1")
	@Version(4)
	public static class Driver1V4 extends AbstractDriverStub {
		@Override
		protected DeviceDriverDefinition createDriverDefinition() {
			return DeviceDriverDefinition
				.builder()
				.withName(Injectors.getServiceName(this))
				.withVersion(Injectors.getServiceVersion(this, com.iris.model.Version.UNVERSIONED))
				.withPopulations(ImmutableList.of("beta"))
				.create();
		}

		@Override
		public boolean supports(AttributeMap attributes) {
			return attributes.containsKey(MATCH_DRIVER1V4);
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId("Driver1", new com.iris.model.Version(4));
		}
	}

	@Named("Driver1")
	@Version(5)
	public static class Driver1V5 extends AbstractDriverStub {
		@Override
		protected DeviceDriverDefinition createDriverDefinition() {
			ReflexDefinition reflex = new ReflexDefinition(
				ImmutableList.of(new ReflexMatchLifecycle(ReflexMatchLifecycle.Type.CONNECTED)),
				ImmutableList.of(new ReflexActionForward())
			);

			return DeviceDriverDefinition
				.builder()
				.withName(Injectors.getServiceName(this))
				.withVersion(Injectors.getServiceVersion(this, com.iris.model.Version.UNVERSIONED))
				.withPopulations(ImmutableList.of("beta"))
				.addReflex(reflex)
				.create();
		}

		@Override
		public boolean supports(AttributeMap attributes) {
			return attributes.containsKey(MATCH_DRIVER1V5);
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId("Driver1", new com.iris.model.Version(5));
		}
	}

	public static class Driver2 extends DriverStub {

		@Override
		public boolean supports(AttributeMap attributes) {
			return attributes.containsKey(MATCH_DRIVER2);
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId(getClass().getSimpleName(), com.iris.model.Version.UNVERSIONED);
		}
	}

	public static class FallbackDriver extends DriverStub {
		@Override
		public boolean supports(AttributeMap attributes) {
			return true;
		}

		@Override
		public DriverId getDriverId() {
			return new DriverId("_" + getClass().getSimpleName(), com.iris.model.Version.UNVERSIONED);
		}
	}

	private static AttributeKey<Boolean> MATCH_DRIVER1V1 =
			AttributeKey.create("MatchDriver1V1", Boolean.class);
	private static AttributeKey<Boolean> MATCH_DRIVER1V2 =
			AttributeKey.create("MatchDriver1V2", Boolean.class);
	private static AttributeKey<Boolean> MATCH_DRIVER1V3 =
			AttributeKey.create("MatchDriver1V3", Boolean.class);
	private static AttributeKey<Boolean> MATCH_DRIVER1V4 =
			AttributeKey.create("MatchDriver1V4", Boolean.class);
	private static AttributeKey<Boolean> MATCH_DRIVER1V5 =
			AttributeKey.create("MatchDriver1V5", Boolean.class);
	private static AttributeKey<Boolean> MATCH_DRIVER2 =
			AttributeKey.create("MatchDriver2", Boolean.class);

}

