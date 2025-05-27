package io.ast.jneurocarto.javafx.atlas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.*;
import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.LogMessageService;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.utils.IOAction;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.StateView;

@NullMarked
public class AtlasPlugin extends InvisibleView implements Plugin, StateView<AtlasBrainViewState> {

    private BrainGlobeDownloader.DownloadResult download;
    private @Nullable BrainAtlas brain;
    private @Nullable ImageVolume volume;
    private ProbeView<?> canvas;
    private SlicePainter painter;

    private final Logger log = LoggerFactory.getLogger(AtlasPlugin.class);

    public AtlasPlugin(CartoConfig config) {
        download = AtlasBrainService.loadAtlas(config);
    }

    @Override
    public String name() {
        return "Brain Atlas";
    }

    /*============*
     * properties *
     *============*/

    public final ObjectProperty<ImageSliceStack.Projection> projection = new SimpleObjectProperty<>();

    public final ImageSliceStack.Projection getProjection() {
        return projection.get();
    }

    public final void setProjection(ImageSliceStack.Projection value) {
        projection.set(value);
    }

    /*=================*
     * state load/save *
     *=================*/

    private List<String> regions = new ArrayList<>();
    private List<CoordinateLabel> labels = new ArrayList<>();

    @Override
    public @Nullable AtlasBrainViewState getState() {
        if (brain == null || image == null) {
            log.debug("save nothing");
            return null;
        }

        log.debug("save");

        var state = new AtlasBrainViewState();
        state.name = brain.name();
        state.projection = projection.get();
        state.plane = image.plane();
        state.offsetWidth = image.dw();
        state.offsetHeight = image.dh();
        state.imagePosX = painter.x();
        state.imagePosY = painter.y();
        state.imageScaleX = painter.sx();
        state.imageScaleY = painter.sy();
        state.imageRoration = painter.r();
        state.imageAlpha = painter.getImageAlpha();
        state.showImage = painter.isVisible();
        state.regions.addAll(regions);
        state.labels.addAll(labels);
        return state;
    }

    @Override
    public void restoreState(@Nullable AtlasBrainViewState state) {
        if (state == null || brain == null) {
            log.debug("restore nothing");
            return;
        }

        log.debug("restore");

        if (state.name == null) {
            log.info("restore with null atlas name. skip.");
            return;
        } else if (!Objects.equals(brain.name(), state.name)) {
            log.info("restore with {} atlas name. skip.", state.name);
            return;
        }

        setProjection(state.projection);

        assert images != null;
        setPlaneIndex(state.plane);
        setOffsetWidthHeight(state.offsetWidth, state.offsetHeight);

        labels.clear();
        labels.addAll(state.labels); // TODO transform

        painter.x(state.imagePosX);
        painter.y(state.imagePosY);
        painter.sx(state.imageScaleX);
        painter.sy(state.imageScaleY);
        painter.r(state.imageRoration);
        painter.setImageAlpha(state.imageAlpha);
        painter.setVisible(state.showImage);
        canvas.repaintBackground();

        regions.clear();
        regions.addAll(state.regions);
    }

    /*===========================*
     * BrainAtlas initialization *
     *===========================*/

    private void checkBrainAtlas() {
        log.debug("checkBrainAtlas");

        if (!download.hasError() || download.isDownloaded()) {
            try {
                brain = download.get();
                loadBrainAtlasImageVolume();
            } catch (IOException e) {
                LogMessageService.printMessage("fail open atlas brain.");
                log.warn("checkBrainAtlas", e);
            }

        } else if (download.isDownloading()) {
            LogMessageService.printMessage("atlas brain is downloading.");

            download.waitDownloadCompleted().whenComplete((result, error) -> {
                if (error != null) {
                    LogMessageService.printMessage("during waiting atlas brain downloading.");
                    log.warn("waitDownloadCompleted", error);
                } else if (result != null) {
                    download = result;
                    checkBrainAtlas();
                }
            });
        } else if (download.isDownloadFailed()) {
            LogMessageService.printMessage("fail download atlas brain.");
        } else {
            LogMessageService.printMessage("fail download atlas brain.");
            log.warn("checkBrainAtlas", download.error());
        }
    }

    private void loadBrainAtlasImageVolume() {
        var brain = this.brain;
        if (brain == null) return;

        IOAction.measure(log, "load reference", () -> {
            volume = new ImageVolume(brain.reference());
            volume.normalizeGrayLevel();
            Platform.runLater(() -> updateProjection(projection.get()));
        });
        IOAction.measure(log, "pre load annotation", brain::annotation);
        IOAction.measure(log, "pre load hemispheres", brain::hemispheres);
    }

    public void setPlane(double plane) {
        sliderPlane.setValue(plane / 1000);
    }

