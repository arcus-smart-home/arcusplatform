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

public class Hex {

	public static int fromString (String s) {
		if ( s.length() != 2 ) return 0;

		int b = 0;
		b = (Character.digit(s.charAt(0), 16) << 4)
				+  (Character.digit(s.charAt(1), 16));

		return b;
	}

	public static byte[] fromHexString (String s) {
	   int n = s.length()/2;
	   byte[] tbr = new byte[n];
	   int strIndex = 0;
	   String char2;
	   for (int i=0;i<n;i++) {
	      char2 = s.substring(strIndex, strIndex+2);
	      tbr[i] = (byte) fromString(char2);
	      strIndex+=2;
	   }
	   return tbr;
	}

	public static String toPrint(byte[] bytes) {
	   return toPrint(bytes,0,bytes.length);
	}

	  public static String toPrint(byte[] bytes, int offset, int length) {
	      String str = "[ ";
	      for (int i = 0; i < length; i++) {
	         str += String.format("%02X ", bytes[i + offset]);
	      }
	      str += "]";
	      return str;    
	   }

	
   public static String toString(byte[] bytes) {
      return toString(bytes,0,bytes.length);
   }

   public static String toString(byte[] bytes,int offset, int length) {
      
      String str = "";
      for (int i = 0; i < length; i++) {
         str += String.format("%02X", bytes[i]);
      }
      return str;
   }
}

