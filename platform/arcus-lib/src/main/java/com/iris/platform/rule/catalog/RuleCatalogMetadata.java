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
package com.iris.platform.rule.catalog;

import java.util.Date;

/**
 * 
 */
public class RuleCatalogMetadata {
   private Date version;
   private String hash;
   private String publisher;

   /**
    * @return the version
    */
   public Date getVersion() {
      return version;
   }
   
   /**
    * @param version the version to set
    */
   public void setVersion(Date version) {
      this.version = version;
   }
   
   /**
    * @return the hash
    */
   public String getHash() {
      return hash;
   }
   
   /**
    * @param hash the hash to set
    */
   public void setHash(String hash) {
      this.hash = hash;
   }
   
   /**
    * @return the publisher
    */
   public String getPublisher() {
      return publisher;
   }
   
   /**
    * @param publisher the publisher to set
    */
   public void setPublisher(String publisher) {
      this.publisher = publisher;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "RuleCatalogMetadata [version=" + version + ", hash=" + hash
            + ", publisher=" + publisher + "]";
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((hash == null) ? 0 : hash.hashCode());
      result = prime * result
            + ((publisher == null) ? 0 : publisher.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
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
      RuleCatalogMetadata other = (RuleCatalogMetadata) obj;
      if (hash == null) {
         if (other.hash != null) return false;
      }
      else if (!hash.equals(other.hash)) return false;
      if (publisher == null) {
         if (other.publisher != null) return false;
      }
      else if (!publisher.equals(other.publisher)) return false;
      if (version == null) {
         if (other.version != null) return false;
      }
      else if (!version.equals(other.version)) return false;
      return true;
   }
   
   
}

