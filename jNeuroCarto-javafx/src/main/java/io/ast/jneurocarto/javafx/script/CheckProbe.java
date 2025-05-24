package io.ast.jneurocarto.javafx.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.ast.jneurocarto.core.ProbeDescription;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckProbe {

    /**
     * {@return probe family name}
     */
    String value() default "";

    Class<? extends ProbeDescription> probe() default ProbeDescription.class;

    /**
     * {@return use channelmap code}
     */
    String code() default "";

    boolean create() default true;
}
