package io.ast.jneurocarto.javafx.chart;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    public interface ApplicationContent {
        void setup(InteractionXYChart chart);
    }

    private final ApplicationContent content;
    private final Logger log = LoggerFactory.getLogger(Application.class);

    public Application(ApplicationContent content) {
        this.content = content;
    }

    /*===========*
     * UI Layout *
     *===========*/

    private Stage stage;
    private InteractionXYChart chart;

    public void start(Stage stage) {
        this.stage = stage;
        log.debug("start");
        stage.setTitle("InteractionXYChart");
        stage.setScene(scene());
        stage.sizeToScene();
        stage.show();

        content.setup(chart);
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

        return new VBox(chart);
    }
}
