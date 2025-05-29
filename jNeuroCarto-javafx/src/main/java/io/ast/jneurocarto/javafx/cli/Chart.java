package io.ast.jneurocarto.javafx.cli;

import javafx.stage.Stage;

import io.ast.jneurocarto.javafx.chart.Application;
import picocli.CommandLine;

@CommandLine.Command(
  name = "chart",
  usageHelpAutoWidth = true,
  description = "show interaction xy chart",
  subcommands = {
    ClickLines.class,
    Bar.class,
    Colorbar.class,
    Matrix.class,
    FlashText.class,
    Complete.class
  }
)
public class Chart implements Runnable {
    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--debug",
      description = "enable debug logging message.")
    public void debug(boolean value) {
        if (value) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto.javafx.chart", "debug");
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto.javafx.cli", "debug");
        }
    }

    private static CommandLine parser;

    public static void main(String[] args) {
        parser = new CommandLine(new Chart());
        parser.setCaseInsensitiveEnumValuesAllowed(true);
//        parser.setParameterExceptionHandler((ex, _) -> {
//            ex.printStackTrace();
//            return 1;
//        });
        parser.execute(args);
    }

    @Override
    public void run() {
        parser.usage(System.out);
    }

    public void launch(Application app) {
        App.INSTANCE = app;
        javafx.application.Application.launch(App.class);
    }

    public static class App extends javafx.application.Application {
        static Application INSTANCE = null;

        @Override
        public void start(Stage primaryStage) throws Exception {
            INSTANCE.start(primaryStage);
        }
    }
}
