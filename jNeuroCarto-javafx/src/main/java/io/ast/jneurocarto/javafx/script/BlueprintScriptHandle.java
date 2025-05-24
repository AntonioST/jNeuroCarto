package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BlueprintScriptHandle {

    public record ScriptParameter(String name, Class<?> type, @Nullable String defaultValue) {
    }

    public final Class<?> declaredClass;
    public final Method declaredMethod;
    public final String name;
    private final Class<?> blueprint;
    public final ScriptParameter[] parameters;
    private final MethodHandle handle;

    public BlueprintScriptHandle(Class<?> declared,
                                 Method method,
                                 String name,
                                 Class<?> blueprint,
                                 ScriptParameter[] parameters,
                                 MethodHandle handle) {
        this.declaredClass = declared;
        this.declaredMethod = method;
        this.name = name;
        this.blueprint = blueprint;
        this.parameters = parameters;
        this.handle = handle;
    }

    public void eval(String line) {
        //XXX Unsupported Operation BlueprintScriptHandle.eval
        throw new UnsupportedOperationException();
    }

    public void invoke(Object... arguments) {
        //XXX Unsupported Operation BlueprintScriptHandle.invoke
        throw new UnsupportedOperationException();
    }
}
