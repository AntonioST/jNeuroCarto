package io.ast.jneurocarto.javafx.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.config.Repository;
import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ProbeDescription;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class Application {

    private static Application INSTANCE = null;
    private final CartoConfig config;
    private final Repository repository;
    private final ProbeDescription<?> probe;
    private final Logger log;

    public Application(CartoConfig config) {
        INSTANCE = this;
        this.config = config;
        repository = new Repository(config);
        probe = ProbeDescription.getProbeDescription(config.probeFamily);
        log = LoggerFactory.getLogger(Application.class);
    }

    public static Application getInstance() {
        return INSTANCE;
    }

    public static void printMessage(String message) {
        getInstance().printMessage0(message);
    }

    public static void printMessage(List<String> message) {
        getInstance().printMessage0(message);
    }

    /*========*
     * layout *
     *========*/

    private Stage stage;
    private ChoiceBox<String> inputFilesSelect;
    private TextField outputImroFile;
    private TextArea logMessageArea;

    public void start(Stage stage) {
        this.stage = stage;
        log.debug("start");
        stage.setTitle("jNeuroCarto - " + config.probeFamily);
        stage.setScene(scene());
        stage.sizeToScene();
        stage.show();
    }

    private Scene scene() {
        var scene = new Scene(root());
        return scene;
    }

    private Parent root() {
        log.debug("init layout");
        return new HBox(rootLeft(), rootCenter(), rootRight());
    }

    private Parent rootLeft() {
        log.debug("init layout - left");

        inputFilesSelect = new ChoiceBox<>();
        inputFilesSelect.setPrefWidth(290);

        var newProbeSelect = new ChoiceBox<String>();
        newProbeSelect.setPrefWidth(90);
        newProbeSelect.getItems().addAll(probe.supportedProbeType());
        newProbeSelect.setOnAction(this::onNewProbe);
        newProbeSelect.setConverter(new StringConverter<>() {
            @Override
            public String toString(String code) {
                if (code == null) return "New";
                try {
                    return probe.probeTypeDescription(code);
                } catch (IllegalArgumentException | NullPointerException e) {
                    return "<unknown>";
                }
            }

            @Override
            public String fromString(String desp) {
                if (desp != null) {
                    for (var code : probe.supportedProbeType()) {
                        if (probe.probeTypeDescription(code).equals(desp)) {
                            return code;
                        }
                    }
                }
                return null;
            }
        });

        var btnOpen = new Button("Open");
        btnOpen.setOnAction(this::onLoadProbe);
        btnOpen.setPrefWidth(90);

        var btnSave = new Button("Save");
        btnSave.setOnAction(this::onSaveProbe);
        btnSave.setPrefWidth(90);

        var btnGroup = new HBox(newProbeSelect, btnOpen, btnSave);
        btnGroup.setSpacing(5);

        outputImroFile = new TextField();
        outputImroFile.setPrefWidth(290);

        var layoutState = new GridPane();
        layoutState.setHgap(5);
        layoutState.setVgap(5);
        addAllIntoGridPane(layoutState, 2, probe.availableStates(), (state) -> {
            var button = new Button(state);
            button.setPrefWidth(140);
            button.setOnAction(this::onStateChanged);
            return button;
        });

        var layoutCate = new GridPane();
        layoutCate.setHgap(5);
        layoutCate.setVgap(5);
        addAllIntoGridPane(layoutCate, 2, probe.availableCategories(), (state) -> {
            var button = new Button(state);
            button.setPrefWidth(140);
            button.setOnAction(this::onCategoryChanged);
            return button;
        });

        logMessageArea = new TextArea();
        logMessageArea.setEditable(false);
        logMessageArea.setPrefWidth(290);
        logMessageArea.setPrefColumnCount(100);


        var root = new VBox(
          new Label("Input file"),
          inputFilesSelect,
          btnGroup,
          new Label("Save filename"),
          outputImroFile,
          new Label("State"),
          layoutState,
          new Label("Category"),
          layoutCate,
          new Label("Log"),
          logMessageArea
        );

        root.setMaxWidth(300);
        root.setSpacing(5);
        root.setPadding(new Insets(5, 2, 5, 2));
        return root;
    }

    private Parent rootCenter() {
        log.debug("init layout - center");
        var root = new VBox();
        return root;
    }

    private Parent rootRight() {
        log.debug("init layout - right");
        var root = new VBox();
        return root;
    }

    /*================*
     * event handlers *
     *================*/

    private void onNewProbe(ActionEvent e) {

    }

    private void onLoadProbe(ActionEvent e) {

    }

    private void onSaveProbe(ActionEvent e) {

    }

    private void onStateChanged(ActionEvent e) {

    }

    private void onCategoryChanged(ActionEvent e) {

    }

    /*=================*
     * event utilities *
     *=================*/

    private void updateChannelmapFileList() {
        updateChannelmapFileList(inputFilesSelect.getValue());
    }

    private void updateChannelmapFileList(String preSelectFile) {
        List<Path> files;
        try {
            files = repository.listChannelmapFiles(probe, false);
        } catch (IOException e) {
            printMessage0("fail to update.");
            log.warn("updateChannelMapFileList", e);
            return;
        }

        var items = files.stream()
          .map(Path::getFileName)
          .map(Path::toString)
          .toList();

        inputFilesSelect.getItems().clear();
        inputFilesSelect.getItems().addAll(items);
        if (preSelectFile != null && items.contains(preSelectFile)) {
            inputFilesSelect.setValue(preSelectFile);
        }
    }

    /*=============*
     * log message *
     *=============*/

    private void printMessage0(String message) {
        log.info(message);

        var area = logMessageArea;
        if (area != null) {
            var content = area.getText();
            area.setText(message + "\n" + content);
        }
    }

    private void printMessage0(@NonNull List<String> message) {
        if (log.isInfoEnabled()) {
            message.forEach(log::info);
        }

        var area = logMessageArea;
        if (area != null) {
            var content = area.getText();
            var updated = Stream.concat(message.stream(), Stream.of(content))
              .collect(Collectors.joining("\n"));
            area.setText(updated);
        }
    }

    public void clearMessages() {
        var area = logMessageArea;
        if (area != null) {
            area.setText("");
        }
    }

    /*==================*
     * layout utilities *
     *==================*/

    private <T> void addAllIntoGridPane(GridPane layout, int column, List<T> data, Function<T, Node> factory) {
        for (int i = 0, size = data.size(); i < size; i++) {
            var node = factory.apply(data.get(i));
            layout.add(node, i % column, i / column);
        }
    }
}
