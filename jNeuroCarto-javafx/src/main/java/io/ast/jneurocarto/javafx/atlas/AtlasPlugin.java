package io.ast.jneurocarto.javafx.atlas;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventType;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.*;
import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.ProbeCoordinate;
import io.ast.jneurocarto.core.ProbeTransform;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.core.numpy.FlatIntArray;
import io.ast.jneurocarto.javafx.app.LogMessageService;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.PluginStateService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.ImagePainter;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import io.ast.jneurocarto.javafx.utils.IOAction;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.StateView;

/// Plug for support showing [BrainAtlas] in the background.
///
/// ### Features
///
/// * showing brain atlas [ImageVolume] image in the background.
/// * project volume image in slice image, and allow to travel in different slices.
/// * image transformation like translate, rotation and Shearing
/// * display the structure information on mouse position
/// * draw structure boundary
///
/// ### Required by
///
/// This plugin is the base plugin of atlas related supporting plugins.
///
/// * [AtlasLabelPlugin] display labels in the chart that supporting different coordinate system.
/// * [ImplantPlugin] probe implant supporting.
///
/// ### Config
///
/// * (local) [AtlasBrainViewState] stores the position information automatically for each channelmap file.
///
/// * (global) [AtlasBrainGlobalViewState] store the default used reference.
/// It does not store automatically. User have to manually add the information in user config.
/// Example:
///
/// {@snippet lang = "JSON":
///     "AtlasBrainView": {
///       "default_use": "bregma",
///       "use_reference": {
///         "allen_mouse_10um": "bregma"
///       }
///     }
///}
///
/// * (global) [AtlasReferenceState] store the atlas reference information.
/// Check [AtlasReferenceService] for more details.
@NullMarked
public class AtlasPlugin extends InvisibleView implements Plugin, StateView<AtlasBrainViewState> {

    private BrainGlobeDownloader.DownloadResult download;
    private @Nullable BrainAtlas brain;
    private @Nullable ImageVolume volume;
    private final AtlasReferenceService references;
    private ProbeTransform</*global*/Coordinate, /*reference*/Coordinate> transform = ProbeTransform.identify(ProbeTransform.ANATOMICAL);
    private ProbeView<?> canvas;
    private SlicePainter painter;
    private ImagePainter maskPainter;

    private final Logger log = LoggerFactory.getLogger(AtlasPlugin.class);

    public AtlasPlugin(CartoConfig config) {
        download = AtlasBrainService.loadAtlas(config);
        references = AtlasReferenceService.loadReferences(download);
    }

    /*========*
     * getter *
     *========*/

    @Override
    public String name() {
        return "Brain Atlas";
    }

    public String atlasName() {
        return download.atlas();
    }

    public @Nullable BrainAtlas getBrainAtlas() {
        return brain;
    }

    /**
     * The current used volume image which has renormalized from origin for visualization purpose.
     *
     * @return current used volume image.
     */
    public @Nullable ImageVolume getVolume() {
        return volume;
    }

    public AtlasReferenceService getReferencesService() {
        return references;
    }

    /**
     * {@return current used slice stack in particular projection.}
     */
    public @Nullable ImageSliceStack getImageSliceStack() {
        return images;
    }

    public @Nullable ImageSliceStack getImageSliceStack(ImageSliceStack.Projection projection) {
        var brain = this.brain;
        var volume = this.volume;
        if (brain == null || volume == null) return null;
        return new ImageSliceStack(brain, volume, projection);
    }

    /**
     * {@return current drew image slice}
     */
    public @Nullable ImageSlice getImageSlice() {
        return image;
    }

    public @Nullable ImageSlice getAnnotationImageSlice() {
        var image = this.image;
        if (image == null) return null;
        var annotations = getAnnotationImageStack(getProjection());
        if (annotations == null) return null;
        return annotations.sliceAtPlane(image);
    }

    public @Nullable ImageSlice getAnnotationImageSlice(ImageSlice image) {
        var annotations = getAnnotationImageStack(image.projection());
        if (annotations == null) return null;
        return annotations.sliceAtPlane(image);
    }

    public SlicePainter painter() {
        return painter;
    }

    /*============*
     * properties *
     *============*/

    public final ObjectProperty<ImageSliceStack.Projection> projection = new SimpleObjectProperty<>(ImageSliceStack.Projection.coronal);

