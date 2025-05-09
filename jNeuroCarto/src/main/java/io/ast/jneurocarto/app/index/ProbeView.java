package io.ast.jneurocarto.app.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;

import io.ast.jneurocarto.app.CartoUserConfig;
import io.ast.jneurocarto.app.Repository;
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
    }

    private Select<String> inputFilesSelect;
    private TextField outputImroFile;

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
        inputFilesSelect = new Select<>("Input file", this::onInputFileSelectValueChange);
        outputImroFile = new TextField("Save filename");
//        var stateButtons = initStateButtonGroup();

        var layout = new VerticalLayout();
        layout.add(inputFilesSelect);
        layout.add(outputImroFile);
        return layout;
    }

//    private Component initStateButtonGroup() {
//
//    }

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


}
