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
package com.iris.common.subsystem.care;

import com.iris.messages.ErrorEvent;
import com.iris.messages.errors.Errors;

public class CareErrors {
   public static final String CODE_NAME_IN_USE = "care.name_in_use";
   public static final String CODE_WINDOW_DURATION_TOO_SHORT = "care.window_duration_too_short";
   public static final String CODE_DUPLICATE_OR_OVERLAPPING_WINDOWS = "care.duplicate_windows";

   public static ErrorEvent duplicateName(String name) {
      return Errors.fromCode(CODE_NAME_IN_USE, String.format("Behavior name %s already exists",name));
   }
   public static ErrorEvent durationLongerThanActivityWindow(String window,int duration) {
      return Errors.fromCode(CODE_WINDOW_DURATION_TOO_SHORT, String.format("Window %s is shorter than duration %d secs",window,duration));
   }
   public static ErrorEvent duplicateTimeWindows(String window) {
      return Errors.fromCode(CODE_DUPLICATE_OR_OVERLAPPING_WINDOWS, String.format("Duplicate or overlapping windows for Window %s",window));
   }

}

