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
package com.iris.oculus.modules.device.mockaction;

import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;

public class MockActions {
	private Map<String,List<MockAction>> actionMap;

	@SuppressWarnings("unchecked")
	public Map<String,List<MockAction>> getActionMap() {
		return Collections.unmodifiableMap(actionMap);
	}

	public void setActionMap(Map<String, List<MockAction>> actionMap) {
		this.actionMap = actionMap;
	}

   @Override
   public String toString() {
      return "MockActions [actionMap=" + actionMap + "]";
   }
}

