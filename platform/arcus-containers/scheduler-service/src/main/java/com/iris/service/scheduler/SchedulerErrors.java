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
/**
 * 
 */
package com.iris.service.scheduler;

import com.iris.messages.ErrorEvent;
import com.iris.messages.errors.Errors;

/**
 * 
 */
public class SchedulerErrors {
   public static final String CODE_TYPE_CONFLICT = "scheduler.type_conflict";

   public static ErrorEvent typeConflict() {
      return Errors.fromCode(CODE_TYPE_CONFLICT, "Schedule with the same id of another type already exists");
   }

}

