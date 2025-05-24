package io.ast.jneurocarto.javafx.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScriptParameter {

    String value();

    String NO_DEFAULT = "--NO_DEFAULT--";

    String defaultValue() default NO_DEFAULT;

}
