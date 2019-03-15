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
package com.iris.platform.subsystem.state;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.AttributeType.RawType;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.subs.SubsystemModel;

public abstract class StateMachine<M extends SubsystemModel, S extends State<M>> {
	private final SubsystemContext<M> context;
	private final Supplier<String> getName;
	private final Consumer<String> setName;
	private S current;
	
	protected StateMachine(SubsystemContext<M> context, AttributeDefinition attribute) {
		this.context = context;
		Preconditions.checkArgument(attribute.getType().isEnum() || attribute.getType().getRawType() == RawType.STRING, "Only strings and enums are supported for state machine parameters");
		final String attributeName = attribute.getName();
		this.getName = () -> (String) context.getAttribute(attributeName);
		this.setName = (v) -> context.model().setAttribute(attributeName, v);
	}
	
	protected StateMachine(SubsystemContext<M> context, String variableName) {
		this.context = context;
		this.getName = () -> context.getVariable(variableName).as(String.class);
		this.setName = (v) -> context.setVariable(variableName, v);
	}
	
	protected abstract S state(String name);

	protected SubsystemContext<M> context() {
		return context;
	}
	
	protected S current() {
		if(current == null) {
			String state = getName.get();
			Preconditions.checkState(StringUtils.isNotEmpty(state), "Must initialize state attribute");
			current = state( state );
		}
		return current;
	}
	
	protected void transition(String next) {
		if (current.name().equals(next)) {
			return;
		}
		try {
			current.onExit(context);
		} catch (Exception e) {
			context.logger().warn("Error exiting state [{}]", current, e);
		}
		current = state(next);
		setName.accept(current.name());
		try {
			next = current.onEnter(context);
		} catch (Exception e) {
			context.logger().warn("Error entering state [{}]", next, e);
		}
		transition(next);
	}
	
	public void onStarted() {
		transition(current().onStarted(context));
	}
	
	public void onTimeout(ScheduledEvent event) {
		S current = current();
		if(SubsystemUtils.isMatchingTimeout(event, context, current.name())) {
			transition(current.onTimeout(context));
		}
	}
}

