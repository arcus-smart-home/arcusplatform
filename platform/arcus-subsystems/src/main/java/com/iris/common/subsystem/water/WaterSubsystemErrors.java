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
package com.iris.common.subsystem.water;

import com.iris.messages.ErrorEvent;
import com.iris.messages.errors.Errors;

public class WaterSubsystemErrors {
	public static final String CODE_NOT_WATERHEATER = "water.not_waterheater";
	   public static final String CODE_NOT_WATERSOFTENER = "water.not_watersoftener";
	   
	   public static ErrorEvent notWaterHeater(String address) {
	      return Errors.fromCode(CODE_NOT_WATERHEATER, "Device at address '" + address + "' is not a water heater");
	   }

	   public static ErrorEvent notWaterSoftener(String address) {
	      return Errors.fromCode(CODE_NOT_WATERSOFTENER, "Device at address '" + address + "' is not a water softener");
	   }

	   

}

