package io.ast.jneurocarto.javafx.script;

import java.util.Arrays;
import java.util.List;
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

    //    @BlueprintScript(value = "count", description = "count until", async = true)
    public void count(BlueprintAppToolkit<Object> toolkit,
                      @ScriptParameter("n") int n,
                      @ScriptParameter(value = "e", defaultValue = "0",
                        description = "throw error on end") boolean e) throws InterruptedException {
        var i = 0;
        while (i < n) {
            toolkit.printLogMessage("" + i);
            Thread.sleep(1000);
            i++;
        }

        if (e) {
            throw new RuntimeException("" + n);
        }

        toolkit.printLogMessage("" + n);
    }

    //    @BlueprintScript(value = "count-echo", description = "count and echo", async = true)
    public void countEcho(BlueprintAppToolkit<Object> toolkit,
                          @ScriptParameter("n") int n) throws InterruptedException {
        var plugin = toolkit.getPlugin(ScriptPlugin.class).orElseThrow(RuntimeException::new);

        var success = ScriptThread.awaitFxApplicationThread(() -> {
            if (plugin.selectScript("echo")) {
                plugin.setScriptInputLine("");
                return true;
            } else {
                return false;
            }
        }).getOrThrowRuntimeException();
        if (!success) return;

        var i = 0;
        while (i < n) {
            var j = i;
            ScriptThread.awaitFxApplicationThread(() -> {
                plugin.appendScriptInputValueText("" + j);
            });
            Thread.sleep(1000);
            i++;
        }

        ScriptThread.awaitFxApplicationThread((Runnable) plugin::runScript);

        ScriptThread.awaitFxApplicationThread(() -> {
            toolkit.printLogMessage("count");
            plugin.showAndRunScript("count", "" + n);
        });
        var error = plugin.waitScriptFinished("count");
        toolkit.printLogMessage(List.of("error?", Objects.toString(error)));
    }
}
