package io.ast.jneurocarto.javafx.view;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.core.config.Repository;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.utils.FormattedTextField;

public abstract class AbstractImagePlugin extends InvisibleView {

    protected final CartoConfig config;
    protected Repository repository;
    protected ProbeView<?> canvas;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public AbstractImagePlugin(CartoConfig config) {
        this.config = config;
    }

    /*============*
     * properties *
     *============*/

    public final BooleanProperty showImageProperty = new SimpleBooleanProperty();

    public final boolean isShowImage() {
        return showImageProperty.get();
    }

    public final void setShowImage(boolean value) {
        showImageProperty.set(value);
    }

    public final ObjectProperty<@Nullable Path> fileProperty = new SimpleObjectProperty<>();

    public final @Nullable Path getFile() {
        return fileProperty.get();
    }

    public final void setFile(@Nullable Path value) {
        fileProperty.set(value);
    }

    /*===========*
     * UI layout *
     *===========*/

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        repository = service.getRepository();
        canvas = service.getProbeView();

        return super.setup(service);
    }

    @Override
    protected HBox setupHeading(PluginSetupService service) {
        var ret = super.setupHeading(service);

        var showImageSwitch = new CheckBox("Show image");
        showImageSwitch.selectedProperty().bindBidirectional(showImageProperty);
        showImageSwitch.selectedProperty().addListener((_, _, _) -> repaint());
        visible.addListener((_, _, e) -> showImageSwitch.setSelected(e));

        ret.getChildren().add(showImageSwitch);
        ret.setSpacing(10);

        return ret;
    }

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {
        var open = new Button("Open");
        open.setOnAction(this::onOpenData);

        var dataFileField = new FormattedTextField.OfObjectField<>(fileProperty, new FileValidator());

        var draw = new Button("Draw");
        draw.setOnAction(this::onDrawData);

        var clear = new Button("Clear");
        clear.setOnAction(this::onClearData);

        var layout = new HBox(open, dataFileField, draw, clear);
        HBox.setHgrow(dataFileField, Priority.ALWAYS);
        layout.setSpacing(5);

        return layout;
    }

    private class FileValidator extends StringConverter<Path> {
        @Override
        public String toString(Path path) {
            return path == null ? "" : path.toString();
        }

        @Override
        public Path fromString(String path) {
            var ret = Path.of(path);
            if (!ret.isAbsolute()) {
                ret = repository.getCurrentResourceRoot().resolve(ret);
            }
            ret = ret.normalize();
            if (!Files.isReadable(ret)) {
                throw new RuntimeException("File not found");
            }

            return ret;
        }
    }

    protected void onOpenData(ActionEvent e) {
    }

    protected void onDrawData(ActionEvent e) {
    }

    protected void onClearData(ActionEvent e) {
    }

    protected Optional<Path> openDataFileDialog(String title, FileChooser.ExtensionFilter filter) {
        var chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(repository.getCurrentResourceRoot().toFile());

        chooser.setSelectedExtensionFilter(filter);

        File file = chooser.showOpenDialog(null);
        return file == null ? Optional.empty() : Optional.of(file.toPath());
    }

    /*================*
     * curve plotting *
     *================*/

    public abstract void repaint();
}
