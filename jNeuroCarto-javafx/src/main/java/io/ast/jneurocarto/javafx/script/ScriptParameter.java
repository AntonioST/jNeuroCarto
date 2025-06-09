package io.ast.jneurocarto.javafx.script;

import java.lang.annotation.*;
import java.util.List;
import java.util.function.Function;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScriptParameter {

    /**
     * {@return parameter name}
     */
    String value();

    /**
     * The parameter index. It will be ignored when used on the parameters.
     * <p>
     * By default, annotated fields are resolved first.
     * Therefore, annotated method should explict index to insert itself to
     * correct parameter order.
     * The parameter order resolving method just use {@link List#add(int, Object)},
     * starting from the smallest member, without any additional checking.
     * Therefore, any two annotated members (field or method) use same index
     * will cause undetermined parameter order.
     *
     * @return The parameter index.
     */
    int index() default -1;

    /**
     * {@return parameter value label}
     */
    String label() default "";

    String NO_DEFAULT = "--NO_DEFAULT--";

    /**
     * @return default value in string form.
     */
    String defaultValue() default NO_DEFAULT;

    /**
     * {@return parameter description.}
     */
    String description() default "";

    class RawString implements Function<PyValue, String> {
        @Override
        public String apply(PyValue v) {
            throw new UnsupportedOperationException("special case");
        }
    }

    class AutoCasting implements Function<PyValue, PyValue> {
        @Override
        public PyValue apply(PyValue v) {
            throw new UnsupportedOperationException("special case");
        }
    }

    Class<? extends Function<PyValue, ?>> converter() default AutoCasting.class;

}
