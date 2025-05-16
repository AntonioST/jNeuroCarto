package io.ast.jneurocarto.javafx.cli;

import java.util.Arrays;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.Application;
import javafx.stage.Stage;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        var config = new CartoConfig();
        var exit = new CommandLine(config).execute(args);
        if (exit != 0 || config.help) System.exit(exit);

        if (config.debug) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto", "debug");
        }

        new Application(config);
        javafx.application.Application.launch(App.class);
    }

    public static class App extends javafx.application.Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Application.getInstance().start(primaryStage);
        }
    }
}
