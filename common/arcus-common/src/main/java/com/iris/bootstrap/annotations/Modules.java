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
