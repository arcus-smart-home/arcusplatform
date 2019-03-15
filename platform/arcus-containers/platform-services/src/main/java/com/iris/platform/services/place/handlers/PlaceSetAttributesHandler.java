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
package com.iris.platform.services.place.handlers;

import static com.google.common.collect.Sets.union;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRCOUNTYFIPS;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRGEOPRECISION;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRLATITUDE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRLONGITUDE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRRDI;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRTYPE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRVALIDATED;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRZIPTYPE;
import static com.iris.messages.capability.PlaceCapability.ATTR_CITY;
import static com.iris.messages.capability.PlaceCapability.ATTR_COUNTRY;
import static com.iris.messages.capability.PlaceCapability.ATTR_STATE;
import static com.iris.messages.capability.PlaceCapability.ATTR_STATEPROV;
import static com.iris.messages.capability.PlaceCapability.ATTR_STREETADDRESS1;
import static com.iris.messages.capability.PlaceCapability.ATTR_STREETADDRESS2;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZID;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZNAME;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZOFFSET;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZUSESDST;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPCODE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPPLUS4;
import static com.iris.messages.model.Place.GEOPRECISION_UNKNOWN;
import static org.apache.commons.collections.CollectionUtils.containsAny;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.address.updater.AddressUpdater;
import com.iris.platform.address.updater.AddressUpdaterFactory;
import com.iris.platform.location.TimezonesChangeHelper;
import com.iris.platform.location.TimezonesManager;
import com.iris.platform.services.AbstractSetAttributesPlatformMessageHandler;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.TimeZones;

@Singleton
public class PlaceSetAttributesHandler extends AbstractSetAttributesPlatformMessageHandler<Place>
{
   private static final Logger logger = getLogger(PlaceSetAttributesHandler.class);

   private static final Set<String> ADDRESS_ATTRIBUTES = ImmutableSet.of(
      ATTR_STREETADDRESS1,
      ATTR_STREETADDRESS2,
      ATTR_CITY,
      ATTR_STATE,
      ATTR_ZIPCODE);

   // For backwards compatibility after ITWO-8686
   private static final Set<String> WRITE_HONORED_ATTRIBUTES = ADDRESS_ATTRIBUTES;

   // For backwards compatibility after ITWO-8686
   private static final Set<String> WRITE_IGNORED_ATTRIBUTES = ImmutableSet.of(
      ATTR_TZNAME,
      ATTR_TZOFFSET,
      ATTR_TZUSESDST,
      ATTR_STATEPROV,
      ATTR_ZIPPLUS4,
      ATTR_COUNTRY,
      ATTR_ADDRVALIDATED,
      ATTR_ADDRTYPE,
      ATTR_ADDRZIPTYPE,
      ATTR_ADDRRDI,
      ATTR_ADDRCOUNTYFIPS,
      ATTR_ADDRGEOPRECISION);

   // For backwards compatibility after ITWO-8686
   private static final Set<String> WRITE_ALLOWED_ATTRIBUTES = union(
      WRITE_HONORED_ATTRIBUTES,
      WRITE_IGNORED_ATTRIBUTES);

   private final AddressUpdaterFactory updaterFactory;
   private final PlaceDAO placeDao;
   private final TimezonesChangeHelper timezoneHelper;

   @Inject
   public PlaceSetAttributesHandler(CapabilityRegistry capabilityRegistry,
      BeanAttributesTransformer<Place> placeTransformer, PlatformMessageBus platformBus,
      AddressUpdaterFactory updaterFactory, PlaceDAO placeDao, TimezonesManager timezonesMgr,
      PlacePopulationCacheManager populationCacheMgr)
   {
      super(capabilityRegistry, placeTransformer, platformBus, populationCacheMgr);

      this.updaterFactory = updaterFactory;
      this.placeDao = placeDao;
      this.timezoneHelper = new TimezonesChangeHelper(timezonesMgr);
   }

   /*
    * For backwards compatibility, allow writes to certain attributes that were changed to read-only in ITWO-8686.
    */
   @Override
   protected ErrorEvent validateSettableAttributes(Map<String, Object> attributes)
   {
      Map<String, Object> attributesToValidate = new HashMap<>(attributes);

      attributesToValidate.keySet().removeAll(WRITE_ALLOWED_ATTRIBUTES);

      return super.validateSettableAttributes(attributesToValidate);
   }

