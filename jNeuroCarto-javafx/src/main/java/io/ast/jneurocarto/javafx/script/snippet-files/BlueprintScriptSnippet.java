package io.ast.jneurocarto.javafx.script.snippets;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.script.BlueprintScript;
import io.ast.jneurocarto.javafx.script.ScriptParameter;

@NullMarked
public class BlueprintScriptSnippet<C> {

    // @start region="BlueprintScript on class"
    @BlueprintScript() // as a collection
    @RequestChannelmap(probe = ProbeDescription.class) // declare all method are required certain probe    // @replace substring="ProbeDescription.class" replacement="..."
    public final class BlueprintScriptCollection {
        // collect all methods and inner classes that annotated by BlueprintScript
    }
    // @end

    // @start region="BlueprintScript on method"
    @BlueprintScript(value = "name", description = """
      description
      """)
    @RequestChannelmap(code = "...") // declare required on certain probe
    public void method(BlueprintAppToolkit<C> bp, // or BlueprintToolkit, Blueprint. Required.      // @highlight regex="public|BlueprintAppToolkit"
                       @ScriptParameter(value = "parameter", defaultValue = "0",
                         description = "parameter description") int parameter) {
        // do something
    }
    // @end

    // @start region="BlueprintScript on inner class"
    @BlueprintScript(value = "name", description = """
      description
      """)
    @RequestChannelmap(code = "...", create = false)
    public static class Method<C> implements Runnable {                                             // @highlight regex="public|static|Runnable"  @replace substring="<C>" replacement=""

        @ScriptParameter(value = "parameter", defaultValue = "0",
          description = "parameter description")
        public int parameter; // fields are parameters                                              // @highlight regex="public"

        private final BlueprintAppToolkit<C> bp;

        public Method(BlueprintAppToolkit<C> bp) { // or BlueprintToolkit, Blueprint, no-arg.       // @highlight regex="public"
            this.bp = bp;
        }

        @Override
        public void run() {
            // do something
        }
    }
    // @end

}