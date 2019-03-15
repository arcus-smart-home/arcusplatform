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
/**
 * 
 */
package com.iris.bridge.server.shiro;

import java.util.Optional;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.MockDaoSecurityModule;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.security.Login;
import com.iris.security.SessionConfig;
import com.iris.security.dao.AppHandoffDao;
import com.iris.security.dao.AppHandoffDao.SessionHandoff;
import com.iris.security.dao.AuthenticationDAO;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

import io.netty.buffer.Unpooled;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;

/**
 * 
 */
@Modules({ MockDaoSecurityModule.class, ShiroModule.class })
public class TestShiroAuthenticator extends IrisTestCase {
   ShiroAuthenticator authenticator;
   LocalChannel channel;
   
   @Inject org.apache.shiro.mgt.SecurityManager manager;
   
   @Inject AppHandoffDao appHandoffDao;
   @Inject AuthenticationDAO authenticationDao;
   @Inject SessionDAO sessionDao;
   @Inject SessionConfig config;
   @Inject CookieConfig cookieConfig;
   
   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      this.authenticator = new ShiroAuthenticator(cookieConfig, ServiceLocator.getInstance(ClientFactory.class), new BridgeMetrics("test"), config);
      this.channel = new LocalChannel();
      SecurityUtils.setSecurityManager(manager);
   }
   
   protected void replay() {
      EasyMock.replay(appHandoffDao, authenticationDao, sessionDao);
   }
   
   protected void verify() {
      EasyMock.verify(appHandoffDao, authenticationDao, sessionDao);
   }
   
   @Test
   public void testLogin() throws Exception {
      Login login = new Login();
      login.setUserId(UUID.randomUUID());
      login.setUsername("joe");
      login.setPassword("password");
      
      Capture<Session> sessionRef = Capture.<Session>newInstance();
      
      EasyMock
         .expect(authenticationDao.findLogin("joe"))
         .andReturn(login);
      
      EasyMock
         .expect(sessionDao.create(EasyMock.capture(sessionRef)))
         .andAnswer(() -> {
            SimpleSession value = (SimpleSession) sessionRef.getValue();
            value.setId("session-id");
            return "session-id";
         });
      
      sessionDao.update(EasyMock.capture(sessionRef));
      EasyMock
         .expectLastCall()
         .times(3);

      EasyMock
         .expect(sessionDao.readSession("session-id"))
         .andAnswer(() -> sessionRef.getValue())
         .anyTimes()
         ;
      
      replay();
      
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, 
            HttpMethod.POST, 
            "http://localhost/client",
            Unpooled.wrappedBuffer("{username:\"joe\",password:\"password\"}".getBytes("UTF-8"))
      );
      
      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);
      
      verify();
   }

   @Test
   public void testLoginBadPassword() throws Exception {
      Login login = new Login();
      login.setUserId(UUID.randomUUID());
      login.setUsername("joe");
      login.setPassword("password");
      
      EasyMock
         .expect(authenticationDao.findLogin("joe"))
         .andReturn(login);
      
      replay();
      
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, 
            HttpMethod.POST, 
            "http://localhost/client",
            Unpooled.wrappedBuffer("{username:\"joe\",password:\"wrong\"}".getBytes("UTF-8"))
      );
      
      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.UNAUTHORIZED, response.getStatus());
      assertCookieCleared(response);
      
      verify();
   }

   @Test
   public void testLoginNoSuchUser() throws Exception {
      EasyMock
         .expect(authenticationDao.findLogin("joe"))
         .andReturn(null)
         ;
      
      replay();
      
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, 
            HttpMethod.POST, 
            "http://localhost/client",
            Unpooled.wrappedBuffer("{username:\"joe\",password:\"password\"}".getBytes("UTF-8"))
      );
      
      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.UNAUTHORIZED, response.getStatus());
      assertCookieCleared(response);
      
      verify();
   }

   @Test
   public void testHandoff() throws Exception {
      SessionHandoff handoff = new SessionHandoff();
      handoff.setPersonId(UUID.randomUUID());
      
      Capture<Session> sessionRef = Capture.<Session>newInstance();
      
      EasyMock
         .expect(appHandoffDao.validate("token"))
         .andReturn(Optional.of(handoff));
      
      EasyMock
         .expect(sessionDao.create(EasyMock.capture(sessionRef)))
         .andAnswer(() -> {
            SimpleSession value = (SimpleSession) sessionRef.getValue();
            value.setId("session-id");
            return "session-id";
         });
      
      sessionDao.update(EasyMock.capture(sessionRef));
      EasyMock
         .expectLastCall()
         .times(3);

      EasyMock
         .expect(sessionDao.readSession("session-id"))
         .andAnswer(() -> sessionRef.getValue())
         .anyTimes()
         ;
      
      replay();
      
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, 
            HttpMethod.POST, 
            "http://localhost/client",
            Unpooled.wrappedBuffer("{token:\"token\"}".getBytes("UTF-8"))
      );
      
      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);
      
      verify();
   }

   @Test
   public void testHandoffInvalidToken() throws Exception {
      EasyMock
         .expect(appHandoffDao.validate("token"))
         .andReturn(Optional.empty());
      
      replay();
      
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, 
            HttpMethod.POST, 
            "http://localhost/client",
            Unpooled.wrappedBuffer("{token:\"token\"}".getBytes("UTF-8"))
      );
      
      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.UNAUTHORIZED, response.getStatus());
      assertCookieCleared(response);
      
      verify();
   }

   protected void assertCookieSet(FullHttpResponse response) {
      Cookie cookie = ClientCookieDecoder.STRICT.decode(response.headers().get("Set-Cookie"));
      assertEquals("session-id", cookie.value());
   }

   protected void assertCookieCleared(FullHttpResponse response) {
      Cookie cookie = ClientCookieDecoder.STRICT.decode(response.headers().get("Set-Cookie"));
      assertEquals("", cookie.value());
   }

}

