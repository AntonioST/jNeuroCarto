package io.ast.jneurocarto.javafx.script;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.view.Plugin;

@NullMarked
public final class BlueprintScriptHandles {

    private static final Logger log = LoggerFactory.getLogger(BlueprintScriptHandles.class);

    private BlueprintScriptHandles() {
        throw new RuntimeException();
    }

    /*========*
     * lookup *
     *========*/

    public static List<BlueprintScriptCallable> lookupClass(MethodHandles.Lookup lookup, Class<?> clazz) {
        if (BlueprintScriptProvider.class.isAssignableFrom(clazz)) {
            return lookupBlueprintScriptProvider(lookup, (Class<BlueprintScriptProvider>) clazz);
        } else {
            return lookupPainClass(lookup, clazz);
        }
    }

    private static List<BlueprintScriptCallable> lookupBlueprintScriptProvider(MethodHandles.Lookup lookup, Class<BlueprintScriptProvider> clazz) {
        log.debug("lookup provider  {}", clazz.getSimpleName());
        var modifiers = clazz.getModifiers();
        if ((modifiers & Modifier.INTERFACE) != 0) {
            log.warn("class {} is interface", clazz.getSimpleName());
            return List.of();
        }
        if ((modifiers & Modifier.ABSTRACT) != 0) {
            log.warn("class {} is abstract", clazz.getSimpleName());
            return List.of();
        }

        BlueprintScriptProvider instance;

        try {
            instance = clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("lookupBlueprintScriptProvider", e);
            return List.of();
        }

        return instance.getBlueprintScripts(lookup);
    }

    private static List<BlueprintScriptCallable> lookupPainClass(MethodHandles.Lookup lookup, Class<?> clazz) {
        log.debug("lookup class  {}", clazz.getSimpleName());

        Object instance;

        try {
            instance = clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.debug("class {} no no-arg constructor", clazz.getSimpleName());
            instance = null;
        }

        var ret = new ArrayList<BlueprintScriptCallable>();
        for (var method : clazz.getMethods()) {
            var ann = method.getDeclaredAnnotation(BlueprintScript.class);
            if (ann != null) {
                var h = lookupMethod(lookup, clazz, method, instance);
                if (h != null) {
                    ret.add(h);
                }
            }
        }

        for (var inner : clazz.getClasses()) {
            var ann = inner.getDeclaredAnnotation(BlueprintScript.class);
            if (ann != null) {
                var h = lookupInnerClass(lookup, clazz, inner);
                if (h != null) {
                    ret.add(h);
                }
            }
        }
        return ret;
    }

    /*===============*
     * lookup method *
     *===============*/

    private static @Nullable BlueprintScriptMethodHandle lookupMethod(MethodHandles.Lookup lookup, Class<?> clazz, Method method, @Nullable Object instance) {
        log.debug("lookup method {}.{}", clazz.getSimpleName(), method.getName());

        var modifiers = method.getModifiers();
        if ((modifiers & Modifier.PUBLIC) == 0) {
            log.warn("method {}.{} not public", clazz.getSimpleName(), method.getName());
            return null;
        }
        var is_static = (modifiers & Modifier.STATIC) != 0;
        if (!is_static && instance == null) {
            log.warn("method {}.{} not static", clazz.getSimpleName(), method.getName());
            return null;
        }
        var parameters = method.getParameters();
        if (parameters.length == 0) {
            log.warn("method {}.{} signature does not to (Blueprint, ...)", clazz.getSimpleName(), method.getName());
            return null;
        }

        Class<?> blueprint = checkMethodBlueprintParameter(parameters[0]);
        if (blueprint == null) {
            log.warn("method {}.{} signature does not to (Blueprint, ...)", clazz.getSimpleName(), method.getName());
            return null;
        }
        Class<? extends Plugin>[] plugins = checkMethodPluginParameter(parameters, 1);

        MethodHandle handle;

        try {
            handle = lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            log.warn("lookupMethod for method", e);
            return null;
        }

        var ann = method.getAnnotation(BlueprintScript.class);
        assert ann != null;

        var name = ann.value();
        if (name.isEmpty()) name = method.getName();

        var description = ann.description();

        var ps = lookupMethodParameter(method, parameters, 1 + plugins.length);

        if (instance != null && !is_static) {
            handle = handle.bindTo(instance);
        }

        return new BlueprintScriptMethodHandle(clazz, method, name, description, blueprint, plugins, ps, ann.async(), handle);
    }

