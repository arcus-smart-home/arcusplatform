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
package com.iris.platform.address.validation.smartystreets;

import static com.iris.messages.errors.Errors.genericError;
import static com.iris.util.GsonUtil.getMemberAsDouble;
import static com.iris.util.GsonUtil.getMemberAsString;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.errors.ErrorEventException;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;
import com.iris.platform.address.StreetAddress;

@Singleton
public class HttpSmartyStreetsClient implements SmartyStreetsClient
{
   private static final Logger logger = getLogger(HttpSmartyStreetsClient.class);

   private static final IrisMetricSet metrics = IrisMetrics.metrics("smartystreets");

   @SuppressWarnings("serial")
   private static final Type streetAddressesType = new TypeToken<List<StreetAddress>>(){}.getType();

   @SuppressWarnings("serial")
   private static final Type detailedStreetAddressesType = new TypeToken<List<DetailedStreetAddress>>(){}.getType();

   private static final Gson gson = new GsonBuilder()
      .registerTypeAdapter(StreetAddress.class, new StreetAddressDeserializer())
      .registerTypeAdapter(DetailedStreetAddress.class, new DetailedStreetAddressDeserializer())
      .create();

   private final URI serviceBaseUri;

   private final CloseableHttpClient httpClient;

   private final TaggingMetric<Timer> requestTimerMetric;
   private final TaggingMetric<Counter> failureCounterMetric;

   @Inject
   public HttpSmartyStreetsClient(SmartyStreetsClientConfig config, CloseableHttpClient httpClient)
      throws URISyntaxException
   {
      serviceBaseUri = new URIBuilder(config.getServiceBaseUrl())
         .addParameter("auth-id", config.getAuthId())
         .addParameter("auth-token", config.getAuthToken())
         .addParameter("candidates", Integer.toString(config.getCandidates()))
         .build();

      this.httpClient = httpClient;

      requestTimerMetric = metrics.taggingTimer("request.time");

      failureCounterMetric = metrics.taggingCounter("failure.count");
   }

   @Override
   public List<StreetAddress> getSuggestions(StreetAddress address)
   {
      try (Context timerContext = requestTimerMetric.tag("op", "getSuggestions").time())
      {
         String jsonResponse = sendRequest(address);

         return gson.fromJson(jsonResponse, streetAddressesType);
      }
   }

   @Override
   public List<DetailedStreetAddress> getDetailedSuggestions(StreetAddress address)
   {
      try (Context timerContext = requestTimerMetric.tag("op", "getDetailedSuggestions").time())
      {
         String jsonResponse = sendRequest(address);

         return gson.fromJson(jsonResponse, detailedStreetAddressesType);
      }
   }

   private String sendRequest(StreetAddress address)
   {
      URI serviceUri = buildServiceUri(address);

      HttpGet httpGet = new HttpGet(serviceUri);

      // These headers are mentioned as required by the SmartyStreets docs
      httpGet.addHeader("Content-Type", APPLICATION_JSON.getMimeType());
      httpGet.addHeader("Host", serviceUri.getHost());

      return sendRequest(httpGet);
   }

   private URI buildServiceUri(StreetAddress address)
   {
      try
      {
         return new URIBuilder(serviceBaseUri)
            .addParameter("street",  address.getLine1())
            .addParameter("street2", address.getLine2())
            .addParameter("city",    address.getCity())
            .addParameter("state",   address.getState())
            .addParameter("zipcode", address.getZip())
            .build();
      }
      catch (URISyntaxException e)
      {
         throw new ErrorEventException(genericError(), e);
      }
   }

   /*
    * NOTE: Although HttpClient is thread-safe, Apache docs "highly recommend" that each thread use a separate
    * HttpContext:
    * 
    * https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e405 ("2.4. Multithreaded request
    * execution")
    * 
    * For now we are keeping the HttpContext as a local, although it's possible it could be moved into a ThreadLocal for
    * better memory efficiency.  (The context might get into a weird state and requests which run on a
    * certain thread might fail.)
    */
   private String sendRequest(HttpUriRequest httpUriRequest)
   {
      HttpContext httpContext = HttpClientContext.create();

      logger.trace("SmartyStreets API request = {}", httpUriRequest);

      try (CloseableHttpResponse response = httpClient.execute(httpUriRequest, httpContext))
      {
         logger.trace("SmartyStreets API response = {}", response);

         int statusCode = response.getStatusLine().getStatusCode();

         if (statusCode != SC_OK)
         {
            failureCounterMetric
               .tag("httpResponse.statusCode", Integer.toString(statusCode))
               .inc();

            // No root cause to include here, but we have the status code included in the failure counter above
            throw new ErrorEventException(genericError());
         }

         try (InputStream contentIn = response.getEntity().getContent())
         {
            String content = IOUtils.toString(contentIn);

            logger.trace("SmartyStreets API response content = {}", content);

            return content;
         }
      }
      catch (IOException e)
      {
         throw new ErrorEventException(genericError(), e);
      }
   }

