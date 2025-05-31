package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.core.RequestChannelmapInfo;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;

@NullMarked
public final class BlueprintScriptClassHandle extends BlueprintScriptHandle {

    public final Class<Runnable> declaredInner;
    private final @Nullable Class<?> blueprint;
    private final MethodHandle constructor;
    private final MethodHandle[] fields;

    public BlueprintScriptClassHandle(Class<?> declaredClass,
                                      Class<Runnable> declaredInner,
                                      String name,
                                      String description,
                                      @Nullable Class<?> blueprint,
                                      Parameter[] parameters,
                                      boolean isAsync,
                                      MethodHandle constructor,
                                      MethodHandle[] fields) {
        if (parameters.length != fields.length) throw new RuntimeException();
        super(declaredClass, name, description, parameters, isAsync);
        this.declaredInner = declaredInner;
        this.blueprint = blueprint;
        this.constructor = constructor;
        this.fields = fields;
    }

    @Override
    public @Nullable RequestChannelmapInfo requestChannelmap() {
        var c1 = declaredClass.getAnnotation(RequestChannelmap.class);
        var c2 = declaredInner.getAnnotation(RequestChannelmap.class);
        return RequestChannelmapInfo.of(c1, c2);
    }

    @Override
    public void invoke(BlueprintAppToolkit<?> toolkit, Object... arguments) throws Throwable {
        Runnable instance;
        if (blueprint == null) {
            instance = (Runnable) constructor.invoke();
        } else if (blueprint == Blueprint.class) {
            instance = (Runnable) constructor.invoke(toolkit.blueprint());
        } else {
            instance = (Runnable) constructor.invoke(toolkit);
        }

        for (int i = 0, length = parameters.length; i < length; i++) {
            var para = parameters[i];
            var field = fields[i];

            Object value;
            if (para.isVarArg()) {
                value = Arrays.copyOfRange(arguments, i, arguments.length);
            } else {
                value = arguments[i];
            }

            field.bindTo(instance).invoke(value);
        }

        instance.run();
    }

}
