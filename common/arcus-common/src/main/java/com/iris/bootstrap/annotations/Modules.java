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
package com.iris.bootstrap.annotations;

import com.google.inject.Module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares module dependencies for a Guice module.
 * Modules listed in {@code value} or {@code include} will be automatically
 * installed when this module is loaded.
 *
 * Drop-in replacement for com.netflix.governator.annotations.Modules.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Modules {
   Class<? extends Module>[] value() default {};
   Class<? extends Module>[] include() default {};
}
