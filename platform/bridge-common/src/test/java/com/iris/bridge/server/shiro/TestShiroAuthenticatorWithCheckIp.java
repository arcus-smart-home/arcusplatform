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

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.MockDaoSecurityModule;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.security.SessionConfig;
import com.iris.security.dao.AppHandoffDao;
import com.iris.security.dao.AppHandoffDao.SessionHandoff;
import com.iris.security.dao.AuthenticationDAO;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.Attribute;
import io.netty.util.DefaultAttributeMap;

/**
 * This test basically test scenario when AppHandoffRealm.checkSameIp is set to true
 */
@Mocks({Channel.class, 
	ChannelHandlerContext.class,
	Client.class})
@Modules({ MockDaoSecurityModule.class })
public class TestShiroAuthenticatorWithCheckIp extends IrisMockTestCase {
   ShiroAuthenticator authenticator;
   @Inject
   private Channel channel;
   @Inject
   private Client client;
   
   @Inject
   private ChannelHandlerContext context;
   
   
   @Inject org.apache.shiro.mgt.SecurityManager manager;
   
   @Inject AppHandoffDao appHandoffDao;
   @Inject AuthenticationDAO authenticationDao;
   @Inject SessionDAO sessionDao;
   @Inject SessionConfig config;
   @Inject CookieConfig cookieConfig;
   
   @Override
	protected Set<String> configs() {
		Set<String> configs = super.configs();
		configs.add("src/test/resources/test-security.properties");
		return configs;
	}
   
   
   
   @Override
	protected void configure(Binder binder)
	{
		binder.bind(Authenticator.class).to(ShiroAuthenticator.class);
		binder.bind(ClientFactory.class).to(ShiroClientRegistry.class);
		super.configure(binder);
	}



	@Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      this.authenticator = new ShiroAuthenticator(cookieConfig, ServiceLocator.getInstance(ClientFactory.class), new BridgeMetrics("test"), config);
      
      SecurityUtils.setSecurityManager(manager);
   }
   
   protected void replay() {
      EasyMock.replay(appHandoffDao, authenticationDao, sessionDao, channel, client, context);
   }
   
   protected void verify() {
      EasyMock.verify(appHandoffDao, authenticationDao, sessionDao, channel, client, context);
   }
   

   @Test
   public void testHandoffSuccess() throws Exception {  	
      String ip = "192.12.0.1";
      doHandoff(ip, ip, HttpResponseStatus.OK, true);      
   }
   
   @Test
   public void testHandoffFail1() throws Exception {  	
      String ip = "192.12.0.1";
      String ip2 = "192.12.0.0";  //not matching
      doHandoff(ip, ip2, HttpResponseStatus.UNAUTHORIZED, false);      
   }
   
   @Test
   public void testHandoffFail2() throws Exception {  	
      String ip = null;
      String ip2 = null;  //both null
      doHandoff(ip, ip2, HttpResponseStatus.UNAUTHORIZED, false);      
   }
   
   @Test
   public void testHandoffFail3() throws Exception {  	
      String ip = "192.12.0.1";
      String ip2 = null;  //one null
      doHandoff(ip, ip2, HttpResponseStatus.UNAUTHORIZED, false);      
   }
   
   @Test
   public void testHandoffFail4() throws Exception {  	
      String ip = null;
      String ip2 = "192.12.0.1";  //one null
      doHandoff(ip, ip2, HttpResponseStatus.UNAUTHORIZED, false);      
   }
   
   
   private void doHandoff(String ip, String matchingIp, HttpResponseStatus responseStatus, boolean isSuccess) throws UnsupportedEncodingException {
   	if(StringUtils.isBlank(ip)) {
   		EasyMock.expect(channel.remoteAddress()).andReturn(null).anyTimes();
   	}else{
   		EasyMock.expect(channel.remoteAddress()).andReturn(new InetSocketAddress(ip, 33213)).anyTimes();
   	}
      SessionHandoff handoff = new SessionHandoff();
      handoff.setPersonId(UUID.randomUUID());
      handoff.setIp(matchingIp);
      
      Capture<Session> sessionRef = Capture.<Session>newInstance();
      
      EasyMock
         .expect(appHandoffDao.validate("token"))
         .andReturn(Optional.of(handoff));
      
      if(isSuccess) {
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
      }

      EasyMock
         .expect(sessionDao.readSession("session-id"))
         .andAnswer(() -> sessionRef.getValue())
         .anyTimes()
         ;
      Attribute<Client> attribMap = new DefaultAttributeMap().attr(Client.ATTR_CLIENT);
      EasyMock.expect(channel.attr(Client.ATTR_CLIENT)).andReturn(attribMap).times(2);
      
      replay();
      
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, 
            HttpMethod.POST, 
            "http://localhost/client",
            Unpooled.wrappedBuffer("{token:\"token\"}".getBytes("UTF-8"))
      );
      
      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(responseStatus, response.getStatus());
      if(isSuccess) {
      	assertCookieSet(response);
      }else{
      	assertCookieCleared(response);
      }
      
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

