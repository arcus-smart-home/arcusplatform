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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

import javax.xml.bind.DatatypeConverter;

@RunWith(JUnit4.class)
public class TestCRC16 {

	@Test
	public void testHubFromMac() {
		String str = "00112233445566778899AABBCCDDEEFF";
		byte[] bytes = DatatypeConverter.parseHexBinary(str);
		assertEquals((short) 0x27A9, CRC16.ARC.crc(bytes));
		assertEquals((short) 0x57E3, CRC16.AUG.crc(bytes));
		assertEquals((short) 0x0B62, CRC16.BUYPASS.crc(bytes));
		assertEquals((short) 0x7842, CRC16.CCITT.crc(bytes));
		assertEquals((short) 0x9F0B, CRC16.CDMA2000.crc(bytes));
		assertEquals((short) 0x8545, CRC16.DDS110.crc(bytes));
		assertEquals((short) 0xDFE2, CRC16.DECT_R.crc(bytes));
		assertEquals((short) 0xDFE3, CRC16.DECT_X.crc(bytes));
		assertEquals((short) 0xF289, CRC16.DNP.crc(bytes));
		assertEquals((short) 0xAF18, CRC16.EN_13757.crc(bytes));
		assertEquals((short) 0x87BD, CRC16.GENIBUS.crc(bytes));
		assertEquals((short) 0xD856, CRC16.MAXIM.crc(bytes));
		assertEquals((short) 0x70AD, CRC16.MCRF4XX.crc(bytes));
		assertEquals((short) 0x198A, CRC16.RIELLO.crc(bytes));
		assertEquals((short) 0xB1B2, CRC16.T10_DIF.crc(bytes));
		assertEquals((short) 0x70BC, CRC16.TELEDISK.crc(bytes));
		assertEquals((short) 0x348C, CRC16.TMS37157.crc(bytes));
		assertEquals((short) 0x28E8, CRC16.USB.crc(bytes));
		assertEquals((short) 0x69CC, CRC16.A.crc(bytes));
		assertEquals((short) 0xD717, CRC16.MODBUS.crc(bytes));		
		assertEquals((short) 0x8F52, CRC16.X25.crc(bytes));
		assertEquals((short) 0x1248, CRC16.XMODEM.crc(bytes));
		assertEquals((short) 0x20FB, CRC16.KERMIT.crc(bytes));
	}

}

