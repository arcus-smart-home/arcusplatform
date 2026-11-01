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
package com.iris.modelmanager;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;

/**
 * A session whose prepared statements keep the values bound to them. A mocked
 * statement accepts anything, while the real one throws for a type the driver
 * has no codec for, so tests use {@link #assertDriverCanEncode()} to catch that.
 */
public class BindCapture {
   private final List<Object> values = new ArrayList<>();

   public CqlSession session() {
      BoundStatement bound = EasyMock.createNiceMock(BoundStatement.class);
      PreparedStatement prepared = (PreparedStatement) Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[] { PreparedStatement.class },
            (proxy, method, args) -> {
               if (!"bind".equals(method.getName())) {
                  throw new UnsupportedOperationException(method.getName());
               }
               values.addAll(Arrays.asList((Object[]) args[0]));
               return bound;
            });

      CqlSession session = EasyMock.createNiceMock(CqlSession.class);
      EasyMock.expect(session.prepare(EasyMock.anyString())).andReturn(prepared).anyTimes();
      EasyMock.replay(session, bound);
      return session;
   }

   public List<Object> values() {
      return values;
   }

   /** Encodes each bound value the way the driver would, throwing CodecNotFoundException if it can't. */
   public void assertDriverCanEncode() {
      for (Object value : values) {
         if (value != null) {
            CodecRegistry.DEFAULT.codecFor(value).encode(value, ProtocolVersion.DEFAULT);
         }
      }
   }
}
