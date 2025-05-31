package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.core.RequestChannelmapInfo;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;

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
                                       boolean isAsync,
                                       MethodHandle handle) {
        super(declaredClass, name, description, parameters, isAsync);
        this.declaredMethod = declaredMethod;
        this.blueprint = blueprint;
        this.handle = handle;
    }

    @Override
    public @Nullable RequestChannelmapInfo requestChannelmap() {
        var c1 = declaredClass.getAnnotation(RequestChannelmap.class);
        var c2 = declaredMethod.getAnnotation(RequestChannelmap.class);
        return RequestChannelmapInfo.of(c1, c2);
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
