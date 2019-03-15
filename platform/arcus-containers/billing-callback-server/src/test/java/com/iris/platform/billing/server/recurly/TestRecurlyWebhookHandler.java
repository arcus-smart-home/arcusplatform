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
package com.iris.platform.billing.server.recurly;

import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.Invoice;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.handlers.TemplatedHttpHandler.TemplatedResponse;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.template.TemplateService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.service.AccountService;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

@Mocks({
   HttpSender.class,
   ChannelHandlerContext.class,
   BridgeMetrics.class,
   TemplateService.class,
   BillingClient.class,
   PlatformMessageBus.class})
public class TestRecurlyWebhookHandler extends IrisMockTestCase {
   
   @Provides
   Map<String,WebhookHandler<? extends Object>> getWebhookhandler(){
      return handlers;
   }
   
   @Inject
   private ChannelHandlerContext ctx;

   @Inject
   private RecurlyCallbackHttpHandler handler;

   @Inject
   private BillingClient mockBillingClient;

   @Inject
   private InMemoryPlatformMessageBus bus;
   
   Map<String,WebhookHandler<? extends Object>>handlers = new HashMap<String, WebhookHandler<? extends Object>>();;
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      handlers.put("closed_invoice_notification",new ClosedInvoiceWebhookHandler(mockBillingClient, bus));
   }

   @Test
   public void shouldCallClosedInvoiceHandle() throws Exception {
      String closedInvoiceFile = "/com/iris/platform/billing/server/recurly/closed_invoice_webhook.xml";
      FullHttpRequest req = createRequestFromFile(closedInvoiceFile);
      SettableFuture<Invoice> invoiceFuture = SettableFuture.create();
      Invoice invoice = new Invoice();
      invoice.setState("failed");
      invoiceFuture.set(invoice);
      expect(mockBillingClient.getInvoice("1000")).andReturn(invoiceFuture);
      replay();
      TemplatedResponse fullResponse = handler.doHandle(req, ctx);
      PlatformMessage success = bus.take();
      String accountId=AccountCapability.DelinquentAccountEventRequest.getAccountId(success.getValue());
      assertEquals("73732bd3-6ca5-4ea2-b1c3-a404878acf2a", accountId);
      assertEquals(HttpResponseStatus.NO_CONTENT, fullResponse.getResponseStatus().get());
      verify();
      reset();
   }
   
   @Test
   public void shouldErrorOnBadContent() throws Exception {
      FullHttpRequest req = createRequest("<badxml></bddxml>");
      TemplatedResponse fullResponse = handler.doHandle(req, ctx);
      assertEquals(HttpResponseStatus.BAD_REQUEST, fullResponse.getResponseStatus().get());
   }
   @Test
   public void shouldIgnoreUnknownValidXML() throws Exception {
      FullHttpRequest req = createRequest("<untracked></untracked>");
      TemplatedResponse fullResponse = handler.doHandle(req, ctx);
      assertEquals(HttpResponseStatus.NO_CONTENT, fullResponse.getResponseStatus().get());
   }

   private FullHttpRequest createRequestFromFile(String fileOnClasspath) throws Exception {
      String testRequest = IOUtils.toString(TestRecurlyWebhookHandler.class.getResourceAsStream(fileOnClasspath));
      return createRequest(testRequest);
   }

   private FullHttpRequest createRequest(String content) {
      FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/recurly/webhook");
      ByteBuf buffer = Unpooled.copiedBuffer(content.getBytes());
      req.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
      req.content().clear().writeBytes(buffer);
      return req;
   }
   
   private PlatformMessage createPlatformMessage(String accountNumber){
      MessageBody body=AccountCapability.DelinquentAccountEventRequest.builder()
            .withAccountId(accountNumber)
            .build();
         
         PlatformMessage event = PlatformMessage.buildEvent(body, Address.platformService(accountNumber, AccountService.NAME))
               .to(Address.platformService(accountNumber, AccountService.NAME))
               .withTimeToLive(86400000)
               .create();
         return event;
   }
   

   @Override
   public void tearDown() throws Exception {
      super.tearDown();
   }

}

