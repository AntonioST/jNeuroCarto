package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandles;
import java.util.List;

public interface BlueprintScriptProvider {

    List<BlueprintScriptCallable> getBlueprintScripts(MethodHandles.Lookup lookup);
}
