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
package com.iris.security;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by jwb on 1/30/15.
 *
 * Used from PersonDAOImpl as it is a private class
 */
public class ParsedEmail {
   private String domain;
   private String user_0_3;
   private String user;

   public static ParsedEmail parse(String email) {
      ParsedEmail parsed = new ParsedEmail();

      int atIndex = email.indexOf('@');
      if(atIndex < 1) {
         throw new IllegalArgumentException("Invalid email address: " + email);
      }
      parsed.domain = email.substring(atIndex + 1).toLowerCase();
      parsed.user = email.substring(0, atIndex).toLowerCase();

      if (parsed.user.length() > 3) {
         parsed.user_0_3 = parsed.user.substring(0, 3);
      } else {
         parsed.user_0_3 = parsed.user;
      }

      return parsed;
   }

   public String getDomain() {
      return domain;
   }

   public String getUser_0_3() {
      return user_0_3;
   }

   public String getUser() {
      return user;
   }
   
   public boolean isValid() {
      return !StringUtils.isEmpty(domain) && !StringUtils.isEmpty(user) && !StringUtils.isEmpty(user_0_3);
   }
}

