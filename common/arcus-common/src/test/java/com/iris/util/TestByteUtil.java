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

import org.junit.Assert;
import org.junit.Test;

public class TestByteUtil {

   @Test
   public void testXor() {
      byte[] a = { (byte) 0xAA, (byte) 0xAA, (byte) 0xAA };
      byte[] b = { 0x55, (byte) 0xFF };
      byte[] c = ByteUtil.xor(a, b);
      
      Assert.assertTrue( c[0] == (byte) 0xFF );
      Assert.assertTrue( c[1] == (byte) 0x55 );
   }
   
   @Test
   public void testPad() {
      byte[] a = new byte[13];
      byte i = 0;
      for ( byte b : a )  b = i++;
      byte[] c = ByteUtil.pad(a, 16);
      
      Assert.assertTrue(c.length == 16);
      i = 0;
      for ( byte b : a )   {
         Assert.assertTrue(b==c[i]);
         i++;
      }
      for (;i<c.length;i++) {
         Assert.assertTrue(c[i] == 0x00);
      }  
   }
   
   
   @Test
   public void testMask() {
      byte v = 0x0F;
      byte m1 = 0x08;
      byte m2 = 0x10;
      
      Assert.assertTrue(ByteUtil.checkMask(v,m1));
      Assert.assertFalse(ByteUtil.checkMask(v,m2));
   }
}

