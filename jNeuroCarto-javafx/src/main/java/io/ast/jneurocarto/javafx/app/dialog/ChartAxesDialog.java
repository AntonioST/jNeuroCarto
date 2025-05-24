package io.ast.jneurocarto.javafx.app.dialog;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.utils.FormattedTextField;

public class ChartAxesDialog extends Dialog<ButtonType> {

    private final ProbeView<?> view;

    private final FormattedTextField.OfDoubleField xl;
    private final FormattedTextField.OfDoubleField xu;
    private final FormattedTextField.OfDoubleField yl;
    private final FormattedTextField.OfDoubleField yu;

    private final Logger log = LoggerFactory.getLogger(ChartAxesDialog.class);
    private final ButtonType apply;
    private final ButtonType done;
    private final ButtonType cancel;

    public ChartAxesDialog(ProbeView<?> view) {
        this.view = view;

        setTitle("Set chart view axes");

        var layout = new GridPane(5, 5);

        layout.add(new Label("low"), 1, 0);
        layout.add(new Label("high"), 2, 0);
        layout.add(new Label("X"), 0, 1);
        layout.add(new Label("Y"), 0, 2);

        var current = view.getAxesBounds();
        xl = new FormattedTextField.OfDoubleField(current.xLower());
        xu = new FormattedTextField.OfDoubleField(current.xUpper());
        yl = new FormattedTextField.OfDoubleField(current.yLower());
        yu = new FormattedTextField.OfDoubleField(current.yUpper());

        layout.add(xl, 1, 1);
        layout.add(xu, 2, 1);
        layout.add(yl, 1, 2);
        layout.add(yu, 2, 2);

        var reset = new Button("Get Default");
        reset.setTooltip(new Tooltip("Get reset boundaries"));
        reset.setOnAction(this::updateFromViewReset);

        var asDefault = new Button("Set Default");
        asDefault.setTooltip(new Tooltip("Set as reset boundaries"));
        asDefault.setOnAction(this::setAsDefault);

        var control = new HBox(reset, asDefault);
        control.setAlignment(Pos.BASELINE_RIGHT);
        control.setSpacing(5);

        var root = new VBox(layout, control);
        root.setSpacing(10);

        getDialogPane().setContent(root);

        apply = new ButtonType("Apply", ButtonBar.ButtonData.APPLY);
        done = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancel, apply, done);

        setOnCloseRequest(this::onClose);
    }

    public InteractionXYChart.AxesBounds getBoundaries() {
        var x1 = xl.getDoubleValue();
        var x2 = xu.getDoubleValue();
        var y1 = yl.getDoubleValue();
        var y2 = yu.getDoubleValue();
        return new InteractionXYChart.AxesBounds(x1, x2, y1, y2);
    }

    public void updateBoundaries(InteractionXYChart.AxesBounds current) {
        xl.setDoubleValue(current.xLower());
        xu.setDoubleValue(current.xUpper());
        yl.setDoubleValue(current.yLower());
        yu.setDoubleValue(current.yUpper());
    }

    private void updateFromViewReset(Event e) {
        updateBoundaries(view.getResetAxesBoundaries());
    }

    private void setAsDefault(Event e) {
        var bounds = getBoundaries();
        view.setResetAxesBoundaries(bounds);
        log.debug("set chart reset view axes to {}}", bounds);
    }

    private void onClose(Event e) {
        var result = getResult();
        if (result == done || result == apply) {
            var bounds = getBoundaries();
            log.debug("set chart view axes to {}", bounds);
            view.setAxesBoundaries(bounds);
            if (result == apply) {
                e.consume();
            }
        } else {
            log.debug("set chart view axes (canceled)");
        }
    }

}
