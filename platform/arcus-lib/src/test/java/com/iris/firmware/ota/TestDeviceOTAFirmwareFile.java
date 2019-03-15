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
package com.iris.firmware.ota;

import java.util.List;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.file.PopulationDAOModule;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Modules;

@Modules({PopulationDAOModule.class})
public class TestDeviceOTAFirmwareFile extends IrisMockTestCase {
	@Inject
   private DeviceOTAFirmwareResolver resolver;
	
	/**
	 * Make sure the xml file parses without any issues
	 */
	@Test
	public void testParseXmlFile() {
		List<DeviceOTAFirmwareItem> items = resolver.getFirmwares();
		assertNotNull(items);
		assertTrue(items.size() > 0);
	}
}

