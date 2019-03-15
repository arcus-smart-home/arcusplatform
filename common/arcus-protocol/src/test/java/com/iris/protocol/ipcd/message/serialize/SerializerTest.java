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
package com.iris.protocol.ipcd.message.serialize;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.Status;
import com.iris.protocol.ipcd.message.model.StatusType;

public class SerializerTest {
   protected IpcdSerDe serDe;
   
   protected Device device;
   protected Status status;
   
   @Before
   public void setUp() throws Exception {
      serDe = new IpcdSerDe();
      
      Device device = new Device();
      device.setVendor("BlackBox");
      device.setModel("Multisensor2");
      device.setSn("00049B3C7A05");
      device.setIpcdver("1.0");
      
      Status status = new Status();
      status.setResult(StatusType.success);
   }
   
   public Reader json(String json) {
      return new StringReader(json);
   }
   
   public Reader getJson(String fileName) throws Exception {
      return new InputStreamReader(this.getClass().getResourceAsStream(fileName + ".json"));
   }
   
   
   @After
   public void tearDown() throws Exception {
   }
}

