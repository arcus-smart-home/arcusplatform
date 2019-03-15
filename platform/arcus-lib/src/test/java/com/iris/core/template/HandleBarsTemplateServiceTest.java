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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.collect.ImmutableMap;
import com.iris.resource.Resources;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.resource.filesystem.FileSystemResourceFactory;

public class HandleBarsTemplateServiceTest {

	HandlebarsTemplateService service;
	Map<String,String>context;
	
	@Before
	public void setUp() throws Exception {
		Resources.registerDefaultFactory(new ClassPathResourceFactory());
		Resources.registerFactory(new ClassPathResourceFactory());
		Resources.registerFactory(new FileSystemResourceFactory() );
		
		context = ImmutableMap.<String, String>builder().
		put("test-key", "test-value").build();

		service = new HandlebarsTemplateService("classpath:/com/iris/core/template",1000);

	}

	@Test
	public void testRenderTemplateFromClasspath() {
		String template = service.render("test-template", context);
		assertEquals("This is a template with test-value", template);		
	}
	
	@Test
	public void testInjectCustomHelper() {
		HashMap<String, Helper<? extends Object>> helpers = new HashMap<String, Helper<? extends Object>>();
		helpers.put("testhelper",(Object a, Options o)->{return "hello";});
		service.registerHelpers(helpers);
		String template = service.render("test-customhelper", context);
		assertEquals("hello", template);		
	}
	
	@Test
	public void testRenderMultipartTemplateFromClasspath() {
        Map<String,Object>context2=context.entrySet()
        		.stream()
        		.collect(Collectors.toMap(Map.Entry::getKey, e->e.getValue()));
        
		Map<String,String> template = service.renderMultipart("test-multipart-template", context2);
		assertEquals("Unsectioned", template.get(TemplateService.UNSECTIONED_CONTENT));
		assertEquals("This is section 1 test-value", template.get("section1"));
		assertEquals("This is section 2\n", template.get("section2"));		
	}
}

