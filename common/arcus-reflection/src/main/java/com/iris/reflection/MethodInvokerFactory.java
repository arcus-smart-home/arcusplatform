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
package com.iris.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

/**
 * 
 */
public class MethodInvokerFactory<I, R> {
   private final ArgumentResolverFactory<I, R> resolverFactory;
   private final Function<Throwable, R> exceptionHandler;

   /**
    * 
    */
   public MethodInvokerFactory(
         ArgumentResolverFactory<I, R> resolverFactory,
         Function<Throwable, R> exceptionHandler
   ) {
      this.resolverFactory = resolverFactory;
      this.exceptionHandler = exceptionHandler;
   }

   public Function<I, R> wrap(Method method) {
      Preconditions.checkNotNull(method, "method may not be null");
      return wrapWithThisProvider(
            method,
            Modifier.isStatic(method.getModifiers()) ? Functions.constant(null) : Functions.identity()
      );
   }
   
   public Function<I, R> wrapWithThis(Method method, Object ths) {
      Preconditions.checkNotNull(method, "method may not be null");
      return wrapWithThisProvider(
            method, 
            Functions.constant(ths)
      );
   }
   
   public Function<I, R> wrapWithThisProvider(Method method, Function<? super I, Object> thisProvider) {
      return new MethodInvoker<>(
            method, 
            thisProvider, 
            resolveArguments(method), 
            resolverFactory.getResolverForReturnType(method), 
            exceptionHandler
      );
   }
   
   @SuppressWarnings("unchecked")
   private Function<I, Object []> resolveArguments(Method m) {
      Type [] parameters = m.getGenericParameterTypes();
      if(parameters.length == 0) {
         return (Function<I, Object []>) EMPTY_RESOLVER;
      }
      
      Annotation[][] annotations = m.getParameterAnnotations();
      List<Function<? super I, ?>> resolvers = new ArrayList<>(parameters.length);
      for(int i=0; i<parameters.length; i++) {
      	Function<? super I, ?> resolver = resolverFactory.getResolverForParameter(m, parameters[i], annotations[i]);
      	Preconditions.checkArgument(resolver != null, "Unable to resolve parameter %s on %s", parameters[i], m);
         resolvers.add( resolver );
      }
      return new ArgumentResolver<I>(resolvers);
   }
   
   public static interface ArgumentResolverFactory<I, R> {
      
      Function<? super I, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations);

      Function<Object, R> getResolverForReturnType(Method method);
   
   }
   
   private static final Function<Object, Object[]> EMPTY_RESOLVER =
         Functions.constant(new Object [] { });
   
   private static class ArgumentResolver<I> implements Function<I, Object []> {
      private final List<Function<? super I, ?>> resolvers;
      
      ArgumentResolver(List<Function<? super I, ?>> resolvers) {
         this.resolvers = resolvers;
      }

      @Override
      public Object[] apply(I input) {
         int len = resolvers.size();
         Object [] rval = new Object[len];
         for(int i=0; i<len; i++) {
            rval[i] = resolvers.get(i).apply(input);
         }
         return rval;
      }
   }
}

