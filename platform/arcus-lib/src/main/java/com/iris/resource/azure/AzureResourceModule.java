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
package com.iris.resource.azure;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.resource.Resources;

public class AzureResourceModule extends AbstractIrisModule {
	@Inject(optional = true) @Named("azure.storage.account.name")
	private String accountName;
	
	@Inject(optional = true) @Named("azure.storage.account.key")
	private String accountKey;
	
	@Inject(optional = true) @Named("azure.storage.account.https")
	private boolean https = true;

	@Provides @Singleton @Nullable
	public AzureResourceFactory azureResourceFactory() throws InvalidKeyException, URISyntaxException  {
		if(StringUtils.isBlank(accountName) || StringUtils.isBlank(accountKey)) {
			return null;
		}else{
   		AzureConfig config = new AzureConfig();
   		config.setAccountKey(accountKey);
   		config.setAccountName(accountName);
   		config.setHttps(https);		
   		AzureResourceFactory azureFactory = new AzureResourceFactory(config);
   		Resources.registerFactory(azureFactory);
   		return azureFactory;		
		}
	}

	@Override
	protected void configure() {
			
	}
}

