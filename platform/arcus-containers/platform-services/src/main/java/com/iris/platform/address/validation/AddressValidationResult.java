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

import com.google.common.collect.ImmutableList;
import com.iris.platform.address.StreetAddress;

import java.util.List;

public class AddressValidationResult {

   private List<StreetAddress> suggestions;
   private boolean valid;

   public AddressValidationResult(List<StreetAddress> suggestions, boolean valid) {
      this.suggestions = suggestions == null ? ImmutableList.of() : suggestions;
      this.valid = valid;
   }

   public List<StreetAddress> getSuggestions() {
      return suggestions;
   }

   public boolean isValid() {
      return valid;
   }
}

