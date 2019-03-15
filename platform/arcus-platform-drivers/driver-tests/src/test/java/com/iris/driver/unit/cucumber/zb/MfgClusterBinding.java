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
package com.iris.driver.unit.cucumber.zb;

import com.iris.driver.groovy.zigbee.cluster.zcl.GeneralBinding;
/****
 * Stub cluster for sending mfg specific cluster messages to drivers in unit test.
 * Must be used with "And with payload" since harness is unaware of method/attribute names" 
 * @author Finch
 *
 */
public class MfgClusterBinding extends GeneralBinding{
	
	private short id;

	MfgClusterBinding(short cluster){
		this.id = cluster;
	}
	
	
	@Override
	public short getId() {
		return id;
	}

	@Override
	public String getName() {
		return "Manufacturer Specific Cluster";
	}

}

