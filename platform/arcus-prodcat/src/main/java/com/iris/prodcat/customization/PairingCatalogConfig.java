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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.prodcat.RedirectBaseUrlHelper;

public class PairingCatalogConfig {
	@Inject(optional = true)
	@Named(value = "pairing.catalog.path")
	private String pairingCatalogPath = "classpath:/pairing_catalog.xml";	
	
	@Inject
	private RedirectBaseUrlHelper urlHelper;

	public String getPairingCatalogPath() {
		return pairingCatalogPath;
	}

	public void setPairingCatalogPath(String pairingCatalogPath) {
		this.pairingCatalogPath = pairingCatalogPath;
	}

	public RedirectBaseUrlHelper getRedirectBaseUrlHelper() {
		return urlHelper;
	}

	public void setRedirectBaseUrlHelper(RedirectBaseUrlHelper urlHelper) {
		this.urlHelper = urlHelper;
	}
	
	

}

