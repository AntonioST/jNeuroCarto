package io.ast.jneurocarto.probe_npx.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import picocli.CommandLine;

@CommandLine.Command(
  name = "select",
  description = "Select electrodes",
  exitCodeListHeading = "Exit Codes:%n",
  exitCodeList = {
    "0:successful electrode selection or normal exit.",
    "1:fail electrode selection."
  }
)
public final class Select implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", paramLabel = "CHMAP",
      description = "channelmap file.")
    Path chmapFile;

    @CommandLine.Parameters(index = "1", paramLabel = "BLUEPRINT",
      description = "blueprint file.")
    Path blueprintFile;

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "FILE",
      description = "output channelmap file.")
    Path outputFile;

    @CommandLine.Option(names = {"-s", "--selector"}, paramLabel = "NAME", defaultValue = "default",
      description = "use selector")
    String selector;

    @CommandLine.Option(names = "-O", paramLabel = "NAME=VALUE")
    Map<String, String> options = Map.of();

    @CommandLine.Option(names = {"-p", "--print"},
      description = "print channelmap result.")
    boolean printResult;

    @CommandLine.Option(names = "--list-options", help = true)
    boolean listOptions;

    @CommandLine.Option(names = "--list-selectors", help = true)
    boolean listSelectors;

    private Logger log;

    @Override
    public Integer call() {
        log = LoggerFactory.getLogger(getClass());
        log.debug("run()");

        if (listSelectors) {
            printSelectors();
            return 0;
        } else if (listOptions) {
            printOptions(selector);
            return 0;
        } else {
            try {
                return select();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void printSelectors() {
        var desp = new NpxProbeDescription();
        System.out.println("Selector:");
        desp.getElectrodeSelectors().forEach(name -> {
            System.out.printf("+ %-8s - %s\n", name, desp.newElectrodeSelector(name).getClass().getName());
        });
        System.out.println("use -s=VALUE to choose selector.");
    }

    public void printOptions(String selectorName) {
        var desp = new NpxProbeDescription();

        var selector = desp.newElectrodeSelector(selectorName);
        System.out.println(selector.getClass().getName());

        var options = selector.getOptions();
        if (options.isEmpty()) {
            System.out.println("(no options)");
        } else {
            options.forEach((name, value) -> {
                System.out.printf("+ %-8s - %s\n", name, this.options.getOrDefault(name, value));
            });
            System.out.println("use -ONAME=VALUE to set options.");
        }
    }

    public int select() throws IOException {
        Path useOutputFile;
        if (outputFile == null) {
            useOutputFile = newOutputChannelmapFile(chmapFile);
        } else if (Files.isDirectory(outputFile)) {
            useOutputFile = outputFile.resolve(chmapFile.getFileName());
        } else if (outputFile.getFileName().toString().equals("-")) {
            useOutputFile = null;
        } else {
            useOutputFile = outputFile;
        }

        log.debug("chmapFile={}", chmapFile);
        log.debug("blueprintFile={}", blueprintFile);
        log.debug("outputFile={}", useOutputFile);

        if (!Files.exists(chmapFile)) throw new RuntimeException("channelmap file not existed : " + chmapFile);
        if (!Files.exists(blueprintFile)) throw new RuntimeException("blueprint file not existed : " + blueprintFile);
        if (useOutputFile == null || Files.exists(useOutputFile)) System.err.println("output file will be overwritten");

        var desp = new NpxProbeDescription();

        log.debug("selector={}", selector);
        var selector = desp.newElectrodeSelector(this.selector);
        log.debug("selector.class={}", selector.getClass().getName());

        selector.setOptions(options);
        log.debug("selector.options={}", options);

        log.debug("load(chmapFile)");
        var chmap = desp.load(chmapFile);
        log.debug("probe.type={}", chmap.type().name());

        log.debug("load(blueprintFile)");
        var electrodes = desp.loadBlueprint(blueprintFile, chmap);

        log.debug("select()");
        var newChmap = selector.select(desp, chmap, electrodes);
        log.debug("select {}/{}", newChmap.size(), newChmap.nChannel());

        if (!desp.validateChannelmap(newChmap)) {
            System.err.println("channelmap is incomplete. fail selection");
            return 1;
        }

        if (useOutputFile == null) {
            log.debug("save(stdout)");
            System.out.println(newChmap.toImro());
        } else {
            log.debug("save(outputFile)");
            desp.save(useOutputFile, newChmap);
        }

        if (printResult) {
            ChannelMapUtil.printProbe(System.out, List.of(chmap, newChmap), true);
        }

        if (outputFile == null) {
            System.err.println("Output file : " + useOutputFile);
        }
        return 0;
    }

    public Path newOutputChannelmapFile(Path file) {
        Path dir = Path.of(".");
        String filename = file.getFileName().toString();
        Path ret = dir.resolve(filename);
        if (!Files.exists(ret)) {
            return ret;
        }

        int last = filename.lastIndexOf('.');
        String stem = filename.substring(0, last);
        String suffix = filename.substring(last);
        int i = 0;
        while (Files.exists(ret = dir.resolve(stem + "." + i + suffix))) {
            i++;
        }
        return ret;
    }
}
