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

import static com.iris.platform.notification.provider.NotificationProviderUtil.PLACE_KEY;
import static com.iris.platform.notification.provider.NotificationProviderUtil.addAdditionalParamsAndReturnRecipient;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.NOTIFICATION_ID_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.PERSON_ID_PARAM_NAME;
import static com.iris.platform.notification.provider.ivr.TwilioHelper.PLACE_ID_PARAM_NAME;
import static com.iris.util.Net.urlEncode;
import static com.iris.util.Objects.equalsAny;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.impl.HttpRequestParameters;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.template.TemplateService;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Place;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.ivr.pronounce.StreetAddressPronouncer;
import com.iris.population.PlacePopulationCacheManager;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public abstract class TwilioScriptHandler extends TwilioBaseHandler
{
   protected static final String BASE_URL_PATH = "/ivr/script";

   protected static final String STEP_PARAM           = "step";
   protected static final String CUSTOM_MESSAGE_PARAM = "customMessage";

   protected static final String TWILIO_DIGITS_PARAM = "Digits";

   protected static final String CUSTOM_PARAMETERS_KEY = "customParameters";

   protected static final String CALLBACK_URL_KEY   = "callbackURL";
   protected static final String LANGUAGE_KEY       = "language";
   protected static final String VOICE_KEY          = "voice";
   protected static final String CUSTOM_MESSAGE_KEY = "customMessage";

   protected static final String PRONOUNCED_PLACE_ADDRESS_KEY = "_pronouncedPlaceAddress";

   protected static final String LANGUAGE_ENGLISH = "en_US";

   protected static final String TIMEOUT_MESSAGE = "Timeout or incorrect response";

   protected static int determineRetryCount(Map<String, Object> context, String retryCountKey,
      String... incrementingSteps)
   {
      int retryCount = 0;

      if (context.containsKey(retryCountKey))
      {
         retryCount = (int) context.get(retryCountKey);

         String step = (String) context.get(STEP_PARAM);

         if (equalsAny(step, incrementingSteps))
         {
            retryCount++;
         }
      }

      context.put(retryCountKey, retryCount);

      return retryCount;
   }

   @Inject(optional = true) @Named("twilio.voice")
   private String twilioVoice = "female";
   @Inject(optional = true) @Named("twilio.param.prefix")
   private String twilioNotificationParamPrefix = "_";

   protected final PlaceDAO placeDao;
   protected final PersonDAO personDao;
   protected final AccountDAO accountDao;

   @Inject
   private StreetAddressPronouncer streetAddressPronouncer;

   protected TwilioScriptHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, TemplateService templateService,
      PlaceDAO placeDao, PersonDAO personDao, AccountDAO accountDao, PlacePopulationCacheManager populationCacheMgr)
   {
      super(alwaysAllow, new HttpSender(TwilioScriptHandler.class, metrics), templateService, populationCacheMgr);

      this.placeDao = placeDao;
      this.personDao = personDao;
      this.accountDao = accountDao;
   }

   @Override
   public TemplatedResponse doHandle(FullHttpRequest request, ChannelHandlerContext ctx)
   {
      if (!verifyRequest(request))
      {
         return createTemplateResponse(NOT_FOUND);
      }

      Map<String, Object> context = initializeContext(request);

      String nextStep = determineNextStep(context);

      Map<String, Object> templateContext = buildTemplateContext(context);

      String templateId = (String) context.get(SCRIPT_PARAM);

      return createTemplateResponse(templateId, templateContext, nextStep);
   }

   private Map<String, Object> initializeContext(FullHttpRequest request)
   {
      HttpRequestParameters requestParams = new HttpRequestParameters(request);

      Map<String, Object> context = new HashMap<>();

      context.put(SCRIPT_PARAM,                       requestParams.getParameter(SCRIPT_PARAM));
      context.put(STEP_PARAM,                         requestParams.getParameter(STEP_PARAM, getInitialStep()));
      context.put(PLACE_ID_PARAM_NAME,                requestParams.getParameter(PLACE_ID_PARAM_NAME, ""));
      context.put(PERSON_ID_PARAM_NAME,               requestParams.getParameter(PERSON_ID_PARAM_NAME));
      context.put(TWILIO_DIGITS_PARAM,                requestParams.getParameter(TWILIO_DIGITS_PARAM, ""));
      context.put(NOTIFICATION_ID_PARAM_NAME,         requestParams.getParameter(NOTIFICATION_ID_PARAM_NAME));
      context.put(NOTIFICATION_EVENT_TIME_PARAM_NAME, requestParams.getParameter(NOTIFICATION_EVENT_TIME_PARAM_NAME));
      context.put(CUSTOM_MESSAGE_PARAM,               requestParams.getParameter(CUSTOM_MESSAGE_PARAM, ""));
      context.put(CUSTOM_PARAMETERS_KEY,              getCustomParameters(requestParams));

      customizeContext(context, requestParams);

      return context;
   }

   protected abstract String getInitialStep();

   private Map<String, String> getCustomParameters(HttpRequestParameters requestParams)
   {
      try
      {
         return requestParams.getParameters().entrySet().stream()
            .filter(p -> p.getKey().startsWith(twilioNotificationParamPrefix))
            .collect(toMap(p -> p.getKey(), p -> p.getValue().get(0)));
      }
      catch (Exception e)
      {
         return ImmutableMap.of();
      }
   }

   protected void customizeContext(Map<String, Object> context, HttpRequestParameters requestParams) { }

   protected abstract String determineNextStep(Map<String, Object> context);

   private Map<String, Object> buildTemplateContext(Map<String, Object> context)
   {
      @SuppressWarnings("unchecked")
      Map<String, String> customParameters = (Map<String, String>) context.get(CUSTOM_PARAMETERS_KEY);

      Map<String, String> normalizedCustomParameters = customParameters.entrySet().stream()
         .collect(toMap(e -> e.getKey().substring(twilioNotificationParamPrefix.length()), e -> e.getValue()));

      String callbackUrl = buildCallbackUrl(context);

      String customMessage = (String) context.get(CUSTOM_MESSAGE_PARAM);
      String placeId       = (String) context.get(PLACE_ID_PARAM_NAME);
      String personId      = (String) context.get(PERSON_ID_PARAM_NAME);

      Map<String, BaseEntity<?,?>> additionalParamsAndReturnRecipient =
         addAdditionalParamsAndReturnRecipient(placeDao, personDao, accountDao, placeId, personId);

      StreetAddress pronouncedPlaceAddress =
         streetAddressPronouncer.pronounceFor((Place) additionalParamsAndReturnRecipient.get(PLACE_KEY));

      Map<String, Object> templateContext = new ImmutableMap.Builder<String, Object>()
         .put(LANGUAGE_KEY, LANGUAGE_ENGLISH)
         .put(VOICE_KEY, twilioVoice)
         .put(CALLBACK_URL_KEY, callbackUrl)
         .put(CUSTOM_MESSAGE_KEY, customMessage)
         .putAll(normalizedCustomParameters)
         .putAll(additionalParamsAndReturnRecipient)
         .put(PRONOUNCED_PLACE_ADDRESS_KEY, pronouncedPlaceAddress)
         .build();

      return templateContext;
   }

   private String buildCallbackUrl(Map<String, Object> context)
   {
      @SuppressWarnings("unchecked")
      Map<String, String> customParameters = (Map<String, String>) context.get(CUSTOM_PARAMETERS_KEY);

      Map<String, Object> callbackUrlParamMap = new ImmutableMap.Builder<String, Object>()
         .put(SCRIPT_PARAM,                       context.get(SCRIPT_PARAM))
         .put(NOTIFICATION_ID_PARAM_NAME,         context.get(NOTIFICATION_ID_PARAM_NAME))
         .put(NOTIFICATION_EVENT_TIME_PARAM_NAME, context.get(NOTIFICATION_EVENT_TIME_PARAM_NAME))
         .put(PLACE_ID_PARAM_NAME,                context.get(PLACE_ID_PARAM_NAME))
         .put(PERSON_ID_PARAM_NAME,               context.get(PERSON_ID_PARAM_NAME))
         .putAll(additionalCallbackUrlParams(context))
         .putAll(customParameters)
         .build();

      List<String> callbackUrlParamList = callbackUrlParamMap.entrySet().stream()
         .map(e -> format("%s=%s", e.getKey(), urlEncode(e.getValue().toString())))
         .collect(toList());

      String callbackUrl = getUrlPath() + "?" + String.join("&", callbackUrlParamList);

      return callbackUrl;
   }

   protected Map<String, Object> additionalCallbackUrlParams(Map<String, Object> context)
   {
      return ImmutableMap.of();
   }

   protected abstract String getUrlPath();
}

