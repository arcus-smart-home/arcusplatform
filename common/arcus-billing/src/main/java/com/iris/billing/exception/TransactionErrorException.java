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
package com.iris.billing.exception;

import com.iris.billing.client.model.TransactionError;

public class TransactionErrorException extends BaseException {
	private final TransactionError error;

	// To support Android, limiting super call.
	public TransactionErrorException(TransactionError error) {
   	super("[" + error.getErrorCode() + "]: " + error.getMerchantMessage(), null);

		this.error = error;
	}

	public TransactionError getError() {
		return error;
	}

	public final String getErrorCode() {
		return error.getErrorCode();
	}

	public final String getErrorCategory() {
		return error.getErrorCategory();
	}

	public final String getMerchantMessage() {
		return error.getMerchantMessage();
	}

	public final String getCustomerMessage() {
		return error.getCustomerMessage();
	}
}

