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

    class RawString implements Function<PyValue, String> {
        @Override
        public String apply(PyValue v) {
            throw new UnsupportedOperationException();
        }
    }

    class AutoCasting implements Function<PyValue, PyValue> {
        @Override
        public PyValue apply(PyValue v) {
            throw new UnsupportedOperationException();
        }
    }

    Class<? extends Function<PyValue, ?>> converter() default AutoCasting.class;

}
