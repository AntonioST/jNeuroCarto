package io.ast.jneurocarto.javafx.script;

import java.util.Arrays;
import java.util.Objects;

import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;

@BlueprintScript("debug")
public class DebugScript {

    @BlueprintScript(value = "echo", description = "echo arguments in log message area.")
    public void echo(BlueprintAppToolkit<Object> toolkit,
                     @ScriptParameter("args") PyValue... args) {
        toolkit.printLogMessage("");
        toolkit.printLogMessage(Arrays.stream(args).map(Objects::toString).toList());
    }
}
