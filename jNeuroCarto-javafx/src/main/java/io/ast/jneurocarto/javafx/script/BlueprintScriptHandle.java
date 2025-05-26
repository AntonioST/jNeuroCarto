package io.ast.jneurocarto.javafx.script;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.javafx.app.RequestChannelmapType;

@NullMarked
public sealed abstract class BlueprintScriptHandle implements BlueprintScriptCallable
  permits BlueprintScriptMethodHandle, BlueprintScriptClassHandle {
    public final Class<?> declaredClass;
    public final String name;
    public final String description;
    public final Parameter[] parameters;

    public BlueprintScriptHandle(Class<?> declaredClass,
                                 String name,
                                 String description,
                                 Parameter[] parameters) {
        this.declaredClass = declaredClass;
        this.name = name;
        this.description = description;
        this.parameters = parameters;
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
    public Parameter[] parameters() {
        return parameters;
    }

    public @Nullable RequestChannelmapType requestChannelmap(@Nullable CheckProbe primary) {
        String family = "";
        Class<? extends ProbeDescription> probe = ProbeDescription.class;
        String code = "";
        boolean create = false;

        if (primary != null) {
            family = primary.value();
            probe = primary.probe();
            code = primary.code();
            create = primary.create();
        }

        var check = declaredClass.getAnnotation(CheckProbe.class);
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


}