    private static @Nullable Class<?> checkMethodBlueprintParameter(Parameter parameter) {
        var type = parameter.getType();
        if (type == Blueprint.class) return type;
        if (BlueprintToolkit.class.isAssignableFrom(type)) return type;
        return null;
    }

    private static Class<? extends Plugin>[] checkMethodPluginParameter(Parameter[] parameters, int start) {
        var ret = new ArrayList<Class<? extends Plugin>>();
        for (int i = start, length = parameters.length; i < length; i++) {
            var t = parameters[i].getType();
            if (Plugin.class.isAssignableFrom(t)) {
                ret.add((Class<? extends Plugin>) t);
            } else {
                break;
            }
        }
        return ret.toArray(new Class[0]);
    }

    private static BlueprintScriptCallable.Parameter[] lookupMethodParameter(Method method, Parameter[] parameters, int start) {
        var ret = new ArrayList<BlueprintScriptCallable.Parameter>();
        for (int i = start, length = parameters.length; i < length; i++) {
            var parameter = parameters[i];
            var ann = parameter.getAnnotation(ScriptParameter.class);
            log.trace("method {} find parameter {}", method.getName(), parameter.getName());

            Class<?> type = parameter.getType();
            var isVarArg = parameter.isVarArgs();
            if (isVarArg) type = type.getComponentType();

            if (ann == null) {
                var name = parameter.getName();
                var desp = type.getSimpleName();
                var converter = ScriptParameter.AutoCasting.class;

                ret.add(new BlueprintScriptCallable.Parameter(name, type, desp, null, null, converter, isVarArg));
            } else {
                ret.add(newParameter(type, ann, isVarArg));
            }
        }
        return ret.toArray(BlueprintScriptCallable.Parameter[]::new);
    }

    /*====================*
     * lookup inner class *
     *====================*/

    private static @Nullable BlueprintScriptClassHandle lookupInnerClass(MethodHandles.Lookup lookup, Class<?> clazz, Class<?> inner) {
        log.debug("lookup iclass {}.{}", clazz.getSimpleName(), inner.getSimpleName());

        var modifiers = inner.getModifiers();
        if ((modifiers & Modifier.PUBLIC) == 0) {
            log.warn("class {}.{} not public", clazz.getSimpleName(), inner.getSimpleName());
            return null;
        }
        if ((modifiers & Modifier.STATIC) == 0) {
            log.warn("class {}.{} not static", clazz.getSimpleName(), inner.getSimpleName());
            return null;
        }
        if (!Runnable.class.isAssignableFrom(inner)) {
            log.warn("class {}.{} not Runnable", clazz.getSimpleName(), inner.getSimpleName());
            return null;
        }
        Class<Runnable> runnable = (Class<Runnable>) inner;


        var constructors = inner.getConstructors();
        if (constructors.length == 0) {
            log.warn("class {}.{} has no constructor", clazz.getSimpleName(), inner.getName());
            return null;
        } else if (constructors.length > 1) {
            log.warn("class {}.{} has multiple constructor", clazz.getSimpleName(), inner.getName());
            return null;
        }

        var parameters = constructors[0].getParameters();
        @Nullable Class<?> blueprint = parameters.length > 0 ? checkMethodBlueprintParameter(parameters[0]) : null;
        var pc = blueprint == null ? 0 : 1;
        Class<? extends Plugin>[] plugins = checkMethodPluginParameter(parameters, pc);
        pc += plugins.length;
        if (pc != parameters.length) {
            var p = parameters[pc];
            log.warn("class {}.{}'s constructor has unsupported parameter {}", clazz.getSimpleName(), inner.getName(), p.getType().getSimpleName());
            return null;
        }

        MethodHandle constructor;
        try {
            constructor = lookup.unreflectConstructor(constructors[0]);
        } catch (IllegalAccessException e) {
            log.warn("lookupInnerClass for constructor", e);
            return null;
        }

        var ann = inner.getAnnotation(BlueprintScript.class);
        assert ann != null;

        var name = ann.value();
        if (name.isEmpty()) name = inner.getName();

        var description = ann.description();

        List<FoundParameter> fs;
        try {
            fs = lookupClassParameter(inner);
        } catch (Exception e) {
            log.warn("lookupInnerClass for parameters", e);
            return null;
        }

        var ps = new BlueprintScriptCallable.Parameter[fs.size()];
        var handles = new MethodHandle[fs.size()];

        for (int i = 0, size = fs.size(); i < size; i++) {
            var found = fs.get(i);
            ps[i] = found.parameter;

            if (found.member instanceof Field field) {
                try {
                    handles[i] = lookup.unreflectSetter(field);
                } catch (IllegalAccessException e) {
                    log.warn("lookupInnerClass for field", e);
                    return null;
                }
            } else if (found.member instanceof Method method) {
                try {
                    handles[i] = lookup.unreflect(method);
                } catch (IllegalAccessException e) {
                    log.warn("lookupInnerClass for method", e);
                    return null;
                }
            }
        }

        return new BlueprintScriptClassHandle(clazz, runnable, name, description, blueprint, plugins, ps, ann.async(), constructor, handles);
    }

