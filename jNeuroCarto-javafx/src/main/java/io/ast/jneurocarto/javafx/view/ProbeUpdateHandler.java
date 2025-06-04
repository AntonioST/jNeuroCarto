package io.ast.jneurocarto.javafx.view;

import java.util.List;

import io.ast.jneurocarto.core.ElectrodeDescription;

/**
 * Receive channelmap and blueprint update event.
 *
 * @param <T> channelmap type
 */
public interface ProbeUpdateHandler<T> {

    void onProbeUpdate(T chmap, List<ElectrodeDescription> blueprint);
}
