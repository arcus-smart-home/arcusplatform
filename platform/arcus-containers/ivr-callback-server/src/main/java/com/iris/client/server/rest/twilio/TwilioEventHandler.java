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
package com.iris.client.server.rest.twilio;

import static com.iris.platform.notification.provider.ivr.TwilioHelper.ANSWEREDBY_PARAM_KEY;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.CALL_STATUS_BUSY;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.CALL_STATUS_COMPLETED;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.CALL_STATUS_FAILED;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.CALL_STATUS_NOANSWER;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.CALL_STATUS_PARAM_KEY;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_ID_PARAM_NAME;
import static com.iris.util.Objects.equalsAny;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.impl.HttpRequestParameters;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.template.TemplateService;
import com.iris.population.PlacePopulationCacheManager;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public abstract class TwilioEventHandler extends TwilioBaseHandler
{
   private static final Logger logger = getLogger(TwilioEventHandler.class);

   protected static final String BASE_URL_PATH = "/ivr/event";

   protected TwilioEventHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, TemplateService templateService, PlacePopulationCacheManager populationCacheMgr)
   {
      super(alwaysAllow, new HttpSender(TwilioEventHandler.class, metrics), templateService, populationCacheMgr);
   }

   @Override
   public TemplatedResponse doHandle(FullHttpRequest request, ChannelHandlerContext ctx)
   {
      if (!verifyRequest(request))
      {
         return createTemplateResponse(NOT_FOUND);
      }

      HttpRequestParameters requestParams = new HttpRequestParameters(request);

      String callStatus          = requestParams.getParameter(CALL_STATUS_PARAM_KEY);
      String notificationId      = requestParams.getParameter(NOTIFICATION_ID_PARAM_NAME);
      long notificationTimestamp = requestParams.getParameterLong(NOTIFICATION_EVENT_TIME_PARAM_NAME);
      String answeredBy          = requestParams.getParameter(ANSWEREDBY_PARAM_KEY, "NA");

      logger.debug("Receiving event message for notificationId:{} notificationTimestamp:{} callStatus:{} answeredBy:{}",
         notificationId, notificationTimestamp, callStatus, answeredBy);

      if (equalsAny(callStatus, CALL_STATUS_COMPLETED))
      {
         handleCompleted(requestParams);
      }
      else if (equalsAny(callStatus, CALL_STATUS_FAILED, CALL_STATUS_BUSY, CALL_STATUS_NOANSWER))
      {
         handleFailed(requestParams);
      }

      return createTemplateResponse(NO_CONTENT);
   }

   protected abstract void handleCompleted(HttpRequestParameters requestParams);

   protected abstract void handleFailed(HttpRequestParameters requestParams);
}

