package io.ast.jneurocarto.javafx.cli;

import java.io.IOException;

import javafx.stage.Stage;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.cli.Main;
import io.ast.jneurocarto.javafx.atlas.Application;
import picocli.CommandLine;

@CommandLine.Command(
    name = "slice",
    sortOptions = false,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    description = "show atlas brain slice"
)
public class Slice implements Runnable {

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

        App.INSTANCE = new Application(brain);
        javafx.application.Application.launch(App.class);
    }

    public static class App extends javafx.application.Application {
        private static Application INSTANCE;

        @Override
        public void start(Stage primaryStage) throws Exception {
            INSTANCE.start(primaryStage);
        }
    }
}
