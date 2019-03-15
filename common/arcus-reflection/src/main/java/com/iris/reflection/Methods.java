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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;


/**
 * 
 */
public class Methods {
   
   public static MethodDiscoveryBuilder findMethods() {
      return new MethodDiscoveryBuilder();
   }
   
   public static <I, R> MethodInvokerFactoryBuilder<I, R> buildInvokerFactory() {
      return new MethodInvokerFactoryBuilder<I,R>();
   }
   
   public static <I, R> MethodInvokerFactoryBuilder<I, R> buildInvokerFactory(ArgumentResolverFactory<I, R> resolverFactory) {
      return 
            Methods
               .<I,R>buildInvokerFactory()
               .withResolverFactory(resolverFactory);
   }
   
   @SuppressWarnings("unchecked")
   public static <R> Function<Throwable, R> rethrowExceptions() {
      return (Function<Throwable, R>) RethrowExceptionHandler;
   }
   
   public static Function<Throwable, Void> logExceptions(Logger logger) {
      return logExceptions(logger, Suppliers.<Void>ofInstance(null));
   }

   public static <R> Function<Throwable, R> logExceptionsAndReturn(Logger logger, R value) {
      return logExceptions(logger, Suppliers.ofInstance(value));
   }
   
   public static <R> Function<Throwable, R> logExceptions(Logger logger, Supplier<R> supplier) {
      Preconditions.checkNotNull(logger, "logger may not be null");
      Preconditions.checkNotNull(supplier, "supplier may not be null");
      return new LoggingExceptionHandler<R>(logger, supplier);
   }
   
   public static class MethodDiscoveryBuilder {
      private String name;
      private Set<Class<? extends Annotation>> annotations = new HashSet<>();
      private Predicate<Method> predicate;
      
      public MethodDiscoveryBuilder named(String name) {
         this.name = name;
         return this;
      }

      public MethodDiscoveryBuilder annotatedWith(Class<? extends Annotation> annotation) {
         Preconditions.checkNotNull(annotation, "annotation may not be null");
         Preconditions.checkArgument(isRuntimeRetained(annotation), "non-runtime annotations, can't be used here");

         this.annotations.add(annotation);
         return this;
      }
      
      public MethodDiscoveryBuilder annotatedWith(
            Class<? extends Annotation> annotation0,
            Class<? extends Annotation> annotation1,
            @SuppressWarnings("unchecked")
            Class<? extends Annotation>... annotationN
      ) {
         annotatedWith(annotation0);
         annotatedWith(annotation1);
         for(Class<? extends Annotation> annotation: annotationN) {
            annotatedWith(annotation);
         }
         return this;
      }
      
      public MethodDiscoveryBuilder annotatedWith(Collection<Class<? extends Annotation>> annotations) {
         for(Class<? extends Annotation> annotation: annotations) {
            annotatedWith(annotation);
         }
         return this;
      }
      
      public MethodDiscoverer build() {
         List<Predicate<Method>> predicates = new ArrayList<Predicate<Method>>();
         if(name != null) {
            predicates.add(new MethodNamed(name));
         }
         for(Class<? extends Annotation> annotation: annotations) {
            predicates.add(new AnnotatedWith(annotation));
         }
         if(predicate != null) {
            predicates.add(predicate);
         }
         
         Predicate<Method> delegate;
         if(predicates.isEmpty()) {
            delegate = Predicates.alwaysTrue();
         }
         else if(predicates.size() == 1) {
            delegate = predicates.get(0);
         }
         else {
            delegate = Predicates.and(predicates);
         }
         return new MethodDiscoverer(delegate);
      }
      
   }
   
   public static class MethodInvokerFactoryBuilder<I, R> {
      private ArgumentResolverFactory<I, R> resolverFactory;
      private Function<Throwable, R> exceptionHandler = Methods.<R>rethrowExceptions();
     
      public MethodInvokerFactoryBuilder() {
         
      }
      
      public MethodInvokerFactoryBuilder<I, R> withResolverFactory(ArgumentResolverFactory<I, R> resolverFactory) {
         this.resolverFactory = resolverFactory;
         return this;
      }
      
      public MethodInvokerFactoryBuilder<I, R> withExceptionHandler(Function<Throwable, R> exceptionHandler) {
         this.exceptionHandler = exceptionHandler;
         return this;
      }
      
      public MethodInvokerFactory<I, R> build() {
         return new MethodInvokerFactory<I, R>(resolverFactory, exceptionHandler);
      }
   }
   
   private static boolean isRuntimeRetained(Class<? extends Annotation> annotation) {
      Retention retention = annotation.getAnnotation(Retention.class);
      return retention != null && retention.value() == RetentionPolicy.RUNTIME; 
   }

   private static class MethodNamed implements Predicate<Method> {
      private final String name;
      
      MethodNamed(String name) {
         this.name = name;
      }
      
      @Override
      public boolean apply(Method input) {
         return name.equals(input.getName());
      }
      
      @Override
      public String toString() {
         return "method is named " + name;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((name == null) ? 0 : name.hashCode());
         return result;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         MethodNamed other = (MethodNamed) obj;
         if (name == null) {
            if (other.name != null) return false;
         }
         else if (!name.equals(other.name)) return false;
         return true;
      }

   }
   
   private static class AnnotatedWith implements Predicate<Method> {
      private final Class<? extends Annotation> annotation;
      
      AnnotatedWith(Class<? extends Annotation> annotation) {
         this.annotation = annotation;
      }
      
      @Override
      public boolean apply(Method input) {
         return input.getAnnotation(annotation) != null;
      }

      @Override
      public String toString() {
         return "method is annotated with @" + annotation.getSimpleName();
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result
               + ((annotation == null) ? 0 : annotation.hashCode());
         return result;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         AnnotatedWith other = (AnnotatedWith) obj;
         if (annotation == null) {
            if (other.annotation != null) return false;
         }
         else if (!annotation.equals(other.annotation)) return false;
         return true;
      }
      
   }

   private static final Function<Throwable, ?> RethrowExceptionHandler = 
         new Function<Throwable, Void>() {

            @Override
            public Void apply(Throwable cause) {
               if(cause == null) {
                  throw new UncheckedExecutionException("Unknown cause, root cause was null", new NullPointerException());
               }
               if(cause instanceof RuntimeException) {
                  throw (RuntimeException) cause;
               }
               if(cause instanceof Error) {
                  throw (Error) cause;
               }
               throw new UncheckedExecutionException(cause);
            }
      
   };
   
   private static class LoggingExceptionHandler<R> implements Function<Throwable, R> {
      private final Logger logger;
      private final Supplier<R> supplier;

      LoggingExceptionHandler(Logger logger, Supplier<R> supplier) {
         this.logger = logger;
         this.supplier = supplier;
      }
      
      @Override
      public R apply(Throwable cause) {
         logger.warn("Error while processing event", cause);
         return supplier.get();
      }
      
   }
}

