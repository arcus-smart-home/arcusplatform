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
package com.iris.platform.address.validation;

import com.google.common.base.Preconditions;
import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;

public interface AddressValidator {

   /**
    * if address is null, use place as the address source otherwise merge place just in case not all values were
    * populated in the json before validating.
    */
   default AddressValidationResult validate(Place place, StreetAddress address) {
      Preconditions.checkNotNull(place, "place cannot be null");
      StreetAddress toValidate = address == null ? new StreetAddress() : address;
      StreetAddress placeAsStreetAddr = StreetAddress.fromPlace(place);
      return validate(toValidate.merge(placeAsStreetAddr));
   }

   AddressValidationResult validate(StreetAddress address);

}

