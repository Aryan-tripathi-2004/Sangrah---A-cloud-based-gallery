package com.example.Auth.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark DTO fields as sensitive.
 * Fields annotated with @Sensitive will be masked in logs.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    /**
     * The mask character to use. Default is '*'.
     */
    char maskChar() default '*';

    /**
     * Number of characters to show from the beginning. Default is 0 (full masking).
     */
    int showFirst() default 0;

    /**
     * Number of characters to show from the end. Default is 0 (full masking).
     */
    int showLast() default 0;
}
