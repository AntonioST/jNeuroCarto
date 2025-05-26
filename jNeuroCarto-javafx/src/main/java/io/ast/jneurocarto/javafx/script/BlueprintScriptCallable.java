package io.ast.jneurocarto.javafx.script;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.RequestChannelmapType;

public interface BlueprintScriptCallable {

    record Parameter(
      String name,
      Class<?> type,
      String typeDesp,
      @Nullable String defaultValue,
      @Nullable String description,
      Class<? extends Function<PyValue, ?>> converter,
      boolean isVarArg
    ) {
    }

    String name();

    String description();

    Parameter[] parameters();

    default @Nullable RequestChannelmapType requestChannelmap() {
        return null;
    }

    void invoke(BlueprintAppToolkit<?> toolkit, Object... arguments) throws Throwable;

}
