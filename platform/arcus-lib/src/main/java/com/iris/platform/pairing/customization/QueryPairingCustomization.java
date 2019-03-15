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
package com.iris.platform.pairing.customization;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Predicate;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;

class QueryPairingCustomization extends PairingCustomization {
	private final Predicate<Model> matcher;
	
	QueryPairingCustomization(
			Predicate<Model> matcher,
			String action,
			String id,
			@Nullable String header,
			@Nullable String title,
			@Nullable String note,
			@Nullable List<String> description,
			@Nullable String linkText,
			@Nullable String linkUrl
	) {
		super(
				action,
				id,
				header,
				title,
				note,
				description,
				linkText,
				linkUrl
				
		);
		this.matcher = matcher;
	}

	@Override
	protected boolean apply(Place place, Model pairingDevice) {
		return matcher.apply(pairingDevice);
	}
	
}

