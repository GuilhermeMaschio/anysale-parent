package com.anysale.lead.idempotency;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    String operation();
    String resourceIdParam() default "";
    long ttlSeconds() default 86400;
}
