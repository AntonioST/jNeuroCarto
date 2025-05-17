package io.ast.jneurocarto.javafx.view;

import io.ast.jneurocarto.javafx.app.PluginSetupService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;

public abstract class InvisibleView {

    public final BooleanProperty visible = new SimpleBooleanProperty(true);

    public boolean isVisible() {
        return visible.get();
    }

    public void setVisible(boolean value) {
        visible.set(value);
    }

    protected CheckBox newInvisibleSwitch(String name) {
        var visibleSwitch = new CheckBox(name);
        // TODO styling?
        visibleSwitch.selectedProperty().bindBidirectional(visible);
        return visibleSwitch;
    }

    protected void bindInvisibleNode(Node node) {
        node.visibleProperty().bindBidirectional(visible);
    }

    protected void setupMenuViewItem(PluginSetupService service, String name) {
        var item = new CheckMenuItem(name);
        item.selectedProperty().bindBidirectional(visible);
        service.addMenuInView(item, this instanceof ProbePlugin);
    }
}
