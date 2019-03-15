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
package com.iris.platform.history.appender;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.platform.history.HistoryAppenderConfig;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.SceneDefinition;

/**
 *
 */
// TODO this is a more generic utility
// TODO only cache the attributes we're interested in
@Singleton
public class ObjectNameCache {
   private static final Logger logger = LoggerFactory.getLogger(ObjectNameCache.class);

   public static final String UNKNOWN_PLACE_NAME  = "UNKNOWN_PLACE";
   public static final String UNKNOWN_DEVICE_NAME = "UNKNOWN_DEVICE";
   public static final String UNKNOWN_HUB_NAME    = "UNKNOWN_HUB";
   public static final String UNKNOWN_PERSON_NAME = "Someone";
   public static final String UNKNOWN_RULE_NAME   = "UNKNOWN_RULE";
   public static final String UNKNOWN_SCENE_NAME  = "UNKNOWN_SCENE";

   private final Cache<Address, String> names;

   private final PlaceDAO placeDao;
   private final DeviceDAO deviceDao;
   private final HubDAO hubDao;
   private final PersonDAO personDao;
   private final RuleDao ruleDao;
   private final SceneDao sceneDao;

   /**
    *
    */
   @Inject
   public ObjectNameCache(
         PlaceDAO placeDao,
         DeviceDAO deviceDao,
         HubDAO hubDao,
         PersonDAO personDao,
         RuleDao ruleDao,
         HistoryAppenderConfig config,
         SceneDao sceneDao
   ) {
      names =
            CacheBuilder
               .newBuilder()
               .concurrencyLevel(config.getNameCacheConcurrency())
               .maximumSize(config.getNameCacheMaxSize())
               .expireAfterWrite(config.getNameCacheExpireMinutes(), TimeUnit.MINUTES)
               .build();
      this.placeDao = placeDao;
      this.deviceDao = deviceDao;
      this.hubDao = hubDao;
      this.personDao = personDao;
      this.ruleDao = ruleDao;
      this.sceneDao = sceneDao;
   }

   public void update(PlatformMessage message) {
      if(Capability.EVENT_ADDED.equals(message.getMessageType()) || Capability.EVENT_VALUE_CHANGE.equals(message.getMessageType())) {
         String type = (String) message.getSource().getGroup();
         switch(type) {
         case PlaceCapability.NAMESPACE:
            updateIf(message.getSource(), PlaceCapability.getName(message.getValue()));
            break;
         case DeviceCapability.NAMESPACE:
            updateIf(message.getSource(), DeviceCapability.getName(message.getValue()));
            break;
         case HubCapability.NAMESPACE:
             updateIf(message.getSource(), HubCapability.getName(message.getValue()));
             break;
         case PersonCapability.NAMESPACE:
             updateIf(message.getSource(), PersonCapability.getFirstName(message.getValue()));
             break;
         case RuleCapability.NAMESPACE:
            updateIf(message.getSource(), RuleCapability.getName(message.getValue()));
            break;
/*
 *        @TODO - This will be a future feature. For now we will leave it as a placeholder   
 *        case SceneCapability.NAMESPACE:
 *            updateIf(message.getSource(), SceneCapability.getName(message.getValue()));
 *             break;   
 *      	 
*/
         }
      }
      else if(Capability.EVENT_DELETED.equals(message.getMessageType())) {
         names.invalidate(message.getSource());
      }
   }

   public String getPlaceName(Address place) {
      try {
         return names.get(place, () -> loadPlace(place));
      }
      catch (ExecutionException|UncheckedExecutionException e) {
         logger.warn("Unable to determine place name", e);
         return UNKNOWN_PLACE_NAME;
      }
   }

   public String getDeviceName(Address deviceAddress) {
      try {
         return names.get(deviceAddress, () -> loadDevice(deviceAddress));
      }
      catch (ExecutionException|UncheckedExecutionException e) {
         logger.warn("Unable to determine device name", e);
         return UNKNOWN_DEVICE_NAME;
      }
   }
   
