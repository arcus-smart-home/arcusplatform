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
package com.iris.bridge.server.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.MockDaoSecurityModule;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.shiro.ShiroModule;
import com.iris.security.dao.AuthenticationDAO;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * 
 */
@Modules({ MockDaoSecurityModule.class, ShiroModule.class })
public class TestBindClientContextHandler extends IrisTestCase {

   BindClientContextHandler handler;
   ChannelHandlerContext context;
   LocalChannel channel;
   
   @Inject org.apache.shiro.mgt.SecurityManager manager;
   
   @Inject RequestAuthorizer requestAuthorizer;
   @Inject ClientFactory registry;
   @Inject AuthenticationDAO authenticationDao;
   @Inject SessionDAO sessionDao;
   @Inject CookieConfig cookieConfig;
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      this.handler = new BindClientContextHandler(cookieConfig, registry, requestAuthorizer);
      this.channel = new LocalChannel();
      this.context = EasyMock.createNiceMock(ChannelHandlerContext.class);
      EasyMock
         .expect(this.context.channel())
         .andReturn(this.channel)
         .anyTimes();
      SecurityUtils.setSecurityManager(manager);
   }
   
   protected void replay() {
      EasyMock.replay(authenticationDao, sessionDao, context);
   }
   
   protected void verify() {
      EasyMock.verify(authenticationDao, sessionDao, context);
   }
   
   @Test
   public void testBindNoHeaders() throws Exception {
      replay();
      
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost/client");
      handler.channelRead(context, request);
      
      // an un-authenticated Client should have been bound
      ClientFactory factory = ServiceLocator.getInstance(ClientFactory.class);
      Client client = factory.get(channel);
      assertNotNull(client);
      assertFalse(client.isAuthenticated());
      
      verify();
   }

   @Test
   public void testBindByCookie() throws Exception {
      SimpleSession session = new SimpleSession();
      session.setId("test");
      session.setExpired(false);
      session.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, true);
      
      EasyMock
         .expect(sessionDao.readSession("test"))
         .andReturn(session)
         // FIXME why does shiro load the session so many times????
         .anyTimes();
      
      sessionDao.update(session);
      EasyMock
         .expectLastCall()
         .times(1);
      
      replay();

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost/client");
      DefaultHttpHeaders.addHeader(request, "Cookie", "irisAuthToken=test;");
      handler.channelRead(context, request);
      
      
      // an authenticated Client should have been bound
      ClientFactory factory = ServiceLocator.getInstance(ClientFactory.class);
      Client client = factory.get(channel);
      assertNotNull(client);
      assertTrue(client.isAuthenticated());
      assertEquals("test", client.getSessionId());

      verify();
   }
   
   @Test
   public void testBindByAuthHeader() throws Exception {
      SimpleSession session = new SimpleSession();
      session.setId("test");
      session.setExpired(false);
      session.setAttribute(DefaultSubjectContext.AUTHENTICATED_SESSION_KEY, true);
      
      EasyMock
         .expect(sessionDao.readSession("test"))
         .andReturn(session)
         // FIXME why does shiro load the session so many times????
         .anyTimes();
      
      sessionDao.update(session);
      EasyMock
         .expectLastCall()
         .times(1);
      
      replay();

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost/client");
      DefaultHttpHeaders.addHeader(request, "Authorization", "test");
      handler.channelRead(context, request);
      
      
      // an authenticated Client should have been bound
      ClientFactory factory = ServiceLocator.getInstance(ClientFactory.class);
      Client client = factory.get(channel);
      assertNotNull(client);
      assertTrue(client.isAuthenticated());
      assertEquals("test", client.getSessionId());

      verify();
   }
   
   @Test
   public void testBindUnknownSession() throws Exception {
      EasyMock
         .expect(sessionDao.readSession("test"))
         .andThrow(new UnknownSessionException());
      replay();

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost/client");
      DefaultHttpHeaders.addHeader(request, "Cookie", "irisAuthToken=test;");
      handler.channelRead(context, request);
      
      
      // an authenticated Client should have been bound
      ClientFactory factory = ServiceLocator.getInstance(ClientFactory.class);
      Client client = factory.get(channel);
      assertNotNull(client);
      assertFalse(client.isAuthenticated());
      assertEquals(null, client.getSessionId());

      verify();
   }


}

