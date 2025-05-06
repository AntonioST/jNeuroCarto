package io.ast.jneurocarto.probe_npx.cli;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import picocli.CommandLine;

@CommandLine.Command(
  name = "create",
  description = "create typical channelmap."
)
public final class Create implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "METHOD", arity = "0..1",
      description = "method name.")
    String method;

    @CommandLine.Parameters(index = "1..", arity = "0..", description = "arguments")
    String[] args;

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "FILE", defaultValue = "output.imro",
      description = "output channelmap file.")
    Path outputFile;

    private Logger log;

    @Override
    public void run() {
        log = LoggerFactory.getLogger(getClass());
        log.debug("run()");

        if (method == null) {
            printMethods();
        } else {
            log.debug("method={}", method);
            try {
                new NpxProbeDescription().save(outputFile, create(method));
                log.debug("save={}", outputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void printMethods() {
        System.out.println("""
          Method:
            single S [R]                - npx24SingleShank
            stripe S [R]                - npx24SingleShank
            half (1/2) S[,S] [R]        - npx24HalfDensity
            quarter (1/4) [S[,S]] [R]   - npx24QuarterDensity
            one-eight (1/8) R           - npx24OneEightDensity
          """);
    }

    public ChannelMap create(String method) {
        switch (method) {
        case "single":
            switch (args.length) {
            case 1:
                return ChannelMaps.npx24SingleShank(Integer.parseInt(args[0]), 0);
            case 2: {
                var s = Integer.parseInt(args[0]);

                try {
                    return ChannelMaps.npx24SingleShank(s, Integer.parseInt(args[1]));
                } catch (NumberFormatException e) {
                }
                return ChannelMaps.npx24SingleShank(s, Double.parseDouble(args[1]));
            }
            }
            break;

        case "stripe":
            switch (args.length) {
            case 1:
                return ChannelMaps.npx24Stripe(Integer.parseInt(args[0]), 0);
            case 2: {
                var s = Integer.parseInt(args[0]);

                try {
                    return ChannelMaps.npx24Stripe(s, Integer.parseInt(args[1]));
                } catch (NumberFormatException e) {
                }
                return ChannelMaps.npx24Stripe(s, Double.parseDouble(args[1]));
            }
            }
            break;

        case "half":
        case "1/2":
            switch (args.length) {
            case 1:
                if (args[0].contains(",")) {
                    var i = args[0].indexOf(',');
                    var s1 = Integer.parseInt(args[0].substring(0, i));
                    var s2 = Integer.parseInt(args[0].substring(i + 1));
                    return ChannelMaps.npx24HalfDensity(s1, s2, 0);
                } else {
                    return ChannelMaps.npx24HalfDensity(Integer.parseInt(args[0]), 0);
                }

            case 2: {
                if (args[0].contains(",")) {
                    var i = args[0].indexOf(',');
                    var s1 = Integer.parseInt(args[0].substring(0, i));
                    var s2 = Integer.parseInt(args[0].substring(i + 1));
                    try {
                        return ChannelMaps.npx24HalfDensity(s1, s2, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                    }
                    return ChannelMaps.npx24HalfDensity(s1, s2, Double.parseDouble(args[1]));

                } else {
                    var s = Integer.parseInt(args[0]);
                    try {
                        return ChannelMaps.npx24HalfDensity(s, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                    }
                    return ChannelMaps.npx24HalfDensity(s, Double.parseDouble(args[1]));
                }
            }
            }
            break;

        case "quarter":
        case "1/4":
            switch (args.length) {
            case 0:
                return ChannelMaps.npx24QuarterDensity(0);

            case 1:
                try {
                    return ChannelMaps.npx24QuarterDensity(Integer.parseInt(args[0]));
                } catch (NumberFormatException e) {
                }
                return ChannelMaps.npx24QuarterDensity(Double.parseDouble(args[0]));

            case 2: {
                if (args[0].contains(",")) {
                    var i = args[0].indexOf(',');
                    var s1 = Integer.parseInt(args[0].substring(0, i));
                    var s2 = Integer.parseInt(args[0].substring(i + 1));
                    try {
                        return ChannelMaps.npx24QuarterDensity(s1, s2, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                    }
                    return ChannelMaps.npx24QuarterDensity(s1, s2, Double.parseDouble(args[1]));

                } else {
                    var s = Integer.parseInt(args[0]);
                    try {
                        return ChannelMaps.npx24QuarterDensity(s, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                    }
                    return ChannelMaps.npx24QuarterDensity(s, Double.parseDouble(args[1]));
                }
            }
            }
            break;

        case "one-eight":
        case "1/8":
            switch (args.length) {
            case 0:
                return ChannelMaps.npx24OneEightDensity(0);
            case 1: {
                try {
                    return ChannelMaps.npx24OneEightDensity(Integer.parseInt(args[0]));
                } catch (NumberFormatException e) {
                }
                return ChannelMaps.npx24OneEightDensity(Double.parseDouble(args[0]));
            }
            }
            break;

        default:
            throw new RuntimeException("unknown method : " + method);
        }

        throw new RuntimeException("wrong argument count.");
    }
}
