package io.ast.jneurocarto.probe_npx.cli;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import io.ast.jneurocarto.probe_npx.NpxProbeType;
import picocli.CommandLine;

@CommandLine.Command(
  name = "info",
  description = "information"
)
public final class Info implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "NAME", arity = "0..1", description = "information.")
    String target;

    @CommandLine.Parameters(index = "1..", arity = "0..", description = "arguments")
    String[] args;

    @Override
    public void run() {
        var log = LoggerFactory.getLogger(getClass());
        log.debug("run(target={})", target);

        if (target == null) {
            listTarget();
        } else {
            switch (target) {
            case "probes" -> listProbes();
            case "probe" -> {
                if (args == null || args.length == 0) {
                    System.out.println("require probe code");
                    listProbes();
                } else {
                    printProbe(args[0]);
                }
            }
            case "selectors" -> listSelectors();
            default -> unknownTarget(target);
            }
        }
    }

    public void listTarget() {
        System.out.println("""
          target:
            probes
            selectors
          """);
    }

    public void unknownTarget(String target) {
        System.out.println("unknown target : " + target);
    }

    public void listProbes() {
        var desp = new NpxProbeDescription();

        desp.supportedProbeType().forEach(code -> {
            System.out.printf("%-8s - %s\n", code, desp.probeTypeDescription(code));
        });
    }

    public void printProbe(String code) {
        var type = NpxProbeType.of(code);
        System.out.printf("%-16s %s\n", "code", code);
        System.out.printf("%-16s %d\n", "nShank", type.nShank());
        System.out.printf("%-16s %d\n", "nColumn/Shank", type.nColumnPerShank());
        System.out.printf("%-16s %d\n", "nRow/Shank", type.nRowPerShank());
        System.out.printf("%-16s %d\n", "nChannel", type.nChannel());
        System.out.printf("%-16s %d\n", "nBank", type.nBank());
        System.out.printf("%-16s %d\n", "nBlock", type.nBlock());
        System.out.printf("%-16s %d\n", "nBlock/Bank", type.nBlockPerBank());
        System.out.printf("%-16s %d\n", "nElectrode/Block", type.nElectrodePerBlock());
        System.out.printf("%-16s %d\n", "nElectrode/Shank", type.nElectrodePerShank());
        System.out.printf("%-16s %d\n", "nElectrode", type.nElectrode());
        System.out.printf("%-16s %d\n", "um/Column", type.spacePerColumn());
        System.out.printf("%-16s %d\n", "um/Row", type.spacePerRow());
        System.out.printf("%-16s %d\n", "um/Shank", type.spacePerShank());
        System.out.printf("%-16s %s\n", "reference", Arrays.toString(type.reference()));
    }

    public void listSelectors() {
        var desp = new NpxProbeDescription();
        desp.getElectrodeSelectors().forEach(name -> {
            System.out.printf("%-8s - %s\n", name, desp.newElectrodeSelector(name).getClass().getName());
        });
    }
}
