package io.ast.jneurocarto.javafx.app;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.core.RequestChannelmapInfo;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.core.config.Repository;
import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.Provide;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;

@NullMarked
public final class PluginSetupService {

    private @Nullable Application<?> app;
    private @Nullable Plugin plugin;

    PluginSetupService(Application<?> app) {
        this.app = app;
    }

    /*==========*
     * internal *
     *==========*/

    void unbind() {
        plugin = null;
    }

    void bind(@Nullable Plugin plugin) {
        this.plugin = plugin;
    }

    void dispose() {
        app = null;
        plugin = null;
    }

    private Application<?> checkApplication() {
        return Objects.requireNonNull(this.app, "service is finished.");
    }

    private void checkPlugin() {
        Objects.requireNonNull(this.plugin, "not during UI setup.");
    }

    /**
     * fork a virtual thread for UI-excluded setup.
     *
     * @param name
     * @param consumer
     */
    public void fork(String name, Consumer<PluginSetupService> consumer) {
        var log = LoggerFactory.getLogger(PluginSetupService.class);

        log.debug("fork {}", name);
        var shadow = new PluginSetupService(app);

        Thread.ofVirtual().name(name).start(() -> {
            log.debug("fork start");
            var start = System.currentTimeMillis();
            try {
                consumer.accept(shadow);
            } finally {
                var pass = System.currentTimeMillis() - start;
                shadow.dispose();
                if (pass > 10_000) {
                    log.debug("fork end, use {} sec", String.format("%.4f", (double) pass / 1000));
                } else {
                    log.debug("fork end, use {} ms", pass);
                }
            }
        });
    }

    /*========*
     * getter *
     *========*/

    public CartoConfig getCartoConfig() {
        var app = checkApplication();
        return app.config;
    }

    public Repository getRepository() {
        var app = checkApplication();
        return app.repository;
    }

    public ProbeDescription<?> getProbeDescription() {
        var app = checkApplication();
        return app.probe;
    }

    public @Nullable Plugin getPlugin(String name) {
        var app = checkApplication();
        return app.getPlugin(name);
    }

    public <P extends Plugin> @Nullable P getPlugin(Class<P> cls) {
        var app = checkApplication();
        return app.getPlugin(cls);
    }

    /*======*
     * menu *
     *======*/

    public void addMenuInBar(Menu menu) {
        var app = checkApplication();
        var items = app.menuFile.getItems();
        items.add(items.size() - 1, menu);
    }

    public void addMenuInEdit(MenuItem... item) {
        addMenuInEdit(Arrays.asList(item));
    }

    public void addMenuInEdit(List<MenuItem> items) {
        var app = checkApplication();
        checkPlugin();
        var index = app.findMenuItemIndex(app.menuEdit, plugin instanceof ProbePlugin);
        app.menuEdit.getItems().addAll(index, items);
    }

    public void addMenuInView(MenuItem... item) {
        addMenuInView(Arrays.asList(item));
    }

    public void addMenuInView(List<MenuItem> items) {
        var app = checkApplication();
        checkPlugin();
        var index = app.findMenuItemIndex(app.menuView, plugin instanceof ProbePlugin);
        app.menuView.getItems().addAll(index, items);
    }

    public void addMenuInHelp(MenuItem items) {
        var app = checkApplication();
        var index = app.findMenuItemIndex(app.menuHelp, false);
        app.menuHelp.getItems().add(index, items);
    }

    /*============*
     * probe view *
     *============*/

    public ProbeView<?> getProbeView() {
        var app = checkApplication();
        return app.view;
    }

    public void addAboveProbeView(Node node) {
        var app = checkApplication();
        app.viewLayout.getChildren().addFirst(node);
    }

    public void addBelowProbeView(Node node) {
        var app = checkApplication();
        app.viewLayout.getChildren().add(node);
    }

    /*========*
     * plugin *
     *========*/

    record PluginInfo(PluginProvider provider, Class<? extends Plugin> plugin, String[] name) {
    }

    List<PluginInfo> getProvidePlugins(PluginProvider provider, ProbeDescription<?> probe) {
        var cls = provider.getClass();

        var check = cls.getAnnotation(RequestChannelmap.class);
        if (check != null) {
            var request = RequestChannelmapInfo.of(check);
            if (request == null || !request.checkProbe(probe)) return List.of();
        }

        return Arrays.stream(cls.getAnnotationsByType(Provide.class))
          .map(it -> new PluginInfo(provider, it.value(), it.name()))
          .toList();
    }

