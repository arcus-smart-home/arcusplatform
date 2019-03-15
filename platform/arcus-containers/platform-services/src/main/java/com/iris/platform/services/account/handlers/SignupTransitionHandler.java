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
package com.iris.platform.services.account.handlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;

@Singleton
public class SignupTransitionHandler implements ContextualRequestMessageHandler<Account> {
	private final AccountDAO accountDao;
	private final PlatformMessageBus platformBus;

	public static final String STATE_NOT_SAVED_CODE = "state.not.saved";
	public static final String STATE_NOT_SAVED_MESSAGE = "State was unable to be saved.";
	public static final String ARGUMENT_ERROR = "missing.required.argument";
	public static final String MISSING_ACCOUNT = "Account is required.";
	public static final String MISSING_STATE_TEXT = "State transition is required.";

	private static final String TRANSITION_ARG_NAME = "stepcompleted";

	@Inject
	public SignupTransitionHandler(AccountDAO accountDAO, PlatformMessageBus platformBus) {
		this.accountDao = accountDAO;
		this.platformBus = platformBus;
	}

	@Override
	public String getMessageType() {
		return AccountCapability.SignupTransitionRequest.NAME;
	}

	@Override
	public MessageBody handleRequest(Account context, PlatformMessage msg) {
	   MessageBody request = msg.getValue();
		String stepFinished = (String) request.getAttributes().get(TRANSITION_ARG_NAME);

		if(Account.AccountState.SIGN_UP_1.equalsIgnoreCase(stepFinished)) {
		   return MessageBody.emptyMessage();
		}

		if (context == null) {
			return Errors.fromCode(ARGUMENT_ERROR, MISSING_ACCOUNT);
		} else if (stepFinished == null) {
			return Errors.fromCode(ARGUMENT_ERROR, MISSING_STATE_TEXT);
		}

		if(stepFinished.equals(context.getState())) {
		   // no-op, don't send a duplicate notification in this case
		   return MessageBody.emptyMessage();
		}

      // FIXME: Need to validate what should have been done by now & return accordingly. This is a pass-thru right now.
		context.setState(stepFinished);
		Account copy = accountDao.save(context);

		if (!copy.getState().equals(stepFinished)) {
		   return Errors.fromCode(STATE_NOT_SAVED_CODE, STATE_NOT_SAVED_MESSAGE);
		}

		if(Account.AccountState.ABOUT_YOU.equals(copy.getState())) {
		   AccountStateTransitionUtils.sendAccountCreatedNotification(context.getOwner(), platformBus);
		}

		return MessageBody.emptyMessage();
	}

}

