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
package com.iris.messages.listener.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Request {
   public String value();
   /**
    * Indicates whether this handles object requests or
    * service requests.  
    * Defaults to false, indicating it handles object requests.
    * @return
    */
   public boolean service() default false;
   /**
    * Indicates whether there should be a return value from
    * this request handler.  Most of the time this shoudl be
    * true, however there are special cases (such as async responses)
    * where one may want to suppress a response.  In this case
    * the handler is responsible for placing a response on the bus itself.
    * @return
    */
   public boolean response() default true;
}

