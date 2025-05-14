package io.ast.jneurocarto.app.index;

import java.util.Arrays;

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
import io.ast.jneurocarto.core.ProbeDescription;

@Route("probe/:probe")
public class ProbeView extends Div implements HasDynamicTitle, BeforeEnterObserver, AfterNavigationObserver {

    private final Repository repository;
    private CartoUserConfig config;
    private ProbeDescription<?> probe;

    private final Logger log = LoggerFactory.getLogger(ProbeView.class);

    public ProbeView(Repository repository) {
        this.repository = repository;
        config = repository.getUserConfig();
        add(initLayout());
    }

    public static RouteParameters route(String probe) {
        return new RouteParameters("probe", probe);
    }

    @Override
    public String getPageTitle() {
        return repository.getTitle();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var family = event.getRouteParameters().get("probe").get();
        log.debug("rotate from : {}", family);
        probe = repository.getProbeDescription(family);
        if (probe == null) {
            log.warn("no probe {}", family);
            ProbeSelectView.navigate();
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        log.debug("set probe {}", probe.getClass().getSimpleName());
        updateStateButtonGroup(probe);
        updateCategoryButtonGroup(probe);
    }

    private Select<String> inputFilesSelect;
    private TextField outputImroFile;
    private TwoColumnVerticalLayout stateButtonGroup;
    private TwoColumnVerticalLayout categoryButtonGroup;
    private TextArea logMessageArea;

    private Component initLayout() {
        log.debug("index");
        var main = new HorizontalLayout();
        main.add(initLeftPanel());
        main.add(initCenterPanel());
        main.add(initRightPanel());
        return main;
    }


    private Component initLeftPanel() {
        log.debug("index left");

        inputFilesSelect = new Select<>(this::onInputFileSelectValueChange);
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

        var btnNew = new Button("New", this::onFileActionButtonPressed);
        var btnOpen = new Button("Open", this::onFileActionButtonPressed);
        var btnSave = new Button("Save", this::onFileActionButtonPressed);
        for (var btn : Arrays.asList(btnNew, btnOpen, btnSave)) {
            btn.setWidth(90, Unit.PIXELS);
        }

        var layout = new VerticalLayout(
          new Text("Input file"),
          inputFilesSelect,
          new HorizontalLayout(btnNew, btnOpen, btnSave),
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


    private void updateStateButtonGroup(ProbeDescription<?> probe) {
        stateButtonGroup.removeAll();
        probe.availableStates().forEach(state -> {
            var button = new Button(state, this::onStateButtonPressed);
            button.setWidth(140, Unit.PIXELS);
            stateButtonGroup.add(button);
        });
    }

    private void updateCategoryButtonGroup(ProbeDescription<?> probe) {
        categoryButtonGroup.removeAll();
        probe.availableCategories().forEach(state -> {
            var button = new Button(state, this::onCategoryButtonPressed);
            button.setWidth(140, Unit.PIXELS);
            categoryButtonGroup.add(button);
        });
    }

    private Component initCenterPanel() {
        var layout = new VerticalLayout();
        layout.add(new Div("Center"));
        return layout;
    }

    private Component initRightPanel() {
        var layout = new VerticalLayout();
        layout.add(new Div("Right"));
        return layout;
    }

    private void onInputFileSelectValueChange(AbstractField.ComponentValueChangeEvent<Select<String>, String> e) {
    }

    private void onFileActionButtonPressed(ClickEvent<Button> e) {
    }

    private void onStateButtonPressed(ClickEvent<Button> e) {

    }

    private void onCategoryButtonPressed(ClickEvent<Button> e) {

    }


}
