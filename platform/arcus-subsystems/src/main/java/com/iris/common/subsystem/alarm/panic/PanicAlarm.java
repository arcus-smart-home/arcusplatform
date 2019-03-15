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
package com.iris.common.subsystem.alarm.panic;

import com.google.common.base.Predicates;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.generic.AlarmState;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.alarm.smoke.SmokeAlertState;
import com.iris.common.subsystem.alarm.generic.AlarmStateMachine;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;

public class PanicAlarm extends AlarmStateMachine<AlarmSubsystemModel> {
	public static final String NAME = "PANIC";

	public PanicAlarm() {
		// FIXME update the offline to take rule:state == DISABLED into account
		super(
				NAME,
				ExpressionCompiler.compile(
					"base:caps contains 'keypad' OR " +
					"(" +
						"rule:template == 'pendant-panic' OR " +
						"rule:template == 'button-panic' OR " +
						"rule:template == '01e7de' OR " +
						"rule:template == 'c2dd38'" +
					")"
				),
				Predicates.<Model>alwaysFalse()
		);
	}

	@Override
	protected TriggerEvent getTriggerType(SubsystemContext<AlarmSubsystemModel> context, Model model) {
		throw new UnsupportedOperationException();
	}

   @Override
   protected AlarmState<? super AlarmSubsystemModel> state(String name)
   {
      switch(name) {
         case AlarmCapability.ALERTSTATE_ALERT: return PanicAlertState.instance();
         default: return super.state(name);
      }
   }
	
	

}

