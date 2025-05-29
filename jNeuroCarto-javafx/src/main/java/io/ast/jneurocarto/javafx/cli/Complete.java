package io.ast.jneurocarto.javafx.cli;

import javafx.scene.layout.VBox;

import io.ast.jneurocarto.javafx.chart.Application;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.utils.AutoCompleteTextField;
import picocli.CommandLine;

@CommandLine.Command(
  name = "complete",
  usageHelpAutoWidth = true,
  description = "demo complete text"
)
public class Complete implements Application.ApplicationContent, Runnable {
    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--multiple",
      description = "allow multiple files")
    boolean multiple;

    @CommandLine.Option(names = "--escape",
      description = "allow escape root")
    boolean escape;

    @CommandLine.ParentCommand
    public Chart parent;

    @Override
    public void run() {
        parent.launch(new Application(this));
    }

    /*=============*
     * Application *
     *=============*/

    private AutoCompleteTextField.OfPathField complete;

    @Override
    public void setup(VBox layout) {
        complete = new AutoCompleteTextField.OfPathField();
        complete.setAllowMultipleFileProperty(multiple);
        complete.setAllowEscapeRootProperty(escape);
        layout.getChildren().add(complete);
    }

    @Override
    public void setup(InteractionXYChart chart) {
    }
}
