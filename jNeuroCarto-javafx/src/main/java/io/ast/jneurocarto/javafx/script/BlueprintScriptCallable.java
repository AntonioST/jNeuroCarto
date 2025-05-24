package io.ast.jneurocarto.javafx.script;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.RequestChannelmapType;

public interface BlueprintScriptCallable {

    record ScriptParameter(String name, Class<?> type, @Nullable String defaultValue) {
    }

    String name();

    ScriptParameter[] paramaters();

    default @Nullable RequestChannelmapType requestChannelmap() {
        return null;
    }

    void invoke(BlueprintAppToolkit<?> toolkit, Object... arguments) throws Throwable;

}
