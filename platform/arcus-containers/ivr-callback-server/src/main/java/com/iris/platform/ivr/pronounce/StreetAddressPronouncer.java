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
package com.iris.platform.ivr.pronounce;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.location.UspsDataService;

@Singleton
public class StreetAddressPronouncer
{
   private final AddressLinePronouncer linePronouncer;
   private final StatePronouncer statePronouncer;

   @Inject
   public StreetAddressPronouncer(UspsDataService uspsDataService)
   {
      linePronouncer = new AddressLinePronouncer(uspsDataService);

      statePronouncer = new StatePronouncer(uspsDataService);
   }

   public StreetAddress pronounceFor(Place place)
   {
      StreetAddress address = new StreetAddress();

      address.setLine1(linePronouncer.pronounce(place.getStreetAddress1()));

      address.setLine2(linePronouncer.pronounce(place.getStreetAddress2()));

      address.setState(statePronouncer.pronounce(place.getState()));

      return address;
   }
}

