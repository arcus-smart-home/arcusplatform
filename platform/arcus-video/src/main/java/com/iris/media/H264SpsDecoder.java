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
package com.iris.media;

import java.util.BitSet;

public class H264SpsDecoder {
   private final BitSet data;
   private final int end;
   private int i;

   protected H264SpsDecoder(byte[] data) {
      for (int i = 0; i < data.length; ++i) {
         data[i] = (byte)(Integer.reverse(data[i]) >> 24);
      }

      this.data = BitSet.valueOf(data);
      this.end = data.length*8;
      this.i = 0;
   }

   public void dump() {
      for (int j = i; j < end; ++j) {
         if ((j != i) && ((j-i) % 32) == 0) {
            System.out.println();
         } else if ((j != i) && ((j-i) % 4) == 0) {
            System.out.print(" ");
         }

            System.out.print(data.get(j) ? "1" : "0");
      }
      System.out.println();
   }

   public int u1() {
      return (int)read(1) & 0x1;
   }

   public int u3() {
      return (int)read(3) & 0x7;
   }

   public int u4() {
      return (int)read(4) & 0xF;
   }

   public int u5() {
      return (int)read(5) & 0x1F;
   }

   public int u8() {
      return (int)read(8) & 0xFF;
   }

   public int u16() {
      return (int)read(16) & 0xFFFF;
   }

   public int u32() {
      return (int)(read(32) & 0x00000000FFFFFFFFL);
   }

   public long u64() {
      return read(64);
   }

   public int ue() {
      int cnt = 0;
      while (true) {
         boolean set = data.get(i++);
         if (set) {
            break;
         }

         cnt++;
      }

      if (cnt > 0) {
         int value = (int)read(cnt);
         return (int)((1 << cnt) - 1 + value);
      }

      return 0;
   }

   public int se() {
      int value = ue();
      int sign = ((value & 0x1) << 1) - 1;
      return ((value >> 1) + (value & 0x1)) * sign;
   }

   private long read(int bits) {
      long result = 0;
      for (int n = 0; n < bits; n++) {
         boolean set = data.get(i++);
         result = (result << 1) | (set ? 1L : 0L);
      }

      return result;
   }
}

