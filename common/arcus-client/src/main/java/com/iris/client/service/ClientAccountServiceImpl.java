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
package com.iris.client.service;

import java.io.IOException;
import java.util.UUID;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

// TODO:  jettison due to code generation?  if not fix
public class ClientAccountServiceImpl implements ClientBaseService {
	private final ClientPlatformService platformServices;

    public ClientAccountServiceImpl(ClientPlatformService platformService) {
        this.platformServices = platformService;
    }

    public ListenableFuture signUpStepOne(String firstName, String lastName, String email, String password) throws IOException {
       return Futures.immediateFuture(null);
//
//        return Futures.transform(platformServices.request(ACCOUNT_DESTINATION, new SignUpStepOneRequest(firstName, lastName, email, password)),
//            new Function<MessageBody, SignUpResponse>() {
//                @Override
//                public SignUpResponse apply(MessageBody input) {
//                    SignUpResponse response = new SignUpResponse();
//                    if(input instanceof ErrorEvent) {
//                        response.setErrorCode(((ErrorEvent) input).getCode());
//                        response.setErrorMessage(((ErrorEvent) input).getMessage());
//                    } else if(input instanceof SignUpStepOneResponse) {
//                        response.setAccountId(((SignUpStepOneResponse) input).getAccountId());
//                        response.setPersonId(((SignUpStepOneResponse) input).getPersonId());
//                    } else {
//                        throw new IllegalStateException("Received unexpected response " + input.getClass());
//                    }
//
//                    return response;
//                }
//            });
    }

    public ListenableFuture signUpStepTwo(UUID accountId, UUID personId, String mobileNumber, boolean optInNews) throws IOException {
       return Futures.immediateFuture(null);
//        return Futures.transform(platformServices.request(ACCOUNT_DESTINATION, new SignUpStepTwoRequest(accountId, personId, mobileNumber, optInNews)),
//                new Function<MessageBody, SignUpResponse>() {
//                    @Override
//                    public SignUpResponse apply(MessageBody input) {
//                        SignUpResponse response = new SignUpResponse();
//                        if(input instanceof ErrorEvent) {
//                            response.setErrorCode(((ErrorEvent) input).getCode());
//                            response.setErrorMessage(((ErrorEvent) input).getMessage());
//                        } else if(input instanceof SignUpStepTwoResponse) {
//                            response.setAccountId(((SignUpStepTwoResponse) input).getAccountId());
//                            response.setPersonId(((SignUpStepTwoResponse) input).getPersonId());
//                        } else {
//                            throw new IllegalStateException("Received unexpected response " + input.getClass());
//                        }
//
//                        return response;
//                    }
//                });
    }
}

