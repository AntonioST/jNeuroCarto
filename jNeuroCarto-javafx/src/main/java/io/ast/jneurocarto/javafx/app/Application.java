package io.ast.jneurocarto.javafx.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.config.Repository;
import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.ProbeProviders;
import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.ProbePluginProvider;

public class Application<T> {

    private static Application<?> INSTANCE = null;
    private final CartoConfig config;
    private final Repository repository;
    private final ProbeDescription<T> probe;

    private final Logger log = LoggerFactory.getLogger(Application.class);

    public Application(CartoConfig config) {
        INSTANCE = this;
        this.config = config;
        repository = new Repository(config);
        probe = (ProbeDescription<T>) ProbeDescription.getProbeDescription(config.probeFamily);
        if (probe == null) throw new RuntimeException("probe " + config.probeFamily + " not found.");

        var method = config.probeSelector;
        if (method != null) selectMethod.set(method);
    }

    Application(CartoConfig config, ProbeDescription<T> probe) {
        INSTANCE = this;
        this.probe = probe;
        this.config = config;
        repository = new Repository(config);

        var method = config.probeSelector;
        if (method != null) selectMethod.set(method);
    }

    public static Application<?> getInstance() {
        return Objects.requireNonNull(INSTANCE, "Application is not initialized.");
    }

    public CartoConfig getConfig() {
        return config;
    }

    public Repository getRepository() {
        return repository;
    }

    public @Nullable T getChannelmap() {
        var view = this.view;
        if (view == null) return null;
        return view.getChannelmap();
    }

    public @Nullable List<ElectrodeDescription> getBlueprint() {
        var view = this.view;
        if (view == null) return null;
        return view.getBlueprint();
    }

    /*==========*
     * property *
     *==========*/

    private final ObjectProperty<@Nullable Path> currentChannelmapFile = new SimpleObjectProperty(null);

    public final ObjectProperty<@Nullable Path> currentChannelmapFileProperty() {
        return currentChannelmapFile;
    }

    public final @Nullable Path getCurrentChannelmapFile() {
        return currentChannelmapFile.get();
    }

    public final void setCurrentChannelmapFile(@Nullable Path path) {
        currentChannelmapFile.set(path);
    }

    private final BooleanProperty autoFresh = new SimpleBooleanProperty(true);

    public final BooleanProperty autoFreshProperty() {
        return autoFresh;
    }

    public final boolean isAutoFresh() {
        return autoFresh.get();
    }

    public void setAutoFresh(boolean value) {
        autoFresh.set(value);
    }

    private final StringProperty selectMethod = new SimpleStringProperty("default");
    private @Nullable String usedSelectMethod;
    private @Nullable ElectrodeSelector selector;

    public final StringProperty selectMethodProperty() {
        return selectMethod;
    }

    public final String getSelectMethod() {
        return selectMethod.get();
    }

    public void setSelectMethod(String method) {
        selectMethod.set(method);
    }

    @Nullable
    ElectrodeSelector getSelector(String method) {
        if (Objects.equals(usedSelectMethod, method)) {
            return Objects.requireNonNull(selector);
        } else {
            ElectrodeSelector selector;
            try {
                selector = probe.newElectrodeSelector(method);
            } catch (IllegalArgumentException e) {
                usedSelectMethod = null;
                this.selector = null;
                return null;
            }

            // TODO load selector options from user config
            this.selector = selector;
            usedSelectMethod = method;
            return selector;
        }
    }

    private final BooleanProperty isControlDown = new SimpleBooleanProperty();

    private void onControlDown(KeyEvent e) {
        if (e.getCode() == KeyCode.CONTROL) {
            isControlDown.set(e.getEventType() == KeyEvent.KEY_PRESSED);
        }
    }

