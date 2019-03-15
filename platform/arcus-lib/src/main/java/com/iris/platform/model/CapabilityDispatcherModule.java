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
/**
 * 
 */
package com.iris.platform.model;

import java.util.Set;

import javax.annotation.PostConstruct;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.iris.core.platform.PlatformDispatcher;
import com.iris.core.platform.PlatformDispatcherFactory;
import com.iris.executor.PlaceExecutor;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;

/**
 * @author tweidlin
 *
 */
public abstract class CapabilityDispatcherModule extends PrivateModule {
	private Multibinder<Object> annotatedObjects;
	private Multibinder<ArgumentResolverFactory<PlatformMessage, MessageBody>> resolvers;
	private Multibinder<AddressMatcher> matchers;

	protected abstract String name();
	
	protected abstract AddressMatcher matcher();
	
	protected abstract Type type();
	
	protected Named named() {
		return Names.named(name() + "." + type().name().toLowerCase());
	}
	
	protected Multibinder<Object> annotatedObjects() {
		Preconditions.checkState(annotatedObjects != null, "Must call super.configure() before accessing annotatedObjects()");
		return annotatedObjects;
	}
	
	protected LinkedBindingBuilder<ArgumentResolverFactory<PlatformMessage, MessageBody>> addArgumentResolverBinding() {
		Preconditions.checkState(resolvers != null, "Must call super.configure() before calling addArgumentResolverBinding()");
		return resolvers.addBinding();
	}
	
	protected void addBroadcastListener() {
		Preconditions.checkState(matchers != null, "Must call super.configure() before calling addBroadcastListener()");
		matchers.addBinding().toInstance(AddressMatchers.BROADCAST_MESSAGE_MATCHER);
	}
	
	protected void addListener(AddressMatcher matcher) {
		Preconditions.checkState(matchers != null, "Must call super.configure() before calling addListener()");
		matchers.addBinding().toInstance(matcher);
	}
	
	/* (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		annotatedObjects = Multibinder.newSetBinder(binder(), Object.class);
		resolvers = Multibinder.newSetBinder(binder(), new TypeLiteral<ArgumentResolverFactory<PlatformMessage, MessageBody>>() { });
		matchers = Multibinder.newSetBinder(binder(), AddressMatcher.class);
		matchers.addBinding().toInstance(matcher());
		bind(Dispatcher.class).asEagerSingleton();
	}
	
	public static class Dispatcher {
		private final PlaceExecutor executor;
		private final PlatformDispatcher dispatcher;
		
		@Inject
		public Dispatcher(
				PlaceExecutor executor,
				PlatformDispatcherFactory factory, 
				Set<AddressMatcher> matchers, 
				Set<ArgumentResolverFactory<PlatformMessage, MessageBody>> resolvers,
				Set<Object> handlers
		) {
			this.executor = executor;
			PlatformDispatcherFactory.DispatcherBuilder builder = factory.buildDispatcher();
			for(AddressMatcher matcher: matchers) {
				builder.addAddressMatcher(matcher);
			}
			for(ArgumentResolverFactory<PlatformMessage, MessageBody> resolver: resolvers) {
				builder.addArgumentResolverFactory(resolver);
			}
			for(Object handler: handlers) {
				builder.addAnnotatedHandler(handler);
			}
			this.dispatcher = builder.build();
		}
		
		@PostConstruct
		public void start() {
			executor.addRequestListener(dispatcher.matchers(), dispatcher);
		}
		
	}

	public enum Type {
		OBJECT,
		SERVICE;
	}
}

