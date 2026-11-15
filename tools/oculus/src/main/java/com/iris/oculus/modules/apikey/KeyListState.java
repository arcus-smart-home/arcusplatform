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
package com.iris.oculus.modules.apikey;

import com.iris.client.exception.ErrorResponseException;
import com.iris.security.authz.AuthzUtil;

/** Whether the API key list for the current place can be shown. */
enum KeyListState {
   /** Loaded, or not asked for yet. */
   AVAILABLE,
   /** The platform has no API key service. */
   UNSUPPORTED,
   /** Only the place's account owner can manage its API keys. */
   NOT_OWNER,
   /** Anything else, which is reported. */
   FAILED;

   private static final String UNSUPPORTED_MESSAGE = "unsupported.message";

   /** The state after apikey:ListKeys failed with error. */
   static KeyListState ofFailure(Throwable error) {
      if (error instanceof ErrorResponseException) {
         String code = ((ErrorResponseException) error).getCode();
         if (UNSUPPORTED_MESSAGE.equals(code)) {
            return UNSUPPORTED;
         }
         if (AuthzUtil.UNAUTHORIZED_CODE.equals(code)) {
            return NOT_OWNER;
         }
      }
      return FAILED;
   }
}
