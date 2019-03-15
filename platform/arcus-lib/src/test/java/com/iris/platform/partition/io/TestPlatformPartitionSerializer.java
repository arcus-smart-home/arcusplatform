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

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import com.iris.platform.partition.DefaultPartition;

public class TestPlatformPartitionSerializer {
   PlatformPartitionSerializer serializer = new PlatformPartitionSerializer();

   @Test
   public void testSerialize() {
      assertArrayEquals(new byte[] { -128, 0, 0, 0 }, serializer.serialize(new DefaultPartition(Integer.MIN_VALUE)));
      assertArrayEquals(new byte[] { -1, -1, -1, -1 }, serializer.serialize(new DefaultPartition(-1)));
      assertArrayEquals(new byte[] { 0, 0, 0, 0 }, serializer.serialize(new DefaultPartition(0)));
      assertArrayEquals(new byte[] { 0, 0, 0, 1 }, serializer.serialize(new DefaultPartition(1)));
      assertArrayEquals(new byte[] { 1, 2, 3, 4 }, serializer.serialize(new DefaultPartition(0x01020304)));
      assertArrayEquals(new byte[] { 127, -1, -1, -1 }, serializer.serialize(new DefaultPartition(Integer.MAX_VALUE)));
   }
}

