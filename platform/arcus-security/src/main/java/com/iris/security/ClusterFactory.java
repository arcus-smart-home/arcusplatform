/*
 * From a Shiro example with changes for IRIS
 *
 * Copyright (C) 2013 Les Hazlewood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.security;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import org.apache.shiro.ShiroException;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Factory;
import org.apache.shiro.util.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Obtains and returns a Cassandra Driver {@link CqlSession} object to be used for performing CQL3 queries.
 *
 * @since 2013-06-09
 */
public class ClusterFactory implements Factory<CqlSession>, Initializable, Destroyable {

   private static final Logger LOG = LoggerFactory.getLogger(ClusterFactory.class);

   private CqlSession session;

   private Set<String> contactPoints;
   private int port;

   public ClusterFactory() {
      this.contactPoints = new HashSet<String>();
      this.contactPoints.add("localhost");
      this.port = 9042; //cassandra default
   }

   public Set<String> getContactPoints() {
      return contactPoints;
   }

   public void setContactPoints(Set<String> contactPoints) {
      this.contactPoints = contactPoints;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public CqlSession getInstance() {
      if (session == null) {
         init();
      }
      return session;
   }

   public void init() throws ShiroException {
      if (session == null) {
         try {
            doInit();
         } catch (Exception e) {
            throw new ShiroException(e);
         }
      }
   }

   protected void doInit() throws Exception {
      if (session == null) {
         session = createSession();
      }
   }

   protected CqlSession createSession() {
      com.datastax.oss.driver.api.core.CqlSessionBuilder builder = CqlSession.builder();

      if (this.contactPoints != null && !this.contactPoints.isEmpty()) {
         for (String cp : this.contactPoints) {
            builder.addContactPoint(new InetSocketAddress(cp, this.port));
         }
      }

      CqlSession session = builder.build();

      Metadata metadata = session.getMetadata();

      LOG.info("Connected to Cassandra cluster: " + metadata.getClusterName().orElse("unknown"));
      for (Node node : metadata.getNodes().values()) {
         LOG.info("DataCenter: {}, Rack: {}, Host: {}",
                  new Object[]{node.getDatacenter(), node.getRack(), node.getEndPoint()});
      }

      return session;
   }

   public void destroy() throws Exception {
      try {
         if (session != null) {
            session.close();
         }
      } finally {
         session = null;
      }
   }
}
