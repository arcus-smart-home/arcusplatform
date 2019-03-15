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
import java.util.regex.Pattern;

/**
 * Compares zip codes of a StreetAddress object. The zips are considered
 * equal if they are both the same length and match exactly or
 * if they are different lengths and the first five characters match exactly.
 */
public class ZipStringComparator extends IrisComparator<String> {

   private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("[^0-9a-zA-Z]");

   /**
    *
    * @param zip1 target to which zip2 is compared.
    * @param zip2 target to which zip1 is compared.
    * @return 0 if the two zips are the same length and match exactly
    * or are of length 5 and length 9 if the first 5 characters match exactly (case insensitive).
    * String.compare otherwise.
    */
   @Override
   public int compare(String zip1, String zip2) {
      //if any are null, equal length, or not of length 5 or 9,
      //we simply return the null safe, case insensitive value of the comparison.
      if (     zip1 == null
            || zip2 == null) {
         return NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR.compare(zip1, zip2);

      }

      //get rid of any characters which are not 0-9 or A-Z
      //so the lengths are normalized
      zip1 = ALPHANUMERIC_PATTERN.matcher(zip1).replaceAll("");
      zip2 = ALPHANUMERIC_PATTERN.matcher(zip2).replaceAll("");

      //if the zips are equal length or it is not the case where one is length
      //5 and one is length 9, then we just return a case insensitive compare
      if (zip1.length() == zip2.length()
            || (!(zip1.length() == 5 && zip2.length() == 9)
            && !(zip1.length() == 9 && zip2.length() == 5))) {
         return NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR.compare(zip1, zip2);
      }

      //now we have one zip of length 5 and one of length 9, so we just
      //take the left 5 characters and do a case insensitive string comparison
      return NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR.compare(zip1.substring(0, 5), zip2.substring(0, 5));
   }

}

