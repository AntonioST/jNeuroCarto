package io.ast.jneurocarto.javafx.app;

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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
        if (probe == null) throw new RuntimeException("probe " + config.probeFamily + " not found.");


        log = LoggerFactory.getLogger(Application.class);
    }

    public static Application getInstance() {
        return INSTANCE;
    }

    /*========*
     * layout *
     *========*/

    private Stage stage;
    private TextArea logMessageArea;

    public void start(Stage stage) {
        this.stage = stage;
        log.debug("start");
        stage.setTitle("jNeuroCarto - " + config.probeFamily);
        stage.setScene(scene());
        stage.sizeToScene();
        stage.setOnCloseRequest(this::checkBeforeClosing);
        stage.show();
    }

    private Scene scene() {
        var scene = new Scene(root());
        return scene;
    }

    private Parent root() {
        log.debug("init layout");
        return new VBox(
          rootMenu(),
          new HBox(rootLeft(), rootCenter(), rootRight())
        );
    }

    /*==========*
     * menu bar *
     *==========*/

    private MenuBar menuBar;
    private Menu menuFile;
    private Menu menuEdit;
    private Menu menuView;
    private Menu menuHelp;

    public sealed interface PluginMenuItem permits PluginSeparatorMenuItem, ProbePluginSeparatorMenuItem {
    }

    public static final class PluginSeparatorMenuItem extends SeparatorMenuItem implements PluginMenuItem {
    }

    public static final class ProbePluginSeparatorMenuItem extends SeparatorMenuItem implements PluginMenuItem {
    }

    private class NewProbeMenuItem extends MenuItem {
        final String code;

        NewProbeMenuItem(String code) {
            super(probe.probeTypeDescription(code));
            this.code = code;
            setOnAction(Application.this::onNewProbe);
        }
    }

    private MenuBar rootMenu() {
        menuBar = new MenuBar(
          menuFile(),
          menuEdit(),
          menuView(),
          menuHelp()
        );
        return menuBar;
    }


    private Menu menuFile() {
        menuFile = new Menu("_File");

        var newMenu = new Menu("_New");
        var newProbeTypeItems = probe.supportedProbeType().stream()
          .map(NewProbeMenuItem::new)
          .toList();
        newMenu.getItems().addAll(newProbeTypeItems);

        var open = new MenuItem("_Open");
        open.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        open.setOnAction(this::onLoadProbe);

        var save = new MenuItem("_Save");
        save.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        save.setOnAction(this::onSaveProbe);

        var saveAs = new MenuItem("Save _As");
        saveAs.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+S"));
        saveAs.setOnAction(this::onSaveAsProbe);

        var loadBlueprint = new MenuItem("Open _Blueprint");
        loadBlueprint.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+O"));
        loadBlueprint.setOnAction(this::onOpenBlueprint);

        var saveBlueprint = new MenuItem("Save _Blueprint");
        saveBlueprint.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+S"));
        saveBlueprint.setOnAction(this::onSaveBlueprint);

        var exit = new MenuItem("_Exit");
        exit.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        exit.setOnAction(this::checkBeforeClosing);

        menuFile.getItems().addAll(
          newMenu,
          open,
          save,
          saveAs,
          new SeparatorMenuItem(),
          loadBlueprint,
          saveBlueprint,
          new SeparatorMenuItem(),
          exit
        );

        return menuFile;
    }

    private Menu menuEdit() {
        menuEdit = new Menu("_Edit");

        var clear = new MenuItem("Clear _channel map");
        clear.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+C"));
        clear.setOnAction(this::clearProbe);

        var clearBlueprint = new MenuItem("Clear _blueprint");
        clearBlueprint.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+B"));
        clear.setOnAction(this::clearBlueprint);

        var editUserConfig = new MenuItem("Edit user config");

        menuEdit.getItems().addAll(
          clear,
          clearBlueprint,
          new SeparatorMenuItem(),
          new PluginSeparatorMenuItem(),
          new ProbePluginSeparatorMenuItem(),
          editUserConfig
        );

        return menuEdit;
    }

    private Menu menuView() {
        menuView = new Menu("_View");

        var resetProbeViewAxes = new MenuItem("Reset View");
        resetProbeViewAxes.setOnAction(_ -> view.resetAxesBoundaries());

        var clearLog = new MenuItem("Clear _log");
        clearLog.setAccelerator(KeyCombination.keyCombination("Shortcut+L"));
        clearLog.setOnAction(_ -> clearMessages());

        menuView.getItems().addAll(
          resetProbeViewAxes,
          new SeparatorMenuItem(),
          new PluginSeparatorMenuItem(),
          new ProbePluginSeparatorMenuItem(),
          clearLog
        );

        return menuView;
    }

    private Menu menuHelp() {
        menuHelp = new Menu("_Help");

        var about = new MenuItem("_About");
        about.setOnAction(this::showAbout);

        menuHelp.getItems().addAll(
          about
        );

        return menuHelp;
    }

    public void addMenuInBar(Menu menu) {
        var items = menu.getItems();
        items.add(items.size() - 2, menu);
    }

    public void addMenuInEdit(MenuItem item, boolean isProbePlugin) {
        var index = findMenuItemIndex(menuEdit, isProbePlugin);
        menuEdit.getItems().add(index - 1, item);
    }

    public void addMenuInEdit(List<MenuItem> items, boolean isProbePlugin) {
        var index = findMenuItemIndex(menuEdit, isProbePlugin);
        menuEdit.getItems().addAll(index - 1, items);
    }

    public void addMenuInView(MenuItem item, boolean isProbePlugin) {
        var index = findMenuItemIndex(menuView, isProbePlugin);
        menuView.getItems().add(index - 1, item);
    }

    public void addMenuInView(List<MenuItem> items, boolean isProbePlugin) {
        var index = findMenuItemIndex(menuView, isProbePlugin);
        menuView.getItems().addAll(index - 1, items);
    }

    private int findMenuItemIndex(Menu menu, boolean isProbePlugin) {
        Class<?> kind;
        if (isProbePlugin) {
            kind = ProbePluginSeparatorMenuItem.class;
        } else {
            kind = PluginSeparatorMenuItem.class;
        }

        var items = menu.getItems();
        var size = items.size();
        for (int i = 0; i < size; i++) {
            if (items.get(i).getClass() == kind) {
                return i;
            }
        }
        return size;
    }

    /*==============*
     * content view *
     *==============*/

    private ProbeView view;


    private static class CodedButton extends Button {
        final String code;

        CodedButton(String code, EventHandler<ActionEvent> callback) {
            super(code);
            this.code = code;
            setOnAction(callback);
        }
    }

    private Parent rootLeft() {
        log.debug("init layout - left");

        var layoutState = new GridPane();
        layoutState.setHgap(5);
        layoutState.setVgap(5);
        addAllIntoGridPane(layoutState, 2, probe.availableStates(),
          (state) -> new CodedButton(state, this::onStateChanged));

        var layoutCate = new GridPane();
        layoutCate.setHgap(5);
        layoutCate.setVgap(5);
        addAllIntoGridPane(layoutCate, 2, probe.availableCategories(),
          (category) -> new CodedButton(category, this::onCategoryChanged));

        logMessageArea = new TextArea();
        logMessageArea.setEditable(false);
        logMessageArea.setPrefWidth(290);
        logMessageArea.setPrefColumnCount(100);


        var root = new VBox(
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

        var toolbox = new HBox();

        view = new ProbeView<>(config, probe);
        view.setMinWidth(600);
        view.setMinHeight(800);

        var root = new VBox(
          toolbox,
          view
        );
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
        if (e.getSource() instanceof NewProbeMenuItem item) {
            printMessage("new probe " + item.code);
            clearProbe(item.code);
        }
    }

    private void onLoadProbe(ActionEvent e) {
        log.debug("TODO onLoadProbe");
    }

    private void onSaveProbe(ActionEvent e) {
        log.debug("TODO onSaveProbe");
    }

    private void onSaveAsProbe(ActionEvent e) {
        log.debug("TODO onSaveAsProbe");
    }

    private void onOpenBlueprint(ActionEvent e) {
        log.debug("TODO onOpenBlueprint");
    }

    private void onSaveBlueprint(ActionEvent e) {
        log.debug("TODO onSaveBlueprint");
    }

    private void onStateChanged(ActionEvent e) {
        if (e.getSource() instanceof CodedButton button) {
            printMessage("set state " + button.code);
            log.debug("TODO onStateChanged");
        }
    }

    private void onCategoryChanged(ActionEvent e) {
        if (e.getSource() instanceof CodedButton button) {
            printMessage("set category " + button.code);
            log.debug("TODO onCategoryChanged");
        }
    }

    private void clearProbe(ActionEvent e) {
        clearProbe();
    }

    private void clearBlueprint(ActionEvent e) {
        log.debug("TODO clearBlueprint");
    }

    /*================*
     * event on probe *
     *================*/

    public void clearProbe() {
        var chmap = view.getChannelmap();
        if (chmap == null) {
            log.debug("clearProbe on nothing");
            return;
        }

        var code = probe.channelmapCode(chmap);
        if (code == null) {
            log.debug("clearProbe on unknown probe : {}", chmap.getClass().getSimpleName());
            return;
        }

        clearProbe(code);
    }

    public void clearProbe(String code) {
        log.debug("clearProbe on probe : {}", code);

        var chmap = probe.newChannelmap(code);
        view.setChannelmap(chmap);
    }

    /*=================*
     * event utilities *
     *=================*/

    private void checkBeforeClosing(Event e) {
        stage.close();
    }

    private void showAbout(ActionEvent e) {
        printMessage(List.of(
          "jNeuroCarto - version 0.0.0",
          "Author: XXX",
          "Github: XXX"
        ));
    }


    /*=============*
     * log message *
     *=============*/

    public void printMessage(String message) {
        log.info(message);

        var area = logMessageArea;
        if (area != null) {
            var content = area.getText();
            area.setText(message + "\n" + content);
        }
    }

    public void printMessage(@NonNull List<String> message) {
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

    private static final int[] GRIDDED_COMPONENT_WIDTH = {290, 140, 90, 30};

    private <T> void addAllIntoGridPane(GridPane layout, int column, List<T> data, Function<T, Region> factory) {
        if (column < 1) throw new IllegalArgumentException();

        for (int i = 0, size = data.size(); i < size; i++) {
            var node = factory.apply(data.get(i));
            if (column <= 4) {
                node.setPrefWidth(GRIDDED_COMPONENT_WIDTH[column - 1]);
            }
            layout.add(node, i % column, i / column);
        }
    }
}
