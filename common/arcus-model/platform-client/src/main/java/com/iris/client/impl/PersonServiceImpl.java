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
package com.iris.client.impl;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.PersonService;

/**
 * 
 */
// TODO auto-generate these things
public class PersonServiceImpl implements PersonService {
   private IrisClient client;
   
   /**
    * 
    */
   public PersonServiceImpl(IrisClient client) {
      this.client = client;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getName()
    */
   @Override
   public String getName() {
      return PersonService.NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getAddress()
    */
   @Override
   public String getAddress() {
      return "SERV:" + PersonService.NAMESPACE + ":";
   }

   
   
   /* (non-Javadoc)
    * @see com.iris.client.service.PersonService#sendPasswordReset(java.lang.String, java.lang.String)
    */
   @Override
   public ClientFuture<SendPasswordResetResponse> sendPasswordReset(String email, String method) {
      ClientRequest request = buildRequest(
            PersonService.CMD_SENDPASSWORDRESET,
            ImmutableMap.<String, Object>of(
                  SendPasswordResetRequest.ATTR_EMAIL, email,
                  SendPasswordResetRequest.ATTR_METHOD, method
            )
      );
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, SendPasswordResetResponse>() {
         @Override
         public SendPasswordResetResponse apply(ClientEvent input) {
            return new SendPasswordResetResponse(input);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.PersonService#resetPassword(java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   public ClientFuture<ResetPasswordResponse> resetPassword(String email, String token, String password) {
      ClientRequest request = buildRequest(
            PersonService.CMD_RESETPASSWORD,
            ImmutableMap.<String, Object>of(
                  ResetPasswordRequest.ATTR_EMAIL, email,
                  ResetPasswordRequest.ATTR_TOKEN, token,
                  ResetPasswordRequest.ATTR_PASSWORD, password
            )
      );
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ResetPasswordResponse>() {
         @Override
         public ResetPasswordResponse apply(ClientEvent input) {
            return new ResetPasswordResponse(input);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.PersonService#changePassword(java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   public ClientFuture<ChangePasswordResponse> changePassword(String currentPassword, String newPassword, String emailAddress) {
      ChangePasswordRequest request = defaults(new ChangePasswordRequest());
      request.setCurrentPassword(currentPassword);
      request.setNewPassword(newPassword);
      request.setEmailAddress(emailAddress);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ChangePasswordResponse>() {
         @Override
         public ChangePasswordResponse apply(ClientEvent input) {
            return new ChangePasswordResponse(input);
         }
      });
   }

   // TODO move down to BaseServiceImpl
   protected ClientRequest buildRequest(String commandName, Map<String, Object> attributes) {
      ClientRequest request = new ClientRequest();
      request.setCommand(commandName);
      request.setAttributes(attributes);
      
      return defaults(request);
   }
   
   protected <C extends ClientRequest> C defaults(C request) {
      request.setAddress(getAddress());
      request.setRestfulRequest(true);
      request.setTimeoutMs(30000);
      return request;
   }
}

