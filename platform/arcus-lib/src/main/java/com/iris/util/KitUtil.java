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
package com.iris.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class KitUtil {
	public static String zigbeeIdToProtocolId(String value) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(Long.parseUnsignedLong(value, 16));
		return new String(Base64.getEncoder().encode(buffer.array()));
	}

	// TODO Move this to a configuration file?
	private static final Map<String,String> kitIdToProductId = new ImmutableMap.Builder<String,String>()
			.put("ZB_GreatStar_MotionSensor",  "432011")
			.put("ZB_GreatStar_ContactSensor", "432021")
			.put("ZB_GreatStar_KeyPad",        "432031")
			.put("ZB_GreatStar_SmartPlug",     "432041").build();

	public static String getProductId(String kitName) {
		return kitIdToProductId.getOrDefault(kitName, "unknown");
	}

}

