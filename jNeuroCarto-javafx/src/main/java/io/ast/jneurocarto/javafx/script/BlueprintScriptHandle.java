package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.view.Plugin;

@NullMarked
public sealed abstract class BlueprintScriptHandle implements BlueprintScriptCallable
    permits BlueprintScriptMethodHandle, BlueprintScriptClassHandle {
    public final Class<?> declaredClass;
    public final String name;
    public final String description;
    protected final @Nullable Class<?> blueprint;
    protected final Class<? extends Plugin>[] plugins;
    protected final Parameter[] parameters;
    public final boolean isAsync;

    public BlueprintScriptHandle(Class<?> declaredClass,
                                 String name,
                                 String description,
                                 @Nullable Class<?> blueprint,
                                 Class<? extends Plugin>[] plugins,
                                 Parameter[] parameters,
                                 boolean isAsync) {
        if (!(blueprint == null
              || blueprint == BlueprintAppToolkit.class
              || blueprint == BlueprintToolkit.class
              || blueprint == Blueprint.class)) {
            throw new RuntimeException("unsupported first parameter type : " + blueprint.getName());
        }

        this.declaredClass = declaredClass;
        this.name = name;
        this.description = description;
        this.blueprint = blueprint;
        this.plugins = plugins;
        this.parameters = parameters;
        this.isAsync = isAsync;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public List<Class<? extends Plugin>> requestPlugins() {
        return Arrays.asList(plugins);
    }

    @Override
    public Parameter[] parameters() {
        return parameters.clone();
    }

    @Override
    public boolean isAsync() {
        return isAsync;
    }

    protected MethodHandle prependParameters(MethodHandle handle, BlueprintAppToolkit<?> toolkit) {
        if (blueprint == BlueprintAppToolkit.class || blueprint == BlueprintToolkit.class) {
            handle = MethodHandles.insertArguments(handle, 0, toolkit);
        } else if (blueprint == Blueprint.class) {
            handle = MethodHandles.insertArguments(handle, 0, toolkit.blueprint());
        }

        for (var plugin : plugins) {
            var instance = toolkit.getPlugin(plugin)
                .orElseThrow(() -> new RuntimeException("plugin " + plugin.getSimpleName() + " not found."));
            handle = MethodHandles.insertArguments(handle, 0, instance);
        }

        return handle;
    }
}
