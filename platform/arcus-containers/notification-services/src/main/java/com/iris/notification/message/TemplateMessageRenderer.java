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
package com.iris.notification.message;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.template.TemplateService;
import com.iris.io.json.JSON;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationFormat;
import com.iris.platform.notification.NotificationMethod;

@Singleton
public class TemplateMessageRenderer implements NotificationMessageRenderer {
      
   public static final String SSR_KEY="_ssr";
   public static final String REDIRECT_BASE_URL_CONTEXT_KEY="_redirectBaseUrl";
   public static final String TWITTER_UNAME_CONTEXT_KEY = "_atTwitter";
   public static final String WEBHOME_BASE_URL_CONTEXT_KEY="_webHomeBaseUrl";
   
   public static final String CUSTOM_MESSAGE_KEY="customMessage";
   private final static String CUSTOM_TEMPLATE="custom.notification";
   private final static String RESOURCE_SERVER_URL_KEY="static.resource.server.url";
   private final static String TWITTER_UNAME_KEY = "twitter.uname";
   private final static String REDIRECT_BASE_URL_KEY="redirect.base.url";
   private static final String WEBHOME_BASE_URL_KEY = "webhome.base.url";
   
   @Inject @Named(RESOURCE_SERVER_URL_KEY) String staticResourceServerUrl;      
   @Inject @Named(TWITTER_UNAME_KEY) String atTwitter;
   @Inject @Named(REDIRECT_BASE_URL_KEY) String redirectBaseUrl;
   @Inject @Named(WEBHOME_BASE_URL_KEY) String webHomeBaseUrl;
   
   private final TemplateService templateService;
   private final ParameterParser parameterParser;
   private Map<String, String> commonParameters;
   
   @Inject
   public TemplateMessageRenderer(TemplateService templateService, ParameterParser parameterParser) {
   	this.templateService = templateService;
   	this.parameterParser = parameterParser;   	
   }
   
   @PostConstruct
   public void init() {
	  ImmutableMap.Builder<String, String> paramBuilder = ImmutableMap.builder(); 
	  paramBuilder.put(SSR_KEY, staticResourceServerUrl);
	  paramBuilder.put(TWITTER_UNAME_CONTEXT_KEY, atTwitter);
	  paramBuilder.put(REDIRECT_BASE_URL_CONTEXT_KEY, redirectBaseUrl);
	  paramBuilder.put(WEBHOME_BASE_URL_CONTEXT_KEY, webHomeBaseUrl);
	  
	  this.commonParameters = paramBuilder.build();
   }

   @Override
   public String renderMessage(Notification notification, NotificationMethod method, Person recipient, Map<String, BaseEntity<?, ?>> additionalEntityParams) {
      if (notification.isCustomMessage()) {
         return notification.getCustomMessage();
      }
      Map<String, Object> context = new HashMap<String, Object>();
      addParams(context, notification, method, recipient, additionalEntityParams);      
      return templateService.render(getTemplateName(notification, method), context);
   }

   @Override
   public Map<String, String> renderMultipartMessage(Notification notification, NotificationMethod method, Person recipient, Map<String, BaseEntity<?, ?>> additionalEntityParams) {

      Map<String, Object> context = new HashMap<String, Object>();
      if (notification.isCustomMessage()) {
         context.put(CUSTOM_MESSAGE_KEY, notification.getCustomMessage());
      }
      addParams(context, notification, method, recipient, additionalEntityParams);
      return templateService.renderMultipart(getTemplateName(notification, method), context);
   }
   
   private void addParams(Map<String, Object> context, Notification notification, NotificationMethod method, Person recipient, Map<String, BaseEntity<?,?>> additionalEntityParams) {   	  
      context.putAll(commonParameters);
      
      //add additional entity parameters if any
      if(additionalEntityParams != null) {
    	  for(Map.Entry<String, BaseEntity<?, ?>> entry : additionalEntityParams.entrySet()) {
    		  context.put(entry.getKey(), entry.getValue());
    	  }
      }
      
      if (notification.getMessageParams() != null) {
      	Map<String, String> resolvedParameters = resolveParameters(notification.getMessageParams());
      	if (NotificationFormat.JSON.equals(method.format())) {
	      	for (Map.Entry<String, String> entry : resolvedParameters.entrySet()) {
	      		context.put(entry.getKey(), escapeJson(entry.getValue()));
	      	}
      	}
      	else {
      		context.putAll(resolvedParameters);
      	}
      }
   }
   
   private String escapeJson(String value) {
      if(StringUtils.isEmpty(value)) {
         return "";
      }
      value = JSON.toJson(value);
      // strip the beginning " and trailing " since this is being embedded in a JSON structure
   	return value.substring(1, value.length() - 1);
   }

   private String getTemplateName(Notification notification, NotificationMethod method) {
      String template = notification.getMessageKey() != null ? notification.getMessageKey().toLowerCase() : CUSTOM_TEMPLATE;
      return template + "-" + method.toString().toLowerCase();
   }
   
   private Map<String,String> resolveParameters(Map<String, String> origParams) {
   	if (origParams != null && !origParams.isEmpty()) {
   		Map<String, String> resolvedParameters = new HashMap<>(origParams.size());
   		for (Map.Entry<String, String> entry : origParams.entrySet()) {
   			resolvedParameters.put(entry.getKey(), parameterParser.parse(entry.getValue()));
   		}
   		return resolvedParameters;
   	}
   	else {
   		return origParams;
   	}
   }
}

