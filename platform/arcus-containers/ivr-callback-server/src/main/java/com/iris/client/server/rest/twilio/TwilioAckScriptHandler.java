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

import static com.iris.client.server.rest.twilio.TwilioAckScriptHandler.URL_PATH;
import static com.iris.messages.capability.NotificationCapability.IvrNotificationRefusedEvent.CODE_NO_RESPONSE;
import static com.iris.platform.notification.audit.AuditEventState.DELIVERED;
import static com.iris.platform.notification.audit.AuditEventState.FAILED;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_ID_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.PERSON_ID_PARAM_NAME;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpRequestParameters;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.template.TemplateService;
import com.iris.platform.notification.audit.NotificationAuditor;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
@HttpGet(URL_PATH + "?*")
public class TwilioAckScriptHandler extends TwilioScriptHandler
{
   protected static final String URL_PATH = BASE_URL_PATH + "/ack";

   protected static final String RETRY_COUNT_PARAM = "retryCount";

   protected static final String STEP_GREETING     = "greeting";
   protected static final String STEP_SUBMIT_ACK   = "submitAck";
   protected static final String STEP_ACKNOWLEDGED = "acknowledged";
   protected static final String STEP_RETRY        = "retry";
   protected static final String STEP_ERROR        = "error";

   protected static final String ACKNOWLEDGE_KEY = "#";

   protected static final String ACKNOWLEDGED_MESSAGE = "User acknowledged IVR message";

   @Inject(optional = true) @Named("twilio.script.ack.maxRetries")
   private Integer maxRetries = 2;

   private final NotificationAuditor notificationAuditor;

   @Inject
   public TwilioAckScriptHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, TemplateService templateService,
      PlaceDAO placeDao, PersonDAO personDao, AccountDAO accountDao, NotificationAuditor notificationAuditor, PlacePopulationCacheManager populationCacheMgr)
   {
      super(alwaysAllow, metrics, templateService, placeDao, personDao, accountDao, populationCacheMgr);

      this.notificationAuditor = notificationAuditor;
   }

   @Override
   protected String getInitialStep()
   {
      return STEP_GREETING;
   }

   @Override
   protected void customizeContext(Map<String, Object> context, HttpRequestParameters requestParams)
   {
      Optional<String> retryCountOpt = requestParams.getOptionalParameter(RETRY_COUNT_PARAM);

      if (retryCountOpt.isPresent())
      {
         context.put(RETRY_COUNT_PARAM, parseInt(retryCountOpt.get()));
      }
   }

   @Override
   protected String determineNextStep(Map<String, Object> context)
   {
      String script              = (String) context.get(SCRIPT_PARAM);
      String step                = (String) context.get(STEP_PARAM);
      String digits              = (String) context.get(TWILIO_DIGITS_PARAM);
      String personId            = (String) context.get(PERSON_ID_PARAM_NAME);
      String notificationId      = (String) context.get(NOTIFICATION_ID_PARAM_NAME);
      Date notificationTimestamp = new Date(parseLong((String) context.get(NOTIFICATION_EVENT_TIME_PARAM_NAME)));

      int retryCount = determineRetryCount(context, RETRY_COUNT_PARAM, STEP_GREETING);

      if (retryCount > maxRetries)
      {
         notificationAuditor.log(notificationId, Instant.ofEpochMilli(notificationTimestamp.getTime()), FAILED,
            TIMEOUT_MESSAGE);

         broadcastIvrNotificationRefusedEvent(notificationId, notificationTimestamp, TIMEOUT_MESSAGE, CODE_NO_RESPONSE,
            personId, script);

         return STEP_ERROR;
      }

      if (step.equals(STEP_SUBMIT_ACK))
      {
         if (digits.equals(ACKNOWLEDGE_KEY))
         {
            notificationAuditor.log(notificationId, Instant.ofEpochMilli(notificationTimestamp.getTime()), DELIVERED,
               ACKNOWLEDGED_MESSAGE);

            broadcastIvrNotificationAcknowledgedEvent(notificationId, notificationTimestamp, digits, personId, script);

            return STEP_ACKNOWLEDGED;
         }
         else
         {
            return STEP_RETRY;
         }
      }
      else
      {
         return step;
      }
   }

   @Override
   protected Map<String, Object> additionalCallbackUrlParams(Map<String, Object> context)
   {
      return new ImmutableMap.Builder<String, Object>()
         .put(RETRY_COUNT_PARAM, context.get(RETRY_COUNT_PARAM))
         .build();
   }

   @Override
   protected String getUrlPath()
   {
      return URL_PATH;
   }
}

