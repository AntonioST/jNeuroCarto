package io.ast.jneurocarto.javafx.base;

import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;

public interface InvisibleView {
    static void setupMenuView(Plugin plugin, Node node) {
        var item = new CheckMenuItem(plugin.name());
        item.selectedProperty().bindBidirectional(node.visibleProperty());
        Plugins.addMenuInView(item, plugin instanceof ProbePlugin);
    }
}
