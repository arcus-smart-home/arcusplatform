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
package com.iris.protocol;

import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.io.json.gson.GsonModule;
import com.iris.messages.address.Address;
import com.iris.messages.address.HubAddress;
import com.iris.protocol.ipcd.IpcdProtocol;

public class TestProtocolMessageBuilder {
   private final Address fromAddress = Address.platformDriverAddress(UUID.randomUUID());
   private final Address toAddress = HubAddress.hubAddress("ABC-1234");

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      Bootstrap bootstrap = Bootstrap.builder()
            .withModuleClasses(GsonModule.class)
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
   }

   @After
   public void tearDown() throws Exception {
      ServiceLocator.destroy();
   }

   @Test
   public void testBasicIpcdMessage() {
      ProtocolMessage message = ProtocolMessage.builder()
                        .from(fromAddress)
                        .to(toAddress)
                        .withPayload(IpcdProtocol.NAMESPACE, new byte[1])
                        .create();
      Assert.assertFalse(message.isPlatform());
      Assert.assertFalse(message.isRequest());
      Assert.assertFalse(message.isError());
      Assert.assertNull(message.getCorrelationId());
      Assert.assertEquals(IpcdProtocol.NAMESPACE, message.getMessageType());
      Assert.assertEquals(fromAddress, message.getSource());
      Assert.assertEquals(toAddress, message.getDestination());
      Assert.assertEquals(-1, message.getTimeToLive());
      long now = System.currentTimeMillis();
      long sentTime = message.getTimestamp().getTime();
      Assert.assertTrue(((now - 500) <= sentTime) && (sentTime <= now));
   }
}

