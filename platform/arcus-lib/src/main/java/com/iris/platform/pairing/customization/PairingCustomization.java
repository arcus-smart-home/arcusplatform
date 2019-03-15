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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;
import com.iris.messages.type.PairingCustomizationStep;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.prodcat.pairing.serializer.Customization;
import com.iris.prodcat.pairing.serializer.CustomizationType;
import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

public abstract class PairingCustomization {
	private static final Logger logger = LoggerFactory.getLogger(PairingCustomization.class);
	private static final String ID_PREFIX = "customization/";
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static List<PairingCustomization> toPairingCustomization(List<Customization > customizations) throws ValidationException {
		Validator vlad = new Validator();
		List<PairingCustomization> result = new ArrayList<>();
		for(Customization customization: customizations) {
			try {
				result.add(toPairingCustomization(customization, vlad));
			}
			catch(Exception e) {
				vlad.error(e.getMessage());
			}
		}
		// TODO global validation
		vlad.throwIfErrors();
		return result;
	}
	
	private static PairingCustomization toPairingCustomization(Customization customization, Validator validator) {
		return 
			builder()
				.withType(customization.getType())
				.withId(customization.getId())
				.withTitle(customization.getTitle())
				.withHeader(customization.getHeader())
				.withNote(customization.getNote())
				.withDescription(customization.getP())
				.withQuery(customization.getQuery())
				.withRecommendedRules(customization.getRuleTemplates())
				.withLinkText(customization.getLinkText())
				.withLinkUrl(customization.getLinkUrl())
				.build(validator);
	}

	private final String action;
	private final String id;
	@Nullable
	private final String header;
	@Nullable
	private final String title;
	@Nullable
	private final String note;
	private final List<String> description;
	@Nullable
	private final String linkText;
	@Nullable final String linkUrl;
	
	protected PairingCustomization(
			String action,
			String id,
			@Nullable String header,
			@Nullable String title,
			@Nullable String note,
			@Nullable List<String> description,
			@Nullable String linkText,
			@Nullable String linkUrl
	) {
		this.action = action;
		this.id = id;
		this.header = header;
		this.title = title;
		this.note = note;
		this.description = description != null ? ImmutableList.copyOf(description) : ImmutableList.of();
		this.linkText = linkText;
		this.linkUrl = linkUrl;
	}
	
	protected abstract boolean apply(Place place, Model device);
	
	public String getAction() {
		return action;
	}

	public String getName() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getInfo() {
		return note;
	}

	public List<String> getDescription() {
		return description;
	}
	
	public Optional<PairingCustomizationStep> toStepIf(Place place, Model device) {
		return apply(place, device) ? Optional.of(toStep()) : Optional.empty();
	}
	
	public PairingCustomizationStep toStep() {
		PairingCustomizationStep step = new PairingCustomizationStep();
		step.setId(id);
		step.setAction(action);
		step.setHeader(header);
		step.setTitle(title);
		step.setInfo(note);
		step.setDescription(description);
		step.setLinkText(linkText);
		step.setLinkUrl(linkUrl);
		return step;
	}
	
	
	public static class Builder {
		private CustomizationType type;
		private String id;
		private String header;
		private String title;
		private String note;
		private List<String> description;
		private String linkText;
		private String linkUrl;
		private List<String> recommendedRules;
		private Predicate<Model> matcher;
		
		/**
		 * Allow for extension, but don't make
		 * public so that its clear the static
		 * {@link PairingCustomization#builder()}
		 * is the preferred way to create this object.
		 */
		protected Builder() {
			
		}
		
		public CustomizationType getType() {
			return type;
		}
		
		public Builder withType(CustomizationType type) {
			this.type = type;
			return this;
		}
		
		public String getId() {
			return id;
		}
		
		public Builder withId(String id) {
			this.id = id;
			return this;
		}
		
		public String getHeader() {
			return header;
		}

