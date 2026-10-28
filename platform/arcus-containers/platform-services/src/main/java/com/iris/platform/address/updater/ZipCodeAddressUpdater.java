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

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.location.LocationService;

import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRCOUNTYFIPS;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRRDI;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRTYPE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRVALIDATED;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRZIPTYPE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPCODE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPPLUS4;

/**
 * Saves the address as entered. The base class fills in county, coordinates and time zone from the bundled ZIP code
 * table. Nothing checks that the street address exists, so the place is never marked validated, and the attributes
 * SmartyStreets used to supply are cleared so they don't describe a previous address.
 */
@Singleton
public class ZipCodeAddressUpdater extends AddressUpdater {

   @Inject
   public ZipCodeAddressUpdater(BeanAttributesTransformer<Place> transformer, LocationService locationService) {
      super(transformer, locationService);
   }

   @Override
   protected Map<String, Object> doUpdate(Place origPlace, StreetAddress newAddress, boolean residential, boolean allowCreate) {
      Map<String, Object> changes = newAddress.toPlaceAttributes();
      // A 5-digit ZIP code has no ZIP+4. StreetAddress.fromPlace prefers a stored ZIP+4 over the ZIP code, so leaving
      // the old one would bring the old ZIP code back on the next partial address edit.
      if(changes.containsKey(ATTR_ZIPCODE)) {
         changes.putIfAbsent(ATTR_ZIPPLUS4, "");
      }
      changes.put(ATTR_ADDRVALIDATED, false);
      changes.put(ATTR_ADDRTYPE, "");
      changes.put(ATTR_ADDRZIPTYPE, "");
      changes.put(ATTR_ADDRRDI, "");
      changes.put(ATTR_ADDRCOUNTYFIPS, "");
      return changes;
   }
}
