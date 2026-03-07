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
package com.iris.agent.zwave.engine;

import java.util.Arrays;

/**
 * Represents a Z-Wave Serial API frame.
 *
 * Frame format: SOF | Length | Type | FuncID | Data... | Checksum
 * Length = number of bytes from Type to Checksum (inclusive)
 * Checksum = XOR of all bytes from Length to last data byte (inclusive), then 0xFF XOR
 */
public class ZWaveSerialFrame {

   private final byte type;
   private final byte functionId;
   private final byte[] data;

   public ZWaveSerialFrame(byte type, byte functionId, byte[] data) {
      this.type = type;
      this.functionId = functionId;
      this.data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
   }

   public byte getType() {
      return type;
   }

   public byte getFunctionId() {
      return functionId;
   }

   public byte[] getData() {
      return Arrays.copyOf(data, data.length);
   }

   public int getDataLength() {
      return data.length;
   }

   public byte getDataByte(int index) {
      return data[index];
   }

   public boolean isRequest() {
      return type == ZWaveSerialConstants.REQUEST;
   }

   public boolean isResponse() {
      return type == ZWaveSerialConstants.RESPONSE;
   }

   /**
    * Serialize this frame to bytes for transmission.
    */
   public byte[] toBytes() {
      // length = type(1) + funcId(1) + data.length + checksum(1)
      int length = 2 + data.length + 1;
      byte[] frame = new byte[1 + 1 + 1 + 1 + data.length + 1]; // SOF + len + type + funcId + data + checksum
      frame[0] = ZWaveSerialConstants.SOF;
      frame[1] = (byte) length;
      frame[2] = type;
      frame[3] = functionId;
      System.arraycopy(data, 0, frame, 4, data.length);
      frame[frame.length - 1] = computeChecksum(frame, 1, frame.length - 2);
      return frame;
   }

   /**
    * Compute XOR checksum over the given range, then invert (0xFF XOR).
    */
   public static byte computeChecksum(byte[] data, int offset, int length) {
      byte checksum = (byte) 0xFF;
      for (int i = offset; i < offset + length; i++) {
         checksum ^= data[i];
      }
      return checksum;
   }

   /**
    * Parse a frame from raw bytes. The bytes should start at SOF and include the checksum.
    * Returns null if the frame is invalid.
    */
   public static ZWaveSerialFrame parse(byte[] raw, int offset, int length) {
      if (length < 5) {
         return null; // Minimum: SOF + len + type + funcId + checksum
      }
      if (raw[offset] != ZWaveSerialConstants.SOF) {
         return null;
      }

      int frameLen = 0xFF & raw[offset + 1];
      if (frameLen < 3 || (frameLen + 2) > length) {
         return null; // length byte + SOF + frame content must fit
      }

      // Verify checksum: XOR of length through checksum (inclusive) should yield 0
      byte expected = computeChecksum(raw, offset + 1, frameLen + 1);
      if (expected != 0) {
         return null; // Checksum failed
      }

      byte type = raw[offset + 2];
      byte funcId = raw[offset + 3];
      int dataLen = frameLen - 3; // minus type, funcId, checksum
      byte[] data = new byte[dataLen];
      if (dataLen > 0) {
         System.arraycopy(raw, offset + 4, data, 0, dataLen);
      }

      return new ZWaveSerialFrame(type, funcId, data);
   }

   /**
    * Create a request frame.
    */
   public static ZWaveSerialFrame request(byte functionId, byte... data) {
      return new ZWaveSerialFrame(ZWaveSerialConstants.REQUEST, functionId, data);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ZWaveSerialFrame[");
      sb.append(isRequest() ? "REQ" : "RSP");
      sb.append(String.format(" func=0x%02X", 0xFF & functionId));
      sb.append(String.format(" data=%d bytes", data.length));
      sb.append("]");
      return sb.toString();
   }
}
