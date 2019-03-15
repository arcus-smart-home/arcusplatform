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
package com.iris.bridge.bus;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.bridge.server.session.ClientToken;
import com.iris.core.dao.file.FileDAOModule;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.hubcom.server.HubServerModule;
import com.iris.io.json.gson.GsonModule;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;

public class TestProtocolBusService {
   private InMemoryProtocolMessageBus protocolBus;

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() throws Exception {
      Bootstrap bootstrap = Bootstrap.builder()
            .withModuleClasses(GsonModule.class, InMemoryMessageModule.class, HubServerModule.class, FileDAOModule.class)
            .withConfigPaths("src/dist/conf/hub-bridge.properties")
            .build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));

      this.protocolBus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
   }

   @After
   public void tearDown() throws Exception {
      ServiceLocator.destroy();
   }

   @Test
   @Ignore
   public void testAddServiceListener() throws Exception {
      Address destination = Address.fromString("PROT:ZWAV-ABC-1234:" + ProtocolDeviceId.fromBytes(new byte [] { 22 }).getRepresentation());
      ProtocolMessage protocolMessage = ProtocolMessage.createProtocolMessage(
            Address.fromString("DRIV:dev:6ff3a5ff-4650-4ce7-82e0-682a58392316"),
            destination,
            ZWaveProtocol.INSTANCE,
            new ZWaveCommandMessage()
      );
      final SettableFuture<Boolean> future = SettableFuture.create();
      ProtocolBusService protocolBusService = ServiceLocator.getInstance(ProtocolBusService.class);
      protocolBusService.addProtocolListener(new ProtocolBusListener() {

         @Override
         public void onMessage(ClientToken ct, ProtocolMessage msg) {
            try {
               Assert.assertEquals("Client Token key should be hub ID", "ABC-1234", ct.getRepresentation());
               Assert.assertEquals("PROT:ZWAV-ABC-1234:FgAAAAAAAAAAAAAAAAAAAAAAAAA=", msg.getDestination().getRepresentation());
               Assert.assertEquals("DRIV:dev:6ff3a5ff-4650-4ce7-82e0-682a58392316", msg.getSource().getRepresentation());
               Assert.assertEquals(destination, msg.getDestination());
               future.set(true);
            }
            catch(Throwable t) {
               future.setException(t);
            }
         }

      });

      protocolBus.send(protocolMessage);
      boolean messageReceivedFlag = future.get(300, TimeUnit.MILLISECONDS);

      Assert.assertTrue("Message should have arrived.", messageReceivedFlag);
   }

   @Test
   @Ignore
   public void testPlaceMessageOnBus() throws Exception {
      Address source = Address.fromString("PROT:ZWAV-ABC-1234:" + ProtocolDeviceId.fromBytes(new byte [] { 22 }).getRepresentation());
      ProtocolMessage protocolMessage = ProtocolMessage.createProtocolMessage(
            source,
            Address.broadcastAddress(),
            ZWaveProtocol.INSTANCE,
            new ZWaveCommandMessage());
      ProtocolBusService protocolBusService = ServiceLocator.getInstance(ProtocolBusService.class);
      protocolBusService.placeMessageOnProtocolBus(protocolMessage);
      ProtocolMessage msg = protocolBus.take();
      Assert.assertEquals("Messages should match", protocolMessage.getDestination(), msg.getDestination());
      Assert.assertEquals("Messages should match", protocolMessage.getSource(), msg.getSource());
      Assert.assertArrayEquals("Messages should match", protocolMessage.getBuffer(), msg.getBuffer());
   }

}

