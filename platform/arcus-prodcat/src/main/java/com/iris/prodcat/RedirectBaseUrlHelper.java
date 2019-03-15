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
package com.iris.prodcat;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class RedirectBaseUrlHelper {
	@Inject
	@Named(value = "redirect.base.url")
	private String redirectBaseUrl;
	
	private final static String DYNAMIC_LINK_URL = "{{redirectBaseUrl}}";

	public String getRedirectBaseUrl() {
		return redirectBaseUrl;
	}

	public void setRedirectBaseUrl(String redirectBaseUrl) {
		this.redirectBaseUrl = redirectBaseUrl;
	}
	
	/**
	 * Replace any {{redirectBaseUrl}} in the url with the configured value
	 * @param urlLink
	 * @return
	 */
	public String replaceRedirectBaseUrl(String urlLink) {
		if(StringUtils.isNotBlank(urlLink) && urlLink.contains(DYNAMIC_LINK_URL)) {
			return urlLink.replace(DYNAMIC_LINK_URL, redirectBaseUrl);
		}else{
			return urlLink;
		}
	}
}

