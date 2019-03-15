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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.billing.webhooks.model.ClosedInvoiceNotification;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.handlers.TemplatedHttpHandler;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.template.TemplateService;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

@Singleton
@HttpPost("/recurly/webhook")
public class RecurlyCallbackHttpHandler extends TemplatedHttpHandler {
   private static final String MSG_IGNORE_NOTIFICATION = "un-interesting webhook message from recurly";
   public static final String TRANS_TYPE_CLOSED_INVOICE_NOTIFICATION = "closed_invoice_notification";
   private static final Logger LOGGER = LoggerFactory.getLogger(RecurlyCallbackHttpHandler.class);
   private static final String SERVICE_NAME="recurly.webhook.handler";
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(SERVICE_NAME);
   private final Timer WEBHOOK_TIMER = METRICS.timer("webhook.timer");
   private final Counter IGNORED_COUNTER = METRICS.counter("ignored.webhook.count");
   private final Counter ERRORED_COUNTER = METRICS.counter("errored.webhook.count");
   
   @Inject
   private Map<String,WebhookHandler<? extends Object>>handlers;
   

   @Inject
   public RecurlyCallbackHttpHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, TemplateService templateService) {
      super(alwaysAllow, new HttpSender(RecurlyCallbackHttpHandler.class, metrics), templateService);
   }

   public TemplatedResponse doHandle(FullHttpRequest request, ChannelHandlerContext ctx) throws Exception {
      Context timer = WEBHOOK_TIMER.time();
      
      try{
         String recurlyXML = request.content().toString(CharsetUtil.UTF_8);
         Document document = XMLHelper.parse(recurlyXML);
         String transactionType = document.getDocumentElement().getTagName();
         WebhookHandler<? extends Object>handler=handlers.get(transactionType);
         
         if(transactionType.equals(TRANS_TYPE_CLOSED_INVOICE_NOTIFICATION)){
            ClosedInvoiceNotification notification = XMLHelper.unmarshall(document,ClosedInvoiceNotification.class);
           ((WebhookHandler)handler).handleWebhook(notification);
         }
         else{
            IGNORED_COUNTER.inc();
            LOGGER.info(MSG_IGNORE_NOTIFICATION);
         }
         return createTemplateResponse(HttpResponseStatus.NO_CONTENT);
      }
      catch(Exception e){
         ERRORED_COUNTER.inc();
         LOGGER.error("Unknown error processing recurly webhook",e);
         return createTemplateResponse(HttpResponseStatus.BAD_REQUEST);
      }
      finally{
         timer.stop();
      }
   }

   @Override
   public String getContentType() {
      return MediaType.APPLICATION_XML_UTF_8.toString();
   }
}

