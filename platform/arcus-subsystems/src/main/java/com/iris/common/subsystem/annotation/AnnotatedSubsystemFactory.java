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
package com.iris.common.subsystem.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.event.SubsystemEventAndContext;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ListenerList;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ModelReportEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnReport;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.reflection.MethodInvokerFactory;
import com.iris.reflection.Methods;
import com.iris.util.IrisCollections;

/**
 * 
 */
public class AnnotatedSubsystemFactory {
   private static final Logger logger = LoggerFactory.getLogger(AnnotatedSubsystemFactory.class);
   
   private static final MethodInvokerFactory<SubsystemEventAndContext, MessageBody> RequestHandlerFactory =
         Methods
            .buildInvokerFactory(new RequestHandlerResolverFactory())
            .withExceptionHandler(new RequestExceptionHandler())
            .build();
   private static final MethodInvokerFactory<SubsystemEventAndContext, Void> MessageReceivedFactory =
         Methods
            .buildInvokerFactory(new MessageReceivedResolverFactory())
            .withExceptionHandler(Methods.logExceptions(logger))
            .build();
   private static final MethodInvokerFactory<SubsystemEventAndContext, Void> ModelAddedFactory =
         Methods
            .buildInvokerFactory(new EventResolverFactory(ModelAddedEvent.class))
            .withExceptionHandler(Methods.logExceptions(logger))
            .build();
   private static final MethodInvokerFactory<SubsystemEventAndContext, Void> ModelChangedFactory =
         Methods
            .buildInvokerFactory(new EventResolverFactory(ModelChangedEvent.class))
            .withExceptionHandler(Methods.logExceptions(logger))
            .build();
   private static final MethodInvokerFactory<SubsystemEventAndContext, Void> AttrReportFactory =
      Methods
         .buildInvokerFactory(new EventResolverFactory(ModelReportEvent.class))
         .withExceptionHandler(Methods.logExceptions(logger))
         .build();
   private static final MethodInvokerFactory<SubsystemEventAndContext, Void> ModelRemovedFactory =
         Methods
            .buildInvokerFactory(new ModelRemovedResolverFactory())
            .withExceptionHandler(Methods.logExceptions(logger))
            .build();
   private static final MethodInvokerFactory<SubsystemEventAndContext, Void> ScheduledEventFactory =
         Methods
            .buildInvokerFactory(new EventResolverFactory(ScheduledEvent.class))
            .withExceptionHandler(Methods.logExceptions(logger))
            .build();
   
   
   public static Map<String, Function<SubsystemEventAndContext, MessageBody>> createRequestHandlers(final Object o) {
      final Map<String, Function<SubsystemEventAndContext, MessageBody>> handlers = new HashMap<String, Function<SubsystemEventAndContext, MessageBody>>();
      visitAnnotatedMethod(
            o.getClass(),
            Request.class,
            new AnnotatedMethodVisitor<Request>() {
               @Override
               public boolean visit(Request annotation, Method input) {
                  Function<SubsystemEventAndContext, MessageBody> delegate = RequestHandlerFactory.wrapWithThis(input, o);
                  Function<SubsystemEventAndContext, MessageBody> old = handlers.put(annotation.value(), delegate);
                  if(old != null) {
                     throw new IllegalArgumentException("Multiple handlers defined for " + annotation.value() + "\n"
                           + "\tOn:  " + old + "\n"
                           + "\tAnd: " + delegate
                     );
                  }
                  return true;
               }
            });
         return handlers;
   }
   
   public static ListenerList<SubsystemEventAndContext> createMessageReceivedListeners(final Object o) {
      final ListenerList<SubsystemEventAndContext> listeners = new ListenerList<>();
      visitAnnotatedMethod(
         o.getClass(),
         OnMessage.class,
         new AnnotatedMethodVisitor<OnMessage>() {
            @Override
            public boolean visit(OnMessage annotation, Method input) {
               List<Predicate<SubsystemEventAndContext>> predicates = new ArrayList<Predicate<SubsystemEventAndContext>>(3);
               
               // TODO instanceOf MessageReceivedEvent?
               
               String [] from = annotation.from();
               if(from.length > 0) {
                  predicates.add(isMessageFrom(from));
               }
               
               String [] types = annotation.types();
               if(types.length > 0) {
                  predicates.add(isMessageOfType(types));
               }
               
               Predicate<SubsystemEventAndContext> filter = and(predicates);
               Function<SubsystemEventAndContext, Void> delegate = MessageReceivedFactory.wrapWithThis(input, o);
               listeners.addListener(new FilteredListener(filter, delegate));
               return true;
            }
         });
      return listeners;
   }
   
