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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.home.graph.v1.HomeGraphApiServiceGrpc;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.resource.Resource;
import com.iris.resource.Resources;
import com.iris.voice.google.GoogleConfig;
import com.iris.voice.google.GoogleWhitelist;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;

/**
 * All the relative pieces for making HomeGraph API calls using gRPC.  Currently only ReportState uses this method.
 */
@Singleton
public class GoogleRpcContext {

   private final boolean reportStateEnabled;
   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private GoogleCredentials creds;
   // Google says that building a channel and stub is a heavy operation.  They want us to hold onto the stub for multiple requests.
   private HomeGraphApiServiceGrpc.HomeGraphApiServiceBlockingStub blockingStub;

   @Inject
   public GoogleRpcContext(GoogleConfig config, GoogleWhitelist whitelist, ProductCatalogManager prodCat) throws IOException {
      this.reportStateEnabled = config.isReportStateEnabled();
      this.whitelist = whitelist;
      this.prodCat = prodCat;

      if (this.reportStateEnabled) {
         setupCredentials(config);
         setupStub();
      }
   }

   /**
    * Get a ReportState builder pre-seeded with the singletons it needs.
    */
   public ReportStateBuilder getRequestBuilder() {

      if (!this.reportStateEnabled) {
         throw new IllegalStateException("Report State is disabled. The blocking stub hasn't been properly initialized.");
      }

      // @formatter:off
      ReportStateBuilder builder = new ReportStateBuilder()
            .withProdCat(this.prodCat)
            .withWhitelist(this.whitelist)
            .withStub(this.blockingStub);
      // @formatter:on

      return builder;
   }

   /**
    * Read in the JSON file downloaded from the Google Service Account Token Creator
    */
   private void setupCredentials(GoogleConfig config) throws IOException {
      Resource resource = Resources.getResource(config.getgRpcKeyFile());

      if (resource.exists()) {
         try (InputStream is = resource.open()) {
            this.creds = GoogleCredentials.fromStream(is);
         }
      }
      else {
         throw new IllegalStateException(MessageFormat.format("Report State has enabled=[{0}], but the gRPC Key File used for authentication does not exist. [{1}]", config.isReportStateEnabled(), config.getgRpcKeyFile()));
      }
   }

   /**
    * Build the HomeGraph stub for making rpc requests to google.
    */
   private void setupStub() {
      ManagedChannel channel = ManagedChannelBuilder.forTarget("homegraph.googleapis.com").build();

      this.blockingStub = HomeGraphApiServiceGrpc.newBlockingStub(channel)
            // See https://grpc.io/docs/guides/auth.html#authenticate-with-google-3.
            .withCallCredentials(MoreCallCredentials.from(this.creds));
   }

   public GoogleWhitelist getWhitelist() {
      return this.whitelist;
   }

   public ProductCatalogManager getProdCat() {
      return this.prodCat;
   }

   public GoogleCredentials getCreds() {
      return this.creds;
   }

   public void setCreds(GoogleCredentials creds) {
      this.creds = creds;
   }

}

