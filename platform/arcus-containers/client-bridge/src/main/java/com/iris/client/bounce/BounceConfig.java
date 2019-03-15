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
package com.iris.client.bounce;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class BounceConfig {
	@Inject @Named("ios.app.ids")
	private String appleAppIds;
	@Inject @Named("bounce.redirect.url")
	private String redirectUrl;
	@Inject @Named("bounce.app.url")
	private String appUrl;
	@Inject @Named("bounce.android.url")
	private String androidStoreUrl;
	@Inject @Named("bounce.apple.url")
	private String appleStoreUrl;
	@Inject @Named("bounce.help.url")
	private String helpUrl;
	@Inject @Named("bounce.web.url")
	private String webUrl;

	public String getAppleAppIds() {
		return appleAppIds;
	}

	public void setAppleAppIds(String appleAppIds) {
		this.appleAppIds = appleAppIds;
	}

	public String[] getAppleIdList() {
		return StringUtils.split(appleAppIds, ',');
	}
	
	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public String getAppUrl() {
		return appUrl;
	}

	public void setAppUrl(String appHost) {
		this.appUrl = appHost;
	}

	public String getAndroidStoreUrl() {
		return androidStoreUrl;
	}

	public void setAndroidStoreUrl(String androidStoreUrl) {
		this.androidStoreUrl = androidStoreUrl;
	}

	public String getAppleStoreUrl() {
		return appleStoreUrl;
	}

	public void setAppleStoreUrl(String appleStoreUrl) {
		this.appleStoreUrl = appleStoreUrl;
	}

	public String getHelpUrl() {
		return helpUrl;
	}

	public void setHelpUrl(String helpUrl) {
		this.helpUrl = helpUrl;
	}

	public String getWebUrl() {
		return webUrl;
	}

	public void setWebUrl(String webUrl) {
		this.webUrl = webUrl;
	}

}

