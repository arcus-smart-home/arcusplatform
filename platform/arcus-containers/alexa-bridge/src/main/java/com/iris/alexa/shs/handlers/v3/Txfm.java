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
package com.iris.alexa.shs.handlers.v3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Endpoint;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.Scope;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaEndpoint;

class Txfm {

   private Txfm() {
   }

   final PlatformMessage txfmRequest(AlexaMessage message, UUID placeId, String population, int ttl) {
      return PlatformMessage.buildRequest(
         txfmRequestBody(message),
         AlexaUtil.ADDRESS_BRIDGE,
         Address.platformService(AlexaService.NAMESPACE)
      )
      .withTimeToLive(ttl)
      .withCorrelationId(message.getHeader().getMessageId())
      .withPlaceId(placeId)
      .withPopulation(population)
      .create();
   }

   @SuppressWarnings("unchecked")
   MessageBody txfmRequestBody(AlexaMessage message) {
      return AlexaService.ExecuteRequest.builder()
         .withDirective(message.getHeader().getName())
         .withCorrelationToken(message.getHeader().getCorrelationToken())
         .withArguments((Map<String, Object>) message.getPayload())
         .withTarget(extractRequestTarget(message))
         .withAllowDeferred(true)
         .build();
   }

   String extractRequestTarget(AlexaMessage message) {
      Endpoint endpoint = message.getEndpoint();
      if(endpoint == null || endpoint.getEndpointId() == null) {
         throw new AlexaException(AlexaErrors.invalidDirective("Directive was missing its endpoint"));
      }
      return endpoint.getEndpointId();
   }

   Optional<String> extractRequestOauthToken(AlexaMessage message) {
      Endpoint endpoint = message.getEndpoint();
      if(endpoint == null) {
         return Optional.empty();
      }
      Scope scope = endpoint.getScope();
      if(scope == null) {
         return Optional.empty();
      }
      return Optional.ofNullable(scope.getToken());
   }

   final AlexaMessage transformResponse(PlatformMessage response, @Nullable String correlationToken) {
      MessageBody body = response.getValue();
      if(AlexaService.AlexaErrorEvent.NAME.equals(body.getMessageType())) {
         throw new AlexaException(body);
      }

      if(Boolean.TRUE.equals(AlexaService.ExecuteResponse.getDeferred(body))) {
         Header h = Header.v3(response.getCorrelationId(), AlexaInterfaces.RESPONSE_DEFERRED, responseNamespace(), correlationToken);
         return new AlexaMessage(h, ImmutableMap.of("estimatedDeferralInSeconds", 30));
      }

      Header header = Header.v3(response.getCorrelationId(), responseName(), responseNamespace(), correlationToken);
      return new AlexaMessage(header, responsePayload(body), null, responseContext(body));
   }

   String responseNamespace() {
      return AlexaInterfaces.RESPONSE_NAMESPACE;
   }

   String responseName() {
      return AlexaInterfaces.RESPONSE_NAME;
   }

   Object responsePayload(MessageBody response) {
      return AlexaService.ExecuteResponse.getPayload(response);
   }

