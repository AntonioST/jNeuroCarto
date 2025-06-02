package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import io.ast.jneurocarto.javafx.utils.FormattedTextField;

public class ImplantEditDialog extends Dialog<ButtonType> {

    public ObjectProperty<ImplantState> implant = new SimpleObjectProperty<>();

    private final ImplantState init;
    private FormattedTextField.OfIntField shank;
    private FormattedTextField.OfDoubleField ap;
    private FormattedTextField.OfDoubleField dv;
    private FormattedTextField.OfDoubleField ml;
    private FormattedTextField.OfDoubleField depth;
    private FormattedTextField.OfDoubleField rap;
    private FormattedTextField.OfDoubleField rdv;
    private FormattedTextField.OfDoubleField rml;
    private ChoiceBox<String> choice;
    private FormattedTextField.OfDoubleField refAP;
    private FormattedTextField.OfDoubleField refDV;
    private FormattedTextField.OfDoubleField refML;

    public ImplantEditDialog(AtlasReferenceService references, ImplantState state) {
        init = state;

        implant.set(state);

        setTitle("Implant Coordinate");
        var c1 = setupCenter(state);
        var c2 = setupButton(references, state);
        var layout = new VBox(c1, c2);
        layout.setSpacing(5);

        getDialogPane().setContent(layout);

        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.APPLY, ButtonType.OK);

