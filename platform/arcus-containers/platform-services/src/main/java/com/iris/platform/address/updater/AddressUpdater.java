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
package com.iris.platform.address.updater;

import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRCOUNTY;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRGEOPRECISION;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRLATITUDE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRLONGITUDE;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZID;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZNAME;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZOFFSET;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZUSESDST;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPCODE;
import static com.iris.messages.model.Place.GEOPRECISION_ZIP5;
import static com.iris.util.TimeZones.getOffsetAsHours;

import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.location.LocationService;
import com.iris.platform.location.PlaceLocation;
import com.iris.util.MapUtil;

public abstract class AddressUpdater {

   private static final Logger logger = LoggerFactory.getLogger(AddressUpdater.class);

   private final BeanAttributesTransformer<Place> transformer;
   private final LocationService locationService;

   protected AddressUpdater(
         BeanAttributesTransformer<Place> transformer,
         LocationService locationService
   ) {
      this.transformer = transformer;
      this.locationService = locationService;
   }

   public Map<String,Object> updateAddress(Place origPlace, StreetAddress newAddress) {
      return updateAddress(origPlace, newAddress, true, false);
   }

   public Map<String,Object> updateAddress(Place origPlace, StreetAddress newAddress, boolean residential) {
      return updateAddress(origPlace, newAddress, residential, false);
   }

   public Map<String, Object> updateAddress(Place origPlace, StreetAddress newAddress, boolean residential, boolean allowCreate) {
      if(newAddress == null) {
         newAddress = new StreetAddress();
      } else if (newAddress.getLine2() == null) {
         //wds - added to handle case when address line 2 needs to be blanked, but applications are sending a null value
         //instead. this will cause the update to consider this in the change set. see: https://eyeris.atlassian.net/browse/ITWO-11143.
         newAddress.setLine2("");
      }

      Map<String, Object> addressOnlyChanges = doUpdate(origPlace.copy(), newAddress.copy(), residential, allowCreate);
      if(addressOnlyChanges != null && !addressOnlyChanges.isEmpty()) {
         updateLocation(addressOnlyChanges);
         addressOnlyChanges = MapUtil.filterChanges(transformer.transform(origPlace), addressOnlyChanges);
      }
      return addressOnlyChanges;
   }

   protected abstract Map<String, Object> doUpdate(Place origPlace, StreetAddress newAddress, boolean residential, boolean allowCreate);

   private void updateLocation(Map<String,Object> pendingPlaceChanges) {

      if(!pendingPlaceChanges.containsKey(ATTR_ZIPCODE)) {
         return;
      }

      String zipCode = (String) pendingPlaceChanges.get(ATTR_ZIPCODE);

      Optional<PlaceLocation> placeLocationOpt = locationService.getForZipCode(zipCode);

      if(placeLocationOpt.isPresent()) {

         PlaceLocation placeLocation = placeLocationOpt.get();

         logger.debug("Updating place location to [{}] based on zip [{}]", placeLocation, zipCode);

         pendingPlaceChanges.put(ATTR_ADDRCOUNTY, placeLocation.getCounty());

         // Put geolocation attributes only if absent, to allow subclasses to set them to a more precise geolocation if
         // they can (e.g. ZIP9 precision instead of ZIP5).
         pendingPlaceChanges.putIfAbsent(ATTR_ADDRLATITUDE,     placeLocation.getGeoLocation().getLatitude());
         pendingPlaceChanges.putIfAbsent(ATTR_ADDRLONGITUDE,    placeLocation.getGeoLocation().getLongitude());
         pendingPlaceChanges.putIfAbsent(ATTR_ADDRGEOPRECISION, StringUtils.isEmpty(placeLocation.getGeoPrecision()) ? Place.GEOPRECISION_UNKNOWN : placeLocation.getGeoPrecision());

         TimeZone timeZone = placeLocation.getTimeZone();
         pendingPlaceChanges.put(ATTR_TZID,      timeZone.getID());
         pendingPlaceChanges.put(ATTR_TZNAME,    timeZone.getDisplayName());
         pendingPlaceChanges.put(ATTR_TZOFFSET,  getOffsetAsHours(timeZone.getRawOffset()));
         pendingPlaceChanges.put(ATTR_TZUSESDST, timeZone.observesDaylightTime());
      }
      else {

         logger.debug("Unable to find zip [{}]; clearing County, won't update other place location fields", zipCode);

         pendingPlaceChanges.put(ATTR_ADDRCOUNTY, "");

         // Don't clear geolocation or timezone.  That that would break scheduling at this Place.
      }
   }

}

