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
package com.iris.protocol.zwave.model;

//import static org.junit.Assert.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.protocol.zwave.constants.ZWaveCommandClassName;

public class TestZWaveAllCommandClasses {

	@Test
	public void testCreateAndLoadGlobals() throws JsonGenerationException, JsonMappingException, IOException {
		ZWaveAllCommandClasses classes = new ZWaveAllCommandClasses();
		File filename = File.createTempFile("TestZWaveCommandClasses", ".json");
		filename.deleteOnExit();

		// Create a set command in json format.
		ZWaveCommandClass 	switchCommandClass 	= new ZWaveCommandClass("Switch Binary",(byte) 0x25);
		ZWaveCommand		setCommand			= new ZWaveCommand("set",(byte) 0x01);
		setCommand.addSendVariable("value");
		ZWaveCommand		getCommand			= new ZWaveCommand("get",(byte) 0x02);
		getCommand.addReceiveVariable("value");
		ZWaveCommand		reportCommand		= new ZWaveCommand("report",(byte) 0x03);
		reportCommand.addReceiveVariable("value");
		switchCommandClass.add(setCommand);
		switchCommandClass.add(getCommand);
		switchCommandClass.add(reportCommand);

		classes.addCommandClass(switchCommandClass);
		ObjectMapper mapper = new ObjectMapper();
		mapper.writerWithDefaultPrettyPrinter().writeValue(new FileWriter(filename), classes);

		// Load the json file to the globals.
		ZWaveAllCommandClasses.loadAllCommandClasses(filename);

		Byte key = new Byte((byte) 0x25);
		ZWaveCommandClass switch2 = ZWaveAllCommandClasses.allClasses.classesByByte.get(key);
		assertNotNull(switch2);
		ZWaveCommand cmd = switch2.get("set");
		assertNotNull(cmd);
	}

	// If the all classes loads properly then the switch should load correctly as well.
	@Test
	public void testAutoLoadJSON() {
		ZWaveCommandClass binarySwitch;
		ZWaveAllCommandClasses.allClasses = null;		// Ensure that this has not already been loaded.
		binarySwitch = ZWaveAllCommandClasses.getClass("Switch Binary");
		assertNotNull(binarySwitch);

		ZWaveCommandClass clock = ZWaveAllCommandClasses.getClass((byte) 0x81);
		assertNotNull(clock);
	}
	
	  @Test
	   public void testLoadWithResponsesAllCommandClasses() {
	      ZWaveCommandClass cmdClass = ZWaveAllCommandClasses.getClass(ZWaveCommandClassName.SWITCH_BINARY);

	      assertTrue(cmdClass.get("get").response_id == 0x03);
	   }
}

