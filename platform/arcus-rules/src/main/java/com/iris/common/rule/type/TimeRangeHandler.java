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
package com.iris.common.rule.type;

import com.iris.common.rule.time.TimeRange;
import com.iris.type.handler.TypeHandlerImpl;

@SuppressWarnings("serial")
public class TimeRangeHandler extends TypeHandlerImpl<TimeRange> {

   public TimeRangeHandler() {
      super(TimeRange.class, String.class);
   }

   @Override
   protected TimeRange convert(Object value) {
      return TimeRange.fromString((String) value);
   }

}

