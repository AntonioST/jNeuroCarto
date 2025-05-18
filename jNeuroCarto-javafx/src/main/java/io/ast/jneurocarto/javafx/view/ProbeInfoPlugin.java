package io.ast.jneurocarto.javafx.view;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.javafx.app.PluginSetupService;

@NullMarked
public abstract class ProbeInfoPlugin<T> extends InvisibleView implements ProbePlugin<T> {

    @Override
    public String name() {
        return "Probe information";
    }

    @Override
    public String description() {
        return "Probe information";
    }

    public abstract List<String> listAllInfoLabels();

    public abstract @Nullable String getInfoValue(String info, T chmap, List<ElectrodeDescription> blueprint);

    /*===========*
     * UI Layout *
     *===========*/

    private @Nullable List<Label> labels;
    private @Nullable List<Label> contents;

    @Override
    protected Node setupContent(PluginSetupService service) {
        var labels = listAllInfoLabels();
        var size = labels.size();
        this.labels = new ArrayList<>(size);
        contents = new ArrayList<>(size);

        var layout = new GridPane();
        for (int i = 0; i < size; i++) {
            var label = new Label(labels.get(i));
            this.labels.add(label);
            layout.add(label, 0, i);

            var content = new Label("");
            contents.add(content);
            layout.add(content, 1, i);
        }

        var c1 = new ColumnConstraints(120);
        var c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);

        layout.getColumnConstraints().addAll(c1, c2);

        return layout;
    }

    @Override
    public void onProbeUpdate(T chmap, List<ElectrodeDescription> blueprint) {
        var labels = this.labels;
        var contents = this.contents;
        if (labels == null || contents == null) return;

        var log = LoggerFactory.getLogger(getClass());

        for (int i = 0, size = labels.size(); i < size; i++) {
            var label = labels.get(i).getText();
            var content = contents.get(i);
            String value;

            try {
                value = getInfoValue(label, chmap, blueprint);
            } catch (Exception e) {
                value = null;
                log.warn(label, e);
            }

            if (value == null) {
                content.setText("");
            } else {
                content.setText(value);
            }
        }
    }
}
