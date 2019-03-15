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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.annotation.PreDestroy;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.bootstrap.guice.AbstractIrisModule;

public class HttpSmartyStreetsModule extends AbstractIrisModule
{
   private static final Logger logger = getLogger(HttpSmartyStreetsModule.class);

   @Override
   protected void configure()
   {
      bind(Destroyer.class).toInstance(new Destroyer());
      bind(SmartyStreetsClient.class).to(HttpSmartyStreetsClient.class);
   }

   @Provides @Singleton
   public CloseableHttpClient provideCloseableHttpClient(SmartyStreetsClientConfig config)
   {
      int timeoutMillis = (int) SECONDS.toMillis(config.getTimeoutSecs());

      RequestConfig requestConfig = RequestConfig.custom()
         .setConnectTimeout(timeoutMillis)
         .setConnectionRequestTimeout(timeoutMillis)
         .setSocketTimeout(timeoutMillis)
         .build();

      CloseableHttpClient httpClient = HttpClientBuilder.create()
         .setDefaultRequestConfig(requestConfig)
         .build();

      // TODO: Customize the settings on the default PoolingHttpClientConnectionManager created by the
      // HttpClientBuilder.  Or we can build our own, but the default one has a lot of stuff set on it that we'd have to
      // do ourselves.
      //PoolingHttpClientConnectionManager connManager =
      //   (PoolingHttpClientConnectionManager) httpClient.getConnectionManager();
      //connManager.set...

      return httpClient;
   }

   private static class Destroyer
   {
      @Inject
      private CloseableHttpClient httpClient;

      @PreDestroy
      public void destroy() throws IOException
      {
         logger.debug("Closing the HttpClient, HttpClientConnectionManager, and all open connections");

         httpClient.close();
      }
   }
}