    {
        projection.addListener((_, _, proj) -> updateProjection(proj));
    }

    public final ImageSliceStack.Projection getProjection() {
        return projection.get();
    }

    public final void setProjection(ImageSliceStack.Projection value) {
        projection.set(value);
    }

    public final ObjectProperty<@Nullable AtlasReference> reference = new SimpleObjectProperty<>(null);
    private final StringProperty referenceNameProperty = new SimpleStringProperty("Global");

    {
        reference.addListener((_, _, ref) -> {
            var name = ref == null ? "Global" : ref.name();
            log.debug("set atlas reference to {}", name);
            referenceNameProperty.set(name);
            onAtlasReferenceUpdate(ref);
            fireAtlasImageUpdateEvent(AtlasUpdateEvent.REFERENCE);
        });
    }

    public final @Nullable String getAtlasReferenceName() {
        var ret = reference.get();
        return ret == null ? null : ret.name();
    }

    public final @Nullable AtlasReference getAtlasReference() {
        return reference.get();
    }

    public final void setAtlasReference(@Nullable String reference) {
        if (reference == null) {
            this.reference.set(null);
        } else {
            this.reference.set(references.getReference(reference));
        }

    }

    public final void setAtlasReference(AtlasReference reference) {
        this.reference.set(reference);
    }

    public record RegionMask(Structure structure, boolean exclude, boolean includeChildren) {
        public int id() {
            return structure.id();
        }

        public String name() {
            return structure.name();
        }
    }

    /*=================*
     * state load/save *
     *=================*/

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
        state.refernce = getAtlasReferenceName();

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

        setAtlasReference(state.refernce);
        setProjection(state.projection);

        assert images != null;
        setPlaneIndex(state.plane);
        setOffsetWidthHeight(state.offsetWidth, state.offsetHeight);

