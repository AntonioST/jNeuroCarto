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

        public Parameter(
            String name,
            Class<?> type,
            String typeDesp,
            String defaultValue,
            @Nullable String description
        ) {
            this(name, type, typeDesp, defaultValue, description, ScriptParameter.AutoCasting.class, false);
        }

        public Parameter(
            String name,
            String typeDesp,
            String defaultValue,
            @Nullable String description
        ) {
            this(name, String.class, typeDesp, defaultValue, description, ScriptParameter.RawString.class, false);
        }

        public Parameter(
            String name,
            Class<?> type,
            String typeDesp,
            @Nullable String description
        ) {
            this(name, type, typeDesp, null, description, ScriptParameter.AutoCasting.class, false);
        }

        public Parameter(
            String name,
            String typeDesp,
            @Nullable String description
        ) {
            this(name, String.class, typeDesp, null, description, ScriptParameter.RawString.class, false);
        }

        public Parameter withDescription(String description) {
            return new Parameter(name, type, typeDesp, defaultValue, description, converter, isVarArg);
        }
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
