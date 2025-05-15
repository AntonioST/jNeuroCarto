package io.ast.jneurocarto.javafx.view;

import io.ast.jneurocarto.javafx.app.Application;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;

public interface InvisibleView {
    static void setupMenuView(Plugin plugin, Node node) {
        var item = new CheckMenuItem(plugin.name());
        item.selectedProperty().bindBidirectional(node.visibleProperty());
        Application.getInstance().addMenuInView(item, plugin instanceof ProbePlugin);
    }
}
