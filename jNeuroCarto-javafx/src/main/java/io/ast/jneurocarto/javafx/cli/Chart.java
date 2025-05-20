package io.ast.jneurocarto.javafx.cli;

import javafx.stage.Stage;

import io.ast.jneurocarto.javafx.chart.Application;
import picocli.CommandLine;

@CommandLine.Command(
  name = "chart",
  usageHelpAutoWidth = true,
  description = "show interaction xy chart"
)
public class Chart implements Runnable {
    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--debug",
      description = "enable debug logging message.")
    public void debug(boolean value) {
        if (value) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto.javafx.chart", "debug");
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Chart()).execute(args);
    }

    @Override
    public void run() {
        new Application();
        javafx.application.Application.launch(App.class);
    }

    public static class App extends javafx.application.Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Application.getInstance().start(primaryStage);
        }
    }
}
