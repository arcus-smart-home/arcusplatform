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
package com.iris.platform.pairing.customization;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.client.model.device.ClientDeviceModel.DeviceAdvanced;
import com.iris.core.dao.DeviceDAO;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.AlertCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.ProductCapability;
import com.iris.messages.capability.SchedulableCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.PairingCustomizationStep;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.platform.pairing.ProductLoader;
import com.iris.platform.subsystem.SubsystemDao;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogEntry.Cert;
import com.iris.prodcat.pairing.serializer.CustomizationType;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({DeviceDAO.class, PairingDeviceDao.class, SubsystemDao.class, ProductLoader.class})
public class TestPairingCustomizationManager extends IrisMockTestCase {

	@Inject
	private DeviceDAO mockDeviceDao;
	@Inject
	private PairingDeviceDao mockPairingDevDao;
	@Inject
	private SubsystemDao mockSubsystemDao;
	@Inject
	private ProductLoader mockProductLoader;
	
	@Inject
	private PairingCustomizationManager customizationMgr;
	
	private String productId = "1dbb3f";
	private Place place;
	
	@Before
	public void initMockData() {		 
		place = Fixtures.createPlace();
		place.setId(UUID.randomUUID());
		
		ModelEntity alarmSubsystemModel = new ModelEntity(ModelFixtures
				.buildSubsystemAttributes(place.getId(), AlarmSubsystemCapability.NAMESPACE).create());
		//Called by SecurityCustomization
		EasyMock.expect(mockSubsystemDao.findByPlaceAndNamespace(place.getId(), AlarmSubsystemCapability.NAMESPACE)).andReturn(alarmSubsystemModel );
		//Called by UncertifiedCustomization
		Address productAddress = Address.platformService(productId, ProductCapability.NAMESPACE);
		ProductCatalogEntry productEntry = new ProductCatalogEntry();
		productEntry.setId(productId);
		productEntry.setCert(Cert.WORKS);
		EasyMock.expect(mockProductLoader.get(place, productAddress)).andReturn(productEntry).anyTimes();
	}
	
