package io.ast.jneurocarto.atlas.cli;

import io.ast.jneurocarto.atlas.ImageSlices;
import picocli.CommandLine;

@CommandLine.Command(
  name = "slice",
  usageHelpAutoWidth = true,
  description = "show atlas brain slice"
)
public class Slice implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.ParentCommand
    public Use use;

    @CommandLine.Option(names = "--view", defaultValue = "coronal")
    public ImageSlices.View view;

    public enum Volume {
        reference, annotations
    }

    @CommandLine.Option(names = "--volume", defaultValue = "reference")
    public Volume volume;

    @Override
    public void run() {
        //XXX Unsupported Operation Slice.run
        throw new UnsupportedOperationException();
    }
}
