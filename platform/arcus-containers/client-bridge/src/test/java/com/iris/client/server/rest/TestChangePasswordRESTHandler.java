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

import java.util.UUID;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.bridge.server.noauth.NoAuthModule;
import com.iris.bridge.server.shiro.ShiroClient;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.model.Person;
import com.iris.messages.service.PersonService;
import com.iris.messages.services.PlatformConstants;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ PersonDAO.class, BridgeMetrics.class, FullHttpRequest.class, ChannelHandlerContext.class, PlatformMessageBus.class })
@Modules({NoAuthModule.class})
public class TestChangePasswordRESTHandler extends IrisMockTestCase {

   @Inject
   private PersonDAO personDAO;

   @Inject
   private ChangePasswordRESTHandler handler;

   @Inject
   private FullHttpRequest request;

   @Inject
   private ChannelHandlerContext ctx;
   
   @Inject
   private PlatformMessageBus platformBus;

   @Test
   public void testChangePasswordSuccess() throws Exception {

      FullHttpResponse response = callRestChangePassword("some_email",true,false);

      MessageBody mb = toClientRequest(response);
      assertEquals(BridgeHeaders.CONTENT_TYPE_JSON_UTF8, response.headers().get(HttpHeaders.Names.CONTENT_TYPE));
      assertEquals(HttpResponseStatus.OK,response.getStatus());
      assertEquals(true,PersonService.ChangePasswordResponse.getSuccess(mb));
   }
   
   @Test
   public void testChangePasswordFail() throws Exception {

      FullHttpResponse response = callRestChangePassword("some_email", false,false);
      MessageBody mb = toClientRequest(response);
      assertEquals(HttpResponseStatus.BAD_REQUEST,response.getStatus());
      assertEquals(false,PersonService.ChangePasswordResponse.getSuccess(mb));
   }
   
   @Test
   public void testChangePasswordNoEmailAddressNoLogin() throws Exception {
      FullHttpResponse response = callRestChangePassword("", false,false);
      MessageBody mb = toClientRequest(response);
      assertEquals("Error",mb.getMessageType());
   }
   
   @Test
   public void testChangePasswordNoEmailAddressWithLogin() throws Exception {
      
      FullHttpResponse response = callRestChangePassword("test@test.com", true,true);
      MessageBody mb = toClientRequest(response);
      assertEquals(true,PersonService.ChangePasswordResponse.getSuccess(mb));
   }   
   

   private FullHttpResponse callRestChangePassword(String emailAddress, boolean daoReturn, boolean bindClient) throws Exception {
      Person person = new Person();
      person.setId(UUID.randomUUID());
      EasyMock.expect(request.content()).andReturn(Unpooled.copiedBuffer(generateClientMessage(generateRequest(emailAddress, "old_password", "new_password")).getBytes()));
      EasyMock.expect(personDAO.findByEmail(emailAddress)).andReturn(person).anyTimes();
      EasyMock.expect(personDAO.updatePassword(emailAddress, "old_password", "new_password")).andReturn(daoReturn).anyTimes();
      EasyMock.expect(platformBus.send(EasyMock.anyObject())).andReturn(null).anyTimes();
      EasyMock.expect(this.ctx.channel()).andReturn(new LocalChannel()).anyTimes();
 
      replay();
      
      if(true){
         Client client = EasyMock.createNiceMock(ShiroClient.class);
         EasyMock.expect(client.isAuthenticated()).andReturn(true);
         EasyMock.expect(client.getPrincipalName()).andReturn(emailAddress);
         EasyMock.replay(client);
         Client.bind(ctx.channel(), client);
      }
      
      FullHttpResponse response = handler.respond(request, ctx);
      verify();
      return response;
   }
   
   private MessageBody toClientRequest(FullHttpResponse response) {
      String json = response.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      return clientMessage.getPayload();
   }

   private MessageBody generateRequest(String email, String current, String newPass) {
      return PersonService.ChangePasswordRequest.builder().withEmailAddress(email).withCurrentPassword(current).withNewPassword(newPass).build();
   }

   private String generateClientMessage(MessageBody body) {
      ClientMessage.Builder messageBuilder = ClientMessage.builder().withCorrelationId("").withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE).getRepresentation()).withPayload(body);
      return JSON.toJson(messageBuilder.create());
   }

}

