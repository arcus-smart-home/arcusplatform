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
package com.iris.platform.address;

import com.iris.util.IrisComparator;

import java.util.Comparator;

/**
 * Compares two StreetAddress objects with case insensitivity, null strings are equal to empty strings,
 * and utilizes the ZipComparator for comparing zips for other fields.
 */
public class StreetAddressLenientComparator extends IrisComparator<StreetAddress> {

   private static final ZipStringComparator ZIP_STRING_COMPARATOR = new ZipStringComparator();
   private static final StreetAddressLinesComparator STREET_ADDRESS_LINES_COMPARATOR = new StreetAddressLinesComparator();

   private static final Comparator<StreetAddress> STREET_ADDRESS_COMPARATOR =
         Comparator.comparing(StreetAddress::getCity, NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR)
               .thenComparing(streetAddress-> streetAddress, STREET_ADDRESS_LINES_COMPARATOR)
               .thenComparing(StreetAddress::getState, NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR)
         .thenComparing(StreetAddress::getZip, ZIP_STRING_COMPARATOR);

   public int compare(StreetAddress streetAddress1, StreetAddress streetAddress2) {
      int nullCaseResult = CompareNullCasesNullFirst(streetAddress1, streetAddress2);
      if (nullCaseResult != NULL_CASE_NOT_FOUND) return nullCaseResult;

      return STREET_ADDRESS_COMPARATOR.compare(streetAddress1, streetAddress2);
   }
}

