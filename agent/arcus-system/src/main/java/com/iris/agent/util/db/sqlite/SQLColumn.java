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
package com.iris.agent.util.db.sqlite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target ({ElementType.FIELD})
public @interface SQLColumn {
   boolean isKey() default false;                      // If this is the primary key
   boolean createIndex() default false;                // Create an index based on this field.
   boolean isForeignKey() default false;               // If this is a foreign key.
   Class<?> foreignTableClass() default SQLNull.class; // The class table used for the foreign key.
   String foreignTableName() default "";               // If the class cannot be used use the name as a backup
   String foreignKey() default "";                     // Name of the key in the other foreign class.
   boolean cascadeDelete() default false;              // If there is a foreign key this will add in a cascade to the delete.
}

