package io.ast.jneurocarto.javafx.app;

import io.ast.jneurocarto.config.Repository;
import io.ast.jneurocarto.config.cli.CartoConfig;
import javafx.stage.Stage;

public class Application {

    private final CartoConfig config;
    private final Repository repository;

    public Application(CartoConfig config) {
        this.config = config;
        repository = new Repository(config);
    }

    public void start(Stage stage) {
        stage.show();
    }
}
