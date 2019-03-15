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
package com.iris.video.cql;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.iris.util.IrisUUID;

import junit.framework.AssertionFailedError;

public class UuidAssert {

	public static UUID timeUUID(long ts) {
		return IrisUUID.timeUUID(ts, 0);
	}
	
	public static void assertTimeUUIDEquals(List<UUID> expected, List<UUID> actual) {
		if(!Objects.equals(expected, actual)) {
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
			StringBuilder sb = new StringBuilder("Did not match\n");
			int length = Math.max(expected.size(), actual.size());
			for(int i=0; i<length; i++) {
				UUID expectedUUID = i < expected.size() ? expected.get(i) : null;
				UUID actualUUID   = i < actual.size()   ? actual.get(i)   : null;
				sb
					.append(String.format("%02d ", i))
					.append(expectedUUID == null ? "<missing>" : (sdf.format(new Date(IrisUUID.timeof(expectedUUID))) + " (" + expectedUUID + ")"))
					.append("\n")
					.append(String.format("%02d ", i))
					.append(actualUUID == null ? "<missing>" : (sdf.format(new Date(IrisUUID.timeof(actualUUID))) + " (" + actualUUID + ")"))
					.append("\n")
					;
			}
			throw new AssertionFailedError(sb.toString());
		}
		
	}


}

