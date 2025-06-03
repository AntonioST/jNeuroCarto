package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.core.RequestChannelmapInfo;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.view.Plugin;

@NullMarked
public final class BlueprintScriptClassHandle extends BlueprintScriptHandle {

    public final Class<Runnable> declaredInner;
    private final MethodHandle constructor;
    private final MethodHandle[] fields;

    public BlueprintScriptClassHandle(Class<?> declaredClass,
                                      Class<Runnable> declaredInner,
                                      String name,
                                      String description,
                                      @Nullable Class<?> blueprint,
                                      Class<? extends Plugin>[] plugins,
                                      Parameter[] parameters,
                                      boolean isAsync,
                                      MethodHandle constructor,
                                      MethodHandle[] fields) {
        if (parameters.length != fields.length) throw new RuntimeException();
        super(declaredClass, name, description, blueprint, plugins, parameters, isAsync);
        this.declaredInner = declaredInner;
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
        var instance = newInstance(toolkit);
        initFields(instance, arguments);
        instance.run();
    }

    private Runnable newInstance(BlueprintAppToolkit<?> toolkit) throws Throwable {
        MethodHandle h = prependParameters(constructor, toolkit);
        return (Runnable) h.invoke();
    }

    private void initFields(Runnable instance, Object[] arguments) throws Throwable {
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
    }

}