   public static ListenerList<SubsystemEventAndContext> createAddedListeners(final Object o) {
      final ListenerList<SubsystemEventAndContext> listeners = new ListenerList<>();
      visitAnnotatedMethod(
         o.getClass(),
         OnAdded.class,
         new AnnotatedMethodVisitor<OnAdded>() {
            @Override
            public boolean visit(OnAdded annotation, Method input) {
               String query = annotation.query();
               Function<SubsystemEventAndContext, Void> delegate = ModelAddedFactory.wrapWithThis(input, o);
               listeners.addListener(new FilteredListener(new AddressableModelFilter(query), delegate));
               return true;
            }
         });
      return listeners;
   }
   
   public static ListenerList<SubsystemEventAndContext> createValueChangedListeners(final Object o) {
      final ListenerList<SubsystemEventAndContext> listeners = new ListenerList<>();
      visitAnnotatedMethod(
         o.getClass(),
         OnValueChanged.class,
         new AnnotatedMethodVisitor<OnValueChanged>() {
            @Override
            public boolean visit(OnValueChanged annotation, Method input) {
               String query = annotation.query();
               String [] attributes = annotation.attributes();
               Function<SubsystemEventAndContext, Void> delegate = ModelChangedFactory.wrapWithThis(input, o);
               Predicate<SubsystemEventAndContext> filter = attributes.length == 0 ? new AddressableModelFilter(query) : new ValueChangedModelFilter(query, attributes);
               listeners.addListener(new FilteredListener(filter, delegate));
               return true;
            }
         });
      return listeners;
   }

   public static ListenerList<SubsystemEventAndContext> createAttrReportListeners(final Object o) {
      final ListenerList<SubsystemEventAndContext> listeners = new ListenerList<>();
      visitAnnotatedMethod(
         o.getClass(),
         OnReport.class,
         new AnnotatedMethodVisitor<OnReport>() {
            @Override
            public boolean visit(OnReport annotation, Method input) {
               String query = annotation.query();
               Function<SubsystemEventAndContext, Void> delegate = AttrReportFactory.wrapWithThis(input, o);
               Predicate<SubsystemEventAndContext> filter = new AddressableModelFilter(query);
               listeners.addListener(new FilteredListener(filter, delegate));
               return true;
            }
         });
      return listeners;
   }

   public static ListenerList<SubsystemEventAndContext> createRemovedListeners(final Object o) {
      final ListenerList<SubsystemEventAndContext> listeners = new ListenerList<>();
      visitAnnotatedMethod(
         o.getClass(),
         OnRemoved.class,
         new AnnotatedMethodVisitor<OnRemoved>() {
            @Override
            public boolean visit(OnRemoved annotation, Method input) {
               String query = annotation.query();
               Function<SubsystemEventAndContext, Void> delegate = ModelRemovedFactory.wrapWithThis(input, o);
               Predicate<SubsystemEventAndContext> filter = new ModelRemovedFilter(query);
               listeners.addListener(new FilteredListener(filter, delegate));
               return true;
            }
         });
      return listeners;
   }
   
   public static ListenerList<SubsystemEventAndContext> createScheduledEventListeners(final Object o) {
      final ListenerList<SubsystemEventAndContext> listeners = new ListenerList<>();
      visitAnnotatedMethod(
         o.getClass(),
         OnScheduledEvent.class,
         new AnnotatedMethodVisitor<OnScheduledEvent>() {
            @Override
            public boolean visit(OnScheduledEvent annotation, Method input) {
               final Function<SubsystemEventAndContext, Void> listener = ScheduledEventFactory.wrapWithThis(input, o);
               listeners.addListener(new Listener<SubsystemEventAndContext>() {
                  @Override
                  public void onEvent(SubsystemEventAndContext event) {
                     listener.apply(event);
                  }
                  
                  @Override
                  public int hashCode() { return listener.hashCode(); }
                  
                  @Override
                  public boolean equals(Object o) { return listener.equals(o); }
                  
                  @Override
                  public String toString() { return listener.toString(); }
               });
               return true;
            }
         });
      return listeners;
   }
   
   private static Predicate<SubsystemEventAndContext> and(List<Predicate<SubsystemEventAndContext>> predicates) {
      if(predicates.isEmpty()) {
         return Predicates.alwaysTrue();
      }
      else if(predicates.size() == 1) {
         return predicates.get(0);
      }
      return Predicates.and(predicates);
   }
   
   private static Predicate<SubsystemEventAndContext> isMessageOfType(String[] types) {
      final Set<String> setOfTypes = IrisCollections.setOf(types);
      return new Predicate<SubsystemEventAndContext>() {

         @Override
         public boolean apply(SubsystemEventAndContext input) {
            return setOfTypes.contains(((MessageReceivedEvent) input.getEvent()).getMessage().getMessageType());
         }
         
         @Override
         public String toString() {
            return "Message type matches " + setOfTypes;
         }
      };
   }

