package io.ast.jneurocarto.javafx.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.app.PluginSetupService;

public abstract class InvisibleView implements Plugin {

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
        node.managedProperty().bindBidirectional(visible);
    }

    protected void setupMenuViewItem(PluginSetupService service, String name) {
        var item = new CheckMenuItem(name);
        item.selectedProperty().bindBidirectional(visible);
        service.addMenuInView(item);
    }

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        var heading = setupHeading(service);

        var content = setupContent(service);
        if (content == null) return heading;

        bindInvisibleNode(content);
        setupMenuViewItem(service, name());

        var paddedContent = new HBox(content);
        HBox.setHgrow(content, Priority.ALWAYS);
        paddedContent.setPadding(new Insets(0, 0, 0, 15));

        var root = new VBox(
          heading,
          paddedContent
        );
        root.setSpacing(5);

        return root;
    }

    protected HBox setupHeading(PluginSetupService service) {
        return new HBox(newInvisibleSwitch(name()));
    }

    protected abstract @Nullable Node setupContent(PluginSetupService service);
}
