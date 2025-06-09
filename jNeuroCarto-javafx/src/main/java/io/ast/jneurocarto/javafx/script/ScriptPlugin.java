package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.core.RequestChannelmapException;
import io.ast.jneurocarto.core.RequestChannelmapInfo;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.*;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.github.classgraph.ClassInfo;

public class ScriptPlugin extends InvisibleView {

    private final Application<Object> application;
    private final CartoConfig config;
    private final ProbeDescription<Object> probe;
    private final List<BlueprintScriptCallable> functions = new ArrayList<>();
    private ProbeView<Object> view;

    private final Logger log = LoggerFactory.getLogger(ScriptPlugin.class);

    public ScriptPlugin(Application<Object> application, CartoConfig config, ProbeDescription<?> probe) {
        this.application = application;
        this.config = config;
        this.probe = (ProbeDescription<Object>) probe;
    }

    @Override
    public String name() {
        return "Script";
    }

    /*=================================*
     * blueprint script initialization *
     *=================================*/

    private void initBlueprintScripts(PluginSetupService service) {
        var lookup = MethodHandles.publicLookup();

        if (config.debug) {
            for (var handle : BlueprintScriptHandles.lookupClass(lookup, DebugScript.class)) {
                initBlueprintScript(service, handle);
            }
        }

        for (var clazz : service.scanAnnotation(BlueprintScript.class, this::filterBlueprintScript)) {
            var coll = new ArrayList<>(BlueprintScriptHandles.lookupClass(lookup, clazz));
            coll.sort(Comparator.comparing(BlueprintScriptCallable::name));
            for (var callable : coll) {
                initBlueprintScript(service, callable);
            }
        }
    }

    private boolean filterBlueprintScript(ClassInfo info) {
        if (info.isInnerClass()) return false;

        var ann = info.getAnnotationInfo(BlueprintScript.class);
        String name;
        if (ann == null) {
            name = info.getSimpleName();
        } else {
            name = (String) ann.getParameterValues().getValue("value");
            if (name.isEmpty()) {
                name = info.getSimpleName();
            }
        }
        log.debug("find \"{}\" = {}", name, info.getName());

        var checkAnn = info.getAnnotationInfo(RequestChannelmap.class);
        if (checkAnn == null) return true;
        RequestChannelmapInfo request;
        try {
            request = RequestChannelmapInfo.of(checkAnn);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return false;
        }

        if (!request.checkProbe(probe)) {
            log.debug("reject probe {}", request.probe().getName());
            return false;
        }
        return true;
    }

    private void initBlueprintScript(PluginSetupService service, BlueprintScriptCallable callable) {
        for (var plugin : callable.requestPlugins()) {
            if (service.getPlugin(plugin) == null) {
                log.info("reject {}, because plugin {} is not load.", callable.name(), plugin.getSimpleName());
                return;
            }
        }

        if (callable instanceof BlueprintScriptMethodHandle handle) {
            log.debug("init {} = {}.{}()", handle.name, handle.declaredClass.getSimpleName(), handle.declaredMethod.getName());
        } else if (callable instanceof BlueprintScriptClassHandle handle) {
            log.debug("init {} = {}.{}", handle.name, handle.declaredClass.getSimpleName(), handle.declaredInner.getSimpleName());
        } else {
            log.debug("init {}", callable.name());
        }

        functions.add(callable);
    }

    /*============*
     * properties *
     *============*/

    /*===========*
     * UI layout *
     *===========*/

    private ChoiceBox<String> script;
    private TextField line;
    private Button run;
    private Label information;
    private Label document;
    private final Map<String, String> cached = new HashMap<>();

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");

        service.fork("ScriptPlugin.initBlueprintScripts", s -> {
            log.debug("initBlueprintScripts");
            initBlueprintScripts(s);
            log.debug("updateScriptChoice");
            Platform.runLater(this::updateScriptChoice);
        });

