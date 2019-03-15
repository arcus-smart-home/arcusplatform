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
package com.iris.protocol.zwave.constants;

public class ZWaveFrameType {
	public static final byte SOF = 0x01;  /* Start Of Frame */
	public static final byte ACK = 0x06;  /* Acknowledge successful frame reception */
	public static final byte NAK = 0x15;  /* Not Acknowledge successful frame reception - please retransmit... */
	public static final byte CAN = 0x18;  /* Frame received (from host) was dropped - waiting for ACK */

	/* Frame types */
	public static final byte REQUEST     = 0x00;
	public static final byte RESPONSE    = 0x01;

	public static String typeToString(byte b) {
		switch (b) {
			case SOF:return "Start of Frame";
			case ACK:return "ACK";
			case NAK:return "NAK";
			case CAN:return "CAN";
		}
		return "Unknown";
	}

	public static String directionToString(byte b) {
		switch (b) {
			case REQUEST:return "Request";
			case RESPONSE:return "Response";
		}
		return "Unknown";
	}

}