   public String getHubName(Address hubAddress) {
	   try {
	         return names.get(hubAddress, () -> loadHub(hubAddress));
	      }
	      catch (ExecutionException|UncheckedExecutionException e) {
	         logger.warn("Unable to determine hub name", e);
	         return UNKNOWN_HUB_NAME;
	      }
	   }

   public String getPersonName(Address personAddress) {
	   try {
		   return names.get(personAddress,  () -> loadPerson(personAddress));
	   } catch (ExecutionException|UncheckedExecutionException e) {
	         logger.warn("Unable to determine person name", e);
	         return UNKNOWN_PERSON_NAME;
	   }
   }
   
   public String getRuleName(Address ruleAddress) {
	   try {
		   return names.get(ruleAddress, () -> loadRule(ruleAddress));
	   } catch (ExecutionException|UncheckedExecutionException e) {
	         logger.warn("Unable to determine rule name", e);
	         return UNKNOWN_RULE_NAME;
	   }
   }

   public String getSceneName(Address sceneAddress) {
	   try {
		   return names.get(sceneAddress, () -> loadScene(sceneAddress));
	   } catch (ExecutionException|UncheckedExecutionException e) {
	         logger.warn("Unable to determine scene name", e);
	         return UNKNOWN_SCENE_NAME;
	   }
   }
   
   public String getName(Address address) {
	   if (address.getGroup().equals(DeviceCapability.NAMESPACE)) {
		   return getDeviceName(address);
	   } else if (address.getGroup().equals(HubCapability.NAMESPACE) || address.isHubAddress()) {
		   return getHubName(address);		   
	   } else if (address.getGroup().equals(PersonCapability.NAMESPACE)) {
		   return getPersonName(address);
	   } else if (address.getGroup().equals(RuleCapability.NAMESPACE)) {
		   return getRuleName(address);
	   } else if (address.getGroup().equals(SceneCapability.NAMESPACE)) {
			   return getSceneName(address);
	   }
	   return "";
   }

   protected String loadRule(Address ruleAddress) {
	   PlatformServiceAddress address = (PlatformServiceAddress)ruleAddress;

	   RuleDefinition rule = ruleDao.findById((UUID) address.getId(), address.getContextQualifier());
	   if (rule == null) {
		   throw new NotFoundException(ruleAddress);
	   }
	   return rule.getName();
   }

   protected String loadScene(Address sceneAddress) {
	   PlatformServiceAddress address = (PlatformServiceAddress)sceneAddress;

	   SceneDefinition scene = sceneDao.findById((UUID) address.getId(), address.getContextQualifier());
	   if (scene == null) {
		   throw new NotFoundException(sceneAddress);
	   }
	   return scene.getName();
   }
   
   protected String loadPerson(Address personAddress) {
	   Person person = personDao.findById((UUID) personAddress.getId());
	   if (person == null) {
		   throw new NotFoundException(personAddress);
	   }
	   return person.getFirstName();
   }
   
   protected String loadPlace(Address placeAddress) {
      Place place = placeDao.findById((UUID) placeAddress.getId());
      if(place == null) {
         throw new NotFoundException(placeAddress);
      }
      return place.getName();
   }

   protected String loadDevice(Address deviceAddress) {
      Device device = deviceDao.findById((UUID) deviceAddress.getId());
      if(device == null) {
         throw new NotFoundException(deviceAddress);
      }
      return device.getName();
   }
   
   protected String loadHub(Address hubAddress) {
	   Hub hub = hubDao.findById(hubAddress.getHubId());
	   if(hub == null) {
		   throw new NotFoundException(hubAddress);
	   }
	   return hub.getName();
   }

   private void updateIf(Address source, String name) {
      if(name == null) {
         return;
      }
      names.put(source, name);
   }

}

