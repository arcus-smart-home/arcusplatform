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
package com.iris.prodcat.customization;

import java.io.InputStream;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.io.xml.JAXBUtil;
import com.iris.prodcat.RedirectBaseUrlHelper;
import com.iris.prodcat.pairing.serializer.Customizations;
import com.iris.resource.Resources;
import com.iris.resource.manager.DefaultSingleFileResourceManager;

@Singleton
public class PairingCatalogManager {
	private final DefaultSingleFileResourceManager<PairingCatalog> delegate;
	private final RedirectBaseUrlHelper urlHelper;
	
	@Inject
	public PairingCatalogManager(PairingCatalogConfig config) {
		this.urlHelper = config.getRedirectBaseUrlHelper();
		this.delegate = new DefaultSingleFileResourceManager<>(Resources.getResource(config.getPairingCatalogPath()), this::parse);		
	}
	
	protected PairingCatalog parse(InputStream is) {
		try {
			com.iris.prodcat.pairing.serializer.PairingCatalog deserialized = JAXBUtil.fromXml(is, com.iris.prodcat.pairing.serializer.PairingCatalog.class);
			
			return new PairingCatalog(normalizeData(deserialized.getCustomizations()));
		} 
		catch (RuntimeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new IllegalArgumentException("Unable to load pairing catalog", e);
		}
	}

	public PairingCatalog getCatalog() {
		return delegate.getParsedData();
	}
	
	//TODO - may want to make this more flexible to accommodate additional dynamic values.
	private Customizations normalizeData(Customizations customizations) {
		customizations.getCustomization().forEach(c -> {
			c.setLinkUrl(urlHelper.replaceRedirectBaseUrl(c.getLinkUrl()));			
		});
		return customizations;
		
	}
	
}

