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
package com.iris.notification.provider.twilio;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.model.Person;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.provider.ivr.TwilioHelper;
import com.iris.util.Net;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.instance.Call;

@Singleton
public class TwilioSender {
   private static final Logger LOGGER = LoggerFactory.getLogger(TwilioSender.class);
   
   private final static String TWILIO_PARAM_METHOD= "GET";
   private final static String TWILIO_PARAM_KEY_METHOD= "Method";
   private final static String TWILIO_PARAM_KEY_TO= "To";
   private final static String TWILIO_PARAM_KEY_FROM= "From";
   private final static String TWILIO_PARAM_KEY_STATUSCALLBACK= "StatusCallback";
   private final static String TWILIO_PARAM_KEY_STATUSCALLBACK_METHOD= "StatusCallbackMethod";
   private final static String TWILIO_PARAM_KEY_STATUSCALLBACK_EVENT= "StatusCallbackEvent";
   private final static String TWILIO_PARAM_KEY_FALLBACK_URL= "FallbackUrl";
   private final static String TWILIO_PARAM_KEY_APPLICATIONSID= "ApplicationSid";

   private final static String TWILIO_PARAM_KEY_RECORD= "Record";

   private final static String TWILIO_PARAM_KEY_IF_MACHINE= "IfMachine";
   private final static String TWILIO_PARAM_VALUE_TRUE= "True";
   
   private final static String TWILIO_PARAM_KEY_STATUSCALLBACK_EVENT_COMPLETED= "completed";

   private final static String IRIS_ACK_HANDLER_PATH="/ivr/script/ack";
   private final static String IRIS_ACK_EVENT_HANDLER_PATH="/ivr/event/ack";
   private final static String CUSTOM_MESSAGE_PARAM_NAME = "customMessage";
   private final static String SCRIPT_PARAM_NAME = "script";
   
   @Inject @Named("twilio.account.from") private String twilioAccountFrom;
   @Inject @Named("twilio.callback.serverurl") private String twilioCallbackServerUrl;
   @Inject(optional = true) @Named("twilio.applicationSid") private String twilioApplicationSid;
   @Inject(optional = true) @Named("twilio.fallbackUrl") private String twilioFallbackUrl;
   @Inject(optional = true) @Named("twilio.ifmachine") String ifMachine;
   @Inject(optional = true) @Named("twilio.recordCalls") boolean recordCalls=false;
   @Inject(optional = true) @Named("twilio.param.prefix") private String twilioNotificationParamPrefix="_";

   private TwilioRestClient twilio;
   
   @Inject
   public TwilioSender(@Named("twilio.account.sid") String twilioAccountSid, @Named("twilio.account.auth") String twilioAccountAuth) {
      this.twilio = new TwilioRestClient(twilioAccountSid, twilioAccountAuth);
   }

   public String sendIVR(Notification notification, Person recipient) throws DispatchUnsupportedByUserException {
      
      Map<String,String>messageParameters=notification.getMessageParams() != null?notification.getMessageParams() : new HashMap<String,String>();
      
      if(notification.isCustomMessage()){
         messageParameters.put(CUSTOM_MESSAGE_PARAM_NAME, notification.getCustomMessage());
      }
      
      String parameters=Net.toQueryString(messageParameters, twilioNotificationParamPrefix);

      String linkback = String.join("&", new ImmutableMap.Builder<String,String>()
            .put(SCRIPT_PARAM_NAME, notification.getMessageKey() != null ? notification.getMessageKey() : "ivr.custom")
            .put(TwilioHelper.NOTIFICATION_ID_PARAM_NAME,notification.getEventIdentifier())
            .put(TwilioHelper.NOTIFICATION_EVENT_TIME_PARAM_NAME,Long.toString(notification.getRxTimestamp().toEpochMilli()))
            .put(TwilioHelper.PERSON_ID_PARAM_NAME,notification.getPersonId().toString())
            .put(TwilioHelper.PLACE_ID_PARAM_NAME, notification.getPlaceId().toString())
            .build().entrySet().stream()
            .map(e->String.format("%s=%s",e.getKey(),Net.urlEncode(e.getValue())))
            .collect(Collectors.toList()));
      
      List<NameValuePair> params = new ArrayList<NameValuePair>();

      String scriptHandlerPath = IRIS_ACK_HANDLER_PATH;

      String eventHandlerPath = IRIS_ACK_EVENT_HANDLER_PATH;

      params.add(new BasicNameValuePair("Url", String.format("%s%s?%s&%s",
            twilioCallbackServerUrl,
            scriptHandlerPath,
            linkback,
            parameters)));
      
      params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_TO, recipient.getMobileNumber()));
      params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_METHOD, TWILIO_PARAM_METHOD));
      params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_FROM, twilioAccountFrom));
      params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_STATUSCALLBACK,
         format("%s%s?%s", twilioCallbackServerUrl, eventHandlerPath, linkback)));
      params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_STATUSCALLBACK_METHOD, TWILIO_PARAM_METHOD));
      params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_STATUSCALLBACK_EVENT, TWILIO_PARAM_KEY_STATUSCALLBACK_EVENT_COMPLETED));
      
      if(recordCalls){
         params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_RECORD, TWILIO_PARAM_VALUE_TRUE));
      }
      
      if(twilioApplicationSid!=null){
         params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_APPLICATIONSID, twilioApplicationSid));
      }
      
      if(twilioFallbackUrl!=null){
         params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_FALLBACK_URL, twilioFallbackUrl));
      }
      if(ifMachine!=null){
         params.add(new BasicNameValuePair(TWILIO_PARAM_KEY_IF_MACHINE, ifMachine));
      }
      
      CallFactory callFactory = twilio.getAccount().getCallFactory();
      try {
         Call call = callFactory.create(params);
         return call.getSid();
      }
      catch (TwilioRestException tre) {
         LOGGER.error("Error Contacting Twilio",tre);
         switch(tre.getErrorCode()){
            case 21211:   
            case 13224:
               throw new DispatchUnsupportedByUserException(tre.getErrorMessage());
            default:
               throw new RuntimeException("unknown twilio exception",tre);
         }
      }
      catch (Exception e) {
         LOGGER.error("Error Contacting Twilio",e);
         throw new RuntimeException(e);
      }
   }
}