        return super.setup(service);
    }

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {

        script = new ChoiceBox<>();
        script.setMaxWidth(100);
        script.valueProperty().addListener((_, old, value) -> onScriptSelection(old, value));

        line = new TextField();
        line.setFont(Font.font("monospace"));
        line.setOnAction(this::onScriptRun);
        line.addEventFilter(KeyEvent.KEY_PRESSED, this::onInputKeyPressed);

        run = new Button("Run");
        run.setOnAction(this::onScriptRun);

        var reset = new Button("Reset");
        reset.setOnAction(this::onScriptReset);

        var left = new Label("(");
        var right = new Label(")");
        var font = Font.font("monospace", FontWeight.BOLD, 14);
        left.setFont(font);
        right.setFont(font);

        var head = new HBox(script, left, line, right, run, reset);
        head.setSpacing(10);
        HBox.setHgrow(line, Priority.ALWAYS);

        information = new Label("");
        information.setFont(Font.font("monospace", FontWeight.BOLD, 12));
        information.setWrapText(true);

        document = new Label("");
        document.setFont(Font.font("monospace"));
        document.setWrapText(true);
        document.setVisible(false);

        information.setOnMouseEntered(_ -> {
            document.setVisible(true);
            document.setManaged(true);
        });
        information.setOnMouseExited(_ -> {
            document.setVisible(false);
            document.setManaged(false);
        });

        var root = new VBox(head, information, document);
        root.setSpacing(5);

        return root;
    }

    @Override
    protected void setupChartContent(PluginSetupService service, ProbeView<?> canvas) {
        view = (ProbeView<Object>) canvas;
    }

    /*==============*
     * event handle *
     *==============*/

    private void onScriptSelection(String old, String name) {
        var oldInput = line.getText();
        if (old != null && oldInput != null && !oldInput.isEmpty()) {
            cached.put(old, oldInput);
        }

        updateRunButtonState();

        BlueprintScriptCallable script;
        if (name == null || name.isEmpty() || (script = getScript(name)) == null) {
            line.setText("");
            information.setText("");
            document.setText("");
            return;
        }

        log.debug("select script : {}", name);

        var newInput = cached.get(name);
        if (newInput != null && !newInput.isEmpty()) {
            line.setText(newInput);
        } else if (oldInput != null && !oldInput.isEmpty()) {
            line.setText(oldInput);
        }

        information.setText(BlueprintScriptHandles.getScriptSignature(script));
        document.setText(BlueprintScriptHandles.buildScriptDocument(script));
    }

    private void onInputKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.UP) {
            onInputKeySelect(-1);
            e.consume();
        } else if (e.getCode() == KeyCode.DOWN) {
            onInputKeySelect(1);
            e.consume();
        }
    }

    private void onInputKeySelect(int d) {
        var value = script.getValue();
        var index = script.getItems().indexOf(value);
        try {
            var next = script.getItems().get(index + d);
            script.setValue(next);
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private void onScriptRun(ActionEvent e) {
        var name = script.getValue();
        if (name == null || name.isEmpty()) return;
        var script = getScript(name);
        if (script == null) return;

        if (isRunning(name)) {
            interruptScript(name);
        } else {
            var input = line.getText();
            if (input == null) input = "";

            try {
                evalScript(script, input);
            } catch (Exception ex) {
                onScriptFail(script, ex);
            }
        }
    }

    private void onScriptFail(BlueprintScriptCallable callable, Throwable error) {
        if (error instanceof InterruptedException) {
            LogMessageService.printMessage("script \"" + callable.name() + "\" interrupted");
        } else {
            LogMessageService.printMessage(List.of(
                "script \"" + callable.name() + "\" failed with",
                error.getMessage()
            ));
            log.warn("evalScript", error);
        }
    }

    private void onScriptReset(ActionEvent e) {
        log.debug("reset blueprint");
        var tool = BlueprintAppToolkit.newToolkit();
        tool.clear();
        tool.apply(view.getBlueprint());
    }

    /*=================*
     * script controls *
     *=================*/

    public boolean selectScript(String name) {
        if (getScript(name) != null) {
            script.setValue(name);
            return true;
        } else {
            return false;
        }
    }

    public String getScriptInputLine() {
        return line.getText();
    }

    public void setScriptInputLine(String line) {
        this.line.setText(line);
    }

    public void appendScriptInputText(String text) {
        line.appendText(text);
    }

    public void appendScriptInputValueText(String text) {
        if (!line.getText().isEmpty()) line.appendText(", ");
        line.appendText(text);
    }

    public void appendScriptInputValueText(String name, String value) {
        if (name.contains("=")) throw new IllegalArgumentException();
        if (name.contains(",")) throw new IllegalArgumentException();
        appendScriptInputValueText(name + "=" + value);
    }

    public void runScript() {
        var name = script.getValue();
        if (name == null || name.isEmpty()) throw new RuntimeException();

        var input = line.getText();
        if (input == null) input = "";

        runScript(name, input);
    }

    public boolean showAndRunScript(String name, String line) {
        if (!selectScript(name)) return false;
        setScriptInputLine(line);
        runScript(name, line);
        return true;
    }

    public void runScript(String name, String line) {
        var script = getScript(name);
        if (script == null) throw new RuntimeException("script \"" + name + "\" not found");

        if (isRunning(name)) {
            throw new RuntimeException("script \"" + name + "\" is running");
        }

        try {
            evalScript(script, line);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                throw new RuntimeException("script \"" + name + "\" is interrupted", ex);
            } else {
                throw new RuntimeException("script \"" + name + "\" fail. " + ex.getMessage(), ex);
            }
        }
    }

    public void runScript(String name, List<String> args, Map<String, String> kwargs) {
        var script = getScript(name);
        if (script == null) throw new RuntimeException("script \"" + name + "\" not found");

        if (isRunning(name)) {
            throw new RuntimeException("script \"" + name + "\" is running");
        }

        try {
            invokeScript(script, BlueprintScriptHandles.parseScriptInputArgs(args, kwargs));
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                throw new RuntimeException("script \"" + name + "\" is interrupted", ex);
            } else {
                throw new RuntimeException("script \"" + name + "\" fail. " + ex.getMessage(), ex);
            }
        }
    }

    private void updateScriptChoice() {
        var current = script.getValue();
        var items = script.getItems();
        items.clear();
        items.addAll(functions.stream().map(BlueprintScriptCallable::name).toList());
        if (current != null && !current.isEmpty() && getScript(current) != null) {
            script.setValue(current);
        } else if (config.debug && getScript("echo") != null) {
            script.setValue("echo");
        }
    }

    private void updateRunButtonState() {
        var name = script.getValue();
        if (isRunning(name)) {
            run.setText("Interrupt");
            run.setTextFill(Color.RED);
        } else {
            run.setText("Run");
            run.setTextFill(Color.BLACK);
        }
    }

    /*=================*
     * script invoking *
     *=================*/

    public boolean hasScript(String name) {
        return getScript(name) != null;
    }

    private @Nullable BlueprintScriptCallable getScript(String name) {
        for (var function : functions) {
            if (function.name().equals(name)) return function;
        }
        return null;
    }

    private void evalScript(BlueprintScriptCallable callable, String line) {
        log.debug("run script {} with \"{}\"", callable.name(), line);
        invokeScript(callable, BlueprintScriptHandles.parseScriptInputLine(line));
    }

    private void invokeScript(BlueprintScriptCallable callable, List<PyValue.PyParameter> arguments) {
        log.debug("run script {} with {}", callable.name(), arguments);
        invokeScript(callable, BlueprintScriptHandles.pairScriptArguments(callable, arguments));
    }

    private void invokeScript(BlueprintScriptCallable callable, Object[] arguments) {
        var request = callable.requestChannelmap();
        if (request != null) {
            log.debug("check {} {}", callable.name(), request);
            if (!checkScriptRequest(request)) {
                return;
            }
        }

        if (log.isDebugEnabled()) {
            var parameters = callable.parameters();
            var message = IntStream.range(0, arguments.length).mapToObj(i -> {
                var p = i < parameters.length ? parameters[i] : null;
                var a = Objects.toString(arguments[i]);
                if (p == null) {
                    return a;
                } else {
                    return p.name() + "=" + a;
                }
            }).collect(Collectors.joining(", "));
            log.debug("invoke script {} with [{}]", callable.name(), message);
        }

        if (callable.isAsync()) {
            invokeScriptAsync(callable, arguments, this::onScriptThreadComplete);
        } else {
            invokeScriptSync(callable, arguments);
        }
    }

    private boolean checkScriptRequest(RequestChannelmapInfo request) {
        if (!request.probe().isInstance(probe)) {
            LogMessageService.printMessage("probe mis-matched.");
            return false;
        }

        var code = request.code();
        var channelmap = view.getChannelmap();
        if (channelmap == null) {
            if (code != null && request.create()) {
                log.debug("create probe {}", request);
                application.clearProbe(code);
                return true;
            } else {
                return false;
            }

        } else if (code != null && !Objects.equals(probe.channelmapCode(channelmap), code)) {
            LogMessageService.printMessage("probe mis-matched.");
            return false;
        }
        return true;
    }

    private void invokeScriptSync(BlueprintScriptCallable callable, Object[] arguments) {
        try {
            var toolkit = BlueprintAppToolkit.newToolkit();
            callable.invoke(toolkit, arguments);
            return;
        } catch (RequestChannelmapException e) {
            log.debug("fail. check {} {}", callable.name(), e.request);
            if (!checkScriptRequest(e.request)) {
                throw e;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        try {
            log.debug("reinvoke {}", callable.name());
            var toolkit = BlueprintAppToolkit.newToolkit();
            callable.invoke(toolkit, arguments);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /*=======================*
     * async script invoking *
     *=======================*/

    private static final class ScriptResult {
        private final Thread virtual;
        private final ScriptThread thread;
        private volatile boolean complete;
        private volatile @Nullable Throwable error;

        private ScriptResult(Thread virtual, ScriptThread thread) {
            this.virtual = virtual;
            this.thread = thread;
        }

        private String name() {
            return thread.callable.name();
        }

        private synchronized @Nullable Throwable complete(@Nullable Throwable error) {
            if (!complete) {
                complete = true;
                this.error = error;
            }
            notifyAll();
            return this.error;
        }
    }

    private static class WaitingResult {
        private final ScriptResult thread;

        WaitingResult(ScriptResult thread) {
            this.thread = thread;
        }

        private @Nullable Throwable waitComplete(long wait) throws InterruptedException {
            if (!thread.complete) {
                synchronized (thread) {
                    while (!thread.complete) {
                        thread.wait(wait);
                    }
                }
            }
            return thread.error;
        }
    }

    private final Deque<ScriptResult> running = new LinkedBlockingDeque<>();

    public boolean isRunning(String name) {
        for (var thread : running) {
            if (thread.name().equals(name)) return true;
        }
        return false;
    }

    private ScriptPlugin.@Nullable ScriptResult getScriptResult(Object thread) {
        for (var r : running) {
            if (r.virtual == thread || r.thread == thread) return r;
        }
        return null;
    }

    /**
     * Interrupt the script running thread.
     * <p>
     * If the script is not an async script, then it is uninterruptible,
     * and always return {@code false}.
     * <p>
     * Unexisted script {@code name} always returns {@code false}.
     *
     * @param name the name of the script.
     * @return Did interruption set?
     */
    public boolean interruptScript(String name) {
        for (var thread : running) {
            if (thread.name().equals(name)) {
                interruptScript(thread);
                return true;
            }
        }
        return false;
    }

    private void interruptScript(ScriptResult script) {
        log.debug("interrupt script {}", script.name());
        script.virtual.interrupt();
    }

    public @Nullable Throwable waitScriptFinished(String name) throws InterruptedException {
        return waitScriptFinished(name, 0L);
    }

    public @Nullable Throwable waitScriptFinished(String name, long wait) throws InterruptedException {
        if (Platform.isFxApplicationThread()) throw new IllegalStateException("run this method in FX application thread");

        for (var thread : running) {
            if (thread.name().equals(name)) {
                return waitScriptFinished(thread, wait);
            }
        }

        throw new RuntimeException("nothing to wait");
    }

    private @Nullable Throwable waitScriptFinished(ScriptResult script, long wait) throws InterruptedException {
        assert !Platform.isFxApplicationThread();
        var current = ScriptThread.current();
        if (current == null) {
            log.debug("wait script {}", script.name());
        } else {
            log.debug("{} wait script {}", current.name(), script.name());
        }
        var w = new WaitingResult(script);
        return w.waitComplete(wait);
    }

    private void invokeScriptAsync(BlueprintScriptCallable callable, Object[] arguments, BiConsumer<ScriptThread, @Nullable Throwable> complete) {
        var thread = new ScriptThread(BlueprintAppToolkit.newToolkit(), callable, arguments, complete);
        var ret = Thread.ofVirtual().name(thread.callable.name()).unstarted(thread);
        ret.setUncaughtExceptionHandler(this::onScriptUncaughtException);
        running.add(new ScriptResult(ret, thread));
        ret.start();
        updateRunButtonState();
    }

    private void onScriptUncaughtException(Thread t, Throwable e) {
        var result = getScriptResult(t);
        if (result == null) {
            log.warn("uncaught error {}: {}", t.getName(), e.toString());
        } else {
            log.debug("{} set complete with an uncaught error {}", result.name(), e.toString());
            result.complete(e);
            interruptScript(result);
        }
    }

    /**
     * mark {@code thread} completed. It will be removed from running queue. The error state will be settled.
     *
     * @param thread script thread.
     * @param error  the received error.
     * @return The primary error
     */
    private @Nullable Throwable notifyScriptThreadComplete(ScriptThread thread, @Nullable Throwable error) {
        assert Platform.isFxApplicationThread();

        var result = getScriptResult(thread);
        if (result != null) {
            this.running.remove(result);
            error = result.complete(error);
            log.debug("{} complete with {}", thread.name(), Objects.toString(error));
        }

        updateRunButtonState();
        return error;
    }

    private void onScriptThreadComplete(ScriptThread thread, @Nullable Throwable error) {
        assert Platform.isFxApplicationThread();

        error = notifyScriptThreadComplete(thread, error);

        var callable = thread.callable;
        if (error instanceof RequestChannelmapException e) {
            log.debug("fail. check {} {}", callable.name(), e.request);
            if (!checkScriptRequest(e.request)) {
                onScriptFail(callable, error);
            } else {
                invokeScriptAsync(callable, thread.arguments, this::onScriptThreadCompleteFinal);
            }
        } else if (error != null) {
            onScriptFail(callable, error);
        }
    }

    private void onScriptThreadCompleteFinal(ScriptThread thread, @Nullable Throwable error) {
        assert Platform.isFxApplicationThread();

        error = notifyScriptThreadComplete(thread, error);

        var callable = thread.callable;
        if (error != null) {
            onScriptFail(callable, error);
        }
    }
}
