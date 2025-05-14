package io.ast.jneurocarto.app.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;

import io.ast.jneurocarto.app.CartoUserConfig;
import io.ast.jneurocarto.app.Repository;
import io.ast.jneurocarto.app.base.TwoColumnVerticalLayout;
import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;

@Route("probe/:probe")
@NullUnmarked
public class ProbeView<T> extends Div
  implements HasDynamicTitle, BeforeEnterObserver, AfterNavigationObserver, LogMessageService.LogMessageProvider {

    private final @NonNull Repository repository;
    private @NonNull CartoUserConfig config;
    private ProbeDescription<T> probe;

    private final Logger log = LoggerFactory.getLogger(ProbeView.class);

    public ProbeView(@NonNull Repository repository,
                     @NonNull LogMessageService logMessageService) {
        this.repository = repository;
        config = repository.getUserConfig();
        add(initLayout());

        logMessageService.register(this);
    }

    public static @NonNull RouteParameters route(@NonNull String probe) {
        return new RouteParameters("probe", probe);
    }

    @Override
    public @NonNull String getPageTitle() {
        return repository.getTitle();
    }

    @Override
    public void beforeEnter(@NonNull BeforeEnterEvent event) {
        var family = event.getRouteParameters().get("probe").get();
        log.debug("rotate from : {}", family);
        probe = (ProbeDescription<T>) repository.getProbeDescription(family);
        if (probe == null) {
            log.warn("no probe {}", family);
            ProbeSelectView.navigate();
        }
    }

    @Override
    public void afterNavigation(@NonNull AfterNavigationEvent event) {
        clearMessages();
        log.debug("set probe {}", probe.getClass().getSimpleName());

        updateProbeTypeList(probe);
        updateStateButtonGroup(probe);
        updateCategoryButtonGroup(probe);
        updateChannelMapFileList(probe);
    }

    private Select<String> inputFilesSelect;
    private Select<String> newProbeSelect;
    private TextField outputImroFile;
    private TwoColumnVerticalLayout stateButtonGroup;
    private TwoColumnVerticalLayout categoryButtonGroup;
    private TextArea logMessageArea;

    private @NonNull Component initLayout() {
        log.debug("index");
        var main = new HorizontalLayout();
        main.add(initLeftPanel());
        main.add(initCenterPanel());
        main.add(initRightPanel());
        return main;
    }

    private @NonNull Component initLeftPanel() {
        log.debug("index left");

        inputFilesSelect = new Select<>();
        inputFilesSelect.setWidth(290, Unit.PIXELS);

        outputImroFile = new TextField();
        outputImroFile.setWidth(290, Unit.PIXELS);

        stateButtonGroup = new TwoColumnVerticalLayout();
        stateButtonGroup.setWidth(290, Unit.PIXELS);
        stateButtonGroup.setPadding(false);

        categoryButtonGroup = new TwoColumnVerticalLayout();
        categoryButtonGroup.setWidth(290, Unit.PIXELS);
        categoryButtonGroup.setPadding(false);

        logMessageArea = new TextArea();
        logMessageArea.setMaxLength(100);
        logMessageArea.setWidth(290, Unit.PIXELS);
        logMessageArea.setEnabled(false);

        newProbeSelect = new Select<>(this::onNewProbe);
        newProbeSelect.setPlaceholder("New");
        newProbeSelect.setEmptySelectionAllowed(true);
        newProbeSelect.setItemLabelGenerator(probe::probeTypeDescription);

        var btnOpen = new Button("Open", this::onLoadProbe);
        var btnSave = new Button("Save", this::onSaveProbe);
        for (HasSize btn : Arrays.asList(newProbeSelect, btnOpen, btnSave)) {
            btn.setWidth(90, Unit.PIXELS);
        }

        var layout = new VerticalLayout(
          new Text("Input file"),
          inputFilesSelect,
          new HorizontalLayout(newProbeSelect, btnOpen, btnSave),
          new Text("Save filename"),
          outputImroFile,
          new Text("State"),
          stateButtonGroup,
          new Text("Category"),
          categoryButtonGroup,
          new Text("Log"),
          logMessageArea
        );
        layout.setSpacing(5, Unit.PIXELS);
        layout.setWidth(300, Unit.PIXELS);

        return layout;
    }

    private void updateProbeTypeList(@NonNull ProbeDescription<?> probe) {
        newProbeSelect.setItems(probe.supportedProbeType());
    }

    private void updateChannelMapFileList() {
        var probe = this.probe;
        if (probe != null) {
            updateChannelMapFileList();
        }
    }

    private void updateChannelMapFileList(@NonNull ProbeDescription<?> probe) {
        updateChannelMapFileList(probe, inputFilesSelect.getValue());
    }

    private void updateChannelMapFileList(@NonNull ProbeDescription<?> probe, String preSelectFile) {
        printMessage("update channelmap file list");

        List<Path> files;
        try {
            files = repository.listChannelmapFiles(probe, false);
        } catch (IOException e) {
            printMessage("fail to update.");
            log.warn("updateChannelMapFileList", e);
            return;
        }

        var items = files.stream()
          .map(Path::getFileName)
          .map(Path::toString)
          .toList();

        inputFilesSelect.setItems(items);
        if (preSelectFile != null && items.contains(preSelectFile)) {
            inputFilesSelect.setValue(preSelectFile);
        }
    }

    private void updateStateButtonGroup(@NonNull ProbeDescription<?> probe) {
        stateButtonGroup.removeAll();
        probe.availableStates().forEach(state -> {
            var button = new Button(state, this::onStateButtonPressed);
            button.setWidth(140, Unit.PIXELS);
            stateButtonGroup.add(button);
        });
    }

    private void updateCategoryButtonGroup(@NonNull ProbeDescription<?> probe) {
        categoryButtonGroup.removeAll();
        probe.availableCategories().forEach(state -> {
            var button = new Button(state, this::onCategoryButtonPressed);
            button.setWidth(140, Unit.PIXELS);
            categoryButtonGroup.add(button);
        });
    }

    private @NonNull Component initCenterPanel() {
        var layout = new VerticalLayout();
        layout.add(new Div("Center"));
        return layout;
    }

    private @NonNull Component initRightPanel() {
        var layout = new VerticalLayout();
        layout.add(new Div("Right"));
        return layout;
    }

    private void onNewProbe(AbstractField.ComponentValueChangeEvent<Select<String>, String> e) {
        var code = e.getValue();
        printMessage("new probe " + code);

    }

    private void onLoadProbe(@NonNull ClickEvent<Button> e) {
        var filename = inputFilesSelect.getValue();
        if (filename.isEmpty()) return;
        loadProbe(filename);
    }

    private void loadProbe(String filename) {
        var probe = this.probe;
        if (probe == null) return;

        printMessage("load " + filename);

        // load channelmap file
        var chmapFile = repository.getChannelmapFile(probe, filename);
        T chmap;

        try {
            log.debug("load {}", chmapFile);
            chmap = repository.loadChannelmapFile(probe, chmapFile);
        } catch (IOException e) {
            printMessage("chmap file not exist.");
            log.warn("loadProbe(chmap)", e);
            updateChannelMapFileList(probe);
            return;
        }

        // update save filename
        Path saveFile;
        if (config.overwriteChmapFile) {
            saveFile = chmapFile;
        } else {
            saveFile = repository.saveChannelmapFilename(probe, chmapFile);
        }
        outputImroFile.setValue(saveFile.getFileName().toString());

        // load blueprint file
        var blueprintFile = repository.getBlueprintFile(probe, chmapFile);
        List<ElectrodeDescription> blueprint;
        try {
            log.debug("load {}", blueprintFile);
            blueprint = repository.loadBlueprintFile(probe, blueprintFile);
        } catch (IOException e) {
            printMessage("blueprint file not exist.");
            log.warn("loadProbe(blueprint)", e);
            return;
        }

        // update
        //TODO Unsupported Operation ProbeView.loadProbe
        System.out.println(chmap);
        System.out.println(blueprint);

    }

    private void onSaveProbe(@NonNull ClickEvent<Button> e) {
        var filename = outputImroFile.getValue();
        if (filename.isEmpty()) {
            printMessage("empty output filename");
            return;
        }

        saveProbe(filename);
    }

    private void saveProbe(String filename) {
        var probe = this.probe;
        if (probe == null) return;

        printMessage("save " + filename);

        var chmapFile = repository.getChannelmapFile(probe, filename);

        //TODO Unsupported Operation ProbeView.saveProbe
        System.out.println(chmapFile);
    }


    private void onStateButtonPressed(@NonNull ClickEvent<Button> e) {

    }

    private void onCategoryButtonPressed(@NonNull ClickEvent<Button> e) {

    }

    @Override
    public void clearMessages() {
        var area = logMessageArea;
        if (area != null) {
            area.setValue("");
        }
    }

    @Override
    public void printMessage(@NonNull String message) {
        log.info(message);

        var area = logMessageArea;
        if (area != null) {
            var content = area.getValue();
            area.setValue(message + "\n" + content);
        }
    }

    @Override
    public void printMessage(@NonNull List<String> message) {
        if (log.isInfoEnabled()) {
            message.forEach(log::info);
        }

        var area = logMessageArea;
        if (area != null) {
            var content = area.getValue();
            var updated = Stream.concat(message.stream(), Stream.of(content))
              .collect(Collectors.joining("\n"));
            area.setValue(updated);
        }
    }
}