   private static abstract class AbstractStreetAddressDeserializer<T extends StreetAddress> implements JsonDeserializer<T>
   {
      @Override
      public T deserialize(JsonElement addressElement, Type typeOfT, JsonDeserializationContext context)
         throws JsonParseException
      {
         JsonObject addressObject = addressElement.getAsJsonObject();

         String line1 = getMemberAsString(addressObject, "delivery_line_1");
         String line2 = getMemberAsString(addressObject, "delivery_line_2");

         JsonObject componentsObject = addressObject.get("components").getAsJsonObject();

         String city     = getMemberAsString(componentsObject, "city_name");
         String state    = getMemberAsString(componentsObject, "state_abbreviation");
         String zip5     = getMemberAsString(componentsObject, "zipcode");
         String zipPlus4 = getMemberAsString(componentsObject, "plus4_code");

         String zip = StreetAddress.zipBuilder()
               .withZip(zip5)
               .withZipPlus4(zipPlus4)
               .build();

         T streetAddress = newStreetAddress();

         streetAddress.setLine1(line1);
         streetAddress.setLine2(line2);
         streetAddress.setCity(city);
         streetAddress.setState(state);
         streetAddress.setZip(zip);

         return streetAddress;
      }

      protected abstract T newStreetAddress();
   }

   private static class StreetAddressDeserializer extends AbstractStreetAddressDeserializer<StreetAddress>
   {
      @Override
      protected StreetAddress newStreetAddress()
      {
         return new StreetAddress();
      }
   }

   private static class DetailedStreetAddressDeserializer extends AbstractStreetAddressDeserializer<DetailedStreetAddress>
   {
      @Override
      public DetailedStreetAddress deserialize(JsonElement addressElement, Type typeOfT, JsonDeserializationContext context)
         throws JsonParseException
      {
         DetailedStreetAddress detailedStreetAddress = super.deserialize(addressElement, typeOfT, context);

         JsonObject addressObject = addressElement.getAsJsonObject();

         JsonObject componentsObject = addressObject.get("components").getAsJsonObject();

         String plus4Code = getMemberAsString(componentsObject, "plus4_code");
         String country   = "US";

         JsonObject metadataObject = addressObject.get("metadata").getAsJsonObject();

         String addrType         = getMemberAsString(metadataObject, "record_type");
         String addrZipType      = getMemberAsString(metadataObject, "zip_type");
         String addrRDI          = getMemberAsString(metadataObject, "rdi");
         String addrCountyFIPS   = getMemberAsString(metadataObject, "county_fips");
         Double addrLatitude     = getMemberAsDouble(metadataObject, "latitude");
         Double addrLongitude    = getMemberAsDouble(metadataObject, "longitude");
         String addrGeoPrecision = getMemberAsString(metadataObject, "precision");


         String zip = StreetAddress.zipBuilder()
               .withZip(detailedStreetAddress.getZip())
               .withZipPlus4(plus4Code)
               .build();
         detailedStreetAddress.setZipPlus4(zip);

         detailedStreetAddress.setCountry(country);
         detailedStreetAddress.setAddrType(addrType);
         detailedStreetAddress.setAddrZipType(addrZipType);
         detailedStreetAddress.setAddrRDI(addrRDI);
         detailedStreetAddress.setAddrCountyFIPS(addrCountyFIPS);
         detailedStreetAddress.setAddrLatitude(addrLatitude);
         detailedStreetAddress.setAddrLongitude(addrLongitude);
         detailedStreetAddress.setAddrGeoPrecision(addrGeoPrecision.toUpperCase());

         return detailedStreetAddress;
      }

      @Override
      protected DetailedStreetAddress newStreetAddress()
      {
         return new DetailedStreetAddress();
      }
   }
}

