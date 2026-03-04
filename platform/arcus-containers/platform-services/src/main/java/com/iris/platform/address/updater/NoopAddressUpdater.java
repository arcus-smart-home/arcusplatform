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

import static com.iris.messages.capability.PlaceCapability.ATTR_ADDRVALIDATED;

@Singleton
public class NoopAddressUpdater extends AddressUpdater {

   @Inject
   public NoopAddressUpdater(BeanAttributesTransformer<Place> transformer, LocationService locationService) {
      super(transformer, locationService);
   }

   @Override
   protected Map<String, Object> doUpdate(Place origPlace, StreetAddress newAddress, boolean residential, boolean allowCreate) {
      Map<String, Object> changes = newAddress.toPlaceAttributes();
      changes.put(ATTR_ADDRVALIDATED, false);
      return changes;
   }
}
