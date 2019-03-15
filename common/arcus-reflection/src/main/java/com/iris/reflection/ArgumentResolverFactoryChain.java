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
package com.iris.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;

public class ArgumentResolverFactoryChain<I, R> implements ArgumentResolverFactory<I, R> {
	
	public static <I, R> Builder<I, R> builder() {
		return new Builder<>();
	}
	
	public static <I, R> Builder<I, R> builder(ArgumentResolverFactory<? super I, R> first) {
		return new Builder<I, R>().addResolverFactory(first);
	}
	
	private final List<ArgumentResolverFactory<? super I, R>> factories;
	
	private ArgumentResolverFactoryChain(List<ArgumentResolverFactory<? super I, R>> factories) {
		this.factories = factories;
	}

	@Override
	public Function<? super I, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
		for(ArgumentResolverFactory<? super I, ? extends R> factory: factories) {
			Function<? super I, ?> resolver = factory.getResolverForParameter(method, parameter, annotations);
			if(resolver != null) {
				return resolver;
			}
		}
		return null;
	}

	@Override
	public Function<Object, R> getResolverForReturnType(Method method) {
		for(ArgumentResolverFactory<? super I, R> factory: factories) {
			Function<Object, R> resolver = factory.getResolverForReturnType(method);
			if(resolver != null) {
				return resolver;
			}
		}
		return null;
	}
	
	public static class Builder<I, R> {
		private List<ArgumentResolverFactory<? super I, R>> factories = new ArrayList<>();
		
		protected Builder() {
			
		}
		
		public Builder<I,R> addResolverFactory(ArgumentResolverFactory<? super I, R> factory) {
			factories.add(factory);
			return this;
		}
		
		public ArgumentResolverFactory<I, R> build() {
			Preconditions.checkState(!factories.isEmpty(), "Must add at least one factory");
			return new ArgumentResolverFactoryChain<>(new ArrayList<>(factories));
		}
	}
}

