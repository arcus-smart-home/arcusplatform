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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.CallTreeEntry;

@RunWith(Parameterized.class)
public class TestAlarmSubsystemCallTree extends PlatformAlarmSubsystemTestCase {
	private Model owner;
	private Model person1;
	private Model person2;
	List<String> personAddressList;
	private String serviceLevel;
	private Model person3;
	private Model person5;
	private Model person6;
	private Model person4;
	
	@Parameters(name="{0}")
	public static Iterable<Object []> files() {
      return ImmutableList.of(
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM },
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM_ANNUAL },
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON },
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_ANNUAL },
            new Object [] { PlaceCapability.SERVICELEVEL_BASIC },
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM_FREE },
            new Object [] { PlaceCapability.SERVICELEVEL_PREMIUM_PROMON_FREE }
      );
	}
	
	public TestAlarmSubsystemCallTree(String serviceLevel) {
		this.serviceLevel = serviceLevel;
	}
	
	@Before
	public void init() throws Exception {
		PlaceModel.setServiceLevel(placeModel, serviceLevel);		
		addModel(placeModel.toMap());
		
		owner = new SimpleModel(ModelFixtures.createPersonWith("Junie", "Jones", "jjones@email.com", "555-123-1234", "1234", placeModel.getId()));
		person1 = new SimpleModel(ModelFixtures.createPersonWith("Frank", "Miller", "fmiller@email.com", "666-123-1234", "2345", placeModel.getId()));
		person2 = new SimpleModel(ModelFixtures.createPersonWith("Hellen", "Miller", "hmiller@email.com", "666-123-1234", "3456", placeModel.getId()));
		person3 = new SimpleModel(ModelFixtures.createPersonWith("Hellen3", "Miller", "hmiller3@email.com", "666-123-1234", "4456", placeModel.getId()));
		person4 = new SimpleModel(ModelFixtures.createPersonWith("Hellen4", "Miller", "hmiller4@email.com", "666-123-1234", "5456", placeModel.getId()));
		person5 = new SimpleModel(ModelFixtures.createPersonWith("Hellen5", "Miller", "hmiller5@email.com", "666-123-1234", "6456", placeModel.getId()));
		person6 = new SimpleModel(ModelFixtures.createPersonWith("Hellen6", "Miller", "hmiller6@email.com", "666-123-1234", "7456", placeModel.getId()));
		addModel(owner.toMap());
		addModel(person1.toMap());
		addModel(person2.toMap());	
		addModel(person3.toMap());	
		addModel(person4.toMap());	
		addModel(person5.toMap());	
		addModel(person6.toMap());	
		AccountModel.setOwner(accountModel, owner.getId());
		addModel(accountModel.toMap());
		
		
		init(subsystem);
		subsystem.setDefinitionRegistry(ClasspathDefinitionRegistry.instance());
		
		personAddressList = ImmutableList.<String>of(owner.getAddress().getRepresentation(), person1.getAddress().getRepresentation(), person2.getAddress().getRepresentation());
		context.setVariable("treePeople", personAddressList);	//this is a hack for the CallTreeBinder
	}
	

	private CallTreeEntry createCallTreeEntry(Model person, boolean enableFlag) {
		CallTreeEntry entry1 = new CallTreeEntry();
		entry1.setEnabled(enableFlag);
		entry1.setPerson(person.getAddress().getRepresentation());
		return entry1;
	}
	
	@Test
	public void testCallTreeInvalidPerson() {
		List<Map<String, Object>> list = new LinkedList<Map<String,Object>>();
		CallTreeEntry entry1 = createCallTreeEntry(owner, true);
		CallTreeEntry entry2 = createCallTreeEntry(person1, true);
		Model invalidPerson = new SimpleModel(ModelFixtures.createPersonWith("May", "Tattler", "mtattler@email.com", "777-123-1234", "1111", placeModel.getId()));;
		CallTreeEntry entry3 = createCallTreeEntry(invalidPerson, true);
		list.addAll(ImmutableList.<Map<String,Object>>of(entry1.toMap(), entry2.toMap(), entry3.toMap()));
		
		doSetAttributeForFailedScenario(list, "Should get an exception for invalid person and service level ["+this.serviceLevel+"]");			
	}
	
	@Test
	public void testCallTreeNoOwner() {
		List<Map<String, Object>> list = new LinkedList<Map<String,Object>>();
		CallTreeEntry entry1 = createCallTreeEntry(person1, true);
		CallTreeEntry entry2 = createCallTreeEntry(person2, true);		
		list.addAll(ImmutableList.<Map<String,Object>>of(entry1.toMap(), entry2.toMap()));
		
		//1. service level premium, no owner, OK
		doSetAttributeForFailedScenario(list, "Should get an exception for no owner when service level "+this.serviceLevel);	
		
	}
	
	@Test
	public void testCallTreeNoName() {
		PersonModel.setFirstName(person1, "");
		PersonModel.setLastName(person1, "");
		updateModel(person1.getAddress(), person1.toMap());
		CallTreeEntry entry1 = createCallTreeEntry(owner, true);
		CallTreeEntry entry2 = createCallTreeEntry(person1, true);	
		doSetAttributeForFailedScenario(ImmutableList.<Map<String,Object>>of(entry1.toMap(),  entry2.toMap()), "Should get an exception for person with no firstname when service level "+this.serviceLevel);		
	}
	
	@Test
	public void testCallTreeNoPin() {
		PersonModel.setPlacesWithPin(person1, ImmutableSet.<String>of());
		updateModel(person1.getAddress(), person1.toMap());
		CallTreeEntry entry1 = createCallTreeEntry(owner, true);
		CallTreeEntry entry2 = createCallTreeEntry(person1, true);	
		doSetAttributeForFailedScenario(ImmutableList.<Map<String,Object>>of(entry1.toMap(),  entry2.toMap()), "Should get an exception for person with no pin when service level "+this.serviceLevel);		
	}
	
	@Test
	public void testCallTree() {		
		List<Map<String, Object>> list = new LinkedList<Map<String,Object>>();
		CallTreeEntry entry1 = createCallTreeEntry(owner, true);
		CallTreeEntry entry2 = createCallTreeEntry(person1, true);
		CallTreeEntry entry3 = createCallTreeEntry(person2, true);
		list.addAll(ImmutableList.<Map<String,Object>>of(entry1.toMap(), entry2.toMap(), entry3.toMap()));
		replay();
			
						
		//1. owner first and enabled
		doSetAttributeForOkScenario(list, ImmutableList.<CallTreeEntry> of(entry1, entry2, entry3));
		
		
		//2. owner 2nd and enabled, OK, should fail
		Map<String, Object> removed = list.remove(0);
		list.add(1, removed);
		doSetAttributeForFailedScenario(list, "Should get an exception for account owner not first");
		
		//3. owner first and disabled, fail
		entry1.setEnabled(false);
		removed = list.remove(1);
		list.add(0, entry1.toMap());
		doSetAttributeForFailedScenario(list, "Should get an exception for owner first and disabled");		
				
	}
	
	@Test
	public void testCallTreeOnePerson() {
		CallTreeEntry entry1 = createCallTreeEntry(owner, true);
		CallTreeEntry entry2 = createCallTreeEntry(person1, false);
		List<Map<String, Object>> list = ImmutableList.<Map<String, Object>> of(entry1.toMap(), entry2.toMap());
		doSetAttributeForOkScenario(list, ImmutableList.<CallTreeEntry> of(entry1, entry2));
	}
	
	@Test
	public void testCallTreeMaxPeopleLimit() {
		//max 6, CallTree.MAX_CALLTREE_ENTRIES
		CallTreeEntry entry1 = createCallTreeEntry(owner, true);
		CallTreeEntry entry2 = createCallTreeEntry(person1, true);
		CallTreeEntry entry3 = createCallTreeEntry(person2, true);
		CallTreeEntry entry4 = createCallTreeEntry(person3, true);
		CallTreeEntry entry5 = createCallTreeEntry(person4, true);
		CallTreeEntry entry6 = createCallTreeEntry(person5, true);
		CallTreeEntry entry7 = createCallTreeEntry(person6, true);
		List<Map<String, Object>> list = ImmutableList.<Map<String, Object>> of(
				entry1.toMap(), entry2.toMap(), entry3.toMap(),
				entry4.toMap(), entry5.toMap(), entry6.toMap(),
				entry7.toMap()
				);
		doSetAttributeForFailedScenario(list, "The call tree should have errored for more than 6 people");
	}
	

	private void doSetAttributeForOkScenario(List<Map<String, Object>> callTreeValue, List<CallTreeEntry> expectedList) {
		subsystem.setAttribute(AlarmSubsystemCapability.ATTR_CALLTREE, callTreeValue, context);
		List<Map<String, Object>> actualList = context.model().getCallTree();
		assertEquals(expectedList.size(), actualList.size());
		for(int i=0; i<expectedList.size(); i++) {
			assertEqualForCallTreeEntry(expectedList.get(i), new CallTreeEntry(actualList.get(i)));
		}
				
	}
	
	private void doSetAttributeForFailedScenario(List<Map<String, Object>> callTreeValue, String failMsg) {
		try{
			subsystem.setAttribute(AlarmSubsystemCapability.ATTR_CALLTREE, callTreeValue, context);
			fail(failMsg);
		}catch(Exception e) {  
			//ok
		}
	}
	
	
	//this method does not handle null value for CallTreeEntry.Enabled and CallTreeEntry.Person
	private void assertEqualForCallTreeEntry(CallTreeEntry entry1, CallTreeEntry entry2) {
		if(entry1 == null) {
			if(entry2 == null) {
				return;
			}
		}else{
			if(entry2 != null) {
				if(entry1.getEnabled().equals(entry2.getEnabled()) && entry1.getPerson().equals(entry2.getPerson())) {
					return;
				}
			}
		}		
		fail(String.format("CallTreeEntry entry1[%s] not equal to entry2[%s]", entry1.toMap(), entry2.toMap()));
	}
	
	
	
	
}

