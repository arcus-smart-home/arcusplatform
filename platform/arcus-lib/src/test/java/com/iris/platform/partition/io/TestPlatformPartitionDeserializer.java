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
package com.iris.platform.partition.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.iris.platform.partition.DefaultPartition;

public class TestPlatformPartitionDeserializer {
   PlatformPartitionDeserializer deserializer = new PlatformPartitionDeserializer();

   @Test
   public void testSerialize() {
      assertEquals(new DefaultPartition(Integer.MIN_VALUE), deserializer.deserialize(new byte[] { -128, 0, 0, 0 }));
      assertEquals(new DefaultPartition(-1), deserializer.deserialize(new byte[] { -1, -1, -1, -1 }));
      assertEquals(new DefaultPartition(0), deserializer.deserialize(new byte[] { 0, 0, 0, 0 }));
      assertEquals(new DefaultPartition(1), deserializer.deserialize(new byte[] { 0, 0, 0, 1 }));
      assertEquals(new DefaultPartition(0x01020304), deserializer.deserialize(new byte[] { 1, 2, 3, 4 }));
      assertEquals(new DefaultPartition(Integer.MAX_VALUE), deserializer.deserialize(new byte[] { 127, -1, -1, -1 }));
   }
}

