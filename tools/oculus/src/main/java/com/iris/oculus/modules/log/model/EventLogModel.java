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
package com.iris.oculus.modules.log.model;

import java.util.Date;

/**
 * 
 */
public class EventLogModel {
   private String type;
   private String address;
   private Date timestamp;
   private String correlationId; // if we have one...
   private String content;
   /**
    * @return the type
    */
   public String getType() {
      return type;
   }
   /**
    * @param type the type to set
    */
   public void setType(String type) {
      this.type = type;
   }
   /**
    * @return the address
    */
   public String getAddress() {
      return address;
   }
   /**
    * @param address the address to set
    */
   public void setAddress(String address) {
      this.address = address;
   }
   /**
    * @return the timestamp
    */
   public Date getTimestamp() {
      return timestamp;
   }
   /**
    * @param timestamp the timestamp to set
    */
   public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
   }
   /**
    * @return the correlationId
    */
   public String getCorrelationId() {
      return correlationId;
   }
   /**
    * @param correlationId the correlationId to set
    */
   public void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
   }
   /**
    * @return the content
    */
   public String getContent() {
      return content;
   }
   /**
    * @param content the content to set
    */
   public void setContent(String content) {
      this.content = content;
   }

}

