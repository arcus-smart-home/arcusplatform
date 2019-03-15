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
package com.iris.platform.history.appender.matcher;

import com.iris.messages.MessageBody;

public class EnumValueChangeMatcher implements Matcher {
	private final String attribBase;
	private final String attribPrefix;
	private final String[] attribValues;

   public EnumValueChangeMatcher(String attribBase, String... attribValues) {
      this.attribBase = attribBase;
      this.attribPrefix = attribBase + ":";
      this.attribValues = attribValues;
   }
	
	@Override
	public MatchResults matches(MessageBody value) {
		
		// search for attribute in keys in order to match multi-instance values
		String attribName = null;
		Object attribValue = null;
		for (String attrib : value.getAttributes().keySet()) {
			if (attrib.equals(attribBase) || attrib.startsWith(attribPrefix)) {
				attribName = attrib;
				attribValue = value.getAttributes().get(attrib);
				break;
			}
		}

		// if not found, then no match
		if (attribValue == null) return MatchResults.FALSE;
		
		// otherwise, if the found value matches any of the enum values we're interested in...
		for (String v : attribValues) {
			if (v != null && v.equals(attribValue)) return new MatchResults(attribBase, attribName, attribValue, value);
		}
		
		return MatchResults.FALSE;
	}
	
}

