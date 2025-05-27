package io.ast.jneurocarto.javafx.view;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Provides.class)
public @interface Provide {

    Class<? extends Plugin> value();

    String[] name() default {};
}
