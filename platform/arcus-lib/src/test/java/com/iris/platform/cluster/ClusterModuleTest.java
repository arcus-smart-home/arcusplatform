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
package com.iris.platform.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.Test;

import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.LinkedKeyBinding;
import com.iris.platform.cluster.cassandra.CassandraClusterServiceDao;
import com.iris.platform.cluster.zookeeper.ZookeeperClusterServiceDao;

/**
 * Reads the bindings ClusterModule records rather than building an injector,
 * so no Cassandra or ZooKeeper is needed.
 */
public class ClusterModuleTest {

   @Test
   public void unsetUsesCassandra() throws Exception {
      // As Guice leaves it when cluster.service.dao is not configured.
      assertEquals(CassandraClusterServiceDao.class, daoBoundBy(new ClusterModule()));
   }

   @Test
   public void zookeeper() throws Exception {
      assertEquals(ZookeeperClusterServiceDao.class, daoBoundBy(module("zookeeper")));
   }

   @Test
   public void cassandra() throws Exception {
      assertEquals(CassandraClusterServiceDao.class, daoBoundBy(module("cassandra")));
   }

   @Test
   public void unknownFallsBackToCassandra() throws Exception {
      assertEquals(CassandraClusterServiceDao.class, daoBoundBy(module("etcd")));
   }

   private static ClusterModule module(String dao) throws Exception {
      ClusterModule module = new ClusterModule();
      Field field = ClusterModule.class.getDeclaredField("clusterServiceDao");
      field.setAccessible(true);
      field.set(module, dao);
      return module;
   }

   private static Class<?> daoBoundBy(ClusterModule module) {
      Key<ClusterServiceDao> key = Key.get(ClusterServiceDao.class);
      for (Element element : Elements.getElements(module)) {
         if (element instanceof Binding && key.equals(((Binding<?>) element).getKey())) {
            return ((LinkedKeyBinding<?>) element).getLinkedKey().getTypeLiteral().getRawType();
         }
      }
      fail("ClusterModule bound no ClusterServiceDao");
      return null;
   }
}
