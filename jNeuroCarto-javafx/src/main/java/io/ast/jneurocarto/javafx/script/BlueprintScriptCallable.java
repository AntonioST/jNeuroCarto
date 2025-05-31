package io.ast.jneurocarto.javafx.script;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.RequestChannelmapInfo;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;

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

    default @Nullable RequestChannelmapInfo requestChannelmap() {
        return null;
    }

    boolean isAsync();

    void invoke(BlueprintAppToolkit<?> toolkit, Object... arguments) throws Throwable;

}
