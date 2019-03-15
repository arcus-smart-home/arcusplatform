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
package com.iris.oauth.auth;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.core.template.HandlebarsTemplateService;
import com.iris.core.template.TemplateService;
import com.iris.messages.errors.ErrorEventException;
import com.iris.oauth.OAuthConfig;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.app.AppRegistry;
import com.iris.oauth.app.Application;
import com.iris.resource.config.ResourceModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({ResourceModule.class})
public class TestSessionAuthDelegate extends IrisTestCase {

   private SessionAuthDelegate delegate;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      OAuthConfig config = new OAuthConfig();
      config.setAppsPath("classpath:///applications.json");

      TemplateService tmplSvc = new HandlebarsTemplateService("classpath:/templates", 1000);
      delegate = new SessionAuthDelegate(null, null, tmplSvc, config, new AppRegistry(config), null);
   }

   @Test(expected=ErrorEventException.class)
   public void testMissingClientIdThrows() {
      delegate.validateAndGetApplication(ImmutableMap.of());
   }

   @Test(expected=ErrorEventException.class)
   public void testInvalidClientIdThrows() {
      delegate.validateAndGetApplication(ImmutableMap.of(OAuthUtil.ATTR_CLIENT_ID, "wrong"));
   }

   @Test(expected=ErrorEventException.class)
   public void testAppSendsRedirectButMissingThrows() {
      delegate.validateAndGetApplication(ImmutableMap.of(OAuthUtil.ATTR_CLIENT_ID, "app2"));
   }

   @Test(expected=ErrorEventException.class)
   public void testAppSendsRedirectButWrongThrows() {
      delegate.validateAndGetApplication(ImmutableMap.of(
            OAuthUtil.ATTR_CLIENT_ID, "app2",
            OAuthUtil.ATTR_REDIRECT_URI, "http://wrong"
      ));
   }

   @Test
   public void testAppSendsRedirectAndItsValid() {
      assertNotNull(delegate.validateAndGetApplication(ImmutableMap.of(
            OAuthUtil.ATTR_CLIENT_ID, "app2",
            OAuthUtil.ATTR_REDIRECT_URI, "http://localhost"
      )));
   }

   @Test
   public void testRenderLogin() {
      Map<String,String> attrs = ImmutableMap.of(OAuthUtil.ATTR_CLIENT_ID, "app1");
      Application app1 = delegate.validateAndGetApplication(attrs);
      assertNotNull(app1);
      String tmpl = delegate.renderTemplate(app1, attrs);
      assertEquals("app1 login - app1", tmpl.trim());
      attrs = ImmutableMap.of(OAuthUtil.ATTR_CLIENT_ID, "app2", OAuthUtil.ATTR_REDIRECT_URI, "http://localhost");
      Application app2 = delegate.validateAndGetApplication(attrs);
      assertNotNull(app2);
      tmpl = delegate.renderTemplate(app2, attrs);
      assertEquals("app2 login - app2", tmpl.trim());
   }
}

