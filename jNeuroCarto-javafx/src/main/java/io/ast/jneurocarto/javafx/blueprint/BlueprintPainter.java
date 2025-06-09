package io.ast.jneurocarto.javafx.blueprint;

import java.util.Set;

/**
 * The blueprint painter, used as a communicating interface between
 * {@link BlueprintPlugin} and the {@link io.ast.jneurocarto.core.ProbeDescription ProbeDescription}.
 * <br/>
 * The subclass that implement this interface must put under the same package
 * with the corresponding probe {@link io.ast.jneurocarto.javafx.view.PluginProvider PluginProvider}.
 * It allows the {@link io.ast.jneurocarto.javafx.app.PluginSetupService PluginSetupService} to scan the classes
 * under the package and load via java reflection.
 * <br/>
 * The subclass must have a no-arg, public constructor.
 */
public interface BlueprintPainter<T> {

    enum Feature {
        conflict;
    }

    default Set<Feature> supportedFeatures() {
        return Set.of();
    }

    void changeFeature(BlueprintPaintingHandle<T> service);

    void changeChannelmap(BlueprintPaintingHandle<T> service);

    void plotBlueprint(BlueprintPaintingHandle<T> service);
}