    private record FoundParameter(Member member, ScriptParameter ann, BlueprintScriptCallable.Parameter parameter) {
        FoundParameter {
            if (!(member instanceof Field || member instanceof Method)) {
                throw new RuntimeException();
            }
        }

        int index() {
            return ann().index();
        }
    }

    private static List<FoundParameter> lookupClassParameter(Class<?> inner) {
        var unindexed = new ArrayList<FoundParameter>();
        var indexed = new ArrayList<FoundParameter>();

        for (var field : inner.getFields()) {
            var ann = field.getAnnotation(ScriptParameter.class);
            if (ann != null) {
                log.trace("class {} find field {}", inner.getSimpleName(), field.getName());
                var found = new FoundParameter(field, ann, newParameter(field.getType(), ann, false));
                (found.index() < 0 ? unindexed : indexed).add(found);
            }
        }
        for (var method : inner.getMethods()) {
            var ann = method.getAnnotation(ScriptParameter.class);
            if (ann != null) {
                log.trace("class {} find method {}()", inner.getSimpleName(), method.getName());
                if (method.getParameterCount() == 1) {
                    var para = method.getParameters()[0];
                    var found = new FoundParameter(method, ann, newParameter(para.getType(), ann, para.isVarArgs()));
                    (found.index() < 0 ? unindexed : indexed).add(found);
                } else {
                    throw new RuntimeException("not a one-parameter method : " + inner.getSimpleName() + "." + method.getName() + "()");
                }
            }
        }

        indexed.sort(Comparator.comparingInt(FoundParameter::index));
        for (var para : indexed) {
            unindexed.add(para.index(), para);
        }

        if (log.isTraceEnabled()) {
            log.trace("resolved parameter order {}",
                unindexed.stream().map(it -> it.parameter.name()).toList()
            );
        }

        return unindexed;
    }

    private static BlueprintScriptCallable.Parameter newParameter(Class<?> type, ScriptParameter ann, boolean isVarArg) {
        var name = ann.value();

        var typeDesp = ann.label();
        if (typeDesp.isEmpty()) {
            if (type.isArray()) {
                var t = type.getComponentType();
                typeDesp = "list[" + t.getSimpleName() + "]";
            } else {
                typeDesp = type.getSimpleName();
            }
        }

        var defv = ann.defaultValue();
        if (ScriptParameter.NO_DEFAULT.equals(defv)) defv = null;

        var desp = ann.description();
        var converter = ann.converter();

        return new BlueprintScriptCallable.Parameter(name, type, typeDesp, defv, desp, converter, isVarArg);
    }

    /*======*
     * text *
     *======*/

