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
package com.iris.billing.client;

import java.util.Map;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;

import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iris.billing.client.model.RecurlyJSONResponse;
import com.iris.billing.client.model.request.BillingInfoRequest;
import com.iris.billing.exception.RecurlyAPIErrorException;

public class RecurlyTokenClient {
    private static final String TOKEN_URL = "https://api.recurly.com/js/v1/token";
    private static final Gson GSON = new GsonBuilder().create();
    private final AsyncHttpClient client;

    public RecurlyTokenClient() {
        this.client = new DefaultAsyncHttpClient();
    }

    public ListenableFuture<String> getBillingToken(BillingInfoRequest request) {
        return doGetBillingToken(request);
    }

    private final ListenableFuture<String> doGetBillingToken(BillingInfoRequest billingInfoRequest) {
        final SettableFuture<String> future = SettableFuture.create();

        try {
            BoundRequestBuilder builder = client.preparePost(TOKEN_URL)
                    .addHeader(HttpHeaders.ACCEPT, "application/xml")
                    .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

            for (Map.Entry<String, String> item : billingInfoRequest.getMappings().entrySet()) {
                builder.addFormParam(item.getKey(), item.getValue());
            }

            builder.execute(new AsyncCompletionHandler<Void>() {
                @Override
                public void onThrowable(Throwable throwable) {
                    future.setException(throwable);
                }

                @Override
                public Void onCompleted(Response response) throws Exception {
                    try {
                        RecurlyJSONResponse message = GSON.fromJson(
                                response.getResponseBody(),
                                RecurlyJSONResponse.class
                        );
                        if (message.isError()) {
                            future.setException(new RecurlyAPIErrorException(message.getCode(), message.getMessage()));
                        } else {
                            future.set(message.getID());
                        }
                    } catch (Exception ex) {
                        future.setException(ex);
                    }
                    return null;
                }
            });
        } catch (Exception ex) {
            future.setException(ex);
        }

        return future;
    }
}

