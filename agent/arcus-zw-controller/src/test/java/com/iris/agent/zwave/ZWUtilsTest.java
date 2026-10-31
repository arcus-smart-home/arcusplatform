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
package com.iris.agent.zwave;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.test.SystemTestCase;
import com.iris.agent.zwave.code.entity.CmdManSpecificReport;

public class ZWUtilsTest extends SystemTestCase {

   /**
    * The platform and the reflex database know a device by this id, so it has
    * to come out byte for byte the same as under iris2. Every byte of this home
    * id is 0x80 or more, which is what sign extension gets wrong.
    */
   @Test
   public void aDeviceIdKeepsEveryByteOfTheHomeId() throws Exception {
      ByteArrayOutputStream expected = new ByteArrayOutputStream();
      expected.write(0x04);
      expected.write(IrisHal.getHubId().getBytes(StandardCharsets.UTF_8));
      expected.write(new byte[] { (byte)0xE1, (byte)0xA2, (byte)0xB3, (byte)0xC4, 0x04 });

      byte[] id = ZWUtils.getDeviceId(0xE1A2B3C4L, 4).getBytes();

      byte[] actual = new byte[expected.size()];
      System.arraycopy(id, 0, actual, 0, actual.length);
      assertArrayEquals(expected.toByteArray(), actual);
   }

   /**
    * Drivers match on these ids, as unsigned values.
    */
   @Test
   public void aManufacturerReportKeepsItsIdsUnsigned() {
      byte[] report = { 0x72, 0x05, 0x00, (byte)0x91, 0x71, 0x23, (byte)0xB2, (byte)0xC3 };

      CmdManSpecificReport cmd = (CmdManSpecificReport)CmdManSpecificReport.DECODER.decode(report, 0).decoded();

      assertEquals(0x0091, cmd.getManufacturerId());
      assertEquals(0x7123, cmd.getProductTypeId());
      assertEquals(0xB2C3, cmd.getProductId());
   }
}
