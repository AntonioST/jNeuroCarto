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
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class AtlasBrainSliceApplication {

    private final BrainAtlas brain;
    private final ImageVolume volume;

    public final SimpleIntegerProperty maxOffsetProperty = new SimpleIntegerProperty(1000);

    private Stage stage;
    private Label labelInformation;
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

    private ImageSlices.View currentProjection;
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

        labelInformation = new Label("");

        var control = controlView();
        VBox.setMargin(control, new Insets(5, 5, 15, 5));

        root.getChildren().addAll(slice, labelInformation, control);

        return root;
    }

    private Node sliceView() {
        imageView = new AtlasBrainSliceView(600, 500);
        imageView.setOnMouseMoved(this::onMouseMovingInSlice);
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
        slider.valueProperty().addListener((_, _, value) -> onSliderMoved(slider, value.doubleValue()));
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
            changeProjection(ImageSlices.View.coronal);
        } else if (source == btnSagittal) {
            changeProjection(ImageSlices.View.sagittal);
        } else if (source == btnTransverse) {
            changeProjection(ImageSlices.View.transverse);
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

    private void onSliderMoved(Slider source, double value) {
        if (images != null) {
            updateSliceImage();
        }
    }

    public ImageSlices.View getProjection() {
        return currentProjection;
    }

    public void setProjection(ImageSlices.View projection) {
        Platform.runLater(() -> {
            switch (projection) {
            case coronal -> groupProjection.selectToggle(btnCoronal);
            case sagittal -> groupProjection.selectToggle(btnSagittal);
            case transverse -> groupProjection.selectToggle(btnTransverse);
            }
            log.debug("setProjection({})", projection);
        });
    }

    private void changeProjection(ImageSlices.View projection) {
        if (currentProjection == projection && images != null) return;
        log.debug("changeProjection({})", projection);

        currentProjection = projection;

        images = new ImageSlices(brain, volume, projection);

        var maxPlaneLength = images.planeUm() / 1000;
        sliderPlane.setMax(maxPlaneLength);
        if (projection == ImageSlices.View.sagittal) {
            sliderPlane.setValue(maxPlaneLength / 2);
        } else {
            sliderPlane.setValue(0);
        }
        sliderOffsetWidth.setBlockIncrement(images.resolution()[1]);
        sliderOffsetHeight.setBlockIncrement(images.resolution()[2]);

        updateSliceImage();
    }

    private void updateSliceImage() {
        var plane = sliderPlane.getValue() * 1000;
        var dw = sliderOffsetWidth.getValue();
        var dh = sliderOffsetHeight.getValue();
        log.trace("updateSliceImage({}, {}, {})", (int) plane, dw, dh);

        image = images.sliceAtPlace(plane).withOffset(dw, dh);

        imageView.draw(this.image);
        stage.sizeToScene();
    }

    private void onMouseMovingInSlice(MouseEvent e) {
        var image = this.image;
        if (image == null) {
            labelInformation.setText("");
            return;
        }

        var mx = e.getX();
        var my = e.getY();
        var x = image.width() * mx / imageView.getWidth();
        var y = image.height() * my / imageView.getHeight();
        var coor = image.pullBack(image.planeAt(new ImageSlices.Coordinate(0, x, y)));
        var text = String.format("AP=%.0f um, DV=%.0f um, ML=%.0f um", coor.ap(), coor.dv(), coor.ml());
        labelInformation.setText(text);
    }
}
