package io.ast.jneurocarto.javafx.script;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BlueprintScript {

    String value() default "";
}
