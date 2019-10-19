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
package com.iris.firmware;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.iris.messages.model.Hub;

public class TestHubFirmwareURLBuilder {

   private FirmwareURLBuilder<Hub> builder = new HubFirmwareURLBuilder();

   @Test
   public void testFirmwareURLBuild() {
      Hub hub = new Hub();

      assertEquals("https://null/IH200/hubOS_2.0.0.017.bin", builder.buildURL(hub, "IH200/hubOS_2.0.0.017"));
   }

}

