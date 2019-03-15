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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.address.validation.smartystreets.SmartyStreetsValidator;

@Singleton
public class AddressValidatorFactory {

   private final SmartyStreetsValidator smartyStreetsValidator;

   @Inject
   public AddressValidatorFactory(SmartyStreetsValidator smartyStreetsValidator) {
      this.smartyStreetsValidator = smartyStreetsValidator;
   }

   public AddressValidator validatorFor(Place place) {
      Preconditions.checkNotNull(place);
      return smartyStreetsValidator;
   }

   public AddressValidator defaultValidator() {
      return smartyStreetsValidator;
   }

}