        painter.x(state.imagePosX);
        painter.y(state.imagePosY);
        painter.sx(state.imageScaleX);
        painter.sy(state.imageScaleY);
        painter.r(state.imageRoration);
        painter.setImageAlpha(state.imageAlpha);
        painter.setVisible(state.showImage);
        canvas.repaintBackground();
    }

    public void restoreGlobalState() {
        var state = PluginStateService.loadGlobalState(AtlasBrainGlobalViewState.class);
        if (state == null) return;

        log.debug("restore global");
        var reference = state.use_reference.get(download.atlas());
        if (reference == null) {
            reference = state.default_use;
        }

        if (reference == null || reference.isEmpty() || reference.equals("Global")) {
            reference = null;
        }

        log.debug("use reference {}", reference == null ? "Global" : reference);
        setAtlasReference(reference);
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

        fireAtlasImageUpdateEvent(AtlasUpdateEvent.LOADED);
        IOAction.measure(log, "load reference", () -> {
            volume = new ImageVolume(brain.reference());
            volume.normalizeGrayLevel();
            Platform.runLater(() -> updateProjection(projection.get()));
        });
        IOAction.measure(log, "pre load annotation", brain::annotation);
        IOAction.measure(log, "pre load hemispheres", brain::hemispheres);
    }

    private void onAtlasReferenceUpdate(@Nullable AtlasReference reference) {
        if (reference != null) {
            transform = reference.getTransform();
        } else {
            transform = ProbeTransform.identify(ProbeTransform.ANATOMICAL);
        }
//        System.out.println(transform.getTransform());
        updateProjection(projection.get());
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
        var ret = super.setup(service);
        restoreGlobalState();
        checkBrainAtlas();
        return ret;
    }

    @Override
    protected HBox setupHeading(PluginSetupService service) {
        var layout = super.setupHeading(service);

        var showImageSwitch = new CheckBox("Show image");
        showImageSwitch.selectedProperty().bindBidirectional(painter.visible);
        showImageSwitch.selectedProperty().addListener((_, _, _) -> updateSliceImage());
        visible.addListener((_, _, e) -> showImageSwitch.setSelected(e));

        layout.getChildren().add(showImageSwitch);

        return layout;
    }

    private class SetRefMenuItem extends CheckMenuItem {
        private final @Nullable AtlasReference ref;

        SetRefMenuItem(@Nullable AtlasReference ref) {
            String text;
            if (ref == null) {
                text = "Global [0,0,0]";
            } else {
                var coor = ref.coordinate();
                text = "%s [%.1f,%.1f,%.1f]".formatted(ref.name(), coor.ap() / 1000, coor.dv() / 1000, coor.ml() / 1000);
            }

            super(text);
            this.ref = ref;
            selectedProperty().addListener((_, _, e) -> {
                if (e) {
                    setAtlasReference(this.ref);
                }
            });
        }

        void setSelected(@Nullable AtlasReference ref) {
            if (this.ref == null) {
                super.setSelected(ref == null);
            } else {
                super.setSelected(Objects.equals(this.ref.name(), ref.name()));
            }
        }
    }

    protected void setupMenuItems(PluginSetupService service) {
        // edit

        // edit.Set atlas brain reference
        var setReference = new Menu("Set atlas brain reference");

        var setGlobalRef = new SetRefMenuItem(null);
        setReference.getItems().add(setGlobalRef);

        var items = references.getReferenceList().stream()
            .sorted()
            .map(references::getReference)
            .filter(Objects::nonNull)
            .map(SetRefMenuItem::new)
            .toList();

        setReference.getItems().addAll(items);
        reference.addListener((_, _, ref) -> {
            for (var item : setReference.getItems()) {
                if (item instanceof SetRefMenuItem setRef) {
                    setRef.setSelected(ref);
                }
            }
        });

        // edit.Set atlas brain coordinate
        var setCoordinate = new MenuItem("Set atlas brain coordinate");
        setCoordinate.setOnAction(_ -> LogMessageService.printMessage("TODO"));

        service.addMenuInEdit(setReference, setCoordinate);

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

        layout.add(sliderBindLabel(sliderPlane), 2, 1);
        layout.add(sliderBindLabel(sliderRotation), 2, 2);
        layout.add(sliderBindLabel(sliderOffsetWidth), 2, 3);
        layout.add(sliderBindLabel(sliderOffsetHeight), 2, 4);

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
        groupProjection.selectToggle(btnCoronal);

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
        });

        return layout;
    }

    @Override
    protected void setupChartContent(PluginSetupService service, ProbeView<?> canvas) {
        this.canvas = canvas;

        painter = new SlicePainter();
        painter.z(-50);
        painter.flipUD(true);
        painter.flipLR(true);
        painter.invertRotation(false);
        painter.setImageAlpha(0.5);
        canvas.addBackgroundPlotting(painter);

        maskPainter = new ImagePainter();
        maskPainter.bind(painter);
        maskPainter.z(-10);
//        maskPainter.blend(BlendMode.);
        canvas.addBackgroundPlotting(maskPainter);

        canvas.addEventFilter(ChartMouseEvent.CHART_MOUSE_PRESSED, this::onMouseDragged);
        canvas.addEventFilter(ChartMouseEvent.CHART_MOUSE_DRAGGED, this::onMouseDragged);
        canvas.addEventFilter(ChartMouseEvent.CHART_MOUSE_RELEASED, this::onMouseDragged);
        canvas.addEventFilter(ChartMouseEvent.CHART_MOUSE_MOVED, this::onMouseMoved);
        canvas.addEventFilter(ChartMouseEvent.CHART_MOUSE_EXITED, this::onMouseExited);
        canvas.addEventFilter(AtlasUpdateEvent.SLICING, this::onAtlasImageUpdate);
        canvas.addEventFilter(AtlasUpdateEvent.PROJECTION, this::onAtlasImageUpdate);

        // toolbar
        //        bindInvisibleNode(setupToolbar(service));
        bindInvisibleNode(setupInformationBar(service));
    }

    private Node setupToolbar(PluginSetupService service) {
        //XXX Unsupported Operation AtlasPlugin.setupToolbar
        throw new UnsupportedOperationException();
    }

    private Node setupInformationBar(PluginSetupService service) {
        var coorInformation = new Label("Global (AP, DV, ML) um");
        referenceNameProperty.addListener((_, _, name) -> {
            coorInformation.setText(name + " (AP, DV, ML) um");
        });
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

    private Slider newSlider() {
        var slider = new Slider();
        slider.setShowTickLabels(true);
        slider.valueProperty().addListener((_, _, _) -> onSliderMoved(slider));
        return slider;
    }

    private Label sliderBindLabel(Slider slider) {
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

    private @Nullable ChartMouseEvent mousePress;
    private @Nullable ChartMouseEvent mouseMoved;

    private void onSliderMoved(Slider source) {
        if (images != null) {
            canvas.repaintBackground(() -> {
                updateSliceImage();
                if (source == sliderRotation) {
                    fireAtlasImageUpdateEvent(AtlasUpdateEvent.POSITION);
                } else {
                    fireAtlasImageUpdateEvent(AtlasUpdateEvent.SLICING);
                }
            });
        }
    }

    private void onMouseDragged(ChartMouseEvent e) {
        if (e.getEventType() == ChartMouseEvent.CHART_MOUSE_PRESSED) {
            if (e.getButton() == MouseButton.SECONDARY && e.mouse.isShiftDown()) {
                mousePress = e;
            }
        } else if (e.getEventType() == ChartMouseEvent.CHART_MOUSE_RELEASED) {
            mousePress = null;
            mouseMoved = null;
        } else if (e.getEventType() == ChartMouseEvent.CHART_MOUSE_DRAGGED && mousePress != null) {
            var prev = mouseMoved == null ? mousePress : mouseMoved;
            var dx = e.getChartX() - prev.getChartX();
            var dy = e.getChartY() - prev.getChartY();
            painter.translate(dx, dy);
            mouseMoved = e;
            e.consume();

            canvas.repaintBackground(() -> {
                updateSliceImage();
                fireAtlasImageUpdateEvent(AtlasUpdateEvent.POSITION);
            });
        }
    }

    private void onMouseMoved(ChartMouseEvent e) {
        updateMouseInformation(e.point);
    }

    private void onMouseExited(ChartMouseEvent e) {
        labelMouseInformation.setText("");
        labelStructure.setText("");
    }

    private void onAtlasImageUpdate(AtlasUpdateEvent e) {
        // SLICING, POSITION
        updateMaskedRegion();
    }

    /*================*
     * atlas controls *
     *================*/

    public @Nullable Structure getRegion(int id) {
        var brain = this.brain;
        if (brain == null) return null;

        return brain.structures().get(id).orElse(null);
    }

    public @Nullable Structure getRegion(String name) {
        var brain = this.brain;
        if (brain == null) return null;

        var ret = brain.structures().get(name).orElse(null);
        if (ret != null) return ret;

        name = name.toLowerCase();
        for (var structure : brain.structures()) {
            if (structure.acronym().toLowerCase().startsWith(name)) return structure;
            if (structure.name().toLowerCase().contains(name)) return structure;
        }
        return null;
    }

    /**
     * project a global coordinate to referenced coordinate.
     *
     * @param coordinate global anatomical coordinate
     * @return referenced anatomical coordinate
     */
    public Coordinate project(Coordinate coordinate) {
        return transform.transform(coordinate);
    }

    /**
     * project a referenced coordinate to global coordinate.
     *
     * @param coordinate referenced anatomical coordinate
     * @return global anatomical coordinate
     */
    public Coordinate pullback(Coordinate coordinate) {
        return transform.inverseTransform(coordinate);
    }

    /**
     * @param coordinate referenced anatomical coordinate
     * @return slice coordinate
     */
    public SliceCoordinate projectSlice(Coordinate coordinate) {
        var images = Objects.requireNonNull(image, "projection not set");
        return images.project(pullback(coordinate));
    }

    /**
     * @param coordinate slice coordinate
     * @return referenced anatomical coordinate
     */
    public Coordinate pullbackSlice(SliceCoordinate coordinate) {
        var images = Objects.requireNonNull(image, "projection not set");
        return project(images.pullBack(coordinate));
    }


    /**
     * {@return current plane (um) in referenced anatomical space}
     */
    public double getPlane() {
        return sliderPlane.getValue() * 1000;
    }

    /**
     * {@return current plane (um) in global anatomical space}
     */
    public double getPlaneGlobal() {
        return pullbackPlane(sliderPlane.getValue() * 1000);
    }

    /**
     * @param plane plane (um) in referenced anatomical space
     */
    public void setPlane(double plane) {
        sliderPlane.setValue(plane / 1000);
    }

    /**
     * @param plane plane (um) in global anatomical space
     */
    public void setPlaneGlobal(double plane) {
        sliderPlane.setValue(projectPlane(plane) / 1000);
    }

    /**
     * @param plane plane (um) in global anatomical space
     * @return plane (um) in referenced anatomical space
     */
    public double projectPlane(double plane) {
        return switch (projection.get().p) {
            case 0 -> transform.transform(plane, 0, 0).getX();
            case 1 -> transform.transform(0, plane, 0).getY();
            case 2 -> transform.transform(0, 0, plane).getZ();
            default -> throw new RuntimeException();
        };
    }

    /**
     * @param plane plane (um) in referenced anatomical space
     * @return plane (um) in global anatomical space
     */
    public double pullbackPlane(double plane) {
        return switch (projection.get().p) {
            case 0 -> transform.inverseTransform(plane, 0, 0).getX();
            case 1 -> transform.inverseTransform(0, plane, 0).getY();
            case 2 -> transform.inverseTransform(0, 0, plane).getZ();
            default -> throw new RuntimeException();
        };
    }

    /**
     * @param plane plane index in global anatomical space
     */
    private void setPlaneIndex(int plane) {
        assert images != null;
        setPlaneGlobal(plane * images.resolution()[0]);
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

    /**
     * Move the slice to the {@code coordinate}.
     *
     * @param coordinate anatomical coordinate, follow current atlas reference
     */
    public void anchorImageTo(Coordinate coordinate) {
        if (images == null) return;
        setPlaneGlobal(images.project(pullback(coordinate)).p());
    }

    /**
     * Move the slice to make the {@code coordinate} on chart point {@code p}.
     *
     * @param coordinate slice coordinate, {@link SliceCoordinate#p} follow current atlas reference.
     *                   If it is {@link Double#NaN}, then skip plane adjusting.
     * @param p          chart position
     */
    public void anchorImageTo(SliceCoordinate coordinate, Point2D p) {
        if (images == null) return;

        var plane = coordinate.p();
        if (!Double.isNaN(plane)) setPlane(plane);
        anchorImageToXY(coordinate, p);
    }

    /**
     * Move the slice to the {@code coordinate},
     * and move the {@code coordinate} to given position {@code p} on chart.
     *
     * @param coordinate anatomical coordinate, follow current atlas reference
     * @param p          chart position
     */
    public void anchorImageTo(Coordinate coordinate, Point2D p) {
        if (images == null) return;
        var coor = images.project(pullback(coordinate));
        setPlaneGlobal(coor.p());
        anchorImageToXY(coor, p);
    }

    /**
     * Move the slice to make the {@code coordinate} on chart point {@code p},
     * but ignore plane adjusting.
     *
     * @param coordinate slice coordinate
     * @param p          chart position
     */
    private void anchorImageToXY(SliceCoordinate coordinate, Point2D p) {
        // q: coordinate (x, y) on chart
        var q = painter.getChartTransform().transform(coordinate.x(), coordinate.y());
        var dx = p.getX() - q.getX();
        var dy = p.getY() - q.getY();
        painter.x(painter.x() + dx);
        painter.y(painter.y() + dy);
        canvas.repaintBackground();
    }

    /**
     * Move the slice to the {@code coordinate} with given {@code projection}.
     *
     * @param projection slice project.
     * @param coordinate anatomical coordinate, follow current atlas reference
     */
    public void anchorImageTo(ImageSliceStack.Projection projection, Coordinate coordinate) {
        updateProjection(projection);
        anchorImageTo(coordinate);
    }

    /**
     * Move the slice to the {@code coordinate} with given {@code projection},
     * then move the {@code coordinate} to given position {@code p} on chart.
     *
     * @param projection slice project.
     * @param coordinate anatomical coordinate, follow current atlas reference
     * @param p          chart position
     */
    public void anchorImageTo(ImageSliceStack.Projection projection, Coordinate coordinate, Point2D p) {
        updateProjection(projection);
        anchorImageTo(coordinate, p);
    }

    /**
     * @param name       name of reference
     * @param coordinate global anatomical coordinate
     */
    public void addReference(String name, Coordinate coordinate) {
        addReference(name, coordinate, true);
    }

    /**
     * @param name       name of reference
     * @param coordinate global anatomical coordinate
     * @param flipAP
     */
    public void addReference(String name, Coordinate coordinate, boolean flipAP) {
        var references = Objects.requireNonNull(this.references, "AtlasPlugin is not loaded");
        references.addReference(name, coordinate, flipAP);
    }

    /**
     * {@return a transform from global anatomical space to reference anatomical space}
     */
    public ProbeTransform<Coordinate, Coordinate> getReferenceTransform() {
        return transform;
    }

    public @Nullable ProbeTransform<Coordinate, Coordinate> getReferenceTransform(String reference) {
        var ref = references.getReference(reference);
        if (ref == null) return null;
        return ref.getTransform();
    }

    /**
     * {@return a transform from global anatomical coordinate to slice coordinate}
     */
    public @Nullable ProbeTransform<Coordinate, SliceCoordinate> getSliceTransform() {
        var image = images;
        if (image == null) return null;
        return image.getSliceTransform();
    }

    /**
     * A transform from slice coordinate to chart (probe) coordinate.
     * <br/>
     * To get correct result of inverted transform ({@code ProbeTransform<ProbeCoordinate, SliceCoordinate>}),
     * remember to restore plane location at {@link ProbeCoordinate#z()} before doing {@link ProbeTransform#transform()}.
     *
     * @return a transform from slice coordinate to chart (probe) coordinate
     */
    public ProbeTransform<SliceCoordinate, ProbeCoordinate> getChartTransform() {
        return ProbeTransform.create(SliceDomain.INSTANCE, ProbeTransform.PROBE, painter.getChartTransform());
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
        canvas.repaintBackground(() -> {
            this.projection.set(projection);
            images = new ImageSliceStack(brain, volume, projection);
            canvas.setResetAxesBoundaries(0, images.widthUm(), 0, images.heightUm());
            canvas.resetAxesBoundaries();
            canvas.setAxesEqualRatio();

            var c1 = projectPlane(0);
            var c2 = projectPlane(images.planeUm());
            sliderPlane.setMin(Math.min(c1, c2) / 1000);
            sliderPlane.setMax(Math.max(c1, c2) / 1000);

            sliderRotation.setValue(0);
            sliderOffsetWidth.setValue(0);
            sliderOffsetHeight.setValue(0);
            sliderOffsetWidth.setBlockIncrement(images.resolution()[1]);
            sliderOffsetHeight.setBlockIncrement(images.resolution()[2]);

            updateSliceImage();
            fireAtlasImageUpdateEvent(AtlasUpdateEvent.PROJECTION);
        });
    }

    private void updateSliceImage() {
        if (images == null) return;

        var plane = getPlaneGlobal();

        var dw = sliderOffsetWidth.getValue();
        var dh = sliderOffsetHeight.getValue();

        log.trace("updateSliceImage({}, {}, {})", (int) plane, dw, dh);

        var image = images.sliceAtPlane(plane);
        this.image = image = image.withOffset(dw, dh);
        painter.update(image);
        canvas.repaintBackground();
    }

    public void setImageSlice(ImageSlice slice) {
        setImageSlice(slice, 0);
    }

    /**
     * @param slice
     * @param rotation rotate in degree
     */
    public void setImageSlice(ImageSlice slice, double rotation) {
        if (getProjection() != slice.projection()) {
            updateProjection(slice.projection());
        }

        var resolution = slice.resolution();
        setPlaneIndex(slice.plane());
        sliderRotation.setValue(rotation);
        sliderOffsetWidth.setValue(slice.dw() * resolution[1]);
        sliderOffsetHeight.setValue(slice.dh() * resolution[2]);
    }

    /*=============================*
     * atlas region masking handle *
     *=============================*/

    private @Nullable ImageSliceStack annotations;
    private @Nullable FlatIntArray cacheAnnImage;

    private final List<RegionMask> maskedRegions = new ArrayList<>();

    public List<RegionMask> getMaskedRegions() {
        return Collections.unmodifiableList(maskedRegions);
    }

    public void setMaskedRegions(List<RegionMask> masks) {
        maskedRegions.clear();
        maskedRegions.addAll(masks);
        updateMaskedRegion(maskedRegions);
    }

    public void addMaskedRegion(RegionMask mask) {
        maskedRegions.add(mask);
        updateMaskedRegion(maskedRegions);
    }

    public void addMaskedRegion(List<RegionMask> mask) {
        maskedRegions.addAll(mask);
        updateMaskedRegion(maskedRegions);
    }

    public void clearMaskedRegions() {
        maskedRegions.clear();
        updateMaskedRegion(maskedRegions);
    }

    private void updateMaskedRegion() {
        updateMaskedRegion(maskedRegions);
    }

    private @Nullable ImageSliceStack getAnnotationImageStack(ImageSliceStack.Projection projection) {
        var brain = this.brain;
        if (brain == null) return null;

        if (annotations == null || annotations.projection() != projection) {
            try {
                annotations = new ImageSliceStack(brain, brain.annotation(), projection);
            } catch (IOException e) {
                return null;
            }
        }
        return annotations;
    }

    private void updateMaskedRegion(List<RegionMask> masks) {
        if (!updateMaskedRegionUncheck(masks)) {
            maskPainter.setImage(null);
        }
        canvas.repaintBackground();
    }

    private boolean updateMaskedRegionUncheck(List<RegionMask> masks) {
        if (masks.isEmpty()) return false;

        //
        var brain = this.brain;
        var image = this.image;
        if (brain == null || image == null) return false;

        // init annotations
        var annotations = getAnnotationImageStack(image.projection());
        if (annotations == null) return false;

        var annImage = cacheAnnImage = annotations.sliceAtPlane(image)
            .image(ImageSlice.INT_IMAGE, cacheAnnImage);

        // fetch masked stricture ids
        var root = brain.structures();
        var set = new HashSet<Integer>();
        for (var mask : masks) {
            if (mask.includeChildren) {
                if (mask.exclude) {
                    root.forAllChildren(mask.structure, s -> set.remove(s.id()));
                } else {
                    root.forAllChildren(mask.structure, s -> set.add(s.id()));
                }
            } else {
                if (mask.exclude) {
                    set.remove(mask.id());
                } else {
                    set.add(mask.id());
                }
            }
        }
        if (set.isEmpty()) return true;

        var mask = image.image(new MaskImageWriter(annImage, set));
        maskPainter.setImage(mask);

        return true;
    }


    private static class MaskImageWriter implements ImageSlice.ImageWriter<Image> {
        private final FlatIntArray ann;
        private final Set<Integer> values;
        private WritableImage image;
        private PixelWriter writer;

        private MaskImageWriter(FlatIntArray annImage, Set<Integer> values) {
            this.ann = annImage;
            this.values = values;
        }

        @Override
        public void create(int w, int h, @Nullable Image init) {
            image = new WritableImage(w, h);
            writer = image.getPixelWriter();
        }

        @Override
        public void set(int x, int y, int v) {
            var t = values.contains(ann.get(y, x));
            writer.setArgb(x, y, t ? 0x00000000 : 0x50000000);
        }

        @Override
        public Image get() {
            return image;
        }
    }

    /*====================*
     * Image update event *
     *====================*/

    private void fireAtlasImageUpdateEvent(EventType<AtlasUpdateEvent> event) {
        canvas.fireEvent(new AtlasUpdateEvent(event, this));
    }

    /*=================*
     * information bar *
     *=================*/

    /**
     * @param p point on chart.
     */
    private void updateMouseInformation(Point2D p) {
        var image = this.image;
        if (image == null) {
            labelMouseInformation.setText("");
            labelStructure.setText("");
            return;
        }

        p = painter.getImageTransform().transform(p); // slice <- chart
        var coor = image.pullBack(p); // coor <- slice
        var local = project(coor); // reference <- global
        var text = String.format("=(%.0f, %.0f, %.0f)", local.ap(), local.dv(), local.ml());
        labelMouseInformation.setText(text);

        updateStructureInformation(coor);
    }

    /**
     * @param coor global anatomical coordinate
     */
    private void updateStructureInformation(Coordinate coor) {
        var brain = this.brain;
        if (brain == null) return;

        Thread.ofVirtual().name("updateStructureInformation").start(() -> {
            Structure structure = null;
            try {
                structure = brain.structureAt(coor);
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

                var hem = brain.hemisphereAt(coor);
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

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setOnCloseRequest(_ -> {
            var result = dialog.getResult();
            if (result != ButtonType.OK) {
                painter.setImageAlpha(oldAlpha);
            }
            canvas.repaintBackground();
        });

        dialog.show();
    }

}
