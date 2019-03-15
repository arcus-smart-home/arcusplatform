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
package com.iris.util;

import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.iris.messages.model.Device;
import com.iris.model.Version;
import com.iris.util.IrisBeans.BeanGetter;

public class TestIrisBeans {
	private final static UUID ACCOUNT_ID = UUID.fromString("5aab20ec-13d7-4d79-95b5-0d3a464f0479");
	private final static String PROTOCOL = "HALO";
	private final static String PROTOCOL_ID = "SUqbzusG09+RQuv6JBJutAAAAAA=";
	private final static String DRIVER_NAME = "The Driver Name";
	private final static Version DRIVER_VERSION = new Version(2, 3, "abc");
	private final static String DRIVER_ADDRESS = "DRIV:dev:83f4407a-ed2a-4d22-b648-c74081ec6c12";
	private final static String PROTOCOL_ADDRESS = "PROT:HALO:SUqbzusG09+RQuv6JBJutAAAAAA=";
	private final static String HUB_ID = "ABC-1234";
	private final static UUID PLACE_ID = UUID.fromString("176d3cab-f666-4430-8b71-84e75b599b0e");
	private final static Set<String> CAPS = IrisCollections.setOf("base", "dev", "devadv", "devconn", "mot", "temp");
	private final static String DEVTYPE_HINT = "mot";
	private final static String NAME = "My Device";
	private final static String VENDOR = "Aquaman";
	private final static String MODEL = "Thingatron";
	private final static String PRODUCT_ID = "2000x";
	private final static String STATE = "active";

	@Test
	public void testDevice() {
		Device device = new Device();
		device.setAccount(ACCOUNT_ID);
		device.setProtocol(PROTOCOL);
		device.setProtocolid(PROTOCOL_ID);
		device.setDrivername(DRIVER_NAME);
		device.setDriverversion(DRIVER_VERSION);
		device.setAddress(DRIVER_ADDRESS);
		device.setProtocolAddress(PROTOCOL_ADDRESS);
		device.setHubId(HUB_ID);
		device.setPlace(PLACE_ID);
		device.setCaps(CAPS);
		device.setDevtypehint(DEVTYPE_HINT);
		device.setName(NAME);
		device.setVendor(VENDOR);
		device.setModel(MODEL);
		device.setProductId(PRODUCT_ID);
		device.setState(STATE);
		
		BeanGetter<Device> deviceGetter = IrisBeans.getter(Device.class);
		
		Assert.assertEquals(ACCOUNT_ID, deviceGetter.get("account", device));
		Assert.assertEquals(PROTOCOL, deviceGetter.get("protocol", device));
		Assert.assertEquals(PROTOCOL_ID, deviceGetter.get("protocolId", device));
		Assert.assertEquals(DRIVER_NAME, deviceGetter.get("DriverName", device));
		Assert.assertEquals(DRIVER_VERSION, deviceGetter.get("DRIVERVERSION", device));
		Assert.assertEquals(DRIVER_ADDRESS, deviceGetter.get("address", device));
		Assert.assertEquals(PROTOCOL_ADDRESS, deviceGetter.get("ProtocolAddress", device));
		Assert.assertEquals(HUB_ID, deviceGetter.get("hubid", device));
		Assert.assertEquals(PLACE_ID, deviceGetter.get("placeid", device));
		Assert.assertEquals(CAPS, deviceGetter.get("caps", device));
		Assert.assertEquals(DEVTYPE_HINT, deviceGetter.get("devtypeHint", device));
		Assert.assertEquals(NAME, deviceGetter.get("name", device));
		Assert.assertEquals(VENDOR, deviceGetter.get("vendor", device));
		Assert.assertEquals(MODEL, deviceGetter.get("model", device));
		Assert.assertEquals(PRODUCT_ID, deviceGetter.get("productid", device));
		Assert.assertEquals(STATE, deviceGetter.get("state", device));
		Assert.assertNull(deviceGetter.get("subprotocol", device));
	}
	
	@Test
	public void testDeviceWithCoerce() {
		Device device = new Device();
		device.setAccount(ACCOUNT_ID);
		device.setProtocol(PROTOCOL);
		device.setProtocolid(PROTOCOL_ID);
		device.setDrivername(DRIVER_NAME);
		device.setDriverversion(DRIVER_VERSION);
		device.setAddress(DRIVER_ADDRESS);
		device.setProtocolAddress(PROTOCOL_ADDRESS);
		device.setHubId(HUB_ID);
		device.setPlace(PLACE_ID);
		device.setCaps(CAPS);
		device.setDevtypehint(DEVTYPE_HINT);
		device.setName(NAME);
		device.setVendor(VENDOR);
		device.setModel(MODEL);
		device.setProductId(PRODUCT_ID);
		device.setState(STATE);
		
		BeanGetter<Device> deviceGetter = IrisBeans.getter(Device.class);
		
		Assert.assertEquals(ACCOUNT_ID.toString(), deviceGetter.getAs(String.class, "account", device));
	}
}

