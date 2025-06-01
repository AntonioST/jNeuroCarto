package io.ast.jneurocarto.javafx.cli;

import java.util.regex.Pattern;

import javafx.scene.layout.VBox;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.utils.AutoCompleteTextField;
import picocli.CommandLine;

@CommandLine.Command(
  name = "complete",
  usageHelpAutoWidth = true,
  description = "demo complete text field"
)
public class Complete implements Example.Content, Runnable {
    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--directory",
      description = "only allow directory")
    boolean directory;

    @CommandLine.Option(names = {"-r", "--regex"},
      description = "make -g as regex pattern")
    boolean regex;

    @CommandLine.Option(names = {"-g", "--glob"},
      description = "filename match")
    String pattern;

    @CommandLine.Option(names = "--multiple",
      description = "allow multiple files")
    boolean multiple;

    @CommandLine.Option(names = "--escape",
      description = "allow escape root")
    boolean escape;

    @CommandLine.Option(names = "--validate",
      description = "validate during typing")
    boolean validate;

    @CommandLine.ParentCommand
    public Example parent;

    @Override
    public void run() {
        parent.launch(this);
    }

    /*=============*
     * Application *
     *=============*/

    private AutoCompleteTextField.OfPathField complete;

    @Override
    public void setup(VBox layout) {
        complete = new AutoCompleteTextField.OfPathField();
        complete.setOnlyDirectory(directory);
        complete.setAllowMultipleFile(multiple);
        complete.setAllowEscapeRoot(escape);
        complete.setValidateContent(validate);

        var pattern = this.pattern;
        if (pattern != null) {
            if (regex) {
                complete.setFileMatcher(Pattern.compile(pattern));
            } else {
                complete.setFileMatcher(pattern);
            }
        }

        complete.setOnAction(e -> System.out.println(complete.getText()));

        layout.getChildren().add(complete);
    }

    @Override
    public void setup(InteractionXYChart chart) {
    }
}
