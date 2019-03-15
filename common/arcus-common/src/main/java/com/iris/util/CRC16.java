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
/////////////////////////////////////////////////////////////////////////////
// CRC-16 CCITT Code Derived From:
// http://www.ross.net/crc/download/crc_v3.txt
//
// All of the code presented in that document is public domain.
/////////////////////////////////////////////////////////////////////////////
package com.iris.util;

public final class CRC16 {
	public static final CRC16 ARC = new CRC16(0x8005,0x0000,true, true, 0x0000);
	public static final CRC16 AUG = new CRC16(0x1021,0x1D0F,false, false, 0x0000);
	public static final CRC16 BUYPASS = new CRC16(0x8005,0x0000,false, false, 0x0000);
	public static final CRC16 CCITT = new CRC16(0x1021,0xFFFF,false,false,0x0000);
	public static final CRC16 CDMA2000= new CRC16(0xC867,0xFFFF,false,false,0x0000);
	public static final CRC16 DDS110 = new CRC16(0x8005,0x800D,false,false,0x0000);
	public static final CRC16 DECT_R = new CRC16(0x0589,0x0000,false,false,0x0001);
	public static final CRC16 DECT_X = new CRC16(0x0589,0x0000,false,false,0x0000);
	public static final CRC16 DNP = new CRC16(0x3D65,0x0000,true,true,0xFFFF);
	public static final CRC16 EN_13757 = new CRC16(0x3D65,0x0000,false,false,0xFFFF);
	public static final CRC16 GENIBUS = new CRC16(0x1021,0xFFFF,false,false,0xFFFF);
	public static final CRC16 MAXIM = new CRC16(0x8005,0x0000,true,true,0xFFFF);
	public static final CRC16 MCRF4XX = new CRC16(0x1021,0xFFFF,true,true,0x0000);
	public static final CRC16 RIELLO = new CRC16(0x1021,0xB2AA,true,true,0x0000);
	public static final CRC16 T10_DIF = new CRC16(0x8BB7,0x0000,false,false,0x0000);
	public static final CRC16 TELEDISK = new CRC16(0xA097,0x0000,false,false,0x0000);
	public static final CRC16 TMS37157 = new CRC16(0x1021,0x89EC,true,true,0x0000);
	public static final CRC16 USB = new CRC16(0x8005,0xFFFF,true,true,0xFFFF);
	public static final CRC16 A = new CRC16(0x1021,0xC6C6,true,true,0x0000);
	public static final CRC16 MODBUS = new CRC16(0x8005,0xFFFF,true,true,0x0000);
	public static final CRC16 X25 = new CRC16(0x1021,0xFFFF,true,true,0xFFFF);
	public static final CRC16 XMODEM = new CRC16(0x1021,0x0000,false,false,0x0000);
	public static final CRC16 KERMIT = new CRC16(0x1021,0x0000,true,true,0x0000);
	
	private short[] table;
	private final int polynomial;
	private final int initial;
	private final boolean reverseInput;
	private final boolean reverseOutput;
	private final int xorOutput;
	
	private CRC16(int polynomial,int initial, boolean reverseInput, boolean reverseOutput, int xorOutput) {
		this.polynomial = polynomial;
		this.initial = initial;
		this.reverseInput = reverseInput;
		this.reverseOutput = reverseOutput;
		this.xorOutput = xorOutput;				

		generateTable();
	}
		
	private void generateTable() {
		table = new short[256];
		for (short i = 0; i < 256; i++) {
			table[i] = (short) generateTableEntry(i);
		}
	}

	private short generateTableEntry(short index) {
		short topbit = (short) 0x8000;
		short r = (short) (index << 8);
		for (int i = 0; i < 8; i++) {
			if ((r & topbit) != 0) {
				r = (short) ((r << 1) ^ polynomial);
			} else {
				r <<= 1;
			}
		}
		return r;
	}
		
    private short reverseBits(short in)
    {
    		short out = 0;
        for (int i=15;i>=0;i--)
        {
            out |= (in & 1) << i;
            in >>= 1;
        }
        return out;
    }

    private byte reverseByte(byte in)
    {
    		byte out = 0;
        for (int i=7;i>=0;i--)
        {
            out |= (in & 1) << i;
            in >>= 1;
        }
        return out;
    }
    
	public short crc(int crc, byte data) {
		byte in = reverseInput ? reverseByte(data) : data;
		short out = (short) (table[((crc >> 8) ^ in) & 0xFF] ^ (crc << 8));
		return out;
	}

	public short crc(byte[] data) {
		return crc(data,0,data.length);
	}
	
	public short crc(byte[] data,int offset, int length) {
		short crc = (short) initial;
		for (int i=offset; i < offset+length; i++) {
			crc = crc(crc,data[i]);
		}
		if (reverseOutput) {
			crc = reverseBits(crc);
		}
		return (short) (crc ^ xorOutput);
	}
}

