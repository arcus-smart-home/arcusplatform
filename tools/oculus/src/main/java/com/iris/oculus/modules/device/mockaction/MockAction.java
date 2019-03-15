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

import edu.emory.mathcs.backport.java.util.Collections;

public class MockAction {
	private String name;
	private List<Step> steps;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@SuppressWarnings("unchecked")
	public List<Step> getSteps() {
		return Collections.unmodifiableList(steps);
	}
	
	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

   @Override
   public String toString() {
      return "MockAction [name=" + name + ", steps=" + steps + "]";
   }
}

