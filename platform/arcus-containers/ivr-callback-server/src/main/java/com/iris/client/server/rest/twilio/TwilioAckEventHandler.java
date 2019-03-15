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

import static com.iris.client.server.rest.twilio.TwilioAckEventHandler.URL_PATH;
import static com.iris.platform.notification.audit.AuditEventState.FAILED;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.ANSWEREDBY_PARAM_KEY;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.CALL_STATUS_PARAM_KEY;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_ID_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.PERSON_ID_PARAM_NAME;
import static java.lang.String.format;

import java.time.Instant;
import java.util.Date;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpRequestParameters;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.template.TemplateService;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.notification.audit.NotificationAuditor;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
@HttpGet(URL_PATH + "?*")
public class TwilioAckEventHandler extends TwilioEventHandler
{
   protected static final String URL_PATH = BASE_URL_PATH + "/ack";

   protected static final String ACKNOWLEDGED_MESSAGE = "User acknowledged";

   private static final IrisMetricSet metrics = IrisMetrics.metrics("twilio.event.handler");

   private final Counter successCounter = metrics.counter("call.success.count");
   private final Counter failureCounter = metrics.counter("call.failure.count");

   private final NotificationAuditor notificationAuditor;

   @Inject
   public TwilioAckEventHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, TemplateService templateService,
      NotificationAuditor notificationAuditor, PlacePopulationCacheManager populationCacheMgr)
   {
      super(alwaysAllow, metrics, templateService, populationCacheMgr);

      this.notificationAuditor = notificationAuditor;
   }

   @Override
   protected void handleCompleted(HttpRequestParameters requestParams)
   {
      successCounter.inc();
   }

   @Override
   protected void handleFailed(HttpRequestParameters requestParams)
   {
      failureCounter.inc();

      String callStatus          = requestParams.getParameter(CALL_STATUS_PARAM_KEY);
      String notificationId      = requestParams.getParameter(NOTIFICATION_ID_PARAM_NAME);
      long notificationTimestamp = requestParams.getParameterLong(NOTIFICATION_EVENT_TIME_PARAM_NAME);
      String answeredBy          = requestParams.getParameter(ANSWEREDBY_PARAM_KEY, "NA");
      String personId            = requestParams.getParameter(PERSON_ID_PARAM_NAME);
      String script              = requestParams.getParameter(SCRIPT_PARAM);

      notificationAuditor.log(notificationId, Instant.ofEpochMilli(notificationTimestamp), FAILED,
         format("{}::Call Status:%s Answered By:%s", callStatus, answeredBy));

      broadcastIvrNotificationRefusedEvent(
         notificationId, new Date(notificationTimestamp), callStatus, callStatus, personId, script);
   }
}

