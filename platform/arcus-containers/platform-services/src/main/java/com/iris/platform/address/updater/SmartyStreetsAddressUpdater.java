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

import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRCOUNTYFIPS;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRGEOPRECISION;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRLATITUDE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRLONGITUDE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRRDI;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRTYPE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRVALIDATED;
import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRZIPTYPE;
import static com.iris.messages.capability.PlaceCapability.ATTR_COUNTRY;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPPLUS4;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.iris.platform.address.StreetAddressLenientComparator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.address.validation.smartystreets.DetailedStreetAddress;
import com.iris.platform.address.validation.smartystreets.SmartyStreetsClient;
import com.iris.platform.location.LocationService;

@Singleton
public class SmartyStreetsAddressUpdater extends AddressUpdater
{
   private final SmartyStreetsClient smartyStreetsClient;
   private final PlatformMessageBus platformMessageBus;

   @Inject
   public SmartyStreetsAddressUpdater(BeanAttributesTransformer<Place> transformer, LocationService locationService,
      SmartyStreetsClient client, PlatformMessageBus platformMessageBus)
   {
      super(transformer, locationService);

      this.smartyStreetsClient = client;
      this.platformMessageBus = platformMessageBus;
   }

   @Override
   protected Map<String, Object> doUpdate(Place origPlace, StreetAddress newAddress, boolean allowCreate,
      boolean residential)
   {
      Map<String, Object> changes = newAddress.toPlaceAttributes();

      List<DetailedStreetAddress> detailedSuggestedAddresses = smartyStreetsClient.getDetailedSuggestions(newAddress);

      StreetAddressLenientComparator streetAddressLenientComparator = new StreetAddressLenientComparator();

      Optional<DetailedStreetAddress> matchOpt = detailedSuggestedAddresses.stream()
         .filter(detailedSuggestedAddress ->
               streetAddressLenientComparator.compare(detailedSuggestedAddress, newAddress) == 0
               ).findFirst();

      if (matchOpt.isPresent())
      {
         DetailedStreetAddress match = matchOpt.get();

         changes.put(ATTR_ZIPPLUS4,         match.getZipPlus4());
         changes.put(ATTR_COUNTRY,          match.getCountry());
         changes.put(ATTR_ADDRVALIDATED,    true);
         changes.put(ATTR_ADDRTYPE,         match.getAddrType());
         changes.put(ATTR_ADDRZIPTYPE,      match.getAddrZipType());
         changes.put(ATTR_ADDRRDI,          match.getAddrRDI());
         changes.put(ATTR_ADDRCOUNTYFIPS,   match.getAddrCountyFIPS());
         changes.put(ATTR_ADDRLATITUDE,     match.getAddrLatitude());
         changes.put(ATTR_ADDRLONGITUDE,    match.getAddrLongitude());
         changes.put(ATTR_ADDRGEOPRECISION, match.getAddrGeoPrecision());
      }
      else
      {
         changes.put(ATTR_ZIPPLUS4,       "");
         changes.put(ATTR_COUNTRY,        "");
         changes.put(ATTR_ADDRVALIDATED,  false);
         changes.put(ATTR_ADDRTYPE,       "");
         changes.put(ATTR_ADDRZIPTYPE,    "");
         changes.put(ATTR_ADDRRDI,        "");
         changes.put(ATTR_ADDRCOUNTYFIPS, "");
         // Don't clear geolocation.  That that would break scheduling at this Place.
      }

      return changes;
   }
}

