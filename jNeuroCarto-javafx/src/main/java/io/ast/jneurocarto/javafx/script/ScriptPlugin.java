package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
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
import io.ast.jneurocarto.javafx.app.*;
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

        if (config.debug) {
            for (var handle : BlueprintScriptHandles.lookupClass(lookup, DebugScript.class)) {
                initBlueprintScript(handle);
            }
        }

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

        try {
            evalScript(script, input);
        } catch (Exception ex) {
            LogMessageService.printMessage(ex.getMessage());
            log.warn("evalScript", ex);
        }
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
        } else if (config.debug && getScript("echo") != null) {
            script.setValue("echo");
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
          .map(BlueprintScriptCallable.Parameter::name)
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

        var arguments = pairScriptArguments(callable, token);

        try {
            callable.invoke(newToolkit(), arguments);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    private Object[] pairScriptArguments(BlueprintScriptCallable callable, Tokenize tokens) {
        assert tokens.tokens != null;
        assert tokens.values != null;

        var parameters = callable.parameters();
        var ret = new ArrayList<Object>(parameters.length);
        var defv = new Object();
        for (int i = 0, length = parameters.length; i < length; i++) {
            ret.add(defv);
        }

        for (int i = 0, size = tokens.size(); i < size; i++) {
            var token = tokens.tokens.get(i);
            var argument = tokens.values.get(i);

            if (argument instanceof PyValue.PyIndexParameter(var index, var value)) {
                if (index >= parameters.length) {
                    var last = parameters[parameters.length - 1];
                    if (last.isVarArg()) {
                        ret.add(castScriptArgument(last, token, value));
                    } else {
                        throw new RuntimeException("too many arguments given.");
                    }
                } else {
                    ret.set(index, castScriptArgument(parameters[index], token, value));
                }
            } else if (argument instanceof PyValue.PyNamedParameter(var name, var value)) {
                var j = indexOfParameter(parameters, name);
                if (j < 0) {
                    throw new RuntimeException("unresolved parameter name : " + name);
                }
                if (ret.get(j) != defv) {
                    throw new RuntimeException("duplicated parameter : " + name);
                }

                var last = parameters[j];
                if (last.isVarArg()) {
                    ret.add(castScriptArgument(last, token, value));
                } else {
                    ret.set(j, castScriptArgument(last, token, value));
                }

            } else {
                throw new RuntimeException();
            }
        }

        for (int i = 0, length = ret.size(); i < length; i++) {
            if (ret.get(i) == defv) {
                var parameter = parameters[Math.min(i, parameters.length - 1)];
                if (parameter.defaultValue() == null) {
                    ret.set(i, null);
                } else {
                    ret.set(i, castScriptArgument(parameter, (String) null, new Tokenize(parameter.defaultValue()).parseValue()));
                }
            }
        }

        return ret.toArray(Object[]::new);
    }

    private int indexOfParameter(BlueprintScriptCallable.Parameter[] parameters, String name) {
        for (int i = 0, length = parameters.length; i < length; i++) {
            if (parameters[i].name().equals(name)) return i;
        }
        return -1;
    }

    private @Nullable Object castScriptArgument(BlueprintScriptCallable.Parameter parameter,
                                                @Nullable String rawString,
                                                PyValue value) {
        var converter = parameter.converter();
        if (converter == ScriptParameter.RawString.class) {
            return rawString;
        } else if (converter != ScriptParameter.AutoCasting.class) {
            return castScriptArgument(parameter, converter, value);
        }

        var target = parameter.type();
        if (target == int.class || target == Integer.class) {
            return switch (value) {
                case PyValue.PyInt(var ret) -> ret;
                case PyValue.PyNone _ when target == Integer.class -> null;
                case null, default -> throwCCE(rawString, "int");
            };
        } else if (target == double.class || target == Double.class) {
            return switch (value) {
                case PyValue.PyInt(var ret) -> (double) ret;
                case PyValue.PyFloat(double ret) -> ret;
                case PyValue.PyNone _ when target == Double.class -> null;
                case null, default -> throwCCE(rawString, "double");
            };
        } else if (target == int[].class) {
            return switch (value) {
                case PyValue.PyList list -> list.toIntArray();
                case PyValue.PyTuple tuple -> tuple.toIntArray();
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(rawString, "int[]");
            };
        } else if (target == double[].class) {
            return switch (value) {
                case PyValue.PyList list -> list.toDoubleArray();
                case PyValue.PyTuple tuple -> tuple.toDoubleArray();
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(rawString, "double[]");
            };
        } else if (target == String.class) {
            return switch (value) {
                case PyValue.PyInt _, PyValue.PyFloat _ -> rawString;
                case PyValue.PyStr(String ret) -> ret;
                case PyValue.PySymbol(String ret) -> ret;
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(rawString, "String");
            };
        } else if (target.isEnum()) {
            return switch (value) {
                case PyValue.PyInt(int ret) -> target.getEnumConstants()[ret];
                case PyValue.PyStr(String ret) -> Enum.valueOf((Class<Enum>) target, ret);
                case PyValue.PySymbol(String ret) -> Enum.valueOf((Class<Enum>) target, ret);
                case null, default -> throwCCE(rawString, target.getSimpleName());
            };
        } else if (target == PyValue.class) {
            return value;
        } else if (PyValue.class.isAssignableFrom(value.getClass())) {
            throwCCE(rawString, target.getSimpleName() + ", use PyValue or java primitive type instead");
        }

        return throwCCE(rawString, target.getSimpleName());
    }

    private static Object throwCCE(@Nullable String rawString, String target) {
        throw new ClassCastException("cannot cast '" + rawString + "' to " + target + ".");
    }

    private Object castScriptArgument(BlueprintScriptCallable.Parameter parameter,
                                      Class<? extends Function<PyValue, ?>> converter,
                                      PyValue value) {
        Function<PyValue, ?> function;
        try {
            function = converter.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("create converter for parameter " + parameter.name(), e);
        }
        return function.apply(value);
    }
}
