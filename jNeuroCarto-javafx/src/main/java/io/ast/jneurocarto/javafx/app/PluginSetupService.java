package io.ast.jneurocarto.javafx.app;

import java.util.List;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class PluginSetupService {
    private @Nullable Application<?> app;

    PluginSetupService(Application<?> app) {
        this.app = app;
    }

    void dispose() {
        app = null;
    }

    public void addMenuInBar(Menu menu) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        var items = app.menuFile.getItems();
        items.add(items.size() - 1, menu);
    }

    public void addMenuInEdit(MenuItem item, boolean isProbePlugin) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        var index = app.findMenuItemIndex(app.menuEdit, isProbePlugin);
        app.menuEdit.getItems().add(index, item);
    }

    public void addMenuInEdit(List<MenuItem> items, boolean isProbePlugin) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        var index = app.findMenuItemIndex(app.menuEdit, isProbePlugin);
        app.menuEdit.getItems().addAll(index, items);
    }

    public void addMenuInView(MenuItem item, boolean isProbePlugin) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        var index = app.findMenuItemIndex(app.menuView, isProbePlugin);
        app.menuView.getItems().add(index, item);
    }

    public void addMenuInView(List<MenuItem> items, boolean isProbePlugin) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        var index = app.findMenuItemIndex(app.menuView, isProbePlugin);
        app.menuView.getItems().addAll(index, items);
    }

    public void addMenuInHelp(MenuItem items) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        var index = app.findMenuItemIndex(app.menuHelp, false);
        app.menuHelp.getItems().add(index, items);
    }

    public ProbeView<?> getProbeView() {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        return app.view;
    }

    public void addAboveProbeView(Node node) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        app.viewLayout.getChildren().addFirst(node);
    }

    public void addBelowProbeView(Node node) {
        var app = Objects.requireNonNull(this.app, "service is finished.");
        app.viewLayout.getChildren().add(node);
    }
}
