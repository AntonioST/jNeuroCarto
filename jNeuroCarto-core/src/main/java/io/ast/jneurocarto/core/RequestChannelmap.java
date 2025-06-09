package io.ast.jneurocarto.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate a plugin or anything else that it require a specific probe type
 * or a specific channelmap type. It allows a plugin to know this requirement before launch it.
 * The use site are documented in others which mention how they use this annotation.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestChannelmap {

    /**
     * {@return probe family name}
     */
    String value() default "";

    /**
     * Returns the class of a probe description. This priority is higher than {@link #value()}.
     *
     * @return the class of a probe description.
     */
    Class<? extends ProbeDescription> probe() default ProbeDescription.class;

    /**
     * {@return use channelmap code}
     */
    String code() default "";

    /**
     * Create the new probe if it is missing.
     * It works when {@link #code()} is set.
     * <br/>
     * The creation is handled by the application or the plugin.
     * This just give a hint and do not guarantee anything.
     *
     * @return create?
     */
    boolean create() default true;
}
