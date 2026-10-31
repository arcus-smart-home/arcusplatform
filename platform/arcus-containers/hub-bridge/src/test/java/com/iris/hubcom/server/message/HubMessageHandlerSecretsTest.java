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
package com.iris.hubcom.server.message;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.bus.ProtocolBusService;
import com.iris.bridge.hub.HubLogger;
import com.iris.core.messaging.kafka.KafkaOpsConfig;
import com.iris.hubcom.authz.HubMessageFilter;
import com.iris.hubcom.server.session.HubSession;
import com.iris.io.json.JSON;
import com.iris.messages.HubMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubZwaveCapability.S2PinRequiredEvent;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ProtocolMessage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.netty.buffer.Unpooled;

public class HubMessageHandlerSecretsTest {
   private static final String HUB_ID = "ABC-1234";
   private static final String DSK = "00000-11111-22222-33333-44444-55555-11111-22222";

   private final UUID placeId = UUID.randomUUID();

   private Logger logger;
   private Level originalLevel;
   private ListAppender<ILoggingEvent> appender;
   private HubMessageHandler handler;
   private HubSession session;

   @Before
   public void setUp() {
      logger = (Logger) LoggerFactory.getLogger(HubMessageHandler.class);
      originalLevel = logger.getLevel();
      logger.setLevel(Level.TRACE);
      appender = new ListAppender<>();
      appender.start();
      logger.addAppender(appender);

      Properties kafka = new Properties();
      kafka.put("bootstrap.servers", "localhost:1");
      KafkaOpsConfig config = EasyMock.createNiceMock(KafkaOpsConfig.class);
      EasyMock.expect(config.toNuProducerProperties()).andReturn(kafka).anyTimes();
      HubMessageFilter filter = EasyMock.createNiceMock(HubMessageFilter.class);
      EasyMock.expect(filter.acceptFromHub(EasyMock.anyObject(HubSession.class), EasyMock.anyObject(PlatformMessage.class))).andReturn(true).anyTimes();
      EasyMock.expect(filter.acceptFromHub(EasyMock.anyObject(HubSession.class), EasyMock.anyObject(ProtocolMessage.class))).andReturn(true).anyTimes();
      PlatformBusService platformBus = EasyMock.createNiceMock(PlatformBusService.class);
      ProtocolBusService protocolBus = EasyMock.createNiceMock(ProtocolBusService.class);
      DirectMessageExecutor executor = EasyMock.createNiceMock(DirectMessageExecutor.class);
      HubLogger hubLogger = EasyMock.createNiceMock(HubLogger.class);
      PlacePopulationCacheManager population = EasyMock.createNiceMock(PlacePopulationCacheManager.class);
      session = EasyMock.createNiceMock(HubSession.class);
      EasyMock.expect(session.getActivePlace()).andReturn(placeId.toString()).anyTimes();
      EasyMock.replay(config, filter, platformBus, protocolBus, executor, hubLogger, population, session);

      handler = new HubMessageHandler(platformBus, protocolBus, filter, executor, hubLogger, config, population);
   }

   @After
   public void tearDown() throws Exception {
      handler.close();
      logger.detachAppender(appender);
      logger.setLevel(originalLevel);
   }

   @Test
   public void testDskInAHubFrameIsNotLogged() {
      MessageBody body =
         S2PinRequiredEvent
            .builder()
            .withRequestId("request")
            .withNode(12)
            .withDsk(DSK)
            .withTimeoutMs(200000L)
            .build();
      PlatformMessage message =
         PlatformMessage
            .buildMessage(body, Address.hubService(HUB_ID, "zwave"), Address.platformService(placeId, "subpairing"))
            .create();
      byte[] frame = JSON.createSerializer(HubMessage.class).serialize(HubMessage.createPlatform(JSON.createSerializer(PlatformMessage.class).serialize(message)));

      handler.handleMessage(session, Unpooled.wrappedBuffer(frame));

      String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
      assertTrue(logged, logged.contains(S2PinRequiredEvent.NAME));
      assertFalse(logged, logged.contains("11111"));
   }
}
