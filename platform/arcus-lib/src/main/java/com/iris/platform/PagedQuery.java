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
package com.iris.platform;

/**
 * Generally this is a base class for strongly typed query classes.
 */
public class PagedQuery {
   private String token;
   private int limit;

   public PagedQuery() {
   	
   }
   
   public PagedQuery(PagedQuery query) {
		this.token = query.getToken();
		this.limit = query.getLimit();
	}

	/**
    * @return the token
    */
   public String getToken() {
      return token;
   }

   /**
    * @param token the token to set
    */
   public void setToken(String token) {
      this.token = token;
   }

   /**
    * @return the limit
    */
   public int getLimit() {
      if(limit <= 0) {
         return getDefaultLimit();
      }
      return limit;
   }

   /**
    * @param limit the limit to set
    */
   public void setLimit(int limit) {
      this.limit = limit;
   }

   protected int getDefaultLimit() {
      return 25;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "PagedQuery [token=" + token + ", limit=" + limit + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + limit;
      result = prime * result + ((token == null) ? 0 : token.hashCode());
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
      PagedQuery other = (PagedQuery) obj;
      if (limit != other.limit) return false;
      if (token == null) {
         if (other.token != null) return false;
      }
      else if (!token.equals(other.token)) return false;
      return true;
   }

}

