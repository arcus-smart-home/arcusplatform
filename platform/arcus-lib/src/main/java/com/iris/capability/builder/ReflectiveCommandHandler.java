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
package com.iris.capability.builder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.Errors;
import com.iris.model.type.AttributeType;
import com.iris.model.type.VoidType;

/**
 *
 */
class ReflectiveCommandHandler implements ContextualEventHandler<MessageBody> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReflectiveCommandHandler.class);

	public static ContextualEventHandler<MessageBody> create(CommandDefinition command, Method method, Object instance) {
		List<ArgumentMatcher> matchers = extractArguments(command.getName(), method);
		if(!Modifier.isPublic(method.getModifiers())) {
			throw new IllegalStateException("Capability driver " + command + " method must be public");
		}
		Function<Object, ListenableFuture<MessageBody>> responseWrapper = extractResponseWrapper(command, method);
	   return new ReflectiveCommandHandler(method, instance, matchers, responseWrapper);
	}

	private static List<ArgumentMatcher> extractArguments(String command, Method method) {
		int parameters = method.getParameterCount();
		if(parameters == 0) {
			return Collections.emptyList();
		}
	   List<ArgumentMatcher> matchers = new ArrayList<ArgumentMatcher>(parameters);
	   for(Class<?> parameterType: method.getParameterTypes()) {
	   	matchers.add(matcherFor(command, parameterType));
	   }
	   return matchers;
   }

	private static ArgumentMatcher matcherFor(String command, Class<?> parameterType) {
	   if(String.class.equals(parameterType)) {
	   	return CommandMatcher;
	   }
	   if(Map.class.equals(parameterType)) {
	   	return AttributeMatcher;
	   }
	   if(DeviceDriverContext.class.equals(parameterType)) {
	   	return ContextMatcher;
	   }
	   throw new IllegalArgumentException("Illegal method definition for [" + command + "].  Unsupported parameter [" + parameterType + "].  Only String, Map and Device are allowed for CapabilityDriver methods.");
   }

	private static Function<Object, ListenableFuture<MessageBody>> extractResponseWrapper(CommandDefinition command, Method m) {
		boolean methodIsAsync = ListenableFuture.class.equals(m.getReturnType());
		Type returnType = m.getGenericReturnType();
		Map<String, AttributeDefinition> returnDefinitions = command.getReturnParameters();

	   if(returnDefinitions.size() > 0 && !returnDefinitions.containsKey("response") && !returnDefinitions.containsKey("attributes")) {
	      throw new IllegalArgumentException("The reflective command handler only supports commands with no return parameters, or 1 return parameter named 'response' or 'attribute'");
	   }

		AttributeType responseType = VoidType.INSTANCE;
		if(returnDefinitions.containsKey("response")) {
		   responseType = returnDefinitions.get("response").getAttributeType();
		} else if(returnDefinitions.containsKey("attributes")) {
		   responseType = returnDefinitions.get("attributes").getAttributeType();
		}
		if(methodIsAsync) {
			if(returnType instanceof ParameterizedType) {
				returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
			}
			else if(returnType instanceof Class) {
				LOGGER.warn("Missing type information due to erasures, possible cast issues");
				returnType = responseType.getJavaType();
			}
			else {
				throw new IllegalArgumentException("Invalid return type [" + m.getGenericReturnType() + "] expected [" + responseType + "] or [ListenableFuture<" + responseType + ">]");
			}
		}

		if(!responseType.isAssignableFrom(returnType)) {
			throw new IllegalArgumentException("Invalid return type [" + m.getGenericReturnType() + "] expected [" + responseType + "] or [ListenableFuture<" + responseType + ">]");
		}
		com.google.common.base.Function<Object, MessageBody> translator = createTranslator(command);
		if(methodIsAsync) {
			return (o) -> ReflectiveCommandHandler.translateFromAsync(o, translator);
		}
		else {
			return (o) -> ReflectiveCommandHandler.translateFromSync(o, translator);
		}
	}

   private static com.google.common.base.Function<Object, MessageBody> createTranslator(final CommandDefinition command) {
	   final String name = command.getName() + "Response";
	   if(command.getReturnParameters().isEmpty()) {
	      return (o) -> {
	         return MessageBody.buildMessage(name, Collections.emptyMap());
	      };
	   }

	   Map<String, AttributeDefinition> returnDefinitions = command.getReturnParameters();
	   final AttributeType type = returnDefinitions.containsKey("response") ? returnDefinitions.get("response").getAttributeType() : returnDefinitions.get("attributes").getAttributeType();

		return (o) -> {
			return MessageBody.buildMessage(name, Collections.singletonMap("response", type.coerce(o)));
		};
   }

	private static ListenableFuture<MessageBody> translateFromSync(Object o, com.google.common.base.Function<Object, MessageBody> translator) {
		MessageBody event = translator.apply(o);
		return Futures.immediateFuture(event);
	}

	private static ListenableFuture<MessageBody> translateFromAsync(Object future, com.google.common.base.Function<Object, MessageBody> translator) {
		return Futures.transform((ListenableFuture<Object>) future, translator, MoreExecutors.directExecutor());
	}

	private final Method method;
	private final Object instance;
	private final List<ArgumentMatcher> matchers;
	private final Function<Object, ListenableFuture<MessageBody>> responseWrapper;

	private ReflectiveCommandHandler(
			Method method,
			Object instance,
			List<ArgumentMatcher> matchers,
			Function<Object, ListenableFuture<MessageBody>> responseWrapper
	) {
		this.method = method;
		this.instance = instance;
		this.matchers = matchers;
		this.responseWrapper = responseWrapper;
	}

	public Object [] toArgs(DeviceDriverContext context, MessageBody command) {
		Object [] args = new Object[matchers.size()];
		for(int i=0; i<matchers.size(); i++) {
			args[i] = matchers.get(i).getArgument(context, command);
		}
		return args;
	}

	@Override
   public boolean handleEvent(DeviceDriverContext context, MessageBody event) {
      Object [] args = toArgs(context, event);
      try {
         Object response = method.invoke(instance, args);
         Futures.addCallback(
               responseWrapper.apply(response),
               new FutureCallback<MessageBody>() {

                  @Override
                  public void onSuccess(MessageBody event) {
                     context.respondToPlatform(event);
                  }

                  @Override
                  public void onFailure(Throwable t) {
                     ErrorEvent event = Errors.fromException(t);
                     context.respondToPlatform(event);
                  }

               },
			   MoreExecutors.directExecutor()
         );
      }
      catch(InvocationTargetException e) {
         context.respondToPlatform(Errors.fromException(e.getCause()));
      }
      catch (Exception e) {
         context.respondToPlatform(Errors.fromException(e));
      }
      return true;
   }

	private static interface ArgumentMatcher {

		public Object getArgument(DeviceDriverContext context, MessageBody command);
	}

	private static final ArgumentMatcher CommandMatcher = (context, command) -> command.getMessageType();
	private static final ArgumentMatcher AttributeMatcher = (context, command) -> command.getAttributes();
	private static final ArgumentMatcher ContextMatcher = (context, command) -> context;

}

