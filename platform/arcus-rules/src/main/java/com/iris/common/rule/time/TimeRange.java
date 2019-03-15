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
package com.iris.common.rule.time;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;

@SuppressWarnings("serial")
public class TimeRange implements Serializable {
   private static final Pattern PATTERN = Pattern.compile("^\\s*(\\d{1,2}\\:\\d{2}:\\d{2})?\\s*-\\s*(\\d{1,2}\\:\\d{2}:\\d{2})?\\s*$");
   
   public static TimeRange fromString(String s) {
      Matcher m = PATTERN.matcher(s);
      Preconditions.checkArgument(m.matches(), "Invalid time range [" + s + "]");
      String startString = m.group(1);
      String endString = m.group(2);
      return new TimeRange(startString != null ? TimeOfDay.fromString(startString) : null,
            endString != null ? TimeOfDay.fromString(endString) : null);
   }
   
   private final TimeOfDay start;
   private final TimeOfDay end;
   
   
   public TimeRange(@Nullable TimeOfDay start, @Nullable TimeOfDay end) {
      Preconditions.checkArgument(start != null || end != null, "must specify at least one of startTime or endTime");
      Preconditions.checkArgument((start == null || end == null) || start.isBefore(end), "startTime must be before endTime");
      this.start = start;
      this.end = end;
   }

   public TimeOfDay getStart() {
      return start;
   }

   public TimeOfDay getEnd() {
      return end;
   }
   
   public String getRepresentation() {
      return (start != null ? start.toString() : "") + "-" + (end != null ? end.toString() : ""); 
   }

   @Override
   public String toString() {
      return getRepresentation();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((end == null) ? 0 : end.hashCode());
      result = prime * result + ((start == null) ? 0 : start.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      TimeRange other = (TimeRange) obj;
      if (end == null) {
         if (other.end != null)
            return false;
      } else if (!end.equals(other.end))
         return false;
      if (start == null) {
         if (other.start != null)
            return false;
      } else if (!start.equals(other.start))
         return false;
      return true;
   }
}

