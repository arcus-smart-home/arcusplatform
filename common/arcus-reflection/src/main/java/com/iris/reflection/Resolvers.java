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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;

public class Resolvers {

	public static <I, R> ArgumentResolverFactory<I, R> chain(ArgumentResolverFactory<I, R>... resolvers) {
		return chain(Arrays.asList(resolvers));
	}

	public static <I, R> ArgumentResolverFactory<I, R> chain(Collection<ArgumentResolverFactory<I, R>> resolvers) {
		Preconditions.checkState(!resolvers.isEmpty(), "Must specify at least one resolver factory");
		if(resolvers.size() == 1) {
			return Iterables.getFirst(resolvers, null);
		}
		List<ArgumentResolverFactory<I, R>> factories = new ArrayList<>(resolvers.size());
		for(ArgumentResolverFactory<I, R> resolver: resolvers) {
			if(resolver instanceof ArgumentResolverFactoryChain) {
				factories.addAll(((ArgumentResolverFactoryChain<I, R>) resolver).factories);
			}
			else {
				factories.add(resolver);
			}
		}
		return new ArgumentResolverFactoryChain<>(factories);
	}

	private static class ArgumentResolverFactoryChain<I, R> implements ArgumentResolverFactory<I, R> {
		
		private final List<ArgumentResolverFactory<I, R>> factories;
		
		private ArgumentResolverFactoryChain(List<ArgumentResolverFactory<I, R>> factories) {
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
	}
		
}