	@Test
	public void testHWCloudThermostat() throws Exception {		
		ModelEntity deviceModelEntity = createDeviceModel(ImmutableMap.<String, Object>of(
				SchedulableCapability.ATTR_SCHEDULEENABLED, Boolean.TRUE,
				SchedulableCapability.ATTR_TYPE, SchedulableCapability.TYPE_DEVICE_ONLY),
				ThermostatCapability.NAMESPACE, SchedulableCapability.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		
		assertContainSteps(steps, true, CustomizationType.NAME, CustomizationType.FAVORITE);
	}
	
	@Test
	public void testThermostatNoSchedulable() throws Exception {	
		//thermostat does not support Schedulable capability 
		ModelEntity deviceModelEntity = createDeviceModel(ImmutableMap.<String, Object>of(),
				ThermostatCapability.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		
		assertContainSteps(steps, true, CustomizationType.NAME, CustomizationType.FAVORITE, CustomizationType.SCHEDULE);
	}
	
	@Test
	public void testThermostatWithSupportedSchedulable() throws Exception {		
		ModelEntity deviceModelEntity = createDeviceModel(ImmutableMap.<String, Object>of(
				SchedulableCapability.ATTR_SCHEDULEENABLED, Boolean.TRUE,
				SchedulableCapability.ATTR_TYPE, SchedulableCapability.TYPE_SUPPORTED_DRIVER),
				ThermostatCapability.NAMESPACE, SchedulableCapability.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		
		assertContainSteps(steps, true, CustomizationType.NAME, CustomizationType.FAVORITE, CustomizationType.SCHEDULE);
	}
	
	@Test
	public void testSiren() throws Exception {
		ModelEntity deviceModelEntity = createDeviceModel(ImmutableMap.<String, Object>of(),
				AlertCapability.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		
		assertContainSteps(steps, false, CustomizationType.NAME, CustomizationType.FAVORITE);
		assertContainsInfoSteps(steps, true, "siren");
	}
	
	@Test
	public void testPresence() throws Exception {
		ModelEntity deviceModelEntity = createDeviceModel(ImmutableMap.<String, Object>of(),
				PresenceCapability.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		
		assertContainSteps(steps, false, CustomizationType.NAME, CustomizationType.FAVORITE, CustomizationType.PRESENCE_ASSIGNMENT);
		
	}
	
	@Test
	public void testUnknown() throws Exception {
		//empty product id
		ModelEntity deviceModelEntity = createDeviceModel("", ImmutableMap.<String, Object>of(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED),
				DeviceAdvanced.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		printSteps(steps);
		assertEquals(1, steps.size());  //no NAME or FAVORITE
		assertContainsInfoSteps(steps, true, "unknown");
	}
	
	@Test
	public void testUnknown2() throws Exception {
		//product id not exist in product catalog
		String curProductId = "000001";
		Address productAddress = Address.platformService(curProductId, ProductCapability.NAMESPACE);
		EasyMock.expect(mockProductLoader.get(place, productAddress)).andReturn(null).anyTimes();

		ModelEntity deviceModelEntity = createDeviceModel(curProductId, ImmutableMap.<String, Object>of(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED),
				DeviceAdvanced.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		printSteps(steps);
		assertEquals(1, steps.size());  //no NAME or FAVORITE
		assertContainsInfoSteps(steps, true, "unknown");
	}
	
	@Test
	public void testUncertified() throws Exception {
		String curProductId = "000001";
		Address productAddress = Address.platformService(curProductId, ProductCapability.NAMESPACE);
		ProductCatalogEntry productEntry = new ProductCatalogEntry();
		productEntry.setId(curProductId);
		productEntry.setCert(Cert.NONE);
		EasyMock.expect(mockProductLoader.get(place, productAddress)).andReturn(productEntry).anyTimes();
		
		ModelEntity deviceModelEntity = createDeviceModel(curProductId, ImmutableMap.<String, Object>of(DeviceAdvancedCapability.ATTR_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED),
				DeviceAdvanced.NAMESPACE);		
		
		replay();
		
		PairingDevice pairingDevice = new PairingDevice();
		PairingDeviceModel.setDeviceAddress(pairingDevice, deviceModelEntity.getAddress().toString());
		List<PairingCustomizationStep> steps = customizationMgr.apply(place, pairingDevice );		
		printSteps(steps);
		assertEquals(1, steps.size());  //no NAME or FAVORITE
		assertContainsInfoSteps(steps, true, "uncertified");
	}
	

		
	
	private void assertContainsInfoSteps(List<PairingCustomizationStep> steps, boolean checkSize, String... ids) {
		final String idPrefix = "customization/info/";	
		List<PairingCustomizationStep> infoSteps = steps.stream().filter(t -> t.getAction().equals(CustomizationType.INFO.name())).collect(Collectors.toList());
		for(String id : ids) {
			boolean found = false;
			for(PairingCustomizationStep step : infoSteps) {
				if((idPrefix+id).equals(step.getId())) {
					found = true;
				}
			}
			if(!found) {
				fail(id + " does not exist in the return steps.");
			}
		}
		
		if(checkSize) {
			assertEquals(ids.length, infoSteps.size());
		}
		
	}

	private ModelEntity createDeviceModel(Map<String, Object> attributeMap, String... capabilities) {
		return createDeviceModel(productId, attributeMap, capabilities);
	}
	
	private ModelEntity createDeviceModel(String productId, Map<String, Object> attributeMap, String... capabilities) {
		Model deviceModel = ModelFixtures.buildDeviceAttributes(capabilities)
				.putAll(attributeMap)
				.toModel();
		DeviceModel.setPlace(deviceModel, place.getId().toString());
		DeviceModel.setProductId(deviceModel, productId);
		ModelEntity deviceModelEntity = new ModelEntity(deviceModel.toMap());
		EasyMock.expect(mockDeviceDao.modelById(UUID.fromString(deviceModel.getId()))).andReturn(deviceModelEntity);
		return deviceModelEntity;
	}
	
	private void assertContainSteps(List<PairingCustomizationStep> steps, boolean checkSize, CustomizationType... types) {
		printSteps(steps);
		for(CustomizationType type : types) {
			assertContainStep(steps, type);
		}
		if(checkSize) {
			assertEquals(types.length, steps.size());
		}
	}
	
	private void printSteps(List<PairingCustomizationStep> steps) {
		steps.forEach(s -> {
			System.out.println(ReflectionToStringBuilder.toString(s, ToStringStyle.MULTI_LINE_STYLE));
		});
	}

	private void assertContainStep(List<PairingCustomizationStep> steps, CustomizationType type) {
		boolean found = false;
		for(PairingCustomizationStep step : steps) {
			if(type.name().equals(step.getAction())) {
				found = true;
				break;
			}
		}
		assertTrue(found);
	}
}

