package io.ast.jneurocarto.javafx.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScriptParameter {

    String value();

    String type() default "";

    String NO_DEFAULT = "--NO_DEFAULT--";

    String defaultValue() default NO_DEFAULT;

    String description() default "";

    class Identify implements Function<String, String> {
        @Override
        public String apply(String s) {
            return s;
        }
    }

    Class<? extends Function<String, ?>> converter() default Identify.class;

}
