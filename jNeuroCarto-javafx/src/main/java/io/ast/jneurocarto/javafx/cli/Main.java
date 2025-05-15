package io.ast.jneurocarto.javafx.cli;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.Application;
import javafx.stage.Stage;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        var config = new CartoConfig();
        var exit = new CommandLine(config).execute(args);
        if (exit != 0 || config.help) System.exit(exit);

        if (config.debug) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto", "debug");
        }

        App.application = new Application(config);
        javafx.application.Application.launch(App.class);
    }

    public static class App extends javafx.application.Application {
        static Application application;

        @Override
        public void start(Stage primaryStage) throws Exception {
            application.start(primaryStage);
        }
    }
}
