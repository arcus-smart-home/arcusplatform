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
package com.iris.messages.model.support;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Informational annotation that marks a mutator (e.g. {@code setXxx()}, {@code clearXxx()}) as not having its changes
 * persisted by a normal {@code *DAO.save(entity)} call.
 * <p>
 * Should be used to mark mutators of "read-only" properties that require alternative DAO calls to have their changes
 * persisted.
 * 
 * @author Dan Ignat
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface TransientMutator
{
   /**
    * The DAO method(s), if any, that can actually persist this mutator's changes
    */
   String[] persistedBy() default "";
}

