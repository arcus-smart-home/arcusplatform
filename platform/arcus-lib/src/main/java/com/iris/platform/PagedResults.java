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

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;

/**
 * 
 */
public class PagedResults<T> {
   @SuppressWarnings("rawtypes")
   private static final PagedResults EMPTY_PAGE = newPage(ImmutableList.of());
   
   @SuppressWarnings("unchecked")
   public static <T> PagedResults<T> emptyPage() {
      return EMPTY_PAGE;
   }
   
   public static <T> PagedResults<T> newPage(List<T> results) {
      return new PagedResults<>(results, null, UNKNOWN_COUNT);
   }
   
   public static <T> PagedResults<T> newPage(List<T> results, String nextToken) {
      return new PagedResults<>(results, nextToken, UNKNOWN_COUNT);
   }
   
   public static <T> PagedResults<T> newPage(List<T> results, String nextToken, long totalCount) {
      return new PagedResults<>(results, nextToken, totalCount);
   }
   
   public static final long UNKNOWN_COUNT = -1;
   
   private final String nextToken;
   private final List<T> results;
   private final long total;

   /**
    * 
    */
   protected PagedResults(List<T> results, @Nullable String nextToken, long totalCount) {
      this.results = results;
      this.nextToken = nextToken;
      this.total = totalCount;
   }

   /**
    * The token to be used for getting the next page of the results,
    * if this is {@code null} there is no next page.
    * @return the nextToken
    */
   @Nullable
   public String getNextToken() {
      return nextToken;
   }

   /**
    * @return the results
    */
   public List<T> getResults() {
      return results;
   }

   /**
    * @return the total
    */
   public long getTotalCount() {
      return total;
   }

   public boolean isTotalCountKnown() {
      return total > UNKNOWN_COUNT;
   }
   
   public boolean hasNextPage() {
      return nextToken != null;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "PagedResults [nextToken=" + (hasNextPage() ? nextToken : "<none>") + ", results=" + results
            + ", totalCount=" + (isTotalCountKnown() ? total : "<unkown>") + "]";
   }

}