   @Override
   protected void beforeSave(Place context, Map<String, Object> changes)
   {
      super.beforeSave(context, changes);
      
      if (changes.containsKey(ATTR_TZNAME) && !changes.containsKey(ATTR_TZID)) {
         
         /* Adjust for android 1.16 apps that set tzId in the tzName field
          * swap out. We don't need ATTR_TZNAME because it is stripped out anyway
          * for the purposes of this action.
          */
        String tzName = changes.get(ATTR_TZNAME).toString();
        
        Optional<TimeZone> tzOpt = TimeZones.getTimeZoneById(tzName);
        if (tzOpt.isPresent()) { 
           logger.debug("Legacy update to move timezone name to timezone id [{}]", tzName);
           changes.put(ATTR_TZID, changes.get(ATTR_TZNAME));
           changes.remove(ATTR_TZNAME);
        }
        else {
            logger.debug("Ignoring request to set unrecognized timezone [{}]", tzName);
        }
      }

      changes.keySet().removeAll(WRITE_IGNORED_ATTRIBUTES);

      /*
       * The processing order here is intentional.  Address changes must be processed last, since AddressUpdater will
       * use zip code to derive time zone and geolocation attributes, which should only be added to {@code changes} if
       * they are absent.  This ensures that derived changes don't overwrite requested changes.  It also prevents
       * problems while processing time zone and geolocation changes due to not knowing if they were requested or
       * derived.
       */

      processTimeZoneChanges(changes);

      processGeoLocationChanges(context, changes);

      processAddressChanges(context, changes);
   }
   
   private void processTimeZoneChanges(Map<String, Object> changes)
   {
      timezoneHelper.processTimeZoneChanges(changes);
   }

   private void processGeoLocationChanges(Place place, Map<String, Object> changes)
   {
      if (changes.containsKey(ATTR_ADDRLATITUDE) || changes.containsKey(ATTR_ADDRLONGITUDE))
      {
         logger.debug("User request to update gelocation to [lat:{} long:{}]", changes.get(ATTR_ADDRLATITUDE), changes.get(ATTR_ADDRLONGITUDE));
         if(isZed(changes.get(ATTR_ADDRLATITUDE), place.getAddrLatitude()) && isZed(changes.get(ATTR_ADDRLONGITUDE), place.getAddrLongitude())) {
            logger.debug("Ignoring 0.0 by 0.0 geolocation update for place [{}]", place.getId());
            changes.remove(ATTR_ADDRLATITUDE);
            changes.remove(ATTR_ADDRLONGITUDE);
            changes.remove(ATTR_ADDRGEOPRECISION);
         }
         else {
            changes.put(ATTR_ADDRGEOPRECISION, GEOPRECISION_UNKNOWN);
         }
      }
   }

   private void processAddressChanges(Place context, Map<String, Object> changes)
   {
      if (containsAny(changes.keySet(), ADDRESS_ATTRIBUTES))
      {
         // [ITWO-8686] If streetAddress1 changed and streetAddress2 did not, clear streetAddress2
         if (changes.containsKey(ATTR_STREETADDRESS1) && !changes.containsKey(ATTR_STREETADDRESS2))
         {
            changes.put(ATTR_STREETADDRESS2, "");
         }

         AddressUpdater updater = updaterFactory.updaterFor(context);

         StreetAddress changedAddress = StreetAddress.fromPlaceAttributes(changes)
            .merge(StreetAddress.fromPlace(context));

         logger.debug("Processing legacy address change to [{}]", changedAddress);
         Map<String, Object> derivedAddressChanges = updater.updateAddress(context, changedAddress);

         derivedAddressChanges.entrySet().stream().forEach(e -> changes.putIfAbsent(e.getKey(), e.getValue()));
      }
   }

   private boolean isZed(Object requestedTude, Double currentTude) {
      if(requestedTude == null) {
         requestedTude = currentTude;
      }
      if(requestedTude instanceof String) {
         try {
            requestedTude = Double.valueOf((String) requestedTude);
         }
         catch(NumberFormatException e) {
            logger.warn("Received non-numeric geocoordinates: {}", requestedTude);
            return true;
         }
      }
      if(!(requestedTude instanceof Number)) {
         logger.warn("Received non-numeric geocoordinates: {}", requestedTude);
         return true;
      }
      double tude = ((Number) requestedTude).doubleValue();
      return -0.01 < tude && tude < 0.01;
   }

   @Override
   protected void save(Place bean)
   {
      placeDao.save(bean);
   }
}

