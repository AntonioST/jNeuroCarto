package io.ast.jneurocarto;

import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.cli.CartoConfig;


public class Main {

    public static void main(String[] args) {
        var config = new CartoConfig();
        var parser = CartoConfig.newParser(config);

        int exit = parser.execute(args);
        if (exit != 0) {
            System.exit(exit);
        }

        if (config.help) {
            CartoConfig.usage(config, System.out);
            System.exit(0);
        }

        if (config.debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }

        main(config);
    }

    public static void main(CartoConfig config) {
        var logger = LoggerFactory.getLogger(Main.class);
        if (logger.isDebugEnabled()) {
            System.out.println(config);
        }
    }
}
