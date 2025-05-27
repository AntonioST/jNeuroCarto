package io.ast.jneurocarto.javafx.chart;

import java.util.Objects;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static Application INSTANCE = null;

    private final Logger log = LoggerFactory.getLogger(Application.class);

    public Application() {
        INSTANCE = this;
    }

    public static Application getInstance() {
        return Objects.requireNonNull(INSTANCE, "chart.Application is not initialized.");
    }

    private Stage stage;
    private InteractionXYChart<ScatterChart<Number, Number>> chart;
    private InteractionXYPainter painter;
    private XYPath path;

    public void start(Stage stage) {
        this.stage = stage;
        log.debug("start");
        stage.setTitle("InteractionXYChart");
        stage.setScene(scene());
        stage.sizeToScene();
        stage.show();
    }

    private Scene scene() {
        var scene = new Scene(root());
        return scene;
    }

    private Parent root() {
        var x = new NumberAxis(0, 100, 10);
        var y = new NumberAxis(0, 100, 10);

        chart = new InteractionXYChart<>(new ScatterChart<>(x, y));
        chart.setMinWidth(800);
        chart.setMinHeight(800);
        chart.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseClicked);

        painter = chart.getPlotting();
        path = new XYPath();
        painter.addGraphics(path);

        path.line(Color.TRANSPARENT);
        path.normalize(Normalize.N01);
        path.colormap("jet");

        var root = new VBox(chart);
        return root;
    }


    private void onMouseClicked(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            if (e.getClickCount() >= 2) {
                chart.resetAxesBoundaries();
            } else {
                var p = chart.getChartTransformFromScene(e.getSceneX(), e.getSceneY());
                path.addData(p, Math.random());
                painter.repaint();
            }
        }
    }
}
