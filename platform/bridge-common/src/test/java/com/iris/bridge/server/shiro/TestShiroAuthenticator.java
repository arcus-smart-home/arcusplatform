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
   public void testLoginFormEncoded() throws Exception {
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

      String formBody = "user=joe&password=password";
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            Unpooled.wrappedBuffer(formBody.getBytes("UTF-8"))
      );
      request.headers().set("Content-Type", "application/x-www-form-urlencoded");
      request.headers().set("Content-Length", formBody.length());

      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);

      verify();
   }

   @Test
   public void testLoginFormEncodedWithCharset() throws Exception {
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

      String formBody = "user=joe&password=password";
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            Unpooled.wrappedBuffer(formBody.getBytes("UTF-8"))
      );
      request.headers().set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      request.headers().set("Content-Length", formBody.length());

      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);

      verify();
   }

   @Test
   public void testLoginFormEncodedPasswordFirst() throws Exception {
      Login login = new Login();
      login.setUserId(UUID.randomUUID());
      login.setUsername("joe@example.com");
      login.setPassword("password");

      Capture<Session> sessionRef = Capture.<Session>newInstance();

      EasyMock
         .expect(authenticationDao.findLogin("joe@example.com"))
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

      // Mimics Oculus client: password first, URL-encoded @ in email, no public param
      String formBody = "password=password&user=joe%40example.com";
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            Unpooled.wrappedBuffer(formBody.getBytes("UTF-8"))
      );
      request.headers().set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      request.headers().set("Content-Length", formBody.length());

      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);

      verify();
   }

   @Test
   public void testLoginFormEncodedCompositeByteBuf() throws Exception {
      // Simulates how HttpObjectAggregator assembles the request - content is in a CompositeByteBuf
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

      String formBody = "password=password&user=joe";
      io.netty.buffer.CompositeByteBuf composite = io.netty.buffer.Unpooled.compositeBuffer();
      composite.addComponent(true, Unpooled.wrappedBuffer(formBody.getBytes("UTF-8")));

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            composite
      );
      request.headers().set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      request.headers().set("Content-Length", formBody.length());

      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);

      verify();
   }

   @Test
   public void testLoginFormEncodedThroughAggregator() throws Exception {
      // Simulates request going through HttpRequestDecoder + HttpObjectAggregator pipeline
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

      // Build a raw HTTP request as bytes and decode through the Netty pipeline
      String formBody = "password=password&user=joe";
      String rawHttp = "POST /login HTTP/1.1\r\n" +
            "Host: localhost\r\n" +
            "Connection: keep-alive\r\n" +
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n" +
            "Content-Length: " + formBody.length() + "\r\n" +
            "\r\n" +
            formBody;

      io.netty.channel.embedded.EmbeddedChannel decoderChannel = new io.netty.channel.embedded.EmbeddedChannel(
            new io.netty.handler.codec.http.HttpRequestDecoder(),
            new io.netty.handler.codec.http.HttpObjectAggregator(65536)
      );
      decoderChannel.writeInbound(Unpooled.wrappedBuffer(rawHttp.getBytes("UTF-8")));
      io.netty.handler.codec.http.FullHttpRequest decoded = decoderChannel.readInbound();
      assertNotNull("Aggregated request should not be null", decoded);

      FullHttpResponse response = authenticator.authenticateRequest(channel, decoded);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);

      decoded.release();
      decoderChannel.close();

      verify();
   }

   @Test
   public void testLoginFormEncodedGzipBodyWithDecompressor() throws Exception {
      // Simulates nginx gzip-compressing the request body before forwarding.
      // With HttpContentDecompressor in the pipeline, the body is decompressed before extractToken().
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

      String formBody = "user=joe&password=password";
      byte[] uncompressed = formBody.getBytes("UTF-8");

      // Gzip compress the body
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(baos);
      gzip.write(uncompressed);
      gzip.close();
      byte[] compressed = baos.toByteArray();

      // Build raw HTTP with gzip body and run through pipeline with decompressor
      String headers = "POST /login HTTP/1.1\r\n" +
            "Host: localhost\r\n" +
            "Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\n" +
            "Content-Encoding: gzip\r\n" +
            "Content-Length: " + compressed.length + "\r\n" +
            "\r\n";

      io.netty.buffer.ByteBuf raw = Unpooled.copiedBuffer(
            Unpooled.wrappedBuffer(headers.getBytes("UTF-8")),
            Unpooled.wrappedBuffer(compressed)
      );

      io.netty.channel.embedded.EmbeddedChannel decoderChannel = new io.netty.channel.embedded.EmbeddedChannel(
            new io.netty.handler.codec.http.HttpRequestDecoder(),
            new io.netty.handler.codec.http.HttpContentDecompressor(),
            new io.netty.handler.codec.http.HttpObjectAggregator(65536)
      );
      decoderChannel.writeInbound(raw);
      io.netty.handler.codec.http.FullHttpRequest decoded = decoderChannel.readInbound();
      assertNotNull("Aggregated request should not be null", decoded);

      FullHttpResponse response = authenticator.authenticateRequest(channel, decoded);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);

      decoded.release();
      decoderChannel.close();

      verify();
   }

   @Test
   public void testLoginFormEncodedReaderIndexAdvanced() throws Exception {
      // Reproduces production bug: upstream handler consumes body, advancing readerIndex to end
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

      String formBody = "password=password&user=joe";
      io.netty.buffer.CompositeByteBuf composite = io.netty.buffer.Unpooled.compositeBuffer();
      composite.addComponent(true, Unpooled.wrappedBuffer(formBody.getBytes("UTF-8")));
      // Simulate upstream handler reading all content
      composite.markReaderIndex();
      composite.skipBytes(composite.readableBytes());
      assertEquals(0, composite.readableBytes());

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            composite
      );
      request.headers().set("Content-Type", "application/x-www-form-urlencoded");
      request.headers().set("Content-Length", formBody.length());

      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      assertCookieSet(response);

      verify();
   }

   @Test
   public void testLoginUnparseableBody() throws Exception {
      // When the body can't be parsed as form data or JSON, should return 400 not 401
      replay();

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            Unpooled.wrappedBuffer("not-valid-anything".getBytes("UTF-8"))
      );
      request.headers().set("Content-Type", "application/x-www-form-urlencoded");
      request.headers().set("Content-Length", 18);

      FullHttpResponse response = authenticator.authenticateRequest(channel, request);
      assertEquals(HttpResponseStatus.BAD_REQUEST, response.getStatus());

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

   @Test
   public void testExtractTokenJsonPublicTrue() throws Exception {
      replay();

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            Unpooled.wrappedBuffer("{\"username\":\"joe\",\"password\":\"password\",\"public\":\"true\"}".getBytes("UTF-8"))
      );

      org.apache.shiro.authc.AuthenticationToken token = authenticator.extractToken(channel, request);
      assertNotNull(token);
      assertTrue(token instanceof org.apache.shiro.authc.UsernamePasswordToken);
      org.apache.shiro.authc.UsernamePasswordToken upToken = (org.apache.shiro.authc.UsernamePasswordToken) token;
      assertEquals("joe", upToken.getUsername());
      assertFalse("public=true should set rememberMe to false", upToken.isRememberMe());

      verify();
   }

   @Test
   public void testExtractTokenJsonPublicNotSet() throws Exception {
      replay();

      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            Unpooled.wrappedBuffer("{\"username\":\"joe\",\"password\":\"password\"}".getBytes("UTF-8"))
      );

      org.apache.shiro.authc.AuthenticationToken token = authenticator.extractToken(channel, request);
      assertNotNull(token);
      assertTrue(token instanceof org.apache.shiro.authc.UsernamePasswordToken);
      org.apache.shiro.authc.UsernamePasswordToken upToken = (org.apache.shiro.authc.UsernamePasswordToken) token;
      assertEquals("joe", upToken.getUsername());
      assertTrue("no public field should default rememberMe to true", upToken.isRememberMe());

      verify();
   }

   @Test
   public void testExtractTokenFormEncodedPublicTrue() throws Exception {
      replay();

      String formBody = "user=joe&password=password&public=true";
      DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            "http://localhost/client",
            Unpooled.wrappedBuffer(formBody.getBytes("UTF-8"))
      );
      request.headers().set("Content-Type", "application/x-www-form-urlencoded");
      request.headers().set("Content-Length", formBody.length());

      org.apache.shiro.authc.AuthenticationToken token = authenticator.extractToken(channel, request);
      assertNotNull(token);
      assertTrue(token instanceof org.apache.shiro.authc.UsernamePasswordToken);
      org.apache.shiro.authc.UsernamePasswordToken upToken = (org.apache.shiro.authc.UsernamePasswordToken) token;
      assertEquals("joe", upToken.getUsername());
      assertFalse("public=true should set rememberMe to false", upToken.isRememberMe());

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

