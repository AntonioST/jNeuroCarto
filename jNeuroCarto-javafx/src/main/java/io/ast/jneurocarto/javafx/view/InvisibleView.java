package io.ast.jneurocarto.javafx.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;

public abstract class InvisibleView implements Plugin {

    /*============*
     * properties *
     *============*/

    public final BooleanProperty visible = new SimpleBooleanProperty(true);

    public boolean isVisible() {
        return visible.get();
    }

    public void setVisible(boolean value) {
        visible.set(value);
    }

    /*===========*
     * UI layout *
     *===========*/

    protected void bindInvisibleNode(Node node) {
        node.visibleProperty().bind(visible);
        node.managedProperty().bind(visible);
    }

    /// Set up a common UI layout define by [InvisibleView].
    ///
    /// The order of sub-setup process follows the order:
    ///
    /// 1. [setupChartContent][#setupChartContent(PluginSetupService, ProbeView)].
    ///     It usually used to call [PluginSetupService#getProbeView()] to get the
    ///     [ProbeView][io.ast.jneurocarto.javafx.app.ProbeView]. It also can be used as pre-setup.
    /// 2. [setupHeading][#setupHeading(PluginSetupService)].
    ///     Set up the head row beside the visible switch (bind with [visible][InvisibleView#visible]).
    /// 3. [setupChartContent][#setupChartContent(PluginSetupService, ProbeView)].
    ///     Set up hidden-able (managed by [visible][InvisibleView#visible]) content.
    /// 4. [setupMenuItems][#setupMenuItems(PluginSetupService)].
    ///     Set up the menu items. It also can be used as post-setup.
    ///
    /// @param service
    /// @return
    @Override
    public @Nullable Node setup(PluginSetupService service) {
        var log = LoggerFactory.getLogger(getClass());
        log.debug("setup {}", getClass().getSimpleName());

        setupChartContent(service, service.getProbeView());
        var heading = setupHeading(service);
        var content = setupContent(service);
        if (content != null) {
            bindInvisibleNode(content);

            var item = new CheckMenuItem(name());
            item.selectedProperty().bindBidirectional(visible);
            service.addMenuInView(item);
        }

        setupMenuItems(service);

        if (content == null) return heading;

        var root = new VBox(
          heading,
          content
        );
        root.setSpacing(5);
        VBox.setMargin(content, new Insets(0, 0, 0, 15));

        return root;
    }

    protected HBox setupHeading(PluginSetupService service) {
        var visibleSwitch = new CheckBox(name());
        // TODO styling?
        visibleSwitch.selectedProperty().bindBidirectional(visible);

        var layout = new HBox(visibleSwitch);
        layout.setSpacing(10);
        return layout;
    }

    protected @Nullable Node setupContent(PluginSetupService service) {
        return null;
    }

    protected void setupChartContent(PluginSetupService service, ProbeView<?> canvas) {
    }

    protected void setupMenuItems(PluginSetupService service) {
    }
}