    public static String getScriptSignature(BlueprintScriptCallable callable) {
        var name = callable.name();
        var para = Arrays.stream(callable.parameters())
            .map(BlueprintScriptCallable.Parameter::name)
            .collect(Collectors.joining(", ", "(", ")"));
        return name + para;
    }

    public static String buildScriptDocument(BlueprintScriptCallable callable) {
        var doc = callable.description();
        var para = Arrays.stream(callable.parameters())
            .map(it -> {
                var name = it.name();
                var type = it.typeDesp();
                var defv = it.defaultValue();
                if (defv != null) {
                    type = type + "=" + defv;
                }
                var desp = it.description();
                return name + " : (" + type + ") " + desp;
            }).collect(Collectors.joining("\n"));

        return doc + "\n" + para;
    }

    /*=================*
     * parse arguments *
     *=================*/

    public static List<PyValue.PyParameter> parseScriptInputLine(String line) {
        return Objects.requireNonNull(new Tokenize(line).parse().values);
    }

    public static List<PyValue.PyParameter> parseScriptInputArgs(List<String> args, Map<String, String> kwargs) {
        var ret = new ArrayList<PyValue.PyParameter>();
        for (int i = 0, size = args.size(); i < size; i++) {
            var text = args.get(i);
            var value = new Tokenize(text).parseValue();
            ret.add(new PyValue.PyIndexParameter(i, text, -1, value));
        }
        for (var entry : kwargs.entrySet()) {
            var name = entry.getKey();
            var text = entry.getValue();
            var value = new Tokenize(text).parseValue();
            ret.add(new PyValue.PyNamedParameter(name, text, -1, value));
        }
        return ret;
    }

    public static Object[] pairScriptArguments(BlueprintScriptCallable callable, List<PyValue.PyParameter> arguments) {
        return pairScriptArguments(callable.parameters(), arguments);
    }

    public static Object[] pairScriptArguments(BlueprintScriptCallable.Parameter[] parameters, List<PyValue.PyParameter> arguments) {
        var ret = new ArrayList<>(parameters.length);
        var defv = new Object();
        for (int i = 0, length = parameters.length; i < length; i++) {
            ret.add(defv);
        }

        for (var argument : arguments) {
            switch (argument) {
            case PyValue.PyIndexParameter(var index, _, _, _) -> {
                if (index >= parameters.length) {
                    var last = parameters[parameters.length - 1];
                    if (last.isVarArg()) {
                        ret.add(castScriptArgument(last, argument));
                    } else {
                        throw new RuntimeException("too many arguments given.");
                    }
                } else {
                    ret.set(index, castScriptArgument(parameters[index], argument));
                }
            }
            case PyValue.PyNamedParameter(var name, _, _, _) -> {
                var j = indexOfParameter(parameters, name);
                if (j < 0) {
                    throw new RuntimeException("unresolved parameter name : " + name);
                }
                if (ret.get(j) != defv) {
                    throw new RuntimeException("duplicated parameter : " + name);
                }

                var last = parameters[j];
                if (last.isVarArg()) {
                    ret.add(castScriptArgument(last, argument));
                } else {
                    ret.set(j, castScriptArgument(last, argument));
                }
            }
            }
        }

        for (int i = 0, length = ret.size(); i < length; i++) {
            if (ret.get(i) == defv) {
                var parameter = parameters[Math.min(i, parameters.length - 1)];
                var text = parameter.defaultValue();
                if (text == null) {
                    throw new RuntimeException("parameter " + parameter.name() + " is required");
                } else {
                    var value = new Tokenize(text).parseValue();
                    ret.set(i, castScriptArgument(parameter, new PyValue.PyIndexParameter(i, text, 0, value)));
                }
            }
        }

        return ret.toArray(Object[]::new);
    }


    private static int indexOfParameter(BlueprintScriptCallable.Parameter[] parameters, String name) {
        for (int i = 0, length = parameters.length; i < length; i++) {
            if (parameters[i].name().equals(name)) return i;
        }
        return -1;
    }

