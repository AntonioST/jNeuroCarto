package io.ast.jneurocarto.atlas.cli;

import java.io.IOException;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.ImageSlices;
import io.ast.jneurocarto.atlas.gui.AtlasBrainSliceApplication;
import javafx.application.Platform;
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

    @CommandLine.Option(names = "--volume", defaultValue = "reference")
    public AtlasBrainSliceApplication.UseImage volume;

    @Override
    public void run() {
        BrainAtlas brain;
        try {
            brain = use.getAtlas();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var app = new AtlasBrainSliceApplication(brain);
        app.launch();
        Platform.runLater(() -> {
            app.setUseVolumeImage(volume);
            app.setProjection(view);
        });

    }
}