    private void setPlaneIndex(int plane) {
        assert images != null;
        sliderPlane.setValue(plane * images.resolution()[0] / 1000);
    }

    public void setOffsetWidthHeight(double dw, double dh) {
        sliderOffsetWidth.setValue(dw);
        sliderOffsetHeight.setValue(dh);
    }

    private void setOffsetWidthHeightIndex(int dw, int dh) {
        assert images != null;
        var res = images.resolution()[0];
        sliderOffsetWidth.setValue(dw * res);
        sliderOffsetHeight.setValue(dh * res);
    }

    /*===========*
     * UI layout *
     *===========*/

    private Label labelMouseInformation;
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

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");
        checkBrainAtlas();

        canvas = service.getProbeView();

        painter = new SlicePainter();
        painter.flipUD(true);
        painter.flipLR(true);
        painter.invertRotation(false);
        painter.setImageAlpha(0.5);
        canvas.addBackgroundPlotting(painter);

        var ret = super.setup(service);
//        bindInvisibleNode(setupToolbar(service));
        bindInvisibleNode(setupInformationBar(service));
        setupMenuItems(service);

        setProjection(ImageSliceStack.Projection.coronal);

        canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMouseDragged);
        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseDragged);
        canvas.addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        canvas.addEventFilter(MouseEvent.MOUSE_EXITED, this::onMouseExited);

        return ret;
    }

    private void setupMenuItems(PluginSetupService service) {
        // edit
        var setCoordinate = new MenuItem("Set atlas brain coordinate");
        setCoordinate.setOnAction(_ -> LogMessageService.printMessage("TODO"));
        service.addMenuInEdit(List.of(setCoordinate));

        // view
        var setImageAlpha = new MenuItem("Set atlas image alpha");
        setImageAlpha.setOnAction(_ -> showSetImageAlphaDialog());

        var showCoorInfo = new CheckMenuItem("Show cursor coordinate");
        showCoorInfo.selectedProperty().bindBidirectional(labelMouseInformation.visibleProperty());

        var showStructInfo = new CheckMenuItem("Show structure information");
        showStructInfo.selectedProperty().bindBidirectional(labelStructure.visibleProperty());

        service.addMenuInView(List.of(setImageAlpha, showCoorInfo, showStructInfo));

        // help
        var about = new MenuItem("About - " + name());
        about.setOnAction(e -> LogMessageService.printMessage("""
          Atlas Brain - %s
          provider by io.ast.jneurocarto.javafx/io.ast.jneurocarto.javafx.atlas.AtlasPlugin
          """.formatted(download.atlasNameVersion())));
        service.addMenuInHelp(about);
    }

    private Node setupToolbar(PluginSetupService service) {
        //XXX Unsupported Operation AtlasPlugin.setupToolbar
        throw new UnsupportedOperationException();
    }

    private Node setupInformationBar(PluginSetupService service) {
        var coorInformation = new Label("(AP, DV, ML) um");
        labelMouseInformation = new Label("");

        labelStructure = new Label("");

        var infoLayout = new HBox(coorInformation, labelMouseInformation);
        coorInformation.visibleProperty().bind(labelMouseInformation.visibleProperty());
        infoLayout.visibleProperty().bind(labelMouseInformation.visibleProperty());
        infoLayout.managedProperty().bind(labelMouseInformation.visibleProperty());

        var layout = new VBox(infoLayout, labelStructure);
        labelStructure.managedProperty().bind(labelStructure.visibleProperty());

        service.addBelowProbeView(layout);

        return layout;
    }

    @Override
    protected HBox setupHeading(PluginSetupService service) {
        var layout = super.setupHeading(service);

        var showImageSwitch = new CheckBox("Show image");
        showImageSwitch.selectedProperty().bindBidirectional(painter.visible);
        showImageSwitch.selectedProperty().addListener((_, _, _) -> updateSliceImage());
        visible.addListener((_, _, e) -> showImageSwitch.setSelected(e));

        layout.getChildren().add(showImageSwitch);
        layout.setSpacing(10);

        return layout;
    }

    @Override
    protected Node setupContent(PluginSetupService service) {
        var layout = new GridPane();
//        layout.setGridLinesVisible(true);
        layout.setMinWidth(600);
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(new Label("Projection"), 0, 0);
        layout.add(new Label("Plane (mm)"), 0, 1);
        layout.add(new Label("Rotation (deg)"), 0, 2);
        layout.add(new Label("d(Width) (um)"), 0, 3);
        layout.add(new Label("d(Height) (um)"), 0, 4);

        sliderPlane = newSlider();
        sliderPlane.setMajorTickUnit(1);

        sliderRotation = newSlider();
        sliderRotation.setMin(-90);
        sliderRotation.setMax(90);
        sliderRotation.setMajorTickUnit(15);
        sliderRotation.valueProperty().bindBidirectional(painter.r);

        sliderOffsetWidth = newSlider();
        sliderOffsetWidth.setMin(-1000);
        sliderOffsetWidth.setMax(1000);
        sliderOffsetWidth.setMajorTickUnit(200);

        sliderOffsetHeight = newSlider();
        sliderOffsetHeight.setMin(-1000);
        sliderOffsetHeight.setMax(1000);
        sliderOffsetHeight.setMajorTickUnit(200);

        layout.add(setupProjectButtons(), 1, 0, 2, 1);
        layout.add(sliderPlane, 1, 1);
        layout.add(sliderRotation, 1, 2);
        layout.add(sliderOffsetWidth, 1, 3);
        layout.add(sliderOffsetHeight, 1, 4);

        layout.add(sliderBoundLabel(sliderPlane), 2, 1);
        layout.add(sliderBoundLabel(sliderRotation), 2, 2);
        layout.add(sliderBoundLabel(sliderOffsetWidth), 2, 3);
        layout.add(sliderBoundLabel(sliderOffsetHeight), 2, 4);

        btnResetRotation = new Button("Reset");
        btnResetRotation.setOnAction(_ -> sliderRotation.setValue(0));

        btnResetOffsetWidth = new Button("Reset");
        btnResetOffsetWidth.setOnAction(_ -> sliderOffsetWidth.setValue(0));

        btnResetOffsetHeight = new Button("Reset");
        btnResetOffsetHeight.setOnAction(_ -> sliderOffsetHeight.setValue(0));

        layout.add(btnResetRotation, 3, 2);
        layout.add(btnResetOffsetWidth, 3, 3);
        layout.add(btnResetOffsetHeight, 3, 4);

        var c1 = new ColumnConstraints(100, 100, 200);
        c1.setHalignment(HPos.RIGHT);

        var c2 = new ColumnConstraints(400, 400, Double.MAX_VALUE);
        c2.setHgrow(Priority.ALWAYS);

        var c3 = new ColumnConstraints(60, 60, 60);
        c3.setHalignment(HPos.RIGHT);

        var c4 = new ColumnConstraints(60, 60, 60);
        c4.setHalignment(HPos.CENTER);

        layout.getColumnConstraints().addAll(c1, c2, c3, c4);

        return layout;
    }

    private Node setupProjectButtons() {
        var layout = new HBox();

        groupProjection = new ToggleGroup();
        btnCoronal = new RadioButton("Coronal");
        btnCoronal.setToggleGroup(groupProjection);
        btnCoronal.setOnAction(e -> projection.set(ImageSliceStack.Projection.coronal));

        btnSagittal = new RadioButton("Sagittal");
        btnSagittal.setToggleGroup(groupProjection);
        btnSagittal.setOnAction(e -> projection.set(ImageSliceStack.Projection.sagittal));

        btnTransverse = new RadioButton("Transverse");
        btnTransverse.setToggleGroup(groupProjection);
        btnTransverse.setOnAction(e -> projection.set(ImageSliceStack.Projection.transverse));

        layout.getChildren().addAll(btnCoronal, btnSagittal, btnTransverse);

        projection.addListener((_, _, proj) -> {
            log.debug("setProjection({})", proj);

            switch (proj) {
            case coronal -> groupProjection.selectToggle(btnCoronal);
            case sagittal -> groupProjection.selectToggle(btnSagittal);
            case transverse -> groupProjection.selectToggle(btnTransverse);
            }

            updateProjection(proj);
        });

        return layout;
    }

    private Slider newSlider() {
        var slider = new Slider();
        slider.setShowTickLabels(true);
        slider.valueProperty().addListener((_, _, _) -> onSliderMoved());
        return slider;
    }

    private Label sliderBoundLabel(Slider slider) {
        var label = new Label();

        slider.valueProperty().addListener((_, _, value) -> {
            updateSliderLabel(slider, label, value.doubleValue());
        });

        updateSliderLabel(slider, label, 0);

        return label;
    }

    private void updateSliderLabel(Slider slider, Label label, double value) {
        String text;
        if (slider == sliderPlane) {
            text = String.format("%.1f mm", value);
        } else if (slider == sliderRotation) {
            text = String.format("%.1f deg", value);
        } else {
            text = String.format("%.0f um", value);
        }

        label.setText(text);
    }


    /*==============*
     * event handle *
     *==============*/

    private @Nullable MouseEvent mousePress;
    private @Nullable MouseEvent mouseMoved;

    private void onSliderMoved() {
        if (images != null) {
            updateSliceImage();
        }
    }

    private void onMouseDragged(MouseEvent e) {
        if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
            if (e.getButton() == MouseButton.SECONDARY && e.isShiftDown()) {
                mousePress = e;
            }
        } else if (e.getEventType() == MouseEvent.MOUSE_RELEASED) {
            mousePress = null;
            mouseMoved = null;
        } else if (e.getEventType() == MouseEvent.MOUSE_DRAGGED && mousePress != null) {
            var prev = mouseMoved == null ? mousePress : mouseMoved;
            var dx = e.getX() - prev.getX();
            var dy = e.getY() - prev.getY();
            var q = canvas.getChartTransformScaling(dx, dy);
            painter.translate(q.getX(), q.getY());
            mouseMoved = e;
            e.consume();
            updateSliceImage();
        }
    }

    private void onMouseMoved(MouseEvent e) {
        var image = this.image;
        if (image == null) {
            labelMouseInformation.setText("");
            labelStructure.setText("");
            return;
        }

        var p = new Point2D(e.getX(), e.getY()); // canvas
        p = canvas.getChartTransform(p); // chart <- canvas
        p = painter.getImageTransform().transform(p); // slice <- chart
        var coor = image.pullBack(image.planeAt(p)); // coor <- slice
        var text = String.format("=(%.0f, %.0f, %.0f)", coor.ap(), coor.dv(), coor.ml());

        labelMouseInformation.setText(text);
        updateStructureInformation(coor);
    }

    private void onMouseExited(MouseEvent e) {
        labelMouseInformation.setText("");
        labelStructure.setText("");
    }

    private SliceCoordinate getSliceCoordinate(MouseEvent e) {
        var mx = e.getX();
        var my = e.getY();
        var p = canvas.getChartTransform(mx, my);
        var x = (p.getX() - painter.x()) / painter.sx();
        var y = (p.getY() - painter.y()) / painter.sy();
        return new SliceCoordinate(0, x, y);
    }

    /*=====================*
     * atlas brain drawing *
     *=====================*/

    private @Nullable ImageSliceStack images;
    private @Nullable ImageSlice image;

    private void updateProjection(ImageSliceStack.Projection projection) {
        var brain = this.brain;
        var volume = this.volume;
        if (brain == null || volume == null) return;

        log.debug("updateProjection({})", projection);
        images = new ImageSliceStack(brain, volume, projection);
        canvas.setResetAxesBoundaries(0, images.widthUm(), 0, images.heightUm());
        canvas.resetAxesBoundaries();
        canvas.setAxesEqualRatio();

        var maxPlaneLength = images.planeUm() / 1000;
        sliderPlane.setMax(maxPlaneLength);

        sliderRotation.setValue(0);
        sliderOffsetWidth.setValue(0);
        sliderOffsetHeight.setValue(0);
        sliderOffsetWidth.setBlockIncrement(images.resolution()[1]);
        sliderOffsetHeight.setBlockIncrement(images.resolution()[2]);

        updateSliceImage();
    }

    private void updateSliceImage() {
        if (images == null) return;

        var plane = sliderPlane.getValue() * 1000;
        var dw = sliderOffsetWidth.getValue();
        var dh = sliderOffsetHeight.getValue();

        log.trace("updateSliceImage({}, {}, {})", (int) plane, dw, dh);

        var image = images.sliceAtPlane(plane);
        this.image = image = image.withOffset(dw, dh);
        painter.update(image);
        canvas.repaintBackground();
    }

    /*=================*
     * information bar *
     *=================*/

    private void updateStructureInformation(Coordinate coor) {
        var brain = this.brain;
        if (brain == null) return;

        Thread.ofVirtual().start(() -> {
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

    /*=========*
     * dialogs *
     *=========*/

    private void showSetImageAlphaDialog() {
        var dialog = new Dialog<>();
        dialog.setTitle("Set Atlas brain image alpha value");

        var oldAlpha = painter.getImageAlpha();

        var alpha = new Slider(0, 1, oldAlpha);
        alpha.setShowTickLabels(true);
        alpha.setShowTickMarks(true);
        alpha.setMajorTickUnit(0.2);
        alpha.setMinorTickCount(1);
        alpha.setSnapToTicks(true);
        alpha.valueProperty().addListener((_, _, v) -> {
            painter.setImageAlpha((double) Math.round(v.doubleValue() * 10) / 10);
            canvas.repaintBackground();
        });

        var layout = new VBox(new Label("Alpha"), alpha);
        layout.setSpacing(5);

        dialog.getDialogPane().setContent(layout);

        var okay = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        var cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancel, okay);

        dialog.setOnCloseRequest(_ -> {
            var result = dialog.getResult();
            if (result != okay) {
                painter.setImageAlpha(oldAlpha);
            }
            canvas.repaintBackground();
        });

        dialog.show();
    }

}
