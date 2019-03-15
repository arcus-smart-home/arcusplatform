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
package com.iris.client.model.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.annotation.Command;
import com.iris.client.annotation.GetAttribute;
import com.iris.client.annotation.RESTful;
import com.iris.client.annotation.SetAttribute;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.model.Model;

class ModelInvocationFunctions {

	public static ModelInvocationFunction wrap(Method m) {
		if(Model.class.equals(m.getDeclaringClass())) {
			return DelegatingMethod.INSTANCE;
		}
		if(Object.class.equals(m.getDeclaringClass())) {
			if(m.getName().equals("equals")) {
				return EqualsMethod.INSTANCE;
			}
			// TODO modify hashCode / equals / toString ?
			return DelegatingMethod.INSTANCE;
		}
		{
			GetAttribute attribute = m.getAnnotation(GetAttribute.class);
			if(attribute != null) {
				return new GetAttributeMethod(attribute);
			}
		}
		{
			SetAttribute attribute = m.getAnnotation(SetAttribute.class);
			if(attribute != null) {
				return new SetAttributeMethod(attribute);
			}
		}
		{
			Command attribute = m.getAnnotation(Command.class);
			if(attribute != null) {
				try {
					RESTful request = m.getAnnotation(RESTful.class);
					boolean isHTTP = (request == null) ? false : true;
					return new CommandMethod(m.getDeclaringClass(), attribute, isHTTP);
				}
				catch(Exception e) {
					throw new IllegalArgumentException("Unable to create command handler for method: " + m, e);
				}
			}
		}
		throw new IllegalArgumentException("Unsupported method: " + m + ". Is this a capability/model method?");
	}

	private static class EqualsMethod implements ModelInvocationFunction {
		private static final EqualsMethod INSTANCE = new EqualsMethod();

		@Override
		public Object invoke(Model delegate, Method method, Object[] args) throws Exception {
			if(args.length != 1 || args[0] == null) {
				return false;
			}
			Object other = args[0];
			if(Proxy.isProxyClass(other.getClass())) {
				InvocationHandler handler = Proxy.getInvocationHandler(other);
				if(handler instanceof ModelInvocationHandler) {
					return delegate.equals(((ModelInvocationHandler) handler).getDelegate());
				}
			}
			if(other instanceof Model) {
				return delegate.equals(other);
			}
			return false;
		}

	}

	private static class DelegatingMethod implements ModelInvocationFunction {
		private static final DelegatingMethod INSTANCE = new DelegatingMethod();

		@Override
		public Object invoke(Model delegate, Method method, Object[] args) throws Exception {
			return method.invoke(delegate, args);
		}
	}

	private static class GetAttributeMethod implements ModelInvocationFunction {
		private final String name;

		GetAttributeMethod(GetAttribute attribute) {
			this.name = attribute.value();
		}

		@Override
		public Object invoke(Model delegate, Method method, Object[] args) throws Exception {
		   AttributeType type = AttributeTypes.fromJavaType(method.getGenericReturnType());
			return type.coerce(delegate.get(name));
		}
	}

	private static class SetAttributeMethod implements ModelInvocationFunction {
		private final String name;

		SetAttributeMethod(SetAttribute attribute) {
			this.name = attribute.value();
		}

		@Override
		public Object invoke(Model delegate, Method method, Object[] args) throws Exception {
			return delegate.set(name, args[0]);
		}
	}

	private static class CommandMethod implements ModelInvocationFunction {
		private final String name;
		private final List<String> parameterNames;
		private final Constructor<?> responseCtor;
		private final boolean isHTTP;
		
		CommandMethod(Class<?> capability, Command command, boolean asHTTP) throws Exception {
			this.name = command.value();
			this.parameterNames = Arrays.asList(command.parameters());
			String responseName = command.value().split("\\:", 2)[1] + "Response";
			Class<?> responseType = Class.forName(capability.getName() + "$" + responseName);
			responseCtor = responseType.getConstructor(ClientEvent.class);
			isHTTP = asHTTP;
		}

		@Override
		public Object invoke(Model delegate, final Method method, Object[] args) throws Exception {
			ClientRequest request = new ClientRequest();
			request.setCommand(name);
			request.setRestfulRequest(isHTTP);
			if (!parameterNames.isEmpty()) {
				Map<String, Object> parameters = new HashMap<>(parameterNames.size());

				for(int i=0; i<args.length; i++) {
					parameters.put(parameterNames.get(i), args[i]);
				}

				request.setAttributes(parameters);
			}
			
			return
					delegate
						.request(request)
						.transform(new Function<ClientEvent, Object>() {
			
							@Override
							public Object apply(ClientEvent input) {
								try {
									return responseCtor.newInstance(input);
								}
								catch (SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
									throw new RuntimeException("Unable to transform event to type", e);
								}
							}
			
						});
		}
	}

}

