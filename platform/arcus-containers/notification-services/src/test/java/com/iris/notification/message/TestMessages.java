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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.template.EnumHelper;
import com.iris.core.template.HandlebarsHelpersSource;
import com.iris.core.template.TemplateService;
import com.iris.io.json.JSON;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.NotificationPriority;
import com.iris.platform.rule.RuleDao;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.util.TypeMarker;

@Modules({TestMessageModule.class})
@Mocks({DeviceDAO.class, PersonDAO.class, PlaceDAO.class, RuleDao.class})
public class TestMessages extends IrisMockTestCase {
	public static final String MSG_KEY_THING = "thing";
	public static final String MSG_KEY_ENUM = "enum";
	
	static {
		System.setProperty("_atTwitter", "tweet");
		System.setProperty("static.resource.server.url", "http://fakeurl");
	}
	
	@Override
	protected Set<String> configs() {
		Set<String> configs = super.configs();
		configs.add("src/test/resources/test-notification-services.properties");
		return configs;
	}

	@Inject
	private NotificationMessageRenderer messageRenderer;
	
	@Test
	public void testEnumParameterWatering() {
		NotificationBuilder builder = new NotificationBuilder();
		builder.withMessageKey(MSG_KEY_ENUM);
		builder.withPriority(NotificationPriority.MEDIUM);
		builder.withMethod(NotificationMethod.APNS);
		builder.withMessageParam("thing", "WATERING");
		Notification notification = builder.build();
		
		Person recipient = Fixtures.createPerson();
		
		String notificationMessage = messageRenderer.renderMessage(notification, NotificationMethod.APNS, recipient, null);
						
		Map<String,String> msg = JSON.fromJson(notificationMessage, TypeMarker.mapOf(String.class));
		
		System.out.println("title: " + msg.get("title"));
		System.out.println("body:  " + msg.get("body"));
		
		Assert.assertEquals("An Enum thing.", msg.get("title"));
		Assert.assertEquals("The watering thing is watering", msg.get("body"));		
	}
	
	@Test
	public void testEnumParameterNotWatering() {
		NotificationBuilder builder = new NotificationBuilder();
		builder.withMessageKey(MSG_KEY_ENUM);
		builder.withPriority(NotificationPriority.MEDIUM);
		builder.withMethod(NotificationMethod.APNS);
		builder.withMessageParam("thing", "NOT_WATERING");
		Notification notification = builder.build();
		
		Person recipient = Fixtures.createPerson();
		
		String notificationMessage = messageRenderer.renderMessage(notification, NotificationMethod.APNS, recipient, null);
						
		Map<String,String> msg = JSON.fromJson(notificationMessage, TypeMarker.mapOf(String.class));
		
		System.out.println("title: " + msg.get("title"));
		System.out.println("body:  " + msg.get("body"));
		
		Assert.assertEquals("An Enum thing.", msg.get("title"));
		Assert.assertEquals("The watering thing is not watering", msg.get("body"));		
	}
	
	@Test
	public void testEnumParameterOtherValue() {
		NotificationBuilder builder = new NotificationBuilder();
		builder.withMessageKey(MSG_KEY_ENUM);
		builder.withPriority(NotificationPriority.MEDIUM);
		builder.withMethod(NotificationMethod.APNS);
		builder.withMessageParam("thing", "SOME_OTHER_VALUE_NOT_IN_THE_ENUM");
		Notification notification = builder.build();
		
		Person recipient = Fixtures.createPerson();
		
		String notificationMessage = messageRenderer.renderMessage(notification, NotificationMethod.APNS, recipient, null);
						
		Map<String,String> msg = JSON.fromJson(notificationMessage, TypeMarker.mapOf(String.class));
		
		System.out.println("title: " + msg.get("title"));
		System.out.println("body:  " + msg.get("body"));
		
		Assert.assertEquals("An Enum thing.", msg.get("title"));
		Assert.assertEquals("The watering thing some other value not in the enum", msg.get("body"));		
	}

	@Test
	public void testParameterWithQuotes() {
		NotificationBuilder builder = new NotificationBuilder();
		builder.withMessageKey(MSG_KEY_THING);
		builder.withPriority(NotificationPriority.MEDIUM);
		builder.withMethod(NotificationMethod.APNS);
		builder.withMessageParam("thing", "front \"screen\" door");
		Notification notification = builder.build();
		
		Person recipient = Fixtures.createPerson();
		
		String notificationMessage = messageRenderer.renderMessage(notification, NotificationMethod.APNS, recipient, null);
						
		Map<String,String> msg = JSON.fromJson(notificationMessage, TypeMarker.mapOf(String.class));
		
		System.out.println("title: " + msg.get("title"));
		System.out.println("body:  " + msg.get("body"));
		
		Assert.assertEquals("Did Someone Open a Window?", msg.get("title"));
		Assert.assertEquals("front \"screen\" door was opened", msg.get("body"));
		
	}
	
	@Test
	public void testParameterWithoutQuotes() {
		NotificationBuilder builder = new NotificationBuilder();
		builder.withMessageKey(MSG_KEY_THING);
		builder.withPriority(NotificationPriority.MEDIUM);
		builder.withMethod(NotificationMethod.APNS);
		builder.withMessageParam("thing", "front screen door");
		Notification notification = builder.build();
		
		Person recipient = Fixtures.createPerson();
		
		String notificationMessage = messageRenderer.renderMessage(notification, NotificationMethod.APNS, recipient, null);
						
		Map<String,String> msg = JSON.fromJson(notificationMessage, TypeMarker.mapOf(String.class));
		
		System.out.println("title: " + msg.get("title"));
		System.out.println("body:  " + msg.get("body"));
		
		Assert.assertEquals("Did Someone Open a Window?", msg.get("title"));
		Assert.assertEquals("front screen door was opened", msg.get("body"));
		
	}
	
	public static class Templater implements TemplateService {
		private static final String TEMPLATE_ENUM = "{\"title\" : \"An Enum thing.\",\n\"body\"  : \"The watering thing {{{enum thing WATERING=\"is watering\" NOT_WATERING=\"is not watering\"}}}\"}";
		private static final String TEMPLATE_THING = "{\"title\" : \"Did Someone Open a Window?\",\n\"body\"  : \"{{{ thing }}} was opened\"}";
		
		private final Map<String, String> templates = new HashMap<>(); 
		private final Handlebars handlebars;
		
		public Templater() {
			templates.put(MSG_KEY_THING + "-apns", TEMPLATE_THING);
			templates.put(MSG_KEY_ENUM + "-apns", TEMPLATE_ENUM);
			handlebars = new Handlebars()
				.registerHelper(EnumHelper.NAME, EnumHelper.INSTANCE)
				.registerHelpers(HandlebarsHelpersSource.class)
				.registerHelpers(StringHelpers.class);
		}

		@Override
      public String render(String templateId, Object context) {
			Template template;
         try {
	         template = handlebars.compileInline(templates.get(templateId));
	         return template.apply(context);
         } catch (IOException e) {
	         Assert.fail("Unable to apply template");
	         return null;
         }			
      }

		@Override
      public void render(String templateId, Object context, Writer writer) {
			try {
	         writer.write(render(templateId, context));
         } catch (IOException e) {
	         Assert.fail("Unable to write to Writer");
         }	      
      }

		@Override
      public Map<String, String> renderMultipart(String templateId, Object context) {
			// Not Implemented.
	      return null;
      }
		
	}
}

