package io.ast.jneurocarto.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.blueprint.Blueprint;

@NullMarked
public interface ElectrodeSelector {

    /**
     * {@return get option map}.
     */
    default Map<String, String> getOptions() {
        return Map.of();
    }

    /**
     * set options by a map.
     *
     * @param options option map
     */
    default void setOptions(Map<String, String> options) {
        options.forEach(this::setOption);
    }

    /**
     * set option with a string value.
     *
     * @param name  option name
     * @param value option value
     */
    default void setOption(String name, String value) {
    }

    /**
     * Return a new channelmap {@code T} with electrode selected based on the {@code blueprint}.
     * <br/>
     * Note: The implement may modify the content of the {@code blueprint}. It is better to
     * pass its copy via {@link Blueprint#Blueprint(Blueprint)}.
     * <br/>
     * Note: The implement should not modify the channelmap stored in the {@code blueprint}.
     * It should do the electrode selection and modify the new channelmap from
     * {@link Blueprint#newChannelmap()} instead.
     * <br/>
     * Note: The implement should not keep the selection internal state. It is not guarantee
     * that the selector will be reused or not in any case.
     *
     * @param blueprint blueprint
     * @param <T>       channelmap
     * @return a new channelmap
     */
    <T> T select(Blueprint<T> blueprint);

    /**
     * Return a new channelmap {@code T} with electrode selected based on the {@code blueprint}.
     *
     * @param desp      probe description
     * @param chmap     channelmap
     * @param blueprint blueprint
     * @param <T>       channelmap type
     * @return a new channelmap
     * @see #select(Blueprint)
     */
    default <T> T select(ProbeDescription<T> desp, T chmap, List<ElectrodeDescription> blueprint) {
        return select(new Blueprint<>(desp, chmap, blueprint));
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Selector {
        String value();
    }
}