		public Builder withHeader(String header) {
			this.header = header;
			return this;
		}

		public String getTitle() {
			return title;
		}
		
		public Builder withTitle(String title) {
			this.title = title;
			return this;
		}
		
		public String getNote() {
			return note;
		}
		
		public Builder withNote(String note) {
			this.note = note;
			return this;
		}
		
		public String getLinkText() {
			return linkText;
		}
		
		public Builder withLinkText(String linkText) {
			this.linkText = linkText;
			return this;
		}
		
		public String getLinkUrl() {
			return linkUrl;
		}
		
		public Builder withLinkUrl(String linkUrl) {
			this.linkUrl = linkUrl;
			return this;
		}
		
		public List<String> getDescription() {
			return description;
		}
		
		public Builder withDescription(List<String> description) {
			this.description = description;
			return this;
		}
		
		public Predicate<Model> getMatcher() {
			return matcher;
		}
		
		public Builder withMatcher(Predicate<Model> matcher) {
			this.matcher = matcher;
			return this;
		}
		
		public Builder withQuery(String query) {
			if(StringUtils.isEmpty(query)) {
				this.matcher = null;
			}
			else {
				this.matcher = ExpressionCompiler.compile(query);
			}
			return this;
		}

		public Builder withRecommendedRules(String recommendedRules) {
			if(recommendedRules == null) {
				this.recommendedRules = null;
			}
			else {
				this.recommendedRules = Arrays.asList(StringUtils.split(recommendedRules, ", "));
			}
			return this;
		}

		public PairingCustomization build(Validator vlad) {
			Preconditions.checkArgument(type != null, "Must specify a type");
			String action = getAction();
			switch(type) {
			case INFO:
				action = PairingCustomizationStep.ACTION_INFO;
				assertIdRequired(vlad);
				assertTitleRequired(vlad);
				assertDescriptionNotEmpty(vlad);
				// fall through
			case BUTTON_ASSIGNMENT:   // fall through
			case MULTI_BUTTON_ASSIGNMENT:   // fall through
			case CONTACT_USE_HINT:    // fall through
			case CONTACT_TEST:        // fall through
			case FAVORITE:            // fall through
			case NAME:                // fall through
			case PRESENCE_ASSIGNMENT: // fall through
			case SCHEDULE:            // fall through
			case HALO_ROOM:			  // fall through
			case STATE_COUNTY_SELECT:			  // fall through
			case WATER_HEATER:			// fall through
			case IRRIGATION_ZONE:			// fall through
			case MULTI_IRRIGATION_ZONE:			// fall through
			case WEATHER_STATION:     // fall through
				assertQueryRequired(vlad);
				assertRulesUnsupported(vlad);
				return new QueryPairingCustomization(matcher, action, createId(id, type), header, title, note, description, linkText, linkUrl);

			case RULE:
				assertQueryRequired(vlad);
				assertRulesRequired(vlad);
				return new RuleRecommendationCustomization(recommendedRules, matcher, action, createId(id, type), header, title, note, description, linkText, linkUrl);

			case SECURITY_MODE:
				assertQueryUnsupported(vlad);
				assertRulesUnsupported(vlad);
				return new SecurityModeCustomization(action, createId(id, type), header, title, note, description, linkText, linkUrl);

			case UNCERTIFIED:
				assertQueryUnsupported(vlad);
				assertRulesUnsupported(vlad);
				return new UncertifiedPairingCustomization(action, createId(id, CustomizationType.INFO), header, title, note, description, linkText, linkUrl);
			case UNKNOWN:
				assertQueryUnsupported(vlad);
				assertRulesUnsupported(vlad);
				return new UnknownPairingCustomization(action, createId(id, CustomizationType.INFO), header, title, note, description, linkText, linkUrl);
			case OTA_UPGRADE:
				assertQueryRequired(vlad);
				assertRulesUnsupported(vlad);
				return new OtaDeviceUpgradeCustomization(matcher, action, createId(id, type), header, title, note, description, linkText, linkUrl);

			default:
				throw new IllegalArgumentException("Unsupported type: " + type);
			}
		}
		
