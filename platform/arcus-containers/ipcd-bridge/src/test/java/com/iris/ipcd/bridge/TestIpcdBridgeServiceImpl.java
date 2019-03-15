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
package com.iris.ipcd.bridge;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.bridge.bus.ProtocolBusListener;
import com.iris.bridge.bus.ProtocolBusService;
import com.iris.bridge.server.session.ClientToken;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.io.json.gson.GsonModule;
import com.iris.ipcd.server.IpcdServerModule;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.IpcdCommand;
import com.iris.protocol.ipcd.message.model.IpcdEvent;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.SetParameterValuesCommand;
import com.iris.protocol.ipcd.message.model.ValueChange;

public class TestIpcdBridgeServiceImpl {
	private InMemoryProtocolMessageBus protocolBus;
	private Device ipcdDevice;
	private String clientId;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		Bootstrap bootstrap = Bootstrap.builder()
		         .withModuleClasses(IpcdServerModule.class, GsonModule.class, MessagesModule.class)
		         .withModules(new AbstractModule() {
	               @Override
	               protected void configure() {
	                  bind(ProtocolMessageBus.class).to(InMemoryProtocolMessageBus.class);
	                  bind(PlatformMessageBus.class).to(InMemoryPlatformMessageBus.class);
	               }

		         })
            .withConfigPaths("src/dist/conf/ipcd-bridge.properties")
            .build();
		   ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));

			this.protocolBus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
			this.ipcdDevice = new Device();
			this.ipcdDevice.setIpcdver("0.3");
			this.ipcdDevice.setVendor("Blackbox");
			this.ipcdDevice.setModel("Switch1");
			this.ipcdDevice.setSn("123456789");
			this.clientId = ProtocolDeviceId.hashDeviceId(ipcdDevice.getVendor() + "-" + ipcdDevice.getModel() + "-" + ipcdDevice.getSn()).getRepresentation();
	}

	@After
	public void tearDown() throws Exception {
		ServiceLocator.destroy();
	}

	@Ignore
	@Test
	public void testAddBridgeListener() throws Exception {
		SetParameterValuesCommand setParametersCommand = new SetParameterValuesCommand();
		setParametersCommand.setTxnid("12345");
		Map<String,Object> values = new LinkedHashMap<String,Object>();
		values.put("bb.switch", "on");
		setParametersCommand.setValues(values);

		ProtocolMessage sendMsg = ProtocolMessage.createProtocolMessage(
				Address.fromString("DRIV:dev:0a875067-76de-4f1c-82f5-6e0a2f15554e"),
            Address.protocolAddress("IPCD", ProtocolDeviceId.hashDeviceId("Blackbox-Switch1-123456789")),
				IpcdProtocol.INSTANCE,
				setParametersCommand
      );

		final CountDownLatch latch = new CountDownLatch(1);
		ProtocolBusService ipcdBridgeService = ServiceLocator.getInstance(ProtocolBusService.class);
		ipcdBridgeService.addProtocolListener(new ProtocolBusListener() {

         @Override
         public void onMessage(ClientToken ct, ProtocolMessage msg) {
            Assert.assertEquals("Client Id should be protocol address", msg.getDestination().getRepresentation(), ct.getRepresentation());
            Assert.assertEquals("Device Id should match", Address.protocolAddress("IPCD", "Blackbox-Switch1-123456789"), msg.getDestination());
            IpcdMessage ipcdMessage = msg.getValue(IpcdProtocol.INSTANCE);
            Assert.assertTrue(ipcdMessage.getMessageType().isServer());
            Assert.assertEquals(MessageType.command, ipcdMessage.getMessageType());
            IpcdCommand cmd = (IpcdCommand)ipcdMessage;
            Assert.assertTrue("Command should be to set parameter values", cmd instanceof SetParameterValuesCommand);
            Map<String,Object> values = ((SetParameterValuesCommand)cmd).getValues();
            Assert.assertEquals("Should only be one parameter-value pair", 1, values.size());
            Assert.assertEquals("Paraeter bb.switch should have value of on", "on", values.get("bb.switch"));
            latch.countDown();
         }

		});

		protocolBus.send(sendMsg);
		boolean messageReceived = latch.await(1000, TimeUnit.MILLISECONDS);

		Assert.assertTrue("Message should have arrived ", messageReceived);
	}

	@Ignore
	@Test
	public void testPlaceMessageOnBus() throws Exception {
	   ValueChange change = new ValueChange();
	   change.setParameter("bb.switch");
	   change.setValue("on");
	   change.setThresholdRule("onChange");
	   change.setThresholdValue(true);
	   	   
		IpcdEvent event = createValueChangeEvent(Arrays.asList(change));

		String deviceId = getClientId(event.getDevice());
		
      ProtocolMessage protocolMessage = ProtocolMessage.createProtocolMessage(
            Address.protocolAddress("IPCD", deviceId),
            Address.broadcastAddress(),
            IpcdProtocol.INSTANCE,
            event
      );

		ProtocolBusService ipcdBridgeService = ServiceLocator.getInstance(ProtocolBusService.class);
		ipcdBridgeService.placeMessageOnProtocolBus(protocolMessage);

		ProtocolMessage msg = protocolBus.take();
		Assert.assertNotNull("Should have pulled a message off the queue", msg);
		Assert.assertEquals("Client Ids should match", clientId, ((DeviceProtocolAddress) msg.getSource()).getProtocolDeviceId().getRepresentation());
		Assert.assertEquals(Address.broadcastAddress(), msg.getDestination());

		IpcdMessage ipcdMessage = IpcdProtocol.INSTANCE.createDeserializer().deserialize(msg.getBuffer());
		Assert.assertEquals("The message should be an EventAction.", MessageType.event, ipcdMessage.getMessageType());
		IpcdEvent action = (IpcdEvent)ipcdMessage;
		Device deviceInfo = action.getDevice();
		Assert.assertEquals("Device information should match: ", ipcdDevice, deviceInfo);
		Assert.assertEquals("There should be only one event: ", 1, action.getEvents().size());
		Assert.assertEquals("The event should be on value change: ", "onValueChange", action.getEvents().get(0));
		Assert.assertEquals("There should be only one value change: ", 1, action.getValueChanges().size());
		ValueChange valueChange = action.getValueChanges().get(0);
		Assert.assertEquals("The parameter should be bb.swtich: ", "bb.switch", valueChange.getParameter());
		Assert.assertEquals("The value should be on: ", "on", valueChange.getValue());
		Assert.assertEquals("The threshold rule should be onChange", "onChange", valueChange.getThresholdRule());
		Assert.assertTrue("The threshold value should be boolean true", (Boolean)valueChange.getThresholdValue());
	}

	protected IpcdEvent createValueChangeEvent(List<ValueChange> changes) {
		IpcdEvent action = new IpcdEvent();
		action.setDevice(ipcdDevice);
		action.setEvents(Arrays.asList("onValueChange"));
		action.setValueChanges(changes);
		return action;
	}

	private String getClientId(Device device) {
      return device.getVendor() + '-' + device.getModel() + '-' + device.getSn();
   }
}

