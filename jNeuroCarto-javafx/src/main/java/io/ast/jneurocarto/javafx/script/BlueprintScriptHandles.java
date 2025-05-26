package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;

public final class BlueprintScriptHandles {

    private static final Logger log = LoggerFactory.getLogger(BlueprintScriptHandles.class);

    private BlueprintScriptHandles() {
        throw new RuntimeException();
    }

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
        return ret;
    }

    private static @Nullable BlueprintScriptHandle lookupMethod(MethodHandles.Lookup lookup, Class<?> clazz, Method method, @Nullable Object instance) {
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
            log.warn("lookupMethod", e);
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

        return new BlueprintScriptHandle(clazz, method, name, description, blueprint, ps, handle);
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

            Class<?> type = parameter.getType();
            var isvararg = parameter.isVarArgs();
            if (isvararg) type = type.getComponentType();

            if (ann == null) {
                var name = parameter.getName();
                var desp = type.getSimpleName();
                var converter = ScriptParameter.AutoCasting.class;

                ret.add(new BlueprintScriptCallable.Parameter(name, type, desp, null, null, converter, isvararg));
            } else {
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

                ret.add(new BlueprintScriptCallable.Parameter(name, type, typeDesp, defv, desp, converter, isvararg));
            }
        }
        return ret;
    }

}