        setOnCloseRequest(this::onClose);
    }

    private Node setupCenter(ImplantState state) {
        var layout = new GridPane();
//        layout.setGridLinesVisible(true);
        layout.setHgap(5);
        layout.setVgap(5);

        // name:Label, value:TextField, unit:Label
        layout.add(new Label("shank"), 0, 0);
        layout.add(new Label("AP"), 0, 1);
        layout.add(new Label("DV"), 0, 2);
        layout.add(new Label("ML"), 0, 3);

        shank = new FormattedTextField.OfIntField(state.shank);
        shank.getValueProperty().addListener(this::onUpdate);
        ap = new FormattedTextField.OfDoubleField(state.ap / 1000);
        ap.getValueProperty().addListener(this::onUpdate);
        dv = new FormattedTextField.OfDoubleField(state.dv / 1000);
        dv.getValueProperty().addListener(this::onUpdate);
        ml = new FormattedTextField.OfDoubleField(state.ml / 1000);
        ml.getValueProperty().addListener(this::onUpdate);


        layout.add(shank, 1, 0);
        layout.add(ap, 1, 1);
        layout.add(dv, 1, 2);
        layout.add(ml, 1, 3);

        layout.add(new Label("mm"), 2, 1);
        layout.add(new Label("mm"), 2, 2);
        layout.add(new Label("mm"), 2, 3);

        // name:Label, value:TextField, unit:Label
        layout.add(new Label("rot AP"), 3, 0);
        layout.add(new Label("rot DV"), 3, 1);
        layout.add(new Label("rot ML"), 3, 2);
        layout.add(new Label("depth"), 3, 3);

        rap = new FormattedTextField.OfDoubleField(state.rap);
        rap.getValueProperty().addListener(this::onUpdate);
        rdv = new FormattedTextField.OfDoubleField(state.rdv);
        rdv.getValueProperty().addListener(this::onUpdate);
        rml = new FormattedTextField.OfDoubleField(state.rml);
        rml.getValueProperty().addListener(this::onUpdate);
        depth = new FormattedTextField.OfDoubleField(state.depth / 1000);
        depth.getValueProperty().addListener(this::onUpdate);

        layout.add(rap, 4, 0);
        layout.add(rdv, 4, 1);
        layout.add(rml, 4, 2);
        layout.add(depth, 4, 3);

        layout.add(new Label("°"), 5, 0);
        layout.add(new Label("°"), 5, 1);
        layout.add(new Label("°"), 5, 2);
        layout.add(new Label("mm"), 5, 3);

        // ColumnConstraints
        var c0 = new ColumnConstraints(50, 50, 100);
        c0.setHalignment(HPos.RIGHT);

        var c1 = new ColumnConstraints(100, 100, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);

        var c2 = new ColumnConstraints(30, 30, 100);
        c2.setHalignment(HPos.LEFT);

        var c3 = new ColumnConstraints(50, 50, 100);
        c3.setHalignment(HPos.RIGHT);

        var c4 = new ColumnConstraints(100, 100, Double.MAX_VALUE);
        c4.setHgrow(Priority.ALWAYS);

        var c5 = new ColumnConstraints(30, 30, 100);
        c5.setHalignment(HPos.LEFT);

        layout.getColumnConstraints().addAll(c0, c1, c2, c3, c4, c5);

        return layout;
    }

    private Node setupButton(AtlasReferenceService references, ImplantState state) {
        // choice, ap, dv, ml, unit
        var refs = new ArrayList<String>();
        refs.addAll(references.getReferenceList());
        refs.sort(String::compareToIgnoreCase);
        refs.add(0, "Global");

        choice = new ChoiceBox<>();
        choice.setMaxWidth(Integer.MAX_VALUE);
        choice.getItems().addAll(refs);

        refAP = new FormattedTextField.OfDoubleField(0);
        refAP.setMinWidth(50);
        refAP.setMaxWidth(120);
        refAP.setEditable(false);

        refDV = new FormattedTextField.OfDoubleField(0);
        refDV.setMinWidth(50);
        refDV.setMaxWidth(120);
        refDV.setEditable(false);

        refML = new FormattedTextField.OfDoubleField(0);
        refML.setMinWidth(50);
        refML.setMaxWidth(120);
        refML.setEditable(false);

        var isApFlap = new CheckBox("AP flipped");
        isApFlap.setDisable(true);

        choice.setOnAction(e -> {
            var name = choice.getValue();
            refAP.unsetWarning();
            refDV.unsetWarning();
            refML.unsetWarning();

            if ("Global".equals(name)) {
                isApFlap.setSelected(false);
                refAP.setDoubleValue(0);
                refDV.setDoubleValue(0);
                refML.setDoubleValue(0);
            } else {
                var ref = references.getReference(name);
                if (ref == null) {
                    refAP.setDoubleValue(0);
                    refAP.setWarning("reference " + name + " does not exist");
                    refDV.setDoubleValue(0);
                    refDV.setWarning("");
                    refML.setDoubleValue(0);
                    refML.setWarning("");
                } else {
                    var coor = ref.coordinate();
                    isApFlap.setSelected(ref.flipAP());

                    refAP.setDoubleValue(coor.ap() / 1000);
                    refDV.setDoubleValue(coor.dv() / 1000);
                    refML.setDoubleValue(coor.ml() / 1000);
                }
            }

            updateImplantState();
        });

        if (state.reference == null) {
            choice.setValue("Global");
        } else {
            choice.setValue(state.reference);
        }

        var controls = new HBox(
            choice, isApFlap
        );

        controls.setSpacing(5);

        var labels = new HBox(
            new Label("AP"), refAP,
            new Label("DV"), refDV,
            new Label("ML"), refML,
            new Label("mm"));
        labels.setSpacing(5);
        HBox.setHgrow(refAP, Priority.ALWAYS);
        HBox.setHgrow(refDV, Priority.ALWAYS);
        HBox.setHgrow(refML, Priority.ALWAYS);

        var layout = new VBox(controls, labels);
        layout.setSpacing(5);

        return layout;
    }

    private void onClose(DialogEvent e) {
        var result = getResult();
        if (result == ButtonType.APPLY || result == ButtonType.OK) {

            updateImplantState();

            if (result == ButtonType.APPLY) {
                e.consume();
            }
        } else if (result == ButtonType.CANCEL) {
            implant.set(init);
        }
    }

    private void onUpdate(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        updateImplantState();
    }

    private void updateImplantState() {
        var state = new ImplantState();
        state.ap = ap.getDoubleValue() * 1000;
        state.dv = dv.getDoubleValue() * 1000;
        state.ml = ml.getDoubleValue() * 1000;
        state.shank = shank.getValue();
        state.rap = rap.getDoubleValue();
        state.rdv = rdv.getDoubleValue();
        state.rml = rml.getDoubleValue();
        state.depth = depth.getDoubleValue() * 1000;
        state.reference = choice.getValue();

        implant.set(state);
    }
}
