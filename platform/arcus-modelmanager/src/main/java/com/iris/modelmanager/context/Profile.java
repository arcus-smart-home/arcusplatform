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
package com.iris.modelmanager.context;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.ConsistencyLevel;

public class Profile {

   private List<String> nodes;
   private int port;
   private String username;
   private String password;
   private String keyspace;
   private ConsistencyLevel consistencyLevel;

   void setNodes(List<String> nodes) {
      this.nodes = Collections.unmodifiableList(nodes);
   }
   
   void setConsistencyLevel(ConsistencyLevel consistencyLevel) {
   	this.consistencyLevel = consistencyLevel;
   }

   void setPort(int port) {
      this.port = port;
   }

   void setUsername(String username) {
   	if(StringUtils.isNotBlank(username)) {
   		this.username = username;
   	}else{
   		this.username = null;
   	}
   }

   void setPassword(String password) {
   	if(StringUtils.isNotBlank(password)) {
   		this.password = password;
   	}else{
   		this.password = null;
   	}
   }

   void setKeyspace(String keyspace) {
      this.keyspace = keyspace;
   }

   public List<String> getNodes() {
      return nodes;
   }
   
   public ConsistencyLevel getConsistencyLevel() {
   	return consistencyLevel;
   }

   public int getPort() {
     return port;
   }

   public String getUsername() {
      return username;
   }

   public String getPassword() {
      return password;
   }

   public String getKeyspace() {
      return keyspace;
   }
}

