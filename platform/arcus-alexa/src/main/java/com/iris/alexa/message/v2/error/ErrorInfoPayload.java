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
package com.iris.alexa.message.v2.error;

public abstract class ErrorInfoPayload implements ErrorPayload {

   private ErrorInfo errorInfo;

   protected ErrorInfoPayload(String code, String description) {
      ErrorInfo errorInfo = new ErrorInfo();
      errorInfo.setCode(code);
      errorInfo.setDescription(description);
      this.errorInfo = errorInfo;
   }

   public ErrorInfo getErrorInfo() {
      return errorInfo;
   }

   public void setErrorInfo(ErrorInfo errorInfo) {
      this.errorInfo = errorInfo;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [errorInfo=" + errorInfo + ']';
   }
}

