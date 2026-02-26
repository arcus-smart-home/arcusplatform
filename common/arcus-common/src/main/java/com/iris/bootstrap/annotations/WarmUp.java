package com.iris.bootstrap.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called during the warm-up phase of lifecycle startup.
 * WarmUp methods are invoked after all @PostConstruct methods have completed.
 *
 * Drop-in replacement for com.netflix.governator.annotations.WarmUp.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WarmUp {
}
