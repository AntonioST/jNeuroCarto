package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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

    public static List<BlueprintScriptHandle> lookupClass(MethodHandles.Lookup lookup, Class<?> clazz) {
        log.debug("lookup class  {}", clazz.getSimpleName());

        var ret = new ArrayList<BlueprintScriptHandle>();
        for (var method : clazz.getMethods()) {
            var ann = method.getDeclaredAnnotation(BlueprintScript.class);
            if (ann != null) {
                var h = lookupMethod(lookup, clazz, method);
                if (h != null) {
                    ret.add(h);
                }
            }
        }
        return ret;
    }

    private static @Nullable BlueprintScriptHandle lookupMethod(MethodHandles.Lookup lookup, Class<?> clazz, Method method) {
        log.debug("lookup method {}.{}", clazz.getSimpleName(), method.getName());

        var modifiers = method.getModifiers();
        if ((modifiers & Modifier.PUBLIC) == 0) {
            log.warn("method {}.{} not public", clazz.getSimpleName(), method.getName());
            return null;
        }
        if ((modifiers & Modifier.STATIC) == 0) {
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

        var ps = lookupParameter(method).toArray(BlueprintScriptHandle.ScriptParameter[]::new);
        return new BlueprintScriptHandle(clazz, method, name, blueprint, ps, handle);
    }

    private static @Nullable Class<?> checkMethodParameter(Parameter parameter) {
        var type = parameter.getType();
        if (type == Blueprint.class) return type;
        if (BlueprintToolkit.class.isAssignableFrom(type)) return type;
        return null;
    }

    private static List<BlueprintScriptHandle.ScriptParameter> lookupParameter(Method method) {
        var ret = new ArrayList<BlueprintScriptHandle.ScriptParameter>();
        Parameter[] parameters = method.getParameters();
        for (int i = 1, length = parameters.length; i < length; i++) {
            var parameter = parameters[i];
            var ann = parameter.getAnnotation(ScriptParameter.class);
            if (ann == null) {
                ret.add(new BlueprintScriptHandle.ScriptParameter(parameter.getName(), parameter.getType(), null));
            } else {
                var name = ann.value();
                var defaultValue = ann.defaultValue();
                if (defaultValue == ScriptParameter.NO_DEFAULT) defaultValue = null;
                ret.add(new BlueprintScriptHandle.ScriptParameter(name, parameter.getType(), defaultValue));
            }
        }
        return ret;
    }

}
