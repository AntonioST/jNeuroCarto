package io.ast.jneurocarto.javafx.app;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.ProbePluginProvider;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;

@NullMarked
public final class PluginSetupService {

    private @Nullable Application<?> app;
    private @Nullable List<Object> provider;
    private @Nullable Plugin plugin;
    private List<PluginSetupService> subServices = new ArrayList<>();

    PluginSetupService(Application<?> app) {
        this.app = app;
    }

    void unbind() {
        provider = null;
        plugin = null;
    }

    void bind(PluginProvider provider, @Nullable Plugin plugin) {
        this.provider = List.of(provider);
        this.plugin = plugin;
    }

    void bind(ProbePluginProvider provider, ProbePlugin<?> plugin) {
        this.provider = List.of(provider);
        this.plugin = plugin;
    }

    public Object getProvider() {
        return Objects.requireNonNull(provider, "service for plugin setup is finished.");
    }

    void dispose() {
        app = null;
        provider = null;
        plugin = null;

        for (var service : subServices) {
            service.dispose();
        }
    }

    private Application<?> checkApplication() {
        return Objects.requireNonNull(this.app, "service is finished.");
    }

    public PluginSetupService asProbePluginSetupService() {
        var app = checkApplication();
        var ret = new PluginSetupService(app);
        ret.provider = new ArrayList<>(app.providers);
        subServices.add(ret);
        return ret;
    }

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
        var index = app.findMenuItemIndex(app.menuEdit, plugin instanceof ProbePlugin);
        app.menuEdit.getItems().addAll(index, items);
    }

    public void addMenuInView(MenuItem... item) {
        addMenuInView(Arrays.asList(item));
    }

    public void addMenuInView(List<MenuItem> items) {
        var app = checkApplication();
        var index = app.findMenuItemIndex(app.menuView, plugin instanceof ProbePlugin);
        app.menuView.getItems().addAll(index, items);
    }

    public void addMenuInHelp(MenuItem items) {
        var app = checkApplication();
        var index = app.findMenuItemIndex(app.menuHelp, false);
        app.menuHelp.getItems().add(index, items);
    }

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

    public <A extends Annotation> List<Class<?>> scanAnnotation(Class<A> annotation) {
        return scanAnnotation(annotation, _ -> true);
    }

    public <A extends Annotation> List<Class<?>> scanAnnotation(Class<A> annotation, Predicate<ClassInfo> filter) {
        checkApplication();
        var provider = this.provider;
        if (provider == null) throw new RuntimeException("service for plugin setup is finished.");

        var packages = provider.stream()
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
        checkApplication();
        var provider = this.provider;
        if (provider == null) throw new RuntimeException("service for plugin setup is finished.");

        var packages = provider.stream()
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
