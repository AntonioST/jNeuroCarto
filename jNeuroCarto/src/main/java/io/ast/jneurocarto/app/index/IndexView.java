package io.ast.jneurocarto.app.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import io.ast.jneurocarto.app.CartoUserConfig;
import io.ast.jneurocarto.app.Repository;

@Route("")
public final class IndexView extends Main implements HasDynamicTitle {

    private final Repository repository;
    private final CartoUserConfig config;

    private final Logger log = LoggerFactory.getLogger(IndexView.class);

    public IndexView(Repository repository) {
        this.repository = repository;

        log.debug("index");
        this.config = repository.getUserConfig();
    }

    @Override
    public String getPageTitle() {
        return repository.getTitle();
    }
}
