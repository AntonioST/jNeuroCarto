package io.ast.jneurocarto.javafx.app;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.ProbePlugin;

@NullMarked
public final class PluginSetupService {

    private @Nullable Application<?> app;
    private @Nullable Plugin plugin;

    PluginSetupService(Application<?> app) {
        this.app = app;
    }

    void bind(@Nullable Plugin plugin) {
        this.plugin = plugin;
    }

    void dispose() {
        app = null;
    }

    private Application<?> checkApplication() {
        return Objects.requireNonNull(this.app, "service is finished.");
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
}
