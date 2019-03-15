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
package com.iris.platform.history.appender.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.iris.messages.MessageBody;

public class CompositeMatcher implements Matcher{
   
   private final List<Matcher>matchers=new ArrayList<>();
   
   public CompositeMatcher(Matcher... matchers) {
      if(matchers != null) {
         this.matchers.addAll(Arrays.asList(matchers));
      }
   }
   
   @Override
   public MatchResults matches(MessageBody value) {
      for(Matcher matcher:matchers){
         MatchResults results = matcher.matches(value);
         if(results.isMatch()){
            return results;
         }
      }
      return MatchResults.FALSE;
   }

}

