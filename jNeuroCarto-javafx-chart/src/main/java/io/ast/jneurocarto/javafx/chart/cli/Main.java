package io.ast.jneurocarto.javafx.chart.cli;

import java.util.Objects;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
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
    DragDrop.class
  }
)
public class Main implements Runnable {
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
        parser = new CommandLine(new Main());
        parser.setCaseInsensitiveEnumValuesAllowed(true);
        parser.execute(args);
    }

    @Override
    public void run() {
        parser.usage(System.out);
    }

    public void launch(Content app) {
        App.INSTANCE = app;
        javafx.application.Application.launch(App.class);
    }

    public static class App extends javafx.application.Application {
        static Content INSTANCE = null;

        private Content content;

        @Override
        public void start(Stage stage) throws Exception {
            this.content = Objects.requireNonNull(INSTANCE);

            // chart
            InteractionXYChart chart = new InteractionXYChart();
            chart.setResetAxesBoundaries(0, 100, 0, 100);
            chart.resetAxesBoundaries();

            chart.setMinWidth(800);
            chart.setMinHeight(800);

            // layout
            VBox layout = new VBox(chart);
            content.setup(layout);

            // scene
            Scene scene = new Scene(layout);
            content.setup(scene);

            // stage
            stage.setScene(scene);
            stage.setTitle("InteractionXYChart");
            stage.sizeToScene();
            stage.show();

            // setup content
            content.setup(chart);
        }

    }

    public interface Content {
        default void setup(Scene scene) {
        }

        default void setup(VBox layout) {
        }

        void setup(InteractionXYChart chart);
    }
}
