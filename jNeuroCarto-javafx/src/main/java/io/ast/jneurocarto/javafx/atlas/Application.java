package io.ast.jneurocarto.javafx.atlas;


import java.io.IOException;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.*;
import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.javafx.utils.IOAction;

public class Application {

    private static Application INSTANCE = null;
    private final BrainAtlas brain;
    private final ImageVolume volume;

    public final SimpleIntegerProperty maxOffsetProperty = new SimpleIntegerProperty(1000);

    private ImageSliceStack.Projection currentProjection;
    private ImageSliceStack images;
    private ImageSlice image;

    private final Logger log = LoggerFactory.getLogger(Application.class);

    public Application(BrainAtlas brain) {
        INSTANCE = this;
        this.brain = brain;

        ImageVolume volume;
        try {
            volume = brain.reference();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.volume = new ImageVolume(volume);
        this.volume.normalizeGrayLevel();

        IOAction.measure(log, "pre load annotation", brain::annotation);
        IOAction.measure(log, "pre load hemispheres", brain::hemispheres);
    }

    private Stage stage;
    private Label labelMouseInformation;
    private Label labelAnchorInformation;
    private Label labelStructure;
    private ToggleGroup groupProjection;
    private RadioButton btnCoronal;
    private RadioButton btnSagittal;
    private RadioButton btnTransverse;
    private Button btnResetRotation;
    private Button btnResetOffsetWidth;
    private Button btnResetOffsetHeight;
    private Slider sliderPlane;
    private Slider sliderRotation;
    private Slider sliderOffsetWidth;
    private Slider sliderOffsetHeight;
    private SliceView imageView;

    public void start(Stage stage) {
        this.stage = stage;
        log.debug("start");
        stage.setTitle("AtlasBrainSlice");
        stage.setScene(scene());
        stage.sizeToScene();
        stage.show();
    }

    private Scene scene() {
        var scene = new Scene(root());
        return scene;
    }

    private Parent root() {
        var root = new VBox();

        var slice = sliceView();
        VBox.setVgrow(slice, Priority.ALWAYS);

        var coorInformation = new Label("(AP, DV, ML) um");
        labelMouseInformation = new Label("");
        labelAnchorInformation = new Label("");
        var information = new HBox(coorInformation, labelMouseInformation, labelAnchorInformation);
        information.setSpacing(20);

        labelStructure = new Label("");

        var control = controlView();
        VBox.setMargin(control, new Insets(5, 5, 15, 5));

        root.getChildren().addAll(slice, information, labelStructure, control);

        return root;
    }

    private Node sliceView() {
        imageView = new SliceView(600, 500);
        imageView.setOnMouseClicked(this::onMouseClickedInSlice);
        imageView.setOnMouseMoved(this::onMouseMovingInSlice);
        imageView.setOnMouseExited(this::onMouseExitedInSlice);
        imageView.anchor.addListener((_, _, _) -> updateAnchorInformation());
        imageView.painter.invertRotation(true);
        imageView.painter.flipUD(false);
        imageView.painter.flipLR(true);
        return imageView;
    }

    private Node controlView() {
        var layout = new GridPane();
//        layout.setGridLinesVisible(true);
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(new Label("Projection"), 0, 0);
        layout.add(new Label("Plane (mm)"), 0, 1);
        layout.add(new Label("Rotation (deg)"), 0, 2);
        layout.add(new Label("d(Width) (um)"), 0, 3);
        layout.add(new Label("d(Height) (um)"), 0, 4);

        layout.add(projectView(), 1, 0, 2, 1);
        layout.add(bindSliderAndLabel(sliderPlane = newSlider(), new Label()), 1, 1);
        layout.add(bindSliderAndLabel(sliderRotation = newSlider(), new Label()), 1, 2);
        layout.add(bindSliderAndLabel(sliderOffsetWidth = newSlider(), new Label()), 1, 3);
        layout.add(bindSliderAndLabel(sliderOffsetHeight = newSlider(), new Label()), 1, 4);

        layout.add(btnResetRotation = newSliderResetButton(), 2, 2);
        layout.add(btnResetOffsetWidth = newSliderResetButton(), 2, 3);
        layout.add(btnResetOffsetHeight = newSliderResetButton(), 2, 4);

        sliderRotation.valueProperty().bindBidirectional(imageView.painter.r);

        var c1 = new ColumnConstraints(100, 100, 200);
        c1.setHalignment(HPos.RIGHT);

        var c2 = new ColumnConstraints(400, 400, Double.MAX_VALUE);
        c2.setHgrow(Priority.ALWAYS);


        layout.getColumnConstraints().addAll(c1, c2);

        return layout;
    }

    private Node projectView() {
        var layout = new HBox();

        groupProjection = new ToggleGroup();
        btnCoronal = new RadioButton("Coronal");
        btnCoronal.setToggleGroup(groupProjection);
        btnCoronal.setOnAction(this::onProjectButtonPressed);

        btnSagittal = new RadioButton("Sagittal");
        btnSagittal.setToggleGroup(groupProjection);
        btnSagittal.setOnAction(this::onProjectButtonPressed);

        btnTransverse = new RadioButton("Transverse");
        btnTransverse.setToggleGroup(groupProjection);
        btnTransverse.setOnAction(this::onProjectButtonPressed);

        layout.getChildren().addAll(btnCoronal, btnSagittal, btnTransverse);

        return layout;
    }

    private Slider newSlider() {
        var slider = new Slider();
        slider.setShowTickLabels(true);
        slider.valueProperty().addListener((_, _, _) -> onSliderMoved());
        return slider;
    }

    private Node bindSliderAndLabel(Slider slider, Label label) {
        if (slider == sliderOffsetHeight || slider == sliderOffsetWidth) {
            slider.maxProperty().bind(maxOffsetProperty);
            slider.minProperty().bind(maxOffsetProperty.negate());
            slider.majorTickUnitProperty().bind(maxOffsetProperty.divide(10));
        } else if (slider == sliderRotation) {
            slider.setMax(90);
            slider.setMin(-90);
            slider.setMajorTickUnit(10);
        }

        slider.valueProperty().addListener((_, _, value) -> {
            String text;
            if (slider == sliderPlane) {
                text = String.format("%.2f mm", value.doubleValue());
            } else if (slider == sliderRotation) {
                text = String.format("%.1f deg", value.doubleValue());
            } else {
                text = String.format("%d um", value.intValue());
            }

            label.setText(text);
        });

        slider.setValue(1);
        slider.setValue(0);
        label.setMinWidth(50);

        var layout = new HBox(slider, label);
        HBox.setHgrow(slider, Priority.ALWAYS);
        return layout;
    }

    private Button newSliderResetButton() {
        var button = new Button("Reset");
        button.setOnAction(this::onResetButtonPressed);
        return button;
    }

    private void onProjectButtonPressed(ActionEvent e) {
        var source = e.getSource();
        if (source == btnCoronal) {
            changeProjection(ImageSliceStack.Projection.coronal);
        } else if (source == btnSagittal) {
            changeProjection(ImageSliceStack.Projection.sagittal);
        } else if (source == btnTransverse) {
            changeProjection(ImageSliceStack.Projection.transverse);
        }
    }

    private void onResetButtonPressed(ActionEvent e) {
        var source = e.getSource();
        if (source == btnResetOffsetWidth) {
            sliderOffsetWidth.setValue(0);
        } else if (source == btnResetOffsetHeight) {
            sliderOffsetHeight.setValue(0);
        } else if (source == btnResetRotation) {
            sliderRotation.setValue(0);
        }
    }

    private void onSliderMoved() {
        if (images != null) {
            updateSliceImage();
            updateAnchorInformation();
        }
    }

    public ImageSliceStack.Projection getProjection() {
        return currentProjection;
    }

    public void setProjection(ImageSliceStack.Projection projection) {
        Platform.runLater(() -> {
            switch (projection) {
            case coronal -> groupProjection.selectToggle(btnCoronal);
            case sagittal -> groupProjection.selectToggle(btnSagittal);
            case transverse -> groupProjection.selectToggle(btnTransverse);
            }
            log.debug("setProjection({})", projection);
        });
    }

    private void changeProjection(ImageSliceStack.Projection projection) {
        if (currentProjection == projection && images != null) return;
        log.debug("changeProjection({})", projection);

        currentProjection = projection;

        var image = this.image;
        images = new ImageSliceStack(brain, volume, projection);

        var maxPlaneLength = images.planeUm() / 1000;
        sliderPlane.setMax(maxPlaneLength);

        var anchor = imageView.anchor.get();
        if (anchor == null || image == null) {
            if (projection == ImageSliceStack.Projection.sagittal) {
                sliderPlane.setValue(maxPlaneLength / 2);
            } else {
                sliderPlane.setValue(0);
            }
        } else {
            var coor = image.pullBack(anchor);
            sliderPlane.setValue(projection.get(coor, projection.p) / 1000);
            imageView.anchor.setValue(this.image.project(coor));
        }

        sliderRotation.setValue(0);
        sliderOffsetWidth.setValue(0);
        sliderOffsetHeight.setValue(0);
        sliderOffsetWidth.setBlockIncrement(images.resolution()[1]);
        sliderOffsetHeight.setBlockIncrement(images.resolution()[2]);

        updateSliceImage();
    }

    private void updateSliceImage() {
        var plane = sliderPlane.getValue() * 1000;
        var dw = sliderOffsetWidth.getValue();
        var dh = sliderOffsetHeight.getValue();
        log.trace("updateSliceImage({}, {}, {})", (int) plane, dw, dh);

        var regImage = images.sliceAtPlane(plane);
        var anchor = imageView.anchor.get();
        if (anchor != null) {
            regImage = regImage.withAnchor(anchor);
        }
        image = regImage.withOffset(dw, dh);

        imageView.draw(image);
        stage.sizeToScene();
    }

    private void onMouseExitedInSlice(MouseEvent e) {
        labelMouseInformation.setText("");
        labelStructure.setText("");
    }

    private void onMouseMovingInSlice(MouseEvent e) {
        var image = this.image;
        if (image == null) {
            labelMouseInformation.setText("");
            labelStructure.setText("");
            return;
        }

        updateMouseInformation(e);
    }

    private SliceCoordinate getCoordinate(MouseEvent e) {
        var p = imageView.painter.getImageTransform().transform(e.getX(), e.getY());
        return image.planeAt(p);
    }

    private void onMouseClickedInSlice(MouseEvent e) {
        var image = this.image;
        if (image == null) return;

        if (e.getButton() == MouseButton.PRIMARY) {
            imageView.anchor.setValue(getCoordinate(e));
            updateSliceImage();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            imageView.anchor.setValue(null);
            updateSliceImage();
        }
    }


    private void updateAnchorInformation() {
        var image = this.image;
        if (image == null) {
            labelAnchorInformation.setText("");
            return;
        }

        var anchor = imageView.anchor.get();
        if (anchor == null) {
            labelAnchorInformation.setText("");
            return;
        }

        var coor = image.pullBack(anchor);
        var text = String.format("[anchor] (%.0f, %.0f, %.0f)", coor.ap(), coor.dv(), coor.ml());

        labelAnchorInformation.setText(text);
    }

    private void updateMouseInformation(MouseEvent e) {
        var coor = image.pullBack(getCoordinate(e));
        var text = String.format("[mouse] (%.0f, %.0f, %.0f)", coor.ap(), coor.dv(), coor.ml());

        labelMouseInformation.setText(text);
        updateStructureInformation(coor);
    }

    private void updateStructureInformation(Coordinate coor) {
        Thread.ofVirtual().name("updateStructureInformation").start(() -> {
            Structure structure = null;
            try {
                structure = brain.structureFromCoords(coor);
            } catch (Exception e) {
                log.warn("updateStructureInformation fail", e);
            }

            String text;
            if (structure == null) {
                text = "";
            } else {
                text = brain.structures().parents(structure).reversed().stream()
                  .map(Structure::acronym)
                  .collect(Collectors.joining(" / "));

                var hem = brain.hemisphereFromCoords(coor);
                if (hem != null) {
                    text += " [" + hem.name() + "]";
                }
            }

            var finalText = text;
            Platform.runLater(() -> labelStructure.setText(finalText));
        });
    }
}
