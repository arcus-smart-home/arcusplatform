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
package com.iris.util;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class AggregateExecutionException extends ExecutionException {

	public AggregateExecutionException(List<Throwable> causes) {
		this("Multiple errors", causes);
	}

	public AggregateExecutionException(String message, List<Throwable> causes) {
		super(buildMessage(message, causes), causes.get(0));
		// TODO custom stack trace?
	}

	private static String buildMessage(String message, List<Throwable> causes) {
		StringBuilder sb = new StringBuilder(message).append("Caused by:");
		int count = 1;
		for(Throwable cause: causes) {
			sb.append("\n  ").append(count).append(") ").append(cause.getMessage());
			count++;
		}
	   return sb.toString();
   }
}

