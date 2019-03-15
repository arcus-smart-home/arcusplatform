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
package com.iris.voice.google.homegraph;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.home.graph.v1.HomeGraphApiServiceGrpc.HomeGraphApiServiceBlockingStub;
import com.google.home.graph.v1.HomeGraphApiServiceProto.ReportStateAndNotificationDevice;
import com.google.home.graph.v1.HomeGraphApiServiceProto.ReportStateAndNotificationRequest;
import com.google.home.graph.v1.HomeGraphApiServiceProto.ReportStateAndNotificationRequest.Builder;
import com.google.home.graph.v1.HomeGraphApiServiceProto.ReportStateAndNotificationResponse;
import com.google.home.graph.v1.HomeGraphApiServiceProto.StateAndNotificationPayload;
import com.google.protobuf.Struct;
import com.iris.google.Constants;
import com.iris.google.Transformers;
import com.iris.messages.model.Model;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.VoiceUtil;
import com.iris.voice.google.GoogleWhitelist;

/**
 * Creates a Report State Request for Google Home.
 * https://developers.google.com/actions/smarthome/report-state
 * 
 *  {
 *    "requestId": "ff36a3cc-ec34-11e6-b1a0-64510650abcf",
 *    "agentUserId": "1234",
 *    "payload": {
 *      "devices": {
 *        "states": {
 *          "1458765": {
 *            "on": true
 *          },
 *          "4578964": {
 *            "on": true,
 *          }
 *        }
 *      }
 *    }
 *  }
 *
 */
public class ReportStateBuilder {

   private GoogleWhitelist whitelist;
   private ProductCatalogManager prodCat;

   private UUID placeId;
   private String requestId; // optional parameter : this is the request id sent from google during an EXECUTE.  We currently are not using it.
   private List<Model> payloadDevices;
   private Boolean hubOffline;
   private HomeGraphApiServiceBlockingStub blockingStub;

   public ReportStateRequest build() {
      verify();
      StateAndNotificationPayload payload = getPayload(this.payloadDevices, this.hubOffline);

      Builder builder = ReportStateAndNotificationRequest.newBuilder();
      builder.setAgentUserId(this.placeId.toString()).setPayload(payload);

      if (this.requestId != null) {
         builder.setRequestId(this.requestId);
      }

      ReportStateAndNotificationRequest request = builder.build();

      return new ReportStateRequest(request);
   }

   public ReportStateBuilder withPlaceId(UUID placeId) {
      this.placeId = placeId;
      return this;
   }

   public ReportStateBuilder withPlaceId(String placeId) {
      this.placeId = UUID.fromString(placeId);
      return this;
   }

   public ReportStateBuilder withWhitelist(GoogleWhitelist whitelist) {
      this.whitelist = whitelist;
      return this;
   }

   public ReportStateBuilder withProdCat(ProductCatalogManager prodCat) {
      this.prodCat = prodCat;
      return this;
   }

   public ReportStateBuilder withRequestId(String requestId) {
      this.requestId = requestId;
      return this;
   }

   public ReportStateBuilder withPayloadDevices(List<Model> payloadDevices) {
      this.payloadDevices = payloadDevices;
      return this;
   }

   public ReportStateBuilder withHubOffline(Boolean hubOffline) {
      this.hubOffline = hubOffline;
      return this;
   }

   public ReportStateBuilder withStub(HomeGraphApiServiceBlockingStub blockingStub) {
      this.blockingStub = blockingStub;
      return this;
   }
   
   private void verify() {
      if (this.placeId == null) {
         throw new IllegalStateException(MessageFormat.format("PlaceId cannot be null [{0}]", this.placeId));
      }
      
      if (this.whitelist == null) {
         throw new IllegalStateException(MessageFormat.format("Whitelist cannot be null [{0}]", this.whitelist));
      }
      
      if (this.prodCat == null) {
         throw new IllegalStateException(MessageFormat.format("Product Catalog cannot be null [{0}]", this.prodCat));
      }
      
      if (CollectionUtils.isEmpty(this.payloadDevices)) {
         throw new IllegalStateException(MessageFormat.format("Devices cannot be null or emtpy [{0}]", this.payloadDevices));
      }
      
      if (this.blockingStub == null) {
         throw new IllegalStateException(MessageFormat.format("The stub cannot be null.  Make sure you have properly defined google_homegraph_grpc_key_file", this.blockingStub));
      }
   }

   private StateAndNotificationPayload getPayload(List<Model> payloadDevices, boolean hubOffline) {
      com.google.protobuf.Struct.Builder builder = Struct.newBuilder();

      payloadDevices.forEach(model -> {
         transformDevices(builder, model);
      });

      ReportStateAndNotificationDevice devices = ReportStateAndNotificationDevice.newBuilder().setStates(builder.build()).build();

      StateAndNotificationPayload payload = StateAndNotificationPayload.newBuilder().setDevices(devices).build();

      return payload;
   }

   private void transformDevices(com.google.protobuf.Struct.Builder builder, Model model) {
      boolean whitelisted = this.whitelist.isWhitelisted(this.placeId);
      ProductCatalogEntry prodCatEntry = VoiceUtil.getProduct(this.prodCat, model);

      if (!StringUtils.isBlank(model.getId())) {
         Map<String, Object> mapOfValue = Transformers.modelToStateMap(model, hubOffline, whitelisted, prodCatEntry);
         if (!mapOfValue.containsKey(Constants.States.ERROR_CODE)) { // modelToStateMap returns an error code when the model isn't supported by Google Voice
            builder.putFields(model.getAddress().toString(), TransformToGoogleValue.transformMapToValue(mapOfValue));
         }
      }
   }

   /**
    * Simple request delegate to return from the builder.
    *
    */
   public class ReportStateRequest {

      private final ReportStateAndNotificationRequest request;

      public ReportStateRequest(ReportStateAndNotificationRequest request) {
         this.request = request;
      }

      public ReportStateAndNotificationResponse send() {
         ReportStateAndNotificationResponse response = blockingStub.reportStateAndNotification(this.request);
         return response;
      }

      public ReportStateAndNotificationRequest getRequest() {
         return this.request;
      }

      @Override
      public String toString() {
         return "ReportStateRequest [request=" + request + "]";
      }
   }

}

