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
package com.iris.platform.address.validation.smartystreets;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.address.StreetAddressLenientComparator;
import com.iris.platform.address.validation.AddressValidationResult;
import com.iris.platform.address.validation.AddressValidator;

@Singleton
public class SmartyStreetsValidator implements AddressValidator
{
   private final SmartyStreetsClient client;

   @Inject
   public SmartyStreetsValidator(SmartyStreetsClient client)
   {
      this.client = client;
   }

   @Override
   public AddressValidationResult validate(StreetAddress address)
   {
      List<StreetAddress> suggestedAddresses = client.getSuggestions(address);
      StreetAddressLenientComparator lenientComparator = new StreetAddressLenientComparator();

      boolean valid = suggestedAddresses
            .stream()
            .anyMatch(suggestedAddress -> lenientComparator.compare(suggestedAddress, address) == 0);

      return new AddressValidationResult(suggestedAddresses, valid);
   }
}

