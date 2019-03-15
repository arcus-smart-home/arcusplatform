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
package com.iris.driver.unit.cucumber;

public class DataTypeUtilities {

	public static Class<?> inferTypeOf(String value) {

		// Base 10 integer
		try {
			Integer.parseInt(value);
			return Integer.class;
		} catch (NumberFormatException e) {}

		// Base 16 integer
		try {
			if (value.startsWith("0x")) {
				Integer.parseInt(value.substring(2));
				return Integer.class;
			}
		} catch (NumberFormatException e) {}

		// Floating point value
		try {
			Double.parseDouble(value);
			return Double.class;
		} catch (NumberFormatException e) {}

		return String.class;
	}

	public static Byte byteValueOf (Object o) {
		String value = o.toString();
		
		if (value.startsWith("0x"))
			return (byte) Integer.parseInt(value.substring(2), 16);
		else
			return (byte) Integer.parseInt(value, 10);
	}
	
	public static String stringValueOf(Object o) {
		String value = o.toString();
		
		// Try to treat the value as a byte
		try {
			if (value.startsWith("0x"))
				return String.valueOf(Byte.parseByte(value.substring(2), 16));
			else
				return String.valueOf(Byte.parseByte(value, 10) & 0xff);
		} catch (NumberFormatException e) {}
		
		// Try to treat the value as a base 16 integer; compare as signed int
		// string
		try {
			if (value.startsWith("0x"))
				return String.valueOf(Integer.parseInt(value.substring(2), 16));
		} catch (NumberFormatException e) {}

		// Looks to be a plain old string...
		return value;
	}
	
	/**
	 * This parses String hexidecimal into a short value.
	 * It only accepts "0x0000" format. 
	 *  
	 */
	public static Short parseHex(String type){
		int decimal = Integer.parseInt(type.substring(2), 16);
		if(decimal >= Short.MIN_VALUE && decimal <= Short.MAX_VALUE ) {
			return (short)decimal;
		} else
		if(decimal > Short.MAX_VALUE && decimal <= Integer.MAX_VALUE ) {
			return (short)-(65536 - decimal);
		} 
		//throw NumberFormatException 
		return Short.parseShort(type.substring(2), 16);
	}

	
	public static Object parsePrimitive(Class<?> parameterType, String value) {
		if(parameterType == Integer.class || parameterType == int.class) {
			return value.matches("0x[0-9A-F]+") 
					? Integer.parseInt(value.substring(2), 16)
					: Integer.parseInt(value);
		} else
		if(parameterType == Short.class || parameterType == short.class) {
			return value.matches("0x[0-9A-F]+") 
					? Short.parseShort(value.substring(2), 16)
					: Short.parseShort(value);
		} else
		if(parameterType == Byte.class || parameterType == byte.class) {
			return value.matches("0x[0-9A-F]+") 
					? Byte.parseByte(value.substring(2), 16)
					: Byte.parseByte(value);
		}
		
		return null;
	}



}

