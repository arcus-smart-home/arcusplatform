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
package com.iris.protocol.zigbee;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.DataOutputStream;
import java.io.DataInputStream;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestZclData {
   @Test
   public void testRandom() throws Exception {
      for(int test = 0; test < 10000; ++test) {
         ZclData a = ZclData.getRandomInstance();
         ZclData b = ZclData.getRandomInstance();

         assertEquals(a, a);
         assertEquals(b, b);

         // small structures have a higher probability of
         // random collision, so we don't test those.
         if (a.getByteSize() > 4 || b.getByteSize() > 4) {
            assertNotEquals(a, b);
         }
      }
   }

   @Test
   public void testEmpty() throws Exception {
      for(int test = 0; test < 10000; ++test) {
         ZclData a = ZclData.getEmptyInstance();
         ZclData b = ZclData.getEmptyInstance();

         assertEquals(a, a);
         assertEquals(b, b);
         assertEquals(ZclData.LENGTH_MIN, a.getByteSize());
         assertEquals(ZclData.LENGTH_MIN, b.getByteSize());
      }
   }

   @Test
   public void testMinimumSize() throws Exception {
      for(int test = 0; test < 10000; ++test) {
         ZclData a = ZclData.getRandomInstance();
         assertTrue(a.getByteSize() + " is less than the minimum size of " + a.getMinimumSize(), a.getByteSize() >= a.getMinimumSize());
      }
   }

   @Test
   public void testMaximumSize() throws Exception {
      // Messages that don't have a maximum size are skipped
      if (ZclData.LENGTH_MAX < 0) {
         return;
      }

      for(int test = 0; test < 10000; ++test) {
         ZclData a = ZclData.getRandomInstance();
         assertTrue(a.getByteSize() + " is greater than the maximum size of " + a.getMaximumSize(), a.getByteSize() <= a.getMaximumSize());
      }
   }

   @Test
   public void testSize() throws Exception {
      ByteBuf buf = Unpooled.buffer();
      for(int test = 0; test < 10000; ++test) {
         buf.clear();
         ZclData a = ZclData.getRandomInstance();

         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         DataOutputStream dos = new DataOutputStream(bos);
         ByteBuffer buffer = ByteBuffer.allocate(a.getByteSize());

         ZclData.serde().ioSerDe().encode(dos, a);
         ZclData.serde().nioSerDe().encode(buffer, a);
         ZclData.serde().nettySerDe().encode(buf, a);

         buffer.flip();
         assertEquals(bos.toByteArray().length, a.getByteSize());
         assertEquals(buffer.limit(), a.getByteSize());
         assertEquals(buf.readableBytes(), a.getByteSize());
      }
   }

   @Test
   public void testIoSerDe() throws Exception {
      for(int test = 0; test < 10000; ++test) {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         DataOutputStream dos = new DataOutputStream(bos);

         ZclData testValue = ZclData.getRandomInstance();
         ZclData.serde().ioSerDe().encode(dos, testValue);

         ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
         DataInputStream dis = new DataInputStream(bis);

         ZclData serdeValue = ZclData.serde().ioSerDe().decode(dis);

         assertEquals(0, bis.available());
         assertEquals(testValue, serdeValue);
      }
   }

   @Test
   public void testNioSerDe() throws Exception {
      for(int test = 0; test < 10000; ++test) {
         ZclData testValue = ZclData.getRandomInstance();

         ByteBuffer buffer = ByteBuffer.allocate(testValue.getByteSize());
         ZclData.serde().nioSerDe().encode(buffer, testValue);

         buffer.flip();
         ZclData serdeValue = ZclData.serde().nioSerDe().decode(buffer);

         assertFalse(buffer.hasRemaining());
         assertEquals(testValue, serdeValue);
      }
   }

   @Test
   public void testNettySerDe() throws Exception {
      ByteBuf buffer = Unpooled.buffer();
      for(int test = 0; test < 10000; ++test) {
         buffer.clear();
         ZclData testValue = ZclData.getRandomInstance();

         ZclData.serde().nettySerDe().encode(buffer, testValue);
         ZclData serdeValue = ZclData.serde().nettySerDe().decode(buffer);

         assertEquals(0, buffer.readableBytes());
         assertEquals(testValue, serdeValue);
      }
   }
}


