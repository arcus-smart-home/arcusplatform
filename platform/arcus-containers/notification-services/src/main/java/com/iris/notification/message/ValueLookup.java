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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.util.IrisBeans;
import com.iris.util.IrisBeans.BeanGetter;

public class ValueLookup {
	private final static Logger logger = LoggerFactory.getLogger(ValueLookup.class);
	
	private final PlaceDAO placeDao;
   private final DeviceDAO deviceDao;
   private final PersonDAO personDao;
   private final RuleDao ruleDao;
   
   private final BeanGetter<Device> deviceGetter = IrisBeans.getter(Device.class);
   private final BeanGetter<Place> placeGetter  = IrisBeans.getter(Place.class);
   private final BeanGetter<Person> personGetter = IrisBeans.getter(Person.class);
   private final BeanGetter<RuleDefinition> ruleGetter = IrisBeans.getter(RuleDefinition.class);
   
   @Inject
   public ValueLookup(PlaceDAO placeDao, DeviceDAO deviceDao, PersonDAO personDao, RuleDao ruleDao) {
   	this.placeDao = placeDao;
   	this.deviceDao = deviceDao;
   	this.personDao = personDao;
   	this.ruleDao = ruleDao;
   }
   
   public String get(String address, String property) {
   	try {
   		return get(Address.fromString(address), property);
   	} catch(Throwable t) {
   		logger.error("Invalid address {} in notification value lookup.", address, t);
   		return "";
   	}
   }
   
   public String get(Address address, String property) {   	
   	if (address.getGroup().equals(DeviceCapability.NAMESPACE)) {
   		return getValueFromDevice(address, property);
	   } else if (address.getGroup().equals(PersonCapability.NAMESPACE)) {
	   	return getValueFromPerson(address, property);
	   } else if (address.getGroup().equals(RuleCapability.NAMESPACE)) {
	   	return getValueFromRuleDefinition(address, property);
	   } else if (address.getGroup().equals(PlaceCapability.NAMESPACE)) {
	   	return getValueFromPlace(address, property);
	   }
   	return "";
   }
   
   private String getValueFromDevice(Address address, String property) {
   	Device device = deviceDao.findById((UUID)address.getId());
   	System.out.println("DEVICE LOOKUP: " + device);
   	return device != null ? deviceGetter.getAs(String.class, property, device) : "";
   }
   
   private String getValueFromPerson(Address address, String property) {
   	Person person = personDao.findById((UUID)address.getId());
   	return person != null ? personGetter.getAs(String.class, property, person) : "";
   }
   
   private String getValueFromPlace(Address address, String property) {
   	Place place = placeDao.findById((UUID)address.getId());
   	return place != null ? placeGetter.getAs(String.class, property, place) : "";
   }
   
   private String getValueFromRuleDefinition(Address address, String property) {
   	PlatformServiceAddress servAddress = (PlatformServiceAddress)address;
   	RuleDefinition ruleDef = ruleDao.findById((UUID)servAddress.getId(), servAddress.getContextQualifier());
   	return ruleDef != null ? ruleGetter.getAs(String.class, property, ruleDef) : "";
   }
}

