package io.ast.jneurocarto.javafx.cli;

import java.io.IOException;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.cli.Main;
import io.ast.jneurocarto.javafx.atlas.Application;
import javafx.stage.Stage;
import picocli.CommandLine;

@CommandLine.Command(
  name = "slice",
  usageHelpAutoWidth = true,
  description = "show atlas brain slice"
)
public class Slice implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Mixin
    public Main.ConfigOptions config;

    @CommandLine.Mixin
    public Main.UseAtlas use;

    @CommandLine.Option(names = "--debug",
      description = "enable debug logging message.")
    public void debug(boolean value) {
        if (value) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto.javafx.atlas", "debug");
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Slice()).execute(args);
    }

    @Override
    public void run() {
        var downloader = use.newDownloader(config);

        BrainAtlas brain;
        try {
            brain = use.download(downloader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Application(brain);
        javafx.application.Application.launch(App.class);
    }

    public static class App extends javafx.application.Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Application.getInstance().start(primaryStage);
        }
    }
}
