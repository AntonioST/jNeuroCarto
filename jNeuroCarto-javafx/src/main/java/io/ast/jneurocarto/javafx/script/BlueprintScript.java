package io.ast.jneurocarto.javafx.script;

import java.lang.annotation.*;

/// Declare a blueprint script method or collection.
///
/// ### Use on a top-level class.
///
/// It works as a collection.
///
/// {@snippet class = BlueprintScriptSnippet region = "BlueprintScript on class"}
///
/// ### Use on a method.
///
/// It works as a script method.
///
/// {@snippet class = BlueprintScriptSnippet region = "BlueprintScript on method"}
///
/// ### Use on an inner class.
///
/// It works as a script method.
/// {@snippet class = BlueprintScriptSnippet region = "BlueprintScript on inner class"}
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BlueprintScript {

    String value() default "";

    String description() default "";

    /**
     * Is it an async method?
     *
     * @return
     */
    boolean async() default false;
}
