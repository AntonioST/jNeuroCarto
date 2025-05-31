package io.ast.jneurocarto.javafx.script;

import java.lang.annotation.*;

/**
 * Declare a blueprint script method or collection.
 * <p/>
 * <h3>Use on a top-level class.</h3>
 * It works as a collection.
 * <br/>
 * {@snippet file = "BlueprintScriptSnippet.java" region = "BlueprintScript on class"}
 * <br/>
 * <h3>Use on a method.</h3>
 * It works as a script method.
 * <br/>
 * {@snippet file = "BlueprintScriptSnippet.java" region = "BlueprintScript on method"}
 * <h3>Use on an inner class.</h3>
 * It works as a script method.
 * {@snippet file = "BlueprintScriptSnippet.java" region = "BlueprintScript on inner class"}
 */
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
