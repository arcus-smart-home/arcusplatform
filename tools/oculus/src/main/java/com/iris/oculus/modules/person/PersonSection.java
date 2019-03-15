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
package com.iris.oculus.modules.person;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.iris.client.model.PersonModel;
import com.iris.oculus.modules.BaseSection;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.person.ux.PersonToolbar;

@Singleton
public class PersonSection extends BaseSection<PersonModel> {
	private static final String NAME = "Persons";
	private final PersonToolbar toolbar;

	@Inject
	public PersonSection(PersonController controller) {
		super(controller);
		this.toolbar = new PersonToolbar(controller);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected BaseToolbar<PersonModel> createToolbar() {
		return toolbar;
	}

	@Override
	protected String renderLabel(PersonModel model) {
		if(!StringUtils.isBlank(model.getEmail())) {
			return model.getEmail();
		}
		if(StringUtils.isBlank(model.getFirstName()) && StringUtils.isBlank(model.getLastName())) {
			return model.getId();
		}
		return model.getFirstName() + " " + model.getLastName();
	}
}

