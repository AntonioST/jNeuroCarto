package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.Application;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.view.GlobalStateView;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.ClassInfo;

public class ScriptPlugin extends InvisibleView implements GlobalStateView<ScriptConfig> {

    private final CartoConfig config;
    private final ProbeDescription<Object> probe;
    private final List<BlueprintScriptCallable> functions = new ArrayList<>();
    private Application<Object> application;
    private ProbeView<Object> view;

    private final Logger log = LoggerFactory.getLogger(ScriptPlugin.class);


    public ScriptPlugin(CartoConfig config, ProbeDescription<?> probe) {
        this.config = config;
        this.probe = (ProbeDescription<Object>) probe;
    }

    @Override
    public String name() {
        return "script";
    }

    @Override
    public String description() {
        return "run blueprint script";
    }

    /*=================================*
     * blueprint script initialization *
     *=================================*/

    private void initBlueprintScripts(PluginSetupService service) {
        var lookup = MethodHandles.publicLookup();

        service = service.asProbePluginSetupService();
        for (var clazz : service.scanAnnotation(BlueprintScript.class, this::filterBlueprintScript)) {
            for (var handle : BlueprintScriptHandles.lookupClass(lookup, clazz)) {
                initBlueprintScript(handle);
            }
        }
    }

    private boolean filterBlueprintScript(ClassInfo info) {
        var ann = info.getAnnotationInfo(BlueprintScript.class);
        String name;
        if (ann == null) {
            name = info.getSimpleName();
        } else {
            name = (String) ann.getParameterValues().getValue("value");
        }
        log.debug("filter \"{}\" = {}", name, info.getName());

        var checkAnn = info.getAnnotationInfo(CheckProbe.class);
        if (checkAnn == null) return true;

        var check = checkAnn.getParameterValues();
        var family = (String) check.getValue("value");

        var probeValue = check.getValue("probe");
        Class<? extends ProbeDescription> probe;
        if (probeValue instanceof AnnotationClassRef ref) {
            var refClass = ref.loadClass(true);
            if (refClass == null) {
                log.debug("unknown probe() class {}", ref.getName());
                return false;
            } else if (ProbeDescription.class.isAssignableFrom(refClass)) {
                probe = (Class<? extends ProbeDescription>) refClass;
            } else {
                log.debug("illegal probe() class {}", ref.getName());
                return false;
            }
        } else {
            log.debug("unknown probe() value {}", probeValue);
            return false;
        }

        if (probe == ProbeDescription.class) {
            if (family.isEmpty()) return true;

            var ret = ProbeDescription.getProbeDescription(family);
            if (ret == null) {
                log.debug("unknown value(), probe {} not found.", family);
                return false;
            }
            probe = ret.getClass();
        }

        var ret = this.probe.getClass().isAssignableFrom(probe);
        if (!ret) {
            log.debug("reject probe {}", probe.getName());
            return false;
        }
        return ret;
    }

    private void initBlueprintScript(BlueprintScriptCallable callable) {
        if (callable instanceof BlueprintScriptHandle handle) {
            log.debug("init {} = {}.{}", handle.name, handle.declaredClass.getSimpleName(), handle.declaredMethod.getName());
        } else {
            log.debug("init {}", callable.name());
        }

        functions.add(callable);
    }




    /*============*
     * properties *
     *============*/

    /*=================*
     * state load/save *
     *=================*/

    @Override
    public @Nullable ScriptConfig getState() {
        return null;
    }

    @Override
    public void restoreState(@Nullable ScriptConfig state) {

    }

    /*===========*
     * UI layout *
     *===========*/

    private ChoiceBox<String> script;
    private TextField line;
    private Button run;
    private Map<String, String> cached = new HashMap<>();
    private Label information;
    private Label document;

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");
        application = (Application<Object>) Application.getInstance();
        view = (ProbeView<Object>) service.getProbeView();

        initBlueprintScripts(service);

        var ret = super.setup(service);
        updateScriptChoice();
        return ret;
    }

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {

        script = new ChoiceBox<>();
        script.valueProperty().addListener((_, old, value) -> onScriptSelection(old, value));

        line = new TextField();

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

    /*==============*
     * event handle *
     *==============*/

    private void onScriptSelection(String old, String name) {
        var oldInput = line.getText();
        if (old != null && oldInput != null && !oldInput.isEmpty()) {
            cached.put(old, oldInput);
        }

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

        information.setText(getScriptSignature(script));
        document.setText(buildScriptDocument(script));
    }

    private void onScriptRun(ActionEvent e) {
        var name = script.getValue();
        if (name == null || name.isEmpty()) return;
        var script = getScript(name);
        if (script == null) return;

        var input = line.getText();
        if (input == null) input = "";

        evalScript(script, input);
    }

    private void onScriptReset(ActionEvent e) {
        log.debug("reset blueprint");
        var tool = newToolkit();
        if (tool == null) return;

        tool.clear();
        tool.apply(view.getBlueprint());
    }

    private void updateScriptChoice() {
        var current = script.getValue();
        var items = script.getItems();
        items.clear();
        items.addAll(functions.stream().map(BlueprintScriptCallable::name).toList());
        if (current != null && !current.isEmpty() && getScript(current) != null) {
            script.setValue(current);
        }
    }

    /*=================*
     * script invoking *
     *=================*/

    private @Nullable BlueprintScriptCallable getScript(String name) {
        for (var function : functions) {
            if (function.name().equals(name)) return function;
        }
        return null;
    }

    private String getScriptSignature(BlueprintScriptCallable callable) {
        var name = callable.name();
        var para = Arrays.stream(callable.parameters())
          .map(BlueprintScriptCallable.ScriptParameter::name)
          .collect(Collectors.joining(", ", "(", ")"));
        return name + para;
    }

    private String buildScriptDocument(BlueprintScriptCallable callable) {
        var doc = callable.description();
        var para = Arrays.stream(callable.parameters())
          .map(it -> {
              var name = it.name();
              var type = it.typeDesp();
              var defv = it.defaultValue();
              if (defv != ScriptParameter.NO_DEFAULT) {
                  type = type + "=" + defv;
              }
              var desp = it.description();
              return name + " : (" + type + ") " + desp;
          }).collect(Collectors.joining("\n"));

        return doc + "\n" + para;
    }

    private @Nullable BlueprintAppToolkit<Object> newToolkit() {
        var chmap = view.getChannelmap();
        if (chmap == null) return null;
        var electrodes = view.getBlueprint();
        var blueprint = new Blueprint<>(probe, chmap, electrodes);
        return new BlueprintAppToolkit<>(application, blueprint);
    }

    private void evalScript(BlueprintScriptCallable callable, String line) {
        log.debug("run script {} with \"{}\"", callable.name(), line);
        var token = new Tokenize(line).parse();
        if (log.isDebugEnabled()) {
            var content = token.stream()
              .map(PyValue::toString)
              .collect(Collectors.joining(", "));
            log.debug("token {}", content);
        }
    }

}
