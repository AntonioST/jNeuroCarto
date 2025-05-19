package io.ast.jneurocarto.javafx.cli;

import java.io.IOException;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.Application;
import io.ast.jneurocarto.javafx.atlas.AtlasBrainService;
import io.ast.jneurocarto.javafx.utils.IOAction;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        var config = new CartoConfig();
        var exit = new CommandLine(config).execute(args);
        if (exit != 0 || config.help) System.exit(exit);

        if (config.debug) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto", "debug");
        }

        if (config.atlasName != null && !config.atlasName.isEmpty() && AtlasBrainService.isPreloadAtlasBrain()) {
            Thread.ofVirtual().start(() -> preloadAtlasBrain(config));
        }

        var app = new Application(config);
        javafx.application.Application.launch(App.class);

        if (config.file != null) {
            Platform.runLater(() -> {
                try {
                    app.loadChannelmap(config.file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static class App extends javafx.application.Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Application.getInstance().start(primaryStage);
        }
    }

    private static void preloadAtlasBrain(CartoConfig config) {
        var log = LoggerFactory.getLogger(AtlasBrainService.class);
        log.debug("pre check download for {}", config.atlasName);
        IOAction.measure(log, "check download", () -> {
            var result = AtlasBrainService.newDownloader(config).setCheckLatest(true).download();
            if (result.hasError()) {
                if (result.isDownloading()) {
                    log.info("{} is downloading.", result.atlasNameVersion());
                } else if (result.isDownloadFailed()) {
                    log.info("fail to download {}.", result.atlasNameVersion());
                } else if (!result.isDownloaded()) {
                    log.warn("fail download.", result.error());
                }
            }
        });

    }
}
