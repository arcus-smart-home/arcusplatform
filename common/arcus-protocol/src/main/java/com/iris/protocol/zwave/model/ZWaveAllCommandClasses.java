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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = As.PROPERTY, property = "@class")
public class ZWaveAllCommandClasses {

	/////////////////////////////////////////////
	// Globals
	public static ZWaveAllCommandClasses	allClasses;
	public static String resourceName = "ZWaveCommandClasses.json";

	///////////////////////////////////
	//  All of the command class information
	@JsonProperty("Command Classes")
	final public ArrayList<ZWaveCommandClass>				commandClasses 	= new ArrayList<ZWaveCommandClass>();

	@JsonIgnore
	final public LinkedHashMap<String,ZWaveCommandClass>	classesByName 	= new LinkedHashMap<String,ZWaveCommandClass>();
	@JsonIgnore
	final public LinkedHashMap<Byte,ZWaveCommandClass>		classesByByte	= new LinkedHashMap<Byte,ZWaveCommandClass>();

	@JsonCreator
	public ZWaveAllCommandClasses() {
	}

	public static void init() {
      if ( allClasses == null ) {
         loadDefaultClasses();
      }
	}

	// Initializes all of the command classes that are known, filling in each command and class as we..
	public static void loadAllCommandClasses(URI uri) throws JsonParseException, JsonMappingException, IOException {
		File file = new File(uri);
		loadAllCommandClasses(file);
	}

	public static void loadAllCommandClasses ( String filename ) throws IOException {
		File file = new File(filename);
		if (file.exists()) {
		   loadAllCommandClasses(file);
		   return;
		}
		else {
		   InputStream in = ZWaveAllCommandClasses.class.getClassLoader().getResourceAsStream(filename);
		   if (in != null) {
		      byte[] resourceBytes = IOUtils.toByteArray(in);
		      in.close();
		      loadAllCommandClasses(resourceBytes);
		      return;
		   }
		}
	}

	public static void loadAllCommandClasses(File file) throws IOException  {
	   byte[] fileBytes = FileUtils.readFileToByteArray(file);
		loadAllCommandClasses(fileBytes);
	}

	public static void loadAllCommandClasses(byte[] bytes) throws IOException {
	   // Read all command classes in json format.
	   ObjectMapper mapper = new ObjectMapper();
	   ZWaveAllCommandClasses classes = mapper.readValue(bytes, ZWaveAllCommandClasses.class );
      ZWaveAllCommandClasses.allClasses = classes;
	}

	public void setCommandClasses ( ArrayList<ZWaveCommandClass> classes ) {
		Iterator<ZWaveCommandClass> 	i;
		ZWaveCommandClass				commandClass;

		commandClasses.clear();
		classesByName.clear();
		classesByByte.clear();

		i = classes.iterator();
		while ( i.hasNext() ) {
			commandClass = i.next();
			addCommandClass(commandClass);
		}
	}

	public void addCommandClass ( ZWaveCommandClass commandClass ) {
		commandClasses.add(commandClass);
		classesByName.put(scrub(commandClass.name), commandClass);
		classesByByte.put(commandClass.number, commandClass);
	}

	public ZWaveCommandClass get(byte id) {
		return classesByByte.get( new Byte(id) );
	}

	public ZWaveCommandClass get ( String name ) {
		return classesByName.get(scrub(name));
	}

	public static ZWaveCommandClass getClass(byte id) {
	   init();
      if ( allClasses == null ) {
         return null;
      }
		return allClasses.get(id);
	}

	public static ZWaveCommandClass getClass(String name) {
	   init();
		if ( allClasses == null ) {
			return null;
		}
		return allClasses.get(name);
	}

	public static boolean loadDefaultClasses() {
	    try {
			loadAllCommandClasses(resourceName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allClasses != null;
	}

	String scrub(String str) {
		return str.toLowerCase().replace(' ', '_');
	}
}