   @Nullable
   Map<String, Object> responseContext(MessageBody response) {
      ImmutableMap.Builder<String, Object> context = ImmutableMap.builder();
      if(AlexaService.ExecuteResponse.getProperties(response) != null) {
         context.put("properties", AlexaService.ExecuteResponse.getProperties(response));
      }
      return context.build();
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Discover Implementation
   //-------------------------------------------------------------------------------------------------------------------

   private static final Txfm discover = new Txfm() {
      @Override
      MessageBody txfmRequestBody(AlexaMessage message) {
         return AlexaService.DiscoverRequest.instance();
      }

      @SuppressWarnings("unchecked")
      @Override
      Optional<String> extractRequestOauthToken(AlexaMessage message) {
         Map<String, Object> payload = (Map<String, Object>) message.getPayload();
         if(payload == null) {
            return Optional.empty();
         }
         Object scope = payload.get("scope");
         if(!(scope instanceof Map)) {
            return Optional.empty();
         }
         return Optional.ofNullable(Scope.fromMap((Map) scope).getToken());
      }

      @Override
      String responseNamespace() {
         return AlexaInterfaces.Discovery.NAMESPACE;
      }

      @Override
      String responseName() {
         return AlexaInterfaces.Discovery.RESPONSE_DISCOVER;
      }

      @Override
      Object responsePayload(MessageBody response) {
         List<Map<String,Object>> endpoints = AlexaService.DiscoverResponse.getEndpoints(response);

         if(endpoints == null) {
            return ImmutableMap.of("endpoints", ImmutableList.of());
         }

         // strip out the online state and model, that is used in v2 but not v3
         return ImmutableMap.of(
            "endpoints",
            endpoints.stream().map(m -> {
               Map<String, Object> attrs = new HashMap<>(m);
               attrs.remove(AlexaEndpoint.ATTR_ONLINE);
               attrs.remove(AlexaEndpoint.ATTR_MODEL);
               return attrs;
            })
            .collect(Collectors.toList())
         );
      }

      @Nullable
      @Override
      Map<String, Object> responseContext(MessageBody response) {
         return null;
      }
   };

   //-------------------------------------------------------------------------------------------------------------------
   // Activate Implementation
   //-------------------------------------------------------------------------------------------------------------------

   private static final Txfm activate = new Txfm() {
      @Override
      String extractRequestTarget(AlexaMessage message) {
         Endpoint endpoint = message.getEndpoint();
         if(endpoint == null || endpoint.getEndpointId() == null) {
            throw new AlexaException(AlexaErrors.invalidDirective("Directive was missing its endpoint"));
         }
         String endpointId = endpoint.getEndpointId();
         return AlexaUtil.endpointIdToAddress(endpointId);
      }

      @Override
      String responseNamespace() {
         return AlexaInterfaces.SceneController.NAMESPACE;
      }

      @Override
      String responseName() {
         return AlexaInterfaces.SceneController.RESPONSE_ACTIVATE;
      }

   };

   //-------------------------------------------------------------------------------------------------------------------
   // StateReport Implementation
   //-------------------------------------------------------------------------------------------------------------------

   private static final Txfm reportState = new Txfm() {
      @Override
      String responseName() {
         return AlexaInterfaces.RESPONSE_REPORTSTATE;
      }
   };

   //-------------------------------------------------------------------------------------------------------------------
   // AcceptGrant Implementation
   //-------------------------------------------------------------------------------------------------------------------

   private static final Txfm acceptGrant = new Txfm() {
      @SuppressWarnings("unchecked")
      @Override
      MessageBody txfmRequestBody(AlexaMessage message) {
         Map<String, Object> payload = (Map<String, Object>) message.getPayload();
         Map<String, Object> grant = (Map<String, Object>) payload.get(AlexaInterfaces.Authorization.ARG_GRANT);
         if(grant == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.Authorization.ARG_GRANT));
         }
         String code = (String) grant.get(AlexaService.AcceptGrantRequest.ATTR_CODE);
         if(code == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.Authorization.ARG_GRANT + ".code"));
         }

         return AlexaService.AcceptGrantRequest.builder().withCode(code).build();
      }

      @SuppressWarnings("unchecked")
      @Override
      Optional<String> extractRequestOauthToken(AlexaMessage message) {
         Map<String, Object> payload = (Map<String, Object>) message.getPayload();
         if(payload == null) {
            return Optional.empty();
         }
         Map<String, Object> grantee = (Map<String, Object>) payload.get(AlexaInterfaces.Authorization.ARG_GRANTEE);
         if(grantee == null) {
            return Optional.empty();
         }
         return Optional.ofNullable((String) grantee.get("token"));
      }

      @Override
      String responseNamespace() {
         return AlexaInterfaces.Authorization.NAMESPACE;
      }

      @Override
      String responseName() {
         return AlexaInterfaces.Authorization.RESPONSE_ACCEPTGRANT;
      }
   };

   //-------------------------------------------------------------------------------------------------------------------
   // Fallback that covers the majority of requests
   //-------------------------------------------------------------------------------------------------------------------

   private static final Txfm fallback = new Txfm();

   //-------------------------------------------------------------------------------------------------------------------
   // Factory Method
   //-------------------------------------------------------------------------------------------------------------------

   static Txfm transformerFor(AlexaMessage message) {
      switch(message.getHeader().getName()) {
         case AlexaInterfaces.Discovery.REQUEST_DISCOVER: return discover;
         case AlexaInterfaces.SceneController.REQUEST_ACTIVATE: return activate;
         case AlexaInterfaces.Authorization.REQUEST_ACCEPTGRANT: return acceptGrant;
         case AlexaInterfaces.REQUEST_REPORTSTATE: return reportState;
         default: return fallback;
      }
   }
}

