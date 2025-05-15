package io.ast.jneurocarto.javafx.view;

import io.ast.jneurocarto.javafx.app.PluginSetupService;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;

public interface InvisibleView {
    static void setupMenuView(PluginSetupService service, Plugin plugin, Node node) {
        var item = new CheckMenuItem(plugin.name());
        item.selectedProperty().bindBidirectional(node.visibleProperty());
        service.addMenuInView(item, plugin instanceof ProbePlugin);
    }
}
