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
package com.iris.core.template;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

public class HandlebarsTemplateService implements TemplateService {
	
	public static final String SERVICE_NAME = "templates";
	private static final Logger LOGGER = LoggerFactory.getLogger(HandlebarsTemplateService.class);
	private static final IrisMetricSet METRICS = IrisMetrics.metrics(SERVICE_NAME);
	
	private final static Pattern sectionPattern = Pattern.compile("^---(.*?)---$\n(.*?)\n!--(.*?)---",Pattern.DOTALL|Pattern.MULTILINE);
	
	TemplateCache templateCache = null;
	Handlebars handlebars = null;
	String prefix=null;
	
	public HandlebarsTemplateService(String prefix,int cacheSize) {
		init(prefix,cacheSize);
	}
	
	public void registerHelpers(Map<String,Helper<? extends Object>>helpers) {
		if(!helpers.isEmpty()){
			helpers.entrySet().stream().forEach(e->
				handlebars.registerHelper(e.getKey(),e.getValue())
			);
		}
	}
	
    private void incrementMetricForErrors(String templateId, String errorType) {
        METRICS.counter(String.format("template.render.error.%s",errorType.toLowerCase())).inc();
        METRICS.counter(String.format("template.%s.error.%s",templateId.toLowerCase(),errorType.toLowerCase())).inc();
    }

	private void init(String prefix, int cacheSize){
		LOGGER.info("Handlebars Template Service Initialization with a location of {} and cache size of {}",prefix,cacheSize);
		templateCache = new ConcurrentMapTemplateCache();		
		
		TemplateLoader loader=new IrisResourceLoader(prefix, cacheSize);
		handlebars=new Handlebars(loader)
			.with(templateCache)
			.registerHelper(EnumHelper.NAME, EnumHelper.INSTANCE)
			.registerHelper(IfEqualHelper.NAME, IfEqualHelper.INSTANCE)
			.registerHelpers(HandlebarsHelpersSource.class)
			.registerHelpers(StringHelpers.class)
			.registerHelper(PluralizeHelper.NAME, PluralizeHelper.INSTANCE);
	}
	
	@Override
	public String render(String templateId, Object context) {
		StringWriter writer = new StringWriter();
		render(templateId,context,writer);
		return writer.toString();
	}
	
	@Override
	public void render(String templateId, Object context, Writer output) {
		final Timer renderTimer = METRICS.timer(String.format("template.%s.render",templateId));
		try {
			Template template = handlebars.compile(templateId);
			Context timer = renderTimer.time();
	        template.apply(context,output);
	        timer.stop();
		} catch (Throwable e) {
			incrementMetricForErrors(templateId,e.getClass().getSimpleName());
			throw new RuntimeException(String.format("Error rendering or compiling handlebar template with message %s",e.getMessage()),e);
		}
	}
	
	@Override
	public Map<String,String> renderMultipart(String templateId, Object context) {
		String content = render(templateId,context);
		Map<String,String>multipartContent=parseSections(content);
		return multipartContent;
	}
	
	private Map<String,String>parseSections(String content){
		HashMap<String,String>multipartTemplate=new LinkedHashMap<String,String>();
		Matcher match = sectionPattern.matcher(content);
		while(match.find()){
			multipartTemplate.put(match.group(1),match.group(2));
		}
		multipartTemplate.put(TemplateService.UNSECTIONED_CONTENT,sectionPattern.matcher(content).replaceAll("").trim());
		return multipartTemplate;
	}
}

