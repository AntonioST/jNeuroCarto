package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.RequestChannelmapType;

@NullMarked
public final class BlueprintScriptMethodHandle extends BlueprintScriptHandle {

    public final Method declaredMethod;
    private final Class<?> blueprint;
    private final MethodHandle handle;

    public BlueprintScriptMethodHandle(Class<?> declaredClass,
                                       Method declaredMethod,
                                       String name,
                                       String description,
                                       Class<?> blueprint,
                                       Parameter[] parameters,
                                       MethodHandle handle) {
        super(declaredClass, name, description, parameters);
        this.declaredMethod = declaredMethod;
        this.blueprint = blueprint;
        this.handle = handle;
    }

    @Override
    public @Nullable RequestChannelmapType requestChannelmap() {
        return requestChannelmap(declaredMethod.getAnnotation(CheckProbe.class));
    }

    @Override
    public void invoke(BlueprintAppToolkit<?> toolkit, Object... arguments) throws Throwable {
        MethodHandle h = handle;

        if (blueprint == Blueprint.class) {
            h = MethodHandles.insertArguments(h, 0, toolkit.blueprint());
        } else {
            h = MethodHandles.insertArguments(h, 0, toolkit);
        }

        var last = parameters[parameters.length - 1];
        if (last.isVarArg()) {
            h = h.asVarargsCollector(last.type().arrayType());
        }

        h.invokeWithArguments(arguments);
    }
}
