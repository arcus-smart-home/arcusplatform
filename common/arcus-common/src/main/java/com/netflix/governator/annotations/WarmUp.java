package com.netflix.governator.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO: Remove once iris2 controller jars are replaced with open-source
 * implementations that use com.iris.bootstrap.annotations.WarmUp instead.
 *
 * Stub for the Governator @WarmUp annotation.
 * Closed-source iris2 controller jars were compiled against Governator
 * and reference this annotation. Providing it on the classpath allows
 * IrisLifecycleManager to discover and invoke @WarmUp methods on those classes.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WarmUp {
}
