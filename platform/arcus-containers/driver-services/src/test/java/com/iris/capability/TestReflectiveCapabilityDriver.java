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
package com.iris.capability;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.inject.Named;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.iris.Utils;
import com.iris.annotation.Version;
import com.iris.capability.TestReflectiveCapabilityDriver.TestCapabilityDriver;
import com.iris.capability.attribute.Attributes;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.capability.Capability;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 *
 */

@Mocks({PlacePopulationCacheManager.class, TestCapabilityDriver.class, PersonPlaceAssocDAO.class, PersonDAO.class})
@Modules({InMemoryMessageModule.class})
public class TestReflectiveCapabilityDriver extends IrisMockTestCase {
   private String name = "Test";
	private String namespace = "test";

	@Inject private InMemoryPlatformMessageBus platformBus;
	@Inject private TestCapabilityDriver mockTestDriver;
	private Map<String, Object> attributes = new HashMap<>();
	private DeviceDriverContext context;
	@Inject private PlacePopulationCacheManager mockPopulationCacheMgr;
	
	

   @Before
	public void initMocks() throws Exception {
   	
	   EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
	   EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(String.class))).andReturn(Population.NAME_GENERAL).anyTimes();

	   context = new PlatformDeviceDriverContext(
		      Fixtures.createDevice(),
		      DeviceDriverDefinition.builder().withName("TestDriver").withPopulations(ImmutableList.<String>of(Population.NAME_GENERAL)).create(),
		      new DeviceDriverStateHolder(), mockPopulationCacheMgr
      );
		
	}

   public void replayMock() {
		replay();
	}

	public void verifyMock() {
		verify();
	}

	public PlatformMessage toMessage(MessageBody payload) {
	   return
	         PlatformMessage
	            .builder()
	            .from(Fixtures.createClientAddress())
	            .to(Fixtures.createDeviceAddress())
	            .withPayload(payload)
	            .create();
	}

	@Test
	public void testNoArguments() throws Exception {
		mockTestDriver.noArguments();
		EasyMock.expectLastCall();
		replayMock();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand("NoArguments").add()
					.create();

		Capability driver =
   		Capabilities
   			.implement(capability)
   			.with(mockTestDriver);

		MessageBody command = MessageBody.buildMessage("test:NoArguments", new HashMap<>());
		assertTrue(driver.getPlatformMessageHandler().handleEvent(context, toMessage(command)));

		PlatformMessage response = platformBus.take();
		MessageBody event = response.getValue();
		assertEquals("test:NoArgumentsResponse", event.getMessageType());
		assertEquals(Collections.emptyMap(), event.getAttributes());
		verifyMock();
	}

	@Test
	public void testAllArguments() throws Exception {
		String command = "AllArguments";

		mockTestDriver.allArguments(context, Utils.namespace(namespace, command), attributes);
		EasyMock.expectLastCall();
		replay();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand(command).add()
					.create();

		Capability driver =
   		Capabilities
   			.implement(capability)
   			.with(mockTestDriver);

      MessageBody dc = MessageBody.buildMessage(Utils.namespace(namespace, command), attributes);
      assertTrue(driver.getPlatformMessageHandler().handleEvent(context, toMessage(dc)));

      PlatformMessage response = platformBus.take();
      MessageBody event = response.getValue();
      assertEquals("test:AllArgumentsResponse", event.getMessageType());
      assertEquals(Collections.emptyMap(), event.getAttributes());
      verifyMock();
	}

	@Test
	public void testRepeatArguments() throws Exception {
		String command = "RepeatArguments";

		mockTestDriver.repeatArguments(context, context, Utils.namespace(namespace, command), attributes, Utils.namespace(namespace, command));
		EasyMock.expectLastCall();
		replay();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand(command).add()
					.create();

		Capability driver =
   		Capabilities
   			.implement(capability)
   			.with(mockTestDriver);

      MessageBody dc = MessageBody.buildMessage(Utils.namespace(namespace, command), attributes);
      assertTrue(driver.getPlatformMessageHandler().handleEvent(context, toMessage(dc)));

      PlatformMessage response = platformBus.take();
      MessageBody event = response.getValue();
      assertEquals("test:RepeatArgumentsResponse", event.getMessageType());
      assertEquals(Collections.emptyMap(), event.getAttributes());
      verifyMock();
	}

	@Test
	public void testDuplicateMethod() throws Exception {
		String command = "DuplicateCommand";

		replayMock();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand(command).add()
					.create();

		try {
   		Capability driver =
      		Capabilities
      			.implement(capability)
      			.with(mockTestDriver);
   		fail("Created a driver with ambiguous methods");
		}
		catch(IllegalStateException e) {
			// expected
			e.printStackTrace();
		}
		verifyMock();
	}

	@Test
	public void testInvalidArguments() throws Exception {
		String command = "InvalidArguments";

		replayMock();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand(command).add()
					.create();

		try {
   		Capability driver =
      		Capabilities
      			.implement(capability)
      			.with(mockTestDriver);
   		fail("Created a driver with invalid arguments");
		}
		catch(IllegalArgumentException e) {
			// expected
			e.printStackTrace();
		}
		verifyMock();
	}

	@Test
	public void testThrowsException() throws Exception {
		String command = "NoArguments";

		Exception cause = new Exception("Error");
		mockTestDriver.noArguments();
		EasyMock.expectLastCall().andThrow(cause);
		replayMock();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand(command).add()
					.create();

		Capability driver =
   		Capabilities
   			.implement(capability)
   			.with(mockTestDriver);

      MessageBody dc = MessageBody.buildMessage(Utils.namespace(namespace, command), attributes);
      assertTrue(driver.getPlatformMessageHandler().handleEvent(context, toMessage(dc)));

      PlatformMessage response = platformBus.take();
      ErrorEvent event = (ErrorEvent) response.getValue();
      assertEquals(Errors.fromException(cause), event);
      verifyMock();
	}

	@Test
	public void testAnnotatedClass() {
		TestAnnotationsDriver implementation = new TestAnnotationsDriver();
		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand("Method1").add()
					.buildCommand("Method2").add()
					.buildCommand("Method3").add()
					.create();

		Capability driver =
   		Capabilities
   			.implement(capability)
   			.with(implementation);

		assertEquals(namespace, driver.getNamespace());
		assertEquals("TestAnnotations", driver.getName());
		assertEquals(new com.iris.model.Version(3), driver.getVersion());
		// TODO list supported commands?
	}

	@Test
	public void testProtectedMethod() throws Exception {
		TestAnnotationsDriver implementation = new TestAnnotationsDriver();
		String command = "ProtectedMethod";

		replayMock();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand(command).add()
					.create();

		try {
   		Capability driver =
      		Capabilities
      			.implement(capability)
      			.with(implementation);
   		fail("Created a driver handler for a protected method");
		}
		catch(IllegalStateException e) {
			// expected
			e.printStackTrace();
		}
		verifyMock();
	}

	@Test
	public void testVoidReturnType() throws Exception {
		mockTestDriver.noArguments();
		EasyMock.expectLastCall();
		replayMock();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand("NoArguments").add()
					.create();

		Capability driver =
   		Capabilities
   			.implement(capability)
   			.with(mockTestDriver);

      MessageBody command = MessageBody.buildMessage("test:NoArguments", new HashMap<>());
      assertTrue(driver.getPlatformMessageHandler().handleEvent(context, toMessage(command)));

      PlatformMessage response = platformBus.take();
      MessageBody event = response.getValue();
      assertEquals("test:NoArgumentsResponse", event.getMessageType());
      assertEquals(Collections.emptyMap(), event.getAttributes());
      verifyMock();
	}

	@Test
	public void testStringReturnType() throws Exception {
		String command = "ReturnString";
		String returnValue = "test";
		EasyMock
			.expect(mockTestDriver.returnString())
			.andReturn(returnValue);
		replayMock();

		CapabilityDefinition capability =
				Capabilities
					.define()
					.withName(name)
					.withNamespace(namespace)
					.buildCommand(command)
					   .addReturnParameter(
					         Attributes.build(AttributeKey.create("response", String.class))
					            .required()
					            .readWrite()
					            .create())
						.add()
					.create();

		Capability driver =
   		Capabilities
   			.implement(capability)
   			.with(mockTestDriver);

      MessageBody request = MessageBody.buildMessage("test:ReturnString", new HashMap<>());
      assertTrue(driver.getPlatformMessageHandler().handleEvent(context, toMessage(request)));

      PlatformMessage response = platformBus.take();
      MessageBody event = response.getValue();
		assertEquals(Capabilities.namespace("test", command + "Response"), event.getMessageType());

		Map<String,Object> responseMap = new HashMap<String,Object>();
		responseMap.put("response", returnValue);

		assertEquals(responseMap, event.getAttributes());
		verifyMock();
	}

	@Test
	public void testAsyncReturnType() throws Exception {
	   String command = "ReturnAsync";
	   SettableFuture<String> returnValue = SettableFuture.create();
	   EasyMock
   	   .expect(mockTestDriver.returnAsync())
   	   .andReturn(returnValue);
	   replayMock();

	   CapabilityDefinition capability =
	         Capabilities
            .define()
            .withName(name)
            .withNamespace(namespace)
	         .buildCommand(command)
	         .addReturnParameter(
                        Attributes.build(AttributeKey.create("response", String.class))
                           .required()
                           .readWrite()
                           .create())
	         .add()
	         .create();

	   Capability driver =
	         Capabilities
	         .implement(capability)
	         .with(mockTestDriver);

      MessageBody request = MessageBody.buildMessage("test:ReturnAsync", new HashMap<>());
      assertTrue(driver.getPlatformMessageHandler().handleEvent(context, toMessage(request)));

      try {
         platformBus.take();
         fail();
      }
      catch(TimeoutException e) {
         // expected
      }

	   returnValue.set("testvalue");
	   PlatformMessage message = platformBus.take();
	   MessageBody event = message.getValue();
	   assertEquals(Capabilities.namespace("test", command + "Response"), event.getMessageType());

	   Map<String,Object> responseMap = new HashMap<>();
	   responseMap.put("response", "testvalue");

	   assertEquals(responseMap, event.getAttributes());
	   verifyMock();
	}

	// no point in adding annotation here as they aren't on the implemented class
	public static interface TestCapabilityDriver {

		public void noArguments() throws Exception;

		public void allArguments(DeviceDriverContext device, String command, Map<String, Object> attributes);

		public void repeatArguments(DeviceDriverContext device, DeviceDriverContext device2, String command, Map<String, Object> attributes, String command2);

		public void duplicateCommand();

		public void duplicateCommand(String command);

		public void invalidArguments(Date date);

		public String returnString();

		public ListenableFuture<String> returnAsync();
	}

	@Named("TestAnnotations")
	@Version(3)
	public static class TestAnnotationsDriver {

		@Named("Method1")
		public void method() {

		}

		@Named("Method2")
		public void method(String command) {

		}

		public void method3() { }

		@Named("ProtectedMethod")
		protected void method(String command, AttributeMap map) {

		}

		protected void method1(String command) {

		}
	}
}