    List<PluginInfo> filterPluginByNameRule(List<PluginInfo> provides, String rule) {
        var tester = createPluginFilter(rule);
        return provides.stream().filter(tester).toList();
    }

    static Predicate<PluginInfo> createPluginFilter(String rule) {
        Predicate<String> tester;

        if (rule.endsWith(".*")) {
            var name = rule.substring(0, rule.length() - 1);
            tester = it -> it.startsWith(name);
        } else if (rule.contains("*!")) {
            var i = rule.indexOf("*!");
            var name = rule.substring(0, i);
            var exclude = Arrays.stream(rule.substring(i + 2).split(","))
              .map(it -> name + it)
              .collect(Collectors.toSet());

            tester = it -> it.startsWith(name) && !exclude.contains(it);
        } else {
            tester = it -> it.equals(rule);
        }

        return plugin -> tester.test(plugin.plugin().getName())
                         || Arrays.stream(plugin.name()).anyMatch(tester);
    }

    <P extends Plugin> P loadPlugin(Class<P> plugin) throws Throwable {

        Constructor<P> ctor = null;

        try {
            ctor = plugin.getConstructor(PluginSetupService.class);
        } catch (NoSuchMethodException e) {
        }

        if (ctor != null) {
            return loadPlugin(ctor);
        }

        for (var c : plugin.getConstructors()) {
            try {
                return loadPlugin((Constructor<P>) c);
            } catch (Throwable e) {
            }
        }

        throw new RuntimeException("cannot initialize plugin : " + plugin.getName());
    }

    <P extends Plugin> P loadPlugin(Constructor<P> ctor) throws Throwable {
        var ps = ctor.getParameters();
        var os = new Object[ps.length];
        for (int i = 0, length = ps.length; i < length; i++) {
            var t = ps[i].getType();
            if (t == PluginSetupService.class) {
                os[i] = this;
            } else if (t == Application.class) {
                os[i] = app;
            } else if (t == CartoConfig.class) {
                os[i] = app.config;
            } else if (t == Repository.class) {
                os[i] = app.repository;
            } else if (t == ProbeDescription.class) {
                os[i] = app.probe;
//            } else if (t == ProbeView.class) {
//                os[i] = app.view;
            } else if (ProbeDescription.class.isAssignableFrom(t)) {
                var d = app.probe;
                if (t.isInstance(d)) {
                    os[i] = d;
                } else {
                    os[i] = null;
                }
            } else {
                throw new RuntimeException("unsupported parameter inject for " + t.getName());
            }
        }

        return ctor.newInstance(os);
    }

    /*======*
     * scan *
     *======*/

    public <A extends Annotation> List<Class<?>> scanAnnotation(Class<A> annotation) {
        return scanAnnotation(annotation, _ -> true);
    }

    public <A extends Annotation> List<Class<?>> scanAnnotation(Class<A> annotation, Predicate<ClassInfo> filter) {
        var app = checkApplication();

        var packages = app.providers.stream()
          .map(it -> it.getClass().getPackageName())
          .toArray(String[]::new);

        var scan = new ClassGraph()
//          .verbose()
          .enableAnnotationInfo()
          .acceptPackages(packages);

        var ret = new ArrayList<Class<?>>();
        try (var result = scan.scan()) {
            for (var clazz : result.getClassesWithAllAnnotations(annotation)) {
                if (filter.test(clazz)) {
                    ret.add(clazz.loadClass());
                }
            }
        }
        return ret;
    }

    public <T> List<Class<T>> scanInterface(Class<T> interface_) {
        return scanInterface(interface_, _ -> true);
    }

    public <T> List<Class<T>> scanInterface(Class<T> interface_, Predicate<ClassInfo> filter) {
        if (!interface_.isInterface()) throw new IllegalArgumentException();
        var app = checkApplication();

        var packages = app.providers.stream()
          .map(it -> it.getClass().getPackageName())
          .toArray(String[]::new);

        var scan = new ClassGraph()
//          .verbose()
          .enableClassInfo()
          .acceptPackages(packages);

        var ret = new ArrayList<Class<T>>();
        try (var result = scan.scan()) {
            for (var clazz : result.getClassesImplementing(interface_)) {
                if (filter.test(clazz)) {
                    ret.add((Class<T>) clazz.loadClass());
                }
            }
        }
        return ret;
    }
}
