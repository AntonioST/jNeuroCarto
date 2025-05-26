package io.ast.jneurocarto.javafx.script;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;

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
            log.warn("lookupPainClass", e);
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

        Class<?> blueprint = checkMethodParameter(method.getParameters()[0]);
        if (blueprint == null) {
            log.warn("method {}.{} signature does not to (Blueprint, ...)", clazz.getSimpleName(), method.getName());
            return null;
        }

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

        var ps = lookupParameter(method).toArray(BlueprintScriptCallable.Parameter[]::new);

        if (instance != null && !is_static) {
            handle = handle.bindTo(instance);
        }

        return new BlueprintScriptMethodHandle(clazz, method, name, description, blueprint, ps, handle);
    }

    private static @Nullable Class<?> checkMethodParameter(Parameter parameter) {
        var type = parameter.getType();
        if (type == Blueprint.class) return type;
        if (BlueprintToolkit.class.isAssignableFrom(type)) return type;
        return null;
    }

    private static List<BlueprintScriptCallable.Parameter> lookupParameter(Method method) {
        var ret = new ArrayList<BlueprintScriptCallable.Parameter>();
        Parameter[] parameters = method.getParameters();
        for (int i = 1, length = parameters.length; i < length; i++) {
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
        return ret;
    }

    /*====================*
     * lookup inner class *
     *====================*/

    private static @Nullable BlueprintScriptClassHandle lookupInnerClass(MethodHandles.Lookup lookup, Class<?> clazz, Class<?> inner) {
        log.debug("lookup inner class {}.{}", clazz.getSimpleName(), inner.getSimpleName());

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

        Class<?> blueprint;
        MethodHandle constructor;
        try {
            blueprint = checkInnerConstructor(inner);

            Constructor<?> ctor;
            if (blueprint == null) {
                log.trace("class {} use ctor()", inner.getSimpleName());
                ctor = inner.getConstructor();
            } else {
                log.trace("class {} use ctor({})", inner.getSimpleName(), blueprint.getSimpleName());
                ctor = inner.getConstructor(blueprint);
            }
            constructor = lookup.unreflectConstructor(ctor);
        } catch (NoSuchMethodException e) {
            log.warn("class {}.{} does not have a constructor(Blueprint)", clazz.getSimpleName(), inner.getName());
            return null;
        } catch (IllegalAccessException e) {
            log.warn("lookupInnerClass for constructor", e);
            return null;
        }

        var ann = inner.getAnnotation(BlueprintScript.class);
        assert ann != null;

        var name = ann.value();
        if (name.isEmpty()) name = inner.getName();

        var description = ann.description();

        var fs = new ArrayList<Field>();
        var ps = lookupParameter(inner, fs).toArray(BlueprintScriptCallable.Parameter[]::new);

        var fields = new MethodHandle[fs.size()];
        for (int i = 0, size = fs.size(); i < size; i++) {
            try {
                fields[i] = lookup.unreflectSetter(fs.get(i));
            } catch (IllegalAccessException e) {
                log.warn("lookupInnerClass for field", e);
                return null;
            }
        }

        return new BlueprintScriptClassHandle(clazz, runnable, name, description, blueprint, ps, constructor, fields);
    }

    private static @Nullable Class<?> checkInnerConstructor(Class<?> inner) throws NoSuchMethodException {
        try {
            inner.getConstructor(BlueprintAppToolkit.class);
            return BlueprintAppToolkit.class;
        } catch (NoSuchMethodException e) {
        }
        try {
            inner.getConstructor(BlueprintToolkit.class);
            return BlueprintToolkit.class;
        } catch (NoSuchMethodException e) {
        }
        try {
            inner.getConstructor(Blueprint.class);
            return Blueprint.class;
        } catch (NoSuchMethodException e) {
        }
        try {
            inner.getConstructor();
            return null;
        } catch (NoSuchMethodException e) {
        }

        throw new NoSuchMethodException(inner.getSimpleName() + "(Blueprint)");
    }


    private static List<BlueprintScriptCallable.Parameter> lookupParameter(Class<?> inner, List<Field> fields) {
        var ret = new ArrayList<BlueprintScriptCallable.Parameter>();
        Field[] parameters = inner.getFields();
        for (Field parameter : parameters) {
            var ann = parameter.getAnnotation(ScriptParameter.class);
            if (ann != null) {
                log.trace("class {} find field {}", inner.getSimpleName(), parameter.getName());
                ret.add(newParameter(parameter.getType(), ann, false));
                fields.add(parameter);
            }
        }
        return ret;
    }

    private static BlueprintScriptCallable.Parameter newParameter(Class<?> type, ScriptParameter ann, boolean isVarArg) {
        var name = ann.value();

        var typeDesp = ann.type();
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

    public static Object[] pairScriptArguments(BlueprintScriptCallable callable, Tokenize tokens) {
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
                    throw new RuntimeException("parameter " + parameter.name() + "is required");
                } else {
                    ret.set(i, castScriptArgument(parameter, (String) null, new Tokenize(parameter.defaultValue()).parseValue()));
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
                                                      @Nullable String rawString,
                                                      PyValue value) {
        var converter = parameter.converter();
        if (converter == ScriptParameter.RawString.class) {
            return rawString;
        } else if (converter != ScriptParameter.AutoCasting.class) {
            return castScriptArgument(parameter, converter, value);
        }

        var target = parameter.type();
        if (target == boolean.class || target == Boolean.class) {
            return switch (value) {
                case PyValue.PyBool(var ret) -> ret;
                case PyValue.PyInt ret -> ret.asBool().value();
                case PyValue.PyIterable ret -> ret.asBool().value();
                case PyValue.PyDict ret -> ret.asBool().value();
                case PyValue.PyStr ret -> ret.asBool().value();
                case PyValue.PyNone _ -> false;
                case null, default -> throwCCE(rawString, "bool");
            };
        } else if (target == int.class || target == Integer.class) {
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
        } else if (target == File.class) {
            return switch (value) {
                case PyValue.PyStr(String ret) -> new File(ret);
                case PyValue.PySymbol(String ret) -> new File(ret);
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(rawString, "File");
            };
        } else if (target == Path.class) {
            return switch (value) {
                case PyValue.PyStr(String ret) -> Path.of(ret);
                case PyValue.PySymbol(String ret) -> Path.of(ret);
                case PyValue.PyNone _ -> null;
                case null, default -> throwCCE(rawString, "Path");
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

    private static Object throwCCE(@Nullable String rawString, String target) {
        throw new ClassCastException("cannot cast '" + rawString + "' to " + target + ".");
    }
}
