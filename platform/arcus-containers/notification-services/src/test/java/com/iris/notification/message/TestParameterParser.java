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
package com.iris.notification.message;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.StatefulRuleDefinition;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({DeviceDAO.class, PersonDAO.class, PlaceDAO.class, RuleDao.class})
public class TestParameterParser extends IrisMockTestCase {

	private final static String DEVICE_NAME = "Amazatron 2000";
	private final static String PERSON_FIRSTNAME = "Emily";
	private final static String PLACE_NAME = "My Crib";
	private final static String RULE_NAME = "All Shall Obey";
	private final static UUID ID = UUID.fromString("83f4407a-ed2a-4d22-b648-c74081ec6c12");
	private final static String DEVICE_ADDRESS = "DRIV:dev:" + ID.toString();
	private final static String PERSON_ADDRESS = "SERV:person:" + ID.toString();
	private final static String PLACE_ADDRESS = "SERV:place:" + ID.toString();
	private final static String RULE_ADDRESS = "SERV:rule:" + ID.toString() + ".1";
	
	@Inject private DeviceDAO mockDeviceDao;
	@Inject private PersonDAO mockPersonDao;
	@Inject private PlaceDAO mockPlaceDao;
	@Inject private RuleDao mockRuleDao;
	
	private ParameterParser parameterParser;
	private Device device;
	private Person person;
	private Place place;
	private RuleDefinition rule;
	
	@Override
   public void setUp() throws Exception {
	   super.setUp();
	   parameterParser = new ParameterParser(new ValueLookup(mockPlaceDao, mockDeviceDao, mockPersonDao, mockRuleDao));
	   device = Fixtures.createDevice();
	   device.setName(DEVICE_NAME);
	   person = Fixtures.createPerson();
	   person.setFirstName(PERSON_FIRSTNAME);
	   place = Fixtures.createPlace();
	   place.setName(PLACE_NAME);
	   rule = new StatefulRuleDefinition();
	   rule.setName(RULE_NAME);
   }

	@Test
	public void testSimpleParameters() {
		Map<String, String> params = ImmutableMap.of("One", "One Fish", "Two", "Two, Fish", "Three", "RedFish");
		doTest("One Fish", "Two, Fish", "RedFish", params);
	}
	
	@Test
	public void testMultiParameters2() {
		Map<String, String> params = ImmutableMap.of("One", "One Fish", "Two", "Two, Fish", "Three", "[RedFish,BlueFish]");
		doTest("One Fish", "Two, Fish", "RedFish and BlueFish", params);
	}
	
	@Test
	public void testMultiParameters3() {
		Map<String, String> params = ImmutableMap.of("One", "One Fish", "Two", "[ Bob,Fred, Jane]", "Three", "[RedFish,BlueFish]");
		doTest("One Fish", "Bob, Fred, and Jane", "RedFish and BlueFish", params);
	}
	
	@Test
	public void testMultiParameters4() {
		Map<String, String> params = ImmutableMap.of("One", "One Fish", "Two", "[ Bob,Fred, Jane, Jake ]", "Three", "[RedFish,BlueFish]");
		doTest("One Fish", "Bob, Fred, Jane, and Jake", "RedFish and BlueFish", params);
	}
	
	@Test
	public void testDeviceLookup() {
		Map<String, String> params = ImmutableMap.of("One", "One Fish", "Two", "Two Fish", "Three", "{" + DEVICE_ADDRESS + "}.name");
		EasyMock.expect(mockDeviceDao.findById(ID)).andReturn(device);
		EasyMock.replay(mockDeviceDao);
		doTest("One Fish", "Two Fish", DEVICE_NAME, params);
	}
	
	@Test
	public void testPersonLookup() {
		Map<String, String> params = ImmutableMap.of("One", 
				"One Fish", "Two", "Two Fish", "Three", "{" + PERSON_ADDRESS + "}.firstname");
		EasyMock.expect(mockPersonDao.findById(ID)).andReturn(person);
		EasyMock.replay(mockPersonDao);
		doTest("One Fish", "Two Fish", PERSON_FIRSTNAME, params);
	}
	
	@Test
	public void testPlaceLookup() {
		Map<String, String> params = ImmutableMap.of("One", 
				"One Fish", "Two", "Two Fish", "Three", "{" + PLACE_ADDRESS + "}.name");
		EasyMock.expect(mockPlaceDao.findById(ID)).andReturn(place);
		EasyMock.replay(mockPlaceDao);
		doTest("One Fish", "Two Fish", PLACE_NAME, params);
	}
	
	@Test
	public void testRuleLookup() {
		Map<String, String> params = ImmutableMap.of("One", 
				"One Fish", "Two", "Two Fish", "Three", "{" + RULE_ADDRESS + "}.name");
		EasyMock.expect(mockRuleDao.findById(ID, 1)).andReturn(rule);
		EasyMock.replay(mockRuleDao);
		doTest("One Fish", "Two Fish", RULE_NAME, params);
	}
	
	@Test
	public void testMultipleLookup() {
		Map<String, String> params = ImmutableMap.of("One", 
				"One Fish", "Two", "Two Fish", "Three", 
				"[{" + DEVICE_ADDRESS + "}.name, {" + PLACE_ADDRESS + "}.name, {" + PERSON_ADDRESS + "}.firstname]");
		EasyMock.expect(mockDeviceDao.findById(ID)).andReturn(device);
		EasyMock.expect(mockPlaceDao.findById(ID)).andReturn(place);
		EasyMock.expect(mockPersonDao.findById(ID)).andReturn(person);
		EasyMock.replay(mockDeviceDao, mockPlaceDao, mockPersonDao);
		doTest("One Fish", "Two Fish", DEVICE_NAME + ", " + PLACE_NAME + ", and " + PERSON_FIRSTNAME, params);
	}
	
	private void doTest(String value1, String value2, String value3, Map<String, String> origParams) {
		Map<String, String> resolved = resolveParameters(origParams);
		System.out.println(resolved);
		Assert.assertEquals(value1, resolved.get("One"));
		Assert.assertEquals(value2, resolved.get("Two"));
		Assert.assertEquals(value3, resolved.get("Three"));
	}
	
	private Map<String,String> resolveParameters(Map<String, String> origParams) {
   	if (origParams != null && !origParams.isEmpty()) {
   		Map<String, String> resolvedParameters = new HashMap<>(origParams.size());
   		for (Map.Entry<String, String> entry : origParams.entrySet()) {
   			resolvedParameters.put(entry.getKey(), parameterParser.parse(entry.getValue()));
   		}
   		return resolvedParameters;
   	}
   	else {
   		return origParams;
   	}
   }
}

