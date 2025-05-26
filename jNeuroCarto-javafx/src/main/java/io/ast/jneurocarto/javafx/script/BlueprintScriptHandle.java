package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.RequestChannelmapType;

@NullMarked
public class BlueprintScriptHandle implements BlueprintScriptCallable {

    public final Class<?> declaredClass;
    public final Method declaredMethod;
    public final String name;
    public final String description;
    private final Class<?> blueprint;
    public final ScriptParameter[] parameters;
    private final MethodHandle handle;

    public BlueprintScriptHandle(Class<?> declaredClass,
                                 Method declaredMethod,
                                 String name,
                                 String description,
                                 Class<?> blueprint,
                                 ScriptParameter[] parameters,
                                 MethodHandle handle) {
        this.declaredClass = declaredClass;
        this.declaredMethod = declaredMethod;
        this.name = name;
        this.description = description;
        this.blueprint = blueprint;
        this.parameters = parameters;
        this.handle = handle;
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
    public ScriptParameter[] paramaters() {
        return parameters;
    }

    @Override
    public @Nullable RequestChannelmapType requestChannelmap() {
        String family = "";
        Class<? extends ProbeDescription> probe = ProbeDescription.class;
        String code = "";
        boolean create = false;

        var check = declaredMethod.getAnnotation(CheckProbe.class);
        if (check != null) {
            family = check.value();
            probe = check.probe();
            code = check.code();
            create = check.create();
        }

        check = declaredClass.getAnnotation(CheckProbe.class);
        if (check != null) {
            if (family.isEmpty()) family = check.value();
            if (probe == ProbeDescription.class) probe = check.probe();
            if (code.isEmpty()) code = check.code();
        }

        if (probe == ProbeDescription.class) {
            if (family.isEmpty()) return null;
            var ret = ProbeDescription.getProbeDescription(family);
            if (ret == null) return null;
            probe = ret.getClass();
        }

        if (code.isEmpty()) code = null;

        return new RequestChannelmapType(probe, code, create);
    }


    @Override
    public void invoke(BlueprintAppToolkit<?> toolkit, Object... arguments) throws Throwable {
        MethodHandle h;
        if (blueprint == Blueprint.class) {
            h = MethodHandles.insertArguments(handle, 0, toolkit.blueprint());
        } else {
            h = MethodHandles.insertArguments(handle, 0, toolkit);
        }
        h.invokeExact(arguments);
    }
}
