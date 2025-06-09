package io.ast.jneurocarto.core;

import java.util.Map;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.blueprint.Blueprint;

/// An electrode selector.
///
/// ### Declaration
///
/// To create a new {@link ElectrodeSelector}, it should be beside the corresponding {@link ProbeDescription}
/// (same package).
///
/// {@snippet lang = "java":
///
/// @RequestChannelmap(value = "my-probe") // 1
/// public class MySelector implements ElectrodeSelector {
///     public String name() {
///        return "my-selector"; // 2
///     }
/// }
///}
///
/// 1. declare this selector is specific for particular probe type.
/// 2. the name of the selector, which is used in {@link ProbeDescription#getElectrodeSelectors()}
///     and {@link ProbeDescription#newElectrodeSelector(String)}
///
/// ### stateful or stateless?
///
/// The selector usually only used once.
/// @see RandomElectrodeSelector
@NullMarked
public interface ElectrodeSelector {

    String name();

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

}