   private static Predicate<SubsystemEventAndContext> isMessageFrom(String[] sources) {
      Set<Predicate<Address>> matchers = new HashSet<>(sources.length);
      for(String source: sources) {
         matchers.add(AddressMatchers.fromString(source));
      }
      final Predicate<Address> addressMatches = Predicates.or(matchers);
      return new Predicate<SubsystemEventAndContext>() {
         @Override
         public boolean apply(SubsystemEventAndContext input) {
            MessageReceivedEvent event = (MessageReceivedEvent) input.getEvent();
            return addressMatches.apply(event.getMessage().getSource());
         }
         
         @Override
         public String toString() {
            return "Message from " + addressMatches;
         }
      };
   }

   private static <A extends Annotation> void visitAnnotatedMethod(
         Class<?> type,
         final Class<A> annotationClass, 
         final AnnotatedMethodVisitor<A> visitor
   ) {
      Methods
         .findMethods()
         .annotatedWith(annotationClass)
         .build()
         .visit(type, new Predicate<Method>() {
            @Override
            public boolean apply(Method method) {
               return visitor.visit(method.getAnnotation(annotationClass), method);
            }
         });
   }
   
   private static interface AnnotatedMethodVisitor<A extends Annotation> {
      
      boolean visit(A annotation, Method method);
   }
   
   private static class FilteredListener implements Listener<SubsystemEventAndContext> {
      private final Predicate<? super SubsystemEventAndContext> predicate;
      private final Function<? super SubsystemEventAndContext, Void> delegate;
      
      FilteredListener(
            Predicate<? super SubsystemEventAndContext> predicate,
            Function<? super SubsystemEventAndContext, Void> delegate
      ) {
         this.predicate = predicate;
         this.delegate = delegate;
      }
      
      @Override
      public void onEvent(SubsystemEventAndContext event) {
         if(!predicate.apply(event)) {
            return;
         }
         
         delegate.apply(event);
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "FilteredListener [predicate=" + predicate + ", delegate="
               + delegate + "]";
      }
      
   }
   
   private static class AddressableModelFilter implements Predicate<SubsystemEventAndContext> {
      private final Predicate<Model> query;
      
      AddressableModelFilter(String query) {
         this("true".equals(query) ? Predicates.<Model>alwaysTrue() : ExpressionCompiler.compile(query));
      }
      
      AddressableModelFilter(Predicate<Model> query) {
         this.query = query;
      }
      
      @OnAdded
      public boolean apply(SubsystemEventAndContext eac) {
         if(query == null) {
            return true;
         }
         
         AddressableEvent event = eac.getEvent();
         SubsystemContext<?> context = eac.getContext();
         Address address = event.getAddress();
         Model m = context.models().getModelByAddress(address);
         
         if(m == null) {
            context.logger().warn("Ignoring added event for untracked model at address {}", address);
            return false;
         }
         
         if(!query.apply(m)) {
            return false;
         }
         
         return true;
      }
   }

   private static class ModelRemovedFilter implements Predicate<SubsystemEventAndContext> {
      private final Predicate<Model> query;
      
      ModelRemovedFilter(String query) {
         this("true".equals(query) ? null : ExpressionCompiler.compile(query));
      }
      
      ModelRemovedFilter(Predicate<Model> query) {
         this.query = query;
      }
      
      @OnAdded
      public boolean apply(SubsystemEventAndContext eac) {
         AddressableEvent event = eac.getEvent();
         if(!(event instanceof ModelRemovedEvent)) {
            return false;
         }
         
         if(query == null) {
            return true;
         }
         
         SubsystemContext<?> context = eac.getContext();
         Model m = ((ModelRemovedEvent) event).getModel();
         
         if(m == null) {
            context.logger().warn("Ignoring removed event for null model at address {}", event.getAddress());
            return false;
         }
         
         if(!query.apply(m)) {
            return false;
         }
         
         return true;
      }
   }

   private static class ValueChangedModelFilter extends AddressableModelFilter {
      private final Set<String> attributes;
      
      ValueChangedModelFilter(String query, String [] attributes) {
         super(query);
         this.attributes = IrisCollections.setOf(attributes);
      }
      
      @OnAdded
      public boolean apply(SubsystemEventAndContext eac) {
         AddressableEvent event = eac.getEvent();
         if(!(event instanceof ModelChangedEvent)) {
            return false;
         }
         
         ModelChangedEvent changed = (ModelChangedEvent) event;
         if(!attributes.contains(changed.getAttributeName())) {
            return false;
         }
         
         return super.apply(eac);
      }

   }

}

