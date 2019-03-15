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

import org.apache.commons.lang3.StringUtils;

import com.iris.util.IrisComparator;

/**
 * Created by wesleystueve on 4/24/17.
 */
public class StreetAddressLinesComparator extends IrisComparator<StreetAddress> {
   @Override
   public int compare(StreetAddress streetAddress1, StreetAddress streetAddress2) {
      int nullCaseResult = CompareNullCasesNullFirst(streetAddress1, streetAddress2);
      if (nullCaseResult != NULL_CASE_NOT_FOUND) return nullCaseResult;

      int line1Result = NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR.compare(streetAddress1.getLine1(), streetAddress2.getLine1());
      if (line1Result == 0) return line1Result;

      int concatResult = compareConcatAddressLines(streetAddress1, streetAddress2);
      if (concatResult == 0) return concatResult;

      concatResult = compareConcatAddressLines(streetAddress2, streetAddress1);
      if (concatResult == 0) return concatResult;

      return line1Result;
   }

   private int compareConcatAddressLines(StreetAddress streetAddress1, StreetAddress streetAddress2) {
      String line1PlusLine2 = StringUtils.defaultString(streetAddress1.getLine1()) + " " + StringUtils.defaultString(streetAddress1.getLine2());
      return NULL_EQUALS_EMPTY_STRING_CASE_INSENSITIVE_COMPARATOR.compare(line1PlusLine2, streetAddress2.getLine1());
   }
}

