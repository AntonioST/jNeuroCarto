package io.ast.jneurocarto.javafx.base;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

@NullMarked
public final class Plugins {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final @Nullable Class<?> APP;

    static {
        Class<?> app;
        try {
            app = LOOKUP.findClass("io.ast.jneurocarto.javafx.app.Application");
        } catch (ClassNotFoundException | IllegalAccessException e) {
            app = null;
        }
        APP = app;
    }

    private static final @Nullable MethodHandle APP_PM_S;
    private static final @Nullable MethodHandle APP_PM_L;
    private static final @Nullable MethodHandle APP_AM;
    private static final @Nullable MethodHandle APP_AME_I;
    private static final @Nullable MethodHandle APP_AME_L;
    private static final @Nullable MethodHandle APP_AMV_I;
    private static final @Nullable MethodHandle APP_AMV_L;

    static {
        MethodHandle PMS = null;
        MethodHandle PML = null;
        MethodHandle AM = null;
        MethodHandle AME_I = null;
        MethodHandle AME_L = null;
        MethodHandle AMV_I = null;
        MethodHandle AMV_L = null;

        if (APP != null) {
            try {
                PMS = LOOKUP.findStatic(APP, "printMessage", MethodType.methodType(Void.class, String.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
            try {
                PML = LOOKUP.findStatic(APP, "printMessage", MethodType.methodType(Void.class, List.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
            try {
                AM = LOOKUP.findStatic(APP, "addMenuInBar", MethodType.methodType(Void.class, Menu.class, boolean.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
            try {
                AME_I = LOOKUP.findStatic(APP, "addMenuInEdit", MethodType.methodType(Void.class, MenuItem.class, boolean.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
            try {
                AME_L = LOOKUP.findStatic(APP, "addMenuInEdit", MethodType.methodType(Void.class, List.class, boolean.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
            try {
                AMV_I = LOOKUP.findStatic(APP, "addMenuInView", MethodType.methodType(Void.class, MenuItem.class, boolean.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
            try {
                AMV_L = LOOKUP.findStatic(APP, "addMenuInView", MethodType.methodType(Void.class, List.class, boolean.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
            }
        }

        APP_PM_S = PMS;
        APP_PM_L = PML;
        APP_AM = AM;
        APP_AME_I = AME_I;
        APP_AME_L = AME_L;
        APP_AMV_I = AMV_I;
        APP_AMV_L = AMV_L;
    }

    public static void printLogMessage(String message) {
        var h = APP_PM_S;
        if (h != null) {
            try {
                h.invokeExact(message);
            } catch (Throwable e) {
                System.err.println(e);
            }
        }
    }

    public static void printLogMessage(List<String> message) {
        var h = APP_PM_L;
        if (h != null) {
            try {
                h.invokeExact(message);
            } catch (Throwable e) {
                System.err.println(e);
            }
        }
    }

    public static void addMenuInBar(Menu menu) {
        var h = APP_AM;
        if (h != null) {
            try {
                h.invokeExact(menu);
            } catch (Throwable e) {
                System.err.println(e);
            }
        }
    }

    public static void addMenuInEdit(MenuItem item, boolean isProbePlugin) {
        var h = APP_AME_I;
        if (h != null) {
            try {
                h.invokeExact(item, isProbePlugin);
            } catch (Throwable e) {
                System.err.println(e);
            }
        }
    }

    public static void addMenuInEdit(List<MenuItem> items, boolean isProbePlugin) {
        var h = APP_AME_L;
        if (h != null) {
            try {
                h.invokeExact(items, isProbePlugin);
            } catch (Throwable e) {
                System.err.println(e);
            }
        }
    }

    public static void addMenuInView(MenuItem item, boolean isProbePlugin) {
        var h = APP_AMV_I;
        if (h != null) {
            try {
                h.invokeExact(item, isProbePlugin);
            } catch (Throwable e) {
                System.err.println(e);
            }
        }
    }

    public static void addMenuInView(List<MenuItem> items, boolean isProbePlugin) {
        var h = APP_AMV_L;
        if (h != null) {
            try {
                h.invokeExact(items, isProbePlugin);
            } catch (Throwable e) {
                System.err.println(e);
            }
        }
    }
}
