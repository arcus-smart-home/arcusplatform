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
package com.iris.oculus.util;

import java.util.Optional;

import javax.annotation.Nullable;

public abstract class OperationEvent<V> {

	public enum Type {
		LOADING,
		LOADED,
		ERROR;
	}
	
	public abstract Type getType();
	
	public abstract Optional<V> result();
	
	public abstract Optional<Throwable> error();
	
	@SuppressWarnings("unchecked")
	public static <V> OperationEvent<V> loading() { return LoadingOperation.Instance; }
	
	public static <V> OperationEvent<V> loaded(@Nullable V value) {
		return new LoadedOperation<>(value);
	}
	
	public static <V> OperationEvent<Throwable> error(Throwable cause) {
		return new ErrorOperation<>(cause);
	}
	
	private static class LoadingOperation<V> extends OperationEvent<V> {
		@SuppressWarnings("rawtypes")
		private static final LoadingOperation Instance = new LoadingOperation();

		@Override
		public Type getType() {
			return Type.LOADING;
		}

		@Override
		public Optional<V> result() {
			return Optional.empty();
		}

		@Override
		public Optional<Throwable> error() {
			return Optional.empty();
		}
		
	}
	
	private static class LoadedOperation<V> extends OperationEvent<V> {
		private final Optional<V> value;
		
		LoadedOperation(V value) {
			// note if {@code null} is supported by the underlying eventing mechanism the type
			// will let the user know it is a value result with null
			this.value = Optional.ofNullable(value);
		}
		
		@Override
		public Type getType() {
			return Type.LOADED;
		}

		@Override
		public Optional<V> result() {
			return value;
		}

		@Override
		public Optional<Throwable> error() {
			return Optional.empty();
		}
		
	}

	private static class ErrorOperation<V> extends OperationEvent<V> {
		private final Optional<Throwable> error;
		
		ErrorOperation(Throwable error) {
			this.error = Optional.of(error != null ? null : new NullPointerException("Unspecified error"));
		}
		
		@Override
		public Type getType() {
			return Type.ERROR;
		}

		@Override
		public Optional<V> result() {
			return Optional.empty();
		}

		@Override
		public Optional<Throwable> error() {
			return error;
		}
		
	}

}

