package io.ast.jneurocarto.atlas.gui;


import java.io.IOException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.*;
import javafx.application.Application;
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

public class AtlasBrainSliceApplication {

    private final BrainAtlas brain;
    private final ImageVolume volume;

    public final SimpleIntegerProperty maxOffsetProperty = new SimpleIntegerProperty(1000);

    private Stage stage;
    private Label labelMouseInformation;
    private Label labelAnchorInformation;
    private Label labelStructure;
    private ToggleGroup groupProjection;
    private RadioButton btnCoronal;
    private RadioButton btnSagittal;
    private RadioButton btnTransverse;
    private Button btnResetOffsetWidth;
    private Button btnResetOffsetHeight;
    private Slider sliderPlane;
    private Slider sliderOffsetWidth;
    private Slider sliderOffsetHeight;
    private Label labelPlane;
    private Label labelOffsetWidth;
    private Label labelOffsetHeight;
    private AtlasBrainSliceView imageView;

    private ImageSlices.Projection currentProjection;
    private ImageSlices images;
    private ImageSlice image;

    private Logger log;

    public AtlasBrainSliceApplication(BrainAtlas brain) {
        super();

        this.brain = brain;

        ImageVolume volume;
        try {
            volume = brain.reference();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.volume = new ImageVolume(volume);
        this.volume.normalizeGrayLevel();

        log = LoggerFactory.getLogger(AtlasBrainSliceApplication.class);

        Thread.ofVirtual().start(() -> {
            var start = System.currentTimeMillis();
            log.debug("pre loading annotations");
            try {
                brain.annotation(); // pre load
            } catch (IOException e) {
                log.warn("pre load fail", e);
            }
            var pass = System.currentTimeMillis() - start;
            log.debug("pre loaded annotations. use {} sec", String.format("%.4f", (double) pass / 1000));
        });
    }

    public void launch() {
        if (App.APPLICATION != null) throw new RuntimeException();
        App.APPLICATION = this;
        log.debug("launch");
        Application.launch(App.class);
    }

    public static class App extends Application {
        static AtlasBrainSliceApplication APPLICATION;

        @Override
        public void start(Stage primaryStage) {
            APPLICATION.start(primaryStage);
        }
    }

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
        imageView = new AtlasBrainSliceView(600, 500);
        imageView.setOnMouseClicked(this::onMouseClickedInSlice);
        imageView.setOnMouseMoved(this::onMouseMovingInSlice);
        imageView.setOnMouseExited(this::onMouseExitedInSlice);
        imageView.anchor.addListener((_, _, _) -> updateAnchorInformation());
        return imageView;
    }

    private Node controlView() {
        var layout = new GridPane();
//        layout.setGridLinesVisible(true);
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(new Label("Projection"), 0, 0);
        layout.add(new Label("Plane (mm)"), 0, 1);
        layout.add(new Label("d(Width) (um)"), 0, 2);
        layout.add(new Label("d(Height) (um)"), 0, 3);

        layout.add(projectView(), 1, 0, 2, 1);
        layout.add(bindSliderAndLabel(sliderPlane = newSlider(), labelPlane = new Label()), 1, 1);
        layout.add(bindSliderAndLabel(sliderOffsetWidth = newSlider(), labelOffsetWidth = new Label()), 1, 2);
        layout.add(bindSliderAndLabel(sliderOffsetHeight = newSlider(), labelOffsetHeight = new Label()), 1, 3);

        layout.add(btnResetOffsetWidth = newSliderResetButton(), 2, 2);
        layout.add(btnResetOffsetHeight = newSliderResetButton(), 2, 3);

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
        }

        slider.valueProperty().addListener((_, _, value) -> {
            String text;
            if (slider == sliderPlane) {
                text = String.format("%.2f mm", value.doubleValue());
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
            changeProjection(ImageSlices.Projection.coronal);
        } else if (source == btnSagittal) {
            changeProjection(ImageSlices.Projection.sagittal);
        } else if (source == btnTransverse) {
            changeProjection(ImageSlices.Projection.transverse);
        }
    }

    private void onResetButtonPressed(ActionEvent e) {
        var source = e.getSource();
        if (source == btnResetOffsetWidth) {
            sliderOffsetWidth.setValue(0);
        } else if (source == btnResetOffsetHeight) {
            sliderOffsetHeight.setValue(0);
        }
    }

    private void onSliderMoved() {
        if (images != null) {
            updateSliceImage();
            updateAnchorInformation();
        }
    }

    public ImageSlices.Projection getProjection() {
        return currentProjection;
    }

    public void setProjection(ImageSlices.Projection projection) {
        Platform.runLater(() -> {
            switch (projection) {
            case coronal -> groupProjection.selectToggle(btnCoronal);
            case sagittal -> groupProjection.selectToggle(btnSagittal);
            case transverse -> groupProjection.selectToggle(btnTransverse);
            }
            log.debug("setProjection({})", projection);
        });
    }

    private void changeProjection(ImageSlices.Projection projection) {
        if (currentProjection == projection && images != null) return;
        log.debug("changeProjection({})", projection);

        currentProjection = projection;

        var image = this.image;
        images = new ImageSlices(brain, volume, projection);

        var maxPlaneLength = images.planeUm() / 1000;
        sliderPlane.setMax(maxPlaneLength);

        var anchor = imageView.anchor.get();
        if (anchor == null || image == null) {
            if (projection == ImageSlices.Projection.sagittal) {
                sliderPlane.setValue(maxPlaneLength / 2);
            } else {
                sliderPlane.setValue(0);
            }
        } else {
            var coor = image.pullBack(image.planeAt(anchor));
            sliderPlane.setValue(projection.get(coor, projection.p) / 1000);
            imageView.anchor.setValue(this.image.project(coor));
        }

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

        imageView.draw(this.image);
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

        var coor = image.pullBack(image.planeAt(getCoordinate(e)));
        var text = String.format("[mouse] (%.0f, %.0f, %.0f)", coor.ap(), coor.dv(), coor.ml());

        labelMouseInformation.setText(text);
        updateStructureInformation(coor);
    }

    private ImageSlices.Coordinate getCoordinate(MouseEvent e) {
        var mx = e.getX();
        var my = e.getY();
        var x = image.width() * mx / imageView.getWidth();
        var y = image.height() * my / imageView.getHeight();
        return new ImageSlices.Coordinate(0, x, y);
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

        var coor = image.pullBack(image.planeAt(anchor));
        var text = String.format("[anchor] (%.0f, %.0f, %.0f)", coor.ap(), coor.dv(), coor.ml());

        labelAnchorInformation.setText(text);
    }

    private volatile Thread updateStructureInformationTask;

    private void updateStructureInformation(BrainAtlas.Coordinate coor) {
        if (updateStructureInformationTask != null) return;

        updateStructureInformationTask = Thread.ofVirtual().start(() -> {
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
            }

            Platform.runLater(() -> labelStructure.setText(text));

            updateStructureInformationTask = null;
        });
    }
}
