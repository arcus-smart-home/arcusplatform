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
package com.iris.core.dao.cassandra;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.easymock.EasyMock;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;

/**
 * A CqlSession for DAO tests, with no Cassandra behind it. It records the queries
 * prepared and the statements executed, with the values bound to them.
 */
final class RecordingCqlSession {

   /** A prepared query and the values bound to it. */
   static final class Bound {
      final String query;
      final List<Object> values;

      Bound(String query, List<Object> values) {
         this.query = query;
         this.values = values;
      }

      @Override
      public String toString() {
         return query + " " + values;
      }
   }

   interface Recorded {
      Bound recorded();
   }

   final CqlSession session;
   final List<String> prepared = new ArrayList<>();
   /** One entry per execute: the statement, or the statements in a batch. */
   final List<List<Bound>> executed = new ArrayList<>();
   /** What executing a statement returns; a batch is answered for its first statement. */
   Function<Bound, ResultSet> results = bound -> result(true, Collections.emptyList());

   RecordingCqlSession() {
      session = EasyMock.createMock(CqlSession.class);
      EasyMock.expect(session.prepare(EasyMock.anyString()))
            .andAnswer(() -> prepare((String) EasyMock.getCurrentArguments()[0]))
            .anyTimes();
      EasyMock.expect(session.execute(EasyMock.<Statement<?>>anyObject()))
            .andAnswer(() -> execute((Statement<?>) EasyMock.getCurrentArguments()[0]))
            .anyTimes();
      EasyMock.replay(session);
   }

   static ResultSet result(boolean applied, List<Row> rows) {
      ResultSet result = EasyMock.createNiceMock(ResultSet.class);
      EasyMock.expect(result.wasApplied()).andReturn(applied).anyTimes();
      EasyMock.expect(result.all()).andReturn(rows).anyTimes();
      EasyMock.expect(result.iterator()).andAnswer(rows::iterator).anyTimes();
      EasyMock.expect(result.one()).andAnswer(() -> rows.isEmpty() ? null : rows.get(0)).anyTimes();
      EasyMock.replay(result);
      return result;
   }

   private PreparedStatement prepare(String query) {
      prepared.add(query);
      // the statement carries its own query, since DAOs may cache prepared statements
      return proxy(PreparedStatement.class, (self, method, args) -> {
         if (method.getName().equals("bind")) {
            Object[] values = args == null || args.length == 0 ? new Object[0] : (Object[]) args[0];
            return bound(new Bound(query, Arrays.asList(values)));
         }
         return zero(method);
      });
   }

   private static BoundStatement bound(Bound bound) {
      return proxy(BoundStatement.class, (self, method, args) -> {
         if (method.getDeclaringClass() == Recorded.class) {
            return bound;
         }
         if (method.getReturnType().isInstance(self)) {
            // setters return a changed copy
            return self;
         }
         return zero(method);
      }, Recorded.class);
   }

   private ResultSet execute(Statement<?> statement) {
      List<Bound> statements = new ArrayList<>();
      if (statement instanceof BatchStatement) {
         for (BatchableStatement<?> child : (BatchStatement) statement) {
            statements.add(((Recorded) child).recorded());
         }
      }
      else {
         statements.add(((Recorded) statement).recorded());
      }
      executed.add(statements);
      return results.apply(statements.get(0));
   }

   private interface Handler {
      Object handle(Object self, Method method, Object[] args) throws Throwable;
   }

   @SuppressWarnings("unchecked")
   private static <T> T proxy(Class<T> type, Handler handler, Class<?>... more) {
      Class<?>[] interfaces = new Class<?>[more.length + 1];
      interfaces[0] = type;
      System.arraycopy(more, 0, interfaces, 1, more.length);
      return (T) Proxy.newProxyInstance(RecordingCqlSession.class.getClassLoader(), interfaces, (self, method, args) -> {
         if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
            case "equals":
               return self == args[0];
            case "hashCode":
               return System.identityHashCode(self);
            default:
               return type.getSimpleName();
            }
         }
         return handler.handle(self, method, args);
      });
   }

   /** null, or zero of the method's primitive return type. */
   private static Object zero(Method method) {
      Class<?> type = method.getReturnType();
      return type.isPrimitive() && type != void.class ? Array.get(Array.newInstance(type, 1), 0) : null;
   }
}
