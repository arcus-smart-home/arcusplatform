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
package com.iris.platform.scene.resolver;

import static com.google.common.base.Predicates.or;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.SendAction;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ShadeCapability;
import com.iris.messages.capability.Somfyv1Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.type.ActionSelector;
import com.iris.model.predicate.Predicates;

public class ShadeResolver extends BaseResolver {
	private final Predicate<Model> isSomfyBlinds = Predicates.isA(Somfyv1Capability.NAMESPACE);

	private final Predicate<Model> isShade = Predicates.isA(ShadeCapability.NAMESPACE);

	private final Predicate<Model> isBlind = or(isSomfyBlinds, isShade);

	public static final String SHADE_SELECTOR_NAME = "shadeopen";
	public static final String SOMFY_SELECTOR_NAME = "somfyshadeopen";
	
	private static final String SHADE_OPEN = "OPEN";
	private static final String SHADE_CLOSED = "CLOSED";

	public ShadeResolver() {
		super("blinds", "Open or Close Blinds", "blind");
	}

	@Override
	protected List<ActionSelector> resolve(ActionContext context, Model model) {
		if (!isBlind.apply(model)) {
			return ImmutableList.of();
		}

		if (isSomfyBlinds.apply(model)) {
			return ImmutableList.of(resolveSomfy(context, model));
		} else {
			return ImmutableList.of(resolveShade(context, model));
		}
	}

	private ActionSelector resolveSomfy(ActionContext context, Model model) {
		ActionSelector selector = new ActionSelector();
		selector.setName(SOMFY_SELECTOR_NAME);
		selector.setType(ActionSelector.TYPE_LIST);
		selector.setValue(ImmutableList.of(ImmutableList.of(Somfyv1Capability.CURRENTSTATE_OPEN, "Open"),
				ImmutableList.of(Somfyv1Capability.CURRENTSTATE_CLOSED, "Close")));
		return selector;
	}

	/*  */

	/*
	 * Currently Shade Scenes are keyed off of pre-defined levels. I can
	 * anticipate that perhaps we may have a future use for allowing user to set
	 * the level as part of the rule/scene. Such a mechanism would require a
	 * percent selector as defined here:
	 * 
	 * 		ActionSelector selector = new ActionSelector();
	 * 		selector.setName(SHADE_SELECTOR_NAME); selector.setMin(0);
	 * 		selector.setMax(100);
	 * 		selector.setValue(ImmutableList.of(ImmutableList.of("OPEN", "Open"),
	 * 		ImmutableList.of("CLOSED", "Close")));
	 * 		selector.setType(ActionSelector.TYPE_PERCENT);
	 * 
	 * For now we only need specify the ActionSelector as follows:
	 * 
	 * @author TMT
	 */
	private ActionSelector resolveShade(ActionContext context, Model model) {
		ActionSelector selector = new ActionSelector();
		selector.setName(SHADE_SELECTOR_NAME);
		selector.setType(ActionSelector.TYPE_LIST);
		selector.setValue(ImmutableList.of(ImmutableList.of(SHADE_OPEN, "Open"),
				ImmutableList.of(SHADE_CLOSED, "Close")));
		return selector;
	}

	@Override
	public Action generate(ActionContext context, Address target, Map<String, Object> variables) {

		Set<String> selectorType = variables.keySet();

		Action action = (selectorType.contains(SOMFY_SELECTOR_NAME)) ? genSomfy(context, target, variables)
				: genShade(context, target, variables);

		context.logger().debug("generating action [{}] for {} {}", action, target, variables);
		return action;
	}

	private Action genSomfy(ActionContext context, Address target, Map<String, Object> variables) {
		String value = (String) variables.get(SOMFY_SELECTOR_NAME);

		if (StringUtils.isNoneBlank(value)) {
			String requestType;
			switch (value) {
			case Somfyv1Capability.CURRENTSTATE_OPEN:
				requestType = Somfyv1Capability.GoToOpenRequest.NAME;
				break;
			case Somfyv1Capability.CURRENTSTATE_CLOSED:
				requestType = Somfyv1Capability.GoToClosedRequest.NAME;
				break;

			default:
				throw new ErrorEventException(Errors.invalidParam(SOMFY_SELECTOR_NAME));
			}
			return new SendAction(requestType, Functions.constant(target), ImmutableMap.of());
		} else {
			throw new ErrorEventException(Errors.invalidParam(SOMFY_SELECTOR_NAME));
		}
	}

	/*
	 * Currently Shade Scenes are keyed off of pre-defined levels. I can
	 * anticipate that perhaps we may have a future use for allowing user to set
	 * the level as part of the rule/scene
	 * 
	 * For example, such a mechanism would require the following: 
	 * 
	 * 		Object level = (Object) variables.get("level"); 
	 * 		Preconditions.checkNotNull(level,"level is required"); 
	 * 		int levelPct = AttributeTypes.coerceInt(level);
	 * 		Preconditions.checkArgument(levelPct >= MIN_OPEN_LEVEL && levelPct <= MAX_OPEN_LEVEL, "Invalid level");
	 * 
	 * For now we only need specify the command as defined below
	 * 
	 * @author TMT
	 */
	private Action genShade(ActionContext context, Address target, Map<String, Object> variables) {
		String value = (String) variables.get(SHADE_SELECTOR_NAME);

		if (StringUtils.isNoneBlank(value)) {
			String requestType;
			switch (value) {
			case SHADE_OPEN:
				requestType = ShadeCapability.GoToOpenRequest.NAME;
				break;
			case SHADE_CLOSED:
				requestType = ShadeCapability.GoToClosedRequest.NAME;
				break;

			default:
				throw new ErrorEventException(Errors.invalidParam(SHADE_SELECTOR_NAME));
			}
			return new SendAction(requestType, Functions.constant(target), ImmutableMap.of());
		} else {
			throw new ErrorEventException(Errors.invalidParam(SHADE_SELECTOR_NAME));
		}
	}
}

