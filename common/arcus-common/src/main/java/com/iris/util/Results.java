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

import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/**
 *
 */
public class Results {

	public static <T> Result<T> fromValue(T value) {
		return new ResultValue<T>(value);
	}

	public static <T> Result<T> fromError(Throwable error) {
		return new ResultError<T>(error);
	}

	public static <T> ListenableFuture<T> toFuture(Result<T> result) {
		return result.isValue() ? (ListenableFuture<T>)Futures.immediateFuture(result.getValue())
		                        : (ListenableFuture<T>)Futures.immediateFailedFuture(result.getError());
	}

	public static <T> void addCallback(ListenableFuture<T> future, final Callback<T> callback) {
		Futures.addCallback(future, new FutureCallback<T>() {
			@Override
         public void onSuccess(T result) {
	         callback.onResult(Results.fromValue(result));
         }

			@Override
         public void onFailure(Throwable t) {
	         callback.onResult((Result<T>)Results.fromError(t));
         }
		}, MoreExecutors.directExecutor());
	}

	private static abstract class AbstractResult<T> implements Result<T> {
	   @Override
	   public T get() throws ExecutionException {
		   Throwable error = getError();
		   if(error == null) {
			   return getValue();
		   }
		   if(error instanceof Error) {
			   throw new ExecutionError((Error) error);
		   }
		   throw new ExecutionException(error);
	   }
	}

	private static class ResultValue<T> extends AbstractResult<T> {
		private final T value;

		ResultValue(T value) {
			this.value = value;
		}

		@Override
      public boolean isValue() {
	      return true;
      }

		@Override
      public boolean isError() {
	      return false;
      }

		@Override
      public T getValue() {
	      return value;
      }

		@Override
      public Throwable getError() {
	      return null;
      }

		@Override
      public String toString() {
	      return "Result [value=" + value + "]";
      }

		@Override
      public int hashCode() {
	      final int prime = 31;
	      int result = 1;
	      result = prime * result + ((value == null) ? 0 : value.hashCode());
	      return result;
      }

		@Override
      public boolean equals(Object obj) {
	      if (this == obj)
		      return true;
	      if (obj == null)
		      return false;
	      if (getClass() != obj.getClass())
		      return false;
	      ResultValue other = (ResultValue) obj;
	      if (value == null) {
		      if (other.value != null)
			      return false;
	      } else if (!value.equals(other.value))
		      return false;
	      return true;
      }
	}

	private static class ResultError<T> extends AbstractResult<T> {
		private final Throwable error;

		ResultError(Throwable error) {
			this.error = error;
		}

		@Override
      public boolean isValue() {
	      return false;
      }

		@Override
      public boolean isError() {
	      return true;
      }

		@Override
      public T getValue() {
	      return null;
      }

		@Override
      public Throwable getError() {
	      return error;
      }

		@Override
      public String toString() {
	      return "Result [error=" + error + "]";
      }

		@Override
      public int hashCode() {
	      final int prime = 31;
	      int result = 1;
	      result = prime * result + ((error == null) ? 0 : error.hashCode());
	      return result;
      }

		@Override
      public boolean equals(Object obj) {
	      if (this == obj)
		      return true;
	      if (obj == null)
		      return false;
	      if (getClass() != obj.getClass())
		      return false;
	      ResultError other = (ResultError) obj;
	      if (error == null) {
		      if (other.error != null)
			      return false;
	      } else if (!error.equals(other.error))
		      return false;
	      return true;
      }
	}
}

