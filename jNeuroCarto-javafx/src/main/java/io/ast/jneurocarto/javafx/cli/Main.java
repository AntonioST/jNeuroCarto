package io.ast.jneurocarto.javafx.cli;

import javafx.stage.Stage;

import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.Application;
import io.ast.jneurocarto.javafx.atlas.AtlasBrainService;
import io.ast.jneurocarto.javafx.utils.IOAction;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        var config = new CartoConfig();
        var parser = new CommandLine(config);
        var exit = parser.execute(args);
        if (exit != 0 || parser.isUsageHelpRequested() || parser.isVersionHelpRequested()) System.exit(exit);

        if (config.debug) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto", "debug");
        }

        if (config.atlasName != null && !config.atlasName.isEmpty() && AtlasBrainService.isPreloadAtlasBrain()) {
            Thread.ofVirtual().name("preloadAtlasBrain").start(() -> preloadAtlasBrain(config));
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
