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
package com.iris.common.subsystem.event;

import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.event.AddressableEvent;

public abstract class SubsystemResponseEvent extends AddressableEvent {
	
	public static SubsystemResponseEvent response(Address address, SubsystemContext.ResponseAction<?> action, PlatformMessage response) {
		return new ResponseReceivedEvent(address, action, response);
	}
	
	public static SubsystemResponseEvent error(Address address, SubsystemContext.ResponseAction<?> action, Throwable cause) {
		return new ExceptionReceivedEvent(address, action, cause);
	}
	
	public static SubsystemResponseEvent timeout(Address address, SubsystemContext.ResponseAction<?> action) {
		return error(address, action, new TimeoutException());
	}
	
	private final Address address;
	private final SubsystemContext.ResponseAction<?> action;
	
	private SubsystemResponseEvent(Address address, SubsystemContext.ResponseAction<?> action) {
		this.address = address;
		this.action = action;
	}

	@Override
	public Address getAddress() {
		return address;
	}
	
	public SubsystemContext.ResponseAction<?> getAction() {
		return action;
	}
	
	@Nullable
	public abstract PlatformMessage getResponse();
	
	@Nullable
	public abstract Throwable getError();
	
	public abstract boolean isError();
	
	public abstract boolean isTimeout();
	
	private static class ResponseReceivedEvent extends SubsystemResponseEvent {
		private PlatformMessage message;
		private Throwable error;
		
		private ResponseReceivedEvent(Address address, SubsystemContext.ResponseAction<?> action, PlatformMessage message) {
			super(address, action);
			this.message = message;
			this.error = message != null && message.isError() ? new ErrorEventException(message.getValue()) : null; 
		}
		
		@Override
		public PlatformMessage getResponse() {
			return message;
		}
		
		@Nullable
		@Override
		public Throwable getError() {
			return error;
		}

		@Override
		public boolean isError() {
			return error != null;
		}
		
		@Override
		public boolean isTimeout() {
			return false;
		}
	}

	private static class ExceptionReceivedEvent extends SubsystemResponseEvent {
		private Throwable error;
		
		private ExceptionReceivedEvent(Address address, SubsystemContext.ResponseAction<?> action, Throwable error) {
			super(address, action);
			this.error = error; 
		}
		
		@Override
		public PlatformMessage getResponse() {
			return null;
		}
		
		@Nullable
		@Override
		public Throwable getError() {
			return error;
		}

		@Override
		public boolean isError() {
			return true;
		}
		
		@Override
		public boolean isTimeout() {
			return error instanceof TimeoutException;
		}
	}

}

