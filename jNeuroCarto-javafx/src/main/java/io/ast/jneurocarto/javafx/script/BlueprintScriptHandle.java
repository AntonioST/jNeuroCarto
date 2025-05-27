package io.ast.jneurocarto.javafx.script;

import org.jspecify.annotations.NullMarked;

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
}
