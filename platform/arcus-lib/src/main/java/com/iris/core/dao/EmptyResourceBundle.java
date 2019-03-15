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
package com.iris.core.dao;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * This class is useful to bind in test cases to prevent resource not
 * found exceptions.
 */
public class EmptyResourceBundle implements ResourceBundleDAO {
   
   private final Map<String, String> resources;
   
   public EmptyResourceBundle() {
      this(null);
   }
   
   public EmptyResourceBundle(Map<String, String> resources) {
      this.resources = resources != null ? Collections.unmodifiableMap(resources) : Collections.emptyMap();
   }

   @Override
   public Map<String, String> loadBundle(String bundleName, Locale locale) {
      return resources;
   }

   @Override
   public void saveBundle(String bundleName, Locale locale, Map<String, String> localizedValues) {
      // Doesn't do anything.
   }

}

