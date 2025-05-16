package io.ast.jneurocarto.app.index;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.ProbeProvider;

@Route("probe")
@PageTitle("jNeuroCarto")
public class ProbeSelectView extends Div implements BeforeEnterObserver, AfterNavigationObserver {

    private final Logger log = LoggerFactory.getLogger(ProbeSelectView.class);
    private boolean noneProbeFound;

    public ProbeSelectView() {
        add(initLayout());
    }

    public static void navigate() {
        UI.getCurrent().navigate(ProbeSelectView.class);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var families = ProbeDescription.listProbeDescriptions();
        if (families.isEmpty()) {
            noneProbeFound = true;
        } else if (families.size() == 1) {
            event.rerouteTo(ProbeView.class, ProbeView.route(families.get(0)));
            noneProbeFound = false;
        } else {
            noneProbeFound = false;
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        noProbeFound.setVisible(noneProbeFound);
        forProbeSection.setVisible(!noneProbeFound);
    }

    private Component noProbeFound;
    private Component forProbeSection;

    private Component initLayout() {
        log.debug("index");
        return new Div(
          noProbeFound = initForNoProbeFound(),
          forProbeSection = initForProbeSelection()
        );
    }

    private Component initForNoProbeFound() {
        return new VerticalLayout(
          new Div("No probe found. Please check the classpath")
        );
    }

    private Component initForProbeSelection() {
        return new VerticalLayout(
          new Div("Probes"),
          initProbeButtonGroup()
        );
    }

    private Component initProbeButtonGroup() {
        var layout = new HorizontalLayout();
        for (var provider : ServiceLoader.load(ProbeProvider.class)) {
            log.debug("found {}", provider.name());
            layout.add(initProbeButton(provider));
        }
        return layout;
    }

    private Component initProbeButton(ProbeProvider provider) {
        var ret = new Button(provider.name(), e -> {
            UI.getCurrent().navigate(ProbeView.class, ProbeView.route(provider.name()));
        });
        return ret;
    }
}
