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
package com.iris.driver.groovy;

import com.google.common.base.Predicate;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.MatchCriterion;
import com.iris.driver.MatchCriterion.Comparison;

/**
 * A test on one device attribute, built by {@link DriverMatchers} together with a description
 * of what it tests, so the two can't disagree.
 */
public final class AttributeMatcher implements Predicate<AttributeMap> {
   private final Predicate<AttributeMap> predicate;
   private final MatchCriterion criterion;

   AttributeMatcher(AttributeKey<?> key, Comparison comparison, Object value, Predicate<AttributeMap> predicate) {
      this.predicate = predicate;
      this.criterion = new MatchCriterion(key.getName(), comparison, String.valueOf(value));
   }

   public MatchCriterion getCriterion() {
      return criterion;
   }

   @Override
   public boolean apply(AttributeMap input) {
      return predicate.apply(input);
   }

   @Override
   public String toString() {
      return criterion.toString();
   }
}
