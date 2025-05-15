package io.ast.jneurocarto.app.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.ast.jneurocarto.app.cli.CartoConfig;

@Route("")
@PageTitle("jNeuroCarto")
public final class IndexView extends Div implements BeforeEnterObserver {

    private final CartoConfig config;
    private final Logger log = LoggerFactory.getLogger(IndexView.class);


    public IndexView(CartoConfig config) {
        this.config = config;
        log.debug("index");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (config.probeFamily == null) {
            log.debug("forward to ProbeSelectView");
            event.rerouteTo(ProbeSelectView.class);
        } else {
            log.debug("forward to ProbeView");
            event.rerouteTo(ProbeView.class, ProbeView.route(config.probeFamily));
        }
    }
}