		private String createId(String id, CustomizationType type) {			
			if(StringUtils.isBlank(id)) {
				id = type.value();
			}
			return ID_PREFIX+type.value()+"/"+id;
		}

		private void assertIdRequired(Validator vlad) {
			vlad.assertNotEmpty(id, String.format("Must specify an id for type='%s'", type));
		}

		private void assertTitleRequired(Validator vlad) {
			vlad.assertNotEmpty(title, String.format("Must specify a title for type='%s'", type));
		}

		private void assertDescriptionNotEmpty(Validator vlad) {
			vlad.assertNotEmpty(description, String.format("Must specify at least one <p> block for type='%s'", type));
		}

		private void assertQueryRequired(Validator vlad) {
			vlad.assertNotNull(matcher, String.format("Must specify a query for type='%s'", type));
		}

		private void assertQueryUnsupported(Validator vlad) {
			vlad.assertTrue(matcher == null, String.format("May not specify a query for type='%s'", type));
		}

		private void assertRulesRequired(Validator vlad) {
			vlad.assertTrue(recommendedRules != null && recommendedRules.size() > 0, String.format("Must specify one or more rule-templates for type='%s'", type));
		}

		private void assertRulesUnsupported(Validator vlad) {
			vlad.assertTrue(recommendedRules == null, String.format("May not specify rule-template for type='%s'", type));
		}

		private String getAction() {
			switch(type) {
			case BUTTON_ASSIGNMENT:
				return PairingCustomizationStep.ACTION_BUTTON_ASSIGNMENT;
			case MULTI_BUTTON_ASSIGNMENT:
				return PairingCustomizationStep.ACTION_MULTI_BUTTON_ASSIGNMENT;
			case CONTACT_USE_HINT:
				return PairingCustomizationStep.ACTION_CONTACT_TYPE;
			case CONTACT_TEST:
				return PairingCustomizationStep.ACTION_CONTACT_TEST;
			case FAVORITE:
				return PairingCustomizationStep.ACTION_FAVORITE;
			case HALO_ROOM:
				return PairingCustomizationStep.ACTION_ROOM;
			case INFO:
				return PairingCustomizationStep.ACTION_INFO;
			case NAME:
				return PairingCustomizationStep.ACTION_NAME;
			case PRESENCE_ASSIGNMENT:
				return PairingCustomizationStep.ACTION_PRESENCE_ASSIGNMENT;
			case RULE:
				return PairingCustomizationStep.ACTION_RULES;
			case SCHEDULE:
				return PairingCustomizationStep.ACTION_SCHEDULE;
			case SECURITY_MODE:
				return PairingCustomizationStep.ACTION_SECURITY_MODE;
			case UNCERTIFIED:
				return PairingCustomizationStep.ACTION_INFO;
			case UNKNOWN:
				return PairingCustomizationStep.ACTION_INFO;
			case WEATHER_STATION:
				return PairingCustomizationStep.ACTION_WEATHER_RADIO_STATION;
			case STATE_COUNTY_SELECT:
				return PairingCustomizationStep.ACTION_STATE_COUNTY_SELECT;
			case OTA_UPGRADE:
				return PairingCustomizationStep.ACTION_OTA_UPGRADE;
			case WATER_HEATER:			
				return PairingCustomizationStep.ACTION_WATER_HEATER;
			case IRRIGATION_ZONE:
				return PairingCustomizationStep.ACTION_IRRIGATION_ZONE;
			case MULTI_IRRIGATION_ZONE:
				return PairingCustomizationStep.ACTION_MULTI_IRRIGATION_ZONE;
			default:
				throw new IllegalArgumentException("Unsupported type: " + type);
			}
		}
	}
}

