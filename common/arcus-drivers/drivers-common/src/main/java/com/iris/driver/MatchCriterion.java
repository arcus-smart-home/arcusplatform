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
package com.iris.driver;

import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * One test from a driver's {@code matcher} line, kept so the driver can say which devices
 * it matches. A device matches a line when every criterion on it holds, and matches the
 * driver when any of its lines does.
 */
public final class MatchCriterion {

   public enum Comparison {
      /** The attribute equals the value. */
      EQUALS,
      /** The attribute contains a match for the regular expression in the value. */
      REGEX,
      /** The attribute equals one of the values. */
      IN,
      /** The attribute is a set that contains the value. */
      CONTAINS
   }

   private final String attribute;
   private final Comparison comparison;
   private final String value;

   public MatchCriterion(String attribute, Comparison comparison, String value) {
      this.attribute = Preconditions.checkNotNull(attribute, "attribute");
      this.comparison = Preconditions.checkNotNull(comparison, "comparison");
      this.value = Preconditions.checkNotNull(value, "value");
   }

   public String getAttribute() {
      return attribute;
   }

   public Comparison getComparison() {
      return comparison;
   }

   public String getValue() {
      return value;
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) {
         return true;
      }
      if(!(o instanceof MatchCriterion)) {
         return false;
      }
      MatchCriterion other = (MatchCriterion) o;
      return attribute.equals(other.attribute) && comparison == other.comparison && value.equals(other.value);
   }

   @Override
   public int hashCode() {
      return Objects.hash(attribute, comparison, value);
   }

   @Override
   public String toString() {
      return attribute + " " + comparison + " " + value;
   }
}
