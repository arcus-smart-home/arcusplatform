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
/**
 * 
 */
package com.iris.platform.partition.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.iris.io.Deserializer;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.ByteUtil;

/**
 * 
 */
public class PlatformPartitionDeserializer implements Deserializer<PlatformPartition> {

   @Override
   public PlatformPartition deserialize(byte[] input) throws IllegalArgumentException {
      int id = ByteUtil.bytesToInt(input);
      return new DefaultPartition(id);
   }

   @Override
   public PlatformPartition deserialize(InputStream input) throws IOException, IllegalArgumentException {
      byte[] buffer = new byte[4];
      IOUtils.readFully(input, buffer, 0, 4);
      return deserialize(buffer);
   }

}

