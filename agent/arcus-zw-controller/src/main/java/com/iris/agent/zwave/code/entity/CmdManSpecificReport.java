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
package com.iris.agent.zwave.code.entity;

import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.Decoded;
import com.iris.agent.zwave.code.Decoder;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.ManufacturerSpecificCmdClass;

/**
 * Manufacturer Specific Report Cmd
 * v1
 * 
 * 8 Bytes
 * -------
 * 0       : CmdClass (0x72)
 * 1       : Cmd (0x05)
 * 2-3     : Manufacturer ID
 * 4-5     : Product Type ID
 * 6-7     : Product ID
 * 
 * @author Erik Larson
 */
public class CmdManSpecificReport extends AbstractZCmd {
   public final static CmdManSpecificReportDecoder DECODER = new CmdManSpecificReportDecoder();
   private final static int BYTE_LENGTH = 8;
      
   private final int manufacturerId;
   private final int productTypeId;
   private final int productId;
   
   public CmdManSpecificReport(int manufacturerId, int productTypeId, int productId) {
      super(CmdClasses.MANUFACTURER_SPECIFIC.intId(), ManufacturerSpecificCmdClass.CMD_MANUFACTURER_SPECIFIC_REPORT, BYTE_LENGTH);
      this.manufacturerId = manufacturerId;
      this.productTypeId = productTypeId;
      this.productId = productId;
   }

   public int getManufacturerId() {
      return manufacturerId;
   }

   public int getProductTypeId() {
      return productTypeId;
   }

   public int getProductId() {
      return productId;
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.concat(
               ByteUtils.ints2Bytes(cmdClass, cmd),
               ByteUtils.to16Bits(manufacturerId),
               ByteUtils.to16Bits(productTypeId),
               ByteUtils.to16Bits(productId)
            );
   }
   
   public static class CmdManSpecificReportDecoder implements Decoder {
      
      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int manufacturerId = ByteUtils.from16BitToInt(bytes, offset + 2);
         int productTypeId = ByteUtils.from16BitToInt(bytes, offset + 4);
         int productId = ByteUtils.from16BitToInt(bytes, offset + 6);
         return new Decoded(new CmdManSpecificReport(manufacturerId, productTypeId, productId));
      }
   }
}


