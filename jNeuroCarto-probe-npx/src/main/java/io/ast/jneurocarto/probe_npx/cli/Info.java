package io.ast.jneurocarto.probe_npx.cli;

import java.util.Arrays;

import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import io.ast.jneurocarto.probe_npx.NpxProbeType;
import picocli.CommandLine;

@CommandLine.Command(
  name = "info",
  usageHelpAutoWidth = true,
  description = "information"
)
public final class Info implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    @CommandLine.Command(name = "probes", description = "list found probes types.")
    public void listProbes() {
        var desp = new NpxProbeDescription();

        desp.supportedProbeType().forEach(code -> {
            System.out.printf("%-8s - %s\n", code, desp.probeTypeDescription(code));
        });
    }

    @CommandLine.Command(name = "probe", description = "print probe information.")
    public void printProbe(
      @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "CODE", description = "probe code") String code
    ) {
        if (code == null) {
            System.out.println("require probe code");
            listProbes();
        } else {
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
    }

    @CommandLine.Command(name = "selectors", description = "list found electrode selectors.")
    public void listSelectors() {
        var desp = new NpxProbeDescription();
        desp.getElectrodeSelectors().forEach(name -> {
            System.out.printf("%-8s - %s\n", name, desp.newElectrodeSelector(name).getClass().getName());
        });
    }
}
