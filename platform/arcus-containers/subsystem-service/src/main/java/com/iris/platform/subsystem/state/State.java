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

import java.util.Date;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.model.subs.SubsystemModel;

public interface State<M extends SubsystemModel> {
	String name();
	
	default String onStarted(SubsystemContext<M> context) {
		context.logger().debug("Restoring state [{}]", name());
		SubsystemUtils.restoreTimeout(context, name());
		return name();
	}
	
	default String onEnter(SubsystemContext<M> context) { 
		context.logger().debug("Entering state [{}]", name());
		return name();
	}

	default void onExit(SubsystemContext<M> context) {
		context.logger().debug("Exiting state [{}]", name());
		cancelTimeout(context);
	}

	default String onTimeout(SubsystemContext<M> context) {
		context.logger().warn("Received timeout for state [{}]", name());
		return name();
	}
	
	default Date setTimeout(SubsystemContext<M> context, long delayMs) {
		return SubsystemUtils.setTimeout(delayMs, context, name());
	}
	
	default void cancelTimeout(SubsystemContext<M> context) {
		SubsystemUtils.clearTimeout(context, name());
	}

}

