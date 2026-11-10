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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.iris.client.exception.ErrorResponseException;

public class KeyListStateTest {

   @Test
   public void aPlatformWithoutApiKeysIsUnsupported() {
      assertEquals(KeyListState.UNSUPPORTED, KeyListState.ofFailure(new ErrorResponseException("unsupported.message", "nope")));
   }

   // apikey:ListKeys is for the account owner only, so everyone else gets this whenever the place loads
   @Test
   public void aRefusalMeansTheLoginIsNotTheOwner() {
      assertEquals(KeyListState.NOT_OWNER, KeyListState.ofFailure(new ErrorResponseException("error.unauthorized", "only the account owner can manage API keys")));
   }

   @Test
   public void anythingElseIsAFailure() {
      assertEquals(KeyListState.FAILED, KeyListState.ofFailure(new ErrorResponseException("error.notfound", "account not found")));
      assertEquals(KeyListState.FAILED, KeyListState.ofFailure(new TimeoutException()));
      assertEquals(KeyListState.FAILED, KeyListState.ofFailure(null));
   }
}
