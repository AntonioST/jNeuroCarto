package io.ast.jneurocarto.javafx.utils;

import java.util.Collections;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.javafx.chart.Colormap;
import io.ast.jneurocarto.javafx.chart.LinearColormap;

@NullMarked
public class ColormapChooseDialog extends Dialog<ButtonType> {

    private final Canvas canvas;
    private final FormattedTextField.OfDoubleField lower;
    private final FormattedTextField.OfDoubleField upper;
    private final CheckBox reverse;
    private final ChoiceBox<String> choice;
    private final ButtonType done;
    private final ButtonType cancel;
    private final FlashStringBuffer buffer = new FlashStringBuffer();
    public final ObjectProperty<Colormap> colormapProperty = new SimpleObjectProperty<>();

    public ColormapChooseDialog(String title, LinearColormap init) {
        setTitle(title);
        setContentText("Colormap");

        canvas = new Canvas(400, 30);

        choice = new ChoiceBox<>();
        choice.setMinWidth(200);
        choice.setOnAction(this::updateColormap);
        choice.addEventFilter(KeyEvent.KEY_PRESSED, this::selectNextOne);
        choice.addEventFilter(KeyEvent.KEY_RELEASED, this::selectNextOne);

        var maps = Colormap.availableBuiltinColormapName();
        Collections.sort(maps, String::compareToIgnoreCase);
        choice.getItems().addAll(maps);

        var norm = init.normalize();
        lower = new FormattedTextField.OfDoubleField(norm.lower());
        lower.setMinWidth(150);
        lower.setOnAction(this::updateColormap);

        upper = new FormattedTextField.OfDoubleField(norm.upper());
        upper.setMinWidth(150);
        upper.setOnAction(this::updateColormap);

        reverse = new CheckBox("reverse");
        reverse.setOnAction(this::updateColormap);

        var r1 = new HBox(new Label("colormap"), choice, reverse);
        HBox.setHgrow(choice, Priority.ALWAYS);
        r1.setSpacing(5);

        var r2 = new HBox(new Label("lower"), lower, new Label("upper"), upper);
        r2.setSpacing(5);

        var root = new VBox(canvas, r1, r2);
        root.setSpacing(10);

        getDialogPane().setContent(root);

        done = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancel, done);

        var name = init.name();
        if (name == null) {
            updateColormap(init);
        } else {
            choice.setValue(name);
            reverse.setSelected(name.endsWith("_r"));
        }
    }

    private boolean duringFixing;

    private void updateColormap(Event e) {
        if (duringFixing) return;

        var lower = this.lower.getDoubleValue();
        var upper = this.upper.getDoubleValue();
        if (lower <= upper) {
            var name = choice.getValue();
            if (reverse.isSelected()) {
                name += "_r";
            }
            var colormap = Colormap.of(name).withNormalize(lower, upper);
            colormapProperty.set(colormap);
            updateColormap(colormap);
        } else {
            duringFixing = true;
            try {
                this.lower.setDoubleValue(upper);
                this.upper.setDoubleValue(lower);
            } finally {
                duringFixing = false;
            }
        }
    }

    private void updateColormap(LinearColormap colormap) {
        var gc = canvas.getGraphicsContext2D();
        var w = canvas.getWidth();
        var h = canvas.getHeight();

        gc.setFill(colormap.gradient(0, 0, w, 0));
        gc.fillRect(0, 0, w, h);
    }

    private void selectNextOne(KeyEvent e) {
        var pressed = e.getEventType() == KeyEvent.KEY_PRESSED;
        var c = e.getCode();

        var items = choice.getItems();
        int index = items.indexOf(choice.getValue());
        int oldIndex = index;

        switch (c) {
        case KeyCode.UP:
        case KeyCode.DOWN:
            if (pressed) {
                index += (c == KeyCode.UP ? -1 : 1);
            }
            break;
        case KeyCode.HOME:
        case KeyCode.END:
            if (pressed) {
                index = c == KeyCode.HOME ? 0 : items.size() - 1;
            }
            break;
        default:
            if (pressed && buffer.accept(c)) {
                var find = buffer.toString().toLowerCase();
                for (int i = 0, size = items.size(); i < size; i++) {
                    if (items.get(i).toLowerCase().startsWith(find)) {
                        index = i;
                        break;
                    }
                }
                break;
            }
            return;
        }

        if (pressed && oldIndex != index) {
            if (0 <= index && index < items.size()) {
                choice.setValue(items.get(index));
            }
        }
        e.consume();
    }
}
