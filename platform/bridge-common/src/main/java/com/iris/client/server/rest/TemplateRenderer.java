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
package com.iris.client.server.rest;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.template.TemplateService;

@Singleton
public class TemplateRenderer {
	public static final String SSR_KEY="_ssr";
	public static final String AKA_KEY="_aka";
	public static final String TEMPLATE_KEY="template";
	
	private static String TEMPLATE_NAME_V2_SUFFIX = "-v2";
	
	
	private final static String RESOURCE_SERVER_URL_KEY="static.resource.server.url";
	@Inject @Named(RESOURCE_SERVER_URL_KEY) String staticResourceServerUrl;
	
	private final static String REDIRECT_BASE_URL="redirect.base.url";
	@Inject @Named(REDIRECT_BASE_URL) String akaUrl;
	
	private TemplateService templateService;
	private Map<String, String> commonParameters;
	
	@Inject
   public TemplateRenderer(TemplateService templateService) {
   	this.templateService = templateService;
   }
	
	@PostConstruct
   public void init() {
	  ImmutableMap.Builder<String, String> paramBuilder = ImmutableMap.builder(); 
	  paramBuilder.put(SSR_KEY, staticResourceServerUrl);
	  paramBuilder.put(AKA_KEY, akaUrl);
	  
	  this.commonParameters = paramBuilder.build();
   }
	
   public String render(String templateName, Map<String, Object> additionalParams) { 		      
      return render(true, templateName, additionalParams);
   }
   
   public String render(boolean checkTemlateVersion, String templateName, Map<String, Object> additionalParams) {
		Map<String, Object> context = new HashMap<String, Object>();
		addParams(context, additionalParams);  
		if(additionalParams.getOrDefault(TEMPLATE_KEY, null) != null) {     
			templateName = templateName + TEMPLATE_NAME_V2_SUFFIX;      
		}
		return templateService.render(templateName, context);
   }
	
   
   private void addParams(Map<String, Object> context, Map<String, Object> additionalParams) {   	  
      context.putAll(commonParameters);
      context.putAll(additionalParams);
   }
}

