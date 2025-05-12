package io.ast.jneurocarto.atlas.gui;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.atlas.ImageSlices;
import io.ast.jneurocarto.atlas.ImageVolume;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class AtlasBrainSliceApplication {

    public enum UseImage {
        reference, annotation
    }

    private final BrainAtlas brain;

    private RadioButton btnCoronal;
    private RadioButton btnSagittal;
    private RadioButton btnTransverse;
    private Button btnResetPlane;
    private Button btnResetOffsetWidth;
    private Button btnResetOffsetHeight;
    private RadioButton btnImageRef;
    private RadioButton btnImageAnn;
    private Slider sliderPlane;
    private Slider sliderOffsetWidth;
    private Slider sliderOffsetHeight;
    private Label sliderPlaneLabel;
    private Label sliderOffsetWidthLabel;
    private Label sliderOffsetHeightLabel;
    private AtlasBrainSliceView imageView;

    private ImageSlices.View currentProjection;
    private UseImage currentUseVolumeImage;
    private ImageSlices images;
    private ImageSlice image;

    private Logger log;

    public AtlasBrainSliceApplication(BrainAtlas brain) {
        super();

        this.brain = brain;
        log = LoggerFactory.getLogger(AtlasBrainSliceApplication.class);
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
        log.debug("start");
        stage.setTitle("AtlasBrainSlice");
        stage.setScene(scene());
        stage.show();
    }

    private Scene scene() {
        var scene = new Scene(root(), 600, 600);
        return scene;
    }

    private Parent root() {
        var root = new VBox();

        var slice = sliceView();
        VBox.setVgrow(slice, Priority.ALWAYS);

        var control = controlView();
        VBox.setMargin(control, new Insets(5, 5, 5, 5));

        root.getChildren().addAll(slice, control);

        return root;
    }

    private Node sliceView() {
        imageView = new AtlasBrainSliceView();
        return imageView;
    }

    private Node controlView() {
        var layout = new GridPane();
//        layout.setGridLinesVisible(true);
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(new Label("Project"), 0, 0);
        layout.add(new Label("Plane"), 0, 1);
        layout.add(new Label("d(Width)"), 0, 2);
        layout.add(new Label("d(Height)"), 0, 3);
        layout.add(new Label("image"), 0, 4);

        layout.add(projectView(), 1, 0);
        layout.add(planeSliceView(), 1, 1);
        layout.add(widthOffsetSliceView(), 1, 2);
        layout.add(heightOffsetSliceView(), 1, 3);
        layout.add(imageSelectView(), 1, 4);

        var c1 = new ColumnConstraints(100, 100, 200);
        c1.setHalignment(HPos.RIGHT);

        var c2 = new ColumnConstraints(400, 400, Double.MAX_VALUE);
        c2.setHgrow(Priority.ALWAYS);


        layout.getColumnConstraints().addAll(c1, c2);

        return layout;
    }

    private Node projectView() {
        var layout = new HBox();

        var group = new ToggleGroup();
        btnCoronal = new RadioButton("Coronal");
        btnCoronal.setToggleGroup(group);
        btnCoronal.setOnAction(this::onProjectButtonPressed);

        btnSagittal = new RadioButton("Sagittal");
        btnSagittal.setToggleGroup(group);
        btnSagittal.setOnAction(this::onProjectButtonPressed);

        btnTransverse = new RadioButton("Transverse");
        btnTransverse.setToggleGroup(group);
        btnTransverse.setOnAction(this::onProjectButtonPressed);

        layout.getChildren().addAll(btnCoronal, btnSagittal, btnTransverse);

        return layout;
    }

    private Node planeSliceView() {
        var layout = new HBox();

        btnResetPlane = new Button("Reset");
        btnResetPlane.setOnAction(this::onResetButtonPressed);

        sliderPlane = new Slider();
        sliderPlane.setMin(0);
        sliderPlane.setMax(100);
        sliderPlane.valueProperty().addListener((_, _, value) -> onSliderMoved(sliderPlane, value.doubleValue()));

        sliderPlaneLabel = new Label("0 um");

        HBox.setHgrow(sliderPlane, Priority.ALWAYS);

        layout.getChildren().addAll(sliderPlaneLabel, sliderPlane, btnResetPlane);

        return layout;
    }

    private Node widthOffsetSliceView() {
        var layout = new HBox();

        btnResetOffsetWidth = new Button("Reset");
        btnResetOffsetWidth.setOnAction(this::onResetButtonPressed);

        sliderOffsetWidth = new Slider();
        sliderOffsetWidth.setMin(-100);
        sliderOffsetWidth.setMax(100);
        sliderOffsetWidth.setValue(0);
        sliderOffsetWidth.valueProperty().addListener((_, _, value) -> onSliderMoved(sliderOffsetWidth, value.doubleValue()));
        HBox.setHgrow(sliderOffsetWidth, Priority.ALWAYS);

        sliderOffsetWidthLabel = new Label("0 um");

        layout.getChildren().addAll(sliderOffsetWidthLabel, sliderOffsetWidth, btnResetOffsetWidth);

        return layout;
    }

    private Node heightOffsetSliceView() {
        var layout = new HBox();

        btnResetOffsetHeight = new Button("Reset");
        btnResetOffsetHeight.setOnAction(this::onResetButtonPressed);

        sliderOffsetHeight = new Slider();
        sliderOffsetHeight.setMin(-100);
        sliderOffsetHeight.setMax(100);
        sliderOffsetHeight.setValue(0);
        sliderOffsetHeight.valueProperty().addListener((_, _, value) -> onSliderMoved(sliderOffsetHeight, value.doubleValue()));
        HBox.setHgrow(sliderOffsetHeight, Priority.ALWAYS);

        sliderOffsetHeightLabel = new Label("0 um");

        layout.getChildren().addAll(sliderOffsetHeightLabel, sliderOffsetHeight, btnResetOffsetHeight);

        return layout;
    }

    private Node imageSelectView() {
        var layout = new HBox();

        var group = new ToggleGroup();
        btnImageRef = new RadioButton("Reference");
        btnImageRef.setToggleGroup(group);
        btnImageRef.setOnAction(this::onImageButtonPressed);

        btnImageAnn = new RadioButton("Annotation");
        btnImageAnn.setToggleGroup(group);
        btnImageAnn.setOnAction(this::onImageButtonPressed);

        layout.getChildren().addAll(btnImageRef, btnImageAnn);

        return layout;
    }

    private void onProjectButtonPressed(ActionEvent e) {
        var source = e.getSource();
        if (source == btnCoronal) {
            changeProjection(ImageSlices.View.coronal);
        } else if (source == btnSagittal) {
            changeProjection(ImageSlices.View.sagittal);
        } else if (source == btnTransverse) {
            changeProjection(ImageSlices.View.transverse);
        }
    }

    private void onResetButtonPressed(ActionEvent e) {
        var source = e.getSource();
        if (source == btnResetPlane) {

        } else if (source == btnResetOffsetWidth) {

        } else if (source == btnResetOffsetHeight) {
        }
    }


    private void onImageButtonPressed(ActionEvent e) {
        var source = e.getSource();
        if (source == btnImageRef) {
            changeVolumeImage(UseImage.reference);
        } else if (source == btnImageAnn) {
            changeVolumeImage(UseImage.annotation);
        }
    }

    private void onSliderMoved(Slider source, double value) {
        if (source == sliderPlane) {
            sliderPlaneLabel.setText(String.format("%.0f um", value));
        } else if (source == sliderOffsetWidth) {
            sliderOffsetWidthLabel.setText(String.format("%.2f um", value));
        } else if (source == sliderOffsetHeight) {
            sliderOffsetHeightLabel.setText(String.format("%.2f um", value));
        }
    }

    public UseImage getUseVolumeImage() {
        return currentUseVolumeImage;
    }

    public void setUseVolumeImage(UseImage use) {
        Platform.runLater(() -> changeVolumeImage(use));
    }

    private void changeVolumeImage(UseImage use) {
        if (currentUseVolumeImage == use && images != null) return;
        log.debug("changeVolumeImage({})", use);

        currentUseVolumeImage = use;
        var projection = currentProjection;
        projection = projection == null ? ImageSlices.View.coronal : projection;
        changeProjection(projection);
    }

    public ImageSlices.View getProjection() {
        return currentProjection;
    }

    public void setProjection(ImageSlices.View projection) {
        Platform.runLater(() -> changeProjection(projection));
    }

    private void changeProjection(ImageSlices.View projection) {
        if (currentProjection == projection && images != null) return;
        log.debug("changeProjection({})", projection);

        currentProjection = projection;

        ImageVolume image;
        try {
            image = switch (currentUseVolumeImage) {
                case null -> {
                    currentUseVolumeImage = UseImage.reference;
                    yield brain.reference();
                }
                case reference -> brain.reference();
                case annotation -> brain.annotation();
            };
        } catch (IOException e) {
            log.warn("changeProjection", e);
            return;
        }

        images = new ImageSlices(brain, image, projection);
        sliderPlane.setMax(images.planeUm());
        sliderPlane.setMajorTickUnit(images.resolution()[0]);
        sliderOffsetWidth.setMajorTickUnit(images.resolution()[1]);
        sliderOffsetHeight.setMajorTickUnit(images.resolution()[2]);

        updateSliceImage();
    }

    private void updateSliceImage() {
        var plane = sliderPlane.getValue();
        var dw = sliderOffsetWidth.getValue();
        var dh = sliderOffsetHeight.getValue();
        log.debug("updateSliceImage({}, {}, {})", plane, dw, dh);

        image = images.sliceAtPlace(plane).withOffset(dw, dh);

        imageView.draw(this.image);
    }
}
