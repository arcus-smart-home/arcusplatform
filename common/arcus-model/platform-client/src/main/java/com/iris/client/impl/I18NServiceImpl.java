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
package com.iris.client.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.I18NService;

public class I18NServiceImpl implements I18NService {
	private IrisClient client;

	public I18NServiceImpl(IrisClient irisClient) {
		this.client = irisClient;
	}

    @Override
    public ClientFuture<LoadLocalizedStringsResponse> loadLocalizedStrings(Set<String> strings, String s) {
   	 ClientRequest request = new ClientRequest();
   	 Map<String, Object> elements = new HashMap<>();
   	 elements.put("bundleNames", strings);
   	 elements.put("locale", s);
   	 request.setAttributes(elements);
   	 request.setTimeoutMs(10000);
   	 request.setAddress(getAddress());
   	 request.setCommand(I18NService.CMD_LOADLOCALIZEDSTRINGS);
   	 request.setConnectionURL(client.getConnectionURL());
   	 request.setRestfulRequest(true);

        return Futures.transform(client.request(request), new Function<ClientEvent, LoadLocalizedStringsResponse>() {
            @Override
            public LoadLocalizedStringsResponse apply(ClientEvent input) {
                return (new LoadLocalizedStringsResponse(input));
            }
        });
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getAddress() {
        return "SERV:" + NAMESPACE + ":";
    }
}

