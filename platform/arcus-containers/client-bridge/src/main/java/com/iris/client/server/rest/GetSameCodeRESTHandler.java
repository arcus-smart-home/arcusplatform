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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.handlers.RESTHandler;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.client.nws.SameCodeManager;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.NwsSameCodeService;
import com.iris.messages.service.NwsSameCodeService.GetSameCodeRequest;

@Singleton
@HttpPost("/" + NwsSameCodeService.NAMESPACE + "/GetSameCode")
public class GetSameCodeRESTHandler extends RESTHandler {
   
   private final SameCodeManager manager;

   @Inject
   public GetSameCodeRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, SameCodeManager manager, RESTHandlerConfig restHandlerConfig) {
      super(alwaysAllow, new HttpSender(GetSameCodeRESTHandler.class,metrics), restHandlerConfig);
      this.manager = manager;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request) throws Exception {
      MessageBody payload = request.getPayload();
      String stateCode = NwsSameCodeService.GetSameCodeRequest.getStateCode(payload);
      Errors.assertRequiredParam(stateCode, GetSameCodeRequest.ATTR_STATECODE);

      String county = NwsSameCodeService.GetSameCodeRequest.getCounty(payload);
      Errors.assertRequiredParam(county, GetSameCodeRequest.ATTR_COUNTY);
      
      String code = manager.getSameCode(stateCode, county);
      
      return NwsSameCodeService.GetSameCodeResponse.builder().withCode(code).build();
   }

}