    private void registerOnListonControlDown(Labeled node, @Nullable String append) {
        var title = node.getText();
        isControlDown.addListener((_, _, down) -> {
            if (down && append != null) {
                node.setText(title + append);
            } else {
                node.setText(title);
            }
        });
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

        currentChannelmapFile.addListener((_, _, _) -> updateTitle());
        setupPlugins();
        stage.sizeToScene();
    }

    private void updateTitle() {
        StringBuilder builder = new StringBuilder();
        builder.append("jNeuroCarto");
        builder.append(" - ");
        builder.append(config.probeFamily);

        var file = currentChannelmapFile.get();
        if (file != null) {
            builder.append(" - ").append(file);
        }
        stage.setTitle(builder.toString());
    }

    private Scene scene() {
        var scene = new Scene(root());

        scene.setOnKeyPressed(this::onKeyPressed);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onControlDown);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::onControlDown);

        return scene;
    }

    private Parent root() {
        log.debug("init layout");

        var menubar = rootMenu();
        var left = rootLeft();
        var center = rootCenter();
        var right = rootRight();

        var content = new HBox(left, center, right);
        HBox.setHgrow(right, Priority.ALWAYS);

        var root = new VBox(menubar, content);
        root.setFillWidth(true);

        return root;
    }

    /*==========*
     * menu bar *
     *==========*/

    private static class CodedMenuItem extends MenuItem {
        final String code;

        CodedMenuItem(String code, String title, EventHandler<ActionEvent> callback) {
            super(title);
            this.code = code;
            setOnAction(callback);
        }
    }

    private static class CodedButton extends Button {
        final String code;

        CodedButton(String code, EventHandler<ActionEvent> callback) {
            this(code, code, callback);
        }

        CodedButton(String code, String title, EventHandler<ActionEvent> callback) {
            super(title);
            this.code = code;
            setOnAction(callback);
        }
    }

    private sealed interface PluginMenuItem permits PluginSeparatorMenuItem, ProbePluginSeparatorMenuItem {
    }

    private static final class PluginSeparatorMenuItem extends SeparatorMenuItem implements PluginMenuItem {
    }

    private static final class ProbePluginSeparatorMenuItem extends SeparatorMenuItem implements PluginMenuItem {
    }

    MenuBar menuBar;
    Menu menuFile;
    Menu menuEdit;
    Menu menuView;
    Menu menuHelp;

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

        var newProbeMenu = new Menu("_New");
        var newProbeMenuItems = probe.supportedProbeType().stream()
          .map(code -> new CodedMenuItem(code, probe.probeTypeDescription(code), this::onNewChannelmap))
          .toList();
        newProbeMenu.getItems().addAll(newProbeMenuItems);

        var open = new MenuItem("_Open");
        open.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        open.setOnAction(this::onLoadChannelmap);

        var save = new MenuItem("_Save");
        save.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        save.setOnAction(this::onSaveChannelmap);

        var saveAs = new MenuItem("Save _As");
        saveAs.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+S"));
        saveAs.setOnAction(this::onSaveAsChannelmap);

        var loadBlueprint = new MenuItem("Open _Blueprint");
        loadBlueprint.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+O"));
        loadBlueprint.setOnAction(this::onOpenBlueprint);

        var saveBlueprint = new MenuItem("Save _Blueprint");
        saveBlueprint.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+S"));
        saveBlueprint.setOnAction(this::onSaveBlueprint);

        var otherProbeMenu = new Menu("Open Other Probe _Family");
        var otherProbeMenuItems = ProbeDescription.listProbeDescriptions().stream()
          .map(family -> {
              var title = ProbeProviders.getProbeDescriptionText(family);
              return new CodedMenuItem(family, title, this::openOtherProbeFamily);
          })
          .toList();
        otherProbeMenu.getItems().addAll(otherProbeMenuItems);

        var exit = new MenuItem("_Exit");
        exit.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        exit.setOnAction(this::checkBeforeClosing);

        menuFile.getItems().addAll(
          newProbeMenu,
          open,
          save,
          saveAs,
          new SeparatorMenuItem(),
          loadBlueprint,
          saveBlueprint,
          new SeparatorMenuItem(),
          otherProbeMenu,
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
        clearBlueprint.setOnAction(this::clearBlueprint);

        var refresh = new MenuItem("_Refresh selection");
        refresh.setAccelerator(KeyCombination.keyCombination("Shortcut+R"));
        refresh.setOnAction(this::refreshSelection);

        var autoFresh = new CheckMenuItem("Auto fresh selection");
        autoFresh.selectedProperty().bindBidirectional(this.autoFresh);

        var selectMethodMenu = new Menu("Selection method");
        var selectMethodMenuItems = probe.getElectrodeSelectors().stream()
          .map(method -> {
              var item = new CheckMenuItem(method);
              item.selectedProperty().bind(selectMethod.isEqualTo(method));
              item.setOnAction(e -> selectMethod.set(method));
              return item;
          }).toList();
        selectMethodMenu.getItems().addAll(selectMethodMenuItems);

        var editUserConfig = new MenuItem("Edit user config");

        menuEdit.getItems().addAll(
          clear,
          clearBlueprint,
          new SeparatorMenuItem(),
          refresh,
          selectMethodMenu,
          autoFresh,
          new SeparatorMenuItem(),
          new PluginSeparatorMenuItem(),
          new ProbePluginSeparatorMenuItem(),
          editUserConfig
        );

        return menuEdit;
    }

    private Menu menuView() {
        menuView = new Menu("_View");

        var resetProbeViewAxes = new MenuItem("R_eset View");
        resetProbeViewAxes.setAccelerator(KeyCombination.keyCombination("Shortcut+E"));
        resetProbeViewAxes.setOnAction(_ -> view.fitAxesBoundaries());

        var resetProbeViewAxesRatio = new MenuItem("Reset View Ratio");
        resetProbeViewAxesRatio.setAccelerator(KeyCombination.keyCombination("Shortcut+Alt+E"));
        resetProbeViewAxesRatio.setOnAction(_ -> view.setAxesEqualRatio());

        var clearLog = new MenuItem("Clear _log");
        clearLog.setAccelerator(KeyCombination.keyCombination("Shortcut+L"));
        clearLog.setOnAction(_ -> clearMessages());

        menuView.getItems().addAll(
          resetProbeViewAxes,
          resetProbeViewAxesRatio,
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

    int findMenuItemIndex(Menu menu, boolean isProbePlugin) {
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

    private class ShortcutCodedButton extends CodedButton {

        final String title;
        final int shortcut;
        final boolean withAlt;

        ShortcutCodedButton(String code, String title, int shortcut, boolean withAlt, EventHandler<ActionEvent> callback) {
            super(code, title, callback);
            this.title = title;
            this.shortcut = shortcut;
            this.withAlt = withAlt;

            if (shortcut <= 10) {
                var combine = "Shortcut+" + (withAlt ? "Alt+" : "") + (shortcut % 10);
                setOnKeyCombine(combine, _ -> fire());
                registerOnListonControlDown(this, " (" + shortcut + ")");
            }

        }
    }


    VBox viewLayout;
    VBox pluginLayout;
    ProbeView<T> view;

    private Parent rootLeft() {
        log.debug("init layout - left");

        var stateLabel = new Label("State");
        registerOnListonControlDown(stateLabel, " (Ctrl+Alt+...)");

        var categoryLabel = new Label("Category");
        registerOnListonControlDown(categoryLabel, " (Ctrl+)");

        var layoutState = new GridPane();
        layoutState.setHgap(5);
        layoutState.setVgap(5);
        addAllIntoGridPane(layoutState, 2, probe.availableStates(), (i, state) -> {
            var shortcut = i + 1;
            return new ShortcutCodedButton(state, state, shortcut, true, this::onStateChanged);
        });

        var layoutCate = new GridPane();
        layoutCate.setHgap(5);
        layoutCate.setVgap(5);
        addAllIntoGridPane(layoutCate, 2, probe.availableCategories(), (i, category) -> {
            var shortcut = i + 1;
            return new ShortcutCodedButton(category, category, shortcut, false, this::onCategoryChanged);
        });

        logMessageArea = new TextArea();
        logMessageArea.setEditable(false);
        logMessageArea.setPrefWidth(290);
        logMessageArea.setPrefColumnCount(100);

        var root = new VBox(
          stateLabel,
          layoutState,
          categoryLabel,
          layoutCate,
          new Label("Log"),
          logMessageArea
        );

        root.setMinWidth(300);
        root.setMaxWidth(300);
        root.setSpacing(5);
        root.setPadding(new Insets(5, 5, 5, 5));
        return root;
    }

    private Parent rootCenter() {
        log.debug("init layout - center");

        view = new ProbeView<>(config, probe);
        view.setMinWidth(700);
        view.setMinHeight(800);

        viewLayout = new VBox(
          view
        );
        return viewLayout;
    }

    private Parent rootRight() {
        log.debug("init layout - right");

        var root = new VBox();
        root.setMinWidth(600);
        root.setSpacing(5);
        root.setPadding(new Insets(15, 5, 5, 5));

        pluginLayout = root;
        return root;
    }

    /*=========*
     * plugins *
     *=========*/

    final List<Plugin> plugins = new ArrayList<>();

    private void setupPlugins() {
        log.debug("setup plugins");

        var service = new PluginSetupService(this);

        for (var provider : ServiceLoader.load(ProbePluginProvider.class)) {
            for (var plugin : provider.setup(config, probe)) {
                plugins.add(plugin);
                log.debug("add plugin : {}", plugin.getClass().getName());

                var node = plugin.setup(service);
                if (node != null) {
                    pluginLayout.getChildren().add(node);
                }

            }
        }

        var extra = new ArrayList<String>();
        var rc = repository.getUserConfig();
        if (rc.views == null) {
            extra.add("neurocarto.views.data_density:ElectrodeDensityDataView");
            extra.add("neurocarto.views.view_efficient:ElectrodeEfficiencyData");
            extra.add("blueprint");
            extra.add("atlas");
        } else {
            extra.addAll(rc.views);
        }
        extra.addAll(config.extraViewList);

        var foundPlugins = new HashMap<String, PluginProvider>();
        for (var provider : ServiceLoader.load(PluginProvider.class)) {
            // always put provider class name
            var providerName = provider.getClass().getName();
            foundPlugins.put(providerName, provider);

            // and plugin name
            if (providerName.endsWith("Provider")) {
                foundPlugins.put(providerName.replaceFirst("Provider$", ""), provider);
            }

            var nameList = provider.name();
            if (nameList.isEmpty()) {
                log.debug("found provider {}.", providerName);
            } else {
                log.debug("found provider {} provides {}", providerName, nameList);
                for (var name : nameList) {
                    var old = foundPlugins.putIfAbsent(name, provider);
                    if (old != null) {
                        log.warn("{} conflict with : {}", name, old.getClass().getName());
                    }
                }
            }
        }

        for (int i = 0; i < extra.size(); i++) {
            var name = extra.get(i);
            var provider = foundPlugins.get(name);
            if (provider != null) {
                extra.remove(i--);

                log.debug("use plugin {}", name);
                var plugin = provider.setup(config, probe);
                log.debug("add plugin {}", plugin.getClass().getName());
                plugins.add(plugin);

                var node = plugin.setup(service);
                if (node != null) {
                    pluginLayout.getChildren().add(node);
                }
            }
        }
    }

    /*================*
     * event handlers *
     *================*/

    private void onNewChannelmap(ActionEvent e) {
        if (e.getSource() instanceof CodedMenuItem item) {
            printMessage("new channelmap " + item.code);
            clearProbe(item.code);
        }
    }

    private void onLoadChannelmap(ActionEvent e) {
        log.debug("onLoadChannelmap");

        var channelmapFile = openChannelmapFileDialog(false);
        if (channelmapFile != null) {
            currentChannelmapFile.set(channelmapFile);
            onLoadChannelmap(channelmapFile);
        }
    }

    private void onLoadChannelmap(Path channelmapFile) {
        T channelmap;
        try {
            channelmap = loadChannelmap(channelmapFile);
        } catch (IOException ex) {
            printMessage("fail to open channelmap file " + channelmapFile.getFileName());
            log.warn("loadProbe", ex);
            return;
        }

        List<ElectrodeDescription> blueprint;
        try {
            blueprint = loadBlueprint(channelmapFile);
        } catch (IOException ex) {
            printMessage("fail to load blueprint file.");
            log.warn("loadBlueprint", ex);
            blueprint = Objects.requireNonNull(view.resetBlueprint());
        }

        try {
            loadPluginViewConfig(channelmapFile);
        } catch (IOException ex) {
            printMessage("fail to load view config file.");
            log.warn("loadPluginViewConfig", ex);
        }

        fireProbeUpdate(channelmap, blueprint);
    }

    private void onSaveChannelmap(ActionEvent e) {
        var channelmap = getChannelmap();
        if (channelmap == null) return;

        log.debug("onSaveProbe");
        var channelmapFile = currentChannelmapFile.get();
        if (channelmapFile == null) {
            channelmapFile = openChannelmapFileDialog(true);
        }
        if (channelmapFile != null) {
            channelmapFile = repository.getChannelmapFile(probe, channelmapFile);
            currentChannelmapFile.set(channelmapFile);
            onSaveChannelmap(channelmapFile, channelmap);
        }
    }

    private void onSaveAsChannelmap(ActionEvent e) {
        var channelmap = getChannelmap();
        if (channelmap == null) return;

        log.debug("onSaveAsChannelmap");
        var channelmapFile = openChannelmapFileDialog(true);
        if (channelmapFile != null) {
            channelmapFile = repository.getChannelmapFile(probe, channelmapFile);
            currentChannelmapFile.set(channelmapFile);
            onSaveChannelmap(channelmapFile, channelmap);
        }
    }

    private void onSaveChannelmap(Path channelmapFile, T channelmap) {
        if (!probe.validateChannelmap(channelmap)) {
            printMessage("incomplete channelmap");

            if (!repository.getUserConfig().alwaysSaveBlueprintFile) {
                return;
            }
        } else {
            try {
                log.debug("saveChannelmap {}", channelmapFile.getFileName());
                repository.saveChannelmapFile(probe, channelmap, channelmapFile);
            } catch (IOException e) {
                printMessage("fail to save channelmap file " + channelmapFile.getFileName());
                log.warn("saveChannelmapFile", e);
                return;
            }
        }

        var blueprint = getBlueprint();
        if (blueprint != null) {
            try {
                saveBlueprint(channelmapFile, blueprint);
            } catch (IOException e) {
                printMessage("fail to save blueprint file.");
                log.warn("saveBlueprint", e);
            }
        }

        try {
            savePluginViewConfig(channelmapFile);
        } catch (IOException e) {
            printMessage("fail to save view config file.");
            log.warn("savePluginViewConfig", e);
        }
    }


    private void onOpenBlueprint(ActionEvent e) {
        log.debug("onOpenBlueprint");

        var blueprintFile = openFileDialog("Blueprint", false, "*.blueprint.npy", null);
        if (blueprintFile != null) {
            try {
                loadBlueprint(blueprintFile);
            } catch (IOException ex) {
                printMessage("fail to load blueprint file.");
                log.warn("loadBlueprint", ex);
                return;
            }

            fireProbeUpdate();
        }
    }

    private void onSaveBlueprint(ActionEvent e) {
        var blueprint = getBlueprint();
        if (blueprint == null) return;

        log.debug("onSaveBlueprint");

        var blueprintFile = currentChannelmapFile.get();
        if (blueprintFile != null) blueprintFile = repository.getBlueprintFile(probe, blueprintFile);
        blueprintFile = openFileDialog("Blueprint", true, "*.blueprint.npy", blueprintFile);
        if (blueprintFile != null) {
            try {
                saveBlueprint(blueprintFile, blueprint);
            } catch (IOException ex) {
                printMessage("fail to save blueprint file.");
                log.warn("saveBlueprint", ex);
            }
        }
    }

    private @Nullable Path openChannelmapFileDialog(boolean save) {
        var chooser = new FileChooser();
        chooser.setTitle((save ? "Save" : "Load") + " Channelmap file");
        chooser.setInitialDirectory(repository.currentResourceRoot().toFile());

        var suffixes = probe.channelMapFileSuffix();
        var exts = suffixes.stream()
          .map(it -> "*" + it)
          .toList();
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("channelmap file", exts));

        if (save) {
            var channelmapFile = currentChannelmapFile.get();
            if (channelmapFile == null) {
                var suffix = suffixes.get(0);
                chooser.setInitialFileName("New" + suffix);
            } else {
                var filename = repository.getChannelmapName(probe, channelmapFile.getFileName().toString());
                chooser.setInitialFileName(filename);
            }
        }

        File file;
        if (save) {
            file = chooser.showSaveDialog(stage);
        } else {
            file = chooser.showOpenDialog(stage);
        }

        if (file == null) {
            log.debug("onLoadProbe (canceled)");
            return null;
        }
        return file.toPath();
    }

    private @Nullable Path openFileDialog(String description, boolean save, @Nullable String suffix, @Nullable Path initFile) {
        var chooser = new FileChooser();
        chooser.setTitle((save ? "Save" : "Load") + " " + description + " file");
        chooser.setInitialDirectory(repository.currentResourceRoot().toFile());

        if (suffix != null) {
            chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(description, suffix));
        }

        if (save && initFile != null) {
            chooser.setInitialFileName(initFile.getFileName().toString());
        }

        File file;
        if (save) {
            file = chooser.showSaveDialog(stage);
        } else {
            file = chooser.showOpenDialog(stage);
        }

        if (file == null) {
            log.debug("openFileDialog (canceled)");
            return null;
        }
        return file.toPath();
    }

    private void onStateChanged(ActionEvent e) {
        if (e.getSource() instanceof CodedButton button) {
            printMessage("set state " + button.code);
            var state = probe.stateOf(button.code);
            state.ifPresent(view::setStateForCaptured);
            if (state.isPresent()) fireProbeUpdate();
        }
    }

    private void onCategoryChanged(ActionEvent e) {
        if (e.getSource() instanceof CodedButton button) {
            // TODO selectedAsPreSelected
            printMessage("set category " + button.code);
            var cate = probe.categoryOf(button.code);
            cate.ifPresent(view::setCategoryForCaptured);
            if (cate.isPresent()) {
                if (autoFresh.get()) {
                    refreshSelection(e);
                } else {
                    fireProbeUpdate();
                }
            }
        }
    }

    private void refreshSelection(ActionEvent e) {
        refreshSelection(selectMethod.get());
    }

    private void clearProbe(ActionEvent e) {
        clearProbe();
    }

    private void clearBlueprint(ActionEvent e) {
        clearBlueprint();
    }

    private void openOtherProbeFamily(ActionEvent e) {
        if (e.getSource() instanceof CodedMenuItem item) {
            log.info("open other probe family : {}", item.code);
            var probe = ProbeDescription.getProbeDescription(item.code);
            if (probe == null) {
                printMessage("probe family " + item.code + " not found.");
                return;
            }

            checkBeforeClosing(e);

            var application = new Application<>(config, probe);
            var stage = new Stage();
            application.start(stage);
        }
    }

    private final Map<KeyCombination, EventHandler<KeyEvent>> keyCombineHandlers = new HashMap<>();

    void setOnKeyCombine(String combine, EventHandler<KeyEvent> handler) {
        setOnKeyCombine(KeyCombination.keyCombination(combine), handler);
    }

    void setOnKeyCombine(KeyCombination combine, EventHandler<KeyEvent> handler) {
        keyCombineHandlers.put(combine, handler);
    }

    private void onKeyPressed(KeyEvent e) {
        for (var entry : keyCombineHandlers.entrySet()) {
            if (entry.getKey().match(e)) {
                entry.getValue().handle(e);
                if (e.isConsumed()) break;
            }
        }
    }

    /*================*
     * event on probe *
     *================*/

    public T loadChannelmap(String name) throws IOException {
        return loadChannelmap(repository.getChannelmapFile(probe, name));
    }

    public T loadChannelmap(Path channelmapFile) throws IOException {
        if (!Files.exists(channelmapFile)) throw new FileNotFoundException(channelmapFile.toString());

        log.debug("changeResourceRoot {}", channelmapFile.getParent());
        repository.changeResourceRoot(channelmapFile.getParent());

        log.debug("loadChannelmap {}", channelmapFile.getFileName());
        var chmap = repository.loadChannelmapFile(probe, channelmapFile);
        view.setChannelmap(chmap);
        view.resetBlueprint();
        view.fitAxesBoundaries();
        return chmap;
    }

    public void saveChannelmap(Path channelmapFile) throws IOException {
        var channelmap = getChannelmap();
        if (channelmap == null) throw new IOException("nothing to save");
        saveChannelmap(channelmapFile, channelmap);
    }

    public void saveChannelmap(Path channelmapFile, T channelmap) throws IOException {
        if (!probe.validateChannelmap(channelmap)) {
            throw new IOException("bad channelmap state.");
        }

        log.debug("saveChannelmap {}", channelmapFile.getFileName());
        repository.saveChannelmapFile(probe, channelmap, channelmapFile);
    }

    public List<ElectrodeDescription> loadBlueprint(Path channelmapFile) throws IOException {
        var blueprintFile = repository.getBlueprintFile(probe, channelmapFile);
        if (!Files.exists(blueprintFile)) throw new FileNotFoundException(blueprintFile.toString());

        log.debug("loadBlueprint {}", blueprintFile.getFileName());
        var blueprint = repository.loadBlueprintFile(probe, blueprintFile);
        view.setBlueprint(blueprint);
        return blueprint;
    }

    public void saveBlueprint(Path channelmapFile) throws IOException {
        var blueprint = getBlueprint();
        if (blueprint == null) throw new IOException("nothing to save");
        saveBlueprint(channelmapFile, blueprint);
    }

    public void saveBlueprint(Path channelmapFile, List<ElectrodeDescription> blueprint) throws IOException {
        var blueprintFile = repository.getBlueprintFile(probe, channelmapFile);

        log.debug("saveBlueprint {}", blueprintFile.getFileName());
        repository.saveBlueprintFile(probe, blueprint, blueprintFile);
    }

    public void loadPluginViewConfig(Path channelmapFile) throws IOException {
        var viewConfigFile = repository.getViewConfigFile(probe, channelmapFile);
        if (!Files.exists(viewConfigFile)) throw new FileNotFoundException(viewConfigFile.toString());

        log.debug("loadPluginViewConfig {}", viewConfigFile.getFileName());
        repository.loadViewConfigFile(probe, viewConfigFile, true);

        log.debug("retrieveAllStates");
        PluginStateService.retrieveAllStates();
    }

    public void savePluginViewConfig(Path channelmapFile) throws IOException {
        var viewConfigFile = repository.getViewConfigFile(probe, channelmapFile);

        log.debug("saveAllStates");
        PluginStateService.saveAllStates();

        log.debug("savePluginViewConfig {}", viewConfigFile.getFileName());
        repository.saveViewConfigFile(probe, viewConfigFile);
    }

    public void refreshSelection() {
        refreshSelection(selectMethod.get());
    }

    public void refreshSelection(String method) {
        var chmap = getChannelmap();
        if (chmap == null) return;

        var blueprint = getBlueprint();
        if (blueprint == null) return;

        var selector = getSelector(method);
        if (selector == null) {
            printMessage("cannot found selector : " + method);
            return;
        }

        T newMap;
        try {
            newMap = selector.select(probe, chmap, blueprint);
        } catch (RuntimeException ex) {
            printMessage("selection fail.");
            log.warn("refreshSelection", ex);
            return;
        }

        view.setChannelmap(newMap);
        view.resetElectrodeState();
        fireProbeUpdate(newMap, view.getBlueprint());
    }

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
        log.debug("clearProbe for : {}", code);

        var chmap = probe.newChannelmap(code);
        view.setChannelmap(chmap);
        view.resetBlueprint();
        view.fitAxesBoundaries();
        fireProbeUpdate();
    }

    public void clearBlueprint() {
        log.debug("clearBlueprint");

        view.resetBlueprint();
        fireProbeUpdate();
    }

    private void fireProbeUpdate() {
        var channelmap = view.getChannelmap();
        if (channelmap == null) return;

        var blueprint = view.getBlueprint();
        if (blueprint == null) return;

        fireProbeUpdate(channelmap, blueprint);
    }

    public void fireProbeUpdate(T chmap, List<ElectrodeDescription> blueprint) {
        log.debug("onProbeUpdate");

        view.updateElectrode();

        for (var plugin : plugins) {
            if (plugin instanceof ProbePlugin<?> p) {
                log.debug("onProbeUpdate for {}", p.getClass().getSimpleName());
                ((ProbePlugin<T>) p).onProbeUpdate(chmap, blueprint);
            }
        }
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

    void printMessage(String message) {
        log.info(message);

        var area = logMessageArea;
        if (area != null) {
            var content = area.getText();
            var updated = message + "\n" + content;
            if (Platform.isFxApplicationThread()) {
                area.setText(updated);
            } else {
                Platform.runLater(() -> area.setText(updated));
            }
        }
    }

    void printMessage(@NonNull List<String> message) {
        if (log.isInfoEnabled()) {
            message.forEach(log::info);
        }

        var area = logMessageArea;
        if (area != null) {
            var content = area.getText();
            var updated = Stream.concat(message.stream(), Stream.of(content))
              .collect(Collectors.joining("\n"));
            if (Platform.isFxApplicationThread()) {
                area.setText(updated);
            } else {
                Platform.runLater(() -> area.setText(updated));
            }
        }
    }

    void clearMessages() {
        var area = logMessageArea;
        if (area != null) {
            if (Platform.isFxApplicationThread()) {
                area.setText("");
            } else {
                Platform.runLater(() -> area.setText(""));
            }
        }
    }

    /*==================*
     * layout utilities *
     *==================*/

    private static final int[] GRIDDED_COMPONENT_WIDTH = {290, 140, 90, 30};

    private <T, N extends Region> List<N> addAllIntoGridPane(GridPane layout, int column, List<T> data, BiFunction<Integer, T, N> factory) {
        if (column < 1) throw new IllegalArgumentException();

        var ret = new ArrayList<N>();
        for (int i = 0, size = data.size(); i < size; i++) {
            var node = factory.apply(i, data.get(i));
            if (column <= 4) {
                node.setPrefWidth(GRIDDED_COMPONENT_WIDTH[column - 1]);
            }
            layout.add(node, i % column, i / column);
            ret.add(node);
        }

        return ret;
    }
}