    public static @Nullable Object castScriptArgument(BlueprintScriptCallable.Parameter parameter,
                                                      PyValue.PyParameter token) {
        var value = token.value();

        var converter = parameter.converter();
        if (converter == ScriptParameter.RawString.class) {
            return token.text();
        } else if (converter != ScriptParameter.AutoCasting.class) {
            return castScriptArgument(parameter, converter, value);
        }

        var target = parameter.type();
        if (target == boolean.class || target == Boolean.class) {
            return switch (value) {
                case PyValue.PyBool(var ret) -> ret;
                case PyValue.PyInt ret -> ret.asBool().value();
                case PyValue.PyDict ret -> ret.asBool().value();
                case PyValue.PyIterable ret -> ret.asBool().value();
                case PyValue.PyStr ret -> ret.asBool().value();
                case PyValue.PyNone _ -> false;
                case null, default -> throwCCE(parameter, token, "bool");
            };
        } else if (target == int.class || target == Integer.class) {
            return switch (value) {
                case PyValue.PyInt(var ret) -> ret;
                case PyValue.PyNone _ when target == Integer.class -> null;
                case null, default -> throwCCE(parameter, token, "int");
            };
        } else if (target == double.class || target == Double.class) {
            return switch (value) {
                case PyValue.PyInt(var ret) -> (double) ret;
                case PyValue.PyFloat(double ret) -> ret;
                case PyValue.PyNone _ when target == Double.class -> null;
                case null, default -> throwCCE(parameter, token, "double");
            };
        } else if (target == int[].class) {
            return switch (value) {
                case PyValue.PyList list -> list.toIntArray();
                case PyValue.PyTuple tuple -> tuple.toIntArray();
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(parameter, token, "int[]");
            };
        } else if (target == double[].class) {
            return switch (value) {
                case PyValue.PyList list -> list.toDoubleArray();
                case PyValue.PyTuple tuple -> tuple.toDoubleArray();
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(parameter, token, "double[]");
            };
        } else if (target == String.class) {
            return switch (value) {
                case PyValue.PyInt _, PyValue.PyFloat _ -> token.text();
                case PyValue.PyStr(String ret) -> ret;
                case PyValue.PyToken(String ret) -> ret;
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(parameter, token, "String");
            };
        } else if (target == File.class) {
            return switch (value) {
                case PyValue.PyStr(String ret) -> new File(ret);
                case PyValue.PyToken(String ret) -> new File(ret);
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(parameter, token, "File");
            };
        } else if (target == Path.class) {
            return switch (value) {
                case PyValue.PyStr(String ret) -> Path.of(ret);
                case PyValue.PyToken(String ret) -> Path.of(ret);
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(parameter, token, "Path");
            };
        } else if (target.isEnum()) {
            return switch (value) {
                case PyValue.PyInt(int ret) -> target.getEnumConstants()[ret];
                case PyValue.PyStr(String ret) -> castScriptArgumentForEnum((Class<Enum>) target, ret);
                case PyValue.PyToken(String ret) -> castScriptArgumentForEnum((Class<Enum>) target, ret);
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(parameter, token, target.getSimpleName());
            };
        } else if (target == PyValue.class) {
            return value;
        } else if (PyValue.class.isAssignableFrom(value.getClass())) {
            throwCCE(parameter, token, target.getSimpleName() + ", use PyValue or java primitive type instead");
        }

        return throwCCE(parameter, token, target.getSimpleName());
    }

    public static <E extends Enum<E>> E castScriptArgumentForEnum(Class<E> target, String value) {
        try {
            return Enum.valueOf(target, value);
        } catch (IllegalArgumentException e) {
            var start = value.toLowerCase();
            for (var c : target.getEnumConstants()) {
                if (c.name().toLowerCase().startsWith(start)) {
                    return c;
                }
            }

            throw new IllegalArgumentException("mismatch " + target.getSimpleName() + " constant: " + value, e);
        }
    }

    public static Object castScriptArgument(BlueprintScriptCallable.Parameter parameter,
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

    private static Object throwCCE(BlueprintScriptCallable.Parameter parameter, PyValue.PyParameter token, String target) {
        var raw = token.text();
        throw new ClassCastException(parameter.name() + " need " + target + " but '" + raw + "'.");
    }
}
